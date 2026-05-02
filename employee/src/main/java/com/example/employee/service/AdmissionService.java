package com.example.employee.service;

import com.example.employee.model.Applicant;
import com.example.employee.model.AttendanceLog;
import com.example.employee.model.LeaveRequest;
import com.example.employee.repository.ApplicantRepository;
import com.example.employee.repository.AttendanceRepository;
import com.example.employee.repository.LeaveRequestRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.mail.SimpleMailMessage;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;

import jakarta.servlet.http.HttpServletResponse;
import com.lowagie.text.*;
import com.lowagie.text.pdf.*;

import java.time.LocalDate;
import java.time.LocalTime;
import java.time.temporal.ChronoUnit;
import java.util.List;
import java.util.Map;
import java.util.Optional;

import com.example.employee.model.AppUser;
import com.example.employee.repository.AppUserRepository;
import org.springframework.security.crypto.password.PasswordEncoder;

@Service
public class AdmissionService {

    @Autowired
    private ApplicantRepository repository;
    
    @Autowired
    private AttendanceRepository attendanceRepository;

    @Autowired
    private LeaveRequestRepository leaveRequestRepository;

    @Autowired
    private JavaMailSender mailSender;
    
    @Autowired 
    private AppUserRepository appUserRepository;
    
    @Autowired 
    private PasswordEncoder passwordEncoder;

    // --- FILE TRACKING VARIABLES ---
    private String lastUploadedFileName = "No file uploaded yet";
    private String lastUploadTime = "--";

    public String getLastUploadedFileName() { return lastUploadedFileName; }
    public String getLastUploadTime() { return lastUploadTime; }
    
    private String autoAssignDepartment(String position) {
        if (position == null) return "Pending Assignment";
        switch (position) {
            case "College Professor":
            case "High School Teacher":
            case "Guidance Counselor":
            case "School Librarian":
                return "Academic / Faculty";
            case "Registrar Staff":
            case "School Nurse":
                return "Administration";
            case "IT Support Specialist":
                return "IT Services";
            case "Accounting Staff":
                return "Finance & Accounting";
            case "Security Officer":
            case "Maintenance Crew":
                return "Facilities & Maintenance";
            default:
                return "Pending Assignment";
        }
    }

    public void submitApplication(Applicant app) {
        app.setAdmissionType("System Admission");
        
        // Give everyone standard starting balances
        app.setVlBalance(15);
        app.setSlBalance(15);
        app.setBlBalance(3);  // 3 days Bereavement
        app.setSplBalance(7); // 7 days Solo Parent (HR can verify ID later)
        
        // Strict Gender-Based Leaves
        if ("Female".equalsIgnoreCase(app.getGender())) {
            app.setMlBalance(105);
            app.setPlBalance(0);
        } else {
            app.setMlBalance(0);
            app.setPlBalance(7);
        }
        
        repository.save(app); 
        sendEmail(app.getEmail(), "EAC HR: Application Received", 
            "Dear " + app.getFirstName() + ",\n\nWe received your application. Status: PENDING REVIEW.");
    }

    public void acceptApplicant(int id, String interviewDate, String interviewTime) {
        Optional<Applicant> result = repository.findById(id);
        if (result.isPresent()) {
            Applicant app = result.get();
            app.setStatus("ACCEPTED");
            repository.save(app); 
            String subject = "EAC HR: Application APPROVED - Interview Invitation";
            String body = "Dear " + app.getFirstName() + ",\n\nCongratulations! Your application has been APPROVED.\n\nDATE: " + interviewDate + "\nTIME: " + interviewTime + "\n\nEAC HR Team";
            sendEmail(app.getEmail(), subject, body);
        }
    }

    public void rejectApplicant(int id) {
        Optional<Applicant> result = repository.findById(id);
        if (result.isPresent()) {
            Applicant app = result.get();
            app.setStatus("REJECTED");
            repository.save(app);
        }
    }

    public void hireApplicant(int id, String contractDate, String contractTime) {
        Optional<Applicant> result = repository.findById(id);
        if (result.isPresent()) {
            Applicant app = result.get();
            
            app.setStatus("HIRED"); 
            String officialId = generateEacEmployeeId(app);
            app.setCustomEmployeeId(officialId);
            
            repository.save(app); 

            if (appUserRepository.findByUsername(officialId).isEmpty()) {
                AppUser newEmployeeAccount = new AppUser();
                newEmployeeAccount.setUsername(officialId);
                newEmployeeAccount.setPassword(passwordEncoder.encode("password"));
                newEmployeeAccount.setRole("EMPLOYEE");
                appUserRepository.save(newEmployeeAccount);
            }
            
            String subject = "Job Offer: " + app.getPositionApplied() + " at Emilio Aguinaldo College";
            String body = "Dear " + app.getFirstName() + ",\n\n" +
                          "We are pleased to inform you that you have passed the interview process. " +
                          "We would like to formally offer you the position of " + app.getPositionApplied() + ".\n\n" +
                          "Please report to the HR Department on " + contractDate + " at " + contractTime + " for contract signing.\n\n" +
                          "IMPORTANT: Your Employee Portal Login has been generated.\n" +
                          "Username: " + officialId + "\n" +
                          "Password: password\n\n" +
                          "Welcome to the Emilio Aguinaldo College family!\n" +
                          "Virtus, Excelentia, Servitium.";
            
            sendEmail(app.getEmail(), subject, body);
        }
    }

