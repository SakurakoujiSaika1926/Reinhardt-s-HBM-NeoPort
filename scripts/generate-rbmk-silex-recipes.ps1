param(
    [string]$ProjectRoot = (Split-Path -Parent $PSScriptRoot)
)

$ErrorActionPreference = 'Stop'
$outputDirectory = Join-Path $ProjectRoot 'src/main/resources/data/reinhardtshbm/recipe/silex'
New-Item -ItemType Directory -Force -Path $outputDirectory | Out-Null

function ItemOutput([string]$id, [int]$weight) {
    return [ordered]@{
        item = [ordered]@{ id = $id }
        weight = $weight
    }
}

function HbmOutput([string]$id, [int]$weight) {
    return ItemOutput ("reinhardtshbm:$id") $weight
}

function WasteOutput([string]$family, [int]$wasteClass, [int]$weight) {
    return [ordered]@{
        item = [ordered]@{
            id = "reinhardtshbm:nuclear_waste_${family}_tiny"
            components = [ordered]@{
                'minecraft:custom_data' = [ordered]@{ waste_class = $wasteClass }
            }
        }
        weight = $weight
    }
}

function Write-SilexRecipe(
    [string]$pellet,
    [int]$state,
    [int]$fluidProduced,
    [string]$wavelength,
    [object[]]$outputs
) {
    $name = "rbmk_pellet_${pellet}_${state}"
    $recipe = [ordered]@{
        type = 'reinhardtshbm:silex'
        group = "silex.$name"
        ingredient = [ordered]@{
            type = 'neoforge:components'
            items = @("reinhardtshbm:rbmk_pellet_$pellet")
            components = [ordered]@{ 'minecraft:custom_model_data' = $state }
        }
        fluid_produced = $fluidProduced
        fluid_consumed = 100
        wavelength = $wavelength
        outputs = $outputs
    }
    $json = $recipe | ConvertTo-Json -Depth 12
    $path = Join-Path $outputDirectory "$name.json"
    [System.IO.File]::WriteAllText($path, $json + [Environment]::NewLine, [System.Text.UTF8Encoding]::new($false))
}

