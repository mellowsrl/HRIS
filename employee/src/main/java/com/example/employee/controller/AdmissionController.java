package com.example.employee.controller;

import com.example.employee.model.Applicant;
import com.example.employee.model.LeaveRequest;
import com.example.employee.repository.ApplicantRepository;
import com.example.employee.service.AdmissionService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.validation.BindingResult;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.ModelAttribute;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestParam;

import jakarta.servlet.http.HttpServletResponse;
import jakarta.validation.Valid;

@Controller
public class AdmissionController {
    
    @Autowired
    private com.example.employee.repository.LeaveRequestRepository leaveRequestRepository;
    
    @Autowired
    private AdmissionService service;
    
    @Autowired
    private ApplicantRepository repository;

    @GetMapping("/")
    public String showLandingPage() { return "index"; }

    @GetMapping("/apply")
    public String showForm(Model model) {
        model.addAttribute("applicant", new Applicant());
        return "admission-form";
    }

    @PostMapping("/submit")
    public String submitForm(@Valid @ModelAttribute Applicant applicant, BindingResult bindingResult, Model model) {
        if (bindingResult.hasErrors()) {
            return "admission-form"; 
        }
        service.submitApplication(applicant);
        return "success";
    }

    @GetMapping("/login")
    public String showLoginPage() { return "login"; }

    @GetMapping("/hr/dashboard")
    public String showHrDashboard(Model model, @RequestParam(required = false) String status) {
        model.addAttribute("applicants", service.getDashboardApplicants(status));
        model.addAttribute("selectedStatus", status); 
        return "hr-dashboard";
    }
    
    @GetMapping("/hr/employees")
    public String showEmployees(Model model, 
                                @RequestParam(required = false) String department,
                                @RequestParam(required = false) String search) {
        model.addAttribute("employees", service.getEmployees(department, search)); 
        model.addAttribute("selectedDept", department);
        model.addAttribute("searchKeyword", search);
        return "hr-employees";
    }

    @PostMapping("/hr/accept")
    public String acceptApplicant(@RequestParam int id, @RequestParam String date, @RequestParam String time) {
        service.acceptApplicant(id, date, time);
        return "redirect:/hr/dashboard";
    }

    @PostMapping("/hr/reject")
    public String rejectApplicant(@RequestParam int id) {
        service.rejectApplicant(id);
        return "redirect:/hr/dashboard";
    }
    
    @PostMapping("/hr/hire")
    public String hireApplicant(@RequestParam int id, @RequestParam String date, @RequestParam String time) {
        service.hireApplicant(id, date, time);
        return "redirect:/hr/dashboard";
    }

    @PostMapping("/hr/employees/add")
    public String addF2FEmployee(@ModelAttribute Applicant applicant) {
        service.addEmployeeManually(applicant);
        return "redirect:/hr/employees";
    }

    @PostMapping("/hr/employees/edit")
    public String editEmployee(@RequestParam int id, 
                               @RequestParam String department, 
                               @RequestParam String positionApplied, 
                               @RequestParam String status,
                               @RequestParam Double dailyWage,
                               @RequestParam int vlBalance,
                               @RequestParam int slBalance,
                               @RequestParam int mlBalance,
                               @RequestParam int plBalance,
                               @RequestParam int splBalance,
                               @RequestParam int blBalance,
                               @RequestParam String employmentType,
                               @RequestParam String paymentType,
                               @RequestParam String expectedShift,
                               @RequestParam(required = false) Integer biometricId) { 
                               
        service.updateEmployee(id, department, positionApplied, status, dailyWage, vlBalance, slBalance, mlBalance, plBalance, splBalance, blBalance, employmentType, paymentType, expectedShift, biometricId);
        return "redirect:/hr/employees";
    }

    @GetMapping("/kiosk")
    public String showKiosk() { return "kiosk"; }

    @PostMapping("/kiosk/tap")
    public String processKioskTap(@RequestParam int employeeId, @RequestParam String action, Model model) {
        String message = service.processBiometrics(employeeId, action);
        model.addAttribute("message", message);
        return "kiosk";
    }

    @GetMapping("/hr/payroll")
    public String showPayroll(Model model) {
        model.addAttribute("payrollList", service.getPayrollData(null));
        
        java.time.LocalDate today = java.time.LocalDate.now();
        String period = today.getDayOfMonth() <= 15 ? "1st - 15th" : "16th - EOM";
        model.addAttribute("cutoffPeriod", period + " " + today.getMonth().toString());
        
        return "hr-payroll";
    }