    public void addEmployeeManually(Applicant app) {
        app.setStatus("HIRED"); 
        app.setAdmissionType("F2F Admission");
        
        String officialId = generateEacEmployeeId(app);
        app.setCustomEmployeeId(officialId);
        
        repository.save(app); 

        if (appUserRepository.findByUsername(officialId).isEmpty()) {
            AppUser newEmployeeAccount = new AppUser();
            newEmployeeAccount.setUsername(officialId);
            newEmployeeAccount.setPassword(passwordEncoder.encode("password"));
            newEmployeeAccount.setRole("EMPLOYEE");
            appUserRepository.save(newEmployeeAccount);
        }
    }

    public List<Applicant> getEmployees(String department, String searchKeyword) {
        List<Applicant> employees = repository.findByStatusIn(List.of("HIRED", "RESIGNED"));
        
        if (department != null && !department.isEmpty()) {
            employees = employees.stream().filter(e -> e.getDepartment() != null && e.getDepartment().equals(department)).toList();
        }
        if (searchKeyword != null && !searchKeyword.isEmpty()) {
            String keyword = searchKeyword.toLowerCase();
            employees = employees.stream().filter(e -> 
                (e.getFirstName() != null && e.getFirstName().toLowerCase().contains(keyword)) || 
                (e.getLastName() != null && e.getLastName().toLowerCase().contains(keyword))
            ).toList();
        }

        LocalDate today = LocalDate.now();
        for (Applicant emp : employees) {
            Optional<AttendanceLog> log = attendanceRepository.findByEmployeeIdAndLogDate(emp.getId(), today);
            
            if (log.isEmpty()) {
                emp.setTodayStatus("Absent");
            } else if (log.get().getTimeOut() == null) {
                emp.setTodayStatus("Clocked In");
            } else {
                emp.setTodayStatus("Clocked Out");
            }
        }
        return employees;
    }

    public void updateEmployee(int id, String department, String positionApplied, String status, Double dailyWage, 
            int vlBalance, int slBalance, int mlBalance, int plBalance, int splBalance, int blBalance, 
            String employmentType, String paymentType, String expectedShift, Integer biometricId) {
        
        Optional<Applicant> opt = repository.findById(id);
        if (opt.isPresent()) {
            Applicant app = opt.get();
            app.setDepartment(department);
            app.setPositionApplied(positionApplied);
            app.setStatus(status); 
            app.setDailyWage(dailyWage); 
            
            app.setVlBalance(vlBalance);
            app.setSlBalance(slBalance);
            app.setMlBalance(mlBalance);
            app.setPlBalance(plBalance);
            app.setSplBalance(splBalance);
            app.setBlBalance(blBalance);

            app.setEmploymentType(employmentType);
            app.setPaymentType(paymentType);
            app.setExpectedShift(expectedShift);
            app.setBiometricId(biometricId);
            
            repository.save(app);
        }
    }

    public List<Applicant> getDashboardApplicants(String statusFilter) {
        if (statusFilter == null || statusFilter.isEmpty() || statusFilter.equals("ALL")) {
            return repository.findAllByOrderByIdDesc(); 
        } else {
            return repository.findByStatusOrderByIdDesc(statusFilter); 
        }
    }

    public String processBiometrics(int employeeId, String action) {
        Optional<Applicant> optEmp = repository.findById(employeeId);
        if (optEmp.isEmpty() || !"HIRED".equals(optEmp.get().getStatus())) {
            return "Error: Invalid or Inactive Employee ID.";
        }

        LocalDate today = LocalDate.now();
        LocalTime now = LocalTime.now();
        Optional<AttendanceLog> existingLog = attendanceRepository.findByEmployeeIdAndLogDate(employeeId, today);

        if (action.equals("TIME_IN")) {
            if (existingLog.isPresent() && existingLog.get().getTimeIn() != null) {
                return "Already Timed In today!";
            }
            AttendanceLog log = existingLog.orElse(new AttendanceLog());
            log.setEmployeeId(employeeId);
            log.setLogDate(today);
            log.setTimeIn(now);
            log.setStatus("PRESENT");
            attendanceRepository.save(log);
            return "Time In Successful at " + now.toString().substring(0, 5);
            
        } else if (action.equals("TIME_OUT")) {
            if (existingLog.isEmpty() || existingLog.get().getTimeIn() == null) {
                return "Error: Must Time In first!";
            }
            AttendanceLog log = existingLog.get();
            log.setTimeOut(now);
            attendanceRepository.save(log);
            return "Time Out Successful at " + now.toString().substring(0, 5);
        }
        return "Invalid Action.";
    }

