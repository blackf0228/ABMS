package com.aba.abms.payroll;

import java.util.List;

public record SalaryDetailSaveRequest(
    String paymentMonth,
    List<SalaryDetailDraftRow> rows
) {
}
