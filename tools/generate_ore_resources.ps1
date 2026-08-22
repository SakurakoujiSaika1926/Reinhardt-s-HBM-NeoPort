param()

$ErrorActionPreference = "Stop"
Set-StrictMode -Version 2.0

$ProjectRoot = Split-Path -Parent (Split-Path -Parent $MyInvocation.MyCommand.Path)
$AssetsRoot = Join-Path $ProjectRoot "src\main\resources\assets\reinhardtshbm"
$DataRoot = Join-Path $ProjectRoot "src\main\resources\data"
$ModId = "reinhardtshbm"

Add-Type -AssemblyName System.Drawing
Add-Type -AssemblyName System.IO.Compression.FileSystem

function Write-Utf8File {
    param(
        [string] $Path,
        [string] $Content
    )

    $directory = Split-Path -Parent $Path
    if ($directory -and -not (Test-Path $directory)) {
        New-Item -ItemType Directory -Path $directory -Force | Out-Null
    }

    $encoding = New-Object System.Text.UTF8Encoding($false)
    [System.IO.File]::WriteAllText($Path, $Content, $encoding)
}

function Write-JsonFile {
    param(
        [string] $Path,
        [object] $Value,
        [int] $Depth = 16
    )

    $json = $Value | ConvertTo-Json -Depth $Depth
    Write-Utf8File -Path $Path -Content ($json + [Environment]::NewLine)
}

function Update-TagValues {
    param(
        [string] $Path,
        [string[]] $Add = @(),
        [string[]] $Remove = @()
    )

    $replace = $false
    $values = New-Object System.Collections.Generic.List[string]

    if (Test-Path $Path) {
        $json = Get-Content -Raw -Path $Path | ConvertFrom-Json
        if ($json.PSObject.Properties.Name -contains "replace") {
            $replace = [bool] $json.replace
        }
        if ($json.PSObject.Properties.Name -contains "values") {
            foreach ($value in $json.values) {
                $text = [string] $value
                if (-not $values.Contains($text)) {
                    $values.Add($text)
                }
            }
        }
    }

    foreach ($value in $Remove) {
        while ($values.Contains($value)) {
            [void] $values.Remove($value)
        }
    }

    foreach ($value in $Add) {
        if (-not $values.Contains($value)) {
            $values.Add($value)
        }
    }

    Write-JsonFile -Path $Path -Value ([ordered] @{
        replace = $replace
        values = @($values)
    })
}

function Load-MinecraftTexture {
    param([string] $EntryPath)

    $jar = Get-ChildItem -Path (Join-Path $ProjectRoot "build\moddev\artifacts") -Filter "*minecraft-resources.jar" |
            Select-Object -First 1
    if (-not $jar) {
        throw "Could not find the generated Minecraft resources jar. Run a Gradle build once first."
    }

    $zip = [System.IO.Compression.ZipFile]::OpenRead($jar.FullName)
    try {
        $entry = $zip.GetEntry($EntryPath)
        if (-not $entry) {
            throw "Missing $EntryPath in $($jar.FullName)"
        }

        $stream = $entry.Open()
        try {
            $bitmap = New-Object System.Drawing.Bitmap($stream)
            try {
                return New-Object System.Drawing.Bitmap($bitmap)
            } finally {
                $bitmap.Dispose()
            }
        } finally {
            $stream.Dispose()
        }
    } finally {
        $zip.Dispose()
    }
}

function Get-Luma {
    param([System.Drawing.Color] $Color)
    return (0.2126 * $Color.R) + (0.7152 * $Color.G) + (0.0722 * $Color.B)
}

function Get-ColorDistance {
    param(
        [System.Drawing.Color] $A,
        [System.Drawing.Color] $B
    )

    $dr = $A.R - $B.R
    $dg = $A.G - $B.G
    $db = $A.B - $B.B
    return [Math]::Sqrt(($dr * $dr) + ($dg * $dg) + ($db * $db))
}