    @Scheduled(cron = "0 59 23 * * ?")
    public void midnightAutoTimeout() {
        LocalDate today = LocalDate.now();
        List<AttendanceLog> missingTimeOuts = attendanceRepository.findByTimeOutIsNullAndLogDate(today);
        
        for (AttendanceLog log : missingTimeOuts) {
            log.setTimeOut(LocalTime.of(23, 59)); 
            log.setStatus("INVALID_SHIFT"); 
            attendanceRepository.save(log);
        }
        System.out.println("EAC SYSTEM: Midnight Auto-Timeout executed. Flagged " + missingTimeOuts.size() + " records.");
    }

    public String processLeaveRequest(LeaveRequest req) {
        Optional<Applicant> optEmp = repository.findById(req.getEmployeeId());
        if (optEmp.isEmpty() || !"HIRED".equals(optEmp.get().getStatus())) {
            return "Error: Invalid or Inactive Employee ID.";
        }
        Applicant emp = optEmp.get();
        
        if ("Maternity Leave".equals(req.getLeaveType()) && !"Female".equalsIgnoreCase(emp.getGender())) {
            return "Error: Maternity Leave is strictly for female employees.";
        }
        if ("Paternity Leave".equals(req.getLeaveType()) && !"Male".equalsIgnoreCase(emp.getGender())) {
            return "Error: Paternity Leave is strictly for male employees.";
        }
        
        req.setStatus("PENDING");
        leaveRequestRepository.save(req);
        return "Leave Request Submitted Successfully! Awaiting HR Approval.";
    }

    public List<LeaveRequest> getPendingLeaves() {
        return leaveRequestRepository.findByStatusOrderByIdDesc("PENDING");
    }

    public void approveLeave(int leaveId) {
        Optional<LeaveRequest> optReq = leaveRequestRepository.findById(leaveId);
        if (optReq.isPresent()) {
            LeaveRequest req = optReq.get();
            req.setStatus("APPROVED");
            leaveRequestRepository.save(req);

            Applicant emp = repository.findById(req.getEmployeeId()).orElse(null);
            if (emp != null) {
                int days = (int) ChronoUnit.DAYS.between(req.getStartDate(), req.getEndDate()) + 1;
                
                String type = req.getLeaveType();
                if ("Vacation Leave".equals(type) || "Terminal (VL)".equals(type)) emp.setVlBalance(emp.getVlBalance() - days);
                else if ("Sick Leave".equals(type)) emp.setSlBalance(emp.getSlBalance() - days);
                else if ("Maternity Leave".equals(type)) emp.setMlBalance(emp.getMlBalance() - days);
                else if ("Paternity Leave".equals(type)) emp.setPlBalance(emp.getPlBalance() - days);
                else if ("Solo Parent Leave".equals(type)) emp.setSplBalance(emp.getSplBalance() - days);
                else if ("Bereavement Leave".equals(type)) emp.setBlBalance(emp.getBlBalance() - days);
                
                repository.save(emp);
            }
        }
    }

    public void rejectLeave(int leaveId) {
        Optional<LeaveRequest> optReq = leaveRequestRepository.findById(leaveId);
        if (optReq.isPresent()) {
            LeaveRequest req = optReq.get();
            req.setStatus("REJECTED");
            leaveRequestRepository.save(req);
        }
    }

 // ==========================================
    // AUTOMATIC CUTOFF GENERATOR
    // ==========================================
    public java.util.Map<String, String> getRecentCutoffPeriods(int numberOfPeriods) {
        java.util.Map<String, String> periods = new java.util.LinkedHashMap<>();
        LocalDate current = LocalDate.now();
        int year = current.getYear();
        int month = current.getMonthValue();
        int half = current.getDayOfMonth() <= 15 ? 1 : 2;

        java.time.format.DateTimeFormatter formatter = java.time.format.DateTimeFormatter.ofPattern("MMM d");
        
        for (int i = 0; i < numberOfPeriods; i++) {
            String key = year + "-" + month + "-" + half; // e.g., "2026-3-2"
            
            LocalDate startDate;
            LocalDate endDate;
            if (half == 1) {
                startDate = LocalDate.of(year, month, 1);
                endDate = LocalDate.of(year, month, 15);
            } else {
                startDate = LocalDate.of(year, month, 16);
                endDate = LocalDate.of(year, month, java.time.YearMonth.of(year, month).lengthOfMonth());
            }
            
            String label = (i == 0 ? "Current: " : "Historical: ") + 
                           startDate.format(formatter) + " - " + 
                           endDate.format(java.time.format.DateTimeFormatter.ofPattern("MMM d, yyyy"));
            
            periods.put(key, label);
            
            // Move backwards in time by half a month
            if (half == 2) {
                half = 1;
            } else {
                half = 2;
                month--;
                if (month == 0) { month = 12; year--; }
            }
        }
        return periods;
    }

