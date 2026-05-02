package com.example.employee.model;

import jakarta.persistence.*;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;

/**
 * Official Business (out-of-office) requests: employee or HR-entered, HR-approved.
 */
@Entity
@Table(name = "eac_official_business_request")
public class EacOfficialBusinessRequest {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "employee_id", nullable = false)
    private Long employeeId;

    @Column(name = "business_date", nullable = false)
    private LocalDate businessDate;

    @Column(name = "start_time")
    private LocalTime startTime;

    @Column(name = "end_time")
    private LocalTime endTime;

    /** Whole hours (UI / payroll alignment). */
    @Column(name = "ob_hours")
    private Integer obHours;

    @Column(name = "purpose", length = 2000)
    private String purpose;

    @Column(name = "attachment_path", length = 500)
    private String attachmentPath;

    @Column(name = "status", nullable = false, length = 20)
    private String status = "PENDING";

    @Column(name = "next_approver", length = 120)
    private String nextApprover = "HR";

    @Column(name = "last_action_by", length = 120)
    private String lastActionBy;

    /** EMPLOYEE, MANUAL */
    @Column(name = "request_source", length = 30)
    private String requestSource = "EMPLOYEE";

    @Column(name = "notes", length = 2000)
    private String notes;

    @Column(name = "created_at", nullable = false)
    private LocalDateTime createdAt = LocalDateTime.now();

    @Column(name = "decided_at")
    private LocalDateTime decidedAt;

    public EacOfficialBusinessRequest() {}

    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }
    public Long getEmployeeId() { return employeeId; }
    public void setEmployeeId(Long employeeId) { this.employeeId = employeeId; }
    public LocalDate getBusinessDate() { return businessDate; }
    public void setBusinessDate(LocalDate businessDate) { this.businessDate = businessDate; }
    public LocalTime getStartTime() { return startTime; }
    public void setStartTime(LocalTime startTime) { this.startTime = startTime; }
    public LocalTime getEndTime() { return endTime; }
    public void setEndTime(LocalTime endTime) { this.endTime = endTime; }
    public Integer getObHours() { return obHours; }
    public void setObHours(Integer obHours) { this.obHours = obHours; }
    public String getPurpose() { return purpose; }
    public void setPurpose(String purpose) { this.purpose = purpose; }
    public String getAttachmentPath() { return attachmentPath; }
    public void setAttachmentPath(String attachmentPath) { this.attachmentPath = attachmentPath; }
    public String getStatus() { return status; }
    public void setStatus(String status) { this.status = status; }
    public String getNextApprover() { return nextApprover; }
    public void setNextApprover(String nextApprover) { this.nextApprover = nextApprover; }
    public String getLastActionBy() { return lastActionBy; }
    public void setLastActionBy(String lastActionBy) { this.lastActionBy = lastActionBy; }
    public String getRequestSource() { return requestSource; }
    public void setRequestSource(String requestSource) { this.requestSource = requestSource; }
    public String getNotes() { return notes; }
    public void setNotes(String notes) { this.notes = notes; }
    public LocalDateTime getCreatedAt() { return createdAt; }
    public void setCreatedAt(LocalDateTime createdAt) { this.createdAt = createdAt; }
    public LocalDateTime getDecidedAt() { return decidedAt; }
    public void setDecidedAt(LocalDateTime decidedAt) { this.decidedAt = decidedAt; }
}
