param()

$ErrorActionPreference = 'Stop'
$root = (Resolve-Path (Join-Path $PSScriptRoot '..')).Path
$legacy = (Resolve-Path (Join-Path $PSScriptRoot '..\..\..\HBM_1.7.10')).Path
$assets = Join-Path $root 'src\main\resources\assets\reinhardtshbm'
$legacyAssets = Join-Path $legacy 'src\main\resources\assets\hbm'
$objDir = Join-Path $assets 'models\obj\trinkets'
$modelDir = Join-Path $assets 'models\block'
$textureDir = Join-Path $assets 'textures\models\trinkets'
$accessoryTextureDir = Join-Path $textureDir 'accessory'
$recipeDir = Join-Path $root 'src\main\resources\data\reinhardtshbm\recipe\shredder'
New-Item -ItemType Directory -Force -Path $objDir, $modelDir, $textureDir, $accessoryTextureDir, $recipeDir | Out-Null

$obj = Get-Content (Join-Path $legacyAssets 'models\trinkets\bobble.obj') -Raw
$obj = [regex]::Replace($obj, '(?m)^o .+$', { param($match) $match.Value + "`nusemtl bobble" })
Set-Content -Path (Join-Path $objDir 'bobble.obj') -Value ("mtllib bobble.mtl`n" + $obj) -Encoding ascii -NoNewline
@'
newmtl bobble
Ka 0.0 0.0 0.0
Kd 1.0 1.0 1.0
map_Kd reinhardtshbm:models/thegadget3_
'@ | Set-Content -Path (Join-Path $objDir 'bobble.mtl') -Encoding ascii

$groups = @('Body', 'Body17', 'Drillgon', 'Fumo', 'FumoHead', 'Head', 'Head17', 'Horn', 'LA', 'LA17', 'LL', 'LL17', 'PeepHat', 'PeepTail', 'Pellet', 'PelletShine', 'RA', 'RA17', 'RL', 'RL17', 'Socket', 'Fluoro', 'Glow')
foreach ($group in $groups) {
    $visibility = [ordered]@{}
    foreach ($candidate in $groups) { $visibility[$candidate] = $candidate -eq $group }
    $model = [ordered]@{
        loader = 'neoforge:obj'
        model = 'reinhardtshbm:models/obj/trinkets/bobble.obj'
        mtl_override = 'reinhardtshbm:models/obj/trinkets/bobble.mtl'
        automatic_culling = $false
        shade_quads = $true
        flip_v = $false
        visibility = $visibility
        textures = [ordered]@{ particle = 'reinhardtshbm:models/thegadget3_' }
    }
    $file = 'bobble_' + $group.ToLowerInvariant() + '.json'
    $model | ConvertTo-Json -Depth 4 | Set-Content -Path (Join-Path $modelDir $file) -Encoding ascii
}

