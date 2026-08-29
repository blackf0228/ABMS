package com.aba.abms.sales;

import java.math.BigDecimal;

public record AllowanceAnalysisDetail(
    String month,
    Integer contractNo,
    String engineerName,
    BigDecimal contractAmount,
    BigDecimal engineerUnitPrice,
    BigDecimal allowanceAmount
) {
}