function Clamp-Byte {
    param([double] $Value)
    return [Math]::Max(0, [Math]::Min(255, [int] [Math]::Round($Value)))
}

function Convert-ToDeepslateOre {
    param(
        [string] $SourcePath,
        [string] $TargetPath,
        [System.Drawing.Bitmap] $Stone,
        [System.Drawing.Bitmap] $Deepslate
    )

    if (-not (Test-Path $SourcePath)) {
        throw "Missing source ore texture: $SourcePath"
    }

    $source = New-Object System.Drawing.Bitmap($SourcePath)
    try {
        $target = New-Object System.Drawing.Bitmap($source.Width, $source.Height)
        try {
            for ($y = 0; $y -lt $source.Height; $y++) {
                for ($x = 0; $x -lt $source.Width; $x++) {
                    $src = $source.GetPixel($x, $y)
                    $stoneColor = $Stone.GetPixel(($x % $Stone.Width), ($y % $Stone.Height))
                    $deep = $Deepslate.GetPixel(($x % $Deepslate.Width), ($y % $Deepslate.Height))

                    $distance = Get-ColorDistance -A $src -B $stoneColor
                    $isStonePixel = $distance -lt 58

                    if ($isStonePixel) {
                        $delta = (Get-Luma -Color $src) - (Get-Luma -Color $stoneColor)
                        $r = Clamp-Byte ($deep.R + ($delta * 0.55))
                        $g = Clamp-Byte ($deep.G + ($delta * 0.55))
                        $b = Clamp-Byte ($deep.B + ($delta * 0.55))
                        $target.SetPixel($x, $y, [System.Drawing.Color]::FromArgb($src.A, $r, $g, $b))
                    } else {
                        $r = Clamp-Byte (($src.R * 0.92) + ($deep.R * 0.08))
                        $g = Clamp-Byte (($src.G * 0.92) + ($deep.G * 0.08))
                        $b = Clamp-Byte (($src.B * 0.92) + ($deep.B * 0.08))
                        $target.SetPixel($x, $y, [System.Drawing.Color]::FromArgb($src.A, $r, $g, $b))
                    }
                }
            }

            $directory = Split-Path -Parent $TargetPath
            if ($directory -and -not (Test-Path $directory)) {
                New-Item -ItemType Directory -Path $directory -Force | Out-Null
            }
            $target.Save($TargetPath, [System.Drawing.Imaging.ImageFormat]::Png)
        } finally {
            $target.Dispose()
        }
    } finally {
        $source.Dispose()
    }
}

function Write-BlockAssets {
    param([string] $Id)

    Write-JsonFile -Path (Join-Path $AssetsRoot "blockstates\$Id.json") -Value ([ordered] @{
        variants = [ordered] @{
            "" = [ordered] @{
                model = "$ModId`:block/$Id"
            }
        }
    })

    Write-JsonFile -Path (Join-Path $AssetsRoot "models\block\$Id.json") -Value ([ordered] @{
        parent = "minecraft:block/cube_all"
        textures = [ordered] @{
            all = "$ModId`:block/$Id"
        }
    })

    Write-JsonFile -Path (Join-Path $AssetsRoot "models\item\$Id.json") -Value ([ordered] @{
        parent = "$ModId`:block/$Id"
    })
}

