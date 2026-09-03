# Repeated same-slot race at fixed concurrency for wallClockMs statistics.
param(
    [int] $Concurrency = 50,
    [int] $Repetitions = 15,
    [string] $SlotStart = "2026-08-03T14:00:00",
    [string[]] $Strategies = @("UNSAFE", "SYNCHRONIZED", "REENTRANT_LOCK", "PESSIMISTIC", "OPTIMISTIC"),
    [string] $OutputFile = (Join-Path $PSScriptRoot "results\race-repeated-c50.json")
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

$props = Read-Properties (Join-Path $PSScriptRoot "user.properties")
$baseUrl = $props.baseUrl
$login = Invoke-RestMethod -Method Post -Uri "$baseUrl/api/auth/login" -ContentType "application/json" -Body (@{ username = $props.username; password = $props.password } | ConvertTo-Json)
$token = $login.accessToken
$headers = @{ Authorization = "Bearer $token" }
$end = (Get-Date $SlotStart).AddMinutes(30).ToString("yyyy-MM-dd'T'HH:mm:ss")

$runs = @()
foreach ($strategy in $Strategies) {
    for ($rep = 1; $rep -le $Repetitions; $rep++) {
        $body = @{ providerId = [long]$props.providerId; startTime = $SlotStart; endTime = $end; type = $props.appointmentType } | ConvertTo-Json
        $uri = "$baseUrl/api/concurrency/race?strategy=$strategy&concurrency=$Concurrency&cleanupBeforeRun=true"
        $race = Invoke-RestMethod -Method Post -Uri $uri -Headers $headers -ContentType "application/json" -Body $body
        $runs += [ordered]@{
            strategy           = $strategy
            repetition         = $rep
            concurrency        = $Concurrency
            slotStart          = $SlotStart
            successes          = [int]$race.successes
            conflicts          = [int]$race.conflicts
            doubleBookingPairs = [int]$race.doubleBookingPairs
            wallClockMs        = [int]$race.wallClockMs
            timestampUtc       = (Get-Date).ToUniversalTime().ToString("o")
        }
        Write-Host "[$strategy rep $rep] wallClockMs=$($race.wallClockMs) pairs=$($race.doubleBookingPairs)"
    }
}

$payload = [ordered]@{
    generatedAtUtc = (Get-Date).ToUniversalTime().ToString("o")
    concurrency    = $Concurrency
    repetitions    = $Repetitions
    slotStart      = $SlotStart
    runs           = $runs
}
$json = $payload | ConvertTo-Json -Depth 5
[System.IO.File]::WriteAllText($OutputFile, $json, [System.Text.UTF8Encoding]::new($false))
Write-Host "Saved $OutputFile"
