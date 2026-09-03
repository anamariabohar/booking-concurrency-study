# JMeter locking study at multiple thread counts (local single-node load).
param(
    [int[]] $ThreadLevels = @(10, 50, 100, 250, 500),
    [int] $Loops = 2,
    [int] $RampUpSeconds = 5
)

$ErrorActionPreference = "Stop"
$Root = $PSScriptRoot
$ResultsRoot = Join-Path $Root "results\load-sweep"
New-Item -ItemType Directory -Force -Path $ResultsRoot | Out-Null

$JmeterHome = $env:JMETER_HOME
if (-not $JmeterHome) {
    $candidate = "C:\Users\Anamaria\tools\apache-jmeter-5.6.3"
    if (Test-Path "$candidate\bin\jmeter.bat") { $JmeterHome = $candidate }
}
if (-not $JmeterHome) { throw "JMETER_HOME not set" }

$Plan = Join-Path $Root "booking-locking-study.jmx"
$BaseProps = Join-Path $Root "user.properties"
$sweepMeta = @()

foreach ($threads in $ThreadLevels) {
    $tag = "locking-t${threads}-l${Loops}"
    $outDir = Join-Path $ResultsRoot $tag
    New-Item -ItemType Directory -Force -Path $outDir | Out-Null
    $jtl = Join-Path $outDir "locking.jtl"
    $report = Join-Path $outDir "report"
    $propsFile = Join-Path $outDir "run.properties"

    Copy-Item $BaseProps $propsFile -Force
    Add-Content $propsFile "threads=$threads"
    Add-Content $propsFile "loops=$Loops"
    Add-Content $propsFile "rampUpSeconds=$RampUpSeconds"

    Write-Host "JMeter: threads=$threads loops=$Loops"
    if (Test-Path $jtl) { Remove-Item $jtl -Force }
    if (Test-Path $report) { Remove-Item $report -Recurse -Force }

    & "$JmeterHome\bin\jmeter.bat" -n -t $Plan -q $propsFile -l $jtl -e -o $report
    if ($LASTEXITCODE -ne 0) { throw "JMeter failed for threads=$threads" }

    $statsFile = Join-Path $report "statistics.json"
    $stats = if (Test-Path $statsFile) { Get-Content $statsFile -Raw | ConvertFrom-Json } else { $null }

    $sweepMeta += [ordered]@{
        threads       = $threads
        loops         = $Loops
        rampUpSeconds = $RampUpSeconds
        jtl           = $jtl
        report        = $report
        statistics    = $stats
        timestampUtc  = (Get-Date).ToUniversalTime().ToString("o")
    }
}

$manifest = [ordered]@{
    generatedAtUtc = (Get-Date).ToUniversalTime().ToString("o")
    nodeMode       = "local-single-node"
    threadLevels   = $ThreadLevels
    runs           = $sweepMeta
}
$manifestPath = Join-Path $ResultsRoot "load-sweep-manifest.json"
$manifest | ConvertTo-Json -Depth 10 | Set-Content $manifestPath -Encoding UTF8
Write-Host "Manifest: $manifestPath"
