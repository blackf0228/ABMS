$ErrorActionPreference = "Continue"

Write-Host "ABMS environment check"
Write-Host "======================"
Write-Host ""

$missing = New-Object System.Collections.Generic.List[string]

function Test-Command {
  param(
    [string] $Name,
    [string] $Command,
    [string[]] $CommandArgs
  )

  Write-Host "[$Name]"
  $found = Get-Command $Command -ErrorAction SilentlyContinue

  if (-not $found) {
    Write-Host "Not found"
    $script:missing.Add($Name)
    Write-Host ""
    return
  }

  & $Command @CommandArgs
  Write-Host ""
}

Test-Command -Name "Git" -Command "git" -CommandArgs @("--version")
Test-Command -Name "Node.js" -Command "node" -CommandArgs @("--version")
Test-Command -Name "npm" -Command "npm.cmd" -CommandArgs @("--version")

Write-Host "[Java]"
$java = Get-Command java -ErrorAction SilentlyContinue
if (-not $java) {
  Write-Host "Not found"
  $missing.Add("Java 21")
} else {
  $javaOutput = & cmd.exe /c "java -version 2>&1"
  $javaOutput
  $javaText = $javaOutput -join "`n"

  if ($javaText -notmatch 'version "21\.') {
    Write-Host "Java is installed, but ABMS requires Java 21."
    $missing.Add("Java 21")
  }
}
Write-Host ""

Test-Command -Name "Maven" -Command "mvn.cmd" -CommandArgs @("--version")
Test-Command -Name "Docker" -Command "docker" -CommandArgs @("--version")

if ($missing.Count -gt 0) {
  Write-Host "Missing or incorrect tools:"
  foreach ($item in $missing) {
    Write-Host "- $item"
  }
  Write-Host ""
  Write-Host "Please follow docs/WINDOWS_INSTALL_CN.md, then restart PowerShell and run this check again."
  exit 1
}

Write-Host "All required tools are ready."
