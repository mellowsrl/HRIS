package com.example.employee.model;

import jakarta.persistence.*;
import java.time.LocalDate;
import java.time.LocalTime;

@Entity
@Table(name = "attendance_logs")
public class AttendanceLog {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private int id;

    @Column(name = "employee_id")
    private int employeeId;

    private LocalDate logDate;
    private LocalTime timeIn;
    private LocalTime timeOut;
    private String status;

    // ==========================================
    // PHASE 1: TCMS V3 CSV COLUMNS (Saved to Database)
    // ==========================================
    private int undertimeMinutes = 0;
    private String workCode = "Normal";
    private String dayType = "Workday"; // 'Workday' or 'Rest Day'
    private String leaveType;           // 'LWOP', 'EL', 'STL', etc.

    // ==========================================
    // TRANSIENT UI VARIABLES (Not saved to DB, used by AdmissionService math)
    // ==========================================
    @Transient
    private double hoursWorked;
    
    @Transient
    private double earnedToday;

    // ==========================================
    // ALL GETTERS AND SETTERS
    // ==========================================
    public int getId() { return id; }
    public void setId(int id) { this.id = id; }
    
    public int getEmployeeId() { return employeeId; }
    public void setEmployeeId(int employeeId) { this.employeeId = employeeId; }
    
    public LocalDate getLogDate() { return logDate; }
    public void setLogDate(LocalDate logDate) { this.logDate = logDate; }
    
    public LocalTime getTimeIn() { return timeIn; }
    public void setTimeIn(LocalTime timeIn) { this.timeIn = timeIn; }
    
    public LocalTime getTimeOut() { return timeOut; }
    public void setTimeOut(LocalTime timeOut) { this.timeOut = timeOut; }
    
    public String getStatus() { return status; }
    public void setStatus(String status) { this.status = status; }

    public int getUndertimeMinutes() { return undertimeMinutes; }
    public void setUndertimeMinutes(int undertimeMinutes) { this.undertimeMinutes = undertimeMinutes; }
    
    public String getWorkCode() { return workCode; }
    public void setWorkCode(String workCode) { this.workCode = workCode; }
    
    public String getDayType() { return dayType; }
    public void setDayType(String dayType) { this.dayType = dayType; }
    
    public String getLeaveType() { return leaveType; }
    public void setLeaveType(String leaveType) { this.leaveType = leaveType; }

    public double getHoursWorked() { return hoursWorked; }
    public void setHoursWorked(double hoursWorked) { this.hoursWorked = hoursWorked; }

    public double getEarnedToday() { return earnedToday; }
    public void setEarnedToday(double earnedToday) { this.earnedToday = earnedToday; }
    @Transient
    public String getFormattedHoursText() {
        int h = (int) this.hoursWorked;
        int m = (int) Math.round((this.hoursWorked - h) * 60);
        return h + " hrs " + m + " mins";
    }
}