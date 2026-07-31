# ABMS Startup Guide

This guide starts the first-phase ABMS skeleton only.

## 1. Open Project

Open this folder in VS Code:

```text
abms
```

## 2. Start PostgreSQL 17

Open a PowerShell terminal in the `abms` folder:

```powershell
.\scripts\start-database.cmd
```

This starts PostgreSQL with:

```text
Host: localhost
Port: 5432
Database: abms
User: abms
Password: abms_password
```

## 3. Start Backend

Open a second PowerShell terminal:

```powershell
.\scripts\start-backend.cmd
```

Backend URL:

```text
http://localhost:8080
```

Hello API:

```text
http://localhost:8080/api/hello
```

Expected API response:

```json
{
  "message": "Hello ABA"
}
```

## 4. Start Frontend

Open a third PowerShell terminal:

```powershell
.\scripts\start-frontend.cmd
```

Frontend URL:

```text
http://localhost:4200
```

Expected browser display:

```text
Hello ABA
```

After the billing request screens are added, the browser shows the ABMS application with:

```text
登録
検索
```

Use `登録` to save billing request data. Use `検索` to search and show saved rows.

## 5. Stop Services

Stop Angular and Spring Boot with `Ctrl+C` in their terminals.

Stop PostgreSQL:

```powershell
docker compose down
```

To delete the local database volume:

```powershell
docker compose down -v
```
