package com.example.employee.model;

import jakarta.persistence.*;
import jakarta.validation.constraints.*;
import java.time.LocalDate;

@Entity
@Table(name = "employee_applicants")
@NamedStoredProcedureQuery(
    name = "Applicant.computePayroll",
    procedureName = "sp_compute_payroll",
    parameters = {
        @StoredProcedureParameter(mode = ParameterMode.IN, name = "p_emp_id", type = Integer.class),
        @StoredProcedureParameter(mode = ParameterMode.IN, name = "p_start_date", type = LocalDate.class),
        @StoredProcedureParameter(mode = ParameterMode.IN, name = "p_end_date", type = LocalDate.class),
        @StoredProcedureParameter(mode = ParameterMode.OUT, name = "p_total_hours", type = Double.class),
        @StoredProcedureParameter(mode = ParameterMode.OUT, name = "p_ot_hours", type = Double.class),
        @StoredProcedureParameter(mode = ParameterMode.OUT, name = "p_net_pay", type = Double.class)
    }
)
public class Applicant {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private int id; 
    
    // ==========================================
    // ATS RELATIONSHIPS (Fixed paths)
    // ==========================================
    @ManyToOne
    @JoinColumn(name = "user_id")
    private AppUser user; 

    @ManyToOne
    @JoinColumn(name = "job_posting_id")
    private JobPosting jobPosting;
    // ==========================================
    
    private String customEmployeeId = "PENDING";
    
    @NotBlank(message = "First name is required")
    @Size(max = 50, message = "First name must be at most 50 characters")
    private String firstName;
    
    @NotBlank(message = "Last name is required")
    @Size(max = 50, message = "Last name must be at most 50 characters")
    private String lastName;
    
    @NotBlank(message = "Email is required")
    @Email(message = "Must be a valid email format")
    @Size(max = 100, message = "Email must be at most 100 characters")
    private String email;
    
    @NotBlank(message = "Phone number is required")
    @Size(max = 30, message = "Phone must be at most 30 characters")
    private String phone;
    
    @NotBlank(message = "Please select a gender")
    private String gender; 
    
    @NotBlank(message = "Please select a position")
    @Size(max = 100, message = "Position must be at most 100 characters")
    private String positionApplied;
    
    /** Department code; must match a row in the department table. */
    @NotBlank(message = "Please select a department")
    @Size(max = 20, message = "Department code at most 20 characters")
    private String department;
    private String admissionType = "System Admission";
    
    @NotBlank(message = "Employment type is required")
    private String employmentType; 
    
    private String paymentType = "Hourly Rate"; 
    private String expectedShift = "08:00 AM - 05:00 PM"; 

    /** Proposed daily rate in PHP; HR / payroll use this when creating the employee record. */
    @NotNull(message = "Daily rate is required")
    @DecimalMin(value = "0.01", inclusive = true, message = "Enter a daily amount greater than zero (PHP)")
    private Double dailyWage;

    /** Optional; supplied during hiring if not collected at application. */
    @DecimalMin(value = "0.0", inclusive = true, message = "Honorarium cannot be negative")
    private Double honorarium;

    /** Optional. */
    @DecimalMin(value = "0.0", inclusive = true, message = "Longevity pay cannot be negative")
    private Double longevity; 
    
    private int vlBalance = 15;
    private int slBalance = 15;
    private int mlBalance = 0;
    private int plBalance = 0;  
    private int splBalance = 0; 
    private int blBalance = 0;  
    private int incentiveLeaveBalance = 5;
    private int studyLeaveBalance = 0;

    @NotBlank(message = "Emergency contact name is required")
    @Size(max = 100, message = "Emergency contact name too long")
    private String emergencyContactName;

    @NotBlank(message = "Emergency contact phone is required")
    @Size(max = 30, message = "Emergency phone at most 30 characters")
    private String emergencyContactPhone;

    @Size(max = 100, message = "Highest degree at most 100 characters")
    private String highestDegree;
    
    private String status = "PENDING"; 
    
    @Min(value = 0, message = "Experience cannot be negative")
    private int yearsExperience;

    /**
     * Free-text: e.g. "None", "6 months", "2 years", "3 years in retail".
     */
    @NotBlank(message = "Please describe your work experience, or enter \"None\" if you have none")
    @Size(max = 500, message = "Please keep this to 500 characters or less")
    @Column(name = "experience_text", length = 500)
    private String experienceText;
    
