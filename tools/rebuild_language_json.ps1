param(
    [string]$ProjectRoot = ".",
    [string]$LegacyRoot = "..\\..\\HBM_1.7.10"
)

$ErrorActionPreference = "Stop"
$ProjectRoot = (Resolve-Path -LiteralPath $ProjectRoot).Path
$LegacyRoot = if ([IO.Path]::IsPathRooted($LegacyRoot)) {
    $LegacyRoot
} else {
    Join-Path $ProjectRoot $LegacyRoot
}
$LegacyRoot = (Resolve-Path -LiteralPath $LegacyRoot).Path

function Read-LegacyLang([string]$Path) {
    $entries = @{}
    foreach ($line in [IO.File]::ReadAllLines($Path, [Text.Encoding]::UTF8)) {
        if (-not $line -or $line.StartsWith('#')) {
            continue
        }
        $separator = $line.IndexOf('=')
        if ($separator -lt 1) {
            continue
        }
        $entries[$line.Substring(0, $separator)] = $line.Substring($separator + 1)
    }
    return $entries
}

function Read-SalvageableJson([string]$Path) {
    $entries = [ordered]@{}
    foreach ($line in [IO.File]::ReadAllLines($Path, [Text.Encoding]::UTF8)) {
        $match = [regex]::Match($line, '^\s*"([^"\\]+)"\s*:\s*"(.*)"\s*,?\s*$')
        if (-not $match.Success) {
            continue
        }
        try {
            $parsed = ConvertFrom-Json -InputObject ('{"value":"' + $match.Groups[2].Value + '"}')
            $entries[$match.Groups[1].Value] = [string]$parsed.value
        } catch {
            # A malformed legacy value is replaced from the 1.7.10 source.
        }
    }
    return $entries
}

function Find-LegacyValue([string]$ModernKey, [hashtable]$LegacyEntries) {
    $unscoped = $ModernKey.Replace('.reinhardtshbm.', '.')
    $candidates = [Collections.Generic.List[string]]::new()
    $candidates.Add($unscoped)

    if ($ModernKey.StartsWith('item.reinhardtshbm.')) {
        $id = $ModernKey.Substring('item.reinhardtshbm.'.Length)
        $candidates.Add("item.$id.name")
    }
    if ($ModernKey.StartsWith('block.reinhardtshbm.')) {
        $id = $ModernKey.Substring('block.reinhardtshbm.'.Length)
        $candidates.Add("tile.$id.name")
    }
    if ($ModernKey.StartsWith('entity.reinhardtshbm.')) {
        $id = $ModernKey.Substring('entity.reinhardtshbm.'.Length)
        $candidates.Add("entity.$id.name")
    }
    if ($ModernKey.StartsWith('container.reinhardtshbm.')) {
        $id = $ModernKey.Substring('container.reinhardtshbm.'.Length)
        $candidates.Add("container.$id")
    }

    foreach ($candidate in $candidates) {
        if ($LegacyEntries.ContainsKey($candidate)) {
            return $LegacyEntries[$candidate]
        }
    }
    return $null
}

$assetRoot = Join-Path $ProjectRoot 'src\main\resources\assets\reinhardtshbm\lang'
$tabTranslations = [ordered]@{
    'itemGroup.reinhardtshbm' = 'Reinhardt''s HBM'
    'creative_tab.reinhardtshbm.miscellaneous' = 'HBM ' + [char]0x6742 + [char]0x9879
    'creative_tab.reinhardtshbm.parts' = 'HBM ' + [char]0x6750 + [char]0x6599
    'creative_tab.reinhardtshbm.blocks' = 'HBM ' + [char]0x65B9 + [char]0x5757
    'creative_tab.reinhardtshbm.tools' = 'HBM ' + [char]0x5DE5 + [char]0x5177
    'creative_tab.reinhardtshbm.power' = 'HBM ' + [char]0x7535 + [char]0x529B
    'creative_tab.reinhardtshbm.power_grid' = 'HBM ' + [char]0x7535 + [char]0x7F51
    'creative_tab.reinhardtshbm.generators' = 'HBM ' + [char]0x53D1 + [char]0x7535 + [char]0x673A
    'creative_tab.reinhardtshbm.power_storage' = 'HBM ' + [char]0x50A8 + [char]0x80FD
    'creative_tab.reinhardtshbm.building' = 'HBM ' + [char]0x5EFA + [char]0x6750
    'creative_tab.reinhardtshbm.legacy_items' = 'HBM ' + [char]0x65E7 + [char]0x7248 + [char]0x5360 + [char]0x4F4D + [char]0x7269 + [char]0x54C1
    'creative_tab.reinhardtshbm.legacy_blocks' = 'HBM ' + [char]0x65E7 + [char]0x7248 + [char]0x5360 + [char]0x4F4D + [char]0x65B9 + [char]0x5757
}

$encoding = [Text.UTF8Encoding]::new($false)
foreach ($locale in @(
        @{ Modern = 'en_us.json'; Legacy = 'en_US.lang' },
        @{ Modern = 'zh_cn.json'; Legacy = 'zh_CN.lang' }
    )) {
    $modernPath = Join-Path $assetRoot $locale.Modern
    $legacyPath = Join-Path $LegacyRoot ('src\main\resources\assets\hbm\lang\' + $locale.Legacy)
    $legacyEntries = Read-LegacyLang $legacyPath
    $existingEntries = Read-SalvageableJson $modernPath
    $rebuilt = [ordered]@{}

    foreach ($key in $existingEntries.Keys) {
        $legacyValue = Find-LegacyValue $key $legacyEntries
        if ($null -ne $legacyValue) {
            $rebuilt[$key] = $legacyValue
        } elseif ($locale.Modern -eq 'zh_cn.json' -and $tabTranslations.Contains($key)) {
            $rebuilt[$key] = $tabTranslations[$key]
        } else {
            $rebuilt[$key] = $existingEntries[$key]
        }
    }

    # Reintroduce keys whose old value line was malformed. Their keys still
    # appear in the raw file even though their values cannot be parsed.
    foreach ($line in [IO.File]::ReadAllLines($modernPath, [Text.Encoding]::UTF8)) {
        $keyMatch = [regex]::Match($line, '^\s*"([^"\\]+)"\s*:')
        if (-not $keyMatch.Success -or $rebuilt.Contains($keyMatch.Groups[1].Value)) {
            continue
        }
        $key = $keyMatch.Groups[1].Value
        $legacyValue = Find-LegacyValue $key $legacyEntries
        if ($null -ne $legacyValue) {
            $rebuilt[$key] = $legacyValue
        } elseif ($locale.Modern -eq 'zh_cn.json' -and $tabTranslations.Contains($key)) {
            $rebuilt[$key] = $tabTranslations[$key]
        } else {
            $rebuilt[$key] = $key
        }
    }

    $json = $rebuilt | ConvertTo-Json -Depth 4
    [IO.File]::WriteAllText($modernPath, $json + [Environment]::NewLine, $encoding)
    Write-Host "$($locale.Modern): $($rebuilt.Count) keys"
}
