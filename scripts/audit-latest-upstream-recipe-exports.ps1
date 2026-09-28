param(
    [string]$LatestRoot = ("..\..\HBM_1.7.10" + [char]0x65B0),
    [string]$Manifest = "gradle/verification/latest-upstream-recipe-exports.tsv",
    [string]$WorkRecipeCounts = "gradle/verification/machine-work-recipe-counts.tsv",
    [string]$DefaultRuntimeReport = "run-recipe-audit/recipe-audit.tsv",
    [string]$Mode528RuntimeReport = "run-recipe-audit-528/recipe-audit.tsv",
    [string]$OutputReport = "build/reports/latest-upstream-recipe-exports.txt",
    [switch]$FailOnMismatch
)

$ErrorActionPreference = 'Stop'
$referenceCommit = 'e674541d85f47fd267d6d99b7dc1f0595e031c05'
$knownHandling = [System.Collections.Generic.HashSet[string]]::new([string[]]@(
    'CODE_TABLE', 'DYNAMIC_FOUNDRY', 'FILTERED_DEPRECATED_CHAIN', 'FILTERED_SYSTEM',
    'RETIRED', 'SHARED_FUSION_TABLE', 'STATIC', 'STATIC_PLUS_DYNAMIC',
    'STATIC_PLUS_MODE', 'STATIC_PLUS_MODERN'
))
$problems = [System.Collections.Generic.List[string]]::new()

function Read-TabRows([string]$Path, [int]$Columns) {
    if (-not (Test-Path -LiteralPath $Path -PathType Leaf)) {
        throw "Missing required audit input $Path"
    }
    $rows = @()
    $lineNumber = 0
    foreach ($line in Get-Content -LiteralPath $Path) {
        $lineNumber++
        if (-not $line.Trim() -or $line.TrimStart().StartsWith('#')) { continue }
        $parts = $line -split "`t", $Columns
        if ($parts.Count -lt $Columns) {
            throw "${Path}:${lineNumber} must contain at least $Columns tab-separated columns"
        }
        $rows += ,$parts
    }
    return $rows
}

function Read-RuntimeCounts([string]$Path) {
    if (-not (Test-Path -LiteralPath $Path -PathType Leaf)) {
        throw "Missing runtime recipe report $Path"
    }
    $counts = @{}
    foreach ($line in Get-Content -LiteralPath $Path) {
        $parts = $line -split "`t"
        if ($parts.Count -ge 3 -and $parts[0] -eq 'RECIPE') {
            if (-not $counts.ContainsKey($parts[2])) { $counts[$parts[2]] = 0 }
            $counts[$parts[2]]++
        }
    }
    return $counts
}

$latestPath = (Resolve-Path -LiteralPath $LatestRoot).Path
$exportRoot = Join-Path $latestPath 'eclipse/config/hbmRecipes'
if (-not (Test-Path -LiteralPath $exportRoot -PathType Container)) {
    throw "Latest-upstream recipe export directory is missing: $exportRoot"
}
$actualCommit = (& git -C $latestPath rev-parse HEAD).Trim()
if ($LASTEXITCODE -ne 0) { throw "Unable to read latest-upstream commit from $latestPath" }
if ($actualCommit -cne $referenceCommit) {
    $problems.Add("Latest-upstream checkout is $actualCommit, but the reviewed recipe contract is pinned to $referenceCommit")
}

$workTypes = [System.Collections.Generic.HashSet[string]]::new()
foreach ($parts in Read-TabRows $WorkRecipeCounts 4) {
    [void]$workTypes.Add($parts[0].Trim())
}
$normalCounts = Read-RuntimeCounts $DefaultRuntimeReport
$mode528Counts = Read-RuntimeCounts $Mode528RuntimeReport

