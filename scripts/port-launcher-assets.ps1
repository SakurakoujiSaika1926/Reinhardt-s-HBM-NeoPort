param([string]$LegacyRoot = 'E:/MC/Modsource/HBM_1.7.10')
$ErrorActionPreference = 'Stop'
$projectRoot = Split-Path $PSScriptRoot -Parent
$assets = Join-Path $projectRoot 'src/main/resources/assets/reinhardtshbm'
$oldAssets = Join-Path $LegacyRoot 'src/main/resources/assets/hbm'
$utf8 = [System.Text.UTF8Encoding]::new($false)

# Mechanical OBJ/material conversion only. Vertex/UV/normal coordinates are not transformed.
function Export-Model([string]$source, [string]$name, [string]$texture, [string]$part = '') {
    $lines = [IO.File]::ReadAllLines($source)
    $geometry = [Collections.Generic.List[string]]::new()
    $geometry.Add('mtllib ' + $name + '.mtl')
    foreach ($line in $lines) { if ($line -match '^v[tn]?\s') { $geometry.Add($line) } }
    $geometry.Add('o ' + $name)
    $geometry.Add('usemtl launcher_material')
    $selected = $part -eq ''
    $faces = 0
    foreach ($line in $lines) {
        if ($line -match '^[og]\s+(.+)$') { $selected = $part -eq '' -or $Matches[1].Trim() -eq $part }
        if ($selected -and $line -match '^f\s') {
            if ($line -match '(\s|/)-\d') { throw "Relative OBJ indices require explicit conversion: $source" }
            $geometry.Add($line)
            $faces++
        }
    }
    if ($faces -eq 0) { throw "No faces for $name / $part" }
    $objDir = Join-Path $assets 'models/obj/launcher_port'
    $jsonDir = Join-Path $assets 'models/block/launcher_port'
    [IO.Directory]::CreateDirectory($objDir) | Out-Null
    [IO.Directory]::CreateDirectory($jsonDir) | Out-Null
    $textureFile = Join-Path $assets ('textures/' + $texture + '.png')
    if (-not (Test-Path -LiteralPath $textureFile)) {
        $sourceTexture = Join-Path $oldAssets ('textures/' + $texture + '.png')
        if (-not (Test-Path -LiteralPath $sourceTexture)) { throw "Missing legacy texture: $sourceTexture" }
        [IO.Directory]::CreateDirectory((Split-Path $textureFile -Parent)) | Out-Null
        Copy-Item -LiteralPath $sourceTexture -Destination $textureFile
    }
    [IO.File]::WriteAllLines((Join-Path $objDir ($name + '.obj')), $geometry, $utf8)
    [IO.File]::WriteAllText((Join-Path $objDir ($name + '.mtl')), "newmtl launcher_material`nKa 0 0 0`nKd 1 1 1`nmap_Kd reinhardtshbm:$texture`n", $utf8)
    $model = [ordered]@{ loader='neoforge:obj'; model="reinhardtshbm:models/obj/launcher_port/$name.obj"; automatic_culling=$false; shade_quads=$true; flip_v=$true; emissive_ambient=$false; textures=@{particle="reinhardtshbm:$texture"} }
    [IO.File]::WriteAllText((Join-Path $jsonDir ($name + '.json')), ($model | ConvertTo-Json -Depth 5), $utf8)
    Write-Output "$name : $faces faces"
}
$erector = Join-Path $assets 'models/obj/launch/launch_pad_erector.obj'
Export-Model $erector 'pad_base' 'models/launchpad/pad' 'Pad'
foreach ($factor in @('ABM','Micro','V2','Strong','Huge','Atlas')) {
    foreach ($part in @('Pad','Erector','Pivot','Rope')) {
        Export-Model $erector ($factor + '_' + $part).ToLowerInvariant() ('models/launchpad/erector_' + $factor.ToLowerInvariant()) ($factor + '_' + $part)
    }
}
$missiles = @{
    missile_test=@('missile_micro','missile_test'); missile_micro=@('missile_micro','missile_micro');
    missile_taint=@('missile_micro','missile_micro_taint'); missile_bhole=@('missile_micro','missile_micro_bhole');
    missile_schrabidium=@('missile_micro','missile_micro_schrab'); missile_emp=@('missile_micro','missile_micro_emp');
    missile_stealth=@('missile_stealth','missile_stealth'); missile_anti_ballistic=@('missile_abm','missile_abm');
    missile_generic=@('missile_v2','missile_v2'); missile_incendiary=@('missile_v2','missile_v2_inc');
    missile_cluster=@('missile_v2','missile_v2_cl'); missile_buster=@('missile_v2','missile_v2_bu'); missile_decoy=@('missile_v2','missile_v2_decoy');
    missile_strong=@('missile_strong','missile_strong'); missile_incendiary_strong=@('missile_strong','missile_strong_inc');
    missile_cluster_strong=@('missile_strong','missile_strong_cl'); missile_buster_strong=@('missile_strong','missile_strong_bu'); missile_emp_strong=@('missile_strong','missile_strong_emp');
    missile_burst=@('missile_huge','missile_huge'); missile_inferno=@('missile_huge','missile_huge_inc');
    missile_rain=@('missile_huge','missile_huge_cl'); missile_drill=@('missile_huge','missile_huge_bu');
    missile_nuclear=@('missile_atlas','missile_atlas_nuclear'); missile_nuclear_cluster=@('missile_atlas','missile_atlas_thermo');
    missile_volcano=@('missile_atlas','missile_atlas_tectonic'); missile_doomsday=@('missile_atlas','missile_atlas_doomsday');
    missile_doomsday_rusted=@('missile_atlas','missile_atlas_doomsday_weathered'); missile_shuttle=@('missileShuttle','missile_shuttle')
}
foreach ($entry in $missiles.GetEnumerator()) {
    Export-Model (Join-Path $oldAssets ('models/' + $entry.Value[0] + '.obj')) $entry.Key ('models/missile/' + $entry.Value[1])
}
