$ErrorActionPreference = "Stop"

Set-Location (Join-Path $PSScriptRoot "..\backend")

$port = 9000
$listener = Get-NetTCPConnection -LocalPort $port -State Listen -ErrorAction SilentlyContinue |
  Select-Object -First 1
if ($listener) {
  try {
    $health = Invoke-WebRequest -UseBasicParsing -Uri "http://127.0.0.1:$port/api/hello" -TimeoutSec 3
    if ($health.StatusCode -eq 200) {
      Write-Host "ABMS backend is already running on port $port."
      exit 0
    }
  } catch {
    # The port is occupied, but the process is not responding as ABMS.
  }
  throw "Port $port is already in use by another process. Stop that process or configure the backend to use another port."
}

$mavenHome = "C:\Tools\apache-maven-3.9.16"
$launcher = Get-ChildItem "$mavenHome\boot\plexus-classworlds-*.jar" | Select-Object -First 1

if (-not $launcher) {
  throw "Maven launcher was not found under $mavenHome. Please check Maven installation."
}

java `
  -classpath $launcher.FullName `
  "-Dclassworlds.conf=$mavenHome\bin\m2.conf" `
  "-Dmaven.home=$mavenHome" `
  "-Dlibrary.jansi.path=$mavenHome\lib\jansi-native" `
  "-Dmaven.multiModuleProjectDirectory=$PWD" `
  org.codehaus.plexus.classworlds.launcher.Launcher `
  spring-boot:run `
  "-Dmaven.test.skip=true"
