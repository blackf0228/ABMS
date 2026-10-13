package com.aba.abms.contract;

import java.time.LocalDate;
import java.time.YearMonth;

final class OrderDocumentPolicy {

  private OrderDocumentPolicy() {
  }

  static LocalDate issueDate(YearMonth baseMonth) {
    return baseMonth.minusMonths(1).atEndOfMonth();
  }
}
