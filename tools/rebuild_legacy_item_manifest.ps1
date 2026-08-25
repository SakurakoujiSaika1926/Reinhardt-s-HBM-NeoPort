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
$sourcePath = Join-Path $LegacyRoot "src\main\java\com\hbm\items\ModItems.java"
$manifestPath = Join-Path $ProjectRoot "src\main\resources\legacy\reinhardtshbm\item_catalog.txt"

if (-not (Test-Path -LiteralPath $sourcePath)) {
    throw "Missing 1.7.10 item registry source: $sourcePath"
}

# ModItems registers every standalone item directly with its field. The field
# name is not always the actual registry id: 1.7.10 uses the item's
# unlocalized name (for example mp_chip_1 -> mp_c_1) when registering.
#
# Strip comments first so disabled registrations never become live manifest
# entries. Then split Java assignment statements before matching them. A
# multiline regex over the complete initializer section can cross a semicolon
# boundary and assign the next item's name to the previous field.
$source = Get-Content -LiteralPath $sourcePath -Raw
$source = [regex]::Replace($source, '(?s)/\*.*?\*/', '')
$source = [regex]::Replace($source, '(?m)//.*$', '')
$matches = [regex]::Matches(
    $source,
    'GameRegistry\s*\.\s*registerItem\s*\(\s*([A-Za-z_][A-Za-z0-9_]*)\s*,'
)

$declaredFields = [System.Collections.Generic.HashSet[string]]::new()
foreach ($match in [regex]::Matches($source, '(?m)^\s*public\s+static\s+(?:final\s+)?[A-Za-z0-9_$.<>]+\s+([A-Za-z_][A-Za-z0-9_]*)\s*;')) {
    [void]$declaredFields.Add($match.Groups[1].Value)
}

# addRemap() registers a method-local temporary named remap. It is a runtime
# compatibility conversion rather than a stable registry entry, so keep only
# fields declared by ModItems itself.
$registeredFields = @($matches | ForEach-Object { $_.Groups[1].Value } |
    Where-Object { $declaredFields.Contains($_) } | Sort-Object -Unique)
if ($registeredFields.Count -lt 1700) {
    throw "Parsed only $($registeredFields.Count) 1.7.10 item registrations; refusing to replace the manifest."
}

$registryIdsByField = @{}
$externalFieldAssignments = [System.Collections.Generic.HashSet[string]]::new()
$unresolvedLocalInitializers = [System.Collections.Generic.HashSet[string]]::new()
foreach ($statement in ($source -split ';')) {
    $assignment = [regex]::Match(
        $statement,
        '(?s)\b([A-Za-z_][A-Za-z0-9_]*)\s*='
    )
    if (-not $assignment.Success) {
        continue
    }

    $field = $assignment.Groups[1].Value
    if ($field -notin $registeredFields) {
        continue
    }

    $unlocalized = [regex]::Match($statement, '\.setUnlocalizedName\(\s*"([a-z0-9_]+)"\s*\)')
    if (-not $unlocalized.Success) {
        # Armor and a few legacy items are built in another registry class and
        # merely assigned here. That is not a ModItems-local initializer, so
        # their already-matching field id is the only valid fallback.
        if ($statement -match '\bnew\s+[A-Za-z0-9_$.]+' -or $statement -match '\.copy\s*\(') {
            [void]$unresolvedLocalInitializers.Add($field)
        } else {
            [void]$externalFieldAssignments.Add($field)
        }
        continue
    }

    $id = $unlocalized.Groups[1].Value
    if ($registryIdsByField.ContainsKey($field) -and $registryIdsByField[$field] -ne $id) {
        throw "Conflicting registry ids for ${field}: $($registryIdsByField[$field]) and $id"
    }
    $registryIdsByField[$field] = $id
}

$unresolvedFields = @($unresolvedLocalInitializers | Where-Object { -not $registryIdsByField.ContainsKey($_) })
if ($unresolvedFields.Count -gt 0) {
    throw "Local 1.7.10 item initializers without setUnlocalizedName: $($unresolvedFields -join ', ')"
}

foreach ($field in $registeredFields) {
    if (-not $registryIdsByField.ContainsKey($field)) {
        # No ModItems-local initializer was found. This is how the armor
        # registry exposes its fields: it constructs them in ModItemsArmor and
        # retains the matching public field name for registration here.
        $registryIdsByField[$field] = $field
    }
}

$ids = @($registeredFields | ForEach-Object { $registryIdsByField[$_] } | Sort-Object -Unique)
if ($ids.Count -lt 1700) {
    throw "Resolved only $($ids.Count) unique 1.7.10 registry ids; refusing to replace the manifest."
}

$invalid = @($ids | Where-Object { $_ -notmatch '^[a-z0-9_]+$' })
if ($invalid.Count -gt 0) {
    throw "Invalid legacy item ids: $($invalid -join ', ')"
}

$sourceHash = (Get-FileHash -LiteralPath $sourcePath -Algorithm SHA256).Hash
$lines = @(
    '# Generated from HBM_1.7.10 ModItems.mainRegistry() direct GameRegistry.registerItem calls.',
    "# Source: $sourcePath",
    "# SHA256: $sourceHash",
    "# Item registrations: $($ids.Count)",
    ''
) + $ids

$encoding = New-Object System.Text.UTF8Encoding($false)
[IO.File]::WriteAllLines($manifestPath, $lines, $encoding)
Write-Host "Rebuilt 1.7.10 item catalog: $($ids.Count) ids"
