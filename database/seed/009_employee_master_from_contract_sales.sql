-- Add non-employee names used by contracts and sales to the employee master.
-- 彭　闖 is an alias of 小林　和成 and is intentionally excluded.
WITH requested(last_name, first_name) AS (
  VALUES
    ('燕', '洪彰'), ('王', '希'), ('王', '浩玉'), ('王', '彩霞'), ('王', '帥'), ('王', '超'),
    ('郭', '嘉'), ('韓', '明哲'), ('仇', '華忠'), ('喬', '一宸'), ('金', '楊'), ('顧', '平世'),
    ('呉', '暁亮'), ('高', '長源'), ('周', '剣華'), ('周', '瑶'), ('常', '家鑫'), ('曾', '維杰'),
    ('蘇', '芮烽'), ('孫', '偉東'), ('孫', '群'), ('遅', '帥'), ('張', '永恒'), ('張', '剣輝'),
    ('程', '偉'), ('陶', '然'), ('楊', '月'), ('李', '鴻民'), ('李', '超然'), ('李', '氷'),
    ('龍', '氷晶'), ('呂', '加舜'), ('呂', '煒'), ('于', '建勇'), ('于', '占玖'), ('于', '繁燁'),
    ('于', '洋'), ('彭', '闖'), ('耿', '琪'), ('詹', '重阔'), ('賴', '品維')
), candidates AS (
  SELECT r.*
  FROM requested r
  WHERE r.last_name || r.first_name <> '彭闖'
    AND NOT EXISTS (
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
  employee_no,
  employee_name,
  last_name,
  first_name,
  employee_type,
  base_salary,
  dependent_count,
  deduction_count,
  consumption_tax_rate,
  deletion_flag
)
SELECT
  employee_no,
  last_name || '　' || first_name,
  last_name,
  first_name,
  '2',
  0,
  0,
  0,
  0,
  '0'
FROM numbered;