    @GetMapping("/hr/payroll/download")
    public void downloadPayslip(@RequestParam int id, HttpServletResponse response) {
        response.setContentType("application/pdf");
        response.setHeader("Content-Disposition", "attachment; filename=EAC_Payslip_EAC" + id + ".pdf");
        service.exportPayslipToPDF(id, response);
    }

    @GetMapping("/hr/records")
    public String showHrRecords(Model model) {
        model.addAttribute("applicants", repository.findAll());
        return "hr-records";
    }

    @PostMapping("/hr/records/update")
    public String updateEmployeeRecords(
            @RequestParam int id,
            @RequestParam(required = false) String sssNumber,
            @RequestParam(required = false) String tinNumber,
            @RequestParam(required = false) String philhealthNumber,
            @RequestParam(required = false) String pagibigNumber,
            @RequestParam(required = false) String highestDegree,
            @RequestParam(required = false) String emergencyContactName,
            @RequestParam(required = false) String emergencyContactPhone) {
        
        Applicant emp = repository.findById(id).orElse(null);
        if (emp != null) {
            emp.setSssNumber(sssNumber);
            emp.setTinNumber(tinNumber);
            emp.setPhilhealthNumber(philhealthNumber);
            emp.setPagibigNumber(pagibigNumber);
            emp.setHighestDegree(highestDegree);
            emp.setEmergencyContactName(emergencyContactName);
            emp.setEmergencyContactPhone(emergencyContactPhone);
            repository.save(emp);
        }
        return "redirect:/hr/records";
    }

    @GetMapping("/leave")
    public String showLeaveForm(Model model) {
        model.addAttribute("leaveRequest", new LeaveRequest());
        return "leave-form";
    }

    @PostMapping("/leave/submit")
    public String submitLeaveForm(@ModelAttribute LeaveRequest leaveRequest, Model model) {
        String message = service.processLeaveRequest(leaveRequest);
        model.addAttribute("message", message);
        model.addAttribute("leaveRequest", new LeaveRequest()); 
        return "leave-form";
    }

    @GetMapping("/hr/leaves")
    public String showHrLeaves(Model model) {
        model.addAttribute("pendingLeaves", service.getPendingLeaves());
        return "hr-leaves";
    }

    @PostMapping("/hr/leaves/approve")
    public String approveLeaveAction(@RequestParam int id) {
        service.approveLeave(id);
        return "redirect:/hr/leaves";
    }

    @PostMapping("/hr/leaves/reject")
    public String rejectLeaveAction(@RequestParam int id) {
        service.rejectLeave(id);
        return "redirect:/hr/leaves";
    }

    @GetMapping("/403")
    public String accessDenied() {
        return "403";
    }

    // ==========================================
    // EMPLOYEE SELF-SERVICE PORTAL ROUTES
    // ==========================================
    @GetMapping("/employee-login")
    public String showEmployeeLogin() {
        return "employee-login";
    }

    @GetMapping("/employee/dashboard")
    public String viewEmployeeDashboard(java.security.Principal principal, org.springframework.ui.Model model) {
        String loggedInId = principal.getName(); 
        com.example.employee.model.Applicant emp = repository.findByCustomEmployeeId(loggedInId).orElse(null);
        
        if (emp != null) {
            model.addAttribute("employee", emp);
            model.addAttribute("myLeaves", leaveRequestRepository.findByEmployeeIdOrderByIdDesc(emp.getId()));
        }
        return "employee-dashboard"; 
    }

    @PostMapping("/employee/leave/submit")
    public String submitLeaveRequest(@org.springframework.web.bind.annotation.ModelAttribute com.example.employee.model.LeaveRequest req, java.security.Principal principal, org.springframework.web.servlet.mvc.support.RedirectAttributes redirectAttributes) {
        com.example.employee.model.Applicant emp = repository.findByCustomEmployeeId(principal.getName()).orElse(null);
        
        if (emp != null) {
            req.setEmployeeId(emp.getId()); 
            String result = service.processLeaveRequest(req);
            
            if (result.contains("Error")) {
                redirectAttributes.addFlashAttribute("errorMessage", result);
            } else {
                redirectAttributes.addFlashAttribute("successMessage", result);
            }
        }
        return "redirect:/employee/dashboard";
    }

    @GetMapping("/hr/attendance")
    public String showEmployeeAttendance(@RequestParam int id, Model model) {
        Applicant employee = repository.findById(id).orElse(null);
        model.addAttribute("employee", employee);
        model.addAttribute("logs", service.getEmployeeAttendanceHistory(id));
        return "hr-attendance";
    }

