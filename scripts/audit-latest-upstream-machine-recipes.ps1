param(
    [string]$RecipeRoot = "src/main/resources/data/reinhardtshbm/recipe",
    [string]$OutputReport = "build/reports/latest-upstream-machine-recipes.txt",
    [switch]$FailOnMismatch
)

$ErrorActionPreference = "Stop"
$referenceCommit = "e674541d85f47fd267d6d99b7dc1f0595e031c05"
$problems = [System.Collections.Generic.List[string]]::new()
$checks = 0

function Add-Problem([string]$Message) {
    $script:problems.Add($Message)
}

function Assert-SourceContract([string]$RelativePath, [string]$Label, [string]$Pattern) {
    if (-not (Test-Path -LiteralPath $RelativePath -PathType Leaf)) {
        Add-Problem "Missing source for $Label`: $RelativePath"
        return
    }
    $source = Get-Content -LiteralPath $RelativePath -Raw
    $script:checks++
    if ($source -notmatch $Pattern) {
        Add-Problem "$Label is missing from $RelativePath"
    }
}

function Assert-Equal([string]$Label, [object]$Actual, [object]$Expected) {
    $script:checks++
    if ([string]$Actual -cne [string]$Expected) {
        Add-Problem "$Label is '$Actual', expected '$Expected'"
    }
}

function Assert-Sequence([string]$Label, [object[]]$Actual, [object[]]$Expected) {
    $script:checks++
    $actualText = @($Actual) -join "|"
    $expectedText = @($Expected) -join "|"
    if ($actualText -cne $expectedText) {
        Add-Problem "$Label is '$actualText', expected '$expectedText'"
    }
}

function Read-Recipe([string]$RelativePath) {
    $path = Join-Path $RecipeRoot $RelativePath
    if (-not (Test-Path -LiteralPath $path -PathType Leaf)) {
        Add-Problem "Missing latest-upstream recipe $RelativePath"
        return $null
    }
    try {
        return Get-Content -LiteralPath $path -Raw | ConvertFrom-Json
    } catch {
        Add-Problem "Invalid JSON in ${RelativePath}: $($_.Exception.Message)"
        return $null
    }
}

function Ingredient-Key([object]$Entry) {
    if ($null -eq $Entry) { return "<null>" }
    $ingredient = if ($null -ne $Entry.PSObject.Properties['ingredient']) { $Entry.ingredient } else { $Entry }
    $count = if ($null -ne $Entry.PSObject.Properties['count']) { [int]$Entry.count } else { 1 }
    if ($ingredient.item -is [string]) { return "item:$($ingredient.item)x$count" }
    if ($ingredient.tag -is [string]) { return "tag:$($ingredient.tag)x$count" }
    if ($ingredient.type -eq 'neoforge:components') {
        $items = @($ingredient.items) -join ','
        $variant = $ingredient.components.'minecraft:custom_data'.variant
        return "components:$items`:$variant`x$count"
    }
    return "unknown:$($ingredient | ConvertTo-Json -Depth 8 -Compress)x$count"
}

function Stack-Key([object]$Stack) {
    if ($null -eq $Stack) { return "<null>" }
    $count = if ($null -ne $Stack.PSObject.Properties['count']) { [int]$Stack.count } else { 1 }
    return "$($Stack.id)x$count"
}

function Fluid-Key([object]$Fluid) {
    if ($null -eq $Fluid) { return "<null>" }
    $pressure = if ($null -ne $Fluid.PSObject.Properties['pressure']) { [int]$Fluid.pressure } else { 0 }
    return "$($Fluid.fluid)x$([int]$Fluid.amount)@p$pressure"
}

function Ingredient-Keys([object]$Entries) {
    return @($Entries | ForEach-Object { Ingredient-Key $_ })
}

function Stack-Keys([object]$Entries) {
    return @($Entries | ForEach-Object { Stack-Key $_ })
}

function Fluid-Keys([object]$Entries) {
    return @($Entries | ForEach-Object { Fluid-Key $_ })
}

function Assert-FileSet([string]$Directory, [string[]]$ExpectedNames) {
    $path = Join-Path $RecipeRoot $Directory
    if (-not (Test-Path -LiteralPath $path -PathType Container)) {
        Add-Problem "Missing latest-upstream recipe directory $Directory"
        return
    }
    $actual = @(Get-ChildItem -LiteralPath $path -Filter '*.json' -File | Select-Object -ExpandProperty Name | Sort-Object)
    Assert-Sequence "$Directory recipe files" $actual @($ExpectedNames | Sort-Object)
}

function Assert-ShapedRecipe(
    [string]$RelativePath,
    [string[]]$Pattern,
    [hashtable]$Keys,
    [string]$ResultId,
    [int]$ResultCount = 1
) {
    $recipe = Read-Recipe $RelativePath
    if ($null -eq $recipe) { return }
    Assert-Equal "$RelativePath type" $recipe.type 'minecraft:crafting_shaped'
    Assert-Sequence "$RelativePath pattern" @($recipe.pattern) $Pattern
    Assert-Sequence "$RelativePath key names" @($recipe.key.PSObject.Properties.Name | Sort-Object) @($Keys.Keys | Sort-Object)
    foreach ($key in $Keys.Keys) {
        Assert-Equal "$RelativePath key $key" (Ingredient-Key $recipe.key.$key) $Keys[$key]
    }
    Assert-Equal "$RelativePath result" (Stack-Key $recipe.result) "${ResultId}x${ResultCount}"
}

function Assert-AssemblyRoute(
    [string]$File,
    [string]$Group,
    [int]$Duration,
    [long]$Power,
    [string]$Result,
    [string[]]$Ingredients = @()
) {
    $relative = "assembly_machine/$File"
    $recipe = Read-Recipe $relative
    if ($null -eq $recipe) { return }
    Assert-Equal "$relative type" $recipe.type 'reinhardtshbm:assembly_machine'
    Assert-Equal "$relative group" $recipe.group $Group
    Assert-Equal "$relative duration" $recipe.duration $Duration
    Assert-Equal "$relative power" $recipe.power $Power
    Assert-Equal "$relative result" (Stack-Key $recipe.result) $Result
    if ($Ingredients.Count -gt 0) {
        Assert-Sequence "$relative ingredients" (Ingredient-Keys $recipe.ingredients) $Ingredients
    }
}

function Assert-UnconditionalRecipe([string]$RelativePath) {
    $recipe = Read-Recipe $RelativePath
    if ($null -eq $recipe) { return }
    $script:checks++
    if ($null -ne $recipe.PSObject.Properties['neoforge:conditions']) {
        Add-Problem "$RelativePath is condition-gated, but latest upstream registers it unconditionally"
    }
}

