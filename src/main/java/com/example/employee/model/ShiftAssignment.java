package com.example.employee.model;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.PrePersist;
import jakarta.persistence.PreUpdate;
import jakarta.persistence.Table;
import jakarta.persistence.UniqueConstraint;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;

@Entity
@Table(
    name = "shift_assignment",
    uniqueConstraints = @UniqueConstraint(name = "uk_shift_assignment_emp_date", columnNames = {"employee_id", "work_date"})
)
public class ShiftAssignment {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "employee_id", nullable = false)
    private OfficialEmployee employee;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "shift_id", nullable = false)
    private ShiftSchedule shift;

    @Column(name = "work_date", nullable = false)
    private LocalDate workDate;

    @Column(name = "source", length = 32)
    private String source = "manual";

    @Column(name = "notes", length = 300)
    private String notes;

    @Column(name = "override_time_in")
    private LocalTime overrideTimeIn;

    @Column(name = "override_time_out")
    private LocalTime overrideTimeOut;

    @Column(name = "override_break_minutes")
    private Integer overrideBreakMinutes;

    @Column(name = "override_grace_minutes")
    private Integer overrideGraceMinutes;

    @Column(name = "created_by", length = 80)
    private String createdBy;

    @Column(name = "updated_by", length = 80)
    private String updatedBy;

    @Column(name = "created_at", nullable = false)
    private LocalDateTime createdAt;

    @Column(name = "updated_at", nullable = false)
    private LocalDateTime updatedAt;

    @PrePersist
    public void prePersist() {
        LocalDateTime now = LocalDateTime.now();
        this.createdAt = now;
        this.updatedAt = now;
    }

    @PreUpdate
    public void preUpdate() {
        this.updatedAt = LocalDateTime.now();
    }

    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }
    public OfficialEmployee getEmployee() { return employee; }
    public void setEmployee(OfficialEmployee employee) { this.employee = employee; }
    public ShiftSchedule getShift() { return shift; }
    public void setShift(ShiftSchedule shift) { this.shift = shift; }
    public LocalDate getWorkDate() { return workDate; }
    public void setWorkDate(LocalDate workDate) { this.workDate = workDate; }
    public String getSource() { return source; }
    public void setSource(String source) { this.source = source; }
    public String getNotes() { return notes; }
    public void setNotes(String notes) { this.notes = notes; }
    public LocalTime getOverrideTimeIn() { return overrideTimeIn; }
    public void setOverrideTimeIn(LocalTime overrideTimeIn) { this.overrideTimeIn = overrideTimeIn; }
    public LocalTime getOverrideTimeOut() { return overrideTimeOut; }
    public void setOverrideTimeOut(LocalTime overrideTimeOut) { this.overrideTimeOut = overrideTimeOut; }
    public Integer getOverrideBreakMinutes() { return overrideBreakMinutes; }
    public void setOverrideBreakMinutes(Integer overrideBreakMinutes) { this.overrideBreakMinutes = overrideBreakMinutes; }
    public Integer getOverrideGraceMinutes() { return overrideGraceMinutes; }
    public void setOverrideGraceMinutes(Integer overrideGraceMinutes) { this.overrideGraceMinutes = overrideGraceMinutes; }
    public String getCreatedBy() { return createdBy; }
    public void setCreatedBy(String createdBy) { this.createdBy = createdBy; }
    public String getUpdatedBy() { return updatedBy; }
    public void setUpdatedBy(String updatedBy) { this.updatedBy = updatedBy; }
    public LocalDateTime getCreatedAt() { return createdAt; }
    public void setCreatedAt(LocalDateTime createdAt) { this.createdAt = createdAt; }
    public LocalDateTime getUpdatedAt() { return updatedAt; }
    public void setUpdatedAt(LocalDateTime updatedAt) { this.updatedAt = updatedAt; }
}

