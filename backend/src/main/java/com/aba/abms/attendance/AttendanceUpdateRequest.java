package com.aba.abms.attendance;

import java.math.BigDecimal;

public record AttendanceUpdateRequest(
    String workingMonth,
    BigDecimal workingHours,
    String remarks) {
}
