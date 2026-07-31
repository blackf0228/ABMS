package com.aba.abms.sales;

import java.math.BigDecimal;

public record SalesDetailItem(
    String engineerName,
    BigDecimal billingAmount
) {
}
