CREATE TABLE IF NOT EXISTS customer_masters (
  company_id VARCHAR(20) PRIMARY KEY,
  company_name VARCHAR(200) NOT NULL
);

CREATE TABLE IF NOT EXISTS sales_imports (
  no INTEGER PRIMARY KEY,
  billing_source_company_id VARCHAR(20) NOT NULL,
  billing_destination_company_id VARCHAR(20) NOT NULL,
  billing_month DATE NOT NULL,
  engineer_name VARCHAR(100) NOT NULL,
  billing_amount NUMERIC(15, 2) NOT NULL
);

CREATE TABLE IF NOT EXISTS contracts (
  contract_no INTEGER PRIMARY KEY,
  engineer_type VARCHAR(20),
  engineer_name VARCHAR(100) NOT NULL,
  contract_amount NUMERIC(15, 2),
  customer_name VARCHAR(200),
  contract_start_date DATE,
  contract_end_date DATE,
  settlement_lower_hour INTEGER,
  settlement_upper_hour INTEGER,
  engineer_unit_price NUMERIC(15, 2),
  engineer_company_name VARCHAR(200),
  project_affiliation VARCHAR(4) NOT NULL DEFAULT '',
  engineer_proposer VARCHAR(4) NOT NULL DEFAULT '',
  operation_status VARCHAR(50)
);

ALTER TABLE contracts
  ADD COLUMN IF NOT EXISTS project_affiliation VARCHAR(4) NOT NULL DEFAULT '',
  ADD COLUMN IF NOT EXISTS engineer_proposer VARCHAR(4) NOT NULL DEFAULT '',
  ADD COLUMN IF NOT EXISTS settlement_lower_hour INTEGER,
  ADD COLUMN IF NOT EXISTS settlement_upper_hour INTEGER;

CREATE INDEX IF NOT EXISTS idx_sales_imports_billing_month
  ON sales_imports (billing_month);

CREATE INDEX IF NOT EXISTS idx_sales_imports_destination_month_engineer
  ON sales_imports (billing_destination_company_id, billing_month, engineer_name);

CREATE INDEX IF NOT EXISTS idx_contracts_engineer_name
  ON contracts (engineer_name);

CREATE INDEX IF NOT EXISTS idx_contracts_operation_status
  ON contracts (operation_status);

INSERT INTO customer_masters (company_id, company_name) VALUES
  ('0059', '株式会社大通ソフト'),
  ('0060', '株式会社OnePlus-Tech'),
  ('0061', 'MCTJAPAN株式会社'),
  ('0062', 'ソフトシンク株式会社'),
  ('0063', 'ETC株式会社')
ON CONFLICT (company_id) DO UPDATE
SET company_name = EXCLUDED.company_name;
