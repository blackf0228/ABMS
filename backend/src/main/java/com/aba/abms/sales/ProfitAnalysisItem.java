package com.aba.abms.sales;

import java.math.BigDecimal;

public record ProfitAnalysisItem(
    String postingMonth,
    BigDecimal workingPersonCount,
    BigDecimal profitAmount
) {
}
