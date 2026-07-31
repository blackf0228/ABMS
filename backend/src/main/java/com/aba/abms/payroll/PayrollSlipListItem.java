package com.aba.abms.payroll;

import java.math.BigDecimal;

public record PayrollSlipListItem(
    String employeeNo,
    String employeeName,
    String paymentMonth,
    BigDecimal baseSalary,
    BigDecimal commutingAllowance,
    BigDecimal managementAllowance,
    BigDecimal transportationAdjustment,
    BigDecimal overtimePay,
    BigDecimal advanceExpense,
    BigDecimal advanceExpense2,
    BigDecimal otherAmount,
    BigDecimal transferPaymentAmount
) {
}
