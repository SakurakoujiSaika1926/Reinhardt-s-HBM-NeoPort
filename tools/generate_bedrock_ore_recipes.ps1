param()

$ErrorActionPreference = "Stop"
Set-StrictMode -Version Latest

$projectRoot = Split-Path -Parent $PSScriptRoot
$recipeRoot = Join-Path $projectRoot "src\main\resources\data\reinhardtshbm\recipe"
$crystallizerRoot = Join-Path $recipeRoot "crystallizer"
$centrifugeRoot = Join-Path $recipeRoot "centrifuge"
$modId = "reinhardtshbm"

function Write-JsonFile {
    param(
        [string] $Path,
        [object] $Value
    )

    $json = $Value | ConvertTo-Json -Depth 12
    $encoding = [System.Text.UTF8Encoding]::new($false)
    [System.IO.File]::WriteAllText($Path, $json + [Environment]::NewLine, $encoding)
}

function New-BedrockIngredient {
    param(
        [string] $Grade,
        [string] $OreType
    )

    return [ordered] @{
        type = "neoforge:components"
        items = @("$modId`:bedrock_ore_new")
        components = [ordered] @{
            "minecraft:custom_data" = [ordered] @{
                grade = $Grade
                type = $OreType
            }
        }
    }
}

function New-BedrockStack {
    param(
        [string] $Grade,
        [string] $OreType,
        [int] $Count = 1
    )

    $stack = [ordered] @{
        id = "$modId`:bedrock_ore_new"
        components = [ordered] @{
            "minecraft:custom_data" = [ordered] @{
                grade = $Grade
                type = $OreType
            }
            "minecraft:custom_model_data" = (($gradeOrder[$Grade] * $types.Count) + $typeOrder[$OreType])
        }
    }
    if ($Count -ne 1) {
        $stack.count = $Count
    }
    return $stack
}

function New-FragmentStack {
    param([object] $Output)

    return [ordered] @{
        id = "$modId`:bedrock_ore_fragment"
        count = [int] $Output.count
        components = [ordered] @{
            "minecraft:custom_data" = [ordered] @{
                material_id = [int] $materialIds[$Output.material]
                material = [string] $Output.material
            }
        }
    }
}

function New-CrystallizerRecipe {
    param(
        [string] $InputGrade,
        [string] $OutputGrade,
        [string] $OreType,
        [string] $Fluid,
        [int] $Amount,
        [int] $Duration,
        [int] $InputCount = 1
    )

    return [ordered] @{
        type = "$modId`:crystallizer"
        group = "crystallizer.bedrock_ore"
        ingredient = New-BedrockIngredient -Grade $InputGrade -OreType $OreType
        input_count = $InputCount
        acid = [ordered] @{
            fluid = $Fluid
            amount = $Amount
        }
        duration = $Duration
        result = New-BedrockStack -Grade $OutputGrade -OreType $OreType
    }
}

function Write-CrystallizerRecipe {
    param(
        [string] $Name,
        [string] $InputGrade,
        [string] $OutputGrade,
        [string] $OreType,
        [string] $Fluid,
        [int] $Amount,
        [int] $Duration,
        [int] $InputCount = 1
    )

    Write-JsonFile -Path (Join-Path $crystallizerRoot "bedrock_$OreType`_$Name.json") -Value (New-CrystallizerRecipe `
            -InputGrade $InputGrade `
            -OutputGrade $OutputGrade `
            -OreType $OreType `
            -Fluid $Fluid `
            -Amount $Amount `
            -Duration $Duration `
            -InputCount $InputCount)
}

function Write-CentrifugeRecipe {
    param(
        [string] $Name,
        [string] $InputGrade,
        [string] $OreType,
        [object[]] $Results
    )

    $recipe = [ordered] @{
        type = "$modId`:centrifuge"
        group = "centrifuge"
        ingredient = New-BedrockIngredient -Grade $InputGrade -OreType $OreType
        input_count = 1
        results = @($Results)
    }
    Write-JsonFile -Path (Join-Path $centrifugeRoot "bedrock_$OreType`_$Name.json") -Value $recipe
}

$types = @("light", "heavy", "rare", "actinide", "nonmetal", "crystal")
$typeOrder = @{}
for ($index = 0; $index -lt $types.Count; $index++) {
    $typeOrder[$types[$index]] = $index
}