function Assert-FluidPackRecipe([string]$FluidName) {
    $packRelative = "assembly_machine/fluid_pack_$FluidName.json"
    $pack = Read-Recipe $packRelative
    if ($null -ne $pack) {
        Assert-Equal "$packRelative type" $pack.type 'reinhardtshbm:assembly_machine'
        Assert-Equal "$packRelative group" $pack.group "ass.packagehbmfluid.$FluidName"
        Assert-Equal "$packRelative duration" $pack.duration 40
        Assert-Equal "$packRelative power" $pack.power 100
        Assert-Sequence "$packRelative ingredients" (Ingredient-Keys $pack.ingredients) @('item:reinhardtshbm:fluid_pack_emptyx1')
        Assert-Sequence "$packRelative input fluids" (Fluid-Keys $pack.input_fluids) @("${FluidName}x32000@p0")
        Assert-Equal "$packRelative result" (Stack-Key $pack.result) 'reinhardtshbm:fluid_pack_fullx1'
        Assert-Equal "$packRelative result fluid" $pack.result.components.'minecraft:custom_data'.fluid $FluidName
    }

    $unpackRelative = "assembly_machine/fluid_unpack_$FluidName.json"
    $unpack = Read-Recipe $unpackRelative
    if ($null -ne $unpack) {
        Assert-Equal "$unpackRelative type" $unpack.type 'reinhardtshbm:assembly_machine'
        Assert-Equal "$unpackRelative group" $unpack.group "ass.unpackagehbmfluid.$FluidName"
        Assert-Equal "$unpackRelative duration" $unpack.duration 40
        Assert-Equal "$unpackRelative power" $unpack.power 100
        Assert-Equal "$unpackRelative ingredient count" $unpack.ingredients[0].count 1
        Assert-Equal "$unpackRelative ingredient type" $unpack.ingredients[0].ingredient.type 'neoforge:components'
        Assert-Sequence "$unpackRelative ingredient items" @($unpack.ingredients[0].ingredient.items) @('reinhardtshbm:fluid_pack_full')
        Assert-Equal "$unpackRelative ingredient fluid" $unpack.ingredients[0].ingredient.components.'minecraft:custom_data'.fluid $FluidName
        Assert-Sequence "$unpackRelative output fluids" (Fluid-Keys $unpack.output_fluids) @("${FluidName}x32000@p0")
        Assert-Equal "$unpackRelative result" (Stack-Key $unpack.result) 'reinhardtshbm:fluid_pack_emptyx1'
    }
}

function Assert-GenericRecipe(
    [string]$RelativePath,
    [string]$Type,
    [string]$Group,
    [int]$Duration,
    [long]$Power,
    [string[]]$InputItems,
    [string[]]$InputFluids,
    [string[]]$OutputItems,
    [string[]]$OutputFluids
) {
    $recipe = Read-Recipe $RelativePath
    if ($null -eq $recipe) { return }
    Assert-Equal "$RelativePath type" $recipe.type $Type
    if ($Group) { Assert-Equal "$RelativePath group" $recipe.group $Group }
    Assert-Equal "$RelativePath duration" $recipe.duration $Duration
    Assert-Equal "$RelativePath power" $recipe.power $Power
    Assert-Sequence "$RelativePath input items" (Ingredient-Keys $recipe.input_items) $InputItems
    Assert-Sequence "$RelativePath input fluids" (Fluid-Keys $recipe.input_fluids) $InputFluids
    Assert-Sequence "$RelativePath output items" (Stack-Keys $recipe.output_items) $OutputItems
    Assert-Sequence "$RelativePath output fluids" (Fluid-Keys $recipe.output_fluids) $OutputFluids
}

# Exact latest-upstream machine/block manufacturing changes from CraftingManager.
Assert-ShapedRecipe 'red_pylon.json' @('CWC', 'PWP', ' S ') @{
    C = 'item:reinhardtshbm:coil_copperx1'; W = 'tag:minecraft:planksx1';
    P = 'tag:c:plates/polymerx1'; S = 'tag:c:cobblestonesx1'
} 'reinhardtshbm:red_pylon' 4
Assert-ShapedRecipe 'red_pylon_steel.json' @('CWC', 'PWP', ' S ') @{
    C = 'item:reinhardtshbm:coil_copperx1'; W = 'tag:c:pipes/steelx1';
    P = 'tag:c:plates/polymerx1'; S = 'tag:c:cobblestonesx1'
} 'reinhardtshbm:red_pylon_steel' 4
Assert-ShapedRecipe 'pile_loader.json' @(' A ', 'CBS') @{
    A = 'tag:c:plates/aluminiumx1'; C = 'tag:c:plates/cast_steelx1';
    B = 'tag:c:ingots/boronx1'; S = 'item:reinhardtshbm:shell_steelx1'
} 'reinhardtshbm:pile_loader'
Assert-ShapedRecipe 'pile_vent.json' @(' M ', 'ACA', ' S ') @{
    M = 'item:reinhardtshbm:motorx1'; A = 'tag:c:plates/aluminiumx1';
    C = 'item:reinhardtshbm:shell_copperx1'; S = 'tag:c:plates/cast_steelx1'
} 'reinhardtshbm:pile_vent'
Assert-ShapedRecipe 'pile_control.json' @(' B ', 'SBS', 'SBS') @{
    B = 'tag:c:ingots/boronx1'; S = 'tag:c:plates/steelx1'
} 'reinhardtshbm:pile_control'
Assert-ShapedRecipe 'fluid_pump.json' @('S', 'G', 'M') @{
    S = 'item:reinhardtshbm:shell_steelx1'; G = 'tag:c:ingots/graphitex1';
    M = 'item:reinhardtshbm:motorx1'
} 'reinhardtshbm:fluid_pump'
Assert-ShapedRecipe 'photo_panel.json' @(' G ', 'IPI', ' C ') @{
    G = 'tag:c:glass_panesx1'; I = 'tag:c:plates/polymerx1';
    P = 'item:reinhardtshbm:billet_siliconx1'; C = 'item:reinhardtshbm:circuit_pcbx1'
} 'reinhardtshbm:photo_panel'
Assert-ShapedRecipe 'machine_satlinker.json' @('PSP', 'SCS', 'PSP') @{
    P = 'tag:c:ingots/resistant_alloyx1'; S = 'tag:c:ingots/plasticx1';
    C = 'item:reinhardtshbm:sat_chipx1'
} 'reinhardtshbm:machine_satlinker'
Assert-ShapedRecipe 'machine_tape_drive.json' @('PPP', 'CCC', 'PPP') @{
    P = 'tag:c:ingots/plasticx1'; C = 'item:reinhardtshbm:circuit_pcbx1'
} 'reinhardtshbm:machine_tape_drive'
Assert-ShapedRecipe 'barrel_tcalloy.json' @('IPI', 'I I', 'IPI') @{
    I = 'tag:c:ingots/tcalloyx1'; P = 'tag:c:plates/titaniumx1'
} 'reinhardtshbm:barrel_tcalloy'
Assert-ShapedRecipe 'barrel_antimatter.json' @('IPI', 'I I', 'IPI') @{
    I = 'tag:c:plates/saturnitex1'; P = 'tag:c:wires_dense/bsccox1'
} 'reinhardtshbm:barrel_antimatter'
Assert-ShapedRecipe 'rtty_designator.json' @('R', 'C', 'T') @{
    R = 'item:reinhardtshbm:radio_torch_readerx1'; C = 'item:reinhardtshbm:circuit_analogx1';
    T = 'item:reinhardtshbm:rangefinderx1'
} 'reinhardtshbm:rtty_designator'

# New latest-upstream recipe families are exact sets, not merely non-empty directories.
Assert-FileSet 'rock_mill' @(
    'bauxite.json', 'clay.json', 'cobblestone.json', 'gneiss.json', 'gravel.json',
    'hematite.json', 'netherrack.json', 'sand.json', 'soul_sand.json'
)
Assert-FileSet 'supercomputer' @(
    'beigeprints.json', 'blueprints.json', 'copyflightcalc.json', 'copyfligthdata.json',
    'copyparticlecalc.json', 'flightcalc_helium.json', 'flightcalc_pfm.json',
    'flightcalc_water.json', 'klaus.json', 'particlecalc_helium.json',
    'particlecalc_pfm.json', 'particlecalc_water.json', 'processflight_helium.json',
    'processflight_pfm.json', 'processflight_water.json', 'processorbit_helium.json',
    'processorbit_pfm.json', 'processorbit_water.json'
)
Assert-FileSet 'space_assembler' @('crystal_circuit.json')

