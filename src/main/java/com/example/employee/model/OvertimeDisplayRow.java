package com.example.employee.model;

import java.time.LocalDate;

/**
 * One row in HR / employee overtime list (TCMS or EAC form).
 */
public record OvertimeDisplayRow(
    String rowKey,
    String sourceLabel,
    long employeeInternalId,
    String eacId,
    String employeeName,
    String department,
    String designation,
    String campus,
    String campusCode,
    LocalDate workDate,
    String startTime,
    String endTime,
    String overtimeHours,
    String offsetDate,
    String status,
    String otType,
    String nextApprover,
    String lastActionBy,
    String attachmentUrl,
    long tcmsAttendanceId,
    long eacRequestId
) {
    public boolean isTcms() { return tcmsAttendanceId > 0; }
    public boolean isEacForm() { return eacRequestId > 0; }
}
