# 2026年8月・9月売上明細作成 Implementation Plan

> **For agentic workers:** REQUIRED SUB-SKILL: Use superpowers:subagent-driven-development (recommended) or superpowers:executing-plans to implement this plan task-by-task. Steps use checkbox (`- [ ]`) syntax for tracking.

**Goal:** 2026年8月・9月に有効な契約から、超過精算・不足控除を含まない売上明細を作成する。

**Architecture:** `contracts`を月単位で抽出し、`customer_masters`で会社名を会社IDへ変換する。CMC請求分は契約単価、BP支払分は技術者単価を` sales_imports`へ登録し、同一年月・請求元・請求先・技術者・金額の既存行は再登録しない。

**Tech Stack:** PostgreSQL 17、SQL

**Spec:** ユーザー指定（2026年8月・9月、勤怠非参照、超過・不足なし）

## Global Constraints

- 対象年月は`202608`と`202609`。
- `稼働状況 = 契約中`かつ対象月に契約期間が重なる契約だけを対象とする。
- 勤怠データは参照・作成しない。
- 超過精算・不足控除の行は作成しない。
- 会社名は`customer_masters.company_id`へ変換する。
- 既存の売上明細を上書き・削除しない。

---

### Task 1: 登録候補の検証

**Files:**
- Create: `database/seed/026_create_sales_202608_202609_from_contracts.sql`

**Interfaces:**
- Consumes: `contracts`, `customer_masters`, `sales_imports`
- Produces: 2026年8月・9月の売上明細

- [x] **Step 1: 既存データ件数を確認する**

`sales_imports`の2026年8月・9月が0件であることを確認する。

- [x] **Step 2: 契約候補を確認する**

8月は契約27件・BP支払11件、9月は契約32件・BP支払14件で、会社ID未変換が0件であることを確認する。

### Task 2: 売上明細の登録

**Files:**
- Create: `database/seed/026_create_sales_202608_202609_from_contracts.sql`

**Interfaces:**
- Consumes: Task 1で検証した契約候補
- Produces: 再実行可能なINSERT処理

- [x] **Step 1: 会社ID変換の検証を実装する**

会社IDが見つからない契約がある場合、例外で登録を中止する。

- [x] **Step 2: CMC請求分を作成する**

請求元`0000`、請求先を取引先会社ID、金額を契約単価として作成する。

- [x] **Step 3: BP支払分を作成する**

技術者所属会社が空白・CMC以外の場合、請求元を所属会社ID、請求先`0000`、金額を技術者単価として作成する。

- [x] **Step 4: DBへ登録する**

自然キー相当の既存行を除外し、`MAX(no) + ROW_NUMBER()`で採番して登録する。

### Task 3: 登録結果の検証

**Files:**
- Verify: `database/seed/026_create_sales_202608_202609_from_contracts.sql`

**Interfaces:**
- Consumes: 登録後の`sales_imports`
- Produces: 月別件数・金額・整合性結果

- [x] **Step 1: 件数と金額を確認する**

8月・9月について、請求元`0000`とそれ以外を分けて件数・合計金額を確認する。

- [x] **Step 2: 契約との対応を確認する**

登録対象契約ごとに基本売上行が1件存在し、会社ID未変換・重複採番がないことを確認する。
