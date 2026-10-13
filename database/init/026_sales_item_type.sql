ALTER TABLE sales_imports
  ADD COLUMN IF NOT EXISTS item_type VARCHAR(20) NOT NULL DEFAULT 'NORMAL';

UPDATE sales_imports
SET item_type = 'NORMAL'
WHERE item_type IS NULL OR btrim(item_type) = '';

ALTER TABLE sales_imports
  ALTER COLUMN item_type SET DEFAULT 'NORMAL',
  ALTER COLUMN item_type SET NOT NULL;
