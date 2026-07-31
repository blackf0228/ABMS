$ErrorActionPreference = "Stop"

Write-Host "ABMS Windows tool installation helper"
Write-Host "====================================="
Write-Host ""

$winget = Get-Command winget -ErrorAction SilentlyContinue

if ($winget) {
  Write-Host "winget found. Installing required tools..."
  winget install --id Git.Git -e
  winget install --id OpenJS.NodeJS.LTS -e
  winget install --id EclipseAdoptium.Temurin.21.JDK -e
  winget install --id Apache.Maven -e
  winget install --id Docker.DockerDesktop -e
  Write-Host ""
  Write-Host "Installation commands finished. Restart PowerShell, then run scripts/check-env.ps1."
  exit 0
}

Write-Host "winget was not found on this computer."
Write-Host "Opening official download pages. Install each tool, then restart PowerShell."
Write-Host ""

$links = @(
  "https://git-scm.com/download/win",
  "https://nodejs.org/en/download",
  "https://adoptium.net/temurin/releases/?version=21",
  "https://maven.apache.org/download.cgi",
  "https://www.docker.com/products/docker-desktop/"
)

foreach ($link in $links) {
  Start-Process $link
}

Write-Host "After installation, run: .\scripts\check-env.ps1"
