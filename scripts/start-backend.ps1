$ErrorActionPreference = "Stop"

Set-Location (Join-Path $PSScriptRoot "..\backend")

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