    @NotBlank(message = "Previous employer is required (Type N/A if none)")
    private String previousEmployer;
    
    /** Stored file path or URL (e.g. /uploads/applicant-resume/uuid.pdf); set when a PDF is uploaded. */
    @Size(max = 255, message = "Resume path is too long")
    private String resumeLink;

    @Size(max = 32, message = "SSS number at most 32 characters")
    private String sssNumber;

    @Size(max = 32, message = "TIN at most 32 characters")
    private String tinNumber;

    @Size(max = 32, message = "PhilHealth number at most 32 characters")
    private String philhealthNumber;

    @Size(max = 32, message = "Pag-IBIG number at most 32 characters")
    private String pagibigNumber;

    @Column(name = "biometric_id")
    private Integer biometricId; 

    @Transient private double totalHours;
    @Transient private double otHours;
    @Transient private double grossPay;             
    @Transient private double sssDeduction;         
    @Transient private double philhealthDeduction;  
    @Transient private double pagibigDeduction;     
    @Transient private double netPay;               
    @Transient private double monthlySalary;
    @Transient private String todayStatus;

    // --- GETTERS AND SETTERS ---
    public int getIncentiveLeaveBalance() { return incentiveLeaveBalance; }
    public void setIncentiveLeaveBalance(int incentiveLeaveBalance) { this.incentiveLeaveBalance = incentiveLeaveBalance; }
    public int getStudyLeaveBalance() { return studyLeaveBalance; }
    public void setStudyLeaveBalance(int studyLeaveBalance) { this.studyLeaveBalance = studyLeaveBalance; }
    public int getPlBalance() { return plBalance; }
    public void setPlBalance(int plBalance) { this.plBalance = plBalance; }
    public int getSplBalance() { return splBalance; }
    public void setSplBalance(int splBalance) { this.splBalance = splBalance; }
    public int getBlBalance() { return blBalance; }
    public void setBlBalance(int blBalance) { this.blBalance = blBalance; }
    public Integer getBiometricId() { return biometricId; }
    public void setBiometricId(Integer biometricId) { this.biometricId = biometricId; }
    public String getEmploymentType() { return employmentType; }
    public void setEmploymentType(String employmentType) { this.employmentType = employmentType; }
    public String getPaymentType() { return paymentType; }
    public void setPaymentType(String paymentType) { this.paymentType = paymentType; }
    public String getExpectedShift() { return expectedShift; }
    public void setExpectedShift(String expectedShift) { this.expectedShift = expectedShift; }
    public String getCustomEmployeeId() { return customEmployeeId; }
    public void setCustomEmployeeId(String customEmployeeId) { this.customEmployeeId = customEmployeeId; }
    public String getTodayStatus() { return todayStatus; }
    public void setTodayStatus(String todayStatus) { this.todayStatus = todayStatus; }
    public String getGender() { return gender; }
    public void setGender(String gender) { this.gender = gender; }
    public int getId() { return id; }
    public void setId(int id) { this.id = id; }
    public String getFirstName() { return firstName; }
    public void setFirstName(String firstName) { this.firstName = firstName; }
    public String getLastName() { return lastName; }
    public void setLastName(String lastName) { this.lastName = lastName; }
    public String getEmail() { return email; }
    public void setEmail(String email) { this.email = email; }
    public String getPhone() { return phone; }
    public void setPhone(String phone) { this.phone = phone; }
    public String getPositionApplied() { return positionApplied; }
    public void setPositionApplied(String positionApplied) { this.positionApplied = positionApplied; }
    public String getDepartment() { return department; }
    public void setDepartment(String department) { this.department = department; }
    public String getAdmissionType() { return admissionType; }
    public void setAdmissionType(String admissionType) { this.admissionType = admissionType; }
    public Double getDailyWage() { return dailyWage; }
    public void setDailyWage(Double dailyWage) { this.dailyWage = dailyWage; }

    public Double getHonorarium() { return honorarium; }
    public void setHonorarium(Double honorarium) { this.honorarium = honorarium; }

