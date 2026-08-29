# 勘定外費用一覧 Implementation Plan

> **For agentic workers:** REQUIRED SUB-SKILL: Use superpowers:subagent-driven-development (recommended) or superpowers:executing-plans to implement this plan task-by-task.

**Goal:** Add a 勘定外費用一覧 screen with date/担当者 search and modal create/edit operations.

**Architecture:** Add a PostgreSQL table and a Spring REST controller. Extend the existing single Angular application with a 共通管理 menu item, list state, and reusable modal form following the employee/customer screens.

**Tech Stack:** Angular 20, TypeScript, Java 21, Spring Boot 3.5, NamedParameterJdbcTemplate, PostgreSQL 17.

**Spec:** Approved in chat on 2026-08-28 from 要件/0828勘定外費用一覧.xlsx.

**Global Constraints**
- Store employee IDs for 担当者 and display 姓＋名 from employee_masters.
- Preserve negative expense amounts.
- Use existing project UI, CORS, API, and database initialization conventions.

### Task 1: Database and API

**Files:**
- Create: database/init/020_off_book_expenses.sql
- Create: backend/src/main/java/com/aba/abms/offbook/OffBookExpenseController.java
- Create: backend/src/main/java/com/aba/abms/offbook/OffBookExpenseItem.java
- Create: backend/src/main/java/com/aba/abms/offbook/OffBookExpenseRequest.java

- [ ] Add table columns for expense number, received date, handler employee number, amount, currency, and reason, with a primary key and employee foreign key.
- [ ] Add GET search, POST create, and PUT update endpoints with handler name joins.
- [ ] Add validation for required date, handler, amount, and reason.

### Task 2: Angular Screen

**Files:**
- Modify: frontend/src/app/app.ts
- Modify: frontend/src/app/app.html
- Modify: frontend/src/app/app.css

- [ ] Add the 共通管理 navigation item and view state.
- [ ] Add search form, result table, new button, and edit modal.
- [ ] Display handler names while submitting employee IDs.
- [ ] Add error/success messages and empty-result state.

### Task 3: Verification

- [ ] Compile the backend with Maven and build the frontend with Angular.
- [ ] Verify the new table exists and the API source uses the approved fields.
