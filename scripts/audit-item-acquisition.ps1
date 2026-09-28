param(
    [string]$RuntimeReport = "run-recipe-audit/recipe-audit.tsv",
    [string]$OutputReport = "build/reports/item-acquisition-audit.txt",
    [string]$DynamicSources = "gradle/verification/dynamic-item-sources.tsv",
    [switch]$FailOnMissing
)

$ErrorActionPreference = "Stop"
$modId = "reinhardtshbm"
$recipeRoot = (Resolve-Path "src/main/resources/data/$modId/recipe").Path
$runtimeLines = Get-Content $RuntimeReport
$activeRecipes = [System.Collections.Generic.HashSet[string]]::new()
$registeredItems = [System.Collections.Generic.HashSet[string]]::new()
$sources = [System.Collections.Generic.HashSet[string]]::new()
$baseSources = [System.Collections.Generic.HashSet[string]]::new()
$exceptions = [System.Collections.Generic.HashSet[string]]::new()
$exceptionReasons = @{}
$inputGroups = @{}
$inputsByRecipe = @{}
$outputsByRecipe = @{}

function Add-RecipeOutput([string]$recipeId, [string]$itemId) {
    if (-not $itemId) { return }
    [void]$sources.Add($itemId)
    if (-not $outputsByRecipe.ContainsKey($recipeId)) {
        $outputsByRecipe[$recipeId] = [System.Collections.Generic.HashSet[string]]::new()
    }
    [void]$outputsByRecipe[$recipeId].Add($itemId)
}

function Add-RecipeInput([string]$recipeId, [string]$inputIndex, [string]$itemId) {
    $key = $recipeId + "#" + $inputIndex
    if (-not $inputGroups.ContainsKey($key)) {
        $inputGroups[$key] = [System.Collections.Generic.HashSet[string]]::new()
    }
    [void]$inputGroups[$key].Add($itemId)
    if (-not $inputsByRecipe.ContainsKey($recipeId)) {
        $inputsByRecipe[$recipeId] = @{}
    }
    if (-not $inputsByRecipe[$recipeId].ContainsKey($inputIndex)) {
        $inputsByRecipe[$recipeId][$inputIndex] = [System.Collections.Generic.HashSet[string]]::new()
    }
    [void]$inputsByRecipe[$recipeId][$inputIndex].Add($itemId)
}

foreach ($line in $runtimeLines) {
    $parts = $line -split "`t"
    switch ($parts[0]) {
        "ITEM" { [void]$registeredItems.Add($parts[1]) }
        "RECIPE" { [void]$activeRecipes.Add($parts[1]) }
        "RECIPE_OUTPUT" { Add-RecipeOutput $parts[1] $parts[2] }
        "ANVIL_OUTPUT" { Add-RecipeOutput ("anvil:" + $parts[1]) $parts[2] }
        # Foundry recipes are generated from a validated material/mold matrix rather than
        # data-pack recipes. Keep them as explicit machine acquisition roots; their material
        # inputs are audited independently by the regular ingot/billet recipe chains.
        "FOUNDRY_OUTPUT" {
            [void]$sources.Add($parts[3])
            [void]$baseSources.Add($parts[3])
        }
        "ANVIL_INPUT" { Add-RecipeInput ("anvil:" + $parts[1]) $parts[2] $parts[3] }
        "RECIPE_INPUT" {
            Add-RecipeInput $parts[1] $parts[2] $parts[3]
        }
    }
}

function Add-OutputNode([string]$recipeId, $node) {
    if ($null -eq $node) { return }
    if ($node -is [System.Collections.IDictionary]) {
        if ($node.Contains("id") -and $node.id -is [string]) {
            Add-RecipeOutput $recipeId $node.id
        }
        if ($node.Contains("item")) { Add-OutputNode $recipeId $node.item }
        if ($node.Contains("result")) { Add-OutputNode $recipeId $node.result }
        return
    }
    if ($node -is [pscustomobject]) {
        $id = $node.PSObject.Properties["id"]
        if ($null -ne $id -and $id.Value -is [string]) {
            Add-RecipeOutput $recipeId $id.Value
        }
        $item = $node.PSObject.Properties["item"]
        if ($null -ne $item) { Add-OutputNode $recipeId $item.Value }
        $result = $node.PSObject.Properties["result"]
        if ($null -ne $result) { Add-OutputNode $recipeId $result.Value }
        return
    }
    if ($node -is [System.Collections.IEnumerable] -and $node -isnot [string]) {
        foreach ($entry in $node) { Add-OutputNode $recipeId $entry }
    }
}

