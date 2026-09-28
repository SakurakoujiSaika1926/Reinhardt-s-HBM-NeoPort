param(
    [string]$ProjectRoot = ".",
    [string]$BaselineRoot = "..\..\HBM_1.7.10",
    [string]$LatestRoot = ("..\..\HBM_1.7.10" + [char]0x65B0),
    [string]$OutputReport = "build\reports\latest-upstream-content-gap.md",
    [switch]$FailOnIncomplete
)

$ErrorActionPreference = "Stop"

function Resolve-Root([string]$Path, [string]$Base) {
    $candidate = if ([IO.Path]::IsPathRooted($Path)) { $Path } else { Join-Path $Base $Path }
    return (Resolve-Path -LiteralPath $candidate).Path
}

function Get-Sha256([string]$Path) {
    $stream = [IO.File]::OpenRead($Path)
    try {
        $sha256 = [Security.Cryptography.SHA256]::Create()
        try {
            return ([BitConverter]::ToString($sha256.ComputeHash($stream))).Replace('-', '')
        } finally {
            $sha256.Dispose()
        }
    } finally {
        $stream.Dispose()
    }
}

function Get-UncommentedJava([string]$Path) {
    if (-not (Test-Path -LiteralPath $Path)) {
        throw "Missing Java source: $Path"
    }

    $source = Get-Content -LiteralPath $Path -Raw
    $source = [regex]::Replace($source, '(?s)/\*.*?\*/', '')
    return [regex]::Replace($source, '(?m)//.*$', '')
}

function Get-LegacyItemIds([string]$Root) {
    $path = Join-Path $Root "src\main\java\com\hbm\items\ModItems.java"
    $source = Get-UncommentedJava $path

    $declaredFields = [System.Collections.Generic.HashSet[string]]::new()
    foreach ($match in [regex]::Matches(
            $source,
            '(?m)^\s*public\s+static\s+(?:final\s+)?[A-Za-z0-9_$.<>]+\s+([A-Za-z_][A-Za-z0-9_]*)\s*;')) {
        [void]$declaredFields.Add($match.Groups[1].Value)
    }

    $registeredFields = @([regex]::Matches(
            $source,
            'GameRegistry\s*\.\s*registerItem\s*\(\s*([A-Za-z_][A-Za-z0-9_]*)\s*,') |
        ForEach-Object { $_.Groups[1].Value } |
        Where-Object { $declaredFields.Contains($_) } |
        Sort-Object -Unique)

    if ($registeredFields.Count -lt 1600) {
        throw "Parsed only $($registeredFields.Count) registered items from $path"
    }

    $registeredSet = [System.Collections.Generic.HashSet[string]]::new([string[]]$registeredFields)
    $registryIdsByField = @{}
    $unresolvedLocalInitializers = [System.Collections.Generic.HashSet[string]]::new()

    foreach ($statement in ($source -split ';')) {
        $assignment = [regex]::Match($statement, '(?s)\b([A-Za-z_][A-Za-z0-9_]*)\s*=')
        if (-not $assignment.Success) { continue }

        $field = $assignment.Groups[1].Value
        if (-not $registeredSet.Contains($field)) { continue }

        $unlocalized = [regex]::Match($statement, '\.setUnlocalizedName\(\s*"([a-z0-9_]+)"\s*\)')
        if ($unlocalized.Success) {
            $id = $unlocalized.Groups[1].Value
            if ($registryIdsByField.ContainsKey($field) -and $registryIdsByField[$field] -ne $id) {
                throw "Conflicting item ids for ${field}: $($registryIdsByField[$field]) and $id"
            }
            $registryIdsByField[$field] = $id
        } elseif ($statement -match '\bnew\s+[A-Za-z0-9_$.]+' -or $statement -match '\.copy\s*\(') {
            [void]$unresolvedLocalInitializers.Add($field)
        }
    }

    $unresolved = @($unresolvedLocalInitializers | Where-Object { -not $registryIdsByField.ContainsKey($_) })
    if ($unresolved.Count -gt 0) {
        throw "Local item initializers without setUnlocalizedName in ${path}: $($unresolved -join ', ')"
    }

    foreach ($field in $registeredFields) {
        if (-not $registryIdsByField.ContainsKey($field)) {
            $registryIdsByField[$field] = $field
        }
    }

    return @($registeredFields | ForEach-Object { $registryIdsByField[$_] } | Sort-Object -Unique)
}

