package com.aba.abms.payroll;

import java.math.BigDecimal;
import java.util.List;

public record ShareholderSalaryResponse(
    String employeeNo,
    String employeeName,
    String fiscalStartMonth,
    String fiscalEndMonth,
    List<ShareholderSalaryRow> rows,
    BigDecimal totalOverUnder) {
}
