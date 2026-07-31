$ErrorActionPreference = "Stop"

$extensions = @(
  "mtxr.sqltools",
  "mtxr.sqltools-driver-pg"
)

Write-Host "Installing VS Code database extensions..."

foreach ($extension in $extensions) {
  Write-Host "Installing: $extension"
  code --install-extension $extension --force
}

Write-Host ""
Write-Host "Done."
Write-Host "Please restart VS Code, then open SQLTools from the left sidebar."
