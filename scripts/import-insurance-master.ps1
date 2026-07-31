$ErrorActionPreference = "Stop"

$root = Resolve-Path (Join-Path $PSScriptRoot "..")
$sqlPath = Join-Path $root "database\seed\003_insurance_master_data.sql"
$containerName = "abms-postgres"
$containerSqlPath = "/tmp/003_insurance_master_data.sql"

if (-not (Test-Path $sqlPath)) {
  throw "SQL file was not found: $sqlPath"
}

Write-Host "Importing insurance master data..."
Write-Host "Source SQL: $sqlPath"

docker cp $sqlPath "$containerName`:$containerSqlPath"
if ($LASTEXITCODE -ne 0) {
  throw "Failed to copy SQL file to Docker container. Please confirm Docker Desktop is running and your user can access Docker."
}

docker exec -i $containerName psql -U abms -d abms -f $containerSqlPath
if ($LASTEXITCODE -ne 0) {
  throw "Failed to execute SQL file in PostgreSQL."
}

Write-Host ""
Write-Host "Import result:"
docker exec -i $containerName psql -U abms -d abms -c "select 'employment_insurance_rates' as table_name, count(*) from employment_insurance_rates union all select 'social_insurance_premium_amounts', count(*) from social_insurance_premium_amounts;"
if ($LASTEXITCODE -ne 0) {
  throw "Failed to confirm imported row counts."
}