function Get-LegacyBlockIds([string]$Root) {
    $path = Join-Path $Root "src\main\java\com\hbm\blocks\ModBlocks.java"
    $source = Get-UncommentedJava $path

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
        $field = $match.Groups[1].Value
        if ($declared.Contains($field)) { [void]$registered.Add($field) }
    }
    foreach ($match in [regex]::Matches(
            $source,
            '(?m)^\s*register\s*\(\s*([A-Za-z_][A-Za-z0-9_]*)\s*(?:,|\))')) {
        $field = $match.Groups[1].Value
        if ($declared.Contains($field)) { [void]$registered.Add($field) }
    }

    if ($registered.Count -lt 900) {
        throw "Parsed only $($registered.Count) registered blocks from $path"
    }

    $fieldToRegistryId = @{}
    foreach ($statement in ($source -split ';')) {
        $fieldMatch = [regex]::Match($statement, '(?s)\b([A-Za-z_][A-Za-z0-9_]*)\s*=')
        $nameMatch = [regex]::Match($statement, '\.setBlockName\(\s*"([a-z0-9_]+)"\s*\)')
        if ($fieldMatch.Success -and $nameMatch.Success) {
            $fieldToRegistryId[$fieldMatch.Groups[1].Value] = $nameMatch.Groups[1].Value
        }
    }

    return @($registered | ForEach-Object {
        if ($fieldToRegistryId.ContainsKey($_)) { $fieldToRegistryId[$_] } else { $_ }
    } | Sort-Object -Unique)
}

function Get-GitValue([string]$Root, [string[]]$Arguments) {
    if (-not (Test-Path -LiteralPath (Join-Path $Root ".git"))) { return $null }
    $value = & git -C $Root @Arguments 2>$null
    if ($LASTEXITCODE -ne 0) { return $null }
    return ($value | Out-String).Trim()
}

function Test-SourceLiteral([string]$Source, [string]$Id) {
    return [regex]::IsMatch($Source, '["'']' + [regex]::Escape($Id) + '["'']')
}

function Get-ProducedRecipeIds([string]$RecipeRoot) {
    $ids = [System.Collections.Generic.HashSet[string]]::new()
    if (-not (Test-Path -LiteralPath $RecipeRoot)) { return ,$ids }

    function Add-OutputId([object]$Value, [System.Collections.Generic.HashSet[string]]$Target) {
        if ($null -eq $Value) { return }
        $id = if ($Value -is [System.Collections.IDictionary]) { $Value['id'] } else { $Value.PSObject.Properties['id'].Value }
        if ($id -is [string]) {
            [void]$Target.Add($id)
        }
        $nested = if ($Value -is [System.Collections.IDictionary]) { $Value['result'] } else { $Value.PSObject.Properties['result'].Value }
        if ($null -ne $nested) {
            Add-OutputId $nested $Target
        }
    }

    foreach ($file in Get-ChildItem -LiteralPath $RecipeRoot -Filter '*.json' -File -Recurse) {
        try {
            $recipe = ConvertTo-Hashtable (Get-Content -LiteralPath $file.FullName -Raw | ConvertFrom-Json)
        } catch {
            throw "Invalid recipe JSON while collecting outputs: $($file.FullName): $($_.Exception.Message)"
        }
        if ($recipe -isnot [System.Collections.IDictionary]) { continue }

        if ($recipe.Contains('result')) { Add-OutputId $recipe['result'] $ids }
        if ($recipe.Contains('output')) { Add-OutputId $recipe['output'] $ids }
        foreach ($field in @('output_items', 'outputs')) {
            if ($recipe.Contains($field) -and $recipe[$field] -is [System.Collections.IEnumerable]) {
                foreach ($output in $recipe[$field]) { Add-OutputId $output $ids }
            }
        }
    }
    return ,$ids
}

