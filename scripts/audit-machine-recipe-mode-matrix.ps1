param(
    [string]$DefaultRuntimeReport = "run-recipe-audit/recipe-audit.tsv",
    [string]$Mode528RuntimeReport = "run-recipe-audit-528/recipe-audit.tsv",
    [string]$ExpensiveRuntimeReport = "run-recipe-audit-expensive/recipe-audit.tsv",
    [string]$Mode528ExpensiveRuntimeReport = "run-recipe-audit-528-expensive/recipe-audit.tsv",
    [string]$RecipeRoot = "src/main/resources/data/reinhardtshbm/recipe",
    [string]$OutputReport = "build/reports/machine-recipe-mode-matrix.txt",
    [switch]$FailOnMismatch
)

$ErrorActionPreference = "Stop"
$modId = "reinhardtshbm"
$condition528 = "$modId`:enable_528_mode"
$conditionExpensive = "$modId`:enable_expensive_mode"
$problems = [System.Collections.Generic.List[string]]::new()

$modes = [ordered]@{
    normal = [pscustomobject]@{ Report = $DefaultRuntimeReport; Mode528 = $false; Expensive = $false }
    mode528 = [pscustomobject]@{ Report = $Mode528RuntimeReport; Mode528 = $true; Expensive = $false }
    expensive = [pscustomobject]@{ Report = $ExpensiveRuntimeReport; Mode528 = $false; Expensive = $true }
    mode528_expensive = [pscustomobject]@{ Report = $Mode528ExpensiveRuntimeReport; Mode528 = $true; Expensive = $true }
}

function Read-RecipeIds([string]$Path) {
    if (-not (Test-Path -LiteralPath $Path -PathType Leaf)) {
        throw "Recipe mode audit is missing runtime report: $Path"
    }
    $ids = [System.Collections.Generic.HashSet[string]]::new([System.StringComparer]::Ordinal)
    foreach ($line in Get-Content -LiteralPath $Path) {
        $parts = $line -split "`t"
        if ($parts.Count -ge 2 -and $parts[0] -ceq 'RECIPE') {
            [void]$ids.Add($parts[1])
        }
    }
    return $ids
}

function Get-ConditionLeafTypes([object]$Node) {
    $types = @()
    if ($null -eq $Node) { return $types }
    if ($Node -is [System.Collections.IEnumerable] -and $Node -isnot [string] -and
            $Node -isnot [System.Management.Automation.PSCustomObject]) {
        foreach ($entry in $Node) { $types += @(Get-ConditionLeafTypes $entry) }
        return $types
    }
    $typeProperty = $Node.PSObject.Properties['type']
    if ($null -ne $typeProperty) {
        $type = [string]$typeProperty.Value
        if ($type -cne 'neoforge:not') { $types += $type }
    }
    foreach ($property in $Node.PSObject.Properties) {
        if ($property.Name -ceq 'type' -or $property.Value -is [string] -or $null -eq $property.Value) { continue }
        $types += @(Get-ConditionLeafTypes $property.Value)
    }
    return $types
}

function Get-ConditionPolarities([object]$Node, [string]$Target, [bool]$Negated = $false) {
    $polarities = @()
    if ($null -eq $Node) { return $polarities }
    if ($Node -is [System.Collections.IEnumerable] -and $Node -isnot [string] -and
            $Node -isnot [System.Management.Automation.PSCustomObject]) {
        foreach ($entry in $Node) { $polarities += @(Get-ConditionPolarities $entry $Target $Negated) }
        return $polarities
    }
    $typeProperty = $Node.PSObject.Properties['type']
    if ($null -ne $typeProperty) {
        $type = [string]$typeProperty.Value
        if ($type -ceq $Target) {
            return @(if ($Negated) { -1 } else { 1 })
        }
        if ($type -ceq 'neoforge:not') {
            $valueProperty = $Node.PSObject.Properties['value']
            if ($null -ne $valueProperty) {
                return @(Get-ConditionPolarities $valueProperty.Value $Target (-not $Negated))
            }
        }
    }
    foreach ($property in $Node.PSObject.Properties) {
        if ($property.Name -in @('type', 'value') -or $property.Value -is [string] -or $null -eq $property.Value) { continue }
        $polarities += @(Get-ConditionPolarities $property.Value $Target $Negated)
    }
    return $polarities
}

