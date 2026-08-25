param(
    [string]$ProjectRoot = ".",
    [string]$LegacyRoot = "..\..\HBM_1.7.10"
)

$ErrorActionPreference = "Stop"
$ProjectRoot = (Resolve-Path -LiteralPath $ProjectRoot).Path
$legacyRootPath = if ([IO.Path]::IsPathRooted($LegacyRoot)) {
    $LegacyRoot
} else {
    Join-Path $ProjectRoot $LegacyRoot
}

& (Join-Path $PSScriptRoot "rebuild_legacy_block_manifest.ps1") `
    -ProjectRoot $ProjectRoot `
    -LegacyRoot $legacyRootPath

$legacyDir = Join-Path $ProjectRoot "src\main\resources\legacy\reinhardtshbm"
$catalogPath = Join-Path $legacyDir "block_catalog.txt"
$placeholderPath = Join-Path $legacyDir "blocks.txt"
$hbmBlocksPath = Join-Path $ProjectRoot "src\main\java\com\reinhardt\hbm\registry\HbmBlocks.java"
$legacyContentPath = Join-Path $ProjectRoot "src\main\java\com\reinhardt\hbm\registry\LegacyHbmContent.java"
$reportPath = Join-Path $legacyDir "block_audit.txt"

function Read-Ids([string]$Path) {
    return @(Get-Content -LiteralPath $Path | ForEach-Object { $_.Trim() } |
        Where-Object { $_ -and -not $_.StartsWith('#') } | Sort-Object -Unique)
}

$catalog = Read-Ids $catalogPath
$placeholders = Read-Ids $placeholderPath
$hbmBlocks = Get-Content -LiteralPath $hbmBlocksPath -Raw
$legacyContent = Get-Content -LiteralPath $legacyContentPath -Raw

$core = [System.Collections.Generic.HashSet[string]]::new()
foreach ($match in [regex]::Matches(
        $hbmBlocks,
        'public\s+static\s+final\s+DeferredBlock<[^;]+?\s+[A-Z0-9_]+\s*=\s*(?:register|registerBlock)\s*\(\s*"([a-z0-9_.-]+)"',
        [Text.RegularExpressions.RegexOptions]::Singleline)) {
    [void]$core.Add($match.Groups[1].Value)
}

$coreText = [regex]::Match(
    $legacyContent,
    'CORE_BLOCKS\s*=\s*Set\.of\((.*?)\);',
    [Text.RegularExpressions.RegexOptions]::Singleline).Groups[1].Value
foreach ($match in [regex]::Matches($coreText, '"([a-z0-9_.-]+)"')) {
    [void]$core.Add($match.Groups[1].Value)
}

$catalogSet = [System.Collections.Generic.HashSet[string]]::new([string[]]$catalog)
$placeholderSet = [System.Collections.Generic.HashSet[string]]::new([string[]]$placeholders)
$currentNotLegacy = @($placeholders | Where-Object { -not $catalogSet.Contains($_) })
$resolved = @($catalog | Where-Object { $core.Contains($_) })
$unresolved = @($catalog | Where-Object { -not $core.Contains($_) })
$missingFromPlaceholderManifest = @($unresolved | Where-Object { -not $placeholderSet.Contains($_) })

$lines = @(
    '# Generated block migration audit. Do not use this report as a registry manifest.',
    "# Legacy registered block ids: $($catalog.Count)",
    "# Formally registered 1.21.1 blocks: $($resolved.Count)",
    "# Legacy block ids still unresolved: $($unresolved.Count)",
    "# Existing blocks.txt ids absent from 1.7.10 registry: $($currentNotLegacy.Count)",
    "# Unresolved 1.7.10 ids missing from blocks.txt: $($missingFromPlaceholderManifest.Count)",
    '',
    '[UNRESOLVED_1_7_10_BLOCKS]'
) + $unresolved + @('', '[UNRESOLVED_MISSING_FROM_BLOCKS_TXT]') + $missingFromPlaceholderManifest + @('', '[BLOCKS_TXT_NOT_IN_1_7_10_CATALOG]') + $currentNotLegacy

$encoding = New-Object System.Text.UTF8Encoding($false)
[IO.File]::WriteAllLines($reportPath, $lines, $encoding)
Write-Host "Wrote block audit: $reportPath"
Write-Host "Legacy=$($catalog.Count) resolved=$($resolved.Count) unresolved=$($unresolved.Count) missing-manifest=$($missingFromPlaceholderManifest.Count)"
