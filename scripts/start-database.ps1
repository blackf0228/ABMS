$ErrorActionPreference = "Stop"

Set-Location (Join-Path $PSScriptRoot "..")

$dockerDesktop = "C:\Program Files\Docker\Docker\Docker Desktop.exe"

function Test-DockerReady {
  $previousErrorActionPreference = $ErrorActionPreference
  $ErrorActionPreference = "Continue"

  try {
    $output = & docker info 2>&1
    $script:lastDockerInfoOutput = $output -join "`n"
    return $LASTEXITCODE -eq 0
  } finally {
    $ErrorActionPreference = $previousErrorActionPreference
  }
}

if (-not (Test-DockerReady)) {
  Write-Host "Docker Desktop is not running. Starting Docker Desktop..."
  Write-Host "If Windows asks for permission, click Yes."

  if (Test-Path $dockerDesktop) {
    Start-Process $dockerDesktop
  } else {
    throw "Docker Desktop was not found at $dockerDesktop. Please install or start Docker Desktop manually."
  }

  Write-Host "Waiting for Docker Engine to become ready. This can take 1-2 minutes..."

  $ready = $false
  for ($i = 1; $i -le 60; $i++) {
    Start-Sleep -Seconds 2
    if (Test-DockerReady) {
      $ready = $true
      break
    }
    Write-Host "Still waiting for Docker... ($i/60)"
  }

  if (-not $ready) {
    Write-Host ""
    Write-Host "Docker Engine is still not ready."

    if ($script:lastDockerInfoOutput -match "permission denied|Access is denied") {
      Write-Host ""
      Write-Host "Docker permission issue detected."
      Write-Host "Please sign out of Windows or restart Windows, then open Docker Desktop again."
      Write-Host "If the issue remains, fix permissions for: C:\Users\black\.docker"
    }

    Write-Host "Please open Docker Desktop manually and wait until it says Docker is running."
    Write-Host "Then run: .\scripts\start-database.cmd"
    exit 1
  }
}

docker compose up -d postgres
docker compose ps
