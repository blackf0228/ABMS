WITH base AS (
  SELECT
    d.employee_no,
    d.payment_month,
    COALESCE(c.base_salary, e.base_salary, 0)
      + COALESCE(c.commuting_allowance, 0)
      + COALESCE(c.management_allowance, 0)
      + COALESCE(c.transportation_adjustment, 0)
      + COALESCE(c.overtime_pay, 0) AS remuneration,
    TO_DATE(d.payment_month || '01', 'YYYYMMDD') AS payment_date
  FROM monthly_salary_details d
  JOIN employee_masters e
    ON e.employee_no = d.employee_no
  LEFT JOIN monthly_salary_calculation_conditions c
    ON c.employee_no = d.employee_no
   AND c.payment_month = d.payment_month
  WHERE e.employee_type = '0'
    AND d.payment_month >= '202606'
),
calculated AS (
  SELECT
    b.employee_no,
    b.payment_month,
    d.company_employment_insurance_amount AS old_company_employment,
    d.workers_accident_insurance_amount AS old_workers_accident,
    d.company_pension_insurance_amount AS old_company_pension,
    d.company_health_insurance_amount AS old_company_health,
    d.company_total_amount AS old_company_total,
    CASE
      WHEN b.employee_no = '0010' THEN 0
      WHEN er.employer_rate IS NULL THEN d.company_employment_insurance_amount
      ELSE ROUND(b.remuneration * er.employer_rate, 2)
    END AS company_employment,
    CASE
      WHEN b.employee_no = '0010' THEN 0
      ELSE ROUND(b.remuneration * 0.003, 2)
    END AS workers_accident,
    CASE
      WHEN sp.payment_month IS NULL THEN d.company_pension_insurance_amount
      ELSE COALESCE(sp.pension_insurance_employer_amount, 0)
    END AS company_pension,
    CASE
      WHEN sp.payment_month IS NULL THEN d.company_health_insurance_amount
      ELSE COALESCE(sp.health_insurance_employer_amount, 0)
    END AS company_health
  FROM base b
  JOIN monthly_salary_details d
    ON d.employee_no = b.employee_no
   AND d.payment_month = b.payment_month
  LEFT JOIN LATERAL (
    SELECT worker_rate, employer_rate, total_rate
    FROM employment_insurance_rates
    WHERE effective_start_date <= b.payment_date
      AND effective_end_date >= b.payment_date
    ORDER BY no
    LIMIT 1
  ) er ON TRUE
  LEFT JOIN LATERAL (
    SELECT payment_month, pension_insurance_employer_amount, health_insurance_employer_amount
    FROM social_insurance_premium_amounts
    WHERE REPLACE(payment_month, '-', '') <= b.payment_month
      AND (monthly_remuneration_from IS NULL OR monthly_remuneration_from <= b.remuneration)
      AND (monthly_remuneration_to IS NULL OR monthly_remuneration_to > b.remuneration)
    ORDER BY REPLACE(payment_month, '-', '') DESC, no
    LIMIT 1
  ) sp ON TRUE
)
UPDATE monthly_salary_details d
SET company_employment_insurance_amount = c.company_employment,
    workers_accident_insurance_amount = c.workers_accident,
    company_pension_insurance_amount = c.company_pension,
    company_health_insurance_amount = c.company_health,
    company_total_amount = COALESCE(c.company_employment, 0)
      + COALESCE(c.workers_accident, 0)
      + COALESCE(c.company_pension, 0)
      + COALESCE(c.company_health, 0)
FROM calculated c
WHERE d.employee_no = c.employee_no
  AND d.payment_month = c.payment_month;