$outputFields = @{
    "minecraft:blasting" = @("result")
    "minecraft:crafting_shaped" = @("result")
    "minecraft:crafting_shapeless" = @("result")
    "minecraft:smelting" = @("result")
    "$modId`:ammo_press" = @("result")
    "$modId`:arc_furnace" = @("solid")
    "$modId`:arc_welder" = @("result")
    "$modId`:assembly_machine" = @("result")
    "$modId`:blast_furnace" = @("result", "byproduct")
    "$modId`:breeder_reactor" = @("result")
    "$modId`:centrifuge" = @("results")
    "$modId`:chemical_plant" = @("output_items")
    "$modId`:coking" = @("output", "byproduct")
    "$modId`:combination_oven" = @("output")
    "$modId`:compressor" = @("output")
    "$modId`:crystallizer" = @("result")
    "$modId`:cyclotron" = @("output")
    "$modId`:electrolyzer_fluid" = @("byproducts")
    "$modId`:electrolyzer_metal" = @("byproducts")
    "$modId`:exposure_chamber" = @("output")
    "$modId`:fuel_pool" = @("result")
    "$modId`:fusion" = @("output_item")
    "$modId`:fusion_breeder_item" = @("result")
    "$modId`:gas_centrifuge" = @("results")
    "$modId`:liquefaction" = @("output")
    "$modId`:particle_accelerator" = @("output1", "output2")
    "$modId`:plasma_forge" = @("result")
    "$modId`:precision_assembler" = @("outputs")
    "$modId`:press" = @("result")
    "$modId`:purex" = @("output_items")
    "$modId`:pyrolysis" = @("item_output")
    "$modId`:rbmk_outgasser" = @("item_output")
    "$modId`:refinery" = @("byproduct")
    "$modId`:rotary_furnace" = @("output")
    "$modId`:shredder" = @("result")
    "$modId`:silex" = @("outputs")
    "$modId`:soldering_station" = @("result")
    "$modId`:solidification" = @("output")
    "$modId`:supercomputer" = @("outputs")
    "$modId`:space_assembler" = @("results")
}

