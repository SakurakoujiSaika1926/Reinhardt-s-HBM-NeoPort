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
# Count every explicit HbmBlocks field regardless of the specialized factory
# used to preserve its behavior (OBJ, material, rail, door, TE, etc.).
foreach ($match in [regex]::Matches(
        $hbmBlocks,
        'public\s+static\s+final\s+DeferredBlock<[^;]+?\s+[A-Z0-9_]+\s*=\s*([a-zA-Z0-9_]+)\s*\(\s*"([a-z0-9_.-]+)"',
        [Text.RegularExpressions.RegexOptions]::Singleline)) {
    [void]$core.Add($match.Groups[2].Value)
}

$coreText = [regex]::Match(
    $legacyContent,
    'CORE_BLOCKS\s*=\s*Set\.of\((.*?)\);',
    [Text.RegularExpressions.RegexOptions]::Singleline).Groups[1].Value
foreach ($match in [regex]::Matches($coreText, '"([a-z0-9_.-]+)"')) {
    [void]$core.Add($match.Groups[1].Value)
}

# Some real blocks are registered through an enum-backed factory rather than
# a string literal at the field declaration. Keep the audit aligned with the
# registry by reading those ids from the same source of truth.
$doorDeclPath = Join-Path $ProjectRoot 'src\main\java\com\reinhardt\hbm\door\HbmDoorDecl.java'
if (Test-Path -LiteralPath $doorDeclPath) {
    $doorDeclText = Get-Content -LiteralPath $doorDeclPath -Raw
    foreach ($match in [regex]::Matches($doorDeclText, '^\s*[A-Z0-9_]+\("([a-z0-9_.-]+)"', [Text.RegularExpressions.RegexOptions]::Multiline)) {
        [void]$core.Add($match.Groups[1].Value)
    }
}

$catalogSet = [System.Collections.Generic.HashSet[string]]::new([string[]]$catalog)
$placeholderSet = [System.Collections.Generic.HashSet[string]]::new([string[]]$placeholders)
$retired = [System.Collections.Generic.HashSet[string]]::new([string[]]@(
    'capacitor_gold',
    'capacitor_niobium',
    'capacitor_schrabidate',
    'capacitor_tantalium',
    'machine_battery',
    'machine_battery_potato',
    'machine_difurnace_extension',
    'machine_difurnace_rtg_off',
    'machine_difurnace_rtg_on',
    'machine_dineutronium_battery',
    'machine_fensu',
    'machine_lithium_battery',
    'machine_minirtg',
    'machine_powerrtg',
    'machine_rtg_furnace_off',
    'machine_rtg_furnace_on',
    'machine_schrabidium_battery',
    # 1.7.10 developer/test registrations with no production name or gameplay role.
    'event_tester',
    'obj_tester',
    'statue_elb_f'
))
$legacyAliases = @{
    # The modern registry keeps these legacy spellings as separate real
    # blocks, except hev_battery: NeoForge cannot share one item id between
    # the old block item and the already registered HEV suit battery item.
    "hev_battery" = "hev_battery_block"
}
$currentNotLegacy = @($placeholders | Where-Object { -not $catalogSet.Contains($_) })
$resolved = @($catalog | Where-Object {
    $core.Contains($_) -or ($legacyAliases.ContainsKey($_) -and $core.Contains($legacyAliases[$_]))
})
$unresolved = @($catalog | Where-Object {
    -not $core.Contains($_) -and -not $retired.Contains($_) -and
        -not ($legacyAliases.ContainsKey($_) -and $core.Contains($legacyAliases[$_]))
})
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
