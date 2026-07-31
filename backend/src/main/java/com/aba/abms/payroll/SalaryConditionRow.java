package com.aba.abms.payroll;

import java.math.BigDecimal;

public record SalaryConditionRow(
    String employeeNo,
    String employeeName,
    String employeeType,
    String paymentMonth,
    BigDecimal baseSalary,
    BigDecimal consumptionTaxRate,
    BigDecimal commutingAllowance,
    BigDecimal managementAllowance,
    BigDecimal transportationAdjustment,
    BigDecimal overtimePay,
    BigDecimal advanceExpense,
    BigDecimal advanceExpense2,
    BigDecimal otherAmount,
    String otherItemName
) {
}
