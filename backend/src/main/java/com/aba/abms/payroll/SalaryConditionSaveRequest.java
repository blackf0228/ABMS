package com.aba.abms.payroll;

import java.util.List;

public record SalaryConditionSaveRequest(
    String paymentMonth,
    List<SalaryConditionRow> rows
) {
}
