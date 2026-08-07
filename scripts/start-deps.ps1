# Starts local dependencies for the concurrency study (PostgreSQL service check).
# Redis is no longer required.

function Test-Port([int]$port) {
  try {
    $c = New-Object System.Net.Sockets.TcpClient
    $iar = $c.BeginConnect("127.0.0.1", $port, $null, $null)
    $ok = $iar.AsyncWaitHandle.WaitOne(800, $false)
    if (-not $ok) { $c.Close(); return $false }
    $c.EndConnect($iar) | Out-Null
    $c.Close()
    return $true
  } catch { return $false }
}

if (Test-Port 5432) {
  Write-Host "PostgreSQL listening on 5432"
} else {
  Write-Warning "PostgreSQL not listening on 5432. Start service postgresql-x64-18."
}

Write-Host "Dependencies ready."
