package com.aba.abms.sales;

import java.math.BigDecimal;

public record SalesMonthlyAnalysisItem(
    String billingMonth,
    BigDecimal workingPersonCount,
    BigDecimal salesAmount
) {
}
