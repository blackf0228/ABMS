BEGIN;

WITH employee_mapping AS (
  SELECT employee_no,
         replace(replace(trim(coalesce(last_name, '') || ' ' || coalesce(first_name, '')), ' ', ''), '　', '') AS normalized_name,
         row_number() over (
           PARTITION BY replace(replace(trim(coalesce(last_name, '') || ' ' || coalesce(first_name, '')), ' ', ''), '　', '')
           ORDER BY CASE WHEN deletion_flag = '0' THEN 0 ELSE 1 END, employee_no
         ) AS priority
  FROM employee_masters
), name_mapping AS (
  SELECT employee_no, normalized_name FROM employee_mapping WHERE priority = 1
  UNION ALL
  SELECT employee_no, '彭闖' FROM employee_mapping WHERE normalized_name = '小林和成'
)
UPDATE contracts c
SET engineer_name = (
  SELECT employee_no
  FROM name_mapping m
  WHERE m.normalized_name = replace(replace(trim(c.engineer_name), ' ', ''), '　', '')
  LIMIT 1
)
WHERE EXISTS (
  SELECT 1
  FROM name_mapping m
  WHERE m.normalized_name = replace(replace(trim(c.engineer_name), ' ', ''), '　', '')
);

WITH employee_mapping AS (
  SELECT employee_no,
         replace(replace(trim(coalesce(last_name, '') || ' ' || coalesce(first_name, '')), ' ', ''), '　', '') AS normalized_name,
         row_number() over (
           PARTITION BY replace(replace(trim(coalesce(last_name, '') || ' ' || coalesce(first_name, '')), ' ', ''), '　', '')
           ORDER BY CASE WHEN deletion_flag = '0' THEN 0 ELSE 1 END, employee_no
         ) AS priority
  FROM employee_masters
), name_mapping AS (
  SELECT employee_no, normalized_name FROM employee_mapping WHERE priority = 1
  UNION ALL
  SELECT employee_no, '彭闖' FROM employee_mapping WHERE normalized_name = '小林和成'
)
UPDATE sales_imports s
SET engineer_name = (
  SELECT employee_no
  FROM name_mapping m
  WHERE m.normalized_name = replace(replace(trim(s.engineer_name), ' ', ''), '　', '')
  LIMIT 1
)
WHERE EXISTS (
  SELECT 1
  FROM name_mapping m
  WHERE m.normalized_name = replace(replace(trim(s.engineer_name), ' ', ''), '　', '')
);

WITH employee_mapping AS (
  SELECT employee_no,
         replace(replace(trim(coalesce(last_name, '') || ' ' || coalesce(first_name, '')), ' ', ''), '　', '') AS normalized_name,
         row_number() over (
           PARTITION BY replace(replace(trim(coalesce(last_name, '') || ' ' || coalesce(first_name, '')), ' ', ''), '　', '')
           ORDER BY CASE WHEN deletion_flag = '0' THEN 0 ELSE 1 END, employee_no
         ) AS priority
  FROM employee_masters
), name_mapping AS (
  SELECT employee_no, normalized_name FROM employee_mapping WHERE priority = 1
  UNION ALL
  SELECT employee_no, '彭闖' FROM employee_mapping WHERE normalized_name = '小林和成'
)
UPDATE attendance_hours a
SET engineer_name = (
  SELECT employee_no
  FROM name_mapping m
  WHERE m.normalized_name = replace(replace(trim(a.engineer_name), ' ', ''), '　', '')
  LIMIT 1
)
WHERE EXISTS (
  SELECT 1
  FROM name_mapping m
  WHERE m.normalized_name = replace(replace(trim(a.engineer_name), ' ', ''), '　', '')
);

COMMIT;
