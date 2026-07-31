package com.aba.abms.payroll;

import java.math.BigDecimal;

public record SalaryDetailDraftRow(
    String employeeNo,
    String employeeName,
    String employeeType,
    String paymentMonth,
    BigDecimal baseSalary,
    BigDecimal remunerationAmount,
    BigDecimal supportTotalAmount,
    BigDecimal employmentInsuranceAmount,
    BigDecimal pensionInsuranceAmount,
    BigDecimal healthInsuranceAmount,
    BigDecimal residentTaxAmount,
    BigDecimal childSupportAmount,
    BigDecimal taxableIncome,
    BigDecimal incomeTax,
    BigDecimal deductionTotalAmount,
    BigDecimal transferPaymentAmount,
    BigDecimal otherAmount
) {
}
