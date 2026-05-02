package com.example.employee.model;

import jakarta.persistence.*;
import java.time.LocalDate;

@Entity
@Table(name = "official_employees")
@NamedStoredProcedureQuery(
    name = "OfficialEmployee.computePayroll",
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
public class OfficialEmployee {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private int id; 
    
    private String customEmployeeId;
    private String firstName;
    private String lastName;
    private String email;
    private String phone;
    private String gender; 
    private String position;
    private String department;
    private String employmentType; 
    private String paymentType; 
    private String expectedShift; 
    private Double dailyWage; 
    
    // Balances
    private int vlBalance;
    private int slBalance;
    private int mlBalance;
    private int plBalance;  
    private int splBalance; 
    private int blBalance;  
    private int incentiveLeaveBalance;
    private int studyLeaveBalance;

    private String emergencyContactName;
    private String emergencyContactPhone;
    private String highestDegree;
    private String status = "HIRED"; 
    private int yearsExperience;
    private String previousEmployer;
    private String resumeLink;

    private String sssNumber;
    private String tinNumber;
    private String philhealthNumber;
    private String pagibigNumber;

    @Column(name = "biometric_id")
    private Integer biometricId;

    // Transient Payroll Variables
    @Transient private double totalHours;
    @Transient private double otHours;
    @Transient private double grossPay;              
    @Transient private double sssDeduction;          
    @Transient private double philhealthDeduction;   
    @Transient private double pagibigDeduction;      
    @Transient private double netPay;                
    @Transient private double monthlySalary;
    @Transient private String todayStatus;

    // Default Constructor
    public OfficialEmployee() {}

    // GETTERS AND SETTERS (Standard)
    public int getId() { return id; }
    public void setId(int id) { this.id = id; }
    public String getCustomEmployeeId() { return customEmployeeId; }
    public void setCustomEmployeeId(String customEmployeeId) { this.customEmployeeId = customEmployeeId; }
    public String getFirstName() { return firstName; }
    public void setFirstName(String firstName) { this.firstName = firstName; }
    public String getLastName() { return lastName; }
    public void setLastName(String lastName) { this.lastName = lastName; }
    public String getEmail() { return email; }
    public void setEmail(String email) { this.email = email; }
    public String getPhone() { return phone; }
    public void setPhone(String phone) { this.phone = phone; }
    public String getGender() { return gender; }
    public void setGender(String gender) { this.gender = gender; }
    public String getPosition() { return position; }
    public void setPosition(String position) { this.position = position; }
    public String getDepartment() { return department; }
    public void setDepartment(String department) { this.department = department; }
    public String getEmploymentType() { return employmentType; }
    public void setEmploymentType(String employmentType) { this.employmentType = employmentType; }
    public String getPaymentType() { return paymentType; }
    public void setPaymentType(String paymentType) { this.paymentType = paymentType; }
    public String getExpectedShift() { return expectedShift; }
    public void setExpectedShift(String expectedShift) { this.expectedShift = expectedShift; }
    public Double getDailyWage() { return dailyWage; }
    public void setDailyWage(Double dailyWage) { this.dailyWage = dailyWage; }
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
    public Integer getBiometricId() { return biometricId; }
    public void setBiometricId(Integer biometricId) { this.biometricId = biometricId; }
    
    // Transient Getters/Setters
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