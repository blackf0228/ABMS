-- One-time migration. Preserve existing master values for historical payroll.
BEGIN;
CREATE TABLE IF NOT EXISTS employee_master_backup_before_history AS TABLE employee_masters;
CREATE TABLE employee_numbers (employee_no VARCHAR(20) PRIMARY KEY);
INSERT INTO employee_numbers SELECT employee_no FROM employee_masters;

-- Employee numbers remain stable even when the master has multiple versions.
DO $migration$
DECLARE fk RECORD;
BEGIN
  FOR fk IN SELECT conrelid::regclass AS relation, conname, pg_get_constraintdef(oid) AS definition
            FROM pg_constraint WHERE contype = 'f' AND confrelid = 'employee_masters'::regclass
  LOOP
    EXECUTE format('ALTER TABLE %s DROP CONSTRAINT %I', fk.relation, fk.conname);
    EXECUTE format('ALTER TABLE %s ADD CONSTRAINT %I %s', fk.relation, fk.conname,
      replace(fk.definition, 'REFERENCES employee_masters(', 'REFERENCES employee_numbers('));
  END LOOP;
END $migration$;

ALTER TABLE employee_masters ADD COLUMN effective_start_date DATE NOT NULL DEFAULT DATE '1900-01-01';
ALTER TABLE employee_masters ADD COLUMN effective_end_date DATE NOT NULL DEFAULT DATE '9999-12-31';
ALTER TABLE employee_masters DROP CONSTRAINT employee_masters_pkey;
ALTER TABLE employee_masters ADD PRIMARY KEY (employee_no, effective_start_date);
ALTER TABLE employee_masters ADD CONSTRAINT employee_master_valid_period
  CHECK (effective_start_date <= effective_end_date);

CREATE FUNCTION register_employee_number() RETURNS trigger LANGUAGE plpgsql AS $function$
BEGIN
  INSERT INTO employee_numbers(employee_no) VALUES (NEW.employee_no) ON CONFLICT DO NOTHING;
  RETURN NEW;
END $function$;
CREATE TRIGGER employee_master_register_number BEFORE INSERT ON employee_masters
FOR EACH ROW EXECUTE FUNCTION register_employee_number();
ALTER TABLE employee_masters ADD FOREIGN KEY (employee_no) REFERENCES employee_numbers(employee_no);

CREATE FUNCTION employee_masters_for_period(period_start DATE, period_end DATE)
RETURNS SETOF employee_masters LANGUAGE sql STABLE AS $function$
  SELECT DISTINCT ON (employee_no) * FROM employee_masters
  WHERE effective_start_date <= period_end AND effective_end_date >= period_start
  ORDER BY employee_no, effective_start_date DESC;
$function$;
CREATE FUNCTION employee_masters_for_month(payment_month TEXT)
RETURNS SETOF employee_masters LANGUAGE sql STABLE AS $function$
  SELECT * FROM employee_masters_for_period(
    to_date(replace(payment_month, '-', ''), 'YYYYMM'),
    (to_date(replace(payment_month, '-', ''), 'YYYYMM') + interval '1 month - 1 day')::date);
$function$;
CREATE VIEW current_employee_masters AS
  SELECT * FROM employee_masters_for_period((CURRENT_TIMESTAMP AT TIME ZONE 'Asia/Tokyo')::date,
                                          (CURRENT_TIMESTAMP AT TIME ZONE 'Asia/Tokyo')::date);
COMMIT;
