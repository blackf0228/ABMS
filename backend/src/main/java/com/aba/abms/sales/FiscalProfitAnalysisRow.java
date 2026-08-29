package com.aba.abms.sales;

import java.math.BigDecimal;
import java.util.List;

public record FiscalProfitAnalysisRow(
    String itemName,
    List<BigDecimal> monthlyAmounts,
    BigDecimal totalAmount) {
}
