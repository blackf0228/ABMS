package com.aba.abms.payroll;

import java.math.BigDecimal;
import java.util.List;

public record ShareholderSalarySaveRequest(
    String employeeNo,
    List<Row> rows) {

  public record Row(
      String paymentMonth,
      BigDecimal contractAmount,
      BigDecimal overtimeAmount,
      BigDecimal baseSalary,
      BigDecimal advanceExpense2,
      BigDecimal companyTotalAmount,
      boolean companyTotalManual) {
  }
}
