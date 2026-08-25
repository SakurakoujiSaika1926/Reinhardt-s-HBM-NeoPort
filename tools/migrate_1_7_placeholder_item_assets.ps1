param(
    [string]$LegacyRoot = "..\..\HBM_1.7.10",
    [string]$ProjectRoot = "."
)

$ErrorActionPreference = "Stop"

$ProjectRoot = (Resolve-Path $ProjectRoot).Path
$LegacyRoot = (Resolve-Path $LegacyRoot).Path
$legacyItems = Join-Path $LegacyRoot "src\main\resources\assets\hbm\textures\items"
$models = Join-Path $ProjectRoot "src\main\resources\assets\reinhardtshbm\models\item"
$textures = Join-Path $ProjectRoot "src\main\resources\assets\reinhardtshbm\textures\item"

# 1.7.10 uses a number of IDs whose visual asset has a different name. Keep
# this map source-controlled: generation must never silently substitute steel.
$textureAliases = @{
    "achievement_icon" = "achievement_icon.questionmark"
    "black_hole" = "singularity_4"
    "bobmazon_hidden" = "bobmazon_special"
    "book_lore" = "book_pages"
    "bottle2_fritz_special" = "bottle2_fritz"
    "bottle2_korl_special" = "bottle2_korl"
    "cheese_quesadilla" = "quesadilla"
    "coltan_tool" = "coltass"
    "designator_range" = "designator_range_alt"
    "drone" = "drone.patrol"
    "euphemium_kit" = "code"
    "flask_infusion" = "flask_shield"
    "fmn" = "tablet"
    "glass" = "record_glass"
    "glitch" = "glitch_1"
    "grenade_extra" = "grenade_extra.frag_sleeve"
    "grenade_filling" = "grenade_filling.powder"
    "grenade_fuze" = "grenade_fuze.s3"
    "grenade_shell" = "grenade_shell.frag"
    "igniter" = "trigger"
    "item_secret" = "item_secret.aberrator"
    "kit_custom" = "kit"
    "lc" = "record_lc"
    "magnetron" = "magnetron_alt"
    "memory" = "mo8_anim"
    "missile_doomsday_rusted" = "missile_doomsday"
    "missile_soyuz" = "soyuz"
    "missile_soyuz_lander" = "soyuz_lander"
    "ncrpa_boots" = "rpa_boots"
    "ncrpa_helmet" = "rpa_helmet"
    "ncrpa_legs" = "rpa_legs"
    "ncrpa_plate" = "rpa_plate"
    "pellet_rtg_depleted" = "pellet_rtg_depleted.lead"
    "polaroid" = "polaroid_1"
    "powder_power" = "powder_energy_alt"
    "singularity_counter_resonant" = "singularity_alt"
    "singularity_super_heated" = "singularity_5"
    "ss" = "record_ss"
    "t51_boots" = "armor"
    "t51_helmet" = "armor"
    "t51_legs" = "armor"
    "t51_plate" = "armor"
    "toolbox" = "kit_toolbox"
    "vc" = "record_vc"
    "wand_k" = "wand"
    "weapon_pipe_lead" = "pipe_lead"
}

# Do not recreate aliases or gun-system-only components removed by project policy.
$retired = [System.Collections.Generic.HashSet[string]]::new([string[]]@(
    "ammo_container", "battery_advanced", "book_guide_book", "boltgun", "cell", "coin_siege",
    "fluid_barrel_v2", "fluid_tank_lead_v2", "fluid_tank_v2", "gun_egon", "gun_vortex", "jetpack_glider",
    "mechanism_launcher_1", "mechanism_launcher_2", "mechanism_revolver_1", "mechanism_revolver_2",
    "mechanism_rifle_1", "mechanism_rifle_2", "mechanism_special", "multitool_beam", "multitool_decon",
    "multitool_dig", "multitool_ext", "multitool_hit", "multitool_joule", "multitool_mega", "multitool_miner",
    "multitool_silk", "multitool_sky", "pellet_canister", "pellet_chlorophyte", "pellet_claws",
    "pellet_flechette", "pellet_meteorite", "sliding_blast_door_skin0", "sliding_blast_door_skin1",
    "sliding_blast_door_skin2", "weaponized_starblaster_cell", "weapon_bat", "weapon_bat_nail",
    "weapon_golf_club", "weapon_pipe_rusty", "weapon_saw"
))

$encoding = [System.Text.UTF8Encoding]::new($false)
$missing = [System.Collections.Generic.List[string]]::new()

Get-ChildItem -LiteralPath $models -Filter *.json | ForEach-Object {
    $model = Get-Content -LiteralPath $_.FullName -Raw | ConvertFrom-Json
    if ($model.textures.layer0 -ne "reinhardtshbm:item/legacy_placeholder") {
        return
    }

    $id = $_.BaseName
    if ($retired.Contains($id)) {
        return
    }

    $sourceId = if ($textureAliases.ContainsKey($id)) { $textureAliases[$id] } else { $id }
    $source = Join-Path $legacyItems "$sourceId.png"
    if (-not (Test-Path -LiteralPath $source)) {
        $missing.Add("$id -> $sourceId")
        return
    }

    Copy-Item -LiteralPath $source -Destination (Join-Path $textures "$id.png") -Force
    $json = @{ parent = "minecraft:item/generated"; textures = @{ layer0 = "reinhardtshbm:item/$id" } } | ConvertTo-Json -Depth 3
    [System.IO.File]::WriteAllText($_.FullName, $json + [Environment]::NewLine, $encoding)
}

if ($missing.Count -gt 0) {
    throw "Missing 1.7.10 item texture mappings:`n$($missing -join [Environment]::NewLine)"
}

Write-Host "Replaced all active legacy item fallback models with mapped 1.7.10 assets."
