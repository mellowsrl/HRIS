package com.example.employee.model;

import java.time.LocalDate;

/**
 * One row in HR "Pending overtime" screen (per attendance record).
 */
public class OvertimeApprovalListItem {

    private long attendanceId;
    private long employeeInternalId;
    private String customEmployeeId;
    private String employeeName;
    private LocalDate workDate;
    private int reportedOtHours;
    private String biometricsStatus;

    public OvertimeApprovalListItem() {}

    public OvertimeApprovalListItem(long attendanceId, long employeeInternalId, String customEmployeeId, String employeeName,
                                    LocalDate workDate, int reportedOtHours, String biometricsStatus) {
        this.attendanceId = attendanceId;
        this.employeeInternalId = employeeInternalId;
        this.customEmployeeId = customEmployeeId;
        this.employeeName = employeeName;
        this.workDate = workDate;
        this.reportedOtHours = reportedOtHours;
        this.biometricsStatus = biometricsStatus;
    }

    public long getAttendanceId() { return attendanceId; }
    public void setAttendanceId(long attendanceId) { this.attendanceId = attendanceId; }
    public long getEmployeeInternalId() { return employeeInternalId; }
    public void setEmployeeInternalId(long employeeInternalId) { this.employeeInternalId = employeeInternalId; }
    public long getEmployeeDbId() { return employeeInternalId; }
    public void setEmployeeDbId(long employeeInternalId) { this.employeeInternalId = employeeInternalId; }
    public String getCustomEmployeeId() { return customEmployeeId; }
    public void setCustomEmployeeId(String customEmployeeId) { this.customEmployeeId = customEmployeeId; }
    public String getEmployeeName() { return employeeName; }
    public void setEmployeeName(String employeeName) { this.employeeName = employeeName; }
    public LocalDate getWorkDate() { return workDate; }
    public void setWorkDate(LocalDate workDate) { this.workDate = workDate; }
    public int getReportedOtHours() { return reportedOtHours; }
    public void setReportedOtHours(int reportedOtHours) { this.reportedOtHours = reportedOtHours; }
    public String getBiometricsStatus() { return biometricsStatus; }
    public void setBiometricsStatus(String biometricsStatus) { this.biometricsStatus = biometricsStatus; }
}
