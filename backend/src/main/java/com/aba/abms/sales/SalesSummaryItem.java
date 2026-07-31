package com.aba.abms.sales;

import java.math.BigDecimal;

public record SalesSummaryItem(
    int no,
    String billingMonth,
    String billingDestinationCompanyId,
    String billingDestinationCompanyName,
    BigDecimal billingAmount
) {
}
