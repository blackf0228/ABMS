CREATE TABLE IF NOT EXISTS off_book_expenses (
  expense_no INTEGER PRIMARY KEY,
  received_date DATE NOT NULL,
  handler_employee_no VARCHAR(20) NOT NULL,
  amount NUMERIC(15, 2) NOT NULL,
  currency VARCHAR(20) NOT NULL,
  reason VARCHAR(2000) NOT NULL,
  CONSTRAINT fk_off_book_expenses_handler
    FOREIGN KEY (handler_employee_no) REFERENCES employee_masters(employee_no)
);

CREATE INDEX IF NOT EXISTS idx_off_book_expenses_received_date
  ON off_book_expenses(received_date);

CREATE INDEX IF NOT EXISTS idx_off_book_expenses_handler
  ON off_book_expenses(handler_employee_no);
