package com.example.employee.model;

import jakarta.persistence.*;

import java.time.LocalDate;

@Entity
@Table(name = "employee") // Matches the other group's table
public class OfficialEmployee {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "employee_id")
    private Long id; 
    
    @Column(name = "custom_employee_id", unique = true)
    private String customEmployeeId;

    @Column(name = "first_name", nullable = false)
    private String firstName;

    @Column(name = "last_name", nullable = false)
    private String lastName;

    @Column(name = "email", unique = true)
    private String email;

    @Column(name = "status")
    private String status = "Active"; 

    @Column(name = "employee_status")
    private String employmentType; 

    @Column(name = "department_code")
    private String department; 

    // --- RESTORED YOUR ORIGINAL FIELDS ---
    @Column(name = "phone")
    private String phone;
    
    @Column(name = "gender")
    private String gender;
    
    @Column(name = "position")
    private String position;

    /** Service start / hire date (optional). */
    @Column(name = "date_hired")
    private LocalDate dateHired;

    /** End of probation period, if applicable. */
    @Column(name = "probation_end_date")
    private LocalDate probationEndDate;

    /** Contract or appointment end, if tracked. */
    @Column(name = "contract_end_date")
    private LocalDate contractEndDate;

    /** Last day of work / separation (offboarding), optional. */
    @Column(name = "separation_date")
    private LocalDate separationDate;

    @Column(name = "separation_note", length = 500)
    private String separationNote;
    
    @Column(name = "payment_type")
    private String paymentType;
    
    @Column(name = "expected_shift")
    private String expectedShift;
    
    @Column(name = "daily_wage")
    private Double dailyWage;

    /** Monthly basic salary (source for DOLE-style monthly rate; part-time may leave null and use hourly/daily). */
    @Column(name = "basic_salary")
    private Double basicSalary;
    
    @Column(name = "biometric_id")
    private Integer biometricId;

    // --- RESTORED LEAVE BALANCES ---
    @Column(name = "vl_balance")
    private int vlBalance;
    @Column(name = "sl_balance")
    private int slBalance;
    @Column(name = "ml_balance")
    private int mlBalance;
    @Column(name = "pl_balance")
    private int plBalance;  
    @Column(name = "spl_balance")
    private int splBalance; 
    @Column(name = "bl_balance")
    private int blBalance;  
    @Column(name = "incentive_leave_balance")
    private int incentiveLeaveBalance;
    @Column(name = "study_leave_balance")
    private int studyLeaveBalance;

    // --- RESTORED PERSONAL INFO ---
    @Column(name = "emergency_contact_name")
    private String emergencyContactName;
    @Column(name = "emergency_contact_phone")
    private String emergencyContactPhone;
    @Column(name = "highest_degree")
    private String highestDegree;
    @Column(name = "years_experience")
    private int yearsExperience;

    @Column(name = "experience_text", length = 500)
    private String experienceText;

    @Column(name = "previous_employer")
    private String previousEmployer;
    @Column(name = "resume_link")
    private String resumeLink;

    /** Served as /uploads/employee-photos/... */
    @Column(name = "profile_photo_path", length = 500)
    private String profilePhotoPath;

    @Column(name = "sss_number")
    private String sssNumber;
    @Column(name = "tin_number")
    private String tinNumber;
    @Column(name = "philhealth_number")
    private String philhealthNumber;
    @Column(name = "pagibig_number")
    private String pagibigNumber;

    // --- PAYROLL FIELDS (From their schema) ---
    @Column(name = "admin_pay")
    private Double adminPay;
    @Column(name = "hourly_rate")
    private Double hourlyRate;
    @Column(name = "honorarium")
    private Double honorarium;
    @Column(name = "longevity")
    private Double longevity;
    @Column(name = "de_minimis")
    private Double deMinimis;
    @Column(name = "allowance")
    private Double allowance;
    @Column(name = "cash_gift")
    private Double cashGift;
    @Column(name = "incentive")
    private Double incentive;
    @Column(name = "relocation_pay")
    private Double relocationPay;

    // --- TRANSIENT FIELDS (For your UI logic) ---
    @Transient private double totalHours;
    @Transient private double otHours;
    @Transient private double grossPay;              
    @Transient private double sssDeduction;          
    @Transient private double philhealthDeduction;   
    @Transient private double pagibigDeduction;      
    @Transient private double netPay;                
    @Transient private double monthlySalary;
    @Transient private String todayStatus;

    /** From payroll row: total_earnings (pre-deduction earnings per SP). */
    @Transient private double payrollTotalEarnings;
    @Transient private double payrollTaxableIncome;
    @Transient private double payrollWithholdingTax;
    @Transient private double payrollLoanDeductions;

    /**
     * User-facing explanation when payroll could not be loaded, the SP failed, or pay is zero
     * despite time recorded. Not persisted.
     */
    @Transient
    private String payrollWarning;

    public OfficialEmployee() {}

    // ==========================================
    // GETTERS AND SETTERS
    // ==========================================

    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }
    
    public String getCustomEmployeeId() { return customEmployeeId; }
    public void setCustomEmployeeId(String customEmployeeId) { this.customEmployeeId = customEmployeeId; }

    /** Campus / branch code from the first segment of the EAC id (e.g. {@code 1} from {@code 1-00001}). */
    public String getCampusCode() {
        if (customEmployeeId == null || customEmployeeId.isBlank()) {
            return "";
        }
        int i = customEmployeeId.indexOf('-');
        if (i <= 0) {
            return customEmployeeId.trim();
        }
        return customEmployeeId.substring(0, i).trim();
    }
    
    public String getFirstName() { return firstName; }
    public void setFirstName(String firstName) { this.firstName = firstName; }
    
    public String getLastName() { return lastName; }
    public void setLastName(String lastName) { this.lastName = lastName; }
    
    public String getEmail() { return email; }
    public void setEmail(String email) { this.email = email; }
    
    public String getStatus() { return status; }
    public void setStatus(String status) { this.status = status; }
    
    public String getEmploymentType() { return employmentType; }
    public void setEmploymentType(String employmentType) { this.employmentType = employmentType; }
    
    public String getDepartment() { return department; }
    public void setDepartment(String department) { this.department = department; }

    public String getPhone() { return phone; }
    public void setPhone(String phone) { this.phone = phone; }

    public String getGender() { return gender; }
    public void setGender(String gender) { this.gender = gender; }

    public String getPosition() { return position; }
    public void setPosition(String position) { this.position = position; }

    public LocalDate getDateHired() { return dateHired; }
    public void setDateHired(LocalDate dateHired) { this.dateHired = dateHired; }

    public LocalDate getProbationEndDate() { return probationEndDate; }
    public void setProbationEndDate(LocalDate probationEndDate) { this.probationEndDate = probationEndDate; }

    public LocalDate getContractEndDate() { return contractEndDate; }
    public void setContractEndDate(LocalDate contractEndDate) { this.contractEndDate = contractEndDate; }

    public LocalDate getSeparationDate() { return separationDate; }
    public void setSeparationDate(LocalDate separationDate) { this.separationDate = separationDate; }

    public String getSeparationNote() { return separationNote; }
    public void setSeparationNote(String separationNote) { this.separationNote = separationNote; }

    public String getPaymentType() { return paymentType; }
    public void setPaymentType(String paymentType) { this.paymentType = paymentType; }

    public String getExpectedShift() { return expectedShift; }
    public void setExpectedShift(String expectedShift) { this.expectedShift = expectedShift; }

    public Double getDailyWage() { return dailyWage; }
    public void setDailyWage(Double dailyWage) { this.dailyWage = dailyWage; }

    public Double getBasicSalary() { return basicSalary; }
    public void setBasicSalary(Double basicSalary) { this.basicSalary = basicSalary; }

    public Integer getBiometricId() { return biometricId; }
    public void setBiometricId(Integer biometricId) { this.biometricId = biometricId; }

    public int getVlBalance() { return vlBalance; }
    public void setVlBalance(int vlBalance) { this.vlBalance = vlBalance; }

    public int getSlBalance() { return slBalance; }
    public void setSlBalance(int slBalance) { this.slBalance = slBalance; }

    public int getMlBalance() { return mlBalance; }
    public void setMlBalance(int mlBalance) { this.mlBalance = mlBalance; }

    public int getPlBalance() { return plBalance; }
    public void setPlBalance(int plBalance) { this.plBalance = plBalance; }

    public int getSplBalance() { return splBalance; }
    public void setSplBalance(int splBalance) { this.splBalance = splBalance; }

    public int getBlBalance() { return blBalance; }
    public void setBlBalance(int blBalance) { this.blBalance = blBalance; }

    public int getIncentiveLeaveBalance() { return incentiveLeaveBalance; }
    public void setIncentiveLeaveBalance(int incentiveLeaveBalance) { this.incentiveLeaveBalance = incentiveLeaveBalance; }

    public int getStudyLeaveBalance() { return studyLeaveBalance; }
    public void setStudyLeaveBalance(int studyLeaveBalance) { this.studyLeaveBalance = studyLeaveBalance; }

    public String getEmergencyContactName() { return emergencyContactName; }
    public void setEmergencyContactName(String emergencyContactName) { this.emergencyContactName = emergencyContactName; }

    public String getEmergencyContactPhone() { return emergencyContactPhone; }
    public void setEmergencyContactPhone(String emergencyContactPhone) { this.emergencyContactPhone = emergencyContactPhone; }

    public String getHighestDegree() { return highestDegree; }
    public void setHighestDegree(String highestDegree) { this.highestDegree = highestDegree; }

    public int getYearsExperience() { return yearsExperience; }
    public void setYearsExperience(int yearsExperience) { this.yearsExperience = yearsExperience; }

    public String getExperienceText() { return experienceText; }
    public void setExperienceText(String experienceText) { this.experienceText = experienceText; }

    public String getPreviousEmployer() { return previousEmployer; }
    public void setPreviousEmployer(String previousEmployer) { this.previousEmployer = previousEmployer; }

    public String getResumeLink() { return resumeLink; }
    public void setResumeLink(String resumeLink) { this.resumeLink = resumeLink; }

    public String getProfilePhotoPath() { return profilePhotoPath; }
    public void setProfilePhotoPath(String profilePhotoPath) { this.profilePhotoPath = profilePhotoPath; }

    public String getSssNumber() { return sssNumber; }
    public void setSssNumber(String sssNumber) { this.sssNumber = sssNumber; }

    public String getTinNumber() { return tinNumber; }
    public void setTinNumber(String tinNumber) { this.tinNumber = tinNumber; }

    public String getPhilhealthNumber() { return philhealthNumber; }
    public void setPhilhealthNumber(String philhealthNumber) { this.philhealthNumber = philhealthNumber; }

    public String getPagibigNumber() { return pagibigNumber; }
    public void setPagibigNumber(String pagibigNumber) { this.pagibigNumber = pagibigNumber; }

    public Double getAdminPay() { return adminPay; }
    public void setAdminPay(Double adminPay) { this.adminPay = adminPay; }

    public Double getHourlyRate() { return hourlyRate; }
    public void setHourlyRate(Double hourlyRate) { this.hourlyRate = hourlyRate; }

    public Double getHonorarium() { return honorarium; }
    public void setHonorarium(Double honorarium) { this.honorarium = honorarium; }

    public Double getLongevity() { return longevity; }
    public void setLongevity(Double longevity) { this.longevity = longevity; }

    public Double getDeMinimis() { return deMinimis; }
    public void setDeMinimis(Double deMinimis) { this.deMinimis = deMinimis; }

    public Double getAllowance() { return allowance; }
    public void setAllowance(Double allowance) { this.allowance = allowance; }

    public Double getCashGift() { return cashGift; }
    public void setCashGift(Double cashGift) { this.cashGift = cashGift; }

    public Double getIncentive() { return incentive; }
    public void setIncentive(Double incentive) { this.incentive = incentive; }

    public Double getRelocationPay() { return relocationPay; }
    public void setRelocationPay(Double relocationPay) { this.relocationPay = relocationPay; }

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

    public double getPayrollTotalEarnings() { return payrollTotalEarnings; }
    public void setPayrollTotalEarnings(double payrollTotalEarnings) { this.payrollTotalEarnings = payrollTotalEarnings; }

    public double getPayrollTaxableIncome() { return payrollTaxableIncome; }
    public void setPayrollTaxableIncome(double payrollTaxableIncome) { this.payrollTaxableIncome = payrollTaxableIncome; }

    public double getPayrollWithholdingTax() { return payrollWithholdingTax; }
    public void setPayrollWithholdingTax(double payrollWithholdingTax) { this.payrollWithholdingTax = payrollWithholdingTax; }

    public double getPayrollLoanDeductions() { return payrollLoanDeductions; }
    public void setPayrollLoanDeductions(double payrollLoanDeductions) { this.payrollLoanDeductions = payrollLoanDeductions; }

    public String getPayrollWarning() { return payrollWarning; }
    public void setPayrollWarning(String payrollWarning) { this.payrollWarning = payrollWarning; }
    
    public String getTodayStatus() { return todayStatus; }
    public void setTodayStatus(String todayStatus) { this.todayStatus = todayStatus; }

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