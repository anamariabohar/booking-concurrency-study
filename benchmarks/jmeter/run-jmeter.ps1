# Run the locking concurrency study with JMeter (non-GUI, HTML report).
# Prerequisites: app on http://localhost:8080, CLIENT user matching user.properties

$ErrorActionPreference = "Stop"

$JmeterHome = $env:JMETER_HOME
if (-not $JmeterHome) {
    $candidate = "C:\Users\Anamaria\tools\apache-jmeter-5.6.3"
    if (Test-Path "$candidate\bin\jmeter.bat") {
        $JmeterHome = $candidate
    }
}
if (-not $JmeterHome -or -not (Test-Path "$JmeterHome\bin\jmeter.bat")) {
    Write-Error "JMeter not found. Set JMETER_HOME or install to C:\Users\Anamaria\tools\apache-jmeter-5.6.3"
}

$Root = Split-Path -Parent $MyInvocation.MyCommand.Path
$Results = Join-Path $Root "results"
New-Item -ItemType Directory -Force -Path $Results | Out-Null

$Study = if ($args[0]) { $args[0] } else { "locking" }
if ($Study -eq "locking") {
    $Plan = Join-Path $Root "booking-locking-study.jmx"
    $Jtl = Join-Path $Results "locking.jtl"
    $Report = Join-Path $Results "locking-report"
} elseif ($Study -eq "threading") {
    $Plan = Join-Path $Root "booking-threading-study.jmx"
    $Jtl = Join-Path $Results "threading.jtl"
    $Report = Join-Path $Results "threading-report"
} else {
    Write-Error "Usage: .\run-jmeter.ps1 [locking|threading]"
}

if (Test-Path $Jtl) { Remove-Item $Jtl -Force }
if (Test-Path $Report) { Remove-Item $Report -Recurse -Force }

Write-Host "Running $Study study..."
& "$JmeterHome\bin\jmeter.bat" -n `
    -t $Plan `
    -q (Join-Path $Root "user.properties") `
    -l $Jtl `
    -e -o $Report

Write-Host ""
Write-Host "Done. Open report:"
Write-Host "  $Report\index.html"
