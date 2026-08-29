param(
    [string]$ProjectRoot = ".",
    [string]$LegacyRoot = "..\..\HBM_1.7.10"
)

$ErrorActionPreference = "Stop"
$ProjectRoot = (Resolve-Path -LiteralPath $ProjectRoot).Path
$LegacyRoot = if ([IO.Path]::IsPathRooted($LegacyRoot)) {
    $LegacyRoot
} else {
    Join-Path $ProjectRoot $LegacyRoot
}
$LegacyRoot = (Resolve-Path -LiteralPath $LegacyRoot).Path

& (Join-Path $PSScriptRoot "split_legacy_item_manifest.ps1") `
    -ProjectRoot $ProjectRoot `
    -LegacyRoot $LegacyRoot
& (Join-Path $PSScriptRoot "rebuild_legacy_block_manifest.ps1") `
    -ProjectRoot $ProjectRoot `
    -LegacyRoot $LegacyRoot
& (Join-Path $PSScriptRoot "rebuild_missile_part_metadata.ps1") `
    -ProjectRoot $ProjectRoot `
    -LegacyRoot $LegacyRoot

$legacyDir = Join-Path $ProjectRoot "src\main\resources\legacy\reinhardtshbm"
$itemManifest = Join-Path $legacyDir "item_catalog.txt"
$blockCatalog = Join-Path $legacyDir "block_catalog.txt"
$oldItemSource = Join-Path $LegacyRoot "src\main\java\com\hbm\items\ModItems.java"
$oldItemTextures = Join-Path $LegacyRoot "src\main\resources\assets\hbm\textures\items"
$newItemTextures = Join-Path $ProjectRoot "src\main\resources\assets\reinhardtshbm\textures\item"
$oldChinese = Join-Path $LegacyRoot "src\main\resources\assets\hbm\lang\zh_CN.lang"
$newChinese = Join-Path $ProjectRoot "src\main\resources\assets\reinhardtshbm\lang\zh_cn.json"
$reportPath = Join-Path $legacyDir "item_audit.tsv"
$remainingPortsPath = Join-Path $legacyDir "remaining_item_ports.csv"

function Read-Ids([string]$Path) {
    return @(Get-Content -LiteralPath $Path | ForEach-Object { $_.Trim() } |
        Where-Object { $_ -and -not $_.StartsWith('#') } | Sort-Object -Unique)
}

$items = Read-Ids $itemManifest
$blocks = [System.Collections.Generic.HashSet[string]]::new([string[]](Read-Ids $blockCatalog))
$source = Get-Content -LiteralPath $oldItemSource -Raw
$sourceWithoutComments = [regex]::Replace($source, '(?s)/\*.*?\*/', '')
$sourceWithoutComments = [regex]::Replace($sourceWithoutComments, '(?m)//.*$', '')
$oldChineseText = Get-Content -LiteralPath $oldChinese -Raw
$newChineseText = Get-Content -LiteralPath $newChinese -Raw
$legacyContentPath = Join-Path $ProjectRoot "src\main\java\com\reinhardt\hbm\registry\LegacyHbmContent.java"
$legacyContentText = Get-Content -LiteralPath $legacyContentPath -Raw
$hbmItemsPath = Join-Path $ProjectRoot "src\main\java\com\reinhardt\hbm\registry\HbmItems.java"
$hbmBlocksPath = Join-Path $ProjectRoot "src\main\java\com\reinhardt\hbm\registry\HbmBlocks.java"
$hbmFluidsPath = Join-Path $ProjectRoot "src\main\java\com\reinhardt\hbm\registry\HbmFluids.java"
$hbmItemsText = Get-Content -LiteralPath $hbmItemsPath -Raw
$hbmBlocksText = Get-Content -LiteralPath $hbmBlocksPath -Raw
$hbmFluidsText = Get-Content -LiteralPath $hbmFluidsPath -Raw