    public List<Applicant> getPayrollData(String cutoffCode) {
        List<Applicant> activeEmployees = repository.findByStatus("HIRED");
        LocalDate startDate;
        LocalDate endDate;

        // Automatically parse the Dynamic Cutoff Code (e.g., "2026-3-2")
        if (cutoffCode != null && !cutoffCode.isEmpty() && cutoffCode.contains("-")) {
            String[] parts = cutoffCode.split("-");
            int year = Integer.parseInt(parts[0]);
            int month = Integer.parseInt(parts[1]);
            int half = Integer.parseInt(parts[2]);
            
            if (half == 1) {
                startDate = LocalDate.of(year, month, 1);
                endDate = LocalDate.of(year, month, 15);
            } else {
                startDate = LocalDate.of(year, month, 16);
                endDate = LocalDate.of(year, month, java.time.YearMonth.of(year, month).lengthOfMonth());
            }
        } else {
            // Default to current date if no code is provided
            LocalDate today = LocalDate.now();
            startDate = today.getDayOfMonth() <= 15 ? today.withDayOfMonth(1) : today.withDayOfMonth(16);
            endDate = today.getDayOfMonth() <= 15 ? today.withDayOfMonth(15) : today.withDayOfMonth(today.lengthOfMonth());
        }
        
        for (Applicant emp : activeEmployees) {
            Map<String, Object> payrollResult = repository.computePayroll(emp.getId(), startDate, endDate);
            
            double totalHours = Double.parseDouble(payrollResult.get("p_total_hours").toString());
            double otHours = Double.parseDouble(payrollResult.get("p_ot_hours").toString());
            double grossPay = Double.parseDouble(payrollResult.get("p_net_pay").toString());
            
            double sss = grossPay > 0 ? grossPay * 0.045 : 0;        
            double philhealth = grossPay > 0 ? grossPay * 0.025 : 0; 
            double pagibig = grossPay > 0 ? 50.00 : 0;               
            
            double totalDeductions = sss + philhealth + pagibig;
            double finalNetPay = grossPay - totalDeductions;
            
            emp.setTotalHours(totalHours);
            emp.setOtHours(otHours);
            emp.setGrossPay(grossPay);
            emp.setSssDeduction(sss);
            emp.setPhilhealthDeduction(philhealth);
            emp.setPagibigDeduction(pagibig);
            emp.setNetPay(finalNetPay); 
            
            emp.setMonthlySalary((emp.getDailyWage() != null ? emp.getDailyWage() : 0.0) * 22.0);
        }
        return activeEmployees;
    }
    // ==========================================
    // PREMIUM PDF: PAYSLIP
    // ==========================================
    public void exportPayslipToPDF(int employeeId, HttpServletResponse response) {
        try {
        	Applicant emp = getPayrollData(null).stream().filter(a -> a.getId() == employeeId).findFirst().orElse(null);
            Document document = new Document(PageSize.A4);
            PdfWriter.getInstance(document, response.getOutputStream());
            document.open();

            // Brand Colors & Fonts
            java.awt.Color eacRed = new java.awt.Color(214, 0, 0);
            java.awt.Color eacDark = new java.awt.Color(45, 52, 54);
            Font titleFont = FontFactory.getFont(FontFactory.HELVETICA_BOLD, 24, eacRed);
            Font subFont = FontFactory.getFont(FontFactory.HELVETICA_BOLD, 12, eacDark);
            Font headerFont = FontFactory.getFont(FontFactory.HELVETICA_BOLD, 10, java.awt.Color.WHITE);
            Font normalFont = FontFactory.getFont(FontFactory.HELVETICA, 10, eacDark);
            Font boldFont = FontFactory.getFont(FontFactory.HELVETICA_BOLD, 10, eacDark);

            // Header Section
            Paragraph title = new Paragraph("EMILIO AGUINALDO COLLEGE", titleFont);
            title.setAlignment(Element.ALIGN_CENTER);
            document.add(title);
            
            Paragraph subTitle = new Paragraph("OFFICIAL EMPLOYEE PAYSLIP", subFont);
            subTitle.setAlignment(Element.ALIGN_CENTER);
            subTitle.setSpacingAfter(20f);
            document.add(subTitle);

            // Employee Info Table
            PdfPTable infoTable = new PdfPTable(4);
            infoTable.setWidthPercentage(100);
            infoTable.setSpacingAfter(20f);
            
            infoTable.addCell(createCell("Employee ID:", boldFont, false));
            infoTable.addCell(createCell(emp.getCustomEmployeeId(), normalFont, false));
            infoTable.addCell(createCell("Department:", boldFont, false));
            infoTable.addCell(createCell(emp.getDepartment(), normalFont, false));
            
            infoTable.addCell(createCell("Name:", boldFont, false));
            infoTable.addCell(createCell(emp.getFirstName() + " " + emp.getLastName(), normalFont, false));
            infoTable.addCell(createCell("Position:", boldFont, false));
            infoTable.addCell(createCell(emp.getPositionApplied(), normalFont, false));
            document.add(infoTable);

            // Financials Table
            PdfPTable finTable = new PdfPTable(2);
            finTable.setWidthPercentage(100);
            finTable.setWidths(new float[]{3f, 1f});

            // Earnings Header
            finTable.addCell(createHeaderCell("EARNINGS & HOURS", headerFont, eacDark));
            finTable.addCell(createHeaderCell("AMOUNT (PHP)", headerFont, eacDark));

            finTable.addCell(createCell("Base Wage Rate", normalFont, true));
            finTable.addCell(createRightCell(String.format("%.2f", emp.getDailyWage()), normalFont));
            finTable.addCell(createCell("Total Regular Hours Logged (" + String.format("%.2f", emp.getTotalHours()) + " hrs)", normalFont, true));
            finTable.addCell(createRightCell("-", normalFont)); // Computed in gross
            finTable.addCell(createCell("Overtime Hours Logged (" + String.format("%.2f", emp.getOtHours()) + " hrs)", normalFont, true));
            finTable.addCell(createRightCell("-", normalFont));

            // Gross Pay Subtotal
            finTable.addCell(createCell("GROSS EARNINGS", boldFont, true));
            finTable.addCell(createRightCell(String.format("%.2f", emp.getGrossPay()), boldFont));

            // Deductions Header
            finTable.addCell(createHeaderCell("MANDATORY DEDUCTIONS", headerFont, eacRed));
            finTable.addCell(createHeaderCell("AMOUNT (PHP)", headerFont, eacRed));

            finTable.addCell(createCell("SSS Contribution (4.5%)", normalFont, true));
            finTable.addCell(createRightCell("-" + String.format("%.2f", emp.getSssDeduction()), normalFont));
            finTable.addCell(createCell("PhilHealth Contribution (2.5%)", normalFont, true));
            finTable.addCell(createRightCell("-" + String.format("%.2f", emp.getPhilhealthDeduction()), normalFont));
            finTable.addCell(createCell("Pag-IBIG Fund", normalFont, true));
            finTable.addCell(createRightCell("-" + String.format("%.2f", emp.getPagibigDeduction()), normalFont));

            document.add(finTable);

            // Final Net Pay
            Paragraph netPay = new Paragraph("NET TAKE-HOME PAY: PHP " + String.format("%.2f", emp.getNetPay()), FontFactory.getFont(FontFactory.HELVETICA_BOLD, 16, new java.awt.Color(0, 150, 0)));
            netPay.setAlignment(Element.ALIGN_RIGHT);
            netPay.setSpacingBefore(15f);
            document.add(netPay);

            document.close();
        } catch (Exception e) { System.err.println("PDF Error: " + e.getMessage()); }
    }

