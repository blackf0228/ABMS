BEGIN;

UPDATE monthly_salary_calculation_conditions
SET employee_no = '0007'
WHERE employee_no = '0004';

UPDATE monthly_salary_details
SET employee_no = '0007'
WHERE employee_no = '0004';

UPDATE employee_resident_tax_details
SET employee_no = '0007'
WHERE employee_no = '0004';

UPDATE employee_masters
SET employee_no = '0007'
WHERE employee_no = '0004'
  AND NOT EXISTS (SELECT 1 FROM employee_masters WHERE employee_no = '0007');

COMMIT;