$initializers = @{}
foreach ($statement in ($sourceWithoutComments -split ';')) {
    $assignment = [regex]::Match($statement, '(?s)\b([a-z][a-z0-9_]*)\s*=')
    $registryId = [regex]::Match($statement, '\.setUnlocalizedName\(\s*"([a-z0-9_]+)"\s*\)')
    if (-not $assignment.Success -or -not $registryId.Success) {
        continue
    }

    $field = $assignment.Groups[1].Value
    $classMatch = [regex]::Match($statement, '\bnew\s+([A-Za-z0-9_$.]+)')
    $class = if ($classMatch.Success) {
        $classMatch.Groups[1].Value
    } elseif ($statement -match '\bItemCustomMissilePart\b|\bmp_') {
        # Cosmetic missile variants are ItemCustomMissilePart.copy() calls.
        "ItemCustomMissilePart"
    } else {
        "UNRESOLVED"
    }

    $id = $registryId.Groups[1].Value
    if (-not $initializers.ContainsKey($id)) {
        $initializers[$id] = [PSCustomObject]@{
            Field = $field
            Class = $class
            Expression = ($statement -replace '\s+', ' ').Trim()
        }
    }
}

# Items registered through LegacyHbmContent.createLegacyItem are real mapped
# registrations, not unresolved placeholders. Keep the audit aligned with the
# runtime registry instead of only scanning static HbmItems declarations.
$dynamicMappedIds = [System.Collections.Generic.HashSet[string]]::new()
$factoryStart = $legacyContentText.IndexOf('private static Item createLegacyItem')
$factoryEnd = $legacyContentText.IndexOf('private static Item createSimpleLegacyItem', $factoryStart)
if ($factoryStart -ge 0 -and $factoryEnd -gt $factoryStart) {
    $legacyFactory = $legacyContentText.Substring($factoryStart, $factoryEnd - $factoryStart)
    foreach ($idMatch in [regex]::Matches($legacyFactory, '"([a-z0-9_.-]+)"')) {
        [void]$dynamicMappedIds.Add($idMatch.Groups[1].Value)
    }
}

# Every catalog id must resolve to an explicit modern registration or a
# deliberately retired legacy id. There is no generic catalog-item fallback.
$explicitModernItemIds = [System.Collections.Generic.HashSet[string]]::new()
foreach ($match in [regex]::Matches($hbmItemsText, '"([a-z][a-z0-9_.-]+)"')) {
    [void]$explicitModernItemIds.Add($match.Groups[1].Value)
}

$formalBlockIds = [System.Collections.Generic.HashSet[string]]::new()
foreach ($match in [regex]::Matches($hbmBlocksText, '"([a-z][a-z0-9_.-]+)"')) {
    [void]$formalBlockIds.Add($match.Groups[1].Value)
}

# LegacyHbmContent's core list is the hand-maintained boundary between a
# formal HbmBlocks registration and createPlaceholderBlock().
$coreBlockMatch = [regex]::Match(
    $legacyContentText,
    'CORE_BLOCKS\s*=\s*Set\.of\((.*?)\);',
    [Text.RegularExpressions.RegexOptions]::Singleline
)
if ($coreBlockMatch.Success) {
    foreach ($match in [regex]::Matches($coreBlockMatch.Groups[1].Value, '"([a-z][a-z0-9_.-]+)"')) {
        [void]$formalBlockIds.Add($match.Groups[1].Value)
    }
}

$retiredIds = [System.Collections.Generic.HashSet[string]]::new()
foreach ($match in [regex]::Matches($legacyContentText, 'RETIRED_LEGACY_ITEM_IDS\s*=\s*Set\.of\((.*?)\);', [Text.RegularExpressions.RegexOptions]::Singleline)) {
    foreach ($idMatch in [regex]::Matches($match.Groups[1].Value, '"([a-z][a-z0-9_.-]+)"')) {
        [void]$retiredIds.Add($idMatch.Groups[1].Value)
    }
}
foreach ($match in [regex]::Matches($hbmItemsText, 'RETIRED_LEGACY_CATALOG_ITEM_IDS\s*=\s*Set\.of\((.*?)\);', [Text.RegularExpressions.RegexOptions]::Singleline)) {
    foreach ($idMatch in [regex]::Matches($match.Groups[1].Value, '"([a-z][a-z0-9_.-]+)"')) {
        [void]$retiredIds.Add($idMatch.Groups[1].Value)
    }
}

