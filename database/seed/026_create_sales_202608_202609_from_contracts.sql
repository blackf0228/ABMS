BEGIN;

DO $$
BEGIN
  IF EXISTS (
    WITH months(month_start) AS (
      VALUES (DATE '2026-08-01'), (DATE '2026-09-01')
    )
    SELECT 1
    FROM months m
    JOIN contracts c
      ON c.operation_status = '契約中'
     AND c.contract_start_date <= (m.month_start + INTERVAL '1 month - 1 day')::date
     AND (c.contract_end_date IS NULL OR c.contract_end_date >= m.month_start)
    LEFT JOIN customer_masters customer
      ON trim(customer.company_name) = trim(c.customer_name)
    WHERE customer.company_id IS NULL
       OR c.contract_amount IS NULL
  ) THEN
    RAISE EXCEPTION '取引先会社IDまたは契約単価を解決できない対象契約があります。';
  END IF;

  IF EXISTS (
    WITH months(month_start) AS (
      VALUES (DATE '2026-08-01'), (DATE '2026-09-01')
    )
    SELECT 1
    FROM months m
    JOIN contracts c
      ON c.operation_status = '契約中'
     AND c.contract_start_date <= (m.month_start + INTERVAL '1 month - 1 day')::date
     AND (c.contract_end_date IS NULL OR c.contract_end_date >= m.month_start)
    LEFT JOIN customer_masters bp
      ON trim(bp.company_name) = trim(c.engineer_company_name)
    WHERE nullif(trim(c.engineer_company_name), '') IS NOT NULL
      AND trim(c.engineer_company_name) <> '株式会社CMCソフト'
      AND (bp.company_id IS NULL OR c.engineer_unit_price IS NULL)
  ) THEN
    RAISE EXCEPTION '技術者所属会社IDまたは技術者単価を解決できない対象契約があります。';
  END IF;
END
$$;

WITH months(month_start) AS (
  VALUES (DATE '2026-08-01'), (DATE '2026-09-01')
), active_contracts AS (
  SELECT
    m.month_start AS billing_month,
    c.contract_no,
    c.engineer_name,
    c.contract_amount,
    c.engineer_unit_price,
    customer.company_id AS customer_id,
    bp.company_id AS bp_company_id
  FROM months m
  JOIN contracts c
    ON c.operation_status = '契約中'
   AND c.contract_start_date <= (m.month_start + INTERVAL '1 month - 1 day')::date
   AND (c.contract_end_date IS NULL OR c.contract_end_date >= m.month_start)
  JOIN customer_masters customer
    ON trim(customer.company_name) = trim(c.customer_name)
  LEFT JOIN customer_masters bp
    ON trim(bp.company_name) = trim(c.engineer_company_name)
), candidate_rows AS (
  SELECT
    billing_month,
    contract_no,
    1 AS row_type_order,
    '0000'::varchar AS billing_source_company_id,
    customer_id::varchar AS billing_destination_company_id,
    engineer_name,
    contract_amount AS billing_amount
  FROM active_contracts

  UNION ALL

  SELECT
    billing_month,
    contract_no,
    2 AS row_type_order,
    bp_company_id::varchar AS billing_source_company_id,
    '0000'::varchar AS billing_destination_company_id,
    engineer_name,
    engineer_unit_price AS billing_amount
  FROM active_contracts
  WHERE bp_company_id IS NOT NULL
    AND bp_company_id <> '0000'
), rows_to_insert AS (
  SELECT candidate.*
  FROM candidate_rows candidate
  WHERE NOT EXISTS (
    SELECT 1
    FROM sales_imports existing
    WHERE existing.billing_month = candidate.billing_month
      AND existing.billing_source_company_id = candidate.billing_source_company_id
      AND existing.billing_destination_company_id = candidate.billing_destination_company_id
      AND existing.engineer_name = candidate.engineer_name
      AND existing.billing_amount = candidate.billing_amount
  )
), numbered_rows AS (
  SELECT
    (
      COALESCE((SELECT max(no) FROM sales_imports), 0)
      + row_number() OVER (
          ORDER BY billing_month, row_type_order, billing_destination_company_id,
                   billing_source_company_id, engineer_name, contract_no
        )
    )::integer AS no,
    billing_source_company_id,
    billing_destination_company_id,
    billing_month,
    engineer_name,
    billing_amount
  FROM rows_to_insert
)
INSERT INTO sales_imports (
  no,
  billing_source_company_id,
  billing_destination_company_id,
  billing_month,
  engineer_name,
  billing_amount
)
SELECT
  no,
  billing_source_company_id,
  billing_destination_company_id,
  billing_month,
  engineer_name,
  billing_amount
FROM numbered_rows
ORDER BY no;

COMMIT;
