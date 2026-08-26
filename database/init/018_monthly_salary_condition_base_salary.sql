ALTER TABLE monthly_salary_calculation_conditions
  ADD COLUMN IF NOT EXISTS base_salary NUMERIC(15, 2);

UPDATE monthly_salary_calculation_conditions c
SET base_salary = COALESCE(e.base_salary, 0)
FROM employee_masters e
WHERE e.employee_no = c.employee_no
  AND c.base_salary IS NULL;