function Get-ItemStatus(
    [string]$Project,
    [string]$ItemSource,
    [hashtable]$English,
    [hashtable]$Chinese,
    [System.Collections.Generic.HashSet[string]]$AcquisitionIds,
    [string]$Id
) {
    $registered = Test-SourceLiteral $ItemSource $Id
    $model = Test-Path -LiteralPath (Join-Path $Project "src\main\resources\assets\reinhardtshbm\models\item\$Id.json")
    $en = $English.ContainsKey("item.reinhardtshbm.$Id")
    $zh = $Chinese.ContainsKey("item.reinhardtshbm.$Id")
    $acquisition = $AcquisitionIds.Contains("reinhardtshbm:$Id")
    $complete = $registered -and $model -and $en -and $zh -and $acquisition
    return [pscustomobject]@{
        Registered = $registered
        Model = $model
        English = $en
        Chinese = $zh
        Acquisition = $acquisition
        Complete = $complete
    }
}

function Get-BlockStatus(
    [string]$Project,
    [string]$BlockSource,
    [hashtable]$English,
    [hashtable]$Chinese,
    [System.Collections.Generic.HashSet[string]]$ProducedRecipeIds,
    [string]$Id
) {
    $registered = Test-SourceLiteral $BlockSource $Id
    if (-not $registered) {
        # Heavy doors own their registry id in HbmDoorDecl and are registered
        # through heavyDoor(HbmDoorDecl.X), so HbmBlocks intentionally has no
        # duplicate string literal. Resolve that exact two-file registration
        # contract instead of reporting a false gap.
        $doorDeclPath = Join-Path $Project "src\main\java\com\reinhardt\hbm\door\HbmDoorDecl.java"
        if (Test-Path -LiteralPath $doorDeclPath) {
            $doorDeclSource = Get-Content -LiteralPath $doorDeclPath -Raw
            $declMatch = [regex]::Match(
                $doorDeclSource,
                '(?m)^\s*([A-Z0-9_]+)\("' + [regex]::Escape($Id) + '"'
            )
            if ($declMatch.Success) {
                $registered = $BlockSource.Contains("HbmDoorDecl.$($declMatch.Groups[1].Value)")
            }
        }
    }
    $blockstate = Test-Path -LiteralPath (Join-Path $Project "src\main\resources\assets\reinhardtshbm\blockstates\$Id.json")
    $itemModel = Test-Path -LiteralPath (Join-Path $Project "src\main\resources\assets\reinhardtshbm\models\item\$Id.json")
    $blockModelPath = Join-Path $Project "src\main\resources\assets\reinhardtshbm\models\block\$Id.json"
    $blockModel = Test-Path -LiteralPath $blockModelPath
    if (-not $blockModel -and $itemModel) {
        # Standalone BER machines intentionally render an empty blockstate and
        # point their item model at an assembled block-model variant. Count the
        # referenced, existing block model instead of requiring a fake unused
        # canonical model at models/block/<registry-id>.json.
        $itemModelPath = Join-Path $Project "src\main\resources\assets\reinhardtshbm\models\item\$Id.json"
        $itemModelJson = Get-Content -LiteralPath $itemModelPath -Raw | ConvertFrom-Json
        if ($itemModelJson.parent -is [string] -and $itemModelJson.parent.StartsWith('reinhardtshbm:block/')) {
            $parentName = $itemModelJson.parent.Substring('reinhardtshbm:block/'.Length)
            $blockModel = Test-Path -LiteralPath (Join-Path $Project "src\main\resources\assets\reinhardtshbm\models\block\$parentName.json")
        }

        # The two latest-upstream launch pads are rendered entirely by their
        # dedicated BER/BEWLR pair.  Their blockstate deliberately points at
        # block/empty and the item model must remain minecraft:builtin/entity;
        # requiring a fake models/block/<id>.json here would hide a broken
        # custom-renderer registration instead of validating the real path.
        if (-not $blockModel `
                -and $itemModelJson.parent -eq 'minecraft:builtin/entity' `
                -and $Id -in @('launchpad_lambda', 'launchpad_soyuz')) {
            $pascal = if ($Id -eq 'launchpad_lambda') { 'LambdaLaunchpad' } else { 'SoyuzLaunchpad' }
            $blockItemPath = Join-Path $Project "src\main\java\com\reinhardt\hbm\item\${pascal}BlockItem.java"
            $itemRendererPath = Join-Path $Project "src\main\java\com\reinhardt\hbm\client\render\${pascal}ItemRenderer.java"
            $entityRendererPath = Join-Path $Project "src\main\java\com\reinhardt\hbm\client\render\${pascal}BlockEntityRenderer.java"
            $emptyModelPath = Join-Path $Project "src\main\resources\assets\reinhardtshbm\models\block\empty.json"
            $blockstateSource = if ($blockstate) {
                Get-Content -LiteralPath (Join-Path $Project "src\main\resources\assets\reinhardtshbm\blockstates\$Id.json") -Raw
            } else { '' }
            $blockModel = (Test-Path -LiteralPath $blockItemPath) `
                    -and (Test-Path -LiteralPath $itemRendererPath) `
                    -and (Test-Path -LiteralPath $entityRendererPath) `
                    -and (Test-Path -LiteralPath $emptyModelPath) `
                    -and $blockstateSource.Contains('reinhardtshbm:block/empty')
        }
    }
    $loot = Test-Path -LiteralPath (Join-Path $Project "src\main\resources\data\reinhardtshbm\loot_table\blocks\$Id.json")
    $en = $English.ContainsKey("block.reinhardtshbm.$Id")
    $zh = $Chinese.ContainsKey("block.reinhardtshbm.$Id")
    $recipe = $ProducedRecipeIds.Contains("reinhardtshbm:$Id")
    $complete = $registered -and $blockstate -and $blockModel -and $itemModel -and $loot -and $en -and $zh -and $recipe
    return [pscustomobject]@{
        Registered = $registered
        Blockstate = $blockstate
        BlockModel = $blockModel
        ItemModel = $itemModel
        Loot = $loot
        English = $en
        Chinese = $zh
        Recipe = $recipe
        Complete = $complete
    }
}

