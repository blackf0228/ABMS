package com.aba.abms.sales;

import java.math.BigDecimal;

public record SalesAnalysisItem(
    String billingDestinationCompanyId,
    String billingDestinationCompanyName,
    BigDecimal workingPersonCount,
    BigDecimal salesAmount
) {
}
