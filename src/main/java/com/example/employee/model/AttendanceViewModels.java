package com.example.employee.model;

import java.time.LocalDate;

/**
 * Read-only DTOs for admin / self-service attendance UIs.
 */
public final class AttendanceViewModels {

    private AttendanceViewModels() {}

    public record AttendanceDayStats(int present, int late, int absent) {}

    public record AdminAttendanceLogRow(
        long logId,
        long employeeId,
        String eacId,
        String name,
        String department,
        String position,
        String campus,
        String campusCode,
        LocalDate workDate,
        String shift,
        String statusLabel,
        String timeIn,
        String timeOut,
        String breakInfo,
        String lateInfo,
        String workedHours,
        String location,
        String device
    ) {}

    public record EmployeeStatSummary(
        String totalHoursToday,
        String totalHoursWeek,
        String totalHoursMonth,
        String nightDiffMonth,
        String lateThisMonth,
        String undertimeThisMonth
    ) {}
}
