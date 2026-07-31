@echo off
setlocal EnableDelayedExpansion

set "PATH=C:\Program Files\Eclipse Adoptium\jdk-21.0.11.10-hotspot\bin;C:\Program Files\Git\cmd;C:\Program Files\nodejs;C:\Program Files\Docker\Docker\resources\bin;C:\Tools\apache-maven-3.9.16\bin;%APPDATA%\npm;%PATH%"

echo ABMS environment check
echo ======================
echo.

set "MISSING="

echo [Git]
where git >nul 2>nul
if errorlevel 1 (
  echo Not found
  set "MISSING=%MISSING% Git"
) else (
  git --version
)
echo.

echo [Node.js]
where node >nul 2>nul
if errorlevel 1 (
  echo Not found
  set "MISSING=%MISSING% Node.js"
) else (
  node --version
)
echo.

echo [npm]
where npm.cmd >nul 2>nul
if errorlevel 1 (
  echo Not found
  set "MISSING=%MISSING% npm"
) else (
  call npm.cmd --version
)
echo.

echo [Java]
where java >nul 2>nul
if errorlevel 1 (
  echo Not found
  set "MISSING=%MISSING% Java21"
) else (
  java -version 2>&1 | findstr /C:"21." >nul
  if errorlevel 1 (
    java -version 2>&1
    echo Java is installed, but ABMS requires Java 21.
    set "MISSING=%MISSING% Java21"
  ) else (
    java -version 2>&1
  )
)
echo.

echo [Maven]
if not exist "C:\Tools\apache-maven-3.9.16\boot\plexus-classworlds-*.jar" (
  echo Not found
  set "MISSING=%MISSING% Maven"
) else (
  for %%J in (C:\Tools\apache-maven-3.9.16\boot\plexus-classworlds-*.jar) do set "MAVEN_LAUNCHER=%%~fJ"
  java -classpath "!MAVEN_LAUNCHER!" "-Dclassworlds.conf=C:\Tools\apache-maven-3.9.16\bin\m2.conf" "-Dmaven.home=C:\Tools\apache-maven-3.9.16" "-Dlibrary.jansi.path=C:\Tools\apache-maven-3.9.16\lib\jansi-native" "-Dmaven.multiModuleProjectDirectory=%CD%" org.codehaus.plexus.classworlds.launcher.Launcher --version
  if errorlevel 1 set "MISSING=%MISSING% Maven"
)
echo.

echo [Docker]
where docker >nul 2>nul
if errorlevel 1 (
  echo Not found
  set "MISSING=%MISSING% Docker"
) else (
  docker --version
)
echo.

if not "%MISSING%"=="" (
  echo Missing or incorrect tools:%MISSING%
  echo.
  echo Please follow docs\WINDOWS_INSTALL_CN.md, then restart PowerShell and run this check again.
  exit /b 1
)

echo All required tools are ready.
exit /b 0
