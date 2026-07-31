@echo off
set "PATH=C:\Program Files\nodejs;%APPDATA%\npm;%PATH%"
powershell.exe -NoProfile -ExecutionPolicy Bypass -File "%~dp0start-frontend.ps1"
exit /b %ERRORLEVEL%
