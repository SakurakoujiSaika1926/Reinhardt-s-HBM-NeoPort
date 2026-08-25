param(
    [string]$ProjectRoot = ".",
    [string]$LegacyRoot = "..\..\HBM_1.7.10"
)

$ErrorActionPreference = "Stop"
$ProjectRoot = Resolve-Path $ProjectRoot
$LegacyRoot = if ([IO.Path]::IsPathRooted($LegacyRoot)) {
    $LegacyRoot
} else {
    Join-Path $ProjectRoot $LegacyRoot
}
$legacyDir = Join-Path $ProjectRoot "src\main\resources\legacy\reinhardtshbm"
$catalogPath = Join-Path $legacyDir "item_catalog.txt"
$placeholderPath = Join-Path $legacyDir "items.txt"
$blocksPath = Join-Path $legacyDir "blocks.txt"
$rebuildScript = Join-Path $PSScriptRoot "rebuild_legacy_item_manifest.ps1"
& $rebuildScript -ProjectRoot $ProjectRoot -LegacyRoot $LegacyRoot

$hbmItemsPath = Join-Path $ProjectRoot "src\main\java\com\reinhardt\hbm\registry\HbmItems.java"
$hbmBlocksPath = Join-Path $ProjectRoot "src\main\java\com\reinhardt\hbm\registry\HbmBlocks.java"
$legacyContentPath = Join-Path $ProjectRoot "src\main\java\com\reinhardt\hbm\registry\LegacyHbmContent.java"

if (-not (Test-Path -LiteralPath $catalogPath)) {
    Copy-Item -LiteralPath $placeholderPath -Destination $catalogPath
}

$catalog = @(Get-Content -LiteralPath $catalogPath | ForEach-Object { $_.Trim() } |
    Where-Object { $_ -and -not $_.StartsWith("#") } | Sort-Object -Unique)
$blocks = @(Get-Content -LiteralPath $blocksPath | ForEach-Object { $_.Trim() } |
    Where-Object { $_ -and -not $_.StartsWith("#") } | Sort-Object -Unique)
$hbmItems = Get-Content -LiteralPath $hbmItemsPath -Raw
$hbmBlocks = Get-Content -LiteralPath $hbmBlocksPath -Raw
$legacyContent = Get-Content -LiteralPath $legacyContentPath -Raw
$retiredText = [regex]::Match(
    $legacyContent,
    'RETIRED_LEGACY_ITEM_IDS = Set\.of\((.*?)\);',
    [Text.RegularExpressions.RegexOptions]::Singleline
).Groups[1].Value
$retired = [System.Collections.Generic.HashSet[string]]::new()
foreach ($match in [regex]::Matches($retiredText, '"([a-z0-9_.-]+)"')) {
    [void]$retired.Add($match.Groups[1].Value)
}

# Static DeferredItem declarations are the authoritative migrated item list.
$core = [System.Collections.Generic.HashSet[string]]::new()
foreach ($match in [regex]::Matches(
        $hbmItems,
        'public\s+static\s+final\s+DeferredItem<[^;]+?\s+([A-Z0-9_]+)\s*=\s*(.*?);',
        [Text.RegularExpressions.RegexOptions]::Singleline)) {
    $id = [regex]::Match($match.Groups[2].Value, '"([a-z0-9_.-]+)"').Groups[1].Value
    if ($id) { [void]$core.Add($id) }
}

# A compact set is used for ordinary 1.7.10 Item registrations. Unlike the
# static DeferredItem declarations above, these are registered in a loop.
foreach ($setName in @('PORTED_PLAIN_ITEM_IDS', 'PORTED_LEGACY_GAMEPLAY_ITEM_IDS', 'PORTED_LEGACY_LORE_ITEM_IDS',
        'PORTED_LEGACY_RBMK_FUEL_IDS', 'PORTED_LEGACY_RBMK_PELLET_IDS')) {
    $setText = [regex]::Match(
        $hbmItems,
        ($setName + '\s*=\s*Set\.of\((.*?)\);'),
        [Text.RegularExpressions.RegexOptions]::Singleline
    ).Groups[1].Value
    foreach ($match in [regex]::Matches($setText, '"([a-z0-9_.-]+)"')) {
        [void]$core.Add($match.Groups[1].Value)
    }
}

# LegacyHbmContent is the authoritative registry for items which are already
# ported through the old class mapping. These IDs are intentionally registered
# dynamically because their constructor depends on the legacy ID. Do not put
# them back into items.txt just because they are not static DeferredItems.
$factoryStart = $legacyContent.IndexOf('private static Item createLegacyItem')
$factoryEnd = $legacyContent.IndexOf('private static Item createSimpleLegacyItem', $factoryStart)
if ($factoryStart -ge 0 -and $factoryEnd -gt $factoryStart) {
    # Extract the complete method by source boundaries. Matching Java braces
    # with a single regex is brittle because the switch contains nested blocks
    # and multiline case labels.
    $legacyFactory = $legacyContent.Substring($factoryStart, $factoryEnd - $factoryStart)
    foreach ($idMatch in [regex]::Matches($legacyFactory, '"([a-z0-9_.-]+)"')) {
        [void]$core.Add($idMatch.Groups[1].Value)
    }
}

