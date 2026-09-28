param(
    [string]$DefaultRuntimeReport = "run-recipe-audit/recipe-audit.tsv",
    [string]$Mode528RuntimeReport = "run-recipe-audit-528/recipe-audit.tsv",
    [string]$ManufacturingContracts = "gradle/verification/machine-manufacturing-contracts.tsv",
    [string]$WorkRecipeCounts = "gradle/verification/machine-work-recipe-counts.tsv",
    [string]$OutputReport = "build/reports/machine-recipe-coverage.txt",
    [switch]$FailOnMissing
)

$ErrorActionPreference = "Stop"
$modId = "reinhardtshbm"
$blocksSourcePath = "src/main/java/com/reinhardt/hbm/registry/HbmBlocks.java"
$blockEntitiesSourcePath = "src/main/java/com/reinhardt/hbm/registry/HbmBlockEntities.java"
$recipeTypesSourcePath = "src/main/java/com/reinhardt/hbm/registry/HbmRecipeTypes.java"
$recipeRoot = "src/main/resources/data/$modId/recipe"

foreach ($required in @(
        $blocksSourcePath,
        $blockEntitiesSourcePath,
        $recipeTypesSourcePath,
        $DefaultRuntimeReport,
        $Mode528RuntimeReport,
        $ManufacturingContracts,
        $WorkRecipeCounts,
        $recipeRoot
    )) {
    if (-not (Test-Path -LiteralPath $required)) {
        throw "Machine recipe audit is missing required input: $required"
    }
}

function Add-OutputId([object]$Value, [System.Collections.Generic.HashSet[string]]$Target) {
    if ($null -eq $Value) { return }
    if ($Value -is [string]) {
        if ($Value -match '^[a-z0-9_.-]+:[a-z0-9_./-]+$') {
            [void]$Target.Add($Value)
        }
        return
    }
    if ($Value -is [System.Collections.IEnumerable] -and $Value -isnot [string]) {
        foreach ($entry in $Value) { Add-OutputId $entry $Target }
        return
    }
    $idProperty = $Value.PSObject.Properties['id']
    if ($null -ne $idProperty -and $idProperty.Value -is [string]) {
        [void]$Target.Add($idProperty.Value)
    }
    foreach ($nestedName in @('item', 'result')) {
        $nested = $Value.PSObject.Properties[$nestedName]
        if ($null -ne $nested) { Add-OutputId $nested.Value $Target }
    }
}

function Read-Runtime([string]$Path) {
    $outputs = [System.Collections.Generic.HashSet[string]]::new()
    $counts = @{}
    foreach ($line in Get-Content -LiteralPath $Path) {
        $parts = $line -split "`t"
        if ($parts.Count -lt 2) { continue }
        switch ($parts[0]) {
            'RECIPE' {
                if ($parts.Count -ge 3) {
                    $type = $parts[2]
                    if (-not $counts.ContainsKey($type)) { $counts[$type] = 0 }
                    $counts[$type]++
                }
            }
            'RECIPE_OUTPUT' {
                if ($parts.Count -ge 3) { [void]$outputs.Add($parts[2]) }
            }
            'ANVIL_OUTPUT' {
                if ($parts.Count -ge 3) { [void]$outputs.Add($parts[2]) }
            }
            'FOUNDRY_OUTPUT' {
                if ($parts.Count -ge 4) { [void]$outputs.Add($parts[3]) }
            }
        }
    }
    return [pscustomobject]@{ Outputs = $outputs; Counts = $counts }
}

function Read-TabContracts([string]$Path, [int]$MinimumColumns) {
    $rows = @()
    $lineNumber = 0
    foreach ($line in Get-Content -LiteralPath $Path) {
        $lineNumber++
        $trimmed = $line.Trim()
        if (-not $trimmed -or $trimmed.StartsWith('#')) { continue }
        $parts = $line -split "`t", $MinimumColumns
        if ($parts.Count -lt $MinimumColumns) {
            throw "${Path}:${lineNumber} must contain at least $MinimumColumns tab-separated columns"
        }
        $rows += ,$parts
    }
    return $rows
}

