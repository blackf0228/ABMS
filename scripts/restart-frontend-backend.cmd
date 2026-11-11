@echo off
setlocal
powershell.exe -NoProfile -ExecutionPolicy Bypass -File "%~dp0restart-frontend-backend.ps1" -ProjectRoot "%~dp0.."
exit /b %ERRORLEVEL%
