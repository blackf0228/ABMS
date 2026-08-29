package com.aba.abms.offbook;

import java.math.BigDecimal;
import java.time.LocalDate;

public record OffBookExpenseRequest(
    LocalDate receivedDate,
    String handlerEmployeeNo,
    BigDecimal amount,
    String currency,
    String reason) {
}
