\i /tmp/006_sales_tables.sql

TRUNCATE TABLE sales_imports;
TRUNCATE TABLE contracts;
TRUNCATE TABLE customer_masters;

COPY customer_masters (
  company_id,
  company_name
)
FROM '/tmp/customer_master.csv'
WITH (FORMAT csv, HEADER true, ENCODING 'UTF8');

COPY sales_imports (
  no,
  billing_source_company_id,
  billing_destination_company_id,
  billing_month,
  engineer_name,
  billing_amount
)
FROM '/tmp/sales_import.csv'
WITH (FORMAT csv, HEADER true, ENCODING 'UTF8');

CREATE TEMP TABLE contracts_csv_staging (
  contract_no_text TEXT,
  engineer_type TEXT,
  engineer_name TEXT,
  contract_amount_text TEXT,
  customer_name TEXT,
  contract_start_date_text TEXT,
  contract_end_date_text TEXT,
  engineer_unit_price_text TEXT,
  engineer_company_name TEXT,
  operation_status TEXT
);

COPY contracts_csv_staging (
  contract_no_text,
  engineer_type,
  engineer_name,
  contract_amount_text,
  customer_name,
  contract_start_date_text,
  contract_end_date_text,
  engineer_unit_price_text,
  engineer_company_name,
  operation_status
)
FROM '/tmp/contracts.csv'
WITH (FORMAT csv, HEADER true, ENCODING 'UTF8');

INSERT INTO contracts (
  contract_no,
  engineer_type,
  engineer_name,
  contract_amount,
  customer_name,
  contract_start_date,
  contract_end_date,
  engineer_unit_price,
  engineer_company_name,
  operation_status
)
SELECT
  NULLIF(TRIM(contract_no_text), '')::INTEGER,
  NULLIF(TRIM(engineer_type), ''),
  NULLIF(TRIM(engineer_name), ''),
  NULLIF(REGEXP_REPLACE(contract_amount_text, '[^0-9.-]', '', 'g'), '')::NUMERIC(15, 2),
  NULLIF(TRIM(customer_name), ''),
  CASE
    WHEN NULLIF(TRIM(contract_start_date_text), '') IS NULL THEN NULL
    ELSE TO_DATE(
      REGEXP_REPLACE(TRIM(contract_start_date_text), '^(\d{4})年(\d{1,2})月(\d{1,2})日$', '\1-\2-\3'),
      'YYYY-MM-DD'
    )
  END,
  CASE
    WHEN NULLIF(TRIM(contract_end_date_text), '') IS NULL THEN NULL
    ELSE TO_DATE(
      REGEXP_REPLACE(TRIM(contract_end_date_text), '^(\d{4})年(\d{1,2})月(\d{1,2})日$', '\1-\2-\3'),
      'YYYY-MM-DD'
    )
  END,
  NULLIF(REGEXP_REPLACE(engineer_unit_price_text, '[^0-9.-]', '', 'g'), '')::NUMERIC(15, 2),
  NULLIF(TRIM(engineer_company_name), ''),
  NULLIF(TRIM(operation_status), '')
FROM contracts_csv_staging
WHERE NULLIF(TRIM(contract_no_text), '') IS NOT NULL;