    public List<AttendanceLog> getEmployeeAttendanceHistory(int employeeId) {
        List<AttendanceLog> logs = attendanceRepository.findByEmployeeIdOrderByLogDateDesc(employeeId);
        Applicant emp = repository.findById(employeeId).orElse(null);
        double hourlyRate = (emp != null && emp.getDailyWage() != null) ? emp.getDailyWage() / 8.0 : 0.0;

        for (AttendanceLog log : logs) {
            if (log.getTimeIn() != null && log.getTimeOut() != null) {
                long minutes = java.time.Duration.between(log.getTimeIn(), log.getTimeOut()).toMinutes();
                double hours = minutes / 60.0;
                log.setHoursWorked(hours);
                log.setEarnedToday(hours * hourlyRate);
            } else {
                log.setHoursWorked(0.0);
                log.setEarnedToday(0.0);
            }
        }
        return logs;
    }

    private void sendEmail(String to, String subject, String text) {
        try {
            SimpleMailMessage message = new SimpleMailMessage();
            message.setTo(to);
            message.setSubject(subject);
            message.setText(text);
            mailSender.send(message);
        } catch (Exception e) {
            System.err.println("Email Error: " + e.getMessage());
        }
    }

    // ==========================================
    // PREMIUM PDF: DAILY TIME RECORD (DTR)
    // ==========================================
 // ==========================================
    // HELPER: FILTER INDIVIDUAL LOGS BY CUTOFF
    // ==========================================
    public List<AttendanceLog> getEmployeeAttendanceHistoryByCutoff(int employeeId, String cutoffCode) {
        List<AttendanceLog> allLogs = getEmployeeAttendanceHistory(employeeId);
        LocalDate startDate;
        LocalDate endDate;

        if (cutoffCode != null && !cutoffCode.isEmpty() && cutoffCode.contains("-")) {
            String[] parts = cutoffCode.split("-");
            int year = Integer.parseInt(parts[0]);
            int month = Integer.parseInt(parts[1]);
            int half = Integer.parseInt(parts[2]);
            if (half == 1) {
                startDate = LocalDate.of(year, month, 1);
                endDate = LocalDate.of(year, month, 15);
            } else {
                startDate = LocalDate.of(year, month, 16);
                endDate = LocalDate.of(year, month, java.time.YearMonth.of(year, month).lengthOfMonth());
            }
        } else {
            LocalDate today = LocalDate.now();
            startDate = today.getDayOfMonth() <= 15 ? today.withDayOfMonth(1) : today.withDayOfMonth(16);
            endDate = today.getDayOfMonth() <= 15 ? today.withDayOfMonth(15) : today.withDayOfMonth(today.lengthOfMonth());
        }

        return allLogs.stream()
            .filter(log -> !log.getLogDate().isBefore(startDate) && !log.getLogDate().isAfter(endDate))
            .toList();
    }