function Write-BlockLoot {
    param(
        [string] $BlockId,
        [string] $DropId,
        [double] $MinCount = 1.0,
        [double] $MaxCount = 1.0,
        [bool] $Fortune = $true
    )

    if (-not $DropId) {
        $DropId = $BlockId
    }

    $dropFunctions = @()
    if (($MinCount -ne 1.0) -or ($MaxCount -ne 1.0)) {
        $dropFunctions += [ordered] @{
            function = "minecraft:set_count"
            add = $false
            count = [ordered] @{
                type = "minecraft:uniform"
                min = $MinCount
                max = $MaxCount
            }
        }
    }
    if ($Fortune) {
        $dropFunctions += [ordered] @{
            function = "minecraft:apply_bonus"
            enchantment = "minecraft:fortune"
            formula = "minecraft:ore_drops"
        }
    }
    $dropFunctions += [ordered] @{
        function = "minecraft:explosion_decay"
    }

    Write-JsonFile -Path (Join-Path $DataRoot "$ModId\loot_table\blocks\$BlockId.json") -Value ([ordered] @{
        type = "minecraft:block"
        random_sequence = "$ModId`:blocks/$BlockId"
        pools = @(
            [ordered] @{
                rolls = 1.0
                bonus_rolls = 0.0
                entries = @(
                    [ordered] @{
                        type = "minecraft:alternatives"
                        children = @(
                            [ordered] @{
                                type = "minecraft:item"
                                conditions = @(
                                    [ordered] @{
                                        condition = "minecraft:match_tool"
                                        predicate = [ordered] @{
                                            predicates = [ordered] @{
                                                "minecraft:enchantments" = @(
                                                    [ordered] @{
                                                        enchantments = "minecraft:silk_touch"
                                                        levels = [ordered] @{
                                                            min = 1
                                                        }
                                                    }
                                                )
                                            }
                                        }
                                    }
                                )
                                name = "$ModId`:$BlockId"
                            },
                            [ordered] @{
                                type = "minecraft:item"
                                functions = @($dropFunctions)
                                name = "$ModId`:$DropId"
                            }
                        )
                    }
                )
            }
        )
    })
}

function Write-SmeltRecipes {
    param(
        [string] $BlockId,
        [string] $OutputId,
        [double] $Experience
    )

    foreach ($kind in @("smelting", "blasting")) {
        $time = 200
        if ($kind -eq "blasting") {
            $time = 100
        }

        Write-JsonFile -Path (Join-Path $DataRoot "$ModId\recipe\$($kind)_$BlockId.json") -Value ([ordered] @{
            type = "minecraft:$kind"
            category = "misc"
            ingredient = [ordered] @{
                item = "$ModId`:$BlockId"
            }
            result = [ordered] @{
                id = "$ModId`:$OutputId"
            }
            experience = $Experience
            cookingtime = $time
        })
    }
}

function Write-OreWorldgen {
    param(
        [string] $FeatureId,
        [string] $StoneBlockId,
        [string] $DeepslateBlockId,
        [int] $Size,
        [int] $Count,
        [int] $MinY,
        [int] $MaxY
    )

    Write-JsonFile -Path (Join-Path $DataRoot "$ModId\worldgen\configured_feature\$FeatureId.json") -Value ([ordered] @{
        type = "minecraft:ore"
        config = [ordered] @{
            size = $Size
            discard_chance_on_air_exposure = 0.0
            targets = @(
                [ordered] @{
                    target = [ordered] @{
                        predicate_type = "minecraft:tag_match"
                        tag = "minecraft:stone_ore_replaceables"
                    }
                    state = [ordered] @{
                        Name = "$ModId`:$StoneBlockId"
                    }
                },
                [ordered] @{
                    target = [ordered] @{
                        predicate_type = "minecraft:tag_match"
                        tag = "minecraft:deepslate_ore_replaceables"
                    }
                    state = [ordered] @{
                        Name = "$ModId`:$DeepslateBlockId"
                    }
                }
            )
        }
    })

    Write-JsonFile -Path (Join-Path $DataRoot "$ModId\worldgen\placed_feature\$FeatureId.json") -Value ([ordered] @{
        feature = "$ModId`:$FeatureId"
        placement = @(
            [ordered] @{
                type = "minecraft:count"
                count = $Count
            },
            [ordered] @{
                type = "minecraft:in_square"
            },
            [ordered] @{
                type = "minecraft:height_range"
                height = [ordered] @{
                    type = "minecraft:uniform"
                    min_inclusive = [ordered] @{
                        absolute = $MinY
                    }
                    max_inclusive = [ordered] @{
                        absolute = $MaxY
                    }
                }
            },
            [ordered] @{
                type = "minecraft:biome"
            }
        )
    })

    Write-JsonFile -Path (Join-Path $DataRoot "$ModId\neoforge\biome_modifier\add_$FeatureId.json") -Value ([ordered] @{
        type = "neoforge:add_features"
        biomes = "#minecraft:is_overworld"
        features = "$ModId`:$FeatureId"
        step = "underground_ores"
    })
}

