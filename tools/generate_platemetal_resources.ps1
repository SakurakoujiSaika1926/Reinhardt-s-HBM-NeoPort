param(
    [string]$ProjectRoot = ".",
    [string]$UpstreamRoot = "..\..\HBM_1.7.10新"
)

$ErrorActionPreference = "Stop"
$ProjectRoot = (Resolve-Path -LiteralPath $ProjectRoot).Path
$UpstreamRoot = if ([IO.Path]::IsPathRooted($UpstreamRoot)) {
    (Resolve-Path -LiteralPath $UpstreamRoot).Path
} else {
    (Resolve-Path -LiteralPath (Join-Path $ProjectRoot $UpstreamRoot)).Path
}

$variants = @(
    [pscustomobject]@{ Id = 'platemetal'; Upstream = 'base'; Dye = $null },
    [pscustomobject]@{ Id = 'platemetal_black'; Upstream = 'black'; Dye = 'black' },
    [pscustomobject]@{ Id = 'platemetal_white'; Upstream = 'white'; Dye = 'white' },
    [pscustomobject]@{ Id = 'platemetal_red'; Upstream = 'red'; Dye = 'red' },
    [pscustomobject]@{ Id = 'platemetal_green'; Upstream = 'green'; Dye = 'green' },
    [pscustomobject]@{ Id = 'platemetal_light_gray'; Upstream = 'light_gray'; Dye = 'light_gray' },
    [pscustomobject]@{ Id = 'platemetal_blue'; Upstream = 'blue'; Dye = 'blue' },
    [pscustomobject]@{ Id = 'platemetal_purple'; Upstream = 'purple'; Dye = 'purple' },
    [pscustomobject]@{ Id = 'platemetal_cyan'; Upstream = 'cyan'; Dye = 'cyan' },
    [pscustomobject]@{ Id = 'platemetal_pink'; Upstream = 'pink'; Dye = 'pink' },
    [pscustomobject]@{ Id = 'platemetal_lime'; Upstream = 'lime'; Dye = 'lime' },
    [pscustomobject]@{ Id = 'platemetal_yellow'; Upstream = 'yellow'; Dye = 'yellow' },
    [pscustomobject]@{ Id = 'platemetal_light_blue'; Upstream = 'light_blue'; Dye = 'light_blue' },
    [pscustomobject]@{ Id = 'platemetal_magenta'; Upstream = 'magenta'; Dye = 'magenta' },
    [pscustomobject]@{ Id = 'platemetal_orange'; Upstream = 'orange'; Dye = 'orange' }
)

$utf8 = New-Object System.Text.UTF8Encoding($false)

function Write-Json([string]$Path, [object]$Value) {
    $parent = Split-Path -Parent $Path
    if (-not (Test-Path -LiteralPath $parent)) {
        New-Item -ItemType Directory -Path $parent -Force | Out-Null
    }
    $json = $Value | ConvertTo-Json -Depth 20
    [IO.File]::WriteAllText($Path, $json + "`n", $utf8)
}

function Copy-Verified([string]$Source, [string]$Destination) {
    if (-not (Test-Path -LiteralPath $Source)) {
        throw "Missing upstream asset: $Source"
    }
    $parent = Split-Path -Parent $Destination
    if (-not (Test-Path -LiteralPath $parent)) {
        New-Item -ItemType Directory -Path $parent -Force | Out-Null
    }
    Copy-Item -LiteralPath $Source -Destination $Destination -Force
    $sourceHash = (Get-FileHash -LiteralPath $Source -Algorithm SHA256).Hash
    $destinationHash = (Get-FileHash -LiteralPath $Destination -Algorithm SHA256).Hash
    if ($sourceHash -ne $destinationHash) {
        throw "Asset hash mismatch after copy: $Source -> $Destination"
    }
}

