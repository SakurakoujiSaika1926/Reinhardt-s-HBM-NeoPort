param(
    [string]$LegacyRoot = "..\..\HBM_1.12.2",
    [string]$ProjectRoot = "."
)

$ErrorActionPreference = "Stop"

$ProjectRoot = Resolve-Path $ProjectRoot
$LegacyRoot = Resolve-Path $LegacyRoot
$legacyAssets = Join-Path $LegacyRoot "src\main\resources\assets\hbm"
$legacyDir = Join-Path $ProjectRoot "src\main\resources\legacy\reinhardtshbm"
$assets = Join-Path $ProjectRoot "src\main\resources\assets\reinhardtshbm"
$data = Join-Path $ProjectRoot "src\main\resources\data"

function New-JsonFile($Path, $Object) {
    $dir = Split-Path -Parent $Path
    New-Item -ItemType Directory -Force -Path $dir | Out-Null
    $encoding = New-Object System.Text.UTF8Encoding($false)
    $json = $Object | ConvertTo-Json -Depth 12
    [System.IO.File]::WriteAllText($Path, $json + [Environment]::NewLine, $encoding)
}

function Copy-TextureOrFallback($Kind, $Id, $Fallback) {
    $legacyKind = if ($Kind -eq "item") { "items" } else { "blocks" }
    $source = Join-Path $legacyAssets "textures\$legacyKind\$Id.png"
    $target = Join-Path $assets "textures\$Kind\$Id.png"
    if (Test-Path $source) {
        Copy-Item -LiteralPath $source -Destination $target -Force
        return "reinhardtshbm:$Kind/$Id"
    }
    return $Fallback
}

$coreItems = @(
    "ingot_uranium", "ingot_u235", "ingot_u238", "ingot_plutonium", "ingot_schrabidium",
    "ingot_steel", "ingot_lead", "ingot_boron", "ingot_beryllium", "ingot_graphite",
    "ingot_advanced_alloy", "dosimeter", "geiger_counter"
)
$coreBlocks = @(
    "ore_uranium", "ore_uranium_scorched", "brick_concrete", "brick_concrete_mossy",
    "brick_concrete_cracked", "brick_concrete_broken", "reinforced_stone", "concrete"
)

New-Item -ItemType Directory -Force -Path (Join-Path $assets "models\item") | Out-Null
New-Item -ItemType Directory -Force -Path (Join-Path $assets "models\block") | Out-Null
New-Item -ItemType Directory -Force -Path (Join-Path $assets "blockstates") | Out-Null
New-Item -ItemType Directory -Force -Path (Join-Path $assets "textures\item") | Out-Null
New-Item -ItemType Directory -Force -Path (Join-Path $assets "textures\block") | Out-Null
New-Item -ItemType Directory -Force -Path (Join-Path $data "reinhardtshbm\loot_table\blocks") | Out-Null

Copy-Item -LiteralPath (Join-Path $legacyAssets "textures\items\ingot_steel.png") -Destination (Join-Path $assets "textures\item\legacy_placeholder.png") -Force
Copy-Item -LiteralPath (Join-Path $legacyAssets "textures\blocks\reinforced_stone.png") -Destination (Join-Path $assets "textures\block\legacy_placeholder.png") -Force

$legacyItems = Get-Content (Join-Path $legacyDir "items.txt")
$legacyBlocks = Get-Content (Join-Path $legacyDir "blocks.txt")
$blockSet = [System.Collections.Generic.HashSet[string]]::new()
foreach ($id in $legacyBlocks) { [void]$blockSet.Add($id) }

foreach ($id in $legacyItems) {
    if ($blockSet.Contains($id)) { continue }
    $texture = Copy-TextureOrFallback "item" $id "reinhardtshbm:item/legacy_placeholder"
    New-JsonFile (Join-Path $assets "models\item\$id.json") @{
        parent = "minecraft:item/generated"
        textures = @{ layer0 = $texture }
    }
}

foreach ($id in $legacyBlocks) {
    $texture = Copy-TextureOrFallback "block" $id "reinhardtshbm:block/legacy_placeholder"
    New-JsonFile (Join-Path $assets "models\block\$id.json") @{
        parent = "minecraft:block/cube_all"
        textures = @{ all = $texture }
    }
    New-JsonFile (Join-Path $assets "models\item\$id.json") @{
        parent = "reinhardtshbm:block/$id"
    }
    New-JsonFile (Join-Path $assets "blockstates\$id.json") @{
        variants = @{ "" = @{ model = "reinhardtshbm:block/$id" } }
    }
    New-JsonFile (Join-Path $data "reinhardtshbm\loot_table\blocks\$id.json") @{
        type = "minecraft:block"
        pools = @(@{
            rolls = 1
            entries = @(@{ type = "minecraft:item"; name = "reinhardtshbm:$id" })
            conditions = @(@{ condition = "minecraft:survives_explosion" })
        })
    }
}

New-JsonFile (Join-Path $data "minecraft\tags\block\mineable\pickaxe.json") @{
    replace = $false
    values = @($legacyBlocks | ForEach-Object { "reinhardtshbm:$_" })
}

New-JsonFile (Join-Path $data "minecraft\tags\block\needs_iron_tool.json") @{
    replace = $false
    values = @(($legacyBlocks | Where-Object { $_ -match "^(ore_|brick_|concrete|reinforced|machine_|block_)" }) | ForEach-Object { "reinhardtshbm:$_" })
}

Write-Host "Regenerated legacy model, blockstate, loot, and tag resources."
