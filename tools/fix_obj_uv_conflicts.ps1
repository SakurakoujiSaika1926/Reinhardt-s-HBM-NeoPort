param(
    [switch]$Check
)

$ErrorActionPreference = 'Stop'
$repoRoot = Split-Path -Parent $PSScriptRoot

function Resolve-ObjIndex([int]$index, [int]$count) {
    if ($index -gt 0) {
        return $index
    }
    return $count + $index + 1
}

function Get-RotatedKey([string[]]$values, [int]$offset) {
    $rotated = for ($index = 0; $index -lt $values.Count; $index++) {
        $values[($index + $offset) % $values.Count]
    }
    return $rotated -join '|'
}

function Read-ObjFaceData([string]$path) {
    $text = [IO.File]::ReadAllText($path)
    $lines = [regex]::Split($text, "`r?`n")
    $vertices = [Collections.Generic.List[string]]::new()
    $textureCoordinates = [Collections.Generic.List[string]]::new()
    $faces = [Collections.Generic.List[object]]::new()
    $objectName = ''

    for ($lineIndex = 0; $lineIndex -lt $lines.Count; $lineIndex++) {
        $line = $lines[$lineIndex].Trim()
        if ($line.StartsWith('v ')) {
            $vertices.Add(($line.Substring(2).Trim() -replace '\s+', ' '))
            continue
        }
        if ($line.StartsWith('vt ')) {
            $textureCoordinates.Add(($line.Substring(3).Trim() -replace '\s+', ' '))
            continue
        }
        if ($line.StartsWith('o ')) {
            $objectName = $line.Substring(2).Trim()
            continue
        }
        if (-not $line.StartsWith('f ')) {
            continue
        }

        $faceTokens = @($line.Substring(2).Trim() -split '\s+')
        $vertexIndices = [Collections.Generic.List[int]]::new()
        $positions = [Collections.Generic.List[string]]::new()
        $uvs = [Collections.Generic.List[string]]::new()
        foreach ($faceToken in $faceTokens) {
            $indices = $faceToken.Split('/')
            $vertexIndex = Resolve-ObjIndex ([int]$indices[0]) $vertices.Count
            $vertexIndices.Add($vertexIndex)
            $positions.Add($vertices[$vertexIndex - 1])
            if ($indices.Count -gt 1 -and -not [string]::IsNullOrWhiteSpace($indices[1])) {
                $textureIndex = Resolve-ObjIndex ([int]$indices[1]) $textureCoordinates.Count
                $uvs.Add($textureCoordinates[$textureIndex - 1])
            } else {
                $uvs.Add('')
            }
        }

        $positionArray = $positions.ToArray()
        $uvArray = $uvs.ToArray()
        $bestOffset = 0
        $bestGeometry = Get-RotatedKey $positionArray 0
        for ($offset = 1; $offset -lt $positionArray.Count; $offset++) {
            $candidate = Get-RotatedKey $positionArray $offset
            if ([string]::CompareOrdinal($candidate, $bestGeometry) -lt 0) {
                $bestGeometry = $candidate
                $bestOffset = $offset
            }
        }
        $faces.Add([pscustomobject]@{
            LineIndex = $lineIndex
            ObjectName = $objectName
            GeometryKey = $bestGeometry
            UvKey = Get-RotatedKey $uvArray $bestOffset
            VertexIndices = $vertexIndices.ToArray()
        })
    }

    return [pscustomobject]@{
        Text = $text
        Lines = $lines
        Faces = $faces
    }
}

function Find-UvConflicts($obj) {
    return @($obj.Faces |
        Group-Object GeometryKey |
        Where-Object { $_.Count -gt 1 -and (@($_.Group.UvKey | Sort-Object -Unique).Count -gt 1) })
}

$targets = @(
    [pscustomobject]@{
        RelativePath = 'src/main/resources/assets/reinhardtshbm/models/obj/machines/core_emitter.obj'
        ExpectedConflicts = 8
        Policy = 'RemoveLaterBodyFace'
    },
    [pscustomobject]@{
        RelativePath = 'src/main/resources/assets/reinhardtshbm/models/obj/launcher_port/missile_shuttle.obj'
        ExpectedConflicts = 162
        Policy = 'RemoveEarlierShellFace'
    }
)

foreach ($target in $targets) {
    $path = Join-Path $repoRoot $target.RelativePath
    $obj = Read-ObjFaceData $path
    $conflicts = Find-UvConflicts $obj
    if ($conflicts.Count -eq 0) {
        Write-Host "$($target.RelativePath): already normalized"
        continue
    }
    if ($conflicts.Count -ne $target.ExpectedConflicts) {
        throw "$($target.RelativePath): expected $($target.ExpectedConflicts) conflicting-UV face pairs, found $($conflicts.Count); refusing to edit"
    }

    $removeLines = [Collections.Generic.HashSet[int]]::new()
    foreach ($conflict in $conflicts) {
        if ($conflict.Count -ne 2) {
            throw "$($target.RelativePath): conflict group has $($conflict.Count) faces instead of one pair; refusing to edit"
        }
        $faces = @($conflict.Group | Sort-Object LineIndex)
        if ($target.Policy -eq 'RemoveLaterBodyFace') {
            # The 1.7.10 core-emitter OBJ adds isolated panel quads first, then repeats those
            # positions in the body. Keep the authored panel UV and remove the later body face.
            [void]$removeLines.Add($faces[1].LineIndex)
            continue
        }

        # The 1.7.10 shuttle Cylinder contains two complete coincident shells. The legacy
        # renderer draws the later shell last with an opaque texture, so preserve its UVs.
        $earlierShellFaces = @($faces | Where-Object {
            @($_.VertexIndices | Where-Object { $_ -lt 513 -or $_ -gt 617 }).Count -eq 0
        })
        $laterShellFaces = @($faces | Where-Object {
            @($_.VertexIndices | Where-Object { $_ -lt 618 -or $_ -gt 722 }).Count -eq 0
        })
        $validShellPair = $earlierShellFaces.Count -eq 1 -and $laterShellFaces.Count -eq 1 -and
                $earlierShellFaces[0].ObjectName -eq 'missile_shuttle' -and $laterShellFaces[0].ObjectName -eq 'missile_shuttle'
        if (-not $validShellPair) {
            $first = $faces[0].VertexIndices -join ','
            $second = $faces[1].VertexIndices -join ','
            throw "$($target.RelativePath): conflicting pair does not match the audited Cylinder shell ranges ($first versus $second); refusing to edit"
        }
        [void]$removeLines.Add($earlierShellFaces[0].LineIndex)
    }

    if ($Check) {
        throw "$($target.RelativePath): contains $($removeLines.Count) audited conflicting-UV duplicate faces"
    }

    $filtered = for ($lineIndex = 0; $lineIndex -lt $obj.Lines.Count; $lineIndex++) {
        if (-not $removeLines.Contains($lineIndex)) {
            $obj.Lines[$lineIndex]
        }
    }
    $newline = if ($obj.Text.Contains("`r`n")) { "`r`n" } else { "`n" }
    $newText = $filtered -join $newline
    [IO.File]::WriteAllText($path, $newText, [Text.UTF8Encoding]::new($false))
    Write-Host "$($target.RelativePath): removed $($removeLines.Count) conflicting-UV duplicate faces"
}
