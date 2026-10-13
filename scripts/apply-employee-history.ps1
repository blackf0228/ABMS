param([Parameter(Mandatory=$true)][int]$BackendProcessId)
$ErrorActionPreference = 'Stop'
$employeeProjectRoot = (Resolve-Path (Join-Path $PSScriptRoot '..')).Path
$employeeBackendDir = Join-Path $employeeProjectRoot 'backend'
$employeeJarPath = Join-Path $employeeBackendDir 'target\abms-backend-0.1.0-SNAPSHOT.jar'
if (-not (Test-Path -LiteralPath $employeeJarPath)) { throw 'Verified backend JAR is missing' }
$employeeRunningBackend = Get-CimInstance Win32_Process -Filter "ProcessId = $BackendProcessId"
if (-not $employeeRunningBackend -or $employeeRunningBackend.CommandLine -notmatch 'com\.aba\.abms|abms-backend') {
  throw 'The specified process is not the ABMS backend; refusing to stop it'
}
$employeePortListener = netstat -ano | Select-String -Pattern ":9000\s+.*LISTENING\s+$BackendProcessId\s*$"
if (-not $employeePortListener) { throw 'The specified backend is no longer the port 9000 listener' }
$employeeMigrationSql = Get-Content -LiteralPath (Join-Path $employeeProjectRoot 'database\init\027_employee_effective_history.sql') -Raw -Encoding UTF8
$employeeMigrationApplied = docker exec abms-postgres psql -U abms -d abms -At -c "SELECT EXISTS (SELECT 1 FROM information_schema.columns WHERE table_schema=current_schema() AND table_name='employee_masters' AND column_name='effective_start_date');"
if ($LASTEXITCODE -ne 0) { throw 'DB preflight failed; backend will not be stopped' }
if ($employeeMigrationApplied -eq 't') { throw 'History migration is already applied; do not rerun this one-time script' }
$employeeLogDir = Join-Path $employeeProjectRoot '.tmp_employee_history'
New-Item -ItemType Directory -Path $employeeLogDir -Force | Out-Null
Stop-Process -Id $BackendProcessId
$employeeMigrationSql | docker exec -i abms-postgres psql -U abms -d abms -v ON_ERROR_STOP=1
if ($LASTEXITCODE -ne 0) { throw 'DB migration failed; transaction was not committed. Backend restart requires investigation.' }
$employeeNewBackend = Start-Process -FilePath $employeeRunningBackend.ExecutablePath -WindowStyle Hidden -PassThru `
  -WorkingDirectory $employeeBackendDir -ArgumentList @('-jar', ('"' + $employeeJarPath + '"'), '--spring.jpa.hibernate.ddl-auto=validate') `
  -RedirectStandardOutput (Join-Path $employeeLogDir 'backend.stdout.log') `
  -RedirectStandardError (Join-Path $employeeLogDir 'backend.stderr.log')
Write-Output "Started updated ABMS backend PID $($employeeNewBackend.Id); verify port 9000 before declaring deployment complete."
