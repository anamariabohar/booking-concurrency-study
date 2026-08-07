#Requires -Version 5.1
# Registers CLIENT + PROVIDER for the JMeter study (app must be running).
# Then prints SQL to insert working hours for Monday 09:00-17:00.

$ErrorActionPreference = "Stop"
$base = "http://localhost:8080"

Write-Host "Checking app at $base ..."
try {
    Invoke-WebRequest -Uri "$base/api/auth/login" -Method POST -ContentType "application/json" `
        -Body '{"username":"__ping__","password":"__ping__"}' -UseBasicParsing -TimeoutSec 5 | Out-Null
} catch {
    if ($_.Exception.Response -eq $null -and $_.Exception.Message -match "timed out|refused|Unable to connect") {
        Write-Error "App is not reachable on $base. Start it in IntelliJ (Run BookingConcurrencyStudyApplication) first."
    }
}

Write-Host "Registering client1 ..."
try {
    Invoke-RestMethod -Method POST -Uri "$base/api/auth/register" -ContentType "application/json" -Body (@{
        username = "client1"
        email = "client1@example.com"
        password = "password"
        role = "CLIENT"
    } | ConvertTo-Json)
    Write-Host "  client1 registered"
} catch {
    Write-Host "  client1 may already exist ($($_.Exception.Message))"
}

Write-Host "Registering provider1 ..."
$providerResp = $null
try {
    $providerResp = Invoke-RestMethod -Method POST -Uri "$base/api/auth/provider/register" -ContentType "application/json" -Body (@{
        username = "provider1"
        email = "provider1@example.com"
        password = "password"
        specialization = "General"
        avgAppointmentDuration = 30
    } | ConvertTo-Json)
    Write-Host "  provider1 registered"
} catch {
    Write-Host "  provider1 may already exist ($($_.Exception.Message))"
}

Write-Host ""
Write-Host "Login as client1 to verify credentials ..."
$login = Invoke-RestMethod -Method POST -Uri "$base/api/auth/login" -ContentType "application/json" -Body (@{
    username = "client1"
    password = "password"
} | ConvertTo-Json)
Write-Host "  OK — got accessToken"

Write-Host ""
Write-Host "Next: insert working hours in PostgreSQL (provider id is usually the provider user id)."
Write-Host "Find provider id:"
Write-Host "  SELECT id, specialization FROM providers;"
Write-Host ""
Write-Host "Then (Monday = 1, matches startTime 2026-08-03 in user.properties):"
Write-Host "  INSERT INTO working_hours (provider_id, day_of_week, start_time, end_time)"
Write-Host "  VALUES (<provider_id>, 1, '09:00:00', '17:00:00');"
Write-Host ""
Write-Host "Update benchmarks/jmeter/user.properties providerId to that id, then run:"
Write-Host "  .\run-jmeter.ps1 locking"
