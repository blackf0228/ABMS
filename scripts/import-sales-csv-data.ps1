$ErrorActionPreference = "Stop"

$root = Resolve-Path (Join-Path $PSScriptRoot "..")
$initSqlPath = Join-Path $root "database\init\006_sales_tables.sql"
$outputDir = Join-Path $root "output"
$salesCsvPath = Join-Path $outputDir "sales_import.csv"
$customerCsvPath = Join-Path $outputDir "customer_master.csv"
$contractsCsvPath = Join-Path $outputDir "contracts.csv"
$importerJavaPath = Join-Path $PSScriptRoot "SalesCsvImporter.java"
$importerClassDir = Join-Path $root ".tools\sales-csv-importer"
$localPostgresJarPath = Join-Path $importerClassDir "postgresql.jar"
$postgresJar = Get-ChildItem -Path "$env:USERPROFILE\.m2\repository\org\postgresql\postgresql" -Recurse -Filter "postgresql-*.jar" -ErrorAction SilentlyContinue |
  Sort-Object FullName -Descending |
  Select-Object -First 1

$requiredFiles = @(
  $initSqlPath,
  $salesCsvPath,
  $customerCsvPath,
  $contractsCsvPath,
  $importerJavaPath
)

foreach ($file in $requiredFiles) {
  if (-not (Test-Path $file)) {
    throw "Required file was not found: $file"
  }
}

if ($null -eq $postgresJar) {
  throw "PostgreSQL JDBC driver was not found. Please start the backend once or build it so Maven can download the driver."
}

if (-not (Test-Path $importerClassDir)) {
  New-Item -ItemType Directory -Path $importerClassDir | Out-Null
}
Copy-Item -LiteralPath $postgresJar.FullName -Destination $localPostgresJarPath -Force

Write-Host "Importing sales CSV data..."
Write-Host "Sales CSV: $salesCsvPath"
Write-Host "Customer CSV: $customerCsvPath"
Write-Host "Contracts CSV: $contractsCsvPath"

javac -encoding UTF-8 -d $importerClassDir $importerJavaPath
if ($LASTEXITCODE -ne 0) {
  throw "Failed to compile sales CSV importer."
}

java -cp "$localPostgresJarPath;$importerClassDir" SalesCsvImporter $root "jdbc:postgresql://localhost:5432/abms" "abms" "abms_password" $initSqlPath
if ($LASTEXITCODE -ne 0) {
  throw "Failed to import sales CSV data into PostgreSQL."
}