# Assembly-machine additions and all changed upstream result/timing identities.
$assemblyRoutes = @(
    @('drive_flash_empty.json', 'ass.flashdrive', 100, 250, 'reinhardtshbm:drive_flash_emptyx1'),
    @('drive_disk_empty.json', 'ass.diskdrive', 200, 250, 'reinhardtshbm:drive_disk_emptyx1'),
    @('pile_rod_ra226be.json', 'autoswitch.pilerod', 40, 200, 'reinhardtshbm:pile_rod_ra226bex1'),
    @('pile_rod_po210be.json', 'autoswitch.pilerod', 40, 200, 'reinhardtshbm:pile_rod_po210bex1'),
    @('pile_rod_zirconium.json', 'autoswitch.pilerod', 40, 200, 'reinhardtshbm:pile_rod_zrx3'),
    @('pile_rod_natural_uranium.json', 'autoswitch.pilerod', 40, 200, 'reinhardtshbm:pile_rod_nux1'),
    @('pile_rod_thorium.json', 'autoswitch.pilerod', 40, 200, 'reinhardtshbm:pile_rod_thoriumx1'),
    @('cargo_door.json', 'ass.cargodoor', 200, 100, 'reinhardtshbm:cargo_doorx1'),
    @('machine_chemical_plant_alt.json', 'ass.chemplantAlt', 200, 100, 'reinhardtshbm:machine_chemical_plantx1'),
    @('machine_chemical_plant_alt_expensive.json', 'ass.chemplantAlt', 200, 100, 'reinhardtshbm:machine_chemical_plantx1'),
    @('machine_supercomputer.json', 'ass.sal9000', 400, 1000, 'reinhardtshbm:machine_supercomputerx1'),
    @('machine_supercomputer_expensive.json', 'ass.sal9000', 400, 1000, 'reinhardtshbm:machine_supercomputerx1'),
    @('pile_brick.json', 'ass.pileblock', 20, 250, 'reinhardtshbm:pile_brickx1'),
    @('pile_brick_expensive.json', 'ass.pileblock', 20, 250, 'reinhardtshbm:pile_brickx1'),
    @('mp_thruster_15_balefire.json', 'ass.mpt15bf', 200, 100, 'reinhardtshbm:mp_thruster_15_balefirex1'),
    @('mp_fuselage_10_long_kerosene.json', 'ass.mpf10kerolong', 100, 100, 'reinhardtshbm:mp_fuselage_10_long_kerosenex1'),
    @('mp_fuselage_10_long_solid.json', 'ass.mpf10solidlong', 100, 100, 'reinhardtshbm:mp_fuselage_10_long_solidx1'),
    @('launchpad_soyuz.json', 'ass.launchpadsoyuz', 1200, 5000, 'reinhardtshbm:launchpad_soyuzx1'),
    @('launchpad_soyuz_expensive.json', 'ass.launchpadsoyuz', 1200, 5000, 'reinhardtshbm:launchpad_soyuzx1'),
    @('launchpad_lambda.json', 'ass.launchpadlambda', 1200, 5000, 'reinhardtshbm:launchpad_lambdax1'),
    @('launchpad_lambda_expensive.json', 'ass.launchpadlambda', 1200, 5000, 'reinhardtshbm:launchpad_lambdax1'),
    @('part_generic_alclad.json', 'ass.alclad', 200, 50000, 'reinhardtshbm:part_generic_alcladx1'),
    @('part_generic_alclad_titanium.json', 'ass.titaniumconstruction', 600, 1000, 'reinhardtshbm:part_generic_alcladx1'),
    @('missile_soyuz.json', 'ass.soyuz', 6000, 25000, 'reinhardtshbm:missile_soyuzx1'),
    @('missile_soyuz_lander.json', 'ass.lander', 2400, 25000, 'reinhardtshbm:missile_soyuz_landerx1'),
    @('missile_lambda.json', 'ass.lambdarocket', 1200, 50000, 'reinhardtshbm:missile_lambdax1'),
    @('satellite_science.json', 'ass.spacelab', 1200, 25000, 'reinhardtshbm:satellite_sciencex1'),
    @('satellite_science_sensor.json', 'ass.sensorrelay', 600, 25000, 'reinhardtshbm:satellite_science_sensorx1'),
    @('satellite_science_sensor_expensive.json', 'ass.sensorrelay', 600, 25000, 'reinhardtshbm:satellite_science_sensorx1'),
    @('satellite_science_assembler.json', 'ass.0gassembler', 600, 25000, 'reinhardtshbm:satellite_science_assemblerx1'),
    @('satellite_spy.json', 'ass.spysat', 1200, 25000, 'reinhardtshbm:satellite_spyx1'),
    @('satellite_scanner.json', 'ass.scansat', 1200, 25000, 'reinhardtshbm:satellite_scannerx1'),
    @('satellite_radar.json', 'ass.radarsat', 1200, 25000, 'reinhardtshbm:satellite_radarx1'),
    @('satellite_miner_astro.json', 'ass.astrominer', 1200, 25000, 'reinhardtshbm:satellite_miner_astrox1'),
    @('satellite_miner_lunar.json', 'ass.lunarminer', 1200, 25000, 'reinhardtshbm:satellite_miner_lunarx1'),
    @('satellite_precision_laser.json', 'ass.orbitallaser', 1200, 25000, 'reinhardtshbm:satellite_precision_laserx1'),
    @('satellite_death_ray.json', 'ass.deathray', 1200, 25000, 'reinhardtshbm:satellite_death_rayx1'),
    @('satellite_xenium_resonator.json', 'ass.xenrelay', 1200, 25000, 'reinhardtshbm:satellite_xenium_resonatorx1'),
    @('satellite_relay.json', 'ass.relaysat', 1200, 25000, 'reinhardtshbm:satellite_relayx1'),
    @('satellite_detector.json', 'ass.detectorsat', 1200, 25000, 'reinhardtshbm:satellite_detectorx1'),
    @('satellite_ray_scan.json', 'ass.rayscansat', 1200, 25000, 'reinhardtshbm:satellite_ray_scanx1'),
    @('machine_satlink.json', 'ass.satlink', 100, 1000, 'reinhardtshbm:machine_satlinkx1')
)
foreach ($route in $assemblyRoutes) {
    Assert-AssemblyRoute $route[0] $route[1] ([int]$route[2]) ([long]$route[3]) $route[4]
}

