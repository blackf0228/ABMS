package com.aba.abms.contract;

import java.time.YearMonth;

public record DocumentCreationRequest(String targetMonth) {

  public YearMonth toYearMonth() {
    if (targetMonth == null || targetMonth.isBlank()) {
      throw new IllegalArgumentException("作成年月は必須です。");
    }
    return YearMonth.parse(targetMonth.trim());
  }
}
