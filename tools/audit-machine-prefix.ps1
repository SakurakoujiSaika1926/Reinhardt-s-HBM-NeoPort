param(
    [string]$ProjectRoot = $PSScriptRoot + "\\..",
    [string]$LegacyRoot = "E:\\MC\\Modsource\\HBM_1.7.10",
    [string]$OutputPath = $PSScriptRoot + "\\..\\docs\\machine-prefix-audit.md"
)

$ErrorActionPreference = "Stop"

function Read-Utf8([string]$Path) {
    return [System.IO.File]::ReadAllText((Resolve-Path -LiteralPath $Path), [System.Text.Encoding]::UTF8)
}

function Escape-Markdown([string]$Text) {
    return $Text.Replace('|', '\\|')
}

# The 1.7.10 block declarations are the source of truth. The old placeholder
# list is intentionally not used here: a machine absent from that list is
# precisely the kind of migration hole this audit needs to expose.
$hbmBlocksPath = Join-Path $ProjectRoot "src\\main\\java\\com\\reinhardt\\hbm\\registry\\HbmBlocks.java"
$blockEntitiesPath = Join-Path $ProjectRoot "src\\main\\java\\com\\reinhardt\\hbm\\registry\\HbmBlockEntities.java"
$oldBlocksPath = Join-Path $LegacyRoot "src\\main\\java\\com\\hbm\\blocks\\ModBlocks.java"
$oldTileMappingsPath = Join-Path $LegacyRoot "src\\main\\java\\com\\hbm\\tileentity\\TileMappings.java"

$newBlocks = Read-Utf8 $hbmBlocksPath
$newBlockEntities = Read-Utf8 $blockEntitiesPath
$oldBlocks = Read-Utf8 $oldBlocksPath
$oldTileMappings = Read-Utf8 $oldTileMappingsPath

# These are source-level renames already used by the 1.21.1 port.
$renames = @{
    'machine_difurnace_extension' = 'machine_difurnace_ext'
    'machine_turbinegas' = 'machine_turbine_gas'
    'machine_fensu' = 'machine_battery_redd'
    'machine_bigasstank' = 'machine_bat9000'
    'machine_boiler' = 'machine_boiler_off'
}

# 1.7.10 left this declaration registered but deliberately removed every
# gameplay surface: the multiblock dimensions, GUI, tick loop, and recipe are
# commented out.  It is an in-game memorial rather than a port target.
$retiredInLegacy = @{
    'machine_industrial_generator' = '1.7.10 memorial only; recipe, GUI, multiblock and tick logic are commented out'
}

$declarations = [regex]::Matches(
    $oldBlocks,
    '(?m)^\s*(?<deprecated>@Deprecated\s+)?public\s+static\s+Block\s+(?<id>machine_[a-z0-9_]+);'
) | ForEach-Object {
    [PSCustomObject]@{
        Id = $_.Groups['id'].Value
        Deprecated = $_.Groups['deprecated'].Success
    }
} | Sort-Object Id -Unique

$registeredIds = [System.Collections.Generic.HashSet[string]]::new([System.StringComparer]::Ordinal)
[regex]::Matches($newBlocks, '"(?<id>machine_[a-z0-9_]+)"') | ForEach-Object {
    [void]$registeredIds.Add($_.Groups['id'].Value)
}

$lines = [System.Collections.Generic.List[string]]::new()
$lines.Add('# machine_ Migration Audit')
$lines.Add('')
$lines.Add('Generated from the 1.7.10 source declarations by `tools/audit-machine-prefix.ps1`.')
$lines.Add('`Complete` means the block has a real 1.21.1 registration. Rendering, collision, ports, GUI, recipes, and runtime behavior remain separately testable migration contracts.')
$lines.Add('')
$lines.Add("- Legacy source: ``$LegacyRoot``")
$lines.Add("- Declared 1.7.10 `machine_` IDs: **$($declarations.Count)**")
$lines.Add("- Explicitly deprecated in 1.7.10: **$(($declarations | Where-Object Deprecated).Count)**")
$lines.Add('')
$lines.Add('| Legacy ID | Legacy block class | Legacy tile mapping | Status | Modern ID | BE registration |')
$lines.Add('| --- | --- | --- | --- | --- | --- |')

$missing = [System.Collections.Generic.List[string]]::new()
$complete = 0
$deprecated = 0
foreach ($declaration in $declarations) {
    $id = $declaration.Id
    $modernId = if ($renames.ContainsKey($id)) { $renames[$id] } else { $id }
    $classPattern = '(?m)^\s*' + [regex]::Escape($id) + '\s*=\s*new\s+([A-Za-z0-9_]+)'
    $classMatch = [regex]::Match($oldBlocks, $classPattern)
    $legacyClass = if ($classMatch.Success) { $classMatch.Groups[1].Value } else { '-' }
    $tileClass = if ($legacyClass -eq '-') { '' } else { 'TileEntity' + $legacyClass }
    $tileMatch = if ($tileClass) {
        $tilePattern = '(?m)^\s*put\(' + [regex]::Escape($tileClass) + '\.class,\s*"([^"]+)"\)'
        [regex]::Match($oldTileMappings, $tilePattern)
    } else {
        $null
    }
    $legacyTile = if ($tileMatch -and $tileMatch.Success) {
        "$tileClass -> $($tileMatch.Groups[1].Value)"
    } else {
        '-'
    }

    if ($declaration.Deprecated) {
        $status = 'Deprecated in 1.7.10 - not ported'
        $deprecated++
        $be = '-'
    } elseif ($retiredInLegacy.ContainsKey($id)) {
        $status = 'Retired in 1.7.10 - not ported'
        $be = '-'
    } elseif ($registeredIds.Contains($modernId)) {
        $status = if ($modernId -eq $id) { 'Complete registration' } else { 'Complete registration (renamed)' }
        $complete++
        $be = if ($newBlockEntities.Contains('HbmBlocks.' + ($modernId -replace '^machine_', 'MACHINE_').ToUpperInvariant())) { 'registered' } else { 'not required or custom' }
    } else {
        $status = 'MISSING - non-deprecated 1.7.10 machine'
        $missing.Add($id)
        $be = '-'
    }

    $lines.Add("| ``$id`` | $(Escape-Markdown $legacyClass) | $(Escape-Markdown $legacyTile) | $status | ``$modernId`` | $be |")
}

$lines.Add('')
$lines.Add('## Result')
$lines.Add('')
$lines.Add("- Active legacy machines registered in 1.21.1: **$complete**")
$lines.Add("- Active legacy machines still missing: **$($missing.Count)**")
if ($missing.Count -gt 0) {
    $lines.Add('- Missing IDs: ' + (($missing | ForEach-Object { "``$_``" }) -join ', '))
}

$outputDirectory = Split-Path -Parent $OutputPath
New-Item -ItemType Directory -Force -Path $outputDirectory | Out-Null
[System.IO.File]::WriteAllLines($OutputPath, $lines, [System.Text.UTF8Encoding]::new($false))
Write-Host "Wrote $OutputPath"