function ConvertTo-Hashtable([object]$Object) {
    $table = @{}
    foreach ($property in $Object.PSObject.Properties) {
        $table[$property.Name] = $property.Value
    }
    return $table
}

function Get-LanguageKeys([string]$Path) {
    # Windows PowerShell 5.1's ConvertFrom-Json cannot reliably deserialize
    # this generated catalog once it grows to several thousand properties.
    # This audit only needs key membership; full JSON syntax remains covered
    # by Gradle's JsonSlurper-based resource validation.
    $keys = @{}
    $source = Get-Content -LiteralPath $Path -Raw
    foreach ($match in [regex]::Matches($source, '(?m)^\s*"((?:\\.|[^"\\])+)"\s*:')) {
        $keys[$match.Groups[1].Value] = $true
    }
    if ($keys.Count -lt 1000) {
        throw "Parsed only $($keys.Count) language keys from $Path"
    }
    return $keys
}

function Mark([bool]$Value) {
    if ($Value) { return "yes" }
    return "no"
}

$ProjectRoot = Resolve-Root $ProjectRoot (Get-Location).Path
$BaselineRoot = Resolve-Root $BaselineRoot $ProjectRoot
$LatestRoot = Resolve-Root $LatestRoot $ProjectRoot
$OutputReport = if ([IO.Path]::IsPathRooted($OutputReport)) {
    $OutputReport
} else {
    Join-Path $ProjectRoot $OutputReport
}

$baselineItems = @(Get-LegacyItemIds $BaselineRoot)
$latestItems = @(Get-LegacyItemIds $LatestRoot)
$baselineBlocks = @(Get-LegacyBlockIds $BaselineRoot)
$latestBlocks = @(Get-LegacyBlockIds $LatestRoot)

$addedItems = @($latestItems | Where-Object { $_ -notin $baselineItems } | Sort-Object)
$removedItems = @($baselineItems | Where-Object { $_ -notin $latestItems } | Sort-Object)
$addedBlocks = @($latestBlocks | Where-Object { $_ -notin $baselineBlocks } | Sort-Object)
$removedBlocks = @($baselineBlocks | Where-Object { $_ -notin $latestBlocks } | Sort-Object)

