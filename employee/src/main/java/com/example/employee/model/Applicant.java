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
    
    // NEW: Your specific 5-digit EAC formula ID
    private String customEmployeeId = "PENDING";
    @NotBlank(message = "First name is required")
    private String firstName;
    
    @NotBlank(message = "Last name is required")
    private String lastName;
    
    @NotBlank(message = "Email is required")
    @Email(message = "Must be a valid email format")
    private String email;
    
    @NotBlank(message = "Phone number is required")
    private String phone;
    
    @NotBlank(message = "Please select a gender")
    private String gender; 
    
    @NotBlank(message = "Please select a position")
    private String positionApplied;
    
    private String department = "Pending Assignment";
    private String admissionType = "System Admission";
    
    // ==========================================
    // NEW: EAC EMPLOYMENT CATEGORIES
    // ==========================================
    @NotBlank(message = "Employment type is required")
    private String employmentType = "Full-Time Non-Faculty"; 
    
    private String paymentType = "Hourly Rate"; 
    private String expectedShift = "08:00 AM - 05:00 PM"; 
    // ==========================================

    private Double dailyWage = 0.0; 
    private int vlBalance = 15;
    private int slBalance = 15;
    private int mlBalance = 0;

    private String emergencyContactName = "N/A";
    private String emergencyContactPhone = "N/A";
    private String highestDegree = "N/A";
    
    private String status = "PENDING"; 
    
    @Min(value = 0, message = "Experience cannot be negative")
    private int yearsExperience;
    
    @NotBlank(message = "Previous employer is required (Type N/A if none)")
    private String previousEmployer;
    
    @NotBlank(message = "Resume link is required")
    private String resumeLink;

    private String sssNumber;
    private String tinNumber;
    private String philhealthNumber;
    private String pagibigNumber;

    // --- PAYROLL & TIME VARIABLES ---
    @Transient private double totalHours;
    @Transient private double otHours;
    
    @Transient private double grossPay;             // Money BEFORE deductions
    @Transient private double sssDeduction;         // 4.5%
    @Transient private double philhealthDeduction;  // 2.5%
    @Transient private double pagibigDeduction;     // Flat P50 per cutoff
    @Transient private double netPay;               // Money AFTER deductions
    
    @Transient private double monthlySalary;
    @Transient private String todayStatus;

    // --- STANDARD GETTERS AND SETTERS ---
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
    
    // ==========================================
    // HOURS & MINUTES FORMATTERS
    // ==========================================
    
 // ==========================================
    // HOURS & MINUTES FORMATTERS
    // ==========================================
    @Transient
    public String getRegHoursText() {
        // Primitive doubles are never null, they default to 0.0!
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
    
 // NEW SPECIFIC LEAVE BALANCES
    private int plBalance = 0;  // Paternity Leave (7 Days)
    private int splBalance = 0; // Solo Parent Leave (7 Days)
    private int blBalance = 0;  // Bereavement Leave (3 Days)

    // Add their Getters and Setters:
    public int getPlBalance() { return plBalance; }
    public void setPlBalance(int plBalance) { this.plBalance = plBalance; }

    public int getSplBalance() { return splBalance; }
    public void setSplBalance(int splBalance) { this.splBalance = splBalance; }

    public int getBlBalance() { return blBalance; }
    public void setBlBalance(int blBalance) { this.blBalance = blBalance; }// ==========================================
    // HARDWARE INTEGRATION (TCMS V3)
    // ==========================================
    @jakarta.persistence.Column(name = "biometric_id")
    private Integer biometricId; // Will be empty (null) until HR enrolls their fingerprint

    public Integer getBiometricId() { return biometricId; }
    public void setBiometricId(Integer biometricId) { this.biometricId = biometricId; }
}