    // ==========================================
    // PREMIUM PDF: DAILY TIME RECORD (DTR)
    // ==========================================
    public void exportDTRToPDF(int employeeId, String cutoffCode, HttpServletResponse response) {
        try {
            Applicant emp = repository.findById(employeeId).orElse(null);
            if (emp == null) return;

            // USE THE NEW TIME-TRAVEL HELPER METHOD
            List<AttendanceLog> cutoffLogs = getEmployeeAttendanceHistoryByCutoff(employeeId, cutoffCode);

            // Re-calculate start and end dates just for the PDF Header Text
            LocalDate startDate;
            LocalDate endDate;
            if (cutoffCode != null && !cutoffCode.isEmpty() && cutoffCode.contains("-")) {
                String[] parts = cutoffCode.split("-");
                int year = Integer.parseInt(parts[0]);
                int month = Integer.parseInt(parts[1]);
                int half = Integer.parseInt(parts[2]);
                if (half == 1) {
                    startDate = LocalDate.of(year, month, 1);
                    endDate = LocalDate.of(year, month, 15);
                } else {
                    startDate = LocalDate.of(year, month, 16);
                    endDate = LocalDate.of(year, month, java.time.YearMonth.of(year, month).lengthOfMonth());
                }
            } else {
                LocalDate today = LocalDate.now();
                startDate = today.getDayOfMonth() <= 15 ? today.withDayOfMonth(1) : today.withDayOfMonth(16);
                endDate = today.getDayOfMonth() <= 15 ? today.withDayOfMonth(15) : today.withDayOfMonth(today.lengthOfMonth());
            }

            Document document = new Document(PageSize.A4);
            PdfWriter.getInstance(document, response.getOutputStream());
            document.open();

            // Brand Colors & Fonts
            java.awt.Color eacRed = new java.awt.Color(214, 0, 0);
            java.awt.Color eacDark = new java.awt.Color(45, 52, 54);
            Font titleFont = FontFactory.getFont(FontFactory.HELVETICA_BOLD, 24, eacRed);
            Font subFont = FontFactory.getFont(FontFactory.HELVETICA_BOLD, 12, eacDark);
            Font headerFont = FontFactory.getFont(FontFactory.HELVETICA_BOLD, 10, java.awt.Color.WHITE);
            Font normalFont = FontFactory.getFont(FontFactory.HELVETICA, 9, eacDark);
            Font boldFont = FontFactory.getFont(FontFactory.HELVETICA_BOLD, 10, eacDark);

            Paragraph title = new Paragraph("EMILIO AGUINALDO COLLEGE", titleFont);
            title.setAlignment(Element.ALIGN_CENTER);
            document.add(title);
            
            Paragraph subTitle = new Paragraph("DAILY TIME RECORD (DTR)", subFont);
            subTitle.setAlignment(Element.ALIGN_CENTER);
            subTitle.setSpacingAfter(20f);
            document.add(subTitle);

            // Info Table
            PdfPTable infoTable = new PdfPTable(4);
            infoTable.setWidthPercentage(100);
            infoTable.setSpacingAfter(15f);
            infoTable.addCell(createCell("Employee ID:", boldFont, false));
            infoTable.addCell(createCell(emp.getCustomEmployeeId(), normalFont, false));
            infoTable.addCell(createCell("Cutoff Period:", boldFont, false));
            infoTable.addCell(createCell(startDate + " to " + endDate, normalFont, false));
            infoTable.addCell(createCell("Name:", boldFont, false));
            infoTable.addCell(createCell(emp.getFirstName() + " " + emp.getLastName(), normalFont, false));
            infoTable.addCell(createCell("Department:", boldFont, false));
            infoTable.addCell(createCell(emp.getDepartment(), normalFont, false));
            document.add(infoTable);

            // DTR Table
            PdfPTable table = new PdfPTable(6);
            table.setWidthPercentage(100);
            table.setWidths(new float[]{2f, 2f, 2f, 1.5f, 1.5f, 2f});

            String[] headers = {"Date", "Time In", "Time Out", "Reg. Hrs", "OT Hrs", "Status"};
            for (String header : headers) {
                table.addCell(createHeaderCell(header, headerFont, eacDark));
            }

            double totalReg = 0, totalOT = 0;

            for (AttendanceLog log : cutoffLogs) {
                table.addCell(createCenterCell(log.getLogDate().toString(), normalFont));
                table.addCell(createCenterCell(log.getTimeIn() != null ? log.getTimeIn().toString() : "--:--", normalFont));
                table.addCell(createCenterCell(log.getTimeOut() != null ? log.getTimeOut().toString() : "--:--", normalFont));

                double hours = log.getHoursWorked();
                double reg = Math.min(hours, 8.0);
                double ot = hours > 8.0 ? hours - 8.0 : 0.0;
                totalReg += reg; totalOT += ot;

                table.addCell(createCenterCell(String.format("%.2f", reg), normalFont));
                table.addCell(createCenterCell(String.format("%.2f", ot), normalFont));
                table.addCell(createCenterCell(log.getStatus(), normalFont));
            }
            document.add(table);

            // Totals
            Paragraph totals = new Paragraph("\nTotal Regular Hours: " + String.format("%.2f", totalReg) + " hrs\n" +
                                             "Total Overtime Hours: " + String.format("%.2f", totalOT) + " hrs", boldFont);
            totals.setAlignment(Element.ALIGN_RIGHT);
            totals.setSpacingAfter(40f);
            document.add(totals);

            // Signatures
            PdfPTable sigTable = new PdfPTable(2);
            sigTable.setWidthPercentage(100);
            sigTable.addCell(createCenterCell("__________________________\nEmployee Signature", normalFont));
            sigTable.addCell(createCenterCell("__________________________\nDean / Head Signature", normalFont));
            document.add(sigTable);

            document.close();
        } catch (Exception e) { System.err.println("PDF Error: " + e.getMessage()); }
    }