$expectedAddedItems = @(
    'coal_eternal', 'drive', 'missile_lambda', 'orbital_assembly',
    'pile_rod', 'rtty_card', 'rtty_designator', 'satellite'
)
$expectedAddedBlocks = @(
    'cargo_door', 'launchpad_lambda', 'launchpad_soyuz', 'machine_rockmill',
    'machine_satlink', 'machine_supercomputer', 'machine_tape_drive', 'pile_block',
    'pile_brick', 'pile_device', 'platemetal', 'pneumatic_storage_exporter',
    'pneumatic_storage_importer', 'red_pylon_steel'
)

if ((Compare-Object $expectedAddedItems $addedItems).Count -ne 0) {
    throw "Latest upstream added-item set changed. Expected [$($expectedAddedItems -join ', ')], parsed [$($addedItems -join ', ')]. Review the mappings before continuing."
}
if ((Compare-Object $expectedAddedBlocks $addedBlocks).Count -ne 0) {
    throw "Latest upstream added-block set changed. Expected [$($expectedAddedBlocks -join ', ')], parsed [$($addedBlocks -join ', ')]. Review the mappings before continuing."
}

$itemTargets = [ordered]@{
    coal_eternal = @('coal_eternal')
    drive = @(
        'drive_flash_empty', 'drive_disk_empty', 'drive_flash_broken', 'drive_disk_broken',
        'drive_flash_flightsim', 'drive_flash_particlesim', 'drive_disk_flightdata',
        'drive_disk_flightdata_processed', 'drive_disk_orbitdata',
        'drive_disk_orbitdata_processed', 'drive_klaus'
    )
    missile_lambda = @('missile_lambda')
    orbital_assembly = @('orbital_assembly_crystal_circuit')
    pile_rod = @(
        'pile_rod_ra226be', 'pile_rod_po210be', 'pile_rod_zr', 'pile_rod_nu',
        'pile_rod_pu239', 'pile_rod_rgp', 'pile_rod_waste', 'pile_rod_thorium',
        'pile_rod_thorium_fuel'
    )
    rtty_designator = @('rtty_designator')
    satellite = @(
        'satellite_spy', 'satellite_scanner', 'satellite_radar', 'satellite_miner_astro',
        'satellite_miner_lunar', 'satellite_precision_laser', 'satellite_death_ray',
        'satellite_xenium_resonator', 'satellite_relay', 'satellite_detector',
        'satellite_ray_scan', 'satellite_science', 'satellite_science_assembler',
        'satellite_science_sensor'
    )
}

$blockTargets = [ordered]@{
    cargo_door = @('cargo_door')
    launchpad_lambda = @('launchpad_lambda')
    launchpad_soyuz = @('launchpad_soyuz')
    machine_rockmill = @('machine_rockmill')
    machine_satlink = @('machine_satlink')
    machine_supercomputer = @('machine_supercomputer')
    machine_tape_drive = @('machine_tape_drive')
    pile_block = @('pile_block')
    pile_brick = @('pile_brick')
    pile_device = @('pile_loader', 'pile_vent', 'pile_control')
    platemetal = @(
        'platemetal', 'platemetal_black', 'platemetal_white', 'platemetal_red',
        'platemetal_green', 'platemetal_light_gray', 'platemetal_blue',
        'platemetal_purple', 'platemetal_cyan', 'platemetal_pink', 'platemetal_lime',
        'platemetal_yellow', 'platemetal_light_blue', 'platemetal_magenta',
        'platemetal_orange'
    )
    pneumatic_storage_exporter = @('pneumatic_storage_exporter')
    pneumatic_storage_importer = @('pneumatic_storage_importer')
    red_pylon_steel = @('red_pylon_steel')
}

$dependencyGroups = @{
    coal_eternal = 'Forgotten-room acquisition and fuel behavior'
    drive = 'Tape drive, satellites and supercomputer data chain'
    missile_lambda = 'Lambda launch system'
    orbital_assembly = 'Satellite/space-lab assembly chain'
    pile_rod = 'Chicago pile and PUREX fuel cycle'
    rtty_designator = 'Existing RTTY server system'
    satellite = 'Satellite, launch and data-processing chain'
    cargo_door = 'Door renderer and synchronized multiblock interaction'
    launchpad_lambda = 'Lambda launch system'
    launchpad_soyuz = 'Soyuz launch system'
    machine_rockmill = 'Modern power/fluid machine implementation'
    machine_satlink = 'Satellite and data-processing chain'
    machine_supercomputer = 'Drive processing chain'
    machine_tape_drive = 'Satellite raw-data chain'
    pile_block = 'Chicago pile simulation'
    pile_brick = 'Chicago pile construction'
    pile_device = 'Chicago pile loading, ventilation and control'
    platemetal = 'Flattened connected-texture building family'
    pneumatic_storage_exporter = 'Modern pneumatic logistics'
    pneumatic_storage_importer = 'Modern pneumatic logistics'
    red_pylon_steel = 'Dedicated pylon model and modern power-grid endpoint'
}

