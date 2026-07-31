@echo off
set "PATH=C:\Program Files\Docker\Docker\resources\bin;%PATH%"
powershell.exe -NoProfile -ExecutionPolicy Bypass -File "%~dp0start-database.ps1"
exit /b %ERRORLEVEL%
