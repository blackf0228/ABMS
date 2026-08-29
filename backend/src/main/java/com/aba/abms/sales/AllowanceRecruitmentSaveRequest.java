package com.aba.abms.sales;

import java.math.BigDecimal;
import java.util.List;

public record AllowanceRecruitmentSaveRequest(List<Row> rows) {

  public record Row(
      String employeeNo,
      List<Amount> amounts) {
  }

  public record Amount(
      String paymentMonth,
      BigDecimal amount) {
  }
}