$allTargets = @($itemTargets.Values | ForEach-Object { $_ }) + @($blockTargets.Values | ForEach-Object { $_ })
$duplicateTargets = @($allTargets | Group-Object | Where-Object Count -gt 1 | ForEach-Object Name)
if ($duplicateTargets.Count -gt 0) {
    throw "Duplicate flattened target ids: $($duplicateTargets -join ', ')"
}

$itemSourcePath = Join-Path $ProjectRoot "src\main\java\com\reinhardt\hbm\registry\HbmItems.java"
$blockSourcePath = Join-Path $ProjectRoot "src\main\java\com\reinhardt\hbm\registry\HbmBlocks.java"
$itemSource = Get-Content -LiteralPath $itemSourcePath -Raw
$blockSource = Get-Content -LiteralPath $blockSourcePath -Raw
$english = Get-LanguageKeys (Join-Path $ProjectRoot "src\main\resources\assets\reinhardtshbm\lang\en_us.json")
$chinese = Get-LanguageKeys (Join-Path $ProjectRoot "src\main\resources\assets\reinhardtshbm\lang\zh_cn.json")
$recipeRoot = Join-Path $ProjectRoot "src\main\resources\data\reinhardtshbm\recipe"
$producedRecipeIds = Get-ProducedRecipeIds $recipeRoot
$anvilRecipePath = Join-Path $ProjectRoot "src\main\java\com\reinhardt\hbm\recipe\anvil\HbmAnvilRecipes.java"
if (Test-Path -LiteralPath $anvilRecipePath) {
    $anvilSource = Get-UncommentedJava $anvilRecipePath
    foreach ($match in [regex]::Matches($anvilSource, 'blockItem\(\s*"([a-z0-9_]+)"\s*,\s*\d+\s*\)')) {
        [void]$producedRecipeIds.Add("reinhardtshbm:$($match.Groups[1].Value)")
    }
}

# pile_block is an internal assembled-core state, not a player-crafted block.
# Count only the exact brick-to-core conversion used by the modern pile
# assembler; inventing a standalone recipe would expose an implementation
# block that upstream deliberately keeps unobtainable.
$pileBrickPath = Join-Path $ProjectRoot "src\main\java\com\reinhardt\hbm\block\PileMk2BrickBlock.java"
if (Test-Path -LiteralPath $pileBrickPath) {
    $pileBrickSource = Get-UncommentedJava $pileBrickPath
    if ($pileBrickSource -match 'level\.setBlock\(\s*place\s*,\s*HbmBlocks\.PILE_BLOCK\.get\(\)\.defaultBlockState\(\)' -and
            $pileBrickSource -match 'PileMk2Block\.KIND') {
        [void]$producedRecipeIds.Add('reinhardtshbm:pile_block')
    }
}
$acquisitionIds = [System.Collections.Generic.HashSet[string]]::new($producedRecipeIds)
$forgottenBrickPath = Join-Path $ProjectRoot "src\main\java\com\reinhardt\hbm\block\ForgottenBrickBlock.java"
if (Test-Path -LiteralPath $forgottenBrickPath) {
    $forgottenBrickSource = Get-UncommentedJava $forgottenBrickPath
    if ($forgottenBrickSource -match 'Variant\.HOLE' -and
            $forgottenBrickSource -match 'HbmItems\.COAL_ETERNAL\.get\(\)' -and
            $forgottenBrickSource -match 'Variant\.HOLE_EMPTY') {
        [void]$acquisitionIds.Add('reinhardtshbm:coal_eternal')
    }
}


