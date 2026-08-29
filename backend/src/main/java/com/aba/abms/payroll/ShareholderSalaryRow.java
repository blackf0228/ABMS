package com.aba.abms.payroll;

import java.math.BigDecimal;

public record ShareholderSalaryRow(
    String paymentMonth,
    BigDecimal contractAmount,
    BigDecimal overtimeAmount,
    BigDecimal baseSalary,
    BigDecimal advanceExpense2,
    BigDecimal companyTotalAmount,
    BigDecimal overUnder,
    boolean companyTotalManual) {
}