# Exact ingredient changes which a count-only check cannot catch.
Assert-AssemblyRoute 'machine_assembly_machine.json' 'ass.assembler' 200 100 'reinhardtshbm:machine_assembly_machinex1' @(
    'tag:c:ingots/steelx4', 'tag:c:plates/copperx4', 'item:reinhardtshbm:motorx2', 'item:reinhardtshbm:circuit_basicx1'
)
Assert-AssemblyRoute 'machine_pyrooven.json' 'ass.pyrooven' 300 100 'reinhardtshbm:machine_pyroovenx1' @(
    'item:reinhardtshbm:plate_welded_steelx16', 'tag:c:ingots/hard_plasticx24', 'item:reinhardtshbm:pipe_copperx12',
    'item:reinhardtshbm:motor_deshx1', 'item:reinhardtshbm:circuit_bismoidx1'
)
Assert-AssemblyRoute 'machine_turbinegas.json' 'ass.gasturbine' 400 100 'reinhardtshbm:machine_turbinegasx1' @(
    'item:reinhardtshbm:shell_steelx10', 'item:reinhardtshbm:wire_dense_goldx12', 'item:reinhardtshbm:pipe_dura_steelx4',
    'item:reinhardtshbm:pipe_steelx4', 'item:reinhardtshbm:turbine_tungstenx1', 'tag:c:ingots/rubberx12',
    'item:reinhardtshbm:circuit_basicx3'
)
Assert-AssemblyRoute 'machine_shredder_expensive.json' 'ass.shredder' 100 100 'reinhardtshbm:machine_shredderx1' @(
    'item:reinhardtshbm:item_expensive_steel_platingx1', 'tag:c:plates/copperx4', 'item:reinhardtshbm:motorx2'
)
Assert-AssemblyRoute 'machine_chemical_plant_expensive.json' 'ass.chemplant' 200 100 'reinhardtshbm:machine_chemical_plantx1' @(
    'item:reinhardtshbm:item_expensive_steel_platingx3', 'tag:c:pipes/copperx2', 'item:reinhardtshbm:plate_polymerx16',
    'item:reinhardtshbm:motorx2', 'item:reinhardtshbm:coil_tungstenx2', 'item:reinhardtshbm:circuit_analogx3'
)
Assert-AssemblyRoute 'machine_combustion_engine_expensive.json' 'ass.combustiongen' 300 100 'reinhardtshbm:machine_combustion_enginex1' @(
    'item:reinhardtshbm:item_expensive_heavy_framex2', 'item:reinhardtshbm:wire_dense_goldx16',
    'item:reinhardtshbm:canister_emptyx4', 'item:reinhardtshbm:item_expensive_circuitx1'
)
Assert-AssemblyRoute 'machine_cyclotron_expensive.json' 'ass.cyclotron' 600 100 'reinhardtshbm:machine_cyclotronx1' @(
    'item:reinhardtshbm:item_expensive_ferro_platingx8', 'item:reinhardtshbm:wire_dense_neodymiumx32',
    'item:reinhardtshbm:plate_welded_aluminiumx16', 'tag:c:ingots/rubberx32',
    'item:reinhardtshbm:item_expensive_circuitx8'
)
Assert-AssemblyRoute 'machine_epress_expensive.json' 'ass.epress' 100 100 'reinhardtshbm:machine_epressx1' @(
    'item:reinhardtshbm:item_expensive_heavy_framex1', 'item:reinhardtshbm:part_piston_hydraulicx4',
    'item:reinhardtshbm:circuit_basicx4'
)
Assert-AssemblyRoute 'machine_exposure_chamber_expensive.json' 'ass.exposurechamber' 200 100 'reinhardtshbm:machine_exposure_chamberx1' @(
    'item:reinhardtshbm:item_expensive_lead_platingx8', 'tag:c:ingots/hard_plasticx24',
    'item:reinhardtshbm:wire_dense_goldx32', 'item:reinhardtshbm:circuit_bismoidx4',
    'item:reinhardtshbm:item_expensive_computerx2'
)
Assert-AssemblyRoute 'machine_turbinegas_expensive.json' 'ass.gasturbine' 400 100 'reinhardtshbm:machine_turbinegasx1' @(
    'item:reinhardtshbm:item_expensive_heavy_framex4', 'item:reinhardtshbm:wire_dense_goldx16',
    'item:reinhardtshbm:pipe_dura_steelx16', 'item:reinhardtshbm:turbine_tungstenx2',
    'item:reinhardtshbm:item_expensive_circuitx3'
)
Assert-AssemblyRoute 'machine_industrial_turbine_expensive.json' 'ass.iturbine' 200 100 'reinhardtshbm:machine_industrial_turbinex1' @(
    'item:reinhardtshbm:item_expensive_heavy_framex4', 'item:reinhardtshbm:turbine_titaniumx3',
    'item:reinhardtshbm:wire_dense_goldx16', 'item:reinhardtshbm:pipe_dura_steelx16',
    'item:reinhardtshbm:item_expensive_circuitx3'
)
Assert-AssemblyRoute 'machine_fluidtank_expensive.json' 'ass.tank' 200 100 'reinhardtshbm:machine_fluidtankx1' @(
    'item:reinhardtshbm:item_expensive_steel_platingx4', 'tag:c:tarsx16'
)
Assert-AssemblyRoute 'machine_precass_expensive.json' 'ass.precass' 1200 100 'reinhardtshbm:machine_precassx1' @(
    'item:reinhardtshbm:item_expensive_steel_platingx4', 'tag:c:ingots/zirconiumx8', 'item:reinhardtshbm:motorx4',
    'item:reinhardtshbm:circuit_capacitor_boardx8'
)
$unconditionalExpensiveParts = @(
    'item_expensive_steel_plating.json', 'item_expensive_heavy_frame.json', 'item_expensive_circuit.json',
    'item_expensive_lead_plating.json', 'item_expensive_ferro_plating.json', 'item_expensive_computer.json',
    'item_expensive_bronze_tubes.json', 'item_expensive_plastic.json', 'item_expensive_gold_dust.json'
)
foreach ($file in $unconditionalExpensiveParts) {
    Assert-UnconditionalRecipe "assembly_machine/$file"
}
$clusterPellet = Read-Recipe 'pellet_cluster.json'
if ($null -ne $clusterPellet) {
    Assert-Equal 'pellet_cluster.json type' $clusterPellet.type 'reinhardtshbm:assembly_machine'
    Assert-Equal 'pellet_cluster.json group' $clusterPellet.group 'ass.clusterpellets'
    Assert-Equal 'pellet_cluster.json duration' $clusterPellet.duration 50
    Assert-Equal 'pellet_cluster.json power' $clusterPellet.power 100
    Assert-Equal 'pellet_cluster.json result' (Stack-Key $clusterPellet.result) 'reinhardtshbm:pellet_clusterx1'
    Assert-Sequence 'pellet_cluster.json ingredients' (Ingredient-Keys $clusterPellet.ingredients) @(
        'tag:c:plates/steelx4', 'tag:c:ingots/high_explosivex1'
    )
}
$paExpensive = @{
    'pa_rfc_expensive.json' = @('item:reinhardtshbm:pa_beamlinex3', 'item:reinhardtshbm:item_expensive_lead_platingx4', 'tag:c:plates/copperx64', 'item:reinhardtshbm:item_expensive_plasticx4', 'item:reinhardtshbm:magnetronx16')
    'pa_quadrupole_expensive.json' = @('item:reinhardtshbm:pa_beamlinex1', 'item:reinhardtshbm:item_expensive_cmb_tubesx2', 'item:reinhardtshbm:item_expensive_plasticx4', 'item:reinhardtshbm:circuit_bismoidx4', 'item:reinhardtshbm:item_expensive_computerx1')
    'pa_dipole_expensive.json' = @('item:reinhardtshbm:pa_beamlinex2', 'item:reinhardtshbm:item_expensive_cmb_tubesx2', 'item:reinhardtshbm:item_expensive_plasticx4', 'item:reinhardtshbm:circuit_bismoidx16', 'item:reinhardtshbm:item_expensive_computerx1')
    'pa_source_expensive.json' = @('item:reinhardtshbm:pa_beamlinex3', 'item:reinhardtshbm:item_expensive_cmb_tubesx8', 'item:reinhardtshbm:item_expensive_plasticx8', 'item:reinhardtshbm:magnetronx16', 'item:reinhardtshbm:circuit_quantumx4', 'item:reinhardtshbm:item_expensive_computerx4')
    'pa_detector_expensive.json' = @('item:reinhardtshbm:pa_beamlinex3', 'item:reinhardtshbm:item_expensive_cmb_tubesx8', 'item:reinhardtshbm:wire_dense_goldx64', 'item:reinhardtshbm:item_expensive_plasticx8', 'item:reinhardtshbm:circuit_quantumx4', 'item:reinhardtshbm:item_expensive_computerx8')
}
foreach ($file in $paExpensive.Keys) {
    $recipe = Read-Recipe "assembly_machine/$file"
    if ($null -ne $recipe) { Assert-Sequence "assembly_machine/$file ingredients" (Ingredient-Keys $recipe.ingredients) $paExpensive[$file] }
}

