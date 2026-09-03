# Aggregate correctness-matrix.json into a thesis-friendly summary.
param(
    [string] $InputFile = (Join-Path $PSScriptRoot "results\correctness-matrix.json"),
    [string] $SummaryJson = (Join-Path $PSScriptRoot "results\correctness-matrix-summary.json"),
    [string] $SummaryMd = (Join-Path $PSScriptRoot "results\correctness-matrix-summary.md")
)

$ErrorActionPreference = "Stop"
$data = Get-Content $InputFile -Raw | ConvertFrom-Json

$byGroup = $data.runs | Group-Object { "$($_.strategy)|$($_.concurrency)|$($_.slotStart)" }

$groups = foreach ($g in $byGroup) {
    $parts = $g.Name -split '\|'
    $runs = $g.Group
    [ordered]@{
        strategy    = $parts[0]
        concurrency = [int]$parts[1]
        slotStart   = $parts[2]
        runCount    = $runs.Count
        successes   = @{
            min = ($runs | Measure-Object successes -Minimum).Minimum
            max = ($runs | Measure-Object successes -Maximum).Maximum
            avg = [math]::Round(($runs | Measure-Object successes -Average).Average, 2)
        }
        conflicts   = @{
            min = ($runs | Measure-Object conflicts -Minimum).Minimum
            max = ($runs | Measure-Object conflicts -Maximum).Maximum
            avg = [math]::Round(($runs | Measure-Object conflicts -Average).Average, 2)
        }
        doubleBookingPairs = @{
            min = ($runs | Measure-Object doubleBookingPairs -Minimum).Minimum
            max = ($runs | Measure-Object doubleBookingPairs -Maximum).Maximum
            avg = [math]::Round(($runs | Measure-Object doubleBookingPairs -Average).Average, 2)
        }
        wallClockMs = @{
            min = ($runs | Measure-Object wallClockMs -Minimum).Minimum
            max = ($runs | Measure-Object wallClockMs -Maximum).Maximum
            avg = [math]::Round(($runs | Measure-Object wallClockMs -Average).Average, 2)
        }
        allZeroDoubleBooking = (($runs | Where-Object { $_.doubleBookingPairs -gt 0 }).Count -eq 0)
    }
}

$unsafeOnly = $groups | Where-Object { $_.strategy -eq "UNSAFE" }
$safeOnly = $groups | Where-Object { $_.strategy -ne "UNSAFE" }

$summary = [ordered]@{
    sourceFile          = (Split-Path $InputFile -Leaf)
    generatedAtUtc      = $data.generatedAtUtc
    repetitions         = $data.repetitions
    concurrencyLevels   = $data.concurrencyLevels
    strategies          = $data.strategies
    slots               = $data.slots
    totalRuns           = $data.runs.Count
    safeStrategiesAlwaysCorrect = ($safeOnly | Where-Object { -not $_.allZeroDoubleBooking }).Count -eq 0
    groups              = $groups
}

$summary | ConvertTo-Json -Depth 8 | Set-Content $SummaryJson -Encoding UTF8

$md = @"
# Rezumat matrice corectitudine (same-slot race)

Generat din: ``$(Split-Path $InputFile -Leaf)`` la $($data.generatedAtUtc)

- Repetitii per configuratie: $($data.repetitions)
- Niveluri concurenta: $($data.concurrencyLevels -join ', ')
- Sloturi: $($data.slots -join ', ')
- Total rulari: $($data.runs.Count)

## Strategii sincronizate (doubleBookingPairs = 0 in toate rulările)

| Strategie | Concurenta | Slot | Rulari | Succese (min-max) | Conflicte (min-max) | wallClockMs (avg) |
|-----------|------------|------|--------|-------------------|---------------------|-------------------|
"@

foreach ($g in ($safeOnly | Sort-Object strategy, concurrency, slotStart)) {
    $md += "| $($g.strategy) | $($g.concurrency) | $($g.slotStart) | $($g.runCount) | $($g.successes.min)-$($g.successes.max) | $($g.conflicts.min)-$($g.conflicts.max) | $($g.wallClockMs.avg) |`n"
}

$md += @"

## UNSAFE (variabilitate intre rulări)

| Concurenta | Slot | Rulari | Succese (min-max) | Perechi DB (min-max) | wallClockMs (avg) |
|------------|------|--------|-------------------|----------------------|-------------------|
"@

foreach ($g in ($unsafeOnly | Sort-Object concurrency, slotStart)) {
    $md += "| $($g.concurrency) | $($g.slotStart) | $($g.runCount) | $($g.successes.min)-$($g.successes.max) | $($g.doubleBookingPairs.min)-$($g.doubleBookingPairs.max) | $($g.wallClockMs.avg) |`n"
}

$md | Set-Content $SummaryMd -Encoding UTF8
Write-Host "Wrote $SummaryJson"
Write-Host "Wrote $SummaryMd"
