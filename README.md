# ABMS (ABA Business Management System)

ABMS is an ABA Business Management System. This repository currently contains only the first-phase technical skeleton.

## Phase 1 Scope

- Angular 20 frontend project
- Java 21 + Spring Boot 3.5 backend project
- PostgreSQL 17 Docker service
- Maven backend build
- Hello World API
- Angular page calling the Hello API
- Browser displays `Hello ABA`
- Startup and installation documentation

No business features have been added yet.

## Project Structure

```text
abms/
  backend/            Spring Boot 3.5 API
  frontend/           Angular 20 application
  database/init/      PostgreSQL initialization scripts
  docs/               Installation and startup guides
  scripts/            Windows helper scripts
  docker-compose.yml  PostgreSQL 17 Docker Compose file
```

## Required Tools

- Windows 11
- VS Code
- Git
- Node.js LTS
- Java 21
- Maven
- Docker Desktop

Run this after installing tools:

```powershell
.\scripts\check-env.cmd
```

## Quick Start

Open three PowerShell terminals in the `abms` folder.

Terminal 1:

```powershell
.\scripts\start-database.cmd
```

Terminal 2:

```powershell
.\scripts\start-backend.cmd
```

Terminal 3:

```powershell
.\scripts\start-frontend.cmd
```

Then open:

```text
http://localhost:4200
```

Expected result:

```text
Hello ABA
```

## API

```http
GET http://localhost:8080/api/hello
```

Response:

```json
{
  "message": "Hello ABA"
}
```

## Billing Request Screens

The second small feature adds:

- 登録 screen for billing requests
- 検索 screen for billing requests
- Left-side navigation between the two screens
- PostgreSQL table `billing_requests`
- Auto-generated primary key column `no`

The backend creates or updates the development table automatically when Spring Boot starts.

## Insurance Master Tables

The backend also defines two insurance master tables:

- `employment_insurance_rates`
  - Stores employment insurance rates for the period from 2026-04-01 to 2027-03-31.
  - Primary key: `no`
- `social_insurance_premium_amounts`
  - Stores health insurance and employee pension insurance premium amount rows from 2026-03 salary period / 2026-04 payment period.
  - Primary key: `no`

DDL is available in `database/init/002_insurance_tables.sql`.

PDF-based seed data is available in `database/seed/003_insurance_master_data.sql`.
To import it into the local PostgreSQL container:

```powershell
.\scripts\import-insurance-master.cmd
```

## Payroll Tables

The backend defines four payroll-related tables:

- `employee_masters`
- `monthly_salary_calculation_conditions`
- `monthly_salary_details`
- `employee_resident_tax_details`

DDL is available in `database/init/004_payroll_tables.sql`.

Initial payroll seed data is available in `database/seed/005_payroll_initial_data.sql`.
To import it into the local PostgreSQL container:

```powershell
.\scripts\import-payroll-initial-data.cmd
```

## Documentation

- [Installation Guide](docs/INSTALLATION_GUIDE.md)
- [Windows Chinese Installation Guide](docs/WINDOWS_INSTALL_CN.md)
- [Startup Guide](docs/STARTUP_GUIDE.md)
