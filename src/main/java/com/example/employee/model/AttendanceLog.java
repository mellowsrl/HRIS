package com.example.employee.model;

import jakarta.persistence.*;
import java.time.LocalDate;
import java.time.LocalTime;
import java.util.Locale;

/**
 * JPA map of the {@code attendance} table (see {@code employee/payroll.sql} and
 * {@code employee/sql/verify_attendance_alignment.sql}). All features (CSV import, DTR,
 * HR calendar, etc.) use this entity. The HTTP path {@code /api/attendance/logs} is only
 * a REST name; the physical table is {@code attendance}, not {@code attendance_logs}.
 */
@Entity
@Table(name = "attendance")
public class AttendanceLog {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "employee_id")
    private String employeeId; // They use a String/Varchar here!

    @Column(name = "date", nullable = false)
    private LocalDate date;

    @Column(name = "time_in")
    private LocalTime timeIn;

    @Column(name = "time_out")
    private LocalTime timeOut;

    @Column(name = "total_hours")
    private Integer totalHours;

    @Column(name = "minutes_late")
    private Integer minutesLate;

    @Column(name = "undertime_hours")
    private Integer undertimeHours;

    /** Payable / approved OT hours (counts toward DTR and payroll). */
    @Column(name = "overtime_hours")
    private Integer overtimeHours;

    /** OT computed from timekeeping / biometrics; awaiting HR approval when status is PENDING. */
    @Column(name = "overtime_reported")
    private Integer overtimeReported;

    /** NONE, PENDING, APPROVED, REJECTED */
    @Column(name = "ot_approval_status", length = 20)
    private String otApprovalStatus;

    @Column(name = "minutes_early_out")
    private Integer minutesEarlyOut;

    /** TCMS/CSV col 11: Workday, Restday, Holiday, etc. */
    @Column(name = "day_type", length = 64)
    private String dayType;

    /** TCMS/CSV col 9: VL, SL, leave type (None when not on leave). */
    @Column(name = "tcms_leave_type", length = 100)
    private String tcmsLeaveType;

    // ==========================================
    // GETTERS AND SETTERS
    // ==========================================
    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }

    public String getEmployeeId() { return employeeId; }
    public void setEmployeeId(String employeeId) { this.employeeId = employeeId; }

    public LocalDate getDate() { return date; }
    public void setDate(LocalDate date) { this.date = date; }

    public LocalTime getTimeIn() { return timeIn; }
    public void setTimeIn(LocalTime timeIn) { this.timeIn = timeIn; }

    public LocalTime getTimeOut() { return timeOut; }
    public void setTimeOut(LocalTime timeOut) { this.timeOut = timeOut; }

    public Integer getTotalHours() { return totalHours; }
    public void setTotalHours(Integer totalHours) { this.totalHours = totalHours; }

    public Integer getMinutesLate() { return minutesLate; }
    public void setMinutesLate(Integer minutesLate) { this.minutesLate = minutesLate; }

    public Integer getUndertimeHours() { return undertimeHours; }
    public void setUndertimeHours(Integer undertimeHours) { this.undertimeHours = undertimeHours; }

    public Integer getOvertimeHours() { return overtimeHours; }
    public void setOvertimeHours(Integer overtimeHours) { this.overtimeHours = overtimeHours; }

    public Integer getOvertimeReported() { return overtimeReported; }
    public void setOvertimeReported(Integer overtimeReported) { this.overtimeReported = overtimeReported; }

    public String getOtApprovalStatus() { return otApprovalStatus; }
    public void setOtApprovalStatus(String otApprovalStatus) { this.otApprovalStatus = otApprovalStatus; }

    public Integer getMinutesEarlyOut() { return minutesEarlyOut; }
    public void setMinutesEarlyOut(Integer minutesEarlyOut) { this.minutesEarlyOut = minutesEarlyOut; }

    public String getDayType() { return dayType; }
    public void setDayType(String dayType) { this.dayType = dayType; }

    public String getTcmsLeaveType() { return tcmsLeaveType; }
    public void setTcmsLeaveType(String tcmsLeaveType) { this.tcmsLeaveType = tcmsLeaveType; }

    /**
     * Short line for DTR/HR: leave type, holiday, or rest day from TCMS.
     * Omits generic workdays to avoid cluttering every row.
     */
    public String getCalendarDayStatusLine() {
        if (tcmsLeaveType != null) {
            String t = tcmsLeaveType.trim();
            if (!t.isEmpty() && !"None".equalsIgnoreCase(t) && !"-".equals(t)) {
                return "Leave (" + t + ")";
            }
        }
        if (dayType == null) {
            return null;
        }
        String d = dayType.trim();
        if (d.isEmpty()) {
            return null;
        }
        String lower = d.toLowerCase(Locale.ROOT);
        if (lower.contains("holiday")) {
            return "Holiday";
        }
        if (lower.contains("rest")) {
            return "Rest day";
        }
        if (lower.contains("work")) {
            return null;
        }
        return d;
    }

    /** Biometrics / grid: complete punch pair, open out, or no punches. */
    public String getPunchStatusLabel() {
        if (timeOut != null) {
            return "Complete";
        }
        if (timeIn != null) {
            return "In progress";
        }
        return "—";
    }
}