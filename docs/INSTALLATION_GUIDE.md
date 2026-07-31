# ABMS Installation Guide

This guide is for Windows 11 development.

## 1. Install VS Code

Download and install Visual Studio Code:

```text
https://code.visualstudio.com/
```

Recommended extensions:

- Angular Language Service
- Extension Pack for Java
- Spring Boot Extension Pack
- Docker

## 2. Install Git

Download Git for Windows:

```text
https://git-scm.com/download/win
```

Use the default installer options unless your team has a different standard.

Verify:

```powershell
git --version
```

## 3. Install Node.js

Download Node.js LTS:

```text
https://nodejs.org/en/download
```

Verify:

```powershell
node --version
npm --version
```

## 4. Install Java 21

Download Eclipse Temurin JDK 21:

```text
https://adoptium.net/temurin/releases/?version=21
```

Choose:

```text
Operating System: Windows
Architecture: x64
Package Type: JDK
Version: 21
```

Verify:

```powershell
java -version
```

The version should show Java 21.

## 5. Install Maven

Download Apache Maven:

```text
https://maven.apache.org/download.cgi
```

Unzip Maven and add the `bin` folder to the Windows `Path` environment variable.

Verify:

```powershell
mvn --version
```

## 6. Install Docker Desktop

Download Docker Desktop:

```text
https://www.docker.com/products/docker-desktop/
```

During installation, allow WSL 2 integration if prompted. Restart Windows if Docker asks for it.

Verify:

```powershell
docker --version
docker compose version
```

## 7. Optional Helper Script

From the `abms` folder, run:

```powershell
.\scripts\install-tools-windows.ps1
```

If `winget` exists, the script installs the required tools. If `winget` is not available, it opens the official download pages.

## 8. Final Environment Check

Run:

```powershell
.\scripts\check-env.ps1
```

All tools should print version information.
