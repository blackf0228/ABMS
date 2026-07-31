$ErrorActionPreference = "Stop"

$root = Resolve-Path (Join-Path $PSScriptRoot "..")
$initSqlPath = Join-Path $root "database\init\005_income_tax_tables.sql"
$seedSqlPath = Join-Path $root "database\seed\006_income_tax_initial_data.sql"
$containerName = "abms-postgres"
$containerInitSqlPath = "/tmp/005_income_tax_tables.sql"
$containerSeedSqlPath = "/tmp/006_income_tax_initial_data.sql"

if (-not (Test-Path $initSqlPath)) {
  throw "SQL file was not found: $initSqlPath"
}
if (-not (Test-Path $seedSqlPath)) {
  throw "SQL file was not found: $seedSqlPath"
}

Write-Host "Importing income tax master..."
Write-Host "Table SQL: $initSqlPath"
Write-Host "Seed SQL : $seedSqlPath"

docker cp $initSqlPath "$containerName`:$containerInitSqlPath"
if ($LASTEXITCODE -ne 0) {
  throw "Failed to copy table SQL file to Docker container. Please confirm Docker Desktop is running and your user can access Docker."
}

docker cp $seedSqlPath "$containerName`:$containerSeedSqlPath"
if ($LASTEXITCODE -ne 0) {
  throw "Failed to copy seed SQL file to Docker container. Please confirm Docker Desktop is running and your user can access Docker."
}

docker exec -i $containerName psql -U abms -d abms -f $containerInitSqlPath
if ($LASTEXITCODE -ne 0) {
  throw "Failed to create income tax master table."
}

docker exec -i $containerName psql -U abms -d abms -f $containerSeedSqlPath
if ($LASTEXITCODE -ne 0) {
  throw "Failed to import income tax master data."
}

Write-Host ""
Write-Host "Import result:"
docker exec -i $containerName psql -U abms -d abms -c "select tax_year, salary_type, withholding_type, count(*) from income_tax_withholding_monthly_rates group by tax_year, salary_type, withholding_type order by withholding_type;"
if ($LASTEXITCODE -ne 0) {
  throw "Failed to confirm imported row counts."
}