$legacyBehaviorIds = [System.Collections.Generic.HashSet[string]]::new()
$legacyFactoryStart = $legacyContentText.IndexOf('private static Item createLegacyItem')
$legacyFactoryEnd = $legacyContentText.IndexOf('private static Item createSimpleLegacyItem', $legacyFactoryStart)
if ($legacyFactoryStart -ge 0 -and $legacyFactoryEnd -gt $legacyFactoryStart) {
    $legacyFactory = $legacyContentText.Substring($legacyFactoryStart, $legacyFactoryEnd - $legacyFactoryStart)
    foreach ($match in [regex]::Matches($legacyFactory, '"([a-z][a-z0-9_.-]+)"')) {
        [void]$legacyBehaviorIds.Add($match.Groups[1].Value)
    }
}

$dynamicFormalLegacyClasses = [System.Collections.Generic.HashSet[string]]::new([string[]]@(
    'ItemCustomMissilePart',
    'ItemMachineUpgrade',
    'ItemStamp',
    'ItemMissile'
))

# These legacy IDs are registered as native BucketItem instances by
# HbmFluids.registerNeoForgeEntries().  Their source fluid, legacy liquid
# block and empty-bucket crafting remainder are all supplied by that path.
$dynamicFluidBucketIds = [System.Collections.Generic.HashSet[string]]::new()
foreach ($id in @('bucket_mud', 'bucket_acid', 'bucket_toxic', 'bucket_schrabidic_acid', 'bucket_sulfuric_acid')) {
    if ($hbmFluidsText.Contains('"' + $id + '"')) {
        [void]$dynamicFluidBucketIds.Add($id)
    }
}

# These are registered by HbmItems.registerLegacyMaterialItems() at bootstrap.
# They intentionally do not have one field per id, so a source-only audit must
# mirror that dispatch instead of reporting hundreds of formal resources as
# missing just because they have no individual field declaration.
function Test-DynamicFormalItem([string]$Id, [string]$LegacyClass) {
    if ($dynamicFluidBucketIds.Contains($Id)) {
        return $true
    }
    if ($dynamicFormalLegacyClasses.Contains($LegacyClass)) {
        return $true
    }
    if ($Id -match '^(ingot_|nugget_|powder_|plate_|billet_|coil_|wire_|pipe_|pipes_|tank_|casing_|circuit_|stamp_|upgrade_|fragment_|bedrock_ore_|raw_)') {
        return $true
    }
    if ($Id -in @('blade_meteorite', 'blade_titanium', 'blade_tungsten', 'turbine_titanium', 'turbine_tungsten',
            'flywheel_beryllium', 'sawblade')) {
        return $true
    }
    return $Id.StartsWith('crystal_') -and $Id -notin @('crystal_energy', 'crystal_horn', 'crystal_xen')
}

# 1.7.10 permitted item and block registries to share a textual name. 1.21
# cannot, so these fully ported blocks have stable block-specific ids while the
# original item ids remain available for their independent item variants.
$blockItemAliases = @{
    'hev_battery' = 'hev_battery_block'
    'ore_bedrock' = 'ore_bedrock_block'
    'pwr_fuel' = 'pwr_fuelrod'
}