# Weighted nitra output: one roll at completion with the exact upstream pool.
$nitra = Read-Recipe 'assembly_machine/nitra.json'
if ($null -ne $nitra) {
    Assert-Equal 'assembly_machine/nitra.json type' $nitra.type 'reinhardtshbm:assembly_machine'
    Assert-Equal 'assembly_machine/nitra.json group' $nitra.group 'ass.nitra'
    Assert-Equal 'assembly_machine/nitra.json duration' $nitra.duration 200
    Assert-Equal 'assembly_machine/nitra.json power' $nitra.power 500
    Assert-Sequence 'assembly_machine/nitra.json ingredients' (Ingredient-Keys $nitra.ingredients) @('item:reinhardtshbm:nitrax1')
    Assert-Sequence 'assembly_machine/nitra.json weighted outputs' @($nitra.outputs | ForEach-Object { "$(Stack-Key $_.result)@$([int]$_.weight)" }) @(
        'reinhardtshbm:casing_shotshellx1@20', 'reinhardtshbm:casing_buckshotx1@10',
        'reinhardtshbm:casing_smallx2@20', 'reinhardtshbm:casing_largex1@10', 'minecraft:gunpowderx1@30',
        'reinhardtshbm:corditex1@20', 'reinhardtshbm:ballistitex1@20',
        'reinhardtshbm:grenade_shell_fragx1@5', 'reinhardtshbm:grenade_shell_stickx1@5',
        'reinhardtshbm:grenade_fuze_s3x1@10'
    )
}

# Latest upstream registers one package and one unpackage recipe for every fluid which is not
# marked NO_CONTAINER. Derive the complete set from the port's authoritative fluid registry so a
# newly added fluid cannot silently omit either direction. This also covers lead-container and
# antimatter fluids; ordinary tank compatibility is deliberately not the fluid-pack filter.
$fluidDefinitionPath = 'src/main/java/com/reinhardt/hbm/fluid/HbmFluidDefinitions.java'
$fluidContainerPath = 'src/main/java/com/reinhardt/hbm/item/HbmFluidContainerItem.java'
$assemblyMachinePath = 'src/main/java/com/reinhardt/hbm/blockentity/AssemblyMachineBlockEntity.java'
if (-not (Test-Path -LiteralPath $fluidDefinitionPath -PathType Leaf)) {
    Add-Problem "Missing fluid definition table $fluidDefinitionPath"
} else {
    $eligibleFluidNames = [System.Collections.Generic.List[string]]::new()
    foreach ($line in Get-Content -LiteralPath $fluidDefinitionPath) {
        if ($line -match 'definition\(\s*[^,]+,\s*[^,]+,\s*"[^"]+",\s*"(?<name>[^"]+)",\s*(?:[^,]+,\s*){6}"(?<traits>[^"]*)"') {
            $name = $Matches.name
            $traits = $Matches.traits
            if ($name -ne 'none' -and $traits -notmatch '(^|\|)NOCON(?:\||$)') {
                $eligibleFluidNames.Add($name)
            }
        }
    }

    $expectedPackFiles = @($eligibleFluidNames | ForEach-Object { "fluid_pack_$_.json" } | Sort-Object)
    $expectedUnpackFiles = @($eligibleFluidNames | ForEach-Object { "fluid_unpack_$_.json" } | Sort-Object)
    $assemblyDirectory = Join-Path $RecipeRoot 'assembly_machine'
    $actualPackFiles = @(Get-ChildItem -LiteralPath $assemblyDirectory -Filter 'fluid_pack_*.json' -File |
        Where-Object Name -ne 'fluid_pack_empty.json' | Select-Object -ExpandProperty Name | Sort-Object)
    $actualUnpackFiles = @(Get-ChildItem -LiteralPath $assemblyDirectory -Filter 'fluid_unpack_*.json' -File |
        Select-Object -ExpandProperty Name | Sort-Object)
    Assert-Sequence 'assembly fluid-pack recipe files' $actualPackFiles $expectedPackFiles
    Assert-Sequence 'assembly fluid-unpack recipe files' $actualUnpackFiles $expectedUnpackFiles
    foreach ($fluidName in $eligibleFluidNames) { Assert-FluidPackRecipe $fluidName }
}

if (-not (Test-Path -LiteralPath $fluidContainerPath -PathType Leaf)) {
    Add-Problem "Missing fluid-container implementation $fluidContainerPath"
} else {
    $fluidContainerSource = Get-Content -LiteralPath $fluidContainerPath -Raw
    $script:checks++
    if ($fluidContainerSource -notmatch 'FLUID_PACK\(32_000,[^\r\n]+HbmFluidContainerItem::allowsFluidPack\)') {
        Add-Problem 'Fluid packs do not use their dedicated legacy-compatible NO_CONTAINER-only filter'
    }
    $script:checks++
    if ($fluidContainerSource -notmatch 'allowsFluidPack\(HbmFluidDefinition fluid\)[\s\S]*?!fluid\.isNone\(\)\s*&&\s*!fluid\.hasTrait\(HbmFluidTrait\.NO_CONTAINER\)') {
        Add-Problem 'Fluid-pack filter does not accept every non-NONE fluid without NO_CONTAINER'
    }
}

if (-not (Test-Path -LiteralPath $assemblyMachinePath -PathType Leaf)) {
    Add-Problem "Missing assembly-machine implementation $assemblyMachinePath"
} else {
    $assemblyMachineSource = Get-Content -LiteralPath $assemblyMachinePath -Raw
    $assemblyTankContracts = @(
        'inputTank\.setCapacity\(Math\.max\(Math\.max\(this\.inputTank\.amount\(\), input\.amount\(\) \* 2\), TANK_CAPACITY\)\)',
        'outputTank\.setCapacity\(Math\.max\(Math\.max\(this\.outputTank\.amount\(\), output\.amount\(\) \* 2\), TANK_CAPACITY\)\)',
        'case 0 -> inputTank\.capacity\(\)',
        'case 1 -> outputTank\.capacity\(\)'
    )
    foreach ($contract in $assemblyTankContracts) {
        $script:checks++
        if ($assemblyMachineSource -notmatch $contract) {
            Add-Problem "Assembly machine is missing dynamic fluid-tank contract $contract"
        }
    }
}

# Other latest-upstream work recipe deltas.
$ammoDgk = Read-Recipe 'ammo_press/ammo_dgk.json'
if ($null -ne $ammoDgk) {
    Assert-Equal 'ammo_press/ammo_dgk.json type' $ammoDgk.type 'reinhardtshbm:ammo_press'
    Assert-Sequence 'ammo_press/ammo_dgk.json grid' (Ingredient-Keys $ammoDgk.input) @(
        'unknown:{}x1', 'tag:c:ingots/steelx5', 'unknown:{}x1',
        'unknown:{}x1', 'tag:c:dusts/smokelessx5', 'unknown:{}x1',
        'unknown:{}x1', 'tag:c:plates/gunmetalx5', 'unknown:{}x1'
    )
    Assert-Equal 'ammo_press/ammo_dgk.json result' (Stack-Key $ammoDgk.result) 'reinhardtshbm:ammo_dgkx1'
}

