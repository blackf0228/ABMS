package com.aba.abms.attendance;

import java.math.BigDecimal;

public record AttendanceBulkRow(
    Integer contractNo,
    String engineerName,
    BigDecimal workingHours,
    String remarks) {
}