# Runtime products are valid acquisition paths even though they cannot be
# represented as static recipe JSON. Validate the exact producer transitions
# rather than granting blanket exemptions.
$satelliteDataPath = Join-Path $ProjectRoot "src\main\java\com\reinhardt\hbm\satellite\SatelliteSavedData.java"
if (Test-Path -LiteralPath $satelliteDataPath) {
    $satelliteDataSource = Get-UncommentedJava $satelliteDataPath
    if ($satelliteDataSource -match 'produceData\(\s*DRIVE_DISK_EMPTY\s*,\s*DRIVE_DISK_FLIGHTDATA\s*\)') {
        [void]$acquisitionIds.Add('reinhardtshbm:drive_disk_flightdata')
    }
    if ($satelliteDataSource -match 'produceData\(\s*DRIVE_DISK_EMPTY\s*,\s*DRIVE_DISK_ORBITDATA\s*\)') {
        [void]$acquisitionIds.Add('reinhardtshbm:drive_disk_orbitdata')
    }
}

$pileRodPath = Join-Path $ProjectRoot "src\main\java\com\reinhardt\hbm\item\PileRodMk2Item.java"
if (Test-Path -LiteralPath $pileRodPath) {
    $pileRodSource = Get-UncommentedJava $pileRodPath
    $runtimeRodTargets = [ordered]@{
        pile_rod_pu239 = 'case\s+NU\s*->\s*HbmItems\.PILE_ROD_PU239\.get\(\)'
        pile_rod_rgp = 'case\s+PU239\s*->\s*HbmItems\.PILE_ROD_RGP\.get\(\)'
        pile_rod_waste = 'case\s+RGP\s*,\s*WASTE\s*,\s*THORIUM_FUEL\s*->\s*HbmItems\.PILE_ROD_WASTE\.get\(\)'
        pile_rod_thorium_fuel = 'case\s+THORIUM\s*->\s*HbmItems\.PILE_ROD_THORIUM_FUEL\.get\(\)'
    }
    foreach ($target in $runtimeRodTargets.Keys) {
        if ($pileRodSource -match $runtimeRodTargets[$target]) {
            [void]$acquisitionIds.Add("reinhardtshbm:$target")
        }
    }
}

$latestCommit = Get-GitValue $LatestRoot @('rev-parse', 'HEAD')
$latestTag = Get-GitValue $LatestRoot @('describe', '--tags', '--abbrev=0')
$latestDate = Get-GitValue $LatestRoot @('show', '-s', '--format=%ci', 'HEAD')
$baselineItemHash = Get-Sha256 (Join-Path $BaselineRoot "src\main\java\com\hbm\items\ModItems.java")
$baselineBlockHash = Get-Sha256 (Join-Path $BaselineRoot "src\main\java\com\hbm\blocks\ModBlocks.java")

$lines = [System.Collections.Generic.List[string]]::new()
$lines.Add('# Latest HBM 1.7.10 upstream content gap')
$lines.Add('')
$lines.Add('This report is read-only. It compares registry sources and inspects the modern port; it never rewrites the frozen legacy catalogs.')
$lines.Add('')
$lines.Add("- Baseline source: ``$BaselineRoot``")
$lines.Add("- Baseline ModItems SHA-256: ``$baselineItemHash``")
$lines.Add("- Baseline ModBlocks SHA-256: ``$baselineBlockHash``")
$lines.Add("- Latest source: ``$LatestRoot``")
$lines.Add("- Latest commit: ``$latestCommit`` ($latestDate)")
$lines.Add("- Latest reachable tag: ``$latestTag``")
$lines.Add("- Items: $($baselineItems.Count) -> $($latestItems.Count), +$($addedItems.Count) / -$($removedItems.Count)")
$lines.Add("- Blocks: $($baselineBlocks.Count) -> $($latestBlocks.Count), +$($addedBlocks.Count) / -$($removedBlocks.Count)")
$lines.Add('')
$lines.Add('## Added item families and flattened targets')
$lines.Add('')
$lines.Add('| Upstream id | Modern target | Dependency group | Registered | Model | EN | ZH | Acquisition | Complete |')
$lines.Add('|---|---|---|---:|---:|---:|---:|---:|---:|')
$incompleteTargets = [System.Collections.Generic.List[string]]::new()