$blast = Read-Recipe 'blast_furnace/firebrick_limestone.json'
if ($null -ne $blast) {
    Assert-Equal 'blast furnace limestone ingredient' (Ingredient-Key $blast.input_a) 'tag:c:dusts/limestonex1'
    Assert-Equal 'blast furnace limestone clay count' $blast.input_b_count 6
    Assert-Equal 'blast furnace limestone duration' $blast.duration 800
    Assert-Equal 'blast furnace limestone result' (Stack-Key $blast.result) 'reinhardtshbm:ingot_firebrickx8'
}
$rareEarth = Read-Recipe 'centrifuge/ore_rare_earth.json'
if ($null -ne $rareEarth) { Assert-Equal 'centrifuge rare-earth input' (Ingredient-Key $rareEarth.ingredient) 'tag:c:ores/rare_earthsx1' }
$thorium = Read-Recipe 'centrifuge/ore_thorium.json'
if ($null -ne $thorium) { Assert-Equal 'centrifuge thorium input' (Ingredient-Key $thorium.ingredient) 'tag:c:ores/thoriumx1' }

Assert-GenericRecipe 'chemical_plant/helium3.json' 'reinhardtshbm:chemical_plant' 'chem.helium3' 25 2000 @('item:reinhardtshbm:moon_turfx1') @() @() @('helium3x500@p0')
Assert-GenericRecipe 'chemical_plant/part_generic_alclad_light_construction.json' 'reinhardtshbm:chemical_plant' 'chem.lightconstruction' 400 25000 @('tag:c:plates/dura_steelx2') @('xylenex1000@p0', 'phosgenex1000@p0', 'sourgasx4000@p0') @('reinhardtshbm:part_generic_alcladx1') @()
Assert-GenericRecipe 'chemical_plant/part_generic_alclad_carbon_carbon.json' 'reinhardtshbm:chemical_plant' 'chem.carboncarbon' 400 100000 @('tag:c:gems/cokex8', 'tag:c:plates/titaniumx1') @('heavyoil_vacuumx1000@p0', 'syngasx8000@p0') @('reinhardtshbm:part_generic_alcladx1') @()

$rebar = Read-Recipe 'crystallizer/rebar_to_concrete_rebar.json'
if ($null -ne $rebar) {
    Assert-Equal 'crystallizer rebar duration' $rebar.duration 1
    Assert-Equal 'crystallizer rebar input' (Ingredient-Key $rebar.ingredient) 'item:reinhardtshbm:rebarx1'
    Assert-Equal 'crystallizer rebar fluid' (Fluid-Key $rebar.acid) 'concretex1000@p0'
    Assert-Equal 'crystallizer rebar result' (Stack-Key $rebar.result) 'reinhardtshbm:concrete_rebarx1'
}

$liquefaction = @{
    'coal_to_coaloil.json' = @('item:minecraft:coalx1', 'coaloilx250@p0')
    'coal_dust_to_coaloil.json' = @('tag:c:dusts/coalx1', 'coaloilx250@p0')
    'lignite_to_coaloil.json' = @('item:reinhardtshbm:lignitex1', 'coaloilx150@p0')
    'lignite_dust_to_coaloil.json' = @('tag:c:dusts/lignitex1', 'coaloilx150@p0')
    'melon_slice_to_ethanol.json' = @('item:minecraft:melon_slicex1', 'ethanolx100@p0')
    'hemp_to_ethanol.json' = @('item:reinhardtshbm:plant_hempx1', 'ethanolx100@p0')
    'mustard_willow_to_ethanol.json' = @('item:reinhardtshbm:plant_mustard_willowx1', 'ethanolx50@p0')
}
foreach ($file in $liquefaction.Keys) {
    $recipe = Read-Recipe "liquefaction/$file"
    if ($null -ne $recipe) {
        Assert-Equal "liquefaction/$file input" (Ingredient-Key $recipe.ingredient) $liquefaction[$file][0]
        Assert-Equal "liquefaction/$file output" (Fluid-Key $recipe.output) $liquefaction[$file][1]
    }
}

$purexRoutes = @{
    'flash_gold.json' = @('600', '1000', 'item:reinhardtshbm:billet_au198x1|item:reinhardtshbm:pellet_chargedx1', 'amatx1000@p0', 'reinhardtshbm:billet_balefire_goldx2')
    'flash_lead.json' = @('600', '1000', 'item:reinhardtshbm:billet_pb209x1|item:reinhardtshbm:billet_balefire_goldx1', 'amatx1000@p0', 'reinhardtshbm:billet_flashleadx1')
    'pile_pu239.json' = @('40', '100', 'item:reinhardtshbm:pile_rod_pu239x1', 'sulfuric_acidx100@p0', 'reinhardtshbm:billet_pu239x2|reinhardtshbm:billet_uraniumx1')
    'pile_rgp.json' = @('40', '100', 'item:reinhardtshbm:pile_rod_rgpx1', 'sulfuric_acidx100@p0', 'reinhardtshbm:billet_pu_mixx2|reinhardtshbm:billet_nuclear_wastex1')
    'pile_thorium.json' = @('40', '100', 'item:reinhardtshbm:pile_rod_thorium_fuelx1', 'sulfuric_acidx100@p0', 'reinhardtshbm:billet_thorium_fuelx2|reinhardtshbm:billet_nuclear_wastex1')
    'pile_waste.json' = @('40', '100', 'item:reinhardtshbm:pile_rod_wastex1', 'sulfuric_acidx100@p0', 'reinhardtshbm:billet_nuclear_wastex2|reinhardtshbm:billet_poloniumx1')
}
foreach ($file in $purexRoutes.Keys) {
    $recipe = Read-Recipe "purex/$file"
    if ($null -ne $recipe) {
        $expected = $purexRoutes[$file]
        Assert-Equal "purex/$file duration" $recipe.duration $expected[0]
        Assert-Equal "purex/$file power" $recipe.power $expected[1]
        Assert-Equal "purex/$file inputs" ((Ingredient-Keys $recipe.input_items) -join '|') $expected[2]
        Assert-Equal "purex/$file fluids" ((Fluid-Keys $recipe.input_fluids) -join '|') $expected[3]
        Assert-Equal "purex/$file outputs" ((Stack-Keys $recipe.output_items) -join '|') $expected[4]
    }
}

$shredderRoutes = @{
    'drive_flash_broken_to_powder_polymer.json' = @('item:reinhardtshbm:drive_flash_brokenx1', 'reinhardtshbm:powder_polymerx2')
    'drive_disk_broken_to_powder_polymer.json' = @('item:reinhardtshbm:drive_disk_brokenx1', 'reinhardtshbm:powder_polymerx4')
    'powder_lapis_to_powder_cobalt_tiny.json' = @('tag:c:dusts/lapisx1', 'reinhardtshbm:powder_cobalt_tinyx1')
}
foreach ($file in $shredderRoutes.Keys) {
    $recipe = Read-Recipe "shredder/$file"
    if ($null -ne $recipe) {
        Assert-Equal "shredder/$file input" (Ingredient-Key $recipe.ingredient) $shredderRoutes[$file][0]
        Assert-Equal "shredder/$file result" (Stack-Key $recipe.result) $shredderRoutes[$file][1]
    }
}

$solderingPcb = @{
    'circuit_controller.json' = @('item:reinhardtshbm:circuit_controller_chassisx1', 'tag:c:ingots/rubberx4')
    'circuit_controller_advanced.json' = @('item:reinhardtshbm:circuit_controller_chassisx1', 'tag:c:ingots/hard_plasticx4')
    'circuit_controller_quantum.json' = @('item:reinhardtshbm:circuit_controller_advancedx2', 'item:reinhardtshbm:upgrade_speed_3x1')
    'sat_chip.json' = @('item:reinhardtshbm:circuit_pcbx2')
}
foreach ($file in $solderingPcb.Keys) {
    $recipe = Read-Recipe "soldering_station/$file"
    if ($null -ne $recipe) { Assert-Sequence "soldering_station/$file pcb" (Ingredient-Keys $recipe.pcb) $solderingPcb[$file] }
}

