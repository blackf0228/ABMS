package com.aba.abms.attendance;

import java.util.List;

public record AttendanceBulkSaveRequest(
    String workingMonth,
    List<AttendanceBulkRow> rows) {
}
