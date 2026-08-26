BEGIN;

-- 付　聰 is the same person as 付　聡. Keep B024 and remove the duplicate B025.
UPDATE contracts
SET engineer_name = '付聡'
WHERE engineer_name IN ('付聰', '付　聰');

UPDATE sales_imports
SET engineer_name = '付聡'
WHERE engineer_name IN ('付聰', '付　聰');

UPDATE employee_masters
SET employee_name = '付　聡',
    last_name = '付',
    first_name = '聡',
    employee_type = '2'
WHERE employee_no = 'B024';

DELETE FROM employee_masters
WHERE employee_no = 'B025';

-- 孔　令豪 is a regular employee.
UPDATE employee_masters
SET employee_type = '0'
WHERE employee_no = 'B029';

COMMIT;
