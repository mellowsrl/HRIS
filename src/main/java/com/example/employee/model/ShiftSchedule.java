package com.example.employee.model;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.PrePersist;
import jakarta.persistence.PreUpdate;
import jakarta.persistence.Table;

import java.time.LocalDateTime;
import java.time.LocalTime;

@Entity
@Table(name = "shift_schedule")
public class ShiftSchedule {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "name", nullable = false, length = 120)
    private String name;

    @Column(name = "branch_code", length = 40)
    private String branchCode = "ALL";

    @Column(name = "start_time")
    private LocalTime startTime;

    @Column(name = "end_time")
    private LocalTime endTime;

    @Column(name = "max_allowed_hours")
    private Double maxAllowedHours;

    @Column(name = "grace_minutes")
    private Integer graceMinutes;

    @Column(name = "break_minutes")
    private Integer breakMinutes;

    @Column(name = "allowed_minutes_before_clock_in")
    private Integer allowedMinutesBeforeClockIn;

    @Column(name = "is_flexible_hours")
    private boolean flexibleHours;

    @Column(name = "flexi_in_window_start")
    private LocalTime flexiInWindowStart;

    @Column(name = "flexi_in_window_end")
    private LocalTime flexiInWindowEnd;

    @Column(name = "required_net_work_minutes")
    private Integer requiredNetWorkMinutes;

    @Column(name = "allow_extra_hours")
    private boolean allowExtraHours;

    @Column(name = "require_ot_approval")
    private Boolean requireOtApproval;

    @Column(name = "missing_punch_policy", length = 32)
    private String missingPunchPolicy;

    // TCMS-compatible advanced rules
    @Column(name = "capture_first_last_as_attendance")
    private Boolean captureFirstLastAsAttendance;

    @Column(name = "max_in_out_pairs")
    private Integer maxInOutPairs;

    @Column(name = "round_work_minutes_to")
    private Integer roundWorkMinutesTo;

    @Column(name = "round_ot_minutes_to")
    private Integer roundOtMinutesTo;

    @Column(name = "round_short_minutes_to")
    private Integer roundShortMinutesTo;

    @Column(name = "exclude_break_from_work_hours")
    private Boolean excludeBreakFromWorkHours;

    @Column(name = "minimum_minutes_for_ot")
    private Integer minimumMinutesForOt;

    @Column(name = "max_ot_hours")
    private Double maxOtHours;

    @Column(name = "treat_restday_as_ot")
    private Boolean treatRestdayAsOt;

    @Column(name = "treat_offday_as_ot")
    private Boolean treatOffdayAsOt;

    @Column(name = "treat_holiday_as_ot")
    private Boolean treatHolidayAsOt;

    @Column(name = "notes", length = 500)
    private String notes;

    @Column(name = "is_active")
    private boolean active = true;

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
    public String getName() { return name; }
    public void setName(String name) { this.name = name; }
    public String getBranchCode() { return branchCode; }
    public void setBranchCode(String branchCode) { this.branchCode = branchCode; }
    public LocalTime getStartTime() { return startTime; }
    public void setStartTime(LocalTime startTime) { this.startTime = startTime; }
    public LocalTime getEndTime() { return endTime; }
    public void setEndTime(LocalTime endTime) { this.endTime = endTime; }
    public Double getMaxAllowedHours() { return maxAllowedHours; }
    public void setMaxAllowedHours(Double maxAllowedHours) { this.maxAllowedHours = maxAllowedHours; }
    public Integer getGraceMinutes() { return graceMinutes; }
    public void setGraceMinutes(Integer graceMinutes) { this.graceMinutes = graceMinutes; }
    public Integer getBreakMinutes() { return breakMinutes; }
    public void setBreakMinutes(Integer breakMinutes) { this.breakMinutes = breakMinutes; }
    public Integer getAllowedMinutesBeforeClockIn() { return allowedMinutesBeforeClockIn; }
    public void setAllowedMinutesBeforeClockIn(Integer allowedMinutesBeforeClockIn) { this.allowedMinutesBeforeClockIn = allowedMinutesBeforeClockIn; }
    public boolean isFlexibleHours() { return flexibleHours; }
    public void setFlexibleHours(boolean flexibleHours) { this.flexibleHours = flexibleHours; }
    public LocalTime getFlexiInWindowStart() { return flexiInWindowStart; }
    public void setFlexiInWindowStart(LocalTime flexiInWindowStart) { this.flexiInWindowStart = flexiInWindowStart; }
    public LocalTime getFlexiInWindowEnd() { return flexiInWindowEnd; }
    public void setFlexiInWindowEnd(LocalTime flexiInWindowEnd) { this.flexiInWindowEnd = flexiInWindowEnd; }
    public Integer getRequiredNetWorkMinutes() { return requiredNetWorkMinutes; }
    public void setRequiredNetWorkMinutes(Integer requiredNetWorkMinutes) { this.requiredNetWorkMinutes = requiredNetWorkMinutes; }
    public boolean isAllowExtraHours() { return allowExtraHours; }
    public void setAllowExtraHours(boolean allowExtraHours) { this.allowExtraHours = allowExtraHours; }
    public Boolean getRequireOtApproval() { return requireOtApproval; }
    public void setRequireOtApproval(Boolean requireOtApproval) { this.requireOtApproval = requireOtApproval; }
    public String getMissingPunchPolicy() { return missingPunchPolicy; }
    public void setMissingPunchPolicy(String missingPunchPolicy) { this.missingPunchPolicy = missingPunchPolicy; }
    public Boolean getCaptureFirstLastAsAttendance() { return captureFirstLastAsAttendance; }
    public void setCaptureFirstLastAsAttendance(Boolean captureFirstLastAsAttendance) { this.captureFirstLastAsAttendance = captureFirstLastAsAttendance; }
    public Integer getMaxInOutPairs() { return maxInOutPairs; }
    public void setMaxInOutPairs(Integer maxInOutPairs) { this.maxInOutPairs = maxInOutPairs; }
    public Integer getRoundWorkMinutesTo() { return roundWorkMinutesTo; }
    public void setRoundWorkMinutesTo(Integer roundWorkMinutesTo) { this.roundWorkMinutesTo = roundWorkMinutesTo; }
    public Integer getRoundOtMinutesTo() { return roundOtMinutesTo; }
    public void setRoundOtMinutesTo(Integer roundOtMinutesTo) { this.roundOtMinutesTo = roundOtMinutesTo; }
    public Integer getRoundShortMinutesTo() { return roundShortMinutesTo; }
    public void setRoundShortMinutesTo(Integer roundShortMinutesTo) { this.roundShortMinutesTo = roundShortMinutesTo; }
    public Boolean getExcludeBreakFromWorkHours() { return excludeBreakFromWorkHours; }
    public void setExcludeBreakFromWorkHours(Boolean excludeBreakFromWorkHours) { this.excludeBreakFromWorkHours = excludeBreakFromWorkHours; }
    public Integer getMinimumMinutesForOt() { return minimumMinutesForOt; }
    public void setMinimumMinutesForOt(Integer minimumMinutesForOt) { this.minimumMinutesForOt = minimumMinutesForOt; }
    public Double getMaxOtHours() { return maxOtHours; }
    public void setMaxOtHours(Double maxOtHours) { this.maxOtHours = maxOtHours; }
    public Boolean getTreatRestdayAsOt() { return treatRestdayAsOt; }
    public void setTreatRestdayAsOt(Boolean treatRestdayAsOt) { this.treatRestdayAsOt = treatRestdayAsOt; }
    public Boolean getTreatOffdayAsOt() { return treatOffdayAsOt; }
    public void setTreatOffdayAsOt(Boolean treatOffdayAsOt) { this.treatOffdayAsOt = treatOffdayAsOt; }
    public Boolean getTreatHolidayAsOt() { return treatHolidayAsOt; }
    public void setTreatHolidayAsOt(Boolean treatHolidayAsOt) { this.treatHolidayAsOt = treatHolidayAsOt; }
    public String getNotes() { return notes; }
    public void setNotes(String notes) { this.notes = notes; }
    public boolean isActive() { return active; }
    public void setActive(boolean active) { this.active = active; }
    public String getCreatedBy() { return createdBy; }
    public void setCreatedBy(String createdBy) { this.createdBy = createdBy; }
    public String getUpdatedBy() { return updatedBy; }
    public void setUpdatedBy(String updatedBy) { this.updatedBy = updatedBy; }
    public LocalDateTime getCreatedAt() { return createdAt; }
    public void setCreatedAt(LocalDateTime createdAt) { this.createdAt = createdAt; }
    public LocalDateTime getUpdatedAt() { return updatedAt; }
    public void setUpdatedAt(LocalDateTime updatedAt) { this.updatedAt = updatedAt; }
}

