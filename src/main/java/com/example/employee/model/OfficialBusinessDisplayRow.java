package com.example.employee.model;

import java.time.LocalDate;

/**
 * One row in HR / employee Official Business list.
 */
public record OfficialBusinessDisplayRow(
    String sourceLabel,
    long employeeInternalId,
    String eacId,
    String employeeName,
    String department,
    String designation,
    String campus,
    String campusCode,
    LocalDate businessDate,
    String startTime,
    String endTime,
    String obHours,
    String purpose,
    String status,
    String nextApprover,
    String lastActionBy,
    String attachmentUrl,
    long obRequestId
) {}
