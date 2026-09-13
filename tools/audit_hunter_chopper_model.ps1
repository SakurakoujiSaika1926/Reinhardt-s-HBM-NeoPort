param(
    [string]$LegacyRoot = (Join-Path $PSScriptRoot '../../../HBM_1.7.10')
)

$ErrorActionPreference = 'Stop'
$currentRoot = Join-Path $PSScriptRoot '../src/main'
$oldRoot = Join-Path $LegacyRoot 'src/main'
$old = Get-Content (Join-Path $oldRoot 'java/com/hbm/render/model/ModelHunterChopper.java') -Raw
$current = Get-Content (Join-Path $currentRoot 'java/com/reinhardt/hbm/client/render/LegacyChopperModel.java') -Raw
$numberEvaluator = [System.Data.DataTable]::new()

function Read-Number([string]$expression) {
    # The old Techne source also writes pivots as e.g. 17F + 1.5F.
    if ($expression -notmatch '^[0-9Ff. +\-]+$') {
        throw "Unsupported numeric expression: $expression"
    }
    return [single]$numberEvaluator.Compute(($expression -replace '[Ff]', ''), $null)
}

$oldParts = [regex]::Matches($old, 'this\.(\w+)\.render\(scaleFactor\);') |
    ForEach-Object { $_.Groups[1].Value }
$newParts = @{}
foreach ($match in [regex]::Matches($current, 'part\(root, "(\w+)", ([^;]+)\);')) {
    $newParts[$match.Groups[1].Value] = $match.Groups[2].Value.Split(',')
}
if ($newParts.Count -ne $oldParts.Count) {
    throw "Part counts differ: old=$($oldParts.Count), current=$($newParts.Count)"
}

$checked = 0
foreach ($name in $oldParts) {
    if (!$newParts.ContainsKey($name)) { throw "Missing part: $name" }
    $ctor = [regex]::Match($old, "this\.$name = new ModelRenderer\(this, ([^)]+)\);")
    $box = [regex]::Match($old, "this\.$name\.addBox\(([^)]+)\);")
    $pivot = [regex]::Match($old, "this\.$name\.setRotationPoint\(([^)]+)\);")
    $rotation = [regex]::Match($old, "setRotation\((?:this\.)?$name, ([^)]+)\);")
    foreach ($field in @($ctor, $box, $pivot, $rotation)) {
        if (!$field.Success) { throw "Incomplete legacy part: $name" }
    }
    $mirror = [regex]::Match($old, "this\.$name\.mirror = true;")
    if ($mirror.Success -and $mirror.Index -lt $box.Index) {
        throw "Legacy cube is mirrored at creation: $name"
    }
    $expected = ($ctor.Groups[1].Value + ',' + $box.Groups[1].Value + ',' +
        $pivot.Groups[1].Value + ',' + $rotation.Groups[1].Value).Split(',')
    if ($expected.Count -ne $newParts[$name].Count) {
        throw "Argument count differs: $name"
    }
    for ($i = 0; $i -lt $expected.Count; $i++) {
        $a = Read-Number $expected[$i]
        $b = Read-Number $newParts[$name][$i]
        if ($a -ne $b) { throw "Part $name field $i differs: $a vs $b" }
        $checked++
    }
}
if ($current -match '\.mirror\(') {
    throw 'Modern model enables mirroring absent at legacy cube creation.'
}
if ($current -notmatch 'LayerDefinition\.create\(mesh, 256, 128\)') {
    throw 'Unexpected texture dimensions in model layer.'
}
$oldTexture = Join-Path $oldRoot 'resources/assets/hbm/textures/entity/chopper.png'
$texture = Join-Path $currentRoot 'resources/assets/reinhardtshbm/textures/entity/chopper.png'
if ((Get-FileHash $oldTexture).Hash -ne (Get-FileHash $texture).Hash) {
    throw 'Texture bytes differ from the legacy source.'
}

Write-Output "Hunter chopper parity: $($oldParts.Count) rendered parts, $checked matching UV/box/pivot/rotation values; texture bytes and cube mirroring match 1.7.10."