    public Double getLongevity() { return longevity; }
    public void setLongevity(Double longevity) { this.longevity = longevity; }
    public int getVlBalance() { return vlBalance; }
    public void setVlBalance(int vlBalance) { this.vlBalance = vlBalance; }
    public int getSlBalance() { return slBalance; }
    public void setSlBalance(int slBalance) { this.slBalance = slBalance; }
    public int getMlBalance() { return mlBalance; }
    public void setMlBalance(int mlBalance) { this.mlBalance = mlBalance; }
    public String getEmergencyContactName() { return emergencyContactName; }
    public void setEmergencyContactName(String emergencyContactName) { this.emergencyContactName = emergencyContactName; }
    public String getEmergencyContactPhone() { return emergencyContactPhone; }
    public void setEmergencyContactPhone(String emergencyContactPhone) { this.emergencyContactPhone = emergencyContactPhone; }
    public String getHighestDegree() { return highestDegree; }
    public void setHighestDegree(String highestDegree) { this.highestDegree = highestDegree; }
    public String getStatus() { return status; }
    public void setStatus(String status) { this.status = status; }
    public int getYearsExperience() { return yearsExperience; }
    public void setYearsExperience(int yearsExperience) { this.yearsExperience = yearsExperience; }
    public String getPreviousEmployer() { return previousEmployer; }
    public void setPreviousEmployer(String previousEmployer) { this.previousEmployer = previousEmployer; }
    public String getResumeLink() { return resumeLink; }
    public void setResumeLink(String resumeLink) { this.resumeLink = resumeLink; }

    public String getExperienceText() { return experienceText; }
    public void setExperienceText(String experienceText) { this.experienceText = experienceText; }
    public String getSssNumber() { return sssNumber; }
    public void setSssNumber(String sssNumber) { this.sssNumber = sssNumber; }
    public String getTinNumber() { return tinNumber; }
    public void setTinNumber(String tinNumber) { this.tinNumber = tinNumber; }
    public String getPhilhealthNumber() { return philhealthNumber; }
    public void setPhilhealthNumber(String philhealthNumber) { this.philhealthNumber = philhealthNumber; }
    public String getPagibigNumber() { return pagibigNumber; }
    public void setPagibigNumber(String pagibigNumber) { this.pagibigNumber = pagibigNumber; }
    
    // --- TRANSIENT GETTERS AND SETTERS ---
    public double getTotalHours() { return totalHours; }
    public void setTotalHours(double totalHours) { this.totalHours = totalHours; }
    public double getOtHours() { return otHours; }
    public void setOtHours(double otHours) { this.otHours = otHours; }
    public double getGrossPay() { return grossPay; }
    public void setGrossPay(double grossPay) { this.grossPay = grossPay; }
    public double getSssDeduction() { return sssDeduction; }
    public void setSssDeduction(double sssDeduction) { this.sssDeduction = sssDeduction; }
    public double getPhilhealthDeduction() { return philhealthDeduction; }
    public void setPhilhealthDeduction(double philhealthDeduction) { this.philhealthDeduction = philhealthDeduction; }
    public double getPagibigDeduction() { return pagibigDeduction; }
    public void setPagibigDeduction(double pagibigDeduction) { this.pagibigDeduction = pagibigDeduction; }
    public double getNetPay() { return netPay; }
    public void setNetPay(double netPay) { this.netPay = netPay; }
    public double getMonthlySalary() { return monthlySalary; }
    public void setMonthlySalary(double monthlySalary) { this.monthlySalary = monthlySalary; }

    // --- ATS GETTERS AND SETTERS ---
    public AppUser getUser() { return user; }
    public void setUser(AppUser user) { this.user = user; }
    public JobPosting getJobPosting() { return jobPosting; }
    public void setJobPosting(JobPosting jobPosting) { this.jobPosting = jobPosting; }
    
    // --- FORMATTERS ---
    @Transient
    public String getRegHoursText() {
        double raw = this.totalHours;
        double ot = this.otHours;
        double reg = raw - ot; 
        if (reg < 0) reg = 0;
        int h = (int) reg;
        int m = (int) Math.round((reg - h) * 60);
        return h + " hrs " + m + " mins";
    }

    @Transient
    public String getOtHoursText() {
        double ot = this.otHours;
        int h = (int) ot;
        int m = (int) Math.round((ot - h) * 60);
        return h + " hrs " + m + " mins";
    }

    @Transient
    public String getRawHoursText() {
        double raw = this.totalHours;
        int h = (int) raw;
        int m = (int) Math.round((raw - h) * 60);
        return h + " hrs " + m + " mins";
    }
}