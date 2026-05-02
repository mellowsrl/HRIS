package com.example.employee.model;

import jakarta.persistence.*;

import org.springframework.format.annotation.DateTimeFormat;

import java.time.LocalDate;

/**
 * Maps to MySQL {@code teaching_pay} (see {@code payroll.sql}).
 * {@code total_teaching_pay} for a pay window is summed into {@code SP_ProcessRegularPayroll}.
 */
@Entity
@Table(name = "teaching_pay")
public class TeachingPay {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "employee_id", nullable = false)
    private Long employeeId;

    @Column(name = "period_start")
    @DateTimeFormat(iso = DateTimeFormat.ISO.DATE)
    private LocalDate periodStart;
    @Column(name = "period_end")
    @DateTimeFormat(iso = DateTimeFormat.ISO.DATE)
    private LocalDate periodEnd;

    @Column(name = "total_teaching_pay")
    private Double totalTeachingPay;
    @Column(name = "hourly_rate")
    private Double hourlyRate;
    @Column(name = "lec_pay")
    private Double lecPay;
    @Column(name = "lab_pay")
    private Double labPay;
    @Column(name = "lab_rate")
    private Double labRate;

    @Column(name = "excess_lec_hours")
    private Double excessLecHours;
    @Column(name = "excess_lab_hours")
    private Double excessLabHours;
    @Column(name = "excess_lec_units")
    private Double excessLecUnits;
    @Column(name = "excess_lab_units")
    private Double excessLabUnits;
    @Column(name = "total_lec_units")
    private Double totalLecUnits;
    @Column(name = "total_lab_units")
    private Double totalLabUnits;
    @Column(name = "total_hours")
    private Double totalHours;
    @Column(name = "total_lab_hours")
    private Double totalLabHours;
    @Column(name = "total_lec_hours")
    private Double totalLecHours;

    @Column(name = "holiday_pay")
    private Double holidayPay;
    @Column(name = "suspension_deduction")
    private Double suspensionDeduction;
    @Column(name = "total_excess_hours")
    private Double totalExcessHours;
    @Column(name = "adjustment_hours")
    private Double adjustmentHours;
    @Column(name = "adjustment_pay")
    private Double adjustmentPay;
    @Column(name = "admin_pay")
    private Double adminPay;
    @Column(name = "deduction_hours")
    private Double deductionHours;
    @Column(name = "honorarium")
    private Double honorarium;
    @Column(name = "rle_pay")
    private Double rlePay;
    @Column(name = "rle_rate")
    private Double rleRate;
    @Column(name = "sgd_hours")
    private Double sgdHours;
    @Column(name = "sgd_pay")
    private Double sgdPay;
    @Column(name = "substitute_hours")
    private Double substituteHours;
    @Column(name = "substitute_pay")
    private Double substitutePay;
    @Column(name = "supplemental_pay")
    private Double supplementalPay;
    @Column(name = "total_rle_hours")
    private Double totalRleHours;
    @Column(name = "tutorial_lab_hours")
    private Double tutorialLabHours;
    @Column(name = "tutorial_lab_pay")
    private Double tutorialLabPay;
    @Column(name = "tutorial_lec_hours")
    private Double tutorialLecHours;
    @Column(name = "tutorial_lec_pay")
    private Double tutorialLecPay;
    @Column(name = "workload_classification", length = 255)
    private String workloadClassification;
    @Column(name = "absent_deduction_hours")
    private Double absentDeductionHours;
    @Column(name = "excess_rle_hours")
    private Double excessRleHours;

    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }
    public Long getEmployeeId() { return employeeId; }
    public void setEmployeeId(Long employeeId) { this.employeeId = employeeId; }
    public LocalDate getPeriodStart() { return periodStart; }
    public void setPeriodStart(LocalDate periodStart) { this.periodStart = periodStart; }
    public LocalDate getPeriodEnd() { return periodEnd; }
    public void setPeriodEnd(LocalDate periodEnd) { this.periodEnd = periodEnd; }
    public Double getTotalTeachingPay() { return totalTeachingPay; }
    public void setTotalTeachingPay(Double totalTeachingPay) { this.totalTeachingPay = totalTeachingPay; }
    public Double getHourlyRate() { return hourlyRate; }
    public void setHourlyRate(Double hourlyRate) { this.hourlyRate = hourlyRate; }
    public Double getLecPay() { return lecPay; }
    public void setLecPay(Double lecPay) { this.lecPay = lecPay; }
    public Double getLabPay() { return labPay; }
    public void setLabPay(Double labPay) { this.labPay = labPay; }
    public Double getLabRate() { return labRate; }
    public void setLabRate(Double labRate) { this.labRate = labRate; }
    public Double getExcessLecHours() { return excessLecHours; }
    public void setExcessLecHours(Double excessLecHours) { this.excessLecHours = excessLecHours; }
    public Double getExcessLabHours() { return excessLabHours; }
    public void setExcessLabHours(Double excessLabHours) { this.excessLabHours = excessLabHours; }
    public Double getExcessLecUnits() { return excessLecUnits; }
    public void setExcessLecUnits(Double excessLecUnits) { this.excessLecUnits = excessLecUnits; }
    public Double getExcessLabUnits() { return excessLabUnits; }
    public void setExcessLabUnits(Double excessLabUnits) { this.excessLabUnits = excessLabUnits; }
    public Double getTotalLecUnits() { return totalLecUnits; }
    public void setTotalLecUnits(Double totalLecUnits) { this.totalLecUnits = totalLecUnits; }
    public Double getTotalLabUnits() { return totalLabUnits; }
    public void setTotalLabUnits(Double totalLabUnits) { this.totalLabUnits = totalLabUnits; }
    public Double getTotalHours() { return totalHours; }
    public void setTotalHours(Double totalHours) { this.totalHours = totalHours; }
    public Double getTotalLabHours() { return totalLabHours; }
    public void setTotalLabHours(Double totalLabHours) { this.totalLabHours = totalLabHours; }
    public Double getTotalLecHours() { return totalLecHours; }
    public void setTotalLecHours(Double totalLecHours) { this.totalLecHours = totalLecHours; }
    public Double getHolidayPay() { return holidayPay; }
    public void setHolidayPay(Double holidayPay) { this.holidayPay = holidayPay; }
    public Double getSuspensionDeduction() { return suspensionDeduction; }
    public void setSuspensionDeduction(Double suspensionDeduction) { this.suspensionDeduction = suspensionDeduction; }
    public Double getTotalExcessHours() { return totalExcessHours; }
    public void setTotalExcessHours(Double totalExcessHours) { this.totalExcessHours = totalExcessHours; }
    public Double getAdjustmentHours() { return adjustmentHours; }
    public void setAdjustmentHours(Double adjustmentHours) { this.adjustmentHours = adjustmentHours; }
    public Double getAdjustmentPay() { return adjustmentPay; }
    public void setAdjustmentPay(Double adjustmentPay) { this.adjustmentPay = adjustmentPay; }
    public Double getAdminPay() { return adminPay; }
    public void setAdminPay(Double adminPay) { this.adminPay = adminPay; }
    public Double getDeductionHours() { return deductionHours; }
    public void setDeductionHours(Double deductionHours) { this.deductionHours = deductionHours; }
    public Double getHonorarium() { return honorarium; }
    public void setHonorarium(Double honorarium) { this.honorarium = honorarium; }
    public Double getRlePay() { return rlePay; }
    public void setRlePay(Double rlePay) { this.rlePay = rlePay; }
    public Double getRleRate() { return rleRate; }
    public void setRleRate(Double rleRate) { this.rleRate = rleRate; }
    public Double getSgdHours() { return sgdHours; }
    public void setSgdHours(Double sgdHours) { this.sgdHours = sgdHours; }
    public Double getSgdPay() { return sgdPay; }
    public void setSgdPay(Double sgdPay) { this.sgdPay = sgdPay; }
    public Double getSubstituteHours() { return substituteHours; }
    public void setSubstituteHours(Double substituteHours) { this.substituteHours = substituteHours; }
    public Double getSubstitutePay() { return substitutePay; }
    public void setSubstitutePay(Double substitutePay) { this.substitutePay = substitutePay; }
    public Double getSupplementalPay() { return supplementalPay; }
    public void setSupplementalPay(Double supplementalPay) { this.supplementalPay = supplementalPay; }
    public Double getTotalRleHours() { return totalRleHours; }
    public void setTotalRleHours(Double totalRleHours) { this.totalRleHours = totalRleHours; }
    public Double getTutorialLabHours() { return tutorialLabHours; }
    public void setTutorialLabHours(Double tutorialLabHours) { this.tutorialLabHours = tutorialLabHours; }
    public Double getTutorialLabPay() { return tutorialLabPay; }
    public void setTutorialLabPay(Double tutorialLabPay) { this.tutorialLabPay = tutorialLabPay; }
    public Double getTutorialLecHours() { return tutorialLecHours; }
    public void setTutorialLecHours(Double tutorialLecHours) { this.tutorialLecHours = tutorialLecHours; }
    public Double getTutorialLecPay() { return tutorialLecPay; }
    public void setTutorialLecPay(Double tutorialLecPay) { this.tutorialLecPay = tutorialLecPay; }
    public String getWorkloadClassification() { return workloadClassification; }
    public void setWorkloadClassification(String workloadClassification) { this.workloadClassification = workloadClassification; }
    public Double getAbsentDeductionHours() { return absentDeductionHours; }
    public void setAbsentDeductionHours(Double absentDeductionHours) { this.absentDeductionHours = absentDeductionHours; }
    public Double getExcessRleHours() { return excessRleHours; }
    public void setExcessRleHours(Double excessRleHours) { this.excessRleHours = excessRleHours; }
}
