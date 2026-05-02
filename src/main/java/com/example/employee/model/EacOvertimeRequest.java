package com.example.employee.model;

import jakarta.persistence.*;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;

/**
 * Standalone overtime / offset requests (manual, employee, or clock actions).
 * TCMS-linked overtime remains on {@link AttendanceLog}; this table is for EAC form workflow.
 */
@Entity
@Table(name = "eac_overtime_request")
public class EacOvertimeRequest {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "employee_id", nullable = false)
    private Long employeeId;

    @Column(name = "work_date", nullable = false)
    private LocalDate workDate;

    @Column(name = "start_time")
    private LocalTime startTime;

    @Column(name = "end_time")
    private LocalTime endTime;

    /** Whole hours (aligned with {@link AttendanceLog} OT fields). */
    @Column(name = "overtime_hours")
    private Integer overtimeHours;

    @Column(name = "ot_type", length = 40)
    private String otType = "REGULAR";

    @Column(name = "offset_date")
    private LocalDate offsetDate;

    @Column(name = "attachment_path", length = 500)
    private String attachmentPath;

    @Column(name = "status", nullable = false, length = 20)
    private String status = "PENDING";

    @Column(name = "notes", length = 2000)
    private String notes;

    /** MANUAL, CLOCK_IN, CLOCK_OUT, EMPLOYEE */
    @Column(name = "request_source", length = 30)
    private String requestSource = "EMPLOYEE";

    @Column(name = "last_action_by", length = 120)
    private String lastActionBy;

    @Column(name = "created_at", nullable = false)
    private LocalDateTime createdAt = LocalDateTime.now();

    @Column(name = "decided_at")
    private LocalDateTime decidedAt;

    public EacOvertimeRequest() {}

    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }
    public Long getEmployeeId() { return employeeId; }
    public void setEmployeeId(Long employeeId) { this.employeeId = employeeId; }
    public LocalDate getWorkDate() { return workDate; }
    public void setWorkDate(LocalDate workDate) { this.workDate = workDate; }
    public LocalTime getStartTime() { return startTime; }
    public void setStartTime(LocalTime startTime) { this.startTime = startTime; }
    public LocalTime getEndTime() { return endTime; }
    public void setEndTime(LocalTime endTime) { this.endTime = endTime; }
    public Integer getOvertimeHours() { return overtimeHours; }
    public void setOvertimeHours(Integer overtimeHours) { this.overtimeHours = overtimeHours; }
    public String getOtType() { return otType; }
    public void setOtType(String otType) { this.otType = otType; }
    public LocalDate getOffsetDate() { return offsetDate; }
    public void setOffsetDate(LocalDate offsetDate) { this.offsetDate = offsetDate; }
    public String getAttachmentPath() { return attachmentPath; }
    public void setAttachmentPath(String attachmentPath) { this.attachmentPath = attachmentPath; }
    public String getStatus() { return status; }
    public void setStatus(String status) { this.status = status; }
    public String getNotes() { return notes; }
    public void setNotes(String notes) { this.notes = notes; }
    public String getRequestSource() { return requestSource; }
    public void setRequestSource(String requestSource) { this.requestSource = requestSource; }
    public String getLastActionBy() { return lastActionBy; }
    public void setLastActionBy(String lastActionBy) { this.lastActionBy = lastActionBy; }
    public LocalDateTime getCreatedAt() { return createdAt; }
    public void setCreatedAt(LocalDateTime createdAt) { this.createdAt = createdAt; }
    public LocalDateTime getDecidedAt() { return decidedAt; }
    public void setDecidedAt(LocalDateTime decidedAt) { this.decidedAt = decidedAt; }
}
