package com.aba.abms.sales;

import java.math.BigDecimal;
import java.util.List;

public record AllowanceAnalysisItem(
    String employeeNo,
    String employeeName,
    String allowanceName,
    List<AllowanceMonthlyAmount> monthlyAmounts,
    BigDecimal subtotal,
    BigDecimal total
) {
}
