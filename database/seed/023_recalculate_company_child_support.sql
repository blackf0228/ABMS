WITH base AS (
  SELECT
    d.employee_no,
    d.payment_month,
    COALESCE(c.base_salary, e.base_salary, 0)
      + COALESCE(c.commuting_allowance, 0)
      + COALESCE(c.management_allowance, 0)
      + COALESCE(c.transportation_adjustment, 0)
      + COALESCE(c.overtime_pay, 0) AS remuneration
  FROM monthly_salary_details d
  JOIN employee_masters e
    ON e.employee_no = d.employee_no
  LEFT JOIN monthly_salary_calculation_conditions c
    ON c.employee_no = d.employee_no
   AND c.payment_month = d.payment_month
  WHERE e.employee_type = '0'
),
calculated AS (
  SELECT
    b.employee_no,
    b.payment_month,
    COALESCE(sp.child_support_employer_amount, 0) AS company_child_support
  FROM base b
  LEFT JOIN LATERAL (
    SELECT child_support_employer_amount
    FROM social_insurance_premium_amounts
    WHERE REPLACE(payment_month, '-', '') <= b.payment_month
      AND (monthly_remuneration_from IS NULL OR monthly_remuneration_from <= b.remuneration)
      AND (monthly_remuneration_to IS NULL OR monthly_remuneration_to > b.remuneration)
    ORDER BY REPLACE(payment_month, '-', '') DESC, no
    LIMIT 1
  ) sp ON TRUE
)
UPDATE monthly_salary_details d
SET company_child_support_amount = c.company_child_support,
    company_total_amount = COALESCE(d.company_employment_insurance_amount, 0)
      + COALESCE(d.workers_accident_insurance_amount, 0)
      + COALESCE(d.company_pension_insurance_amount, 0)
      + COALESCE(d.company_health_insurance_amount, 0)
      + c.company_child_support
FROM calculated c
WHERE d.employee_no = c.employee_no
  AND d.payment_month = c.payment_month;
