param(
    [string]$LegacyRoot = "..\..\HBM_1.7.10",
    [string]$ProjectRoot = "."
)

$ErrorActionPreference = "Stop"
$ProjectRoot = (Resolve-Path -LiteralPath $ProjectRoot).Path
$LegacyRoot = if ([IO.Path]::IsPathRooted($LegacyRoot)) { $LegacyRoot } else { Join-Path $ProjectRoot $LegacyRoot }
$LegacyRoot = (Resolve-Path -LiteralPath $LegacyRoot).Path
$sourcePath = Join-Path $LegacyRoot "src\main\java\com\hbm\items\ModItems.java"
$outputPath = Join-Path $ProjectRoot "src\main\resources\legacy\reinhardtshbm\missile_part_metadata.tsv"

$source = Get-Content -LiteralPath $sourcePath -Raw
$source = [regex]::Replace($source, '(?s)/\*.*?\*/', '')
$source = [regex]::Replace($source, '(?m)//.*$', '')

$lines = @("id`ttitle`tauthor`twitty`trarity`thidden")
foreach ($statement in ($source -split ';')) {
    $id = [regex]::Match($statement, '\.setUnlocalizedName\(\s*"(mp_[a-z0-9_]+)"\s*\)')
    if (-not $id.Success) {
        continue
    }

    $title = [regex]::Match($statement, '\.setTitle\("([^"]*)"\)').Groups[1].Value
    $author = [regex]::Match($statement, '\.setAuthor\("([^"]*)"\)').Groups[1].Value
    $witty = [regex]::Match($statement, '\.setWittyText\("([^"]*)"\)').Groups[1].Value
    $rarity = [regex]::Match($statement, '\.setRarity\(Rarity\.([A-Z_]+)\)').Groups[1].Value
    $hidden = $statement -match '\.setCreativeTab\(null\)'

    $safe = @($id.Groups[1].Value, $title, $author, $witty, $rarity, $hidden.ToString().ToLowerInvariant()) | ForEach-Object {
        $_.Replace("`t", ' ').Replace("`r", ' ').Replace("`n", ' ')
    }
    $lines += ($safe -join "`t")
}

$ids = @($lines | Select-Object -Skip 1 | ForEach-Object { ($_ -split "`t", 2)[0] })
if ($ids.Count -ne 122 -or @($ids | Sort-Object -Unique).Count -ne $ids.Count) {
    throw "Expected 122 unique old missile part metadata records, parsed $($ids.Count)."
}

$encoding = New-Object System.Text.UTF8Encoding($false)
[IO.File]::WriteAllLines($outputPath, $lines, $encoding)
Write-Host "Rebuilt missile part metadata: $($ids.Count) entries"