$manifestByFile = @{}
foreach ($parts in Read-TabRows $Manifest 5) {
    $file = $parts[0].Trim()
    $expectedCount = 0
    if (-not [int]::TryParse($parts[1].Trim(), [ref]$expectedCount)) {
        $problems.Add("Manifest row $file has a non-integer upstream recipe count '$($parts[1])'")
        continue
    }
    $type = $parts[2].Trim()
    $handling = $parts[3].Trim()
    $reason = $parts[4].Trim()
    if ($manifestByFile.ContainsKey($file)) {
        $problems.Add("Duplicate latest-upstream export manifest row: $file")
        continue
    }
    if (-not $knownHandling.Contains($handling)) {
        $problems.Add("Manifest row $file uses unknown handling '$handling'")
    }
    if (-not $reason) {
        $problems.Add("Manifest row $file has no reviewable mapping explanation")
    }
    if ($type -ne '-') {
        if (-not $workTypes.Contains($type)) {
            $problems.Add("Manifest row $file maps to $type, which has no exact normal/528 work-recipe count contract")
        }
        $normal = if ($normalCounts.ContainsKey($type)) { [int]$normalCounts[$type] } else { 0 }
        $mode528 = if ($mode528Counts.ContainsKey($type)) { [int]$mode528Counts[$type] } else { 0 }
        if ($normal -le 0 -or $mode528 -le 0) {
            $problems.Add("Manifest row $file maps to unloaded recipe type ${type}: normal=$normal, 528=$mode528")
        }
    } elseif ($handling -notin @('CODE_TABLE', 'RETIRED')) {
        $problems.Add("Manifest row $file has no modern recipe type but is classified as $handling")
    }
    $manifestByFile[$file] = [pscustomobject]@{
        Expected = $expectedCount
        Type = $type
        Handling = $handling
        Reason = $reason
    }
}

$actualFiles = @(Get-ChildItem -LiteralPath $exportRoot -Filter '_hbm*.json' -File | Sort-Object Name)
$actualNames = [System.Collections.Generic.HashSet[string]]::new([string[]]@($actualFiles.Name))
foreach ($file in $actualFiles) {
    if (-not $manifestByFile.ContainsKey($file.Name)) {
        $problems.Add("Unreviewed latest-upstream machine recipe export: $($file.Name)")
        continue
    }
    try {
        $json = Get-Content -LiteralPath $file.FullName -Raw | ConvertFrom-Json
    } catch {
        $problems.Add("Invalid latest-upstream export $($file.Name): $($_.Exception.Message)")
        continue
    }
    if ($null -eq $json.PSObject.Properties['recipes']) {
        $problems.Add("Latest-upstream export $($file.Name) has no recipes array")
        continue
    }
    $actualCount = @($json.recipes).Count
    $expectedCount = $manifestByFile[$file.Name].Expected
    if ($actualCount -ne $expectedCount) {
        $problems.Add("Latest-upstream export $($file.Name) changed from $expectedCount to $actualCount recipes")
    }
}
foreach ($file in $manifestByFile.Keys) {
    if (-not $actualNames.Contains($file)) {
        $problems.Add("Stale manifest row has no latest-upstream export: $file")
    }
}

# Assembly fluid packages are generated in upstream rather than written as a
# separate table. Compare their semantic names explicitly so a missing pair
# cannot hide behind the larger assembly count. `custom_demo` is created only
# by upstream's example external-fluid config; the eight additions below are
# real fluids registered by this high-version port.
$upstreamAssembly = Get-Content -LiteralPath (Join-Path $exportRoot '_hbmAssemblyMachine.json') -Raw | ConvertFrom-Json
$oldPack = @($upstreamAssembly.recipes |
        Where-Object { $_.name -is [string] -and $_.name.StartsWith('ass.packagehbmfluid.') } |
        ForEach-Object { $_.name.Substring('ass.packagehbmfluid.'.Length) } | Sort-Object -Unique)
$oldUnpack = @($upstreamAssembly.recipes |
        Where-Object { $_.name -is [string] -and $_.name.StartsWith('ass.unpackagehbmfluid.') } |
        ForEach-Object { $_.name.Substring('ass.unpackagehbmfluid.'.Length) } | Sort-Object -Unique)