$lines = @("id`tport_status`timplementation`tlegacy_field`tlegacy_class`told_texture`tnew_texture`told_zh_cn`tnew_zh_cn`tlegacy_block_item`tinitializer")
$remainingPorts = [System.Collections.Generic.List[object]]::new()
foreach ($id in $items) {
    $initializer = $initializers[$id]
    $field = if ($null -eq $initializer) { "" } else { $initializer.Field }
    $class = if ($null -eq $initializer) { "UNRESOLVED" } else { $initializer.Class }
    if ($dynamicMappedIds.Contains($id) -and $class -eq "UNRESOLVED") {
        $class = "LegacyHbmContent.$($class)"
    }
    $expression = if ($null -eq $initializer) { "" } else { $initializer.Expression }
    $oldTexture = Test-Path -LiteralPath (Join-Path $oldItemTextures "$id.png")
    $newTexture = Test-Path -LiteralPath (Join-Path $newItemTextures "$id.png")
    $oldLang = $oldChineseText -match [regex]::Escape("item.$id.name=")
    $newLang = $newChineseText -match [regex]::Escape('"item.reinhardtshbm.' + $id + '"')
    $legacyBlock = $blocks.Contains($id)
    $safeExpression = $expression.Replace("`t", ' ').Replace("`r", ' ').Replace("`n", ' ')
    if ($legacyBlock) {
        $blockId = if ($blockItemAliases.ContainsKey($id)) { $blockItemAliases[$id] } else { $id }
        if ($formalBlockIds.Contains($blockId)) {
            $portStatus = 'formal'
            $implementation = if ($blockId -eq $id) { 'HbmBlocks + BlockItem' } else { "HbmBlocks.$blockId + BlockItem (renamed namespace collision)" }
        } else {
            $portStatus = 'blockitem_pending'
            $implementation = 'LegacyHbmContent.createPlaceholderBlock + BlockItem'
        }
    } elseif ($explicitModernItemIds.Contains($id)) {
        $portStatus = 'formal'
        $implementation = 'HbmItems'
    } elseif (Test-DynamicFormalItem $id $class) {
        $portStatus = 'formal'
        $implementation = 'HbmItems dynamic material/specialized registration'
    } elseif ($retiredIds.Contains($id) -or $id.StartsWith('gun_')) {
        $portStatus = 'retired'
        $implementation = 'deliberately removed legacy content'
    } elseif ($legacyBehaviorIds.Contains($id) -or $dynamicMappedIds.Contains($id)) {
        $portStatus = 'legacy_fallback'
        $implementation = 'LegacyHbmContent.createLegacyItem'
    } else {
        $portStatus = 'unported'
        $implementation = 'missing explicit modern item registration'
    }
    $lines += "$id`t$portStatus`t$implementation`t$field`t$class`t$oldTexture`t$newTexture`t$oldLang`t$newLang`t$legacyBlock`t$safeExpression"
    if ($portStatus -in @('unported', 'legacy_fallback', 'blockitem_pending')) {
        $remainingPorts.Add([PSCustomObject]@{
            id = $id
            port_status = $portStatus
            implementation = $implementation
            legacy_field = $field
            legacy_class = $class
            old_texture = $oldTexture
            new_texture = $newTexture
            old_zh_cn = $oldLang
            new_zh_cn = $newLang
            legacy_block_item = $legacyBlock
            initializer = $safeExpression
        })
    }
}

$encoding = New-Object System.Text.UTF8Encoding($false)
[IO.File]::WriteAllLines($reportPath, $lines, $encoding)
$remainingPorts | Export-Csv -LiteralPath $remainingPortsPath -NoTypeInformation -Encoding utf8
Write-Host "Wrote item audit: $reportPath"
Write-Host "Wrote remaining item ports: $remainingPortsPath"
Write-Host "Audited 1.7.10 standalone items: $($items.Count)"
foreach ($status in 'formal', 'unported', 'legacy_fallback', 'retired', 'blockitem_pending') {
    $count = @($lines | Select-Object -Skip 1 | Where-Object { ($_ -split "`t")[1] -eq $status }).Count
    Write-Host "$status=$count"
}

if ($remainingPorts.Count -gt 0) {
    throw "Legacy item audit found $($remainingPorts.Count) incomplete port(s). See $remainingPortsPath."
}
