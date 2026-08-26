ALTER TABLE employee_masters
  ADD COLUMN IF NOT EXISTS last_name VARCHAR(50),
  ADD COLUMN IF NOT EXISTS first_name VARCHAR(50);

CREATE OR REPLACE FUNCTION sync_employee_master_names()
RETURNS trigger
LANGUAGE plpgsql
AS $$
DECLARE
  parts text[];
BEGIN
  IF (NEW.last_name IS NULL OR btrim(NEW.last_name) = '')
     AND NEW.employee_name IS NOT NULL THEN
    parts := regexp_split_to_array(replace(btrim(NEW.employee_name), '　', ' '), '[[:space:]]+');
    NEW.last_name := parts[1];
    NEW.first_name := NULLIF(parts[2], '');
  END IF;
  IF (NEW.first_name IS NULL OR btrim(NEW.first_name) = '')
     AND NEW.employee_name IS NOT NULL THEN
    parts := regexp_split_to_array(replace(btrim(NEW.employee_name), '　', ' '), '[[:space:]]+');
    NEW.first_name := NULLIF(parts[2], '');
  END IF;
  IF NEW.last_name IS NOT NULL AND NEW.first_name IS NOT NULL THEN
    NEW.employee_name := NEW.last_name || '　' || NEW.first_name;
  END IF;
  RETURN NEW;
END;
$$;

DROP TRIGGER IF EXISTS trg_sync_employee_master_names ON employee_masters;
CREATE TRIGGER trg_sync_employee_master_names
BEFORE INSERT OR UPDATE ON employee_masters
FOR EACH ROW
EXECUTE FUNCTION sync_employee_master_names();

UPDATE employee_masters
SET
  last_name = split_part(replace(trim(employee_name), '　', ' '), ' ', 1),
  first_name = nullif(split_part(replace(trim(employee_name), '　', ' '), ' ', 2), '')
WHERE last_name IS NULL OR first_name IS NULL;

ALTER TABLE employee_masters
  ALTER COLUMN last_name SET NOT NULL,
  ALTER COLUMN first_name SET NOT NULL;
