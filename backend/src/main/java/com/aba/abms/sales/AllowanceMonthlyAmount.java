package com.aba.abms.sales;

import java.math.BigDecimal;

public record AllowanceMonthlyAmount(
    String month,
    BigDecimal amount
) {
}