$precisionRoutes = @{
    'orbital_assembly_crystal_circuit.json' = 'reinhardtshbm:orbital_assembly_crystal_circuitx1@50|reinhardtshbm:broken_itemx1@50'
    'orbital_assembly_crystal_circuit_expensive.json' = 'reinhardtshbm:orbital_assembly_crystal_circuitx1@25|reinhardtshbm:broken_itemx1@75'
    'rpa_helmet.json' = 'reinhardtshbm:rpa_helmetx1@100'
    'rpa_plate.json' = 'reinhardtshbm:rpa_platex1@100'
    'rpa_legs.json' = 'reinhardtshbm:rpa_legsx1@100'
    'rpa_boots.json' = 'reinhardtshbm:rpa_bootsx1@100'
}
foreach ($file in $precisionRoutes.Keys) {
    $recipe = Read-Recipe "precision_assembler/$file"
    if ($null -ne $recipe) {
        Assert-Equal "precision_assembler/$file duration" $recipe.duration $(if ($file.StartsWith('rpa_')) { 3600 } else { 600 })
        Assert-Equal "precision_assembler/$file output pool" (@($recipe.outputs | ForEach-Object { "$(Stack-Key $_.result)@$([int]$_.weight)" }) -join '|') $precisionRoutes[$file]
    }
}

$plasmaNew = @{
    'expensive_steel_plating.json' = @('plsm.exsteelplating', '40', '100000', '1000000', 'reinhardtshbm:item_expensive_steel_platingx1')
    'expensive_heavy_frame.json' = @('plsm.exheavyframe', '120', '100000', '1000000', 'reinhardtshbm:item_expensive_heavy_framex1')
    'expensive_circuit.json' = @('plsm.excircuit', '80', '100000', '1000000', 'reinhardtshbm:item_expensive_circuitx1')
    'expensive_lead_plating.json' = @('plsm.exleadplating', '80', '100000', '1000000', 'reinhardtshbm:item_expensive_lead_platingx1')
    'expensive_ferro_plating.json' = @('plsm.exferroplating', '240', '100000', '1000000', 'reinhardtshbm:item_expensive_ferro_platingx1')
    'expensive_computer.json' = @('plsm.excomputer', '240', '100000', '1000000', 'reinhardtshbm:item_expensive_computerx1')
    'expensive_bronze_tubes.json' = @('plsm.bronzetubes', '600', '1000000', '10000000', 'reinhardtshbm:item_expensive_bronze_tubesx1')
    'expensive_plastic.json' = @('plsm.explastic', '120', '100000', '1000000', 'reinhardtshbm:item_expensive_plasticx1')
    'expensive_gold_dust.json' = @('plsm.exgold', '120', '100000', '1000000', 'reinhardtshbm:item_expensive_gold_dustx1')
    'cmb_tubes.json' = @('plsm.cmbtubes', '6000', '10000000', '12500000', 'reinhardtshbm:item_expensive_cmb_tubesx1')
}
foreach ($file in $plasmaNew.Keys) {
    $recipe = Read-Recipe "plasma_forge/$file"
    if ($null -ne $recipe) {
        $expected = $plasmaNew[$file]
        Assert-Equal "plasma_forge/$file group" $recipe.group $expected[0]
        Assert-Equal "plasma_forge/$file duration" $recipe.duration $expected[1]
        Assert-Equal "plasma_forge/$file power" $recipe.power $expected[2]
        Assert-Equal "plasma_forge/$file ignition" $recipe.ignition_temp $expected[3]
        Assert-Equal "plasma_forge/$file result" (Stack-Key $recipe.result) $expected[4]
    }
}

# Latest annihilator payout is code-backed rather than a custom recipe type.
$annihilatorPath = 'src/main/java/com/reinhardt/hbm/machine/AnnihilatorRecipes.java'
if (-not (Test-Path -LiteralPath $annihilatorPath -PathType Leaf)) {
    Add-Problem "Missing annihilator recipe table $annihilatorPath"
} else {
    $annihilatorSource = Get-Content -LiteralPath $annihilatorPath -Raw
    $script:checks++
    if ($annihilatorSource -notmatch 'item\("drive_flash_flightsim",\s*64,\s*"528\.soyuz"\)') {
        Add-Problem 'Annihilator table is missing the latest-upstream drive_flash_flightsim -> 64 x 528.soyuz payout'
    }
}

# Latest anvil construction deltas are code-backed and must remain exact.
$anvilPath = 'src/main/java/com/reinhardt/hbm/recipe/anvil/HbmAnvilRecipes.java'
if (-not (Test-Path -LiteralPath $anvilPath -PathType Leaf)) {
    Add-Problem "Missing anvil recipe table $anvilPath"
} else {
    $anvilSource = Get-Content -LiteralPath $anvilPath -Raw
    $anvilContracts = @(
        'blockItem\("platemetal",\s*4\)',
        'blockItem\("machine_rockmill",\s*1\)',
        'item\("pile_rod_ra226be",\s*1\)',
        'item\("pile_rod_po210be",\s*1\)',
        'item\("pile_rod_zr",\s*1\)',
        'item\("pile_rod_nu",\s*1\)',
        'ingredientOptional\("platemetal",\s*4\)'
    )
    foreach ($contract in $anvilContracts) {
        $script:checks++
        if ($anvilSource -notmatch $contract) { Add-Problem "Anvil recipe table is missing contract $contract" }
    }
}

