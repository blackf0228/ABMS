package com.aba.abms.attendance;

import java.math.BigDecimal;

public record AttendanceItem(
    Integer contractNo,
    String engineerName,
    String workingMonth,
    String engineerCompanyName,
    String customerName,
    BigDecimal workingHours,
    String remarks) {
}