# HbmItems registers every remaining catalog item as a formal item. Keep the
# unresolved manifest empty for those IDs; it is consumed only by the legacy
# bootstrap and must not duplicate the formal registry.
if ($hbmItems -match 'registerRemainingLegacyCatalogItems') {
    foreach ($id in $catalog) {
        [void]$core.Add($id)
    }
}

# These declarations exist in 1.7.10 but are never initialized before the
# registration pass. They are not obtainable gameplay items and registering
# them as fake content would hide a missing implementation.
$retiredUninitialized = @(
    'ammo_debug', 'ammo_secret', 'ashglasses', 'beta',
    'bismuth_boots', 'bismuth_helmet', 'bismuth_legs', 'bismuth_plate',
    'euphemium_boots', 'euphemium_helmet', 'euphemium_legs', 'euphemium_plate',
    'goggles', 'hat', 'jackt', 'jackt2', 'mask_piss', 'mask_rag', 'no9',
    'rpa_boots', 'rpa_helmet', 'rpa_legs', 'rpa_plate', 'zirconium_legs',
    'weapon_mod_caliber', 'weapon_mod_generic', 'weapon_mod_special', 'weapon_mod_test'
)
foreach ($id in $retiredUninitialized) {
    [void]$retired.Add($id)
}

# Modern Minecraft has a single item registry: an old standalone item cannot
# coexist with a formal BlockItem under the same id. Treat formal block ids as
# resolved by their BlockItem rather than recreating an incompatible Item.
foreach ($match in [regex]::Matches(
        $hbmBlocks,
        'public\s+static\s+final\s+DeferredBlock<[^;]+?\s+[A-Z0-9_]+\s*=\s*(?:register|registerBlock)\s*\(\s*"([a-z0-9_.-]+)"',
        [Text.RegularExpressions.RegexOptions]::Singleline)) {
    [void]$core.Add($match.Groups[1].Value)
}

# These are the dynamic material families registered by HbmItems from the
# catalog. They must stay in item_catalog.txt but must not become placeholders.
foreach ($id in $catalog) {
    if ($id -match '^ingot_' -or
        $id -match '^nugget_' -or
        $id -match '^powder_' -or
        $id -in @('dust', 'dust_tiny') -or
        $id -match '^plate_' -or
        ($id -match '^billet_' -and $id -ne 'billet_silicon') -or
        $id -match '^(coil_|wire_|pipe_|circuit_|pipes_|tank_|casing$|casing_)' -or
        ($id -match '^crystal_' -and $id -notin @('crystal_energy', 'crystal_horn', 'crystal_xen')) -or
        $id -match '^stamp_' -or
        $id -in @('blade_meteorite', 'blade_titanium', 'blade_tungsten', 'turbine_titanium', 'turbine_tungsten', 'flywheel_beryllium', 'sawblade') -or
        $id -match '^upgrade_' -or
        $id -match '^(fragment_|bedrock_ore_)') {
        [void]$core.Add($id)
    }

    # The complete old missile-part family is registered by HbmItems with
    # MissilePartItem, including the old physical compatibility table.
    if ($id -match '^mp_(c_[1-5]|s_20|warhead_[a-z0-9_]+|fuselage_[a-z0-9_]+|stability_[a-z0-9_]+|thruster_[a-z0-9_]+)$') {
        [void]$core.Add($id)
    }

    if ($id -match '^missile_(anti_ballistic|bhole|burst|buster|buster_strong|cluster|cluster_strong|decoy|doomsday|doomsday_rusted|drill|emp|emp_strong|generic|incendiary|incendiary_strong|inferno|micro|nuclear|nuclear_cluster|rain|schrabidium|shuttle|stealth|strong|taint|test|volcano)$') {
        [void]$core.Add($id)
    }
}

$placeholders = @($catalog | Where-Object {
    $_ -notin $core -and $_ -notin $retired -and $_ -notin $blocks -and $_ -notmatch '^gun_'
})
$utf8 = New-Object System.Text.UTF8Encoding($false)
[IO.File]::WriteAllLines($placeholderPath, $placeholders, $utf8)
Write-Host "Catalog items: $($catalog.Count)"
Write-Host "Migrated/core items excluded: $($catalog.Count - $placeholders.Count - @($retired | Where-Object { $_ -in $catalog }).Count - @($blocks | Where-Object { $_ -in $catalog }).Count)"
Write-Host "Retired items excluded: $(@($retired | Where-Object { $_ -in $catalog }).Count)"
Write-Host "Block IDs excluded: $(@($blocks | Where-Object { $_ -in $catalog }).Count)"
Write-Host "Unresolved placeholders: $($placeholders.Count)"