# Several large 1.7.10 recipe tables were generated from runtime registries
# rather than being hand-authored rows.  Guard both machine execution and JEI
# projection so those recipes cannot silently disappear while the static JSON
# count remains unchanged.
$dynamicRecipeContracts = @(
    @('src/main/java/com/reinhardt/hbm/recipe/ArcFurnaceRecipe.java',
      'Arc furnace dynamic foundry-material projection',
      'dynamicOutputs\(ItemStack stack\)[\s\S]*?FoundryMaterial\.smeltingMaterialsFromItem\(stack\)[\s\S]*?SmeltingBehavior\.SMELTABLE'),
    @('src/main/java/com/reinhardt/hbm/blockentity/ArcFurnaceBlockEntity.java',
      'Arc furnace machine fallback to dynamic liquid recipes',
      'isLiquidMode\(\)\s*&&\s*!ArcFurnaceRecipe\.dynamicOutputs\(stack\)\.isEmpty\(\)[\s\S]*?dynamicRecipe\(stack\)'),
    @('src/main/java/com/reinhardt/hbm/integration/jei/ArcFurnaceJeiRecipes.java',
      'Arc furnace JEI projection of every dynamic foundry input',
      'for\s*\(Item item\s*:\s*BuiltInRegistries\.ITEM\)[\s\S]*?ArcFurnaceRecipe\.dynamicOutputs\(input\)'),
    @('src/main/java/com/reinhardt/hbm/integration/jei/ArcFurnaceJeiRecipes.java',
      'Arc furnace JEI projection of compatible vanilla smelting recipes',
      'getAllRecipesFor\(RecipeType\.SMELTING\)[\s\S]*?ArcFurnaceRecipe\.isArcSmeltable\(input, output\)'),
    @('src/main/java/com/reinhardt/hbm/blockentity/CrucibleBlockEntity.java',
      'Crucible runtime consumption of flattened foundry materials',
      'FoundryMaterial\.smeltingMaterialsFromItem\(getItem\(slot\)\)'),
    @('src/main/java/com/reinhardt/hbm/blockentity/CrucibleBlockEntity.java',
      'Crucible input validation against flattened foundry materials',
      'isItemSmeltable\(ItemStack stack\)[\s\S]*?FoundryMaterial\.smeltingMaterialsFromItem\(stack\)'),
    @('src/main/java/com/reinhardt/hbm/blockentity/LegacyMachineBlockEntity.java',
      'Pyrolysis fallback order for bedrock roasting and generated solid fuel',
      'pyroBedrockOreRecipe\(\)[\s\S]*?bedrockRecipe\.isPresent\(\)\s*\?\s*bedrockRecipe\s*:\s*pyroSolidFuelRecipe\(\)'),
    @('src/main/java/com/reinhardt/hbm/blockentity/LegacyMachineBlockEntity.java',
      'Pyrolysis five-grade flattened bedrock roasting table',
      'case BASE\s*->\s*BedrockOreItem\.Grade\.BASE_ROASTED;[\s\S]*?case PRIMARY\s*->\s*BedrockOreItem\.Grade\.PRIMARY_ROASTED;[\s\S]*?case SULFURIC_BYPRODUCT\s*->\s*BedrockOreItem\.Grade\.SULFURIC_ROASTED;[\s\S]*?case SOLVENT_BYPRODUCT\s*->\s*BedrockOreItem\.Grade\.SOLVENT_ROASTED;[\s\S]*?case RAD_BYPRODUCT\s*->\s*BedrockOreItem\.Grade\.RAD_ROASTED;'),
    @('src/main/java/com/reinhardt/hbm/blockentity/LegacyMachineBlockEntity.java',
      'Pyrolysis runtime solid-fuel amount derived from flammable heat energy',
      'pyroSolidFuelRecipe\(\)[\s\S]*?flammableHeatEnergy\(\)[\s\S]*?24_000_000L\s*:\s*1_440_000L[\s\S]*?new PyroOvenRecipe\.FluidInput\(fluid, amount\)'),
    @('src/main/java/com/reinhardt/hbm/integration/jei/PyroOvenJeiRecipes.java',
      'Pyrolysis JEI projection of every flattened bedrock type and roast grade',
      'appendBedrockOreRecipes\(recipes\)[\s\S]*?for\s*\(BedrockOreItem\.Type type\s*:\s*BedrockOreItem\.Type\.values\(\)\)[\s\S]*?for\s*\(BedrockRoast roast\s*:\s*BedrockRoast\.values\(\)\)'),
    @('src/main/java/com/reinhardt/hbm/integration/jei/PyroOvenJeiRecipes.java',
      'Pyrolysis JEI projection of every flammable registered fluid',
      'for\s*\(HbmFluidDefinition fluid\s*:\s*HbmFluids\.definitions\(\)\)[\s\S]*?fluid\.flammableHeatEnergy\(\)'),
    @('src/main/java/com/reinhardt/hbm/blockentity/WasteDrumBlockEntity.java',
      'Spent-fuel pool dynamic cooling of every flattened RBMK fuel rod',
      'stack\.getItem\(\) instanceof RbmkFuelRodItem[\s\S]*?RbmkFuelRodItem\.coolInSpentFuelPool\('),
    @('src/main/java/com/reinhardt/hbm/blockentity/FusionMachineBlockEntity.java',
      'Fusion breeder reuse of the complete RBMK outgasser recipe table',
      'outgasserRecipe\(Level level, ItemStack input\)[\s\S]*?getAllRecipesFor\(HbmRecipeTypes\.RBMK_OUTGASSER\.get\(\)\)[\s\S]*?ingredient\(\)\.test\(input\)'),
    @('src/main/java/com/reinhardt/hbm/blockentity/FusionMachineBlockEntity.java',
      'Fusion breeder execution of shared outgasser item and fluid outputs',
      'processBreederSolid\(Level level\)[\s\S]*?outgasserRecipe\(level, input\)[\s\S]*?mergeOutput\(BREEDER_OUTPUT_SLOT, recipe\.itemOutput\(\)\.copy\(\)\)[\s\S]*?tank\(1\)\.fill\(recipe\.fluidOutput\(\)\.fluid\(\), recipe\.fluidOutput\(\)\.amount\(\), false\)'),
    @('src/main/java/com/reinhardt/hbm/integration/jei/HbmJeiPlugin.java',
      'Fusion breeder JEI catalyst for the shared outgasser recipe table',
      'addRecipeCatalyst\(HbmBlocks\.FUSION_BREEDER\.get\(\), RBMK_OUTGASSER\)')
)
foreach ($contract in $dynamicRecipeContracts) {
    Assert-SourceContract $contract[0] $contract[1] $contract[2]
}

# Freeze the complete assembly recipe condition partition as well as the
# runtime counts. Legacy inputItemsEx is controlled by enableExpensiveMode,
# independently from enable528; treating those branches as 528 recipes was a
# migration bug that this partition must reject permanently.
$assemblyDirectory = Join-Path $RecipeRoot 'assembly_machine'
if (-not (Test-Path -LiteralPath $assemblyDirectory -PathType Container)) {
    Add-Problem 'Missing assembly_machine recipe directory for condition-partition audit'
} else {
    $normalExpensiveBranch = 0
    $expensiveOnly = 0
    $normal528Branch = 0
    $unconditional = 0
    foreach ($file in Get-ChildItem -LiteralPath $assemblyDirectory -Filter '*.json' -File) {
        $raw = Get-Content -LiteralPath $file.FullName -Raw
        $hasExpensive = $raw -match '"type"\s*:\s*"reinhardtshbm:enable_expensive_mode"'
        $has528 = $raw -match '"type"\s*:\s*"reinhardtshbm:enable_528_mode"'
        if ($hasExpensive -and $has528) {
            Add-Problem "Assembly recipe mixes 528 and expensive-mode conditions: $($file.Name)"
        } elseif ($hasExpensive -and $raw -match '"type"\s*:\s*"neoforge:not"[\s\S]*?"type"\s*:\s*"reinhardtshbm:enable_expensive_mode"') {
            $normalExpensiveBranch++
        } elseif ($hasExpensive) {
            $expensiveOnly++
        } elseif ($has528 -and $raw -match '"type"\s*:\s*"neoforge:not"[\s\S]*?"type"\s*:\s*"reinhardtshbm:enable_528_mode"') {
            $normal528Branch++
        } elseif ($has528) {
            Add-Problem "Unexpected 528-only assembly recipe after inputItemsEx separation: $($file.Name)"
        } else {
            $unconditional++
        }
    }
    Assert-Equal 'assembly not-expensive recipe files' $normalExpensiveBranch 105
    Assert-Equal 'assembly expensive-only recipe files' $expensiveOnly 92
    Assert-Equal 'assembly not-528 recipe files' $normal528Branch 10
    # The latest upstream's projection-built struct_soyuz_core route belongs to
    # the explicitly deprecated Soyuz implementation; the direct-place
    # launchpad_soyuz replacement remains covered above.
    Assert-Equal 'assembly unconditional recipe files' $unconditional 554
}

$lines = [System.Collections.Generic.List[string]]::new()
$lines.Add('Latest-upstream machine recipe parity audit')
$lines.Add("reference commit: $referenceCommit")
$lines.Add("semantic assertions: $checks")
$lines.Add("problems: $($problems.Count)")
$lines.Add('')
$lines.Add('PROBLEMS')
foreach ($problem in $problems) { $lines.Add($problem) }

$outputFile = [System.IO.Path]::GetFullPath($OutputReport)
[System.IO.Directory]::CreateDirectory([System.IO.Path]::GetDirectoryName($outputFile)) | Out-Null
[System.IO.File]::WriteAllLines($outputFile, $lines, [System.Text.UTF8Encoding]::new($false))
Write-Host "Latest-upstream machine recipe audit: $checks assertions, $($problems.Count) problems. Report: $outputFile"

if ($FailOnMismatch -and $problems.Count -gt 0) {
    throw "Latest-upstream machine recipe audit failed with $($problems.Count) problem(s); see $outputFile"
}
