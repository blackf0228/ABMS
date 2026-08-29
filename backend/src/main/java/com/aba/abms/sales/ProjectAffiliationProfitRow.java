package com.aba.abms.sales;

import java.math.BigDecimal;

public record ProjectAffiliationProfitRow(
    String employeeNo,
    String projectAffiliation,
    BigDecimal profitAmount,
    BigDecimal percentage) {
}
