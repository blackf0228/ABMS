package com.aba.abms.contract;

import static org.assertj.core.api.Assertions.assertThat;

import java.time.LocalDate;
import java.time.YearMonth;
import org.junit.jupiter.api.Test;

class OrderDocumentPolicyTest {

  @Test
  void usesTheLastDayOfThePreviousMonthAsTheIssueDate() {
    assertThat(OrderDocumentPolicy.issueDate(YearMonth.of(2026, 9)))
        .isEqualTo(LocalDate.of(2026, 8, 31));
    assertThat(OrderDocumentPolicy.issueDate(YearMonth.of(2026, 1)))
        .isEqualTo(LocalDate.of(2025, 12, 31));
  }
}