$grades = @(
    "base", "base_roasted", "base_washed", "primary", "primary_roasted", "primary_sulfuric",
    "primary_nosulfuric", "primary_solvent", "primary_nosolvent", "primary_rad", "primary_norad",
    "primary_first", "primary_second", "crumbs", "sulfuric_byproduct", "sulfuric_roasted",
    "sulfuric_arc", "sulfuric_washed", "solvent_byproduct", "solvent_roasted", "solvent_arc",
    "solvent_washed", "rad_byproduct", "rad_roasted", "rad_arc", "rad_washed"
)
$gradeOrder = @{}
for ($index = 0; $index -lt $grades.Count; $index++) {
    $gradeOrder[$grades[$index]] = $index
}

$materialIds = @{
    iron = 2600; copper = 2900; titanium = 2200; bauxite = 2902; cryolite = 2903; chlorocalcite = 1701
    lithium = 300; sodium = 1100; tungsten = 7400; lead = 8200; gold = 7900; beryllium = 400
    bismuth = 8300; tantalium = 7300; cobalt = 2700; rareearth = 20000; boron = 500; lanthanium = 5700
    niobium = 4100; neodymium = 6000; strontium = 3800; zirconium = 4000; uranium = 9200; thorium = 9032
    radium = 8826; polonium = 8410; technetium = 4399; u238 = 9238; coal = 600; sulfur = 1600; lignite = 601
    kno = 700; fluorite = 900; phosphorus = 1500; silicon = 1400; redstone = 1; cinnabar = 8001; sodalite = 1101
    asbestos = 1401; diamond = 1430; emerald = 401; borax = 501; molysite = 1702
}

$outputs = @{
    light = @{
        primary = @(@{ material = "iron"; count = 9 }, @{ material = "copper"; count = 9 })
        sulfuric = @(@{ material = "titanium"; count = 6 }, @{ material = "bauxite"; count = 9 }, @{ material = "cryolite"; count = 3 })
        solvent = @(@{ material = "chlorocalcite"; count = 5 }, @{ material = "lithium"; count = 5 }, @{ material = "sodium"; count = 3 })
        rad = @(@{ material = "chlorocalcite"; count = 6 }, @{ material = "lithium"; count = 6 }, @{ material = "sodium"; count = 6 })
    }
    heavy = @{
        primary = @(@{ material = "tungsten"; count = 9 }, @{ material = "lead"; count = 9 })
        sulfuric = @(@{ material = "gold"; count = 2 }, @{ material = "gold"; count = 2 }, @{ material = "beryllium"; count = 3 })
        solvent = @(@{ material = "tungsten"; count = 9 }, @{ material = "lead"; count = 9 }, @{ material = "gold"; count = 5 })
        rad = @(@{ material = "bismuth"; count = 2 }, @{ material = "tantalium"; count = 2 }, @{ material = "gold"; count = 6 })
    }
    rare = @{
        primary = @(@{ material = "cobalt"; count = 5 }, @{ material = "rareearth"; count = 5 })
        sulfuric = @(@{ material = "boron"; count = 5 }, @{ material = "lanthanium"; count = 3 }, @{ material = "niobium"; count = 4 })
        solvent = @(@{ material = "neodymium"; count = 3 }, @{ material = "strontium"; count = 3 }, @{ material = "zirconium"; count = 3 })
        rad = @(@{ material = "niobium"; count = 5 }, @{ material = "neodymium"; count = 5 }, @{ material = "strontium"; count = 3 })
    }
    actinide = @{
        primary = @(@{ material = "uranium"; count = 4 }, @{ material = "thorium"; count = 4 })
        sulfuric = @(@{ material = "radium"; count = 2 }, @{ material = "polonium"; count = 2 }, @{ material = "radium"; count = 2 })
        solvent = @(@{ material = "radium"; count = 2 }, @{ material = "polonium"; count = 2 }, @{ material = "technetium"; count = 1 })
        rad = @(@{ material = "technetium"; count = 1 }, @{ material = "u238"; count = 1 })
    }
    nonmetal = @{
        primary = @(@{ material = "coal"; count = 9 }, @{ material = "sulfur"; count = 9 })
        sulfuric = @(@{ material = "lignite"; count = 9 }, @{ material = "kno"; count = 6 }, @{ material = "fluorite"; count = 6 })
        solvent = @(@{ material = "phosphorus"; count = 5 }, @{ material = "fluorite"; count = 6 }, @{ material = "sulfur"; count = 6 })
        rad = @(@{ material = "chlorocalcite"; count = 6 }, @{ material = "silicon"; count = 2 }, @{ material = "silicon"; count = 2 })
    }
    crystal = @{
        primary = @(@{ material = "redstone"; count = 9 }, @{ material = "cinnabar"; count = 4 })
        sulfuric = @(@{ material = "sodalite"; count = 9 }, @{ material = "asbestos"; count = 6 }, @{ material = "diamond"; count = 3 })
        solvent = @(@{ material = "cinnabar"; count = 3 }, @{ material = "asbestos"; count = 5 }, @{ material = "emerald"; count = 3 })
        rad = @(@{ material = "borax"; count = 3 }, @{ material = "molysite"; count = 3 }, @{ material = "sodalite"; count = 9 })
    }
}