Get-ChildItem $recipeRoot -Recurse -Filter *.json | ForEach-Object {
    $relative = $_.FullName.Substring($recipeRoot.Length + 1).Replace("\", "/")
    $recipeId = "$modId`:" + $relative.Substring(0, $relative.Length - 5)
    if (-not $activeRecipes.Contains($recipeId)) { return }
    # Windows PowerShell 5.1 does not provide ConvertFrom-Json -AsHashtable.
    # Keep this audit runnable from both powershell.exe and pwsh.exe.
    $recipe = Get-Content $_.FullName -Raw | ConvertFrom-Json
    foreach ($field in $outputFields[$recipe.type]) {
        $property = $recipe.PSObject.Properties[$field]
        if ($null -ne $property) { Add-OutputNode $recipeId $property.Value }
    }
}

Get-ChildItem src/main/resources/data -Recurse -Filter *.json |
    Where-Object { $_.FullName -match "[\\/]loot_table[s]?[\\/]" } |
    ForEach-Object {
        $raw = Get-Content $_.FullName -Raw
        [regex]::Matches($raw, '"name"\s*:\s*"(reinhardtshbm:[a-z0-9_./-]+)"') |
            ForEach-Object {
                [void]$sources.Add($_.Groups[1].Value)
                [void]$baseSources.Add($_.Groups[1].Value)
            }
    }

if (Test-Path $DynamicSources) {
    Get-Content $DynamicSources | ForEach-Object {
        $trimmed = $_.Trim()
        if ($trimmed -and -not $trimmed.StartsWith("#")) {
            $parts = $trimmed -split "`t"
            $itemId = $parts[0]
            if ($parts.Count -ge 2 -and $parts[1] -eq "EXCEPTION") {
                [void]$exceptions.Add($itemId)
                $exceptionReasons[$itemId] = if ($parts.Count -ge 3) { $parts[2] } else { "documented exception" }
            } else {
                [void]$sources.Add($itemId)
                [void]$baseSources.Add($itemId)
            }
        }
    }
}

$reachable = [System.Collections.Generic.HashSet[string]]::new()
foreach ($itemId in $baseSources) { [void]$reachable.Add($itemId) }
$reachableRecipes = [System.Collections.Generic.HashSet[string]]::new()
$changed = $true
while ($changed) {
    $changed = $false
    foreach ($recipeId in $outputsByRecipe.Keys) {
        if ($reachableRecipes.Contains($recipeId)) { continue }
        $canRun = $true
        if ($inputsByRecipe.ContainsKey($recipeId)) {
            foreach ($alternatives in $inputsByRecipe[$recipeId].Values) {
                $groupReachable = $false
                foreach ($itemId in $alternatives) {
                    if (-not $itemId.StartsWith("$modId`:") -or
                        $reachable.Contains($itemId) -or
                        $exceptions.Contains($itemId)) {
                        $groupReachable = $true
                        break
                    }
                }
                if (-not $groupReachable) {
                    $canRun = $false
                    break
                }
            }
        }
        if (-not $canRun) { continue }
        [void]$reachableRecipes.Add($recipeId)
        foreach ($itemId in $outputsByRecipe[$recipeId]) {
            if ($reachable.Add($itemId)) { $changed = $true }
        }
    }
}

$missingGroups = @(foreach ($entry in $inputGroups.GetEnumerator()) {
    $alternatives = @($entry.Value)
    if ($alternatives | Where-Object { -not $_.StartsWith("$modId`:") }) { continue }
    if ($alternatives | Where-Object { $reachable.Contains($_) }) { continue }
    if ($alternatives | Where-Object { $exceptions.Contains($_) }) { continue }
    [pscustomobject]@{
        Use = $entry.Key
        Alternatives = ($alternatives | Sort-Object) -join ","
    }
})
$missingItems = @($missingGroups.Alternatives -split "," | Where-Object { $_ } | Sort-Object -Unique)
$usedExceptions = @($inputGroups.Values |
    ForEach-Object { $_ } |
    Where-Object { $exceptions.Contains($_) } |
    Sort-Object -Unique)
$sourceLessRegistered = @($registeredItems |
    Where-Object { -not $sources.Contains($_) -and -not $exceptions.Contains($_) } |
    Sort-Object)
$unreachableRecipeOutputs = @($outputsByRecipe.Keys |
    Where-Object { -not $reachableRecipes.Contains($_) } |
    ForEach-Object {
        $recipeId = $_
        $outputsByRecipe[$recipeId] | ForEach-Object { "$recipeId`t$_" }
    } |
    Sort-Object)

$lines = @(
    "HBM item acquisition audit",
    "registered items: $($registeredItems.Count)",
    "active recipes: $($activeRecipes.Count)",
    "known item sources: $($sources.Count)",
    "root item sources: $($baseSources.Count)",
    "transitively reachable items: $($reachable.Count)",
    "documented source exceptions: $($usedExceptions.Count)",
    "registered items without a declared source: $($sourceLessRegistered.Count)",
    "recipes with unreachable outputs: $($unreachableRecipeOutputs.Count)",
    "resolved ingredient groups: $($inputGroups.Count)",
    "unresolved ingredient groups: $($missingGroups.Count)",
    "unresolved unique items: $($missingItems.Count)",
    "",
    "DOCUMENTED EXCEPTIONS"
) + ($usedExceptions | ForEach-Object { "$_`t$($exceptionReasons[$_])" }) +
    @("", "UNRESOLVED ITEMS") + $missingItems + @("", "UNRESOLVED USES") +
    ($missingGroups | Sort-Object Alternatives, Use | ForEach-Object { "$($_.Alternatives)`t$($_.Use)" }) +
    @("", "UNREACHABLE RECIPE OUTPUTS") + $unreachableRecipeOutputs +
    @("", "REGISTERED ITEMS WITHOUT DECLARED SOURCE") + $sourceLessRegistered

$outputFile = [System.IO.Path]::GetFullPath($OutputReport)
[System.IO.Directory]::CreateDirectory([System.IO.Path]::GetDirectoryName($outputFile)) | Out-Null
[System.IO.File]::WriteAllLines($outputFile, $lines, [System.Text.UTF8Encoding]::new($false))
Write-Host "Item acquisition audit: $($missingItems.Count) unresolved items across $($missingGroups.Count) ingredient groups. Report: $outputFile"

if ($FailOnMissing -and $missingItems.Count -gt 0) {
    throw "Item acquisition audit found unresolved recipe inputs: $($missingItems -join ', ')"
}
