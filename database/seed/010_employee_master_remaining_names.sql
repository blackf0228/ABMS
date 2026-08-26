-- Add remaining names found in contracts/sales that were not in the explicit name list.
-- The source values are two- or three-character names, so the first character is used as surname.
-- 彭闖 is an alias of 小林　和成. 1-character source value 耿 is held for clarification.
WITH requested(last_name, first_name) AS (
  VALUES
    ('テキ', '羽佳'), ('于', '吉男'), ('付', '聡'), ('付', '聰'), ('利', '炳根'), ('劉', '燕'),
    ('呂', '景全'), ('孔', '令豪'), ('孟', '君'), ('孫', '丹丹'), ('張', '丹丹'), ('張', '余松'),
    ('張', '俊傑'), ('張', '寧'), ('彭', '歓'), ('李', 'キイ'), ('李', '冰'), ('李', '骥韡'),
    ('王', '佳林'), ('王', '玉峰'), ('王', '玉鋒'), ('王', '碩'), ('王', '遵續'), ('王', '鑫'),
    ('盧', '源慶'), ('翟', '羽佳'), ('趙', '維棟'), ('金', '峰勛'), ('関', '鑫'), ('韓', '明昊'),
    ('馬', '兆洤'), ('黄', '河'), ('黄', '璜')
), candidates AS (
  SELECT r.*
  FROM requested r
  WHERE NOT EXISTS (
    SELECT 1
    FROM employee_masters e
    WHERE replace(replace(trim(e.employee_name), '　', ''), ' ', '') = r.last_name || r.first_name
  )
), numbered AS (
  SELECT
    'B' || lpad((COALESCE((
      SELECT max(substring(employee_no from 2)::integer)
      FROM employee_masters
      WHERE employee_no ~ '^B[0-9]{3}$'
    ), -1) + row_number() over (ORDER BY c.last_name, c.first_name))::text, 3, '0') AS employee_no,
    c.last_name,
    c.first_name
  FROM candidates c
)
INSERT INTO employee_masters (
  employee_no, employee_name, last_name, first_name, employee_type,
  base_salary, dependent_count, deduction_count, consumption_tax_rate, deletion_flag
)
SELECT employee_no, last_name || '　' || first_name, last_name, first_name, '2', 0, 0, 0, 0, '0'
FROM numbered;
