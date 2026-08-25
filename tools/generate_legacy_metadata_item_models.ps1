param(
    [string]$ProjectRoot = "."
)

$ErrorActionPreference = "Stop"
$ProjectRoot = (Resolve-Path -LiteralPath $ProjectRoot).Path
$modelRoot = Join-Path $ProjectRoot "src\main\resources\assets\reinhardtshbm\models\item"
$encoding = [System.Text.UTF8Encoding]::new($false)

function Write-Json([string]$path, [object]$value) {
    [IO.File]::WriteAllText($path, ($value | ConvertTo-Json -Depth 8) + [Environment]::NewLine, $encoding)
}

function Write-VariantModels([string]$id, [string[]]$variants) {
    $base = [ordered]@{
        parent = "reinhardtshbm:item/$id.$($variants[0])"
        overrides = @()
    }
    for ($index = 1; $index -lt $variants.Count; $index++) {
        $base.overrides += [ordered]@{
            predicate = [ordered]@{ "minecraft:custom_model_data" = $index }
            model = "reinhardtshbm:item/$id.$($variants[$index])"
        }
    }
    Write-Json (Join-Path $modelRoot "$id.json") $base
    foreach ($variant in $variants) {
        Write-Json (Join-Path $modelRoot "$id.$variant.json") ([ordered]@{
            parent = "minecraft:item/generated"
            textures = [ordered]@{ layer0 = "reinhardtshbm:item/$id.$variant" }
        })
    }
}

Write-VariantModels "achievement_icon" @(
    "gofish", "acid", "balls", "digammasee", "digammafeel", "digammaknow", "digammakauaimoho",
    "digammaupontop", "digammaforourright", "questionmark"
)
Write-VariantModels "item_secret" @("canister", "controller", "selenium_steel", "aberrator", "folly")
Write-VariantModels "parts_legendary" @("tier1", "tier2", "tier3")

Write-Json (Join-Path $modelRoot "marshmallow.json") ([ordered]@{
    parent = "reinhardtshbm:item/marshmallow.raw"
    overrides = @(
        [ordered]@{
            predicate = [ordered]@{ "minecraft:custom_model_data" = 1 }
            model = "reinhardtshbm:item/marshmallow_roasted"
        }
    )
})
Write-Json (Join-Path $modelRoot "marshmallow.raw.json") ([ordered]@{
    parent = "minecraft:item/generated"
    textures = [ordered]@{ layer0 = "reinhardtshbm:item/marshmallow" }
})

$dyeNames = @(
    "black", "red", "green", "brown", "blue", "purple", "cyan", "silver",
    "gray", "pink", "lime", "yellow", "lightblue", "magenta", "orange", "white"
)
$dye = [ordered]@{
    parent = "minecraft:item/generated"
    textures = [ordered]@{
        layer0 = "reinhardtshbm:item/chemical_dye"
        layer1 = "reinhardtshbm:item/chemical_dye_overlay"
    }
    overrides = @()
}
for ($index = 1; $index -lt $dyeNames.Count; $index++) {
    $dye.overrides += [ordered]@{
        predicate = [ordered]@{ "minecraft:custom_model_data" = $index }
        model = "reinhardtshbm:item/chemical_dye.$($dyeNames[$index])"
    }
}
Write-Json (Join-Path $modelRoot "chemical_dye.json") $dye
foreach ($name in $dyeNames) {
    Write-Json (Join-Path $modelRoot "chemical_dye.$name.json") ([ordered]@{
        parent = "minecraft:item/generated"
        textures = [ordered]@{
            layer0 = "reinhardtshbm:item/chemical_dye"
            layer1 = "reinhardtshbm:item/chemical_dye_overlay"
        }
    })
}

Write-Host "Generated legacy ItemEnumMulti model overrides."
