@echo off
set "PATH=C:\Program Files\Docker\Docker\resources\bin;%PATH%"
powershell.exe -NoProfile -ExecutionPolicy Bypass -File "%~dp0import-payroll-initial-data.ps1"
exit /b %ERRORLEVEL%