$assets = Join-Path $ProjectRoot 'src\main\resources\assets\reinhardtshbm'
$data = Join-Path $ProjectRoot 'src\main\resources\data\reinhardtshbm'
$upstreamAssets = Join-Path $UpstreamRoot 'src\main\resources\assets\hbm'
$ids = @($variants | ForEach-Object { "reinhardtshbm:$($_.Id)" })
$connectTo = @($ids | ForEach-Object { [ordered]@{ block = $_ } })

foreach ($variant in $variants) {
    $id = $variant.Id
    Write-Json (Join-Path $assets "blockstates\$id.json") ([ordered]@{
        variants = [ordered]@{
            '' = [ordered]@{ model = "reinhardtshbm:block/$id" }
        }
    })
    Write-Json (Join-Path $assets "models\block\$id.json") ([ordered]@{
        parent = 'minecraft:block/cube_all'
        textures = [ordered]@{ all = "reinhardtshbm:block/$id" }
    })
    Write-Json (Join-Path $assets "models\item\$id.json") ([ordered]@{
        parent = "reinhardtshbm:block/$id"
    })
    Write-Json (Join-Path $data "loot_table\blocks\$id.json") ([ordered]@{
        type = 'minecraft:block'
        pools = @(
            [ordered]@{
                rolls = 1
                conditions = @([ordered]@{ condition = 'minecraft:survives_explosion' })
                entries = @([ordered]@{ type = 'minecraft:item'; name = "reinhardtshbm:$id" })
            }
        )
    })

    $textureSource = Join-Path $upstreamAssets "textures\blocks\platemetal.$($variant.Upstream).png"
    $ctmSource = Join-Path $upstreamAssets "textures\blocks\platemetal.$($variant.Upstream)_ct.png"
    Copy-Verified $textureSource (Join-Path $assets "textures\block\$id.png")
    Copy-Verified $ctmSource (Join-Path $assets "textures\block\${id}_ctm.png")
    Write-Json (Join-Path $assets "textures\block\$id.png.mcmeta") ([ordered]@{
        ctm = [ordered]@{
            ctm_version = 1
            type = 'CTM'
            textures = @("reinhardtshbm:block/${id}_ctm")
            extra = [ordered]@{ connect_to = $connectTo }
        }
    })

    if ($null -ne $variant.Dye) {
        Write-Json (Join-Path $data "recipe\platemetal\$id.json") ([ordered]@{
            type = 'minecraft:crafting_shaped'
            category = 'building'
            pattern = @('PPP', 'PDP', 'PPP')
            key = [ordered]@{
                P = [ordered]@{ item = 'reinhardtshbm:platemetal' }
                D = [ordered]@{ tag = "c:dyes/$($variant.Dye)" }
            }
            result = [ordered]@{ id = "reinhardtshbm:$id"; count = 8 }
        })
    }
}

Write-Json (Join-Path $data 'tags\item\platemetal.json') ([ordered]@{
    replace = $false
    values = $ids
})
Write-Json (Join-Path $data 'tags\block\platemetal.json') ([ordered]@{
    replace = $false
    values = $ids
})
Write-Json (Join-Path $data 'recipe\platemetal\reset_to_base.json') ([ordered]@{
    type = 'minecraft:crafting_shapeless'
    category = 'building'
    ingredients = @([ordered]@{ tag = 'reinhardtshbm:platemetal' })
    result = [ordered]@{ id = 'reinhardtshbm:platemetal' }
})

foreach ($sound in @(
        'platemetal_bar1.ogg', 'platemetal_bar2.ogg', 'platemetal_bar3.ogg',
        'platemetal_bar4.ogg', 'platemetal_bar5.ogg', 'platemetal_bar6.ogg',
        'platemetal_step.ogg')) {
    Copy-Verified (Join-Path $upstreamAssets "sounds\block\$sound") (Join-Path $assets "sounds\block\$sound")
}

Write-Host "Generated and verified $($variants.Count) flattened platemetal variants."
