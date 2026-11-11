param(
  [string]$ProjectRoot = (Join-Path $PSScriptRoot ".."),
  [switch]$ValidateOnly
)

$ErrorActionPreference = "Stop"
$ProjectRoot = [System.IO.Path]::GetFullPath($ProjectRoot)
$backendRoot = [System.IO.Path]::GetFullPath((Join-Path $ProjectRoot "backend"))
$frontendRoot = [System.IO.Path]::GetFullPath((Join-Path $ProjectRoot "frontend"))
$backendLauncher = Join-Path $ProjectRoot "scripts\start-backend.cmd"
$frontendLauncher = Join-Path $ProjectRoot "scripts\start-frontend.cmd"

function Get-ListenerProcessIds([int]$Port) {
  $netstat = Join-Path $env:SystemRoot "System32\netstat.exe"
  $lines = & $netstat -ano -p tcp
  if ($LASTEXITCODE -ne 0) {
    throw "Could not inspect TCP listeners with netstat. Nothing was stopped."
  }

  $processIds = @()
  foreach ($line in $lines) {
    if ($line -match '^\s*TCP\s+\S+:(?<LocalPort>\d+)\s+\S+\s+LISTENING\s+(?<ProcessId>\d+)\s*$' -and [int]$Matches.LocalPort -eq $Port) {
      $processIds += [int]$Matches.ProcessId
    }
  }

  return @($processIds | Select-Object -Unique)
}

function Get-ListeningProcess([int]$Port, [string]$ExpectedName, [string]$ExpectedMarker) {
  $processIds = @(Get-ListenerProcessIds $Port)
  $matches = @()

  foreach ($processId in $processIds) {
    $process = Get-CimInstance Win32_Process -Filter "ProcessId = $processId"
    if (-not $process) {
      throw "Port $Port is occupied, but its owning process could not be inspected. Nothing was stopped."
    }

    $commandLine = [string]$process.CommandLine
    if ($process.Name -ieq $ExpectedName -and $commandLine.IndexOf($ExpectedMarker, [StringComparison]::OrdinalIgnoreCase) -ge 0) {
      $matches += [pscustomobject]@{ Port = $Port; ProcessId = [int]$processId; Name = $process.Name }
    } else {
      throw "Port $Port is owned by an unrecognized process ($($process.Name), PID $processId). Nothing was stopped. Stop it manually after confirming its purpose, then retry."
    }
  }

  return $matches
}

try {
  $backendProcesses = @(Get-ListeningProcess 9000 "java.exe" "-Dmaven.multiModuleProjectDirectory=$backendRoot")
  $frontendCliMarker = Join-Path $frontendRoot "node_modules\@angular\cli\bin\ng.js"
  $frontendProcesses = @(Get-ListeningProcess 4200 "node.exe" $frontendCliMarker)
  $managedProcesses = @($backendProcesses) + @($frontendProcesses)

  if ($ValidateOnly) {
    if ($managedProcesses.Count -eq 0) {
      Write-Host "Validation passed: ports 9000 and 4200 are free."
    } else {
      $managedProcesses | ForEach-Object { Write-Host "Validation passed: port $($_.Port) belongs to the expected ABMS process (PID $($_.ProcessId))." }
    }
    exit 0
  }

  foreach ($process in $managedProcesses) {
    Write-Host "Stopping ABMS process on port $($process.Port) (PID $($process.ProcessId))..."
    Stop-Process -Id $process.ProcessId -Force
  }

  foreach ($process in $managedProcesses) {
    $deadline = (Get-Date).AddSeconds(20)
    do {
      Start-Sleep -Milliseconds 250
      $stillListening = @(Get-ListenerProcessIds $process.Port).Count -gt 0
    } while ($stillListening -and (Get-Date) -lt $deadline)

    if ($stillListening) {
      throw "Port $($process.Port) did not become free. The services were not started again."
    }
  }

  Write-Host "Starting backend and frontend in separate windows..."
  Start-Process -FilePath $env:ComSpec -ArgumentList @("/k", "call `"$backendLauncher`"") -WorkingDirectory (Split-Path $backendLauncher)
  Start-Process -FilePath $env:ComSpec -ArgumentList @("/k", "call `"$frontendLauncher`"") -WorkingDirectory (Split-Path $frontendLauncher)
  Write-Host "Startup windows opened. Backend: http://localhost:9000  Frontend: http://localhost:4200"
} catch {
  Write-Host "Restart aborted: $($_.Exception.Message)" -ForegroundColor Red
  exit 1
}
