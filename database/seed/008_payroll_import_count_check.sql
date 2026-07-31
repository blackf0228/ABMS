SELECT 'employee_masters' AS table_name, COUNT(*) AS count FROM employee_masters;
SELECT 'monthly_salary_calculation_conditions' AS table_name, COUNT(*) AS count FROM monthly_salary_calculation_conditions;
SELECT 'monthly_salary_details' AS table_name, COUNT(*) AS count FROM monthly_salary_details;
SELECT 'employee_resident_tax_details' AS table_name, COUNT(*) AS count FROM employee_resident_tax_details;
SELECT 'employee_masters_consumption_tax_rate' AS table_name, COUNT(*) AS count FROM employee_masters WHERE consumption_tax_rate IS NOT NULL;
