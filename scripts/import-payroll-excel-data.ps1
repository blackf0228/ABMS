$ErrorActionPreference = "Stop"

$root = Resolve-Path (Join-Path $PSScriptRoot "..")
$sqlPath = Join-Path $root "database\seed\008_payroll_initial_data_from_excel.sql"
$runnerJavaPath = Join-Path $PSScriptRoot "SqlFileRunner.java"
$runnerClassDir = Join-Path $root ".tools\sql-file-runner"
$postgresJar = Get-ChildItem -Path "$env:USERPROFILE\.m2\repository\org\postgresql\postgresql" -Recurse -Filter "postgresql-*.jar" -ErrorAction SilentlyContinue |
  Sort-Object FullName -Descending |
  Select-Object -First 1

foreach ($file in @($sqlPath, $runnerJavaPath)) {
  if (-not (Test-Path $file)) {
    throw "Required file was not found: $file"
  }
}

if ($null -eq $postgresJar) {
  throw "PostgreSQL JDBC driver was not found. Please start the backend once or build it so Maven can download the driver."
}

if (-not (Test-Path $runnerClassDir)) {
  New-Item -ItemType Directory -Path $runnerClassDir | Out-Null
}

Write-Host "Importing payroll Excel data..."
Write-Host "Source SQL: $sqlPath"

javac -encoding UTF-8 -d $runnerClassDir $runnerJavaPath
if ($LASTEXITCODE -ne 0) {
  throw "Failed to compile SQL file runner."
}

java -cp "$($postgresJar.FullName);$runnerClassDir" SqlFileRunner "jdbc:postgresql://localhost:5432/abms" "abms" "abms_password" $sqlPath
if ($LASTEXITCODE -ne 0) {
  throw "Failed to import payroll Excel data into PostgreSQL."
}

Write-Host ""
Write-Host "Import result:"
java -cp "$($postgresJar.FullName);$runnerClassDir" SqlFileRunner "jdbc:postgresql://localhost:5432/abms" "abms" "abms_password" (Join-Path $root "database\seed\008_payroll_import_count_check.sql")
if ($LASTEXITCODE -ne 0) {
  throw "Failed to confirm imported row counts."
}