for ($i = 0; $i -lt 5; $i++) {
    Write-SilexRecipe 'ueu' $i 600 'ir' @(
        (HbmOutput 'nugget_uranium' (86 - $i * 11)),
        (HbmOutput $(if ($i -lt 2) { 'nugget_pu239' } else { 'nugget_pu_mix' }) (10 + $i * 3)),
        (WasteOutput 'long' 0 (2 + 3 * $i)),
        (WasteOutput 'short' 0 (2 + 5 * $i))
    )
    Write-SilexRecipe 'ueu' ($i + 5) 600 'ir' @(
        (HbmOutput 'powder_xe135_tiny' 1), (HbmOutput 'nugget_uranium' (86 - $i * 11)),
        (HbmOutput $(if ($i -lt 2) { 'nugget_pu239' } else { 'nugget_pu_mix' }) (10 + $i * 3)),
        (WasteOutput 'long' 0 (2 + 3 * $i)), (WasteOutput 'short' 0 (1 + 5 * $i))
    )

    Write-SilexRecipe 'meu' $i 600 'ir' @(
        (HbmOutput 'nugget_uranium_fuel' (84 - $i * 16)),
        (HbmOutput $(if ($i -lt 1) { 'nugget_pu239' } else { 'nugget_pu_mix' }) (6 + $i * 4)),
        (WasteOutput 'long' 0 (4 + 5 * $i)), (WasteOutput 'short' 0 (6 + 7 * $i))
    )
    Write-SilexRecipe 'meu' ($i + 5) 600 'ir' @(
        (HbmOutput 'powder_xe135_tiny' 1), (HbmOutput 'nugget_uranium_fuel' (83 - $i * 16)),
        (HbmOutput $(if ($i -lt 1) { 'nugget_pu239' } else { 'nugget_pu_mix' }) (6 + $i * 4)),
        (WasteOutput 'long' 0 (4 + 5 * $i)), (WasteOutput 'short' 0 (6 + 7 * $i))
    )

    foreach ($fuel in @(
        @{ Id='heu233'; Nugget='nugget_u233'; Waste=1 },
        @{ Id='heu235'; Nugget='nugget_u235'; Waste=0 }
    )) {
        Write-SilexRecipe $fuel.Id $i 600 'ir' @(
            (HbmOutput $fuel.Nugget (90 - $i * 20)),
            (WasteOutput 'long' $fuel.Waste (4 + 8 * $i)), (WasteOutput 'short' $fuel.Waste (6 + 12 * $i))
        )
        Write-SilexRecipe $fuel.Id ($i + 5) 600 'ir' @(
            (HbmOutput 'powder_xe135_tiny' 1), (HbmOutput $fuel.Nugget (89 - $i * 20)),
            (WasteOutput 'long' $fuel.Waste (4 + 8 * $i)), (WasteOutput 'short' $fuel.Waste (6 + 12 * $i))
        )
    }

    Write-SilexRecipe 'uzh' $i 600 'ir' @(
        (HbmOutput 'nugget_zirconium' 75), (HbmOutput 'nugget_uranium_fuel' (20 - $i * 4)),
        (HbmOutput 'nugget_pu_mix' (3 + $i * 3)), (WasteOutput 'long' 0 (1 + $i)), (WasteOutput 'short' 0 (1 + $i))
    )
    Write-SilexRecipe 'uzh' ($i + 5) 600 'ir' @(
        (HbmOutput 'powder_xe135_tiny' 1), (HbmOutput 'nugget_zirconium' 75),
        (HbmOutput 'nugget_uranium_fuel' (19 - $i * 4)), (HbmOutput 'nugget_pu_mix' (3 + $i * 3)),
        (WasteOutput 'long' 0 (1 + $i)), (WasteOutput 'short' 0 (1 + $i))
    )

    Write-SilexRecipe 'thmeu' $i 600 'ir' @(
        (HbmOutput 'nugget_thorium_fuel' (84 - $i * 20)), (HbmOutput 'nugget_u233' (6 + $i * 4)),
        (WasteOutput 'long' 3 (10 + 16 * $i))
    )
    Write-SilexRecipe 'thmeu' ($i + 5) 600 'ir' @(
        (HbmOutput 'powder_xe135_tiny' 1), (HbmOutput 'nugget_thorium_fuel' (83 - $i * 20)),
        (HbmOutput 'nugget_u233' (6 + $i * 4)), (WasteOutput 'long' 3 (10 + 16 * $i))
    )

    Write-SilexRecipe 'lep' $i 600 'ir' @(
        (HbmOutput 'nugget_plutonium_fuel' (84 - $i * 14)),
        (HbmOutput $(if ($i -lt 1) { 'nugget_pu239' } else { 'nugget_pu_mix' }) (6 + $i * 2)),
        (WasteOutput 'short' 3 (7 + 8 * $i)), (WasteOutput 'short' 4 (3 + 4 * $i))
    )
    Write-SilexRecipe 'lep' ($i + 5) 600 'ir' @(
        (HbmOutput 'powder_xe135_tiny' 1), (HbmOutput 'nugget_plutonium_fuel' (83 - $i * 14)),
        (HbmOutput $(if ($i -lt 1) { 'nugget_pu239' } else { 'nugget_pu_mix' }) (6 + $i * 2)),
        (WasteOutput 'short' 3 (7 + 8 * $i)), (WasteOutput 'short' 4 (3 + 4 * $i))
    )

    Write-SilexRecipe 'mep' $i 600 'ir' @(
        (HbmOutput 'nugget_pu_mix' (85 - $i * 20)), (WasteOutput 'short' 3 (10 + 10 * $i)),
        (WasteOutput 'short' 4 (5 + 5 * $i))
    )
    Write-SilexRecipe 'mep' ($i + 5) 600 'ir' @(
        (HbmOutput 'powder_xe135_tiny' 1), (HbmOutput 'nugget_pu_mix' (84 - $i * 20)),
        (WasteOutput 'short' 3 (10 + 10 * $i)), (WasteOutput 'short' 4 (5 + 5 * $i))
    )

    foreach ($fuel in @(
        @{ Id='hep239'; Nugget='nugget_pu239'; Waste=3; Wave='ir' },
        @{ Id='hep241'; Nugget='nugget_pu241'; Waste=5; Wave='visible' }
    )) {
        Write-SilexRecipe $fuel.Id $i 600 $fuel.Wave @(
            (HbmOutput $fuel.Nugget (85 - $i * 20)), (WasteOutput 'short' $fuel.Waste (15 + 20 * $i))
        )
        Write-SilexRecipe $fuel.Id ($i + 5) 600 $fuel.Wave @(
            (HbmOutput 'powder_xe135_tiny' 1), (HbmOutput $fuel.Nugget (84 - $i * 20)),
            (WasteOutput 'short' $fuel.Waste (15 + 20 * $i))
        )
    }

    Write-SilexRecipe 'men' $i 600 'ir' @(
        (HbmOutput 'nugget_neptunium_fuel' (84 - $i * 14)),
        (HbmOutput $(if ($i -lt 1) { 'nugget_pu239' } else { 'nugget_pu_mix' }) (6 + $i * 2)),
        (WasteOutput 'long' 2 (4 + 5 * $i)), (WasteOutput 'short' 2 (6 + 7 * $i))
    )
    Write-SilexRecipe 'men' ($i + 5) 600 'ir' @(
        (HbmOutput 'powder_xe135_tiny' 1), (HbmOutput 'nugget_neptunium_fuel' (83 - $i * 14)),
        (HbmOutput $(if ($i -lt 1) { 'nugget_pu239' } else { 'nugget_pu_mix' }) (6 + $i * 2)),
        (WasteOutput 'long' 2 (4 + 5 * $i)), (WasteOutput 'short' 2 (6 + 7 * $i))
    )

    Write-SilexRecipe 'hen' $i 600 'ir' @(
        (HbmOutput 'nugget_neptunium' (90 - $i * 20)), (WasteOutput 'long' 2 (4 + 8 * $i)),
        (WasteOutput 'short' 2 (6 + 12 * $i))
    )
    Write-SilexRecipe 'hen' ($i + 5) 600 'ir' @(
        (HbmOutput 'powder_xe135_tiny' 1), (HbmOutput 'nugget_neptunium' (89 - $i * 20)),
        (WasteOutput 'long' 2 (4 + 8 * $i)), (WasteOutput 'short' 2 (6 + 12 * $i))
    )

    Write-SilexRecipe 'mox' $i 600 'ir' @(
        (HbmOutput 'nugget_mox_fuel' (84 - $i * 20)), (HbmOutput 'nugget_pu_mix' (6 + $i * 4)),
        (WasteOutput 'long' 0 (2 + 3 * $i)), (WasteOutput 'short' 0 (3 + 5 * $i)),
        (WasteOutput 'short' 3 (5 + 8 * $i))
    )
    Write-SilexRecipe 'mox' ($i + 5) 600 'ir' @(
        (HbmOutput 'powder_xe135_tiny' 1), (HbmOutput 'nugget_mox_fuel' (83 - $i * 20)),
        (HbmOutput 'nugget_pu_mix' (6 + $i * 4)), (WasteOutput 'long' 0 (2 + 3 * $i)),
        (WasteOutput 'short' 0 (3 + 5 * $i)), (WasteOutput 'short' 3 (5 + 8 * $i))
    )

    Write-SilexRecipe 'leaus' $i 600 'visible' @(
        (HbmOutput 'nugget_australium_lesser' (90 - $i * 20)), (HbmOutput 'nugget_lead' (6 + 12 * $i)),
        (HbmOutput 'nugget_pb209' (4 + 8 * $i))
    )
    Write-SilexRecipe 'leaus' ($i + 5) 600 'visible' @(
        (HbmOutput 'powder_xe135_tiny' 1), (HbmOutput 'nugget_australium_lesser' (89 - $i * 20)),
        (HbmOutput 'nugget_lead' (6 + 12 * $i)), (HbmOutput 'nugget_pb209' (4 + 8 * $i))
    )

    Write-SilexRecipe 'heaus' $i 600 'visible' @(
        (HbmOutput 'nugget_australium_greater' (90 - $i * 20)), (HbmOutput 'nugget_au198' (5 + 10 * $i)),
        (ItemOutput 'minecraft:gold_nugget' (3 + 6 * $i)), (HbmOutput 'nugget_pb209' (2 + 4 * $i))
    )
    Write-SilexRecipe 'heaus' ($i + 5) 600 'visible' @(
        (HbmOutput 'powder_xe135_tiny' 1), (HbmOutput 'nugget_australium_greater' (89 - $i * 20)),
        (HbmOutput 'nugget_au198' (5 + 10 * $i)), (ItemOutput 'minecraft:gold_nugget' (3 + 6 * $i)),
        (HbmOutput 'nugget_pb209' (2 + 4 * $i))
    )

    foreach ($fuel in @(
        @{ Id='les'; Nugget='nugget_les'; LongNpBase=2; LongNpStep=3; ShortNpBase=2; ShortNpStep=5; LongSBase=1; LongSStep=2; ShortSBase=1; ShortSStep=2; CoalBase=4; CoalStep=8 },
        @{ Id='mes'; Nugget='nugget_schrabidium_fuel'; LongNpBase=1; LongNpStep=3; ShortNpBase=2; ShortNpStep=4; LongSBase=1; LongSStep=3; ShortSBase=2; ShortSStep=4; CoalBase=4; CoalStep=6 },
        @{ Id='hes'; Nugget='nugget_hes'; LongNpBase=1; LongNpStep=2; ShortNpBase=1; ShortNpStep=3; LongSBase=2; LongSStep=5; ShortSBase=4; ShortSStep=6; CoalBase=2; CoalStep=4 }
    )) {
        $outputs = @(
            (HbmOutput $fuel.Nugget (90 - $i * 20)),
            (WasteOutput 'long' 2 ($fuel.LongNpBase + $fuel.LongNpStep * $i)),
            (WasteOutput 'short' 2 ($fuel.ShortNpBase + $fuel.ShortNpStep * $i)),
            (WasteOutput 'long' 4 ($fuel.LongSBase + $fuel.LongSStep * $i)),
            (WasteOutput 'short' 7 ($fuel.ShortSBase + $fuel.ShortSStep * $i)),
            (HbmOutput 'powder_coal_tiny' ($fuel.CoalBase + $fuel.CoalStep * $i))
        )
        Write-SilexRecipe $fuel.Id $i 600 'visible' $outputs
        # 1.7.10 deliberately omitted xenon from LES/MES/HES to keep six outputs.
        Write-SilexRecipe $fuel.Id ($i + 5) 600 'visible' $outputs
    }

    Write-SilexRecipe 'balefire' $i 400 'uv' @(
        (HbmOutput 'powder_balefire' (90 - $i * 20)), (HbmOutput 'nuclear_waste_tiny' (10 + 20 * $i))
    )
    Write-SilexRecipe 'balefire_gold' $i 600 'visible' @(
        (HbmOutput 'nugget_au198' (90 - 20 * $i)), (HbmOutput 'powder_balefire' (10 + 20 * $i))
    )
    Write-SilexRecipe 'flashlead' $i 600 'visible' @(
        (HbmOutput 'nugget_au198' (44 - 10 * $i)), (HbmOutput 'nugget_pb209' (44 - 10 * $i)),
        (HbmOutput 'nugget_bismuth' (1 + 6 * $i)), (HbmOutput 'nugget_mercury' (1 + 6 * $i)),
        (HbmOutput 'nugget_gh336' (10 + 8 * $i))
    )
    Write-SilexRecipe 'po210be' $i 600 'ir' @(
        (HbmOutput 'nugget_polonium' (45 - 10 * $i)), (HbmOutput 'nugget_beryllium' (45 - 10 * $i)),
        (HbmOutput 'nugget_lead' (5 + 10 * $i)), (HbmOutput 'powder_coal_tiny' (5 + 10 * $i))
    )
    Write-SilexRecipe 'pu238be' $i 600 'ir' @(
        (HbmOutput 'nugget_pu238' (45 - 10 * $i)), (HbmOutput 'nugget_beryllium' (45 - 10 * $i)),
        (HbmOutput 'nugget_lead' (3 + 5 * $i)), (HbmOutput 'nuclear_waste_tiny' (2 + 5 * $i)),
        (HbmOutput 'powder_coal_tiny' (5 + 10 * $i))
    )
    Write-SilexRecipe 'pu238be' ($i + 5) 600 'ir' @(
        (HbmOutput 'powder_xe135_tiny' 1), (HbmOutput 'nugget_pu238' (44 - 10 * $i)),
        (HbmOutput 'nugget_beryllium' (45 - 10 * $i)), (HbmOutput 'nugget_lead' (3 + 5 * $i)),
        (HbmOutput 'nuclear_waste_tiny' (2 + 5 * $i)), (HbmOutput 'powder_coal_tiny' (5 + 10 * $i))
    )
    Write-SilexRecipe 'ra226be' $i 600 'ir' @(
        (HbmOutput 'nugget_ra226' (45 - 10 * $i)), (HbmOutput 'nugget_beryllium' (45 - 10 * $i)),
        (HbmOutput 'nugget_lead' (3 + 5 * $i)), (HbmOutput 'nugget_polonium' (2 + 5 * $i)),
        (HbmOutput 'powder_coal_tiny' (5 + 10 * $i))
    )

    $undefined = @(); 1..6 | ForEach-Object { $undefined += HbmOutput 'undefined' 1 }
    Write-SilexRecipe 'drx' $i 600 'gamma' $undefined
    Write-SilexRecipe 'drx' ($i + 5) 600 'gamma' $undefined

    Write-SilexRecipe 'zfb_bismuth' $i 600 'visible' @(
        (HbmOutput 'nugget_uranium' (50 - $i * 10)), (HbmOutput 'nugget_pu241' (50 - $i * 10)),
        (HbmOutput 'nugget_bismuth' (50 + $i * 20)), (HbmOutput 'nugget_zirconium' 150)
    )
    Write-SilexRecipe 'zfb_bismuth' ($i + 5) 600 'visible' @(
        (HbmOutput 'powder_xe135_tiny' 3), (HbmOutput 'nugget_uranium' (50 - $i * 10)),
        (HbmOutput 'nugget_pu241' (50 - $i * 10)), (HbmOutput 'nugget_bismuth' (50 + $i * 20)),
        (HbmOutput 'nugget_zirconium' 147)
    )
    Write-SilexRecipe 'zfb_pu241' $i 600 'visible' @(
        (HbmOutput 'nugget_u235' (50 - $i * 10)), (HbmOutput 'nugget_pu240' (50 - $i * 10)),
        (HbmOutput 'nugget_pu241' (50 + $i * 20)), (HbmOutput 'nugget_zirconium' 150)
    )
    Write-SilexRecipe 'zfb_pu241' ($i + 5) 600 'visible' @(
        (HbmOutput 'powder_xe135_tiny' 3), (HbmOutput 'nugget_u235' (50 - $i * 10)),
        (HbmOutput 'nugget_pu240' (50 - $i * 10)), (HbmOutput 'nugget_pu241' (50 + $i * 20)),
        (HbmOutput 'nugget_zirconium' 147)
    )
    Write-SilexRecipe 'zfb_am_mix' $i 600 'visible' @(
        (HbmOutput 'nugget_pu241' (100 - $i * 20)), (HbmOutput 'nugget_am_mix' (50 + $i * 20)),
        (HbmOutput 'nugget_zirconium' 150)
    )
    Write-SilexRecipe 'zfb_am_mix' ($i + 5) 600 'visible' @(
        (HbmOutput 'powder_xe135_tiny' 3), (HbmOutput 'nugget_pu241' (100 - $i * 20)),
        (HbmOutput 'nugget_am_mix' (50 + $i * 20)), (HbmOutput 'nugget_zirconium' 147)
    )
}

$generated = Get-ChildItem -LiteralPath $outputDirectory -Filter 'rbmk_pellet_*.json'
if ($generated.Count -ne 255) {
    throw "Expected exactly 255 RBMK pellet SILEX recipes, generated $($generated.Count)."
}
Write-Output "Generated $($generated.Count) exact 1.7.10 RBMK pellet SILEX recipes."