Get-ChildItem $crystallizerRoot -Filter "bedrock_*.json" -File -ErrorAction SilentlyContinue | Remove-Item

foreach ($oreType in $types) {
    Write-CrystallizerRecipe -Name "base_water" -InputGrade "base" -OutputGrade "base_washed" -OreType $oreType -Fluid "water" -Amount 250 -Duration 100
    Write-CrystallizerRecipe -Name "base_roasted_water" -InputGrade "base_roasted" -OutputGrade "base_washed" -OreType $oreType -Fluid "water" -Amount 250 -Duration 100

    foreach ($input in @("primary", "primary_roasted")) {
        Write-CrystallizerRecipe -Name "$input`_sulfuric" -InputGrade $input -OutputGrade "primary_sulfuric" -OreType $oreType -Fluid "sulfuric_acid" -Amount 250 -Duration 200
    }
    foreach ($input in @("primary", "primary_roasted", "primary_nosulfuric")) {
        Write-CrystallizerRecipe -Name "$input`_solvent" -InputGrade $input -OutputGrade "primary_solvent" -OreType $oreType -Fluid "solvent" -Amount 250 -Duration 200
    }
    foreach ($input in @("primary", "primary_roasted", "primary_nosulfuric", "primary_nosolvent")) {
        Write-CrystallizerRecipe -Name "$input`_radiosolvent" -InputGrade $input -OutputGrade "primary_rad" -OreType $oreType -Fluid "radiosolvent" -Amount 250 -Duration 200
    }
    foreach ($path in @(
            @{ input = "sulfuric_byproduct"; output = "sulfuric_washed" },
            @{ input = "sulfuric_roasted"; output = "sulfuric_washed" },
            @{ input = "sulfuric_arc"; output = "sulfuric_washed" },
            @{ input = "solvent_byproduct"; output = "solvent_washed" },
            @{ input = "solvent_roasted"; output = "solvent_washed" },
            @{ input = "solvent_arc"; output = "solvent_washed" },
            @{ input = "rad_byproduct"; output = "rad_washed" },
            @{ input = "rad_roasted"; output = "rad_washed" },
            @{ input = "rad_arc"; output = "rad_washed" }
        )) {
        Write-CrystallizerRecipe -Name "$($path.input)_water" -InputGrade $path.input -OutputGrade $path.output -OreType $oreType -Fluid "water" -Amount 250 -Duration 100 -InputCount 4
    }
    foreach ($input in @("primary", "primary_roasted", "primary_sulfuric", "primary_nosulfuric", "primary_solvent", "primary_nosolvent", "primary_rad", "primary_norad")) {
        Write-CrystallizerRecipe -Name "$input`_hydrogen" -InputGrade $input -OutputGrade "primary_first" -OreType $oreType -Fluid "hydrogen" -Amount 250 -Duration 200
        Write-CrystallizerRecipe -Name "$input`_chlorine" -InputGrade $input -OutputGrade "primary_second" -OreType $oreType -Fluid "chlorine" -Amount 250 -Duration 200
    }
    Write-CrystallizerRecipe -Name "crumbs_slop" -InputGrade "crumbs" -OutputGrade "base" -OreType $oreType -Fluid "slop" -Amount 1000 -Duration 200 -InputCount 64

    $primary = @($outputs[$oreType].primary)
    $sulfuric = @($outputs[$oreType].sulfuric)
    $solvent = @($outputs[$oreType].solvent)
    $rad = @($outputs[$oreType].rad)

    Write-CentrifugeRecipe -Name "base" -InputGrade "base" -OreType $oreType -Results @((New-BedrockStack "primary" $oreType), [ordered] @{ id = "minecraft:gravel" })
    Write-CentrifugeRecipe -Name "base_roasted" -InputGrade "base_roasted" -OreType $oreType -Results @((New-BedrockStack "primary" $oreType), [ordered] @{ id = "minecraft:gravel" })
    Write-CentrifugeRecipe -Name "base_washed" -InputGrade "base_washed" -OreType $oreType -Results @((New-BedrockStack "primary" $oreType), (New-BedrockStack "primary" $oreType), [ordered] @{ id = "minecraft:gravel" })
    Write-CentrifugeRecipe -Name "primary_sulfuric" -InputGrade "primary_sulfuric" -OreType $oreType -Results @((New-BedrockStack "primary_nosulfuric" $oreType 2), (New-BedrockStack "sulfuric_byproduct" $oreType 2))
    Write-CentrifugeRecipe -Name "primary_solvent" -InputGrade "primary_solvent" -OreType $oreType -Results @((New-BedrockStack "primary_nosolvent" $oreType 2), (New-BedrockStack "sulfuric_byproduct" $oreType 2), (New-BedrockStack "solvent_byproduct" $oreType 2))
    Write-CentrifugeRecipe -Name "primary_rad" -InputGrade "primary_rad" -OreType $oreType -Results @((New-BedrockStack "primary_norad" $oreType 2), (New-BedrockStack "sulfuric_byproduct" $oreType 2), (New-BedrockStack "solvent_byproduct" $oreType 2), (New-BedrockStack "rad_byproduct" $oreType 2))
    Write-CentrifugeRecipe -Name "primary" -InputGrade "primary" -OreType $oreType -Results @((New-FragmentStack $primary[0]), (New-FragmentStack $primary[1]))
    Write-CentrifugeRecipe -Name "primary_roasted" -InputGrade "primary_roasted" -OreType $oreType -Results @((New-FragmentStack $primary[0]), (New-FragmentStack $primary[1]))
    foreach ($input in @("primary_nosulfuric", "primary_nosolvent", "primary_norad")) {
        Write-CentrifugeRecipe -Name $input -InputGrade $input -OreType $oreType -Results @((New-FragmentStack $primary[0]), (New-FragmentStack $primary[1]), (New-BedrockStack "crumbs" $oreType))
    }
    Write-CentrifugeRecipe -Name "primary_first" -InputGrade "primary_first" -OreType $oreType -Results @((New-FragmentStack $primary[0]), (New-FragmentStack $primary[0]), (New-FragmentStack $primary[1]), (New-BedrockStack "crumbs" $oreType))
    Write-CentrifugeRecipe -Name "primary_second" -InputGrade "primary_second" -OreType $oreType -Results @((New-FragmentStack $primary[0]), (New-FragmentStack $primary[1]), (New-FragmentStack $primary[1]), (New-BedrockStack "crumbs" $oreType))
    foreach ($path in @(
            @{ name = "sulfuric_washed"; material = $sulfuric },
            @{ name = "solvent_washed"; material = $solvent },
            @{ name = "rad_washed"; material = $rad }
        )) {
        $results = @($path.material | ForEach-Object { New-FragmentStack $_ })
        $results += New-BedrockStack "crumbs" $oreType
        Write-CentrifugeRecipe -Name $path.name -InputGrade $path.name -OreType $oreType -Results $results
    }
}

$crystallizerRecipes = @(Get-ChildItem $crystallizerRoot -Filter "bedrock_*.json" -File)
$centrifugeRecipes = @(Get-ChildItem $centrifugeRoot -Filter "bedrock_*.json" -File)
if ($crystallizerRecipes.Count -ne 222) {
    throw "Expected 222 generic bedrock crystallizer recipes, found $($crystallizerRecipes.Count)."
}
if ($centrifugeRecipes.Count -ne 96) {
    throw "Expected 96 generic bedrock centrifuge recipes, found $($centrifugeRecipes.Count)."
}

foreach ($file in $centrifugeRecipes) {
    $recipe = Get-Content $file.FullName -Raw | ConvertFrom-Json
    foreach ($result in $recipe.results) {
        if ($result.id -eq "$modId`:bedrock_ore_fragment" -and $null -eq $result.count) {
            throw "Missing fragment count in $($file.Name)."
        }
    }
}

Write-Host "Generated 222 crystallizer and 96 centrifuge generic bedrock ore recipes."