function Escape-JsonString {
    param([string] $Value)
    return $Value.Replace("\", "\\").Replace("""", "\""")
}

function U {
    param([string] $Value)
    return [regex]::Unescape($Value)
}

function Add-LangEntries {
    param(
        [string] $Path,
        [hashtable] $Entries
    )

    $content = [System.IO.File]::ReadAllText($Path, [System.Text.Encoding]::UTF8)
    $lines = New-Object System.Collections.Generic.List[string]

    foreach ($key in ($Entries.Keys | Sort-Object)) {
        $needle = '"' + $key + '"'
        if ($content.Contains($needle)) {
            continue
        }
        $lines.Add(('    "{0}":  "{1}"' -f $key, (Escape-JsonString -Value ([string] $Entries[$key]))))
    }

    if ($lines.Count -eq 0) {
        return
    }

    $insert = "," + [Environment]::NewLine + ($lines -join ("," + [Environment]::NewLine)) + [Environment]::NewLine
    $updated = [regex]::Replace($content, "\s*\}\s*$", $insert + "}")
    Write-Utf8File -Path $Path -Content $updated
}

$ores = @(
    @{ material = "uranium"; stone = "ore_uranium"; deep = "ore_deepslate_uranium"; drop = "raw_uranium"; output = "ingot_uranium"; xp = 6.0; en = "Deepslate Uranium Ore"; zh = (U "\u6df1\u5c42\u94c0\u77ff\u77f3"); tier = "iron"; world = $true; size = 5; count = 7; min = -48; max = 32 },
    @{ material = "uranium"; stone = "ore_uranium_scorched"; deep = "ore_deepslate_uranium_scorched"; drop = "raw_uranium"; output = "ingot_uranium"; xp = 6.0; en = "Deepslate Scorched Uranium Ore"; zh = (U "\u6df1\u5c42\u70e7\u7126\u94c0\u77ff\u77f3"); tier = "iron"; world = $false },
    @{ material = "thorium"; stone = "ore_thorium"; deep = "ore_deepslate_thorium"; drop = "raw_thorium"; output = "ingot_th232"; xp = 3.0; en = "Deepslate Thorium Ore"; zh = (U "\u6df1\u5c42\u948d\u77ff\u77f3"); tier = "iron"; world = $true; size = 5; count = 7; min = -48; max = 40 },
    @{ material = "titanium"; stone = "ore_titanium"; deep = "ore_deepslate_titanium"; drop = "raw_titanium"; output = "ingot_titanium"; xp = 3.0; en = "Deepslate Titanium Ore"; zh = (U "\u6df1\u5c42\u949b\u77ff\u77f3"); tier = "iron"; world = $true; size = 6; count = 8; min = -48; max = 48 },
    @{ material = "sulfur"; stone = "ore_sulfur"; deep = "ore_deepslate_sulfur"; drop = "sulfur"; output = $null; xp = 0.0; minDrop = 2.0; maxDrop = 4.0; en = "Deepslate Sulfur Ore"; zh = (U "\u6df1\u5c42\u786b\u78fa\u77ff\u77f3"); tier = "stone"; world = $true; size = 8; count = 5; min = -32; max = 48 },
    @{ material = "niter"; stone = "ore_niter"; deep = "ore_deepslate_niter"; drop = "niter"; output = $null; xp = 0.0; minDrop = 1.0; maxDrop = 2.0; en = "Deepslate Niter Ore"; zh = (U "\u6df1\u5c42\u785d\u77f3\u77ff\u77f3"); tier = "stone"; world = $true; size = 6; count = 6; min = -32; max = 48 },
    @{ material = "tungsten"; stone = "ore_tungsten"; deep = "ore_deepslate_tungsten"; drop = "raw_tungsten"; output = "ingot_tungsten"; xp = 6.0; en = "Deepslate Tungsten Ore"; zh = (U "\u6df1\u5c42\u94a8\u77ff\u77f3"); tier = "iron"; world = $true; size = 8; count = 10; min = -48; max = 48 },
    @{ material = "aluminium"; stone = "ore_aluminium"; deep = "ore_deepslate_aluminium"; drop = "raw_aluminium"; output = "ingot_aluminium"; xp = 2.5; en = "Deepslate Aluminium-Bearing Ore"; zh = (U "\u6df1\u5c42\u94dd\u77ff\u77f3"); tier = "stone"; world = $true; size = 6; count = 7; min = -32; max = 64 },
    @{ material = "fluorite"; stone = "ore_fluorite"; deep = "ore_deepslate_fluorite"; drop = "fluorite"; output = $null; xp = 0.0; minDrop = 2.0; maxDrop = 4.0; en = "Deepslate Fluorite Ore"; zh = (U "\u6df1\u5c42\u6c1f\u77f3\u77ff\u77f3"); tier = "stone"; world = $true; size = 4; count = 6; min = -32; max = 72 },
    @{ material = "lead"; stone = "ore_lead"; deep = "ore_deepslate_lead"; drop = "raw_lead"; output = "ingot_lead"; xp = 3.0; en = "Deepslate Lead Ore"; zh = (U "\u6df1\u5c42\u94c5\u77ff\u77f3"); tier = "iron"; world = $true; size = 9; count = 6; min = -48; max = 48 },
    @{ material = "beryllium"; stone = "ore_beryllium"; deep = "ore_deepslate_beryllium"; drop = "raw_beryllium"; output = "ingot_beryllium"; xp = 2.0; en = "Deepslate Beryllium Ore"; zh = (U "\u6df1\u5c42\u94cd\u77ff\u77f3"); tier = "iron"; world = $true; size = 4; count = 6; min = -48; max = 48 },
    @{ material = "lignite"; stone = "ore_lignite"; deep = "ore_deepslate_lignite"; drop = "lignite"; output = $null; xp = 0.0; en = "Deepslate Lignite Ore"; zh = (U "\u6df1\u5c42\u8910\u7164\u77ff\u77f3"); tier = "none"; world = $true; size = 24; count = 2; min = -16; max = 72 },
    @{ material = "asbestos"; stone = "ore_asbestos"; deep = "ore_deepslate_asbestos"; drop = "ingot_asbestos"; output = $null; xp = 0.0; en = "Deepslate Asbestos Ore"; zh = (U "\u6df1\u5c42\u77f3\u68c9\u77ff"); tier = "stone"; world = $true; size = 4; count = 2; min = -32; max = 32 },
    @{ material = "rare_earths"; stone = "ore_rare"; deep = "ore_deepslate_rare"; drop = "chunk_ore"; output = $null; xp = 0.0; en = "Deepslate Rare Earth Ore"; zh = (U "\u6df1\u5c42\u7a00\u571f\u77ff\u77f3"); tier = "iron"; world = $true; size = 5; count = 6; min = -48; max = 32 },
    @{ material = "cobalt"; stone = "ore_cobalt"; deep = "ore_deepslate_cobalt"; drop = "raw_cobalt"; output = "ingot_cobalt"; xp = 2.0; en = "Deepslate Cobalt Ore"; zh = (U "\u6df1\u5c42\u94b4\u77ff\u77f3"); tier = "diamond"; world = $true; size = 4; count = 2; min = -64; max = 16 },
    @{ material = "cinnabar"; stone = "ore_cinnabar"; deep = "ore_deepslate_cinnabar"; drop = "cinnabar"; output = $null; xp = 0.0; minDrop = 1.0; maxDrop = 2.0; en = "Deepslate Cinnabar Ore"; zh = (U "\u6df1\u5c42\u6731\u7802\u77ff\u77f3"); tier = "stone"; world = $true; size = 4; count = 1; min = -48; max = 24 },
    @{ material = "coltan"; stone = "ore_coltan"; deep = "ore_deepslate_coltan"; drop = "raw_coltan"; output = "fragment_coltan"; xp = 20.0; en = "Deepslate Coltan Ore"; zh = (U "\u6df1\u5c42\u94b6\u94bd\u94c1\u77ff"); tier = "diamond"; world = $true; size = 4; count = 2; min = -32; max = 48 },
    @{ material = "schrabidium"; stone = "ore_schrabidium"; deep = "ore_deepslate_schrabidium"; drop = "raw_schrabidium"; output = "ingot_schrabidium"; xp = 128.0; en = "Deepslate Schrabidium Ore"; zh = (U "\u6df1\u5c42Sa326\u77ff\u77f3"); tier = "diamond"; world = $false }
)

$oreClusters = @(
    @{ block = "cluster_copper"; drop = "crystal_copper" }
)

$stoneTexture = Load-MinecraftTexture -EntryPath "assets/minecraft/textures/block/stone.png"
$deepslateTexture = Load-MinecraftTexture -EntryPath "assets/minecraft/textures/block/deepslate.png"
$rawSmeltRecipes = New-Object System.Collections.Generic.HashSet[string]
try {
    $textureRoot = Join-Path $AssetsRoot "textures\block"
    foreach ($ore in $ores) {
        $source = Join-Path $textureRoot ($ore.stone + ".png")
        $target = Join-Path $textureRoot ($ore.deep + ".png")
        Convert-ToDeepslateOre -SourcePath $source -TargetPath $target -Stone $stoneTexture -Deepslate $deepslateTexture

        Write-BlockAssets -Id $ore.stone
        Write-BlockAssets -Id $ore.deep
        $minDrop = 1.0
        $maxDrop = 1.0
        if ($ore.ContainsKey("minDrop")) {
            $minDrop = [double] $ore.minDrop
        }
        if ($ore.ContainsKey("maxDrop")) {
            $maxDrop = [double] $ore.maxDrop
        }

        Write-BlockLoot -BlockId $ore.stone -DropId $ore.drop -MinCount $minDrop -MaxCount $maxDrop
        Write-BlockLoot -BlockId $ore.deep -DropId $ore.drop -MinCount $minDrop -MaxCount $maxDrop

        if ($ore.output) {
            Write-SmeltRecipes -BlockId $ore.stone -OutputId $ore.output -Experience ([double] $ore.xp)
            Write-SmeltRecipes -BlockId $ore.deep -OutputId $ore.output -Experience ([double] $ore.xp)

            $dropId = [string] $ore.drop
            if ($dropId.StartsWith("raw_") -and $rawSmeltRecipes.Add($dropId)) {
                Write-SmeltRecipes -BlockId $dropId -OutputId $ore.output -Experience ([double] $ore.xp)
            }
        }
    }

    foreach ($cluster in $oreClusters) {
        Write-BlockAssets -Id $cluster.block
        Write-BlockLoot -BlockId $cluster.block -DropId $cluster.drop
    }
} finally {
    $stoneTexture.Dispose()
    $deepslateTexture.Dispose()
}

$allOreBlocks = New-Object System.Collections.Generic.List[string]
$stoneTierBlocks = New-Object System.Collections.Generic.List[string]
$ironTierBlocks = New-Object System.Collections.Generic.List[string]
$diamondTierBlocks = New-Object System.Collections.Generic.List[string]
$noTierBlocks = New-Object System.Collections.Generic.List[string]

foreach ($ore in $ores) {
    foreach ($block in @($ore.stone, $ore.deep)) {
        $id = "$ModId`:$block"
        $allOreBlocks.Add($id)
        switch ($ore.tier) {
            "stone" { $stoneTierBlocks.Add($id) }
            "iron" { $ironTierBlocks.Add($id) }
            "diamond" { $diamondTierBlocks.Add($id) }
            default { $noTierBlocks.Add($id) }
        }
    }
}

Update-TagValues -Path (Join-Path $DataRoot "minecraft\tags\block\mineable\pickaxe.json") -Add @($allOreBlocks)
Update-TagValues -Path (Join-Path $DataRoot "minecraft\tags\block\needs_stone_tool.json") -Add @($stoneTierBlocks)
Update-TagValues -Path (Join-Path $DataRoot "minecraft\tags\block\needs_iron_tool.json") -Add @($ironTierBlocks) -Remove @($stoneTierBlocks + $diamondTierBlocks + $noTierBlocks)
Update-TagValues -Path (Join-Path $DataRoot "minecraft\tags\block\needs_diamond_tool.json") -Add @($diamondTierBlocks)

$oreGroups = @{}
foreach ($ore in $ores) {
    if (-not $oreGroups.ContainsKey($ore.material)) {
        $oreGroups[$ore.material] = New-Object System.Collections.Generic.List[string]
    }
    $oreGroups[$ore.material].Add("$ModId`:$($ore.stone)")
    $oreGroups[$ore.material].Add("$ModId`:$($ore.deep)")
}

$commonOreTagRefs = New-Object System.Collections.Generic.List[string]
foreach ($material in ($oreGroups.Keys | Sort-Object)) {
    $values = @($oreGroups[$material])
    Update-TagValues -Path (Join-Path $DataRoot "c\tags\block\ores\$material.json") -Add $values
    Update-TagValues -Path (Join-Path $DataRoot "c\tags\item\ores\$material.json") -Add $values
    $commonOreTagRefs.Add("#c:ores/$material")
}

Update-TagValues -Path (Join-Path $DataRoot "c\tags\block\ores.json") -Add @($commonOreTagRefs)
Update-TagValues -Path (Join-Path $DataRoot "c\tags\item\ores.json") -Add @($commonOreTagRefs)

$ingotTags = @{
    aluminium = "ingot_aluminium"
    asbestos = "ingot_asbestos"
    cobalt = "ingot_cobalt"
    coltan = "fragment_coltan"
    copper = "ingot_copper"
    th232 = "ingot_th232"
    thorium = "ingot_th232"
    titanium = "ingot_titanium"
    tungsten = "ingot_tungsten"
}

foreach ($tag in ($ingotTags.Keys | Sort-Object)) {
    Update-TagValues -Path (Join-Path $DataRoot "c\tags\item\ingots\$tag.json") -Add @("$ModId`:$($ingotTags[$tag])")
}

foreach ($ore in $ores) {
    if ($ore.world) {
        Write-OreWorldgen `
            -FeatureId $ore.stone `
            -StoneBlockId $ore.stone `
            -DeepslateBlockId $ore.deep `
            -Size ([int] $ore.size) `
            -Count ([int] $ore.count) `
            -MinY ([int] $ore.min) `
            -MaxY ([int] $ore.max)
    }
}

$enEntries = @{}
$zhEntries = @{}
foreach ($ore in $ores) {
    $enEntries["block.$ModId.$($ore.deep)"] = $ore.en
    $zhEntries["block.$ModId.$($ore.deep)"] = $ore.zh
}

Add-LangEntries -Path (Join-Path $AssetsRoot "lang\en_us.json") -Entries $enEntries
Add-LangEntries -Path (Join-Path $AssetsRoot "lang\zh_cn.json") -Entries $zhEntries

Write-Host "Generated $($ores.Count) deepslate ore textures and synchronized ore resources."