function Get-SinglePolarity([int[]]$Polarities, [string]$RecipeId, [string]$Label) {
    $unique = @($Polarities | Sort-Object -Unique)
    if ($unique.Count -gt 1) {
        $script:problems.Add("$RecipeId contains contradictory $Label conditions")
        return 0
    }
    if ($unique.Count -eq 0) { return 0 }
    return [int]$unique[0]
}

function Expected-ByPolarity([int]$Polarity, [bool]$Enabled) {
    if ($Polarity -eq 0) { return $true }
    if ($Polarity -gt 0) { return $Enabled }
    return -not $Enabled
}

foreach ($mode in $modes.Values) {
    $mode | Add-Member -NotePropertyName Recipes -NotePropertyValue (Read-RecipeIds $mode.Report)
    $mode | Add-Member -NotePropertyName ReportLines -NotePropertyValue @(Get-Content -LiteralPath $mode.Report)
}

$recipeRootPath = (Resolve-Path -LiteralPath $RecipeRoot).Path
$recipes = [System.Collections.Generic.List[object]]::new()
foreach ($file in Get-ChildItem -LiteralPath $recipeRootPath -Filter '*.json' -File -Recurse) {
    $relative = $file.FullName.Substring($recipeRootPath.Length).TrimStart('\', '/').Replace('\', '/')
    $recipeId = "$modId`:$($relative.Substring(0, $relative.Length - 5))"
    try {
        $json = Get-Content -LiteralPath $file.FullName -Raw | ConvertFrom-Json
    } catch {
        $problems.Add("Invalid recipe JSON ${relative}: $($_.Exception.Message)")
        continue
    }
    $conditionsProperty = $json.PSObject.Properties['neoforge:conditions']
    $conditions = if ($null -ne $conditionsProperty) { $conditionsProperty.Value } else { $null }
    $leafTypes = @(Get-ConditionLeafTypes $conditions | Sort-Object -Unique)
    $polarity528 = Get-SinglePolarity @(Get-ConditionPolarities $conditions $condition528) $recipeId '528-mode'
    $polarityExpensive = Get-SinglePolarity @(Get-ConditionPolarities $conditions $conditionExpensive) $recipeId 'expensive-mode'
    $resultId = ''
    if ($null -ne $json.PSObject.Properties['result']) {
        if ($json.result -is [string]) { $resultId = [string]$json.result }
        elseif ($null -ne $json.result.PSObject.Properties['id']) { $resultId = [string]$json.result.id }
    } elseif ($null -ne $json.PSObject.Properties['output'] -and
            $null -ne $json.output.PSObject.Properties['id']) {
        $resultId = [string]$json.output.id
    }
    $recipes.Add([pscustomobject]@{
        Id = $recipeId
        Relative = $relative
        Type = [string]$json.type
        Group = [string]$json.group
        Result = $resultId
        ConditionTypes = $leafTypes
        Mode528Polarity = $polarity528
        ExpensivePolarity = $polarityExpensive
    })
}

$modeConditionTypes = @($condition528, $conditionExpensive)
foreach ($recipe in $recipes) {
    if ($recipe.Mode528Polarity -eq 0 -and $recipe.ExpensivePolarity -eq 0) { continue }
    $onlyModeConditions = @($recipe.ConditionTypes | Where-Object { $_ -notin $modeConditionTypes }).Count -eq 0
    foreach ($entry in $modes.GetEnumerator()) {
        $mode = $entry.Value
        $expected = (Expected-ByPolarity $recipe.Mode528Polarity $mode.Mode528) -and
                (Expected-ByPolarity $recipe.ExpensivePolarity $mode.Expensive)
        $actual = $mode.Recipes.Contains($recipe.Id)
        if (-not $expected -and $actual) {
            $problems.Add("$($recipe.Id) is active in $($entry.Key), despite its mode condition")
        } elseif ($onlyModeConditions -and $expected -and -not $actual) {
            $problems.Add("$($recipe.Id) is missing from $($entry.Key), although its mode condition is satisfied")
        }
    }
    if ($onlyModeConditions -and $recipe.ExpensivePolarity -ne 0 -and $recipe.Mode528Polarity -eq 0) {
        if ($modes.normal.Recipes.Contains($recipe.Id) -ne $modes.mode528.Recipes.Contains($recipe.Id)) {
            $problems.Add("$($recipe.Id) changes when only 528 mode is toggled; expensive-mode recipes must be independent")
        }
        if ($modes.expensive.Recipes.Contains($recipe.Id) -ne $modes.mode528_expensive.Recipes.Contains($recipe.Id)) {
            $problems.Add("$($recipe.Id) changes when only 528 mode is toggled inside expensive mode")
        }
    }
    if ($onlyModeConditions -and $recipe.Mode528Polarity -ne 0 -and $recipe.ExpensivePolarity -eq 0) {
        if ($modes.normal.Recipes.Contains($recipe.Id) -ne $modes.expensive.Recipes.Contains($recipe.Id)) {
            $problems.Add("$($recipe.Id) changes when only expensive mode is toggled; 528 recipes must be independent")
        }
        if ($modes.mode528.Recipes.Contains($recipe.Id) -ne $modes.mode528_expensive.Recipes.Contains($recipe.Id)) {
            $problems.Add("$($recipe.Id) changes when only expensive mode is toggled inside 528 mode")
        }
    }
}

$expectedExpensivePartitions = [ordered]@{
    "$modId`:assembly_machine" = [pscustomobject]@{ Groups = 90; ExpensiveFiles = 92 }
    "$modId`:plasma_forge" = [pscustomobject]@{ Groups = 13; ExpensiveFiles = 15 }
    "$modId`:precision_assembler" = [pscustomobject]@{ Groups = 2; ExpensiveFiles = 2 }
    "$modId`:exposure_chamber" = [pscustomobject]@{ Groups = 1; ExpensiveFiles = 1 }
}
foreach ($partition in $expectedExpensivePartitions.GetEnumerator()) {
    $type = $partition.Key
    $expensiveRecipes = @($recipes | Where-Object { $_.Type -ceq $type -and $_.ExpensivePolarity -gt 0 })
    $groups = @($expensiveRecipes.Group | Where-Object { $_ } | Sort-Object -Unique)
    if ($groups.Count -ne $partition.Value.Groups) {
        $problems.Add("$type has $($groups.Count) expensive-mode groups; expected $($partition.Value.Groups)")
    }
    if ($expensiveRecipes.Count -ne $partition.Value.ExpensiveFiles) {
        $problems.Add("$type has $($expensiveRecipes.Count) expensive-mode recipe files; expected $($partition.Value.ExpensiveFiles)")
    }
    foreach ($group in $groups) {
        $expensiveGroup = @($expensiveRecipes | Where-Object { $_.Group -ceq $group })
        $normalGroup = @($recipes | Where-Object {
            $_.Type -ceq $type -and $_.Group -ceq $group -and $_.ExpensivePolarity -lt 0
        })
        if ($normalGroup.Count -eq 0) {
            $problems.Add("$type group $group has an expensive branch but no not-expensive branch")
            continue
        }
        $expensiveOutputs = @($expensiveGroup.Result | Where-Object { $_ } | Sort-Object -Unique)
        $normalOutputs = @($normalGroup.Result | Where-Object { $_ } | Sort-Object -Unique)
        foreach ($output in $expensiveOutputs) {
            if ($output -notin $normalOutputs) {
                $problems.Add("$type group $group changes output in expensive mode: $output has no normal counterpart")
            }
        }
        $unguarded = @($recipes | Where-Object {
            $_.Type -ceq $type -and $_.Group -ceq $group -and $_.ExpensivePolarity -eq 0 -and
                    ($_.Result -in $expensiveOutputs)
        })
        foreach ($recipe in $unguarded) {
            $problems.Add("$($recipe.Id) belongs to expensive pair $group but has no expensive-mode condition")
        }
    }
}

function Get-ChemicalFluidInputs([string[]]$ReportLines, [string]$RecipeId) {
    $inputs = [System.Collections.Generic.List[object]]::new()
    foreach ($line in $ReportLines) {
        $parts = $line -split "`t"
        if ($parts.Count -ge 6 -and $parts[0] -ceq 'CHEMICAL_FLUID_INPUT' -and
                $parts[1] -ceq $RecipeId) {
            $inputs.Add([pscustomobject]@{
                Index = [int]$parts[2]
                Fluid = $parts[3]
                Amount = [int]$parts[4]
                Pressure = [int]$parts[5]
            })
        }
    }
    return @($inputs | Sort-Object Index)
}

# GenericRecipe resolves these eight pressure requirements and the Kevlar fluid
# substitution from the 528 sub-toggle. Validate the server-resolved values in
# all four modes so expensive mode can never affect the 528 chemistry branch.
$chemicalProfiles = [ordered]@{
    polymer = @(
        [pscustomobject]@{ Fluid = 'petroleum'; Amount = 1000; Pressure = 0; Pressure528 = 1 }
    )
    bakelite = @(
        [pscustomobject]@{ Fluid = 'aromatics'; Amount = 500; Pressure = 0; Pressure528 = 1 },
        [pscustomobject]@{ Fluid = 'petroleum'; Amount = 500; Pressure = 0; Pressure528 = 1 }
    )
    rubber = @(
        [pscustomobject]@{ Fluid = 'unsaturateds'; Amount = 500; Pressure = 0; Pressure528 = 2 }
    )
    hardplastic = @(
        [pscustomobject]@{ Fluid = 'xylene'; Amount = 500; Pressure = 0; Pressure528 = 2 },
        [pscustomobject]@{ Fluid = 'phosgene'; Amount = 500; Pressure = 0; Pressure528 = 2 }
    )
    pvc = @(
        [pscustomobject]@{ Fluid = 'unsaturateds'; Amount = 250; Pressure = 0; Pressure528 = 2 },
        [pscustomobject]@{ Fluid = 'chlorine'; Amount = 250; Pressure = 0; Pressure528 = 2 }
    )
    kevlar = @(
        [pscustomobject]@{ Fluid = 'aromatics'; Amount = 200; Pressure = 0 },
        [pscustomobject]@{ Fluid = 'nitric_acid'; Amount = 100; Pressure = 0 },
        [pscustomobject]@{ Fluid = 'chlorine'; Fluid528 = 'phosgene'; Amount = 100; Pressure = 0 }
    )
    rocketfuel = @(
        [pscustomobject]@{ Fluid = 'petroleum'; Amount = 200; Pressure = 0; Pressure528 = 1 },
        [pscustomobject]@{ Fluid = 'nitric_acid'; Amount = 100; Pressure = 0 }
    )
    tnt = @(
        [pscustomobject]@{ Fluid = 'aromatics'; Amount = 500; Pressure = 0; Pressure528 = 1 }
    )
    c4 = @(
        [pscustomobject]@{ Fluid = 'unsaturateds'; Amount = 500; Pressure = 0; Pressure528 = 1 }
    )
}
foreach ($modeEntry in $modes.GetEnumerator()) {
    $mode = $modeEntry.Value
    foreach ($profile in $chemicalProfiles.GetEnumerator()) {
        $recipeId = "$modId`:chemical_plant/$($profile.Key)"
        $actualInputs = @(Get-ChemicalFluidInputs $mode.ReportLines $recipeId)
        $expectedInputs = @($profile.Value)
        if ($actualInputs.Count -ne $expectedInputs.Count) {
            $problems.Add("$recipeId has $($actualInputs.Count) fluid inputs in $($modeEntry.Key); expected $($expectedInputs.Count)")
            continue
        }
        for ($index = 0; $index -lt $expectedInputs.Count; $index++) {
            $expected = $expectedInputs[$index]
            $expectedFluid = if ($mode.Mode528 -and $null -ne $expected.PSObject.Properties['Fluid528']) {
                [string]$expected.Fluid528
            } else {
                [string]$expected.Fluid
            }
            $expectedPressure = if ($mode.Mode528 -and $null -ne $expected.PSObject.Properties['Pressure528']) {
                [int]$expected.Pressure528
            } else {
                [int]$expected.Pressure
            }
            $actual = $actualInputs[$index]
            if ($actual.Index -ne $index -or $actual.Fluid -cne $expectedFluid -or
                    $actual.Amount -ne [int]$expected.Amount -or $actual.Pressure -ne $expectedPressure) {
                $problems.Add("$recipeId fluid input $index is $($actual.Fluid) $($actual.Amount)mB P$($actual.Pressure) in $($modeEntry.Key); expected $expectedFluid $($expected.Amount)mB P$expectedPressure")
            }
        }
    }
}

function Assert-PrecisionOutputWeights([string]$ModeName, [string[]]$ReportLines,
        [string]$RecipePath, [string]$ExpectedWeights) {
    $recipeId = "$modId`:precision_assembler/$RecipePath"
    $actualWeights = @()
    foreach ($line in $ReportLines) {
        $parts = $line -split "`t"
        if ($parts.Count -ge 3 -and $parts[0] -ceq 'PRECISION_OUTPUT_WEIGHTS' -and
                $parts[1] -ceq $recipeId) {
            $actualWeights += [string]$parts[2]
        }
    }
    if ($actualWeights.Count -ne 1) {
        $script:problems.Add("$recipeId has $($actualWeights.Count) effective weight records in $ModeName; expected exactly one")
    } elseif ($actualWeights[0] -cne $ExpectedWeights) {
        $script:problems.Add("$recipeId effective weights are $($actualWeights[0]) in $ModeName; expected $ExpectedWeights")
    }
}

# In latest 1.7.10, 528 mode exposes the precision-assembler recycling
# recipes, while expensive mode only lowers the reclaim chance for chips,
# controllers and overdrive upgrades. The other five upgrade families stay at
# 75% even when both toggles are enabled.
$precisionDynamicWeights = [ordered]@{
    chip_recycle = [pscustomobject]@{ Normal = '90,90,90'; Expensive = '50,50,50' }
    chip_bismoid_recycle = [pscustomobject]@{ Normal = '75,75,75,75'; Expensive = '50,50,50,50' }
    chip_quantum_recycle = [pscustomobject]@{ Normal = '75,75,75,75,75'; Expensive = '50,50,50,50,50' }
    atomic_clock_recycle = [pscustomobject]@{ Normal = '75,75,75,75'; Expensive = '50,50,50,50' }
    controller_recycle = [pscustomobject]@{ Normal = '90,90,90,90,90,90'; Expensive = '50,50,50,50,50,50' }
    controller_advanced_recycle = [pscustomobject]@{ Normal = '75,75,75,75,75,75'; Expensive = '50,50,50,50,50,50' }
    controller_quantum_recycle = [pscustomobject]@{ Normal = '75,75,75,75,75,75'; Expensive = '50,50,50,50,50,50' }
    upgrade_overdive_i_recycle = [pscustomobject]@{ Normal = '75,75,75,75,75'; Expensive = '50,50,50,50,50' }
    upgrade_overdive_ii_recycle = [pscustomobject]@{ Normal = '75,75,75,75,75,75'; Expensive = '50,50,50,50,50,50' }
    upgrade_overdive_iii_recycle = [pscustomobject]@{ Normal = '75,75,75,75,75,75'; Expensive = '50,50,50,50,50,50' }
}
$precisionFixedUpgradeRecipes = @(
    'upgrade_ab_ii_recycle',
    'upgrade_ab_iii_recycle',
    'upgrade_effect_ii_recycle',
    'upgrade_effect_iii_recycle',
    'upgrade_fortune_ii_recycle',
    'upgrade_fortune_iii_recycle',
    'upgrade_power_ii_recycle',
    'upgrade_power_iii_recycle',
    'upgrade_speed_ii_recycle',
    'upgrade_speed_iii_recycle'
)
foreach ($modeEntry in $modes.GetEnumerator()) {
    $mode = $modeEntry.Value
    if ($mode.Mode528) {
        foreach ($recipe in $precisionDynamicWeights.GetEnumerator()) {
            $expectedWeights = if ($mode.Expensive) { $recipe.Value.Expensive } else { $recipe.Value.Normal }
            Assert-PrecisionOutputWeights $modeEntry.Key $mode.ReportLines $recipe.Key $expectedWeights
        }
        foreach ($recipePath in $precisionFixedUpgradeRecipes) {
            Assert-PrecisionOutputWeights $modeEntry.Key $mode.ReportLines $recipePath '75,75,75,75'
        }
    }

    $crystalRecipe = if ($mode.Expensive) {
        'orbital_assembly_crystal_circuit_expensive'
    } else {
        'orbital_assembly_crystal_circuit'
    }
    $crystalWeights = if ($mode.Expensive) { '25,75' } else { '50,50' }
    Assert-PrecisionOutputWeights $modeEntry.Key $mode.ReportLines $crystalRecipe $crystalWeights

    $crystalRecycleRecipe = if ($mode.Expensive) {
        'orbital_assembly_crystal_circuit_recycle_expensive'
    } else {
        'orbital_assembly_crystal_circuit_recycle'
    }
    $crystalRecycleWeights = if ($mode.Expensive) { '100,100,100,100,100' } else { '100,100,100,100' }
    Assert-PrecisionOutputWeights $modeEntry.Key $mode.ReportLines $crystalRecycleRecipe $crystalRecycleWeights
}

$normalExposurePath = Join-Path $recipeRootPath 'exposure_chamber/particle_sparkticle_c_ingots_schrabidium_ingot_dineutronium.json'
$expensiveExposurePath = Join-Path $recipeRootPath 'exposure_chamber/particle_sparkticle_item_expensive_degenerate_matter_ingot_dineutronium.json'
$normalExposure = Get-Content -LiteralPath $normalExposurePath -Raw | ConvertFrom-Json
$expensiveExposure = Get-Content -LiteralPath $expensiveExposurePath -Raw | ConvertFrom-Json
if ($normalExposure.particle.item -cne "$modId`:particle_sparkticle" -or
        $normalExposure.ingredient.tag -cne 'c:ingots/schrabidium' -or
        $normalExposure.output.id -cne "$modId`:ingot_dineutronium") {
    $problems.Add('normal exposure-chamber dineutronium branch no longer matches latest 1.7.10')
}
if ($expensiveExposure.particle.item -cne "$modId`:particle_sparkticle" -or
        $expensiveExposure.ingredient.item -cne "$modId`:item_expensive_degenerate_matter" -or
        $expensiveExposure.output.id -cne "$modId`:ingot_dineutronium") {
    $problems.Add('expensive exposure-chamber dineutronium branch no longer matches latest 1.7.10')
}

foreach ($modeEntry in $modes.GetEnumerator()) {
    $mode = $modeEntry.Value
    $assemblyAnvilOutputs = @()
    foreach ($line in $mode.ReportLines) {
        $parts = $line -split "`t"
        if ($parts.Count -ge 4 -and $parts[0] -ceq 'ANVIL_OUTPUT' -and
                $parts[2] -ceq "$modId`:machine_assembly_machine") {
            $assemblyAnvilOutputs += $parts[1]
        }
    }
    if ($assemblyAnvilOutputs.Count -ne 1) {
        $problems.Add("expected one assembly-machine anvil construction recipe in $($modeEntry.Key), found $($assemblyAnvilOutputs.Count)")
        continue
    }
    $recipeKey = $assemblyAnvilOutputs[0]
    $circuitInputs = @()
    foreach ($line in $mode.ReportLines) {
        $parts = $line -split "`t"
        if ($parts.Count -ge 5 -and $parts[0] -ceq 'ANVIL_INPUT' -and $parts[1] -ceq $recipeKey -and
                $parts[3] -in @("$modId`:circuit_vacuum_tube", "$modId`:circuit_analog")) {
            $circuitInputs += [pscustomobject]@{ Item = $parts[3]; Count = [int]$parts[4] }
        }
    }
    $expectedCircuit = if ($mode.Expensive) { "$modId`:circuit_analog" } else { "$modId`:circuit_vacuum_tube" }
    $expectedCount = if ($mode.Expensive) { 2 } else { 4 }
    if ($circuitInputs.Count -ne 1 -or $circuitInputs[0].Item -cne $expectedCircuit -or
            $circuitInputs[0].Count -ne $expectedCount) {
        $actual = ($circuitInputs | ForEach-Object { "$($_.Item)x$($_.Count)" }) -join ', '
        $problems.Add("assembly-machine anvil circuit input is [$actual] in $($modeEntry.Key); expected ${expectedCircuit}x${expectedCount}")
    }
}

$lines = [System.Collections.Generic.List[string]]::new()
$lines.Add('HBM machine recipe mode matrix audit')
$lines.Add("recipe files: $($recipes.Count)")
$lines.Add("mode-conditioned files: $(@($recipes | Where-Object { $_.Mode528Polarity -ne 0 -or $_.ExpensivePolarity -ne 0 }).Count)")
foreach ($entry in $modes.GetEnumerator()) {
    $lines.Add("$($entry.Key) active recipes: $($entry.Value.Recipes.Count)")
}
$lines.Add("problems: $($problems.Count)")
$lines.Add('')
$lines.Add('EXPENSIVE PARTITIONS')
foreach ($partition in $expectedExpensivePartitions.GetEnumerator()) {
    $type = $partition.Key
    $expensiveRecipes = @($recipes | Where-Object { $_.Type -ceq $type -and $_.ExpensivePolarity -gt 0 })
    $lines.Add("$type`tgroups=$(@($expensiveRecipes.Group | Sort-Object -Unique).Count)`tfiles=$($expensiveRecipes.Count)")
}
$lines.Add('')
$lines.Add('PROBLEMS')
foreach ($problem in $problems) { $lines.Add($problem) }

$outputFile = [System.IO.Path]::GetFullPath($OutputReport)
[System.IO.Directory]::CreateDirectory([System.IO.Path]::GetDirectoryName($outputFile)) | Out-Null
[System.IO.File]::WriteAllLines($outputFile, $lines, [System.Text.UTF8Encoding]::new($false))
Write-Host "Machine recipe mode matrix audit: $($recipes.Count) files, $($problems.Count) problems. Report: $outputFile"

if ($FailOnMismatch -and $problems.Count -gt 0) {
    throw "Machine recipe mode matrix audit failed with $($problems.Count) problem(s); see $outputFile"
}