$currentPack = @()
$currentUnpack = @()
foreach ($file in Get-ChildItem 'src/main/resources/data/reinhardtshbm/recipe/assembly_machine' -Filter '*.json' -File) {
    $recipe = Get-Content -LiteralPath $file.FullName -Raw | ConvertFrom-Json
    if ($recipe.group -isnot [string]) { continue }
    if ($recipe.group.StartsWith('ass.packagehbmfluid.')) {
        $currentPack += $recipe.group.Substring('ass.packagehbmfluid.'.Length)
    } elseif ($recipe.group.StartsWith('ass.unpackagehbmfluid.')) {
        $currentUnpack += $recipe.group.Substring('ass.unpackagehbmfluid.'.Length)
    }
}
$currentPack = @($currentPack | Sort-Object -Unique)
$currentUnpack = @($currentUnpack | Sort-Object -Unique)
if (($oldPack -join '|') -cne ($oldUnpack -join '|')) {
    $problems.Add('Latest upstream does not have one unpack route for every generated fluid-pack route')
}
if (($currentPack -join '|') -cne ($currentUnpack -join '|')) {
    $problems.Add('Modern port does not have one unpack route for every generated fluid-pack route')
}
$missingOldPacks = @(Compare-Object @($oldPack | Where-Object { $_ -ne 'custom_demo' }) $currentPack |
        Where-Object SideIndicator -eq '<=' | ForEach-Object InputObject)
if ($missingOldPacks.Count -gt 0) {
    $problems.Add("Latest-upstream built-in fluid packs missing from modern port: $($missingOldPacks -join ', ')")
}
$expectedModernPackAdditions = @('acid_fluid', 'heuf6', 'hydrazine', 'leuf6', 'meuf6', 'mud_fluid', 'toxic_fluid', 'watz_heavy')
$actualModernPackAdditions = @(Compare-Object $oldPack $currentPack |
        Where-Object SideIndicator -eq '=>' | ForEach-Object InputObject | Sort-Object)
if (($actualModernPackAdditions -join '|') -cne (($expectedModernPackAdditions | Sort-Object) -join '|')) {
    $expectedModernPackText = ($expectedModernPackAdditions | Sort-Object) -join ', '
    $problems.Add("Modern-only fluid-pack set is '$($actualModernPackAdditions -join ', ')', expected '$expectedModernPackText'")
}
$missingUpstreamOnly = @(Compare-Object $oldPack $currentPack |
        Where-Object SideIndicator -eq '<=' | ForEach-Object InputObject | Sort-Object)
if (($missingUpstreamOnly -join '|') -cne 'custom_demo') {
    $problems.Add("Upstream-only fluid-pack set is '$($missingUpstreamOnly -join ', ')', expected only config-defined custom_demo")
}

$lines = [System.Collections.Generic.List[string]]::new()
$lines.Add('Latest-upstream complete machine recipe export coverage')
$lines.Add("reference commit: $referenceCommit")
$lines.Add("export tables: $($actualFiles.Count)")
$lines.Add("reviewed tables: $($manifestByFile.Count)")
$lines.Add("problems: $($problems.Count)")
$lines.Add('')
$lines.Add('PROBLEMS')
foreach ($problem in $problems) { $lines.Add($problem) }
$lines.Add('')
$lines.Add('REVIEWED EXPORTS')
foreach ($file in ($manifestByFile.Keys | Sort-Object)) {
    $row = $manifestByFile[$file]
    $lines.Add("$file`t$($row.Expected)`t$($row.Type)`t$($row.Handling)`t$($row.Reason)")
}

$outputFile = [IO.Path]::GetFullPath($OutputReport)
[IO.Directory]::CreateDirectory([IO.Path]::GetDirectoryName($outputFile)) | Out-Null
[IO.File]::WriteAllLines($outputFile, $lines, [Text.UTF8Encoding]::new($false))
Write-Host "Latest-upstream recipe export audit: $($actualFiles.Count) tables, $($manifestByFile.Count) reviewed, $($problems.Count) problems. Report: $outputFile"

if ($FailOnMismatch -and $problems.Count -gt 0) {
    throw "Latest-upstream recipe export audit failed with $($problems.Count) problem(s); see $outputFile"
}