foreach ($sourceId in $itemTargets.Keys) {
    foreach ($target in $itemTargets[$sourceId]) {
        $status = Get-ItemStatus $ProjectRoot $itemSource $english $chinese $acquisitionIds $target
        $lines.Add("| ``$sourceId`` | ``$target`` | $($dependencyGroups[$sourceId]) | $(Mark $status.Registered) | $(Mark $status.Model) | $(Mark $status.English) | $(Mark $status.Chinese) | $(Mark $status.Acquisition) | $(Mark $status.Complete) |")
        if (-not $status.Complete) { $incompleteTargets.Add("item:$target") }
    }
}

$lines.Add('')
$lines.Add('## Deliberately excluded optional integration')
$lines.Add('')
$lines.Add('- ``rtty_card`` is an OpenComputers-only bridge. The NeoForge port keeps the HBM-native RTTY/RoR protocol but intentionally does not register this OC card or an inert placeholder.')

$lines.Add('')
$lines.Add('## Added block families and flattened targets')
$lines.Add('')
$lines.Add('| Upstream id | Modern target | Dependency group | Registered | Blockstate | Block model | Item model | Loot | EN | ZH | Recipe output | Complete |')
$lines.Add('|---|---|---|---:|---:|---:|---:|---:|---:|---:|---:|---:|')

foreach ($sourceId in $blockTargets.Keys) {
    foreach ($target in $blockTargets[$sourceId]) {
        $status = Get-BlockStatus $ProjectRoot $blockSource $english $chinese $producedRecipeIds $target
        $lines.Add("| ``$sourceId`` | ``$target`` | $($dependencyGroups[$sourceId]) | $(Mark $status.Registered) | $(Mark $status.Blockstate) | $(Mark $status.BlockModel) | $(Mark $status.ItemModel) | $(Mark $status.Loot) | $(Mark $status.English) | $(Mark $status.Chinese) | $(Mark $status.Recipe) | $(Mark $status.Complete) |")
        if (-not $status.Complete) { $incompleteTargets.Add("block:$target") }
    }
}

$lines.Add('')
$lines.Add('## Removed upstream registry ids (review only)')
$lines.Add('')
$lines.Add('An upstream removal does not authorize deleting an already-ported modern item or block. Each entry requires a separate behavior and save-compatibility review.')
$lines.Add('')
$lines.Add("- Removed items ($($removedItems.Count)): ``$($removedItems -join '`, `')``")
$lines.Add("- Removed blocks ($($removedBlocks.Count)): ``$($removedBlocks -join '`, `')``")
$lines.Add('')
$lines.Add('## Porting boundary')
$lines.Add('')
$lines.Add('- Import content, behavior, recipes, assets, loot, localization and authoritative values from the current upstream reference.')
$lines.Add("- Keep the NeoForge port's modern energy, fluid, networking, event-driven machine and persistence frameworks.")
$lines.Add('- Player-facing metadata variants must become independent registry entries; internal machine states may remain block states when they are not obtainable inventory variants.')
$lines.Add('- A target is not considered complete until behavior, acquisition/recipe chain, assets, bilingual localization and relevant JEI visibility all pass their audits.')

$parent = Split-Path -Parent $OutputReport
if (-not (Test-Path -LiteralPath $parent)) {
    New-Item -ItemType Directory -Path $parent -Force | Out-Null
}
$utf8 = New-Object System.Text.UTF8Encoding($false)
[IO.File]::WriteAllLines($OutputReport, $lines, $utf8)

Write-Host "Latest upstream audit complete."
Write-Host "Items: $($baselineItems.Count) -> $($latestItems.Count), +$($addedItems.Count) / -$($removedItems.Count)"
Write-Host "Blocks: $($baselineBlocks.Count) -> $($latestBlocks.Count), +$($addedBlocks.Count) / -$($removedBlocks.Count)"
Write-Host "Report: $OutputReport"
if ($FailOnIncomplete -and $incompleteTargets.Count -gt 0) {
    throw "Latest upstream content remains incomplete: $($incompleteTargets -join ', ')"
}
if ($incompleteTargets.Count -eq 0) {
    Write-Host "All in-scope latest upstream targets are complete; OpenComputers-only rtty_card is explicitly excluded."
}
