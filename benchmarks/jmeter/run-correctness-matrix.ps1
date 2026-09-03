# Same-slot correctness matrix via POST /api/concurrency/race
# Prerequisites: app on http://localhost:8080, client1/password, providerId in user.properties

param(
    [int[]] $ConcurrencyLevels = @(10, 50, 100, 250, 500),
    [string[]] $Strategies = @("UNSAFE", "SYNCHRONIZED", "REENTRANT_LOCK", "PESSIMISTIC", "OPTIMISTIC"),
    [int] $Repetitions = 3,
    [string[]] $Slots = @(
        "2026-08-03T10:00:00",
        "2026-08-03T11:00:00",
        "2026-08-03T14:00:00"
    ),
    [string] $PropertiesFile = (Join-Path $PSScriptRoot "user.properties"),
    [string] $OutputFile = (Join-Path $PSScriptRoot "results\correctness-matrix.json")
)

$ErrorActionPreference = "Stop"

function Read-Properties($path) {
    $map = @{}
    Get-Content $path | ForEach-Object {
        if ($_ -match '^\s*#' -or $_ -match '^\s*$') { return }
        $parts = $_ -split '=', 2
        if ($parts.Count -eq 2) { $map[$parts[0].Trim()] = $parts[1].Trim() }
    }
    return $map
}

function Get-AuthToken($baseUrl, $username, $password) {
    $body = @{ username = $username; password = $password } | ConvertTo-Json
    $resp = Invoke-RestMethod -Method Post -Uri "$baseUrl/api/auth/login" -ContentType "application/json" -Body $body
    $token = if ($resp.accessToken) { $resp.accessToken } else { $resp.token }
    if (-not $token) { throw "Login failed: no token returned" }
    return $token
}

function Invoke-Race($baseUrl, $token, $strategy, $concurrency, $providerId, $startTime, $endTime, $type) {
    $body = @{
        providerId = [long]$providerId
        startTime  = $startTime
        endTime    = $endTime
        type       = $type
    } | ConvertTo-Json
    $uri = "$baseUrl/api/concurrency/race?strategy=$strategy&concurrency=$concurrency&cleanupBeforeRun=true"
    $headers = @{ Authorization = "Bearer $token" }
    return Invoke-RestMethod -Method Post -Uri $uri -Headers $headers -ContentType "application/json" -Body $body
}

$props = Read-Properties $PropertiesFile
$baseUrl = $props.baseUrl
$username = $props.username
$password = $props.password
$providerId = $props.providerId
$endTimeTemplate = $props.endTime
$appointmentType = $props.appointmentType

if (-not $baseUrl) { throw "baseUrl missing in $PropertiesFile" }

Write-Host "Waiting for $baseUrl ..."
$ready = $false
for ($i = 0; $i -lt 60; $i++) {
    try {
        Invoke-WebRequest -Uri "$baseUrl/api/auth/login" -Method Post -ContentType "application/json" -Body '{"username":"probe","password":"probe"}' -UseBasicParsing -TimeoutSec 2 | Out-Null
        $ready = $true
        break
    } catch {
        if ($_.Exception.Response) { $ready = $true; break }
        Start-Sleep -Seconds 2
    }
}
if (-not $ready) { throw "Application not reachable at $baseUrl" }

$token = Get-AuthToken $baseUrl $username $password
Write-Host "Authenticated as $username"

$results = @()
$total = $Strategies.Count * $ConcurrencyLevels.Count * $Slots.Count * $Repetitions
$run = 0

foreach ($slotStart in $Slots) {
    $slotEnd = if ($slotStart -match 'T(\d{2}):(\d{2})') {
        $h = [int]$Matches[1]
        $m = [int]$Matches[2]
        $end = (Get-Date "$slotStart").AddMinutes(30)
        $end.ToString("yyyy-MM-dd'T'HH:mm:ss")
    } else {
        $endTimeTemplate
    }

    foreach ($strategy in $Strategies) {
        foreach ($concurrency in $ConcurrencyLevels) {
            for ($rep = 1; $rep -le $Repetitions; $rep++) {
                $run++
                Write-Host "[$run/$total] $strategy concurrency=$concurrency slot=$slotStart rep=$rep"
                $race = Invoke-Race $baseUrl $token $strategy $concurrency $providerId $slotStart $slotEnd $appointmentType
                $results += [ordered]@{
                    runId              = $run
                    repetition         = $rep
                    strategy           = $race.strategy
                    concurrency        = [int]$race.concurrency
                    slotStart          = $slotStart
                    slotEnd            = $slotEnd
                    successes          = [int]$race.successes
                    conflicts          = [int]$race.conflicts
                    errors             = [int]$race.errors
                    doubleBookingPairs = [int]$race.doubleBookingPairs
                    wallClockMs        = [int]$race.wallClockMs
                    timestampUtc       = (Get-Date).ToUniversalTime().ToString("o")
                }
                Start-Sleep -Milliseconds 200
            }
        }
    }
}

$payload = [ordered]@{
    generatedAtUtc = (Get-Date).ToUniversalTime().ToString("o")
    baseUrl        = $baseUrl
    providerId     = [long]$providerId
    repetitions    = $Repetitions
    concurrencyLevels = $ConcurrencyLevels
    strategies     = $Strategies
    slots          = $Slots
    runs           = $results
}

$outDir = Split-Path $OutputFile -Parent
New-Item -ItemType Directory -Force -Path $outDir | Out-Null
$json = $payload | ConvertTo-Json -Depth 6
[System.IO.File]::WriteAllText($OutputFile, $json, [System.Text.UTF8Encoding]::new($false))

Write-Host ""
Write-Host "Saved $($results.Count) runs to $OutputFile"
