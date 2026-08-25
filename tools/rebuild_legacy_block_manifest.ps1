param(
    [string]$LegacyRoot = "..\..\HBM_1.7.10",
    [string]$ProjectRoot = "."
)

$ErrorActionPreference = "Stop"
$ProjectRoot = (Resolve-Path -LiteralPath $ProjectRoot).Path
$LegacyRoot = if ([IO.Path]::IsPathRooted($LegacyRoot)) {
    $LegacyRoot
} else {
    Join-Path $ProjectRoot $LegacyRoot
}
$LegacyRoot = (Resolve-Path -LiteralPath $LegacyRoot).Path
$sourcePath = Join-Path $LegacyRoot "src\main\java\com\hbm\blocks\ModBlocks.java"
$catalogPath = Join-Path $ProjectRoot "src\main\resources\legacy\reinhardtshbm\block_catalog.txt"

if (-not (Test-Path -LiteralPath $sourcePath)) {
    throw "Missing 1.7.10 block registry source: $sourcePath"
}

# A registered block must be one of ModBlocks' static fields. This deliberately
# excludes the register(Block b) helper parameter and disabled/commented calls.
$source = Get-Content -LiteralPath $sourcePath -Raw
$source = [regex]::Replace($source, '(?s)/\*.*?\*/', '')
$source = [regex]::Replace($source, '(?m)//.*$', '')

$declared = [System.Collections.Generic.HashSet[string]]::new()
foreach ($match in [regex]::Matches(
        $source,
        '(?m)^\s*(?:@Deprecated\s+)?public\s+static\s+Block\s+([A-Za-z_][A-Za-z0-9_]*)\s*;')) {
    [void]$declared.Add($match.Groups[1].Value)
}

$registered = [System.Collections.Generic.HashSet[string]]::new()
foreach ($match in [regex]::Matches(
        $source,
        'GameRegistry\s*\.\s*registerBlock\s*\(\s*([A-Za-z_][A-Za-z0-9_]*)\s*,')) {
    $id = $match.Groups[1].Value
    if ($declared.Contains($id)) {
        [void]$registered.Add($id)
    }
}
foreach ($match in [regex]::Matches(
        $source,
        '(?m)^\s*register\s*\(\s*([A-Za-z_][A-Za-z0-9_]*)\s*(?:,|\))')) {
    $id = $match.Groups[1].Value
    if ($declared.Contains($id)) {
        [void]$registered.Add($id)
    }
}

$ids = @($registered | Sort-Object)
if ($ids.Count -lt 900) {
    throw "Parsed only $($ids.Count) 1.7.10 block registrations; refusing to replace the catalog."
}

$invalid = @($ids | Where-Object { $_ -notmatch '^[a-z0-9_]+$' })
if ($invalid.Count -gt 0) {
    throw "Invalid legacy block ids: $($invalid -join ', ')"
}

$sourceHash = (Get-FileHash -LiteralPath $sourcePath -Algorithm SHA256).Hash
$lines = @(
    '# Generated from HBM_1.7.10 ModBlocks.mainRegistry() registrations.',
    '# Direct GameRegistry.registerBlock calls and register(field) helper calls are included.',
    "# Source: $sourcePath",
    "# SHA256: $sourceHash",
    "# Block registrations: $($ids.Count)",
    ''
) + $ids

$encoding = New-Object System.Text.UTF8Encoding($false)
[IO.File]::WriteAllLines($catalogPath, $lines, $encoding)
Write-Host "Rebuilt 1.7.10 block catalog: $($ids.Count) ids"