$blocksSource = Get-Content -LiteralPath $blocksSourcePath -Raw
$blockEntitiesSource = Get-Content -LiteralPath $blockEntitiesSourcePath -Raw
$recipeTypesSource = Get-Content -LiteralPath $recipeTypesSourcePath -Raw

# Resolve every HbmBlocks constant to the literal registry id supplied to its
# registration helper.  This intentionally does not infer names from Java
# constant spelling: a renamed helper or alias must still preserve the real id.
$blockIdsByConstant = @{}
foreach ($match in [regex]::Matches(
        $blocksSource,
        '(?s)public\s+static\s+final\s+DeferredBlock(?:<[^;=]+>)?\s+([A-Z0-9_]+)\s*=\s*([^;]+);')) {
    $idMatch = [regex]::Match($match.Groups[2].Value, '\(\s*"([a-z0-9_]+)"')
    if ($idMatch.Success) {
        $blockIdsByConstant[$match.Groups[1].Value] = $idMatch.Groups[1].Value
    }
}

# Heavy-door ids live in HbmDoorDecl and are registered through
# heavyDoor(HbmDoorDecl.X); resolve that explicit indirection as well.
$doorDeclPath = 'src/main/java/com/reinhardt/hbm/door/HbmDoorDecl.java'
if (Test-Path -LiteralPath $doorDeclPath) {
    $doorDeclSource = Get-Content -LiteralPath $doorDeclPath -Raw
    $doorIds = @{}
    foreach ($match in [regex]::Matches($doorDeclSource, '(?m)^\s*([A-Z0-9_]+)\("([a-z0-9_]+)"')) {
        $doorIds[$match.Groups[1].Value] = $match.Groups[2].Value
    }
    foreach ($match in [regex]::Matches(
            $blocksSource,
            '(?m)^\s*public\s+static\s+final\s+DeferredBlock<Block>\s+([A-Z0-9_]+)\s*=\s*heavyDoor\((?:com\.reinhardt\.hbm\.door\.)?HbmDoorDecl\.([A-Z0-9_]+)\);')) {
        $doorConstant = $match.Groups[2].Value
        if ($doorIds.ContainsKey($doorConstant)) {
            $blockIdsByConstant[$match.Groups[1].Value] = $doorIds[$doorConstant]
        }
    }
}

$auditedCategories = @(
    'POWER_BLOCKS',
    'FLUID_BLOCKS',
    'CONTAINER_BLOCKS',
    'THERMAL_BLOCKS',
    'RBMK_BLOCKS',
    'MACHINE_BLOCKS',
    'MINING_PROCESSING_BLOCKS',
    'PETROLEUM_BLOCKS',
    'FOUNDRY_BLOCKS',
    'REACTOR_BLOCKS',
    'TURRET_BLOCKS',
    'NUCLEAR_WEAPON_BLOCKS',
    'ROCKET_MISSILE_BLOCKS'
)
$candidateConstants = [System.Collections.Generic.HashSet[string]]::new()
foreach ($category in $auditedCategories) {
    $categoryMatch = [regex]::Match(
        $blocksSource,
        '(?s)List<DeferredBlock<Block>>\s+' + [regex]::Escape($category) + '\s*=\s*List\.of\((.*?)\);')
    if (-not $categoryMatch.Success) {
        throw "Unable to resolve HbmBlocks.$category for the machine manufacturing audit"
    }
    foreach ($constant in [regex]::Matches($categoryMatch.Groups[1].Value, '\b[A-Z][A-Z0-9_]+\b')) {
        [void]$candidateConstants.Add($constant.Value)
    }
}