    @GetMapping("/hr/attendance/download")
    public void downloadDTR(@RequestParam int id, @RequestParam(required = false) String cutoffPeriod, HttpServletResponse response) {
        response.setContentType("application/pdf");
        response.setHeader("Content-Disposition", "attachment; filename=DTR_EAC-" + String.format("%05d", id) + ".pdf");
        service.exportDTRToPDF(id, cutoffPeriod, response);
    }

    // ==========================================
    // NEW: DEDICATED BIOMETRICS DASHBOARD
    // ==========================================
    @GetMapping("/hr/biometrics")
    public String viewBiometricsDashboard(org.springframework.ui.Model model, @RequestParam(required = false) String cutoffPeriod) {
        
        java.util.Map<String, String> cutoffOptions = service.getRecentCutoffPeriods(6);
        
        if (cutoffPeriod == null || cutoffPeriod.isEmpty()) {
            cutoffPeriod = cutoffOptions.keySet().iterator().next(); 
        }
        
        model.addAttribute("employees", service.getPayrollData(cutoffPeriod));
        model.addAttribute("lastFile", service.getLastUploadedFileName());
        model.addAttribute("lastTime", service.getLastUploadTime());
        
        model.addAttribute("cutoffOptions", cutoffOptions);
        model.addAttribute("selectedCutoff", cutoffPeriod);
        
        return "hr-biometrics";
    }

    // ==========================================
    // CSV UPLOAD REDIRECT
    // ==========================================
    @PostMapping("/hr/biometrics/upload")
    public String uploadBiometricsCsv(@RequestParam("file") org.springframework.web.multipart.MultipartFile file, org.springframework.web.servlet.mvc.support.RedirectAttributes redirectAttributes) {
        try {
            if (file.isEmpty()) {
                redirectAttributes.addFlashAttribute("errorMessage", "Please select a CSV file to upload.");
                return "redirect:/hr/biometrics"; 
            }
            service.processBiometricsCsv(file);
            redirectAttributes.addFlashAttribute("successMessage", "TCMS V3 Biometrics successfully imported and computed!");
        } catch (Exception e) {
            redirectAttributes.addFlashAttribute("errorMessage", "Error importing file. Please check your CSV format.");
        }
        return "redirect:/hr/biometrics"; 
    }

    // ==========================================
    // INDIVIDUAL EMPLOYEE BIOMETRICS VIEW
    // ==========================================
    @GetMapping("/hr/employees/biometrics")
    public String viewSingleEmployeeBiometrics(@RequestParam("id") int id, @RequestParam(required = false) String cutoffPeriod, org.springframework.ui.Model model) {
        com.example.employee.model.Applicant emp = repository.findById(id).orElse(null);
        if (emp != null) {
            model.addAttribute("employee", emp);
            
            java.util.List<com.example.employee.model.AttendanceLog> logs = service.getEmployeeAttendanceHistoryByCutoff(id, cutoffPeriod);
            model.addAttribute("logs", logs);
            
            double totalRaw = 0;
            double totalReg = 0;
            double totalOt = 0;
            
            for (com.example.employee.model.AttendanceLog log : logs) {
                double raw = log.getHoursWorked();
                totalRaw += raw;
                double reg = Math.min(raw, 8.0); 
                totalReg += reg;
                double ot = raw > 8.0 ? (raw - 8.0) : 0.0;
                totalOt += ot;
            }
            
            model.addAttribute("totalRawText", ((int)totalRaw) + " hrs " + Math.round((totalRaw - (int)totalRaw) * 60) + " mins");
            model.addAttribute("totalRegText", ((int)totalReg) + " hrs " + Math.round((totalReg - (int)totalReg) * 60) + " mins");
            model.addAttribute("totalOtText", ((int)totalOt) + " hrs " + Math.round((totalOt - (int)totalOt) * 60) + " mins");
            
            model.addAttribute("selectedCutoff", cutoffPeriod);
        }
        return "hr-employee-biometrics";
    }

    // ==========================================
    // DELETE EMPLOYEE ROUTE
    // ==========================================
    @GetMapping("/hr/employees/delete")
    public String deleteEmployee(@RequestParam("id") int id, org.springframework.web.servlet.mvc.support.RedirectAttributes redirectAttributes) {
        service.deleteEmployee(id);
        redirectAttributes.addFlashAttribute("successMessage", "Employee successfully deleted from the database.");
        return "redirect:/hr/employees";
    }
}