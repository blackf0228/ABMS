package com.aba.abms.payroll;

import java.util.List;

public record SalaryConditionResponse(
    String paymentMonth,
    String previousPaymentMonth,
    boolean hasData,
    List<SalaryConditionRow> rows
) {
}
