package com.aba.abms.offbook;

import java.math.BigDecimal;
import java.time.LocalDate;

public record OffBookExpenseItem(
    Integer expenseNo,
    LocalDate receivedDate,
    String handlerEmployeeNo,
    String handlerName,
    BigDecimal amount,
    String currency,
    String reason) {
}