    // --- PDF HELPER METHODS ---
    private PdfPCell createCell(String text, Font font, boolean border) {
        PdfPCell cell = new PdfPCell(new Phrase(text, font));
        cell.setBorder(border ? Rectangle.BOTTOM : Rectangle.NO_BORDER);
        cell.setPadding(6f);
        cell.setBorderColor(new java.awt.Color(200, 200, 200));
        return cell;
    }
    private PdfPCell createRightCell(String text, Font font) {
        PdfPCell cell = createCell(text, font, true);
        cell.setHorizontalAlignment(Element.ALIGN_RIGHT);
        return cell;
    }
    private PdfPCell createCenterCell(String text, Font font) {
        PdfPCell cell = createCell(text, font, true);
        cell.setHorizontalAlignment(Element.ALIGN_CENTER);
        return cell;
    }
    private PdfPCell createHeaderCell(String text, Font font, java.awt.Color bgColor) {
        PdfPCell cell = new PdfPCell(new Phrase(text, font));
        cell.setBackgroundColor(bgColor);
        cell.setHorizontalAlignment(Element.ALIGN_CENTER);
        cell.setPadding(8f);
        return cell;
    }

    public void processBiometricsCsv(org.springframework.web.multipart.MultipartFile file) throws Exception {
    	// Save the file info for the dashboard
        this.lastUploadedFileName = file.getOriginalFilename();
        java.time.format.DateTimeFormatter formatter = java.time.format.DateTimeFormatter.ofPattern("MMM dd, yyyy - hh:mm a");
        this.lastUploadTime = java.time.LocalDateTime.now().format(formatter);
        
    	java.io.BufferedReader br = new java.io.BufferedReader(
            new java.io.InputStreamReader(file.getInputStream(), java.nio.charset.StandardCharsets.UTF_8));
        
        String line;
        boolean isFirstLine = true;
        
        java.time.format.DateTimeFormatter dateFormatter = java.time.format.DateTimeFormatter.ofPattern("M/d/yyyy");
        java.time.format.DateTimeFormatter timeFormatter = new java.time.format.DateTimeFormatterBuilder()
                .parseCaseInsensitive()
                .appendPattern("h:mm[:ss] a")
                .toFormatter(java.util.Locale.ENGLISH);

        while ((line = br.readLine()) != null) {
            if (isFirstLine) { isFirstLine = false; continue; }
            
            String[] data = line.split(",", -1); 
            if (data.length < 10) continue; 
            
            try {
                int tcmsUserId = Integer.parseInt(data[0].trim());
                String officialEmpIdStr = data[3].trim(); 
                
                Optional<Applicant> optEmp = repository.findByCustomEmployeeId(officialEmpIdStr);
                
                if (optEmp.isEmpty()) continue; 
                
                Applicant emp = optEmp.get();
                
                if (emp.getBiometricId() == null || emp.getBiometricId() != tcmsUserId) {
                    emp.setBiometricId(tcmsUserId);
                    repository.save(emp);
                }

                int internalDbId = emp.getId(); 
                
                String dateStr = data[4].trim(); 
                String timeInStr = data[5].trim();
                String timeOutStr = data[6].trim();
                String shortStr = data[7].trim();
                
                String leaveType = (data.length > 9) ? data[9].trim() : "";
                String workCode = (data.length > 10) ? data[10].trim() : "Normal";
                String dayType = (data.length > 11) ? data[11].trim() : "Workday";
                
                java.time.LocalDate logDate = java.time.LocalDate.parse(dateStr, dateFormatter);
                java.time.LocalTime timeIn = timeInStr.isEmpty() ? null : java.time.LocalTime.parse(timeInStr, timeFormatter);
                java.time.LocalTime timeOut = timeOutStr.isEmpty() ? null : java.time.LocalTime.parse(timeOutStr, timeFormatter);
                
                int undertime = 0;
                if (!shortStr.isEmpty()) {
                    double shortHours = Double.parseDouble(shortStr);
                    undertime = (int) (shortHours * 60.0);
                }
                
                Optional<AttendanceLog> existingLogOpt = attendanceRepository.findByEmployeeIdAndLogDate(internalDbId, logDate);
                AttendanceLog log;
                
                if (existingLogOpt.isPresent()) {
                    log = existingLogOpt.get(); 
                } else {
                    log = new AttendanceLog();  
                }

                log.setEmployeeId(internalDbId); 
                log.setLogDate(logDate);
                log.setTimeIn(timeIn);
                log.setTimeOut(timeOut);
                log.setUndertimeMinutes(undertime);
                log.setLeaveType(leaveType.isEmpty() ? null : leaveType);
                log.setWorkCode(workCode.isEmpty() ? "Normal" : workCode);
                log.setDayType(dayType.isEmpty() ? "Workday" : dayType);
                
                if (!leaveType.isEmpty()) {
                    log.setStatus(leaveType); 
                } else if (timeIn != null && timeOut != null) {
                    log.setStatus("PRESENT");
                } else if (timeIn != null && timeOut == null) {
                    log.setStatus("ACTIVE");
                } else {
                    log.setStatus("ABSENT");
                }
                
                attendanceRepository.save(log);
                
            } catch (Exception e) {
                System.err.println("Skipped a broken row in CSV: " + line + " -> " + e.getMessage());
            }
        }
        br.close();
    }

    public String generateEacEmployeeId(Applicant emp) {
        String prefixA = (emp.getEmploymentType() != null && emp.getEmploymentType().contains("Part-Time")) ? "2" : "1";
        String prefixB = (emp.getEmploymentType() != null && emp.getEmploymentType().contains("Non-Faculty")) ? "1" : "2";
        
        long sequenceCount = repository.findAll().stream()
            .filter(a -> "HIRED".equals(a.getStatus()))
            .filter(a -> a.getEmploymentType() != null && a.getEmploymentType().equals(emp.getEmploymentType()))
            .filter(a -> a.getCustomEmployeeId() != null && !a.getCustomEmployeeId().equals("PENDING"))
            .count();
            
        String sequence = String.format("%03d", sequenceCount + 1);
        return prefixA + prefixB + sequence;
    }

    public void deleteEmployee(int id) {
        java.util.Optional<com.example.employee.model.Applicant> opt = repository.findById(id);
        if (opt.isPresent()) {
            com.example.employee.model.Applicant app = opt.get();
            
            appUserRepository.findByUsername(app.getCustomEmployeeId()).ifPresent(user -> {
                appUserRepository.delete(user);
            });
            
            repository.deleteById(id);
        }
    }
}