# A functional block entity is also in scope even if somebody accidentally
# omitted its block from a creative-tab category.  This is what catches such
# machines as the tape recorder and the pneumatic storage endpoints.
foreach ($match in [regex]::Matches($blockEntitiesSource, 'HbmBlocks\.([A-Z][A-Z0-9_]+)\.get\(\)')) {
    [void]$candidateConstants.Add($match.Groups[1].Value)
}

# Pneumatic logistics originally lived in the broad general-block catalog, so
# its non-BE tubes and (until fully ported) storage housings would otherwise
# evade both the machine categories and the block-entity safety net above.
foreach ($constant in $blockIdsByConstant.Keys) {
    if ($constant.StartsWith('PNEUMATIC_')) {
        [void]$candidateConstants.Add($constant)
    }
}

$candidateIds = [System.Collections.Generic.HashSet[string]]::new()
$problems = [System.Collections.Generic.List[string]]::new()
foreach ($constant in $candidateConstants) {
    if (-not $blockIdsByConstant.ContainsKey($constant)) {
        $problems.Add("Unable to resolve registry id for audited HbmBlocks.$constant")
        continue
    }
    [void]$candidateIds.Add("$modId`:$($blockIdsByConstant[$constant])")
}

$defaultRuntime = Read-Runtime $DefaultRuntimeReport
$mode528Runtime = Read-Runtime $Mode528RuntimeReport
$producedIds = [System.Collections.Generic.HashSet[string]]::new()
foreach ($id in $defaultRuntime.Outputs) { [void]$producedIds.Add($id) }
foreach ($id in $mode528Runtime.Outputs) { [void]$producedIds.Add($id) }

# Include statically declared outputs that are deliberately inactive in the
# current audit instance because they are guarded by a soft dependency.
$outputFields = @(
    'result', 'output', 'outputs', 'output_items', 'solid', 'byproduct',
    'output_item', 'item_output', 'output1', 'output2', 'results'
)
foreach ($recipeFile in Get-ChildItem -LiteralPath $recipeRoot -Filter '*.json' -File -Recurse) {
    try {
        $recipe = Get-Content -LiteralPath $recipeFile.FullName -Raw | ConvertFrom-Json
    } catch {
        throw "Invalid recipe JSON during machine coverage audit: $($recipeFile.FullName): $($_.Exception.Message)"
    }
    foreach ($field in $outputFields) {
        $property = $recipe.PSObject.Properties[$field]
        if ($null -ne $property) { Add-OutputId $property.Value $producedIds }
    }
}

$allowedKinds = [System.Collections.Generic.HashSet[string]]::new(
    [string[]]@('ALIAS', 'ASSEMBLED', 'CREATIVE', 'DAMAGE_STATE', 'INTERNAL', 'WORLD_SOURCE', 'UPSTREAM_INCOMPLETE'))
$manufacturingById = @{}
foreach ($parts in Read-TabContracts $ManufacturingContracts 4) {
    $id = $parts[0].Trim()
    $kind = $parts[1].Trim()
    $target = $parts[2].Trim()
    $reason = $parts[3].Trim()
    if (-not $candidateIds.Contains($id)) {
        $problems.Add("Manufacturing contract references non-audited block $id")
    }
    if (-not $allowedKinds.Contains($kind)) {
        $problems.Add("Manufacturing contract for $id uses unknown kind $kind")
    }
    if (-not $reason) {
        $problems.Add("Manufacturing contract for $id has no reviewable reason")
    }
    if ($manufacturingById.ContainsKey($id)) {
        $problems.Add("Duplicate manufacturing contract for $id")
    }
    $manufacturingById[$id] = [pscustomobject]@{ Kind = $kind; Target = $target; Reason = $reason }
}

$directManufacturing = 0
$documentedNonRecipes = 0
foreach ($id in $candidateIds) {
    if ($producedIds.Contains($id)) {
        $directManufacturing++
        if ($manufacturingById.ContainsKey($id)) {
            $problems.Add("Stale manufacturing exception for ${id}: the item now has a direct recipe/machine output")
        }
        continue
    }
    if (-not $manufacturingById.ContainsKey($id)) {
        $problems.Add("Audited machine/block has no manufacturing recipe or documented source: $id")
        continue
    }
    $contract = $manufacturingById[$id]
    if ($contract.Kind -eq 'ALIAS') {
        if (-not $contract.Target -or -not $producedIds.Contains($contract.Target)) {
            $problems.Add("Manufacturing alias for $id does not resolve to a produced item: $($contract.Target)")
        }
    } elseif ($contract.Target -ne '-') {
        $problems.Add("Manufacturing contract for $id must use '-' in the target column for kind $($contract.Kind)")
    }
    $documentedNonRecipes++
}

# Every custom recipe family actually queried by a block entity must be loaded
# in both normal and 528 modes.  Exact counts make accidental loss visible even
# when a family remains non-empty.
$recipeTypeIdsByConstant = @{}
foreach ($match in [regex]::Matches(
        $recipeTypesSource,
        '(?s)DeferredHolder<RecipeType<\?>[^;=]*>\s+([A-Z0-9_]+)\s*=\s*RECIPE_TYPES\.register\(\s*"([a-z0-9_]+)"')) {
    $recipeTypeIdsByConstant[$match.Groups[1].Value] = "$modId`:$($match.Groups[2].Value)"
}
$referencedTypeConstants = [System.Collections.Generic.HashSet[string]]::new()
foreach ($file in Get-ChildItem 'src/main/java/com/reinhardt/hbm/blockentity' -Filter '*.java' -File -Recurse) {
    $source = Get-Content -LiteralPath $file.FullName -Raw
    foreach ($match in [regex]::Matches($source, 'HbmRecipeTypes\.([A-Z][A-Z0-9_]+)')) {
        [void]$referencedTypeConstants.Add($match.Groups[1].Value)
    }
}
# The orbital assembler is a satellite work machine backed by world saved
# data, not a block entity. Keep its recipe family in the same exact-count
# contract instead of letting that architectural difference evade coverage.
$satelliteSourcePath = 'src/main/java/com/reinhardt/hbm/satellite/SatelliteSavedData.java'
if (Test-Path -LiteralPath $satelliteSourcePath) {
    $source = Get-Content -LiteralPath $satelliteSourcePath -Raw
    foreach ($match in [regex]::Matches($source, 'HbmRecipeTypes\.([A-Z][A-Z0-9_]+)')) {
        [void]$referencedTypeConstants.Add($match.Groups[1].Value)
    }
}

$expectedCounts = @{}
foreach ($parts in Read-TabContracts $WorkRecipeCounts 4) {
    $type = $parts[0].Trim()
    $normal = 0
    $mode528 = 0
    $normalParsed = [int]::TryParse($parts[1].Trim(), [ref]$normal)
    $mode528Parsed = [int]::TryParse($parts[2].Trim(), [ref]$mode528)
    if (-not $normalParsed -or -not $mode528Parsed) {
        $problems.Add("Work-recipe count contract for $type has non-integer counts")
        continue
    }
    if (-not $parts[3].Trim()) {
        $problems.Add("Work-recipe count contract for $type has no source/reason")
    }
    if ($expectedCounts.ContainsKey($type)) {
        $problems.Add("Duplicate work-recipe count contract for $type")
    }
    $expectedCounts[$type] = [pscustomobject]@{ Normal = $normal; Mode528 = $mode528; Reason = $parts[3].Trim() }
}

$referencedTypeIds = [System.Collections.Generic.HashSet[string]]::new()
foreach ($constant in $referencedTypeConstants) {
    if (-not $recipeTypeIdsByConstant.ContainsKey($constant)) {
        $problems.Add("Block entity references unresolved HbmRecipeTypes.$constant")
        continue
    }
    $type = $recipeTypeIdsByConstant[$constant]
    [void]$referencedTypeIds.Add($type)
    $normalCount = if ($defaultRuntime.Counts.ContainsKey($type)) { [int]$defaultRuntime.Counts[$type] } else { 0 }
    $mode528Count = if ($mode528Runtime.Counts.ContainsKey($type)) { [int]$mode528Runtime.Counts[$type] } else { 0 }
    if ($normalCount -le 0) { $problems.Add("Machine work-recipe family is empty in normal mode: $type") }
    if ($mode528Count -le 0) { $problems.Add("Machine work-recipe family is empty in 528 mode: $type") }
    if (-not $expectedCounts.ContainsKey($type)) {
        $problems.Add("Machine work-recipe family has no exact-count contract: $type")
        continue
    }
    $expected = $expectedCounts[$type]
    if ($normalCount -ne $expected.Normal -or $mode528Count -ne $expected.Mode528) {
        $problems.Add("Machine work-recipe count changed for ${type}: normal ${normalCount}/${expected.Normal}, 528 ${mode528Count}/${expected.Mode528}")
    }
}
foreach ($type in $expectedCounts.Keys) {
    if (-not $referencedTypeIds.Contains($type)) {
        $problems.Add("Stale work-recipe count contract is not referenced by any block entity: $type")
    }
}
# HbmRecipeTypes is exclusively the machine/work-recipe registry.  Auditing
# only references discovered from block entities would let a newly registered
# but never consumed family evade this check, so require exact two-way coverage.
foreach ($constant in $recipeTypeIdsByConstant.Keys) {
    if (-not $referencedTypeConstants.Contains($constant)) {
        $problems.Add("Registered machine work-recipe family is not consumed by any block entity or satellite machine: HbmRecipeTypes.$constant ($($recipeTypeIdsByConstant[$constant]))")
    }
}

$lines = [System.Collections.Generic.List[string]]::new()
$lines.Add('HBM machine recipe coverage audit')
$lines.Add("audited category/block-entity blocks: $($candidateIds.Count)")
$lines.Add("directly manufactured blocks: $directManufacturing")
$lines.Add("documented non-recipe states/sources: $documentedNonRecipes")
$lines.Add("block-entity work recipe families: $($referencedTypeIds.Count)")
$lines.Add("problems: $($problems.Count)")
$lines.Add('')
$lines.Add('PROBLEMS')
foreach ($problem in $problems) { $lines.Add($problem) }
$lines.Add('')
$lines.Add('DOCUMENTED NON-RECIPE BLOCKS')
foreach ($id in ($manufacturingById.Keys | Sort-Object)) {
    $contract = $manufacturingById[$id]
    $lines.Add("$id`t$($contract.Kind)`t$($contract.Target)`t$($contract.Reason)")
}
$lines.Add('')
$lines.Add('WORK RECIPE COUNTS')
foreach ($type in ($referencedTypeIds | Sort-Object)) {
    $normal = if ($defaultRuntime.Counts.ContainsKey($type)) { $defaultRuntime.Counts[$type] } else { 0 }
    $mode528 = if ($mode528Runtime.Counts.ContainsKey($type)) { $mode528Runtime.Counts[$type] } else { 0 }
    $lines.Add("$type`tnormal=$normal`t528=$mode528")
}

$outputFile = [System.IO.Path]::GetFullPath($OutputReport)
[System.IO.Directory]::CreateDirectory([System.IO.Path]::GetDirectoryName($outputFile)) | Out-Null
[System.IO.File]::WriteAllLines($outputFile, $lines, [System.Text.UTF8Encoding]::new($false))
Write-Host "Machine recipe coverage audit: $($candidateIds.Count) blocks, $($referencedTypeIds.Count) work families, $($problems.Count) problems. Report: $outputFile"

if ($FailOnMissing -and $problems.Count -gt 0) {
    throw "Machine recipe coverage audit failed with $($problems.Count) problem(s); see $outputFile"
}
