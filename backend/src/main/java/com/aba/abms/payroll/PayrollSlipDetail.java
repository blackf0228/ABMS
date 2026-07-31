package com.aba.abms.payroll;

import java.math.BigDecimal;

public record PayrollSlipDetail(
    String employeeNo,
    String employeeName,
    String employeeType,
    String paymentMonth,
    BigDecimal baseSalary,
    BigDecimal commutingAllowance,
    BigDecimal managementAllowance,
    BigDecimal transportationAdjustment,
    BigDecimal overtimePay,
    BigDecimal advanceExpense,
    BigDecimal advanceExpense2,
    BigDecimal otherAmount,
    String otherItemName,
    BigDecimal employmentInsuranceAmount,
    BigDecimal pensionInsuranceAmount,
    BigDecimal healthInsuranceAmount,
    BigDecimal incomeTax,
    BigDecimal childSupportAmount,
    BigDecimal residentTaxAmount,
    BigDecimal taxableIncome,
    BigDecimal transferPaymentAmount
) {
}
