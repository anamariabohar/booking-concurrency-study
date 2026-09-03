# Repeated JMeter locking runs for cross-run statistics (thesis).
param(
    [int] $Threads = 50,
    [int] $Loops = 2,
    [int] $RampUpSeconds = 5,
    [int] $Repetitions = 15,
    [string] $OutputFile = (Join-Path $PSScriptRoot "results\jmeter-repeated-t50.json")
)

$ErrorActionPreference = "Stop"
$JmeterHome = $env:JMETER_HOME
if (-not $JmeterHome) {
    $candidate = "C:\Users\Anamaria\tools\apache-jmeter-5.6.3"
    if (Test-Path "$candidate\bin\jmeter.bat") { $JmeterHome = $candidate }
}
if (-not $JmeterHome) { throw "JMETER_HOME not set" }

$Plan = Join-Path $PSScriptRoot "booking-locking-study.jmx"
$BaseProps = Join-Path $PSScriptRoot "user.properties"
$outDir = Join-Path $PSScriptRoot "results\jmeter-repeated-t${Threads}"
New-Item -ItemType Directory -Force -Path $outDir | Out-Null

$labels = @("book-unsafe", "book-synchronized", "book-reentrant-lock", "book-pessimistic", "book-optimistic")
$runs = @()

for ($rep = 1; $rep -le $Repetitions; $rep++) {
    $tag = "rep-$rep"
    $jtl = Join-Path $outDir "$tag.jtl"
    $report = Join-Path $outDir "$tag-report"
    $propsFile = Join-Path $outDir "$tag.properties"

    Copy-Item $BaseProps $propsFile -Force
    Add-Content $propsFile "threads=$Threads"
    Add-Content $propsFile "loops=$Loops"
    Add-Content $propsFile "rampUpSeconds=$RampUpSeconds"

    Write-Host "[$rep/$Repetitions] JMeter locking threads=$Threads"
    if (Test-Path $jtl) { Remove-Item $jtl -Force }
    if (Test-Path $report) { Remove-Item $report -Recurse -Force }

    & "$JmeterHome\bin\jmeter.bat" -n -t $Plan -q $propsFile -l $jtl -e -o $report
    if ($LASTEXITCODE -ne 0) { throw "JMeter failed rep=$rep" }

    $stats = Get-Content (Join-Path $report "statistics.json") -Raw | ConvertFrom-Json
    $row = [ordered]@{
        repetition   = $rep
        timestampUtc = (Get-Date).ToUniversalTime().ToString("o")
        strategies   = @{}
    }
    foreach ($label in $labels) {
        $s = $stats.$label
        if ($s) {
            $row.strategies[$label] = [ordered]@{
                sampleCount   = [int]$s.sampleCount
                meanResTime   = [double]$s.meanResTime
                medianResTime = [double]$s.medianResTime
                minResTime    = [double]$s.minResTime
                maxResTime    = [double]$s.maxResTime
                pct90ResTime  = [double]$s.pct1ResTime
                pct95ResTime  = [double]$s.pct2ResTime
                pct99ResTime  = [double]$s.pct3ResTime
                throughput    = [double]$s.throughput
            }
        }
    }
    $runs += $row
    Start-Sleep -Seconds 1
}

$payload = [ordered]@{
    generatedAtUtc = (Get-Date).ToUniversalTime().ToString("o")
    threads        = $Threads
    loops          = $Loops
    rampUpSeconds  = $RampUpSeconds
    repetitions    = $Repetitions
    runs           = $runs
}
$json = $payload | ConvertTo-Json -Depth 8
[System.IO.File]::WriteAllText($OutputFile, $json, [System.Text.UTF8Encoding]::new($false))
Write-Host "Saved $OutputFile"