$textures = @('socket.png', 'vaultboy.png', 'hbm.png', 'pellet.png', 'frizzle.png', 'vt.png', 'doctor17ph.png', 'thebluehat.png', 'pheo.png', 'adam29.png', 'uffr.png', 'vaer.png', 'nos.png', 'drillgon200.png', 'cirno.png', 'microwave.png', 'peep.png', 'mellowrpg8.png', 'mellowrpg8_glow.png', 'abel.png', 'abel_glow.png', 'glow.png')
foreach ($texture in $textures) {
    Copy-Item (Join-Path $legacyAssets ('textures\models\trinkets\' + $texture)) (Join-Path $textureDir $texture) -Force
}

@'
newmtl accessory
Ka 0.0 0.0 0.0
Kd 1.0 1.0 1.0
map_Kd reinhardtshbm:models/thegadget3_
'@ | Set-Content -Path (Join-Path $objDir 'bobble_accessory.mtl') -Encoding ascii

function Copy-BobbleAccessoryObj([string]$name, [string]$legacyModel) {
    $source = Get-Content (Join-Path $legacyAssets $legacyModel) -Raw
    $source = [regex]::Replace($source, '(?m)^o .+$', { param($match) $match.Value + "`nusemtl accessory" })
    Set-Content -Path (Join-Path $objDir ('bobble_accessory_' + $name + '.obj')) -Value ("mtllib bobble_accessory.mtl`n" + $source) -Encoding ascii -NoNewline
    return [regex]::Matches($source, '(?m)^o (.+)$') | ForEach-Object { $_.Groups[1].Value }
}

function Write-BobbleAccessoryModel([string]$name, [string[]]$objects, [string[]]$visible) {
    $visibility = [ordered]@{}
    foreach ($object in $objects) { $visibility[$object] = $visible -contains $object }
    $model = [ordered]@{
        loader = 'neoforge:obj'
        model = 'reinhardtshbm:models/obj/trinkets/bobble_accessory_' + $name + '.obj'
        mtl_override = 'reinhardtshbm:models/obj/trinkets/bobble_accessory.mtl'
        automatic_culling = $false
        shade_quads = $true
        flip_v = $false
        visibility = $visibility
        textures = [ordered]@{ particle = 'reinhardtshbm:models/thegadget3_' }
    }
    $model | ConvertTo-Json -Depth 4 | Set-Content -Path (Join-Path $modelDir ('bobble_accessory_' + $name + '.json')) -Encoding ascii
}

$hevObjects = Copy-BobbleAccessoryObj 'hev' 'models\armor\hev.obj'
Write-BobbleAccessoryModel 'hev' $hevObjects @('Head')
$hatObjects = Copy-BobbleAccessoryObj 'hat' 'models\armor\hat.obj'
Write-BobbleAccessoryModel 'hat' $hatObjects $hatObjects
$revolverObjects = Copy-BobbleAccessoryObj 'revolver' 'models\weapons\n_i_4_n_i.obj'
Write-BobbleAccessoryModel 'revolver' $revolverObjects @('FrameDark', 'Grip', 'FrameLight', 'Cylinder', 'Barrel')
$axeObjects = Copy-BobbleAccessoryObj 'shimmer_axe' 'models\shimmer_axe.obj'
Write-BobbleAccessoryModel 'shimmer_axe' $axeObjects $axeObjects
$fatmanObjects = Copy-BobbleAccessoryObj 'fatman' 'models\weapons\fatman.obj'
Write-BobbleAccessoryModel 'fatman' $fatmanObjects @('MiniNuke')
$dragonObjects = Copy-BobbleAccessoryObj 'sacred_dragon' 'models\weapons\sacred_dragon.obj'
Write-BobbleAccessoryModel 'sacred_dragon' $dragonObjects @('Stock', 'BarrelShort', 'Buckle', 'Lever')

$accessoryTextures = [ordered]@{
    'hev_helmet.png' = 'textures\armor\hev_helmet.png'
    'hat.png' = 'textures\armor\hat.png'
    'shimmer_axe.png' = 'textures\models\shimmer_axe.png'
    'fatman_mininuke.png' = 'textures\models\weapons\fatman_mininuke.png'
    'sacred_dragon.png' = 'textures\models\weapons\double_barrel_sacred_dragon.png'
    'revolver.png' = 'textures\models\weapons\n_i_4_n_i.png'
    'fluorescent_lamp.png' = 'textures\blocks\fluorescent_lamp.png'
}
foreach ($entry in $accessoryTextures.GetEnumerator()) {
    Copy-Item (Join-Path $legacyAssets $entry.Value) (Join-Path $accessoryTextureDir $entry.Key) -Force
}
Copy-Item (Join-Path $legacyAssets 'sounds\block\bobble.ogg') (Join-Path $assets 'sounds\block\bobble.ogg') -Force

$types = @(
    @('none', 'board_blank'), @('strength', 'bridge_bios'), @('perception', 'bridge_north'), @('endurance', 'bridge_south'), @('charisma', 'bridge_io'), @('intelligence', 'bridge_bus'), @('agility', 'bridge_chipset'), @('luck', 'bridge_cmos'), @('bob', 'cpu_socket'), @('frizzle', 'cpu_clock'), @('pu238', 'cpu_register'), @('vt', 'cpu_ext'), @('doc', 'cpu_cache'), @('bluehat', 'mem_16k_a'), @('pheo', 'mem_16k_b'), @('adam29', 'mem_16k_c'), @('uffr', 'mem_socket'), @('vaer', 'mem_16k_d'), @('nos', 'board_transistor'), @('drillgon', 'cpu_logic'), @('cirno', 'board_blank'), @('microwave', 'board_converter'), @('peep', 'card_board'), @('mellow', 'card_processor'), @('abel', 'cpu_register')
)
for ($index = 0; $index -lt $types.Count; $index++) {
    $type = $types[$index]
    $recipe = [ordered]@{
        type = 'reinhardtshbm:shredder'
        ingredient = [ordered]@{
            type = 'neoforge:components'
            items = @('reinhardtshbm:bobblehead')
            components = [ordered]@{
                'minecraft:custom_data' = [ordered]@{ type = $type[0] }
                'minecraft:custom_model_data' = $index
            }
        }
        result = [ordered]@{
            id = 'reinhardtshbm:scrap_plastic'
            components = [ordered]@{
                'minecraft:custom_data' = [ordered]@{ variant = $type[1] }
                'minecraft:custom_model_data' = @('board_blank', 'board_transistor', 'board_converter', 'bridge_north', 'bridge_south', 'bridge_io', 'bridge_bus', 'bridge_chipset', 'bridge_cmos', 'bridge_bios', 'cpu_register', 'cpu_clock', 'cpu_logic', 'cpu_cache', 'cpu_ext', 'cpu_socket', 'mem_socket', 'mem_16k_a', 'mem_16k_b', 'mem_16k_c', 'mem_16k_d', 'card_board', 'card_processor').IndexOf($type[1])
            }
        }
    }
    $recipe | ConvertTo-Json -Depth 6 | Set-Content -Path (Join-Path $recipeDir ('bobblehead_' + $type[0] + '.json')) -Encoding ascii
}
