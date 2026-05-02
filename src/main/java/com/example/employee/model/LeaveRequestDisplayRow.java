package com.example.employee.model;

import java.time.LocalDate;

/**
 * One row in HR or employee leave list (with labels for name / status / approvers).
 */
public record LeaveRequestDisplayRow(
    int requestId,
    long employeeInternalId,
    String eacId,
    String employeeName,
    String department,
    String designation,
    String campusCode,
    String leaveType,
    LocalDate fromDate,
    LocalDate toDate,
    int numberOfDays,
    String status,
    String nextApprover,
    String lastActionBy
) {}
