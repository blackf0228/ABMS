$ErrorActionPreference = "Stop"

Set-Location (Join-Path $PSScriptRoot "..\frontend")
npm.cmd install
npm.cmd start
