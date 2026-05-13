package com.example.employee.controller;

import com.example.employee.model.Applicant;
import com.example.employee.model.OfficialEmployee;
import com.example.employee.model.LeaveRequest;
import com.example.employee.repository.ApplicantRepository;
import com.example.employee.repository.OfficialEmployeeRepository;
import com.example.employee.service.AdmissionService;
import static com.example.employee.service.AdmissionService.parseOptionalDate;
import com.example.employee.service.DepartmentCodeService;
import com.example.employee.service.LeaveCreditResetMode;
import com.example.employee.util.ListSearchUtil;
import com.example.employee.util.PayrollPeriodUtil;
import com.example.employee.util.PayrollDepartmentOptions;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.validation.BindingResult;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.ModelAttribute;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.multipart.MultipartFile;
import org.springframework.web.util.UriComponentsBuilder;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

import java.io.IOException;
import java.time.LocalDate;
import java.time.YearMonth;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.Optional;

import org.springframework.format.annotation.DateTimeFormat;

import jakarta.servlet.http.HttpServletResponse;
import jakarta.validation.Valid;

@Controller
public class AdmissionController {

    private static final Logger log = LoggerFactory.getLogger(AdmissionController.class);
    
    @Autowired
    private com.example.employee.repository.LeaveRequestRepository leaveRequestRepository;
    
    @Autowired
    private com.example.employee.repository.HolidayRepository holidayRepository;
    
    @Autowired
    private AdmissionService service;
    
    @Autowired
    private ApplicantRepository repository;

    @Autowired
    private OfficialEmployeeRepository officialEmployeeRepository;

    @Autowired
    private DepartmentCodeService departmentCodeService;

    @GetMapping("/")
    public String showLandingPage() { 
        return "index"; 
    }

    @GetMapping("/apply")
    public String showForm(Model model) {
        model.addAttribute("applicant", new Applicant());
        addApplicationFormDepartments(model);
        return "admission-form";
    }

    @PostMapping(value = "/submit", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    public String submitForm(
            @Valid @ModelAttribute Applicant applicant,
            BindingResult bindingResult,
            @RequestParam(value = "resumePdf", required = false) MultipartFile resumePdf,
            Model model) {
        addApplicationFormDepartments(model);

        String resumeError = null;
        if (resumePdf == null || resumePdf.isEmpty()) {
            resumeError = "Please upload your r\u00e9sum\u00e9 as a PDF file.";
        } else {
            String lowerName = Optional.ofNullable(resumePdf.getOriginalFilename()).orElse("").toLowerCase(Locale.ROOT);
            if (!lowerName.endsWith(".pdf")) {
                resumeError = "The file must be a PDF (.pdf).";
            } else if (resumePdf.getSize() > 5L * 1024 * 1024) {
                resumeError = "Maximum file size is 5 MB.";
            }
        }
        if (resumeError != null) {
            model.addAttribute("resumeError", resumeError);
        }
        if (bindingResult.hasErrors() || resumeError != null) {
            return "admission-form";
        }
        try {
            service.submitApplication(applicant, resumePdf);
        } catch (IOException e) {
            model.addAttribute("resumeError", "We could not save your file. Please try again.");
            addApplicationFormDepartments(model);
            return "admission-form";
        }
        return "success";
    }

    /** Public application: master department list, with static payroll list if DB is not yet seeded. */
    private void addApplicationFormDepartments(Model model) {
        var eac = departmentCodeService.listAllForUi();
        model.addAttribute("eacDepartments", eac);
        model.addAttribute("usePayrollDepartmentFallback", eac.isEmpty());
        if (eac.isEmpty()) {
            model.addAttribute("payrollDepartments", PayrollDepartmentOptions.ALL);
        }
    }

    @GetMapping("/hr/dashboard")
    public String showEacSystemDashboard(Model model) {
        model.addAttribute("employeeStats", service.getEmployeeListStats());
        model.addAttribute("pendingApplicants", service.countApplicantsByStatus("PENDING"));
        model.addAttribute("scheduledApplicants", service.countApplicantsByStatus("ACCEPTED"));
        model.addAttribute("employmentMilestones", service.getEmploymentMilestoneAlerts(60, 45));
        model.addAttribute("todayStats", service.getTodayAttendanceStats());
        model.addAttribute("todayBirthdays", service.getTodayBirthdays());
        model.addAttribute("upcomingBirthdays", service.getUpcomingBirthdays(7));
        model.addAttribute("pendingLeave", service.countPendingLeaveRequests());
        model.addAttribute("pendingOT", service.countPendingOvertimeThisMonth());
        return "hr-dashboard";
    }

    @GetMapping("/hr/applicants")
    public String showHrApplicants(Model model, @RequestParam(required = false) String status, @RequestParam(required = false) String search) {
        model.addAttribute("applicants", service.getDashboardApplicants(status, search));
        model.addAttribute("selectedStatus", status);
        model.addAttribute("searchKeyword", search);
        return "hr-applicants";
    }
    
    @GetMapping("/hr/employees")
    public String showEmployees(Model model,
                                @RequestParam(required = false) String department,
                                @RequestParam(required = false) String search,
                                @RequestParam(defaultValue = "list") String tab) {
        String deptParam = (department == null || department.isBlank()) ? null : departmentCodeService.toCanonicalCode(department);
        boolean isArchive = "archive".equalsIgnoreCase(tab);
        if (isArchive) {
            model.addAttribute("employees", service.getArchivedEmployees(department, search));
        } else {
            model.addAttribute("employees", service.getEmployees(department, search));
        }
        model.addAttribute("selectedDept", deptParam);
        model.addAttribute("searchKeyword", search);
        model.addAttribute("employeeTab", isArchive ? "archive" : "list");
        model.addAttribute("isArchive", isArchive);
        model.addAttribute("employeeStats", service.getEmployeeListStats());
        model.addAttribute("eacDepartments", service.getEacDepartmentsForFilter());
        model.addAttribute("deptFac", service.buildFacultyDepartmentOptions());
        model.addAttribute("deptNonFac", service.buildNonFacultyDepartmentOptions());
        model.addAttribute("allBranches", service.getDistinctCampusCodes());
        return "hr-employees";
    }

    @GetMapping("/hr/employee")
    public String showEmployeesAlias() {
        return "redirect:/hr/employees";
    }

    @PostMapping("/hr/accept")
    public String acceptApplicant(@RequestParam int id, @RequestParam String date, @RequestParam String time) {
        service.acceptApplicant(id, date, time);
        return "redirect:/hr/applicants";
    }

    @PostMapping("/hr/reject")
    public String rejectApplicant(@RequestParam int id) {
        service.rejectApplicant(id);
        return "redirect:/hr/applicants";
    }

    @PostMapping("/hr/hire")
    public String hireApplicant(@RequestParam int id, @RequestParam String date, @RequestParam String time) {
        service.hireApplicant(id, date, time);
        return "redirect:/hr/applicants";
    }

    @PostMapping("/hr/employees/add")
    public String addF2FEmployee(
            @ModelAttribute OfficialEmployee employee,
            @RequestParam(required = false) String dateHired,
            @RequestParam(required = false) String probationEndDate,
            @RequestParam(required = false) String contractEndDate,
            @RequestParam(required = false) String separationDate,
            @RequestParam(required = false) String separationNote) {
        employee.setDateHired(parseOptionalDate(dateHired));
        employee.setProbationEndDate(parseOptionalDate(probationEndDate));
        employee.setContractEndDate(parseOptionalDate(contractEndDate));
        employee.setSeparationDate(parseOptionalDate(separationDate));
        if (separationNote != null && !separationNote.isBlank()) {
            String t = separationNote.trim();
            employee.setSeparationNote(t.length() > 500 ? t.substring(0, 500) : t);
        }
        service.addEmployeeManually(employee);
        return "redirect:/hr/employees";
    }

    @GetMapping("/hr/leave-credits/reset")
    public String leaveCreditsResetPage(Model model) {
        model.addAttribute("employees", service.getEmployees(null, null));
        model.addAttribute("eacDepartments", service.getEacDepartmentsForFilter());
        return "hr-leave-credits-reset";
    }

    @PostMapping("/hr/leave-credits/reset")
    public String resetLeaveCredits(
            @RequestParam String mode,
            @RequestParam(required = false) String department,
            @RequestParam(required = false) List<Long> employeeIds,
            RedirectAttributes redirectAttributes) {
        try {
            LeaveCreditResetMode m = LeaveCreditResetMode.valueOf(mode.trim().toUpperCase(Locale.ROOT));
            int n = service.resetLeaveCredits(m, department, employeeIds);
            if (n == 0) {
                redirectAttributes.addFlashAttribute("errorMessage",
                    "No active employees were updated. Choose a valid scope (e.g. select at least one employee, or pick a department).");
            } else {
                redirectAttributes.addFlashAttribute("successMessage", "Leave credits reset for " + n + " employee(s).");
            }
        } catch (IllegalArgumentException ex) {
            log.warn("Invalid leave reset mode: {}", mode);
            redirectAttributes.addFlashAttribute("errorMessage", "Invalid reset mode.");
        }
        return "redirect:/hr/leave-credits/reset";
    }

    @PostMapping("/hr/employees/edit")
    public String editEmployee(java.security.Principal principal,
                               @RequestParam Long id, 
                               @RequestParam String department, 
                               @RequestParam String positionApplied, 
                               @RequestParam String status,
                               @RequestParam Double dailyWage,
                               @RequestParam(value = "basicSalary", required = false) String basicSalaryParam,
                               @RequestParam int vlBalance,
                               @RequestParam int slBalance,
                               @RequestParam int mlBalance,
                               @RequestParam int plBalance,
                               @RequestParam int splBalance,
                               @RequestParam int blBalance,
                               @RequestParam int incentiveLeaveBalance, 
                               @RequestParam int studyLeaveBalance,     
                               @RequestParam String employmentType,
                               @RequestParam String paymentType,
                               @RequestParam String expectedShift,
                               @RequestParam(required = false) Integer biometricId,
                               @RequestParam(required = false) String dateHired,
                               @RequestParam(required = false) String probationEndDate,
                               @RequestParam(required = false) String contractEndDate,
                               @RequestParam(required = false) String separationDate,
                               @RequestParam(required = false) String separationNote,
                               @RequestParam(required = false) String gender,
                               @RequestParam(required = false) String civilStatus,
                               @RequestParam(required = false) String birthDate,
                               @RequestParam(required = false) String birthPlace,
                               @RequestParam(required = false) String nationality,
                               @RequestParam(required = false) String presentAddress,
                               @RequestParam(required = false) String permanentAddress,
                               @RequestParam(required = false) String emergencyContactName,
                               @RequestParam(required = false) String emergencyContactPhone,
                               @RequestParam(required = false) String emergencyContactRelationship,
                               @RequestParam(required = false) String secondaryEmergencyContactName,
                               @RequestParam(required = false) String secondaryEmergencyContactPhone) { 

        Double basicSalary = parseOptionalDouble(basicSalaryParam);
                               
        service.updateEmployee(id, department, positionApplied, status, dailyWage, basicSalary,
                               vlBalance, slBalance, mlBalance, plBalance, splBalance, blBalance,
                               incentiveLeaveBalance, studyLeaveBalance,
                               employmentType, paymentType, expectedShift, biometricId,
                               dateHired, probationEndDate, contractEndDate, separationDate, separationNote,
                               gender, civilStatus, birthDate, birthPlace, nationality,
                               presentAddress, permanentAddress, emergencyContactName, emergencyContactPhone,
                               emergencyContactRelationship, secondaryEmergencyContactName, secondaryEmergencyContactPhone);
        String actor = principal != null ? principal.getName() : "HR";
        service.recordHrAudit(actor, "UPDATE_EMPLOYEE", "OfficialEmployee", id,
            "dept=" + department + " position=" + positionApplied + " status=" + status);
        return "redirect:/hr/employees";
    }

    private static Double parseOptionalDouble(String raw) {
        if (raw == null || raw.isBlank()) {
            return null;
        }
        try {
            return Double.valueOf(raw.trim());
        } catch (NumberFormatException e) {
            return null;
        }
    }

    @GetMapping("/kiosk")
    public String showKiosk() { 
        return "kiosk"; 
    }

    @PostMapping("/kiosk/tap")
    public String processKioskTap(@RequestParam String employeeId, @RequestParam String action, Model model) {
        String message = service.processBiometrics(employeeId, action);
        model.addAttribute("message", message);
        return "kiosk";
    }

    @GetMapping("/hr/payroll")
    public String showPayroll(
            Model model,
            @RequestParam(required = false) String search,
            @RequestParam(required = false) String cutoffPeriod,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate customFrom,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate customTo,
            @RequestParam(required = false) String applyMode) {
        String key = service.effectivePayrollCutoffKey(cutoffPeriod, customFrom, customTo, applyMode);
        model.addAttribute("payrollList", service.getPayrollData(key, search));
        model.addAttribute("selectedPayrollCutoff", key);
        model.addAttribute("searchKeyword", search);
        model.addAttribute("cutoffOptions", service.getCutoffOptionsWithPresets(6));
        enrichCutoffModel(model, key);
        model.addAttribute("cutoffPeriod", PayrollPeriodUtil.formatEnglishRangeLabel(PayrollPeriodUtil.resolve(key)));
        return "hr-payroll";
    }

    @GetMapping("/hr/payroll/download")
    public void downloadPayslip(@RequestParam int id, @RequestParam(required = false) String cutoffPeriod, HttpServletResponse response) {
        response.setContentType("application/pdf");
        response.setHeader("Content-Disposition", "attachment; filename=EAC_Payslip_EAC" + id + ".pdf");
        service.exportPayslipToPDF(id, cutoffPeriod, response);
    }

    @GetMapping("/hr/salary-audit")
    public String showSalaryAudit(
            Model model,
            @RequestParam(required = false) String search,
            @RequestParam(required = false) String department,
            @RequestParam(required = false) String cutoff,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate customFrom,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate customTo,
            @RequestParam(required = false) String applyMode) {
        String key = service.effectivePayrollCutoffKey(
            (cutoff == null || cutoff.isBlank()) ? null : cutoff,
            customFrom, customTo, applyMode);
        if (key == null || key.isBlank()) {
            key = service.getCurrentPayrollCutoffKey();
        }
        PayrollPeriodUtil.PayrollPeriod p = PayrollPeriodUtil.resolve(key);
        model.addAttribute("rows", service.listSalaryAudit(key, search, department));
        model.addAttribute("selectedCutoff", key);
        model.addAttribute("cutoffOptions", service.getCutoffOptionsWithPresets(6));
        model.addAttribute("searchKeyword", search);
        model.addAttribute("selectedDept", department);
        model.addAttribute("eacDepartments", departmentCodeService.listAllForUi());
        model.addAttribute("periodDayCount", p.inclusiveDayCount());
        enrichCutoffModel(model, key);
        return "hr-salary-audit";
    }

    @GetMapping("/hr/records")
    public String showHrRecords(Model model, @RequestParam(required = false) String search) {
        List<OfficialEmployee> list = officialEmployeeRepository.findAll();
        if (ListSearchUtil.isActiveKeyword(search)) {
            list = list.stream().filter(e -> ListSearchUtil.matchesEmployee(e, search)).toList();
        }
        model.addAttribute("employees", list);
        model.addAttribute("searchKeyword", search);
        return "hr-records";
    }

    @PostMapping("/hr/records/update")
    public String updateEmployeeRecords(
            java.security.Principal principal,
            @RequestParam Long id,
            @RequestParam(required = false) String sssNumber,
            @RequestParam(required = false) String tinNumber,
            @RequestParam(required = false) String philhealthNumber,
            @RequestParam(required = false) String pagibigNumber,
            @RequestParam(required = false) String highestDegree,
            @RequestParam(required = false) String emergencyContactName,
            @RequestParam(required = false) String emergencyContactPhone,
            @RequestParam(required = false) String emergencyContactRelationship,
            @RequestParam(required = false) String secondaryEmergencyContactName,
            @RequestParam(required = false) String secondaryEmergencyContactPhone) {

        OfficialEmployee emp = officialEmployeeRepository.findById(id).orElse(null);
        if (emp != null) {
            emp.setSssNumber(blankToNull(sssNumber));
            emp.setTinNumber(blankToNull(tinNumber));
            emp.setPhilhealthNumber(blankToNull(philhealthNumber));
            emp.setPagibigNumber(blankToNull(pagibigNumber));
            emp.setHighestDegree(blankToNull(highestDegree));
            emp.setEmergencyContactName(blankToNull(emergencyContactName));
            emp.setEmergencyContactPhone(blankToNull(emergencyContactPhone));
            emp.setEmergencyContactRelationship(blankToNull(emergencyContactRelationship));
            emp.setSecondaryEmergencyContactName(blankToNull(secondaryEmergencyContactName));
            emp.setSecondaryEmergencyContactPhone(blankToNull(secondaryEmergencyContactPhone));
            officialEmployeeRepository.save(emp);
            String actor = principal != null ? principal.getName() : "HR";
            service.recordHrAudit(actor, "UPDATE_201_RECORDS", "OfficialEmployee", id, "gov IDs / emergency / education from HR records screen");
        }
        return "redirect:/hr/records";
    }

    @PostMapping("/hr/employees/{id}/info/update")
    public String updateEmployeeInfoInline(
            java.security.Principal principal,
            @PathVariable Long id,
            @RequestParam(required = false) String email,
            @RequestParam(required = false) String phone,
            RedirectAttributes ra) {
        OfficialEmployee emp = officialEmployeeRepository.findById(id).orElse(null);
        if (emp == null) {
            ra.addFlashAttribute("errorMessage", "Employee not found.");
            return "redirect:/hr/employees";
        }
        emp.setEmail(blankToNull(email));
        emp.setPhone(blankToNull(phone));
        officialEmployeeRepository.save(emp);
        String actor = principal != null ? principal.getName() : "HR";
        service.recordHrAudit(actor, "UPDATE_EMPLOYEE_INFO", "OfficialEmployee", id, "inline Info tab update");
        ra.addFlashAttribute("successMessage", "Employee info updated.");
        return "redirect:/hr/employees/" + id + "?tab=info";
    }

    private static String blankToNull(String s) {
        if (s == null) {
            return null;
        }
        String t = s.trim();
        return t.isEmpty() ? null : t;
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
    public String showHrLeaves(
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate dateFrom,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate dateTo,
            Model model) {
        LocalDate today = LocalDate.now();
        if (dateFrom == null) {
            dateFrom = today.withDayOfYear(1);
        }
        if (dateTo == null) {
            dateTo = today;
        }
        if (dateFrom.isAfter(dateTo)) {
            LocalDate t = dateFrom;
            dateFrom = dateTo;
            dateTo = t;
        }
        model.addAttribute("dateFrom", dateFrom);
        model.addAttribute("dateTo", dateTo);
        model.addAttribute("leaveRows", service.buildAdminLeaveList(dateFrom, dateTo));
        model.addAttribute("leaveMonthStats", service.getLeaveStatsForMonth(YearMonth.from(today)));
        model.addAttribute("allLeaveTypes", service.getLeaveTypeFilterOptions());
        model.addAttribute("allDepartments", service.getDistinctDepartments());
        model.addAttribute("allBranches", service.getDistinctCampusCodes());
        List<String> des = new ArrayList<>();
        officialEmployeeRepository.findByStatus("Active").forEach(e -> {
            if (e.getPosition() != null && !e.getPosition().isBlank()) {
                des.add(e.getPosition().trim());
            }
        });
        model.addAttribute("allDesignations", des.stream().distinct().sorted(String.CASE_INSENSITIVE_ORDER).toList());
        return "hr-leaves";
    }

    @GetMapping("/hr/leaves/review")
    public String showLeaveReviewPage(@RequestParam int id, Model model) {
        LeaveRequest req = leaveRequestRepository.findById(id).orElse(null);
        if (req == null) {
            return "redirect:/hr/leaves";
        }
        if (!"PENDING".equals(req.getStatus())) {
            return "redirect:/hr/leaves";
        }
        model.addAttribute("req", req);
        model.addAttribute("leaveEmployee", officialEmployeeRepository.findById((long) req.getEmployeeId()).orElse(null));
        return "hr-leave-approval";
    }

    @PostMapping("/hr/overtime-approvals/approve")
    public String approveOvertime(
            @RequestParam long attendanceId,
            @RequestParam(required = false) String cutoffPeriod,
            @RequestParam(required = false) String search) {
        service.approveOvertimeAttendance(attendanceId);
        if (ListSearchUtil.isActiveKeyword(search)) {
            return "redirect:" + UriComponentsBuilder.fromPath("/hr/overtime")
                    .queryParam("search", search)
                    .build()
                    .toUriString();
        }
        return "redirect:/hr/overtime";
    }

    @PostMapping("/hr/overtime-approvals/reject")
    public String rejectOvertime(
            @RequestParam long attendanceId,
            @RequestParam(required = false) String cutoffPeriod,
            @RequestParam(required = false) String search) {
        service.rejectOvertimeAttendance(attendanceId);
        if (ListSearchUtil.isActiveKeyword(search)) {
            return "redirect:" + UriComponentsBuilder.fromPath("/hr/overtime")
                    .queryParam("search", search)
                    .build()
                    .toUriString();
        }
        return "redirect:/hr/overtime";
    }

    @GetMapping("/hr/leave-history")
    public String viewLeaveHistoryList(Model model, @RequestParam(required = false) String search) {
        List<OfficialEmployee> list = new ArrayList<>();
        officialEmployeeRepository.findByStatus("Active").forEach(list::add);
        if (ListSearchUtil.isActiveKeyword(search)) {
            list = list.stream().filter(e -> ListSearchUtil.matchesEmployee(e, search)).toList();
        }
        model.addAttribute("employees", list);
        model.addAttribute("searchKeyword", search);
        return "leave-history"; 
    }

    @GetMapping("/hr/leave-history/{id}")
    public String viewEmployeeLeaveCalendar(@org.springframework.web.bind.annotation.PathVariable Long id, Model model) { 
        OfficialEmployee emp = officialEmployeeRepository.findById(id).orElse(null);
        if (emp == null) return "redirect:/hr/leave-history";

        java.util.List<LeaveRequest> allLeaves = leaveRequestRepository.findByEmployeeIdOrderByIdDesc(id.intValue());

        model.addAttribute("employee", emp);
        model.addAttribute("allLeaves", allLeaves);
        return "leave-calendar";
    }

    @PostMapping("/hr/leaves/approve")
    public String approveLeaveAction(
            @RequestParam int id,
            @RequestParam(required = false) String search,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate dateFrom,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate dateTo,
            java.security.Principal principal) {
        String by = principal != null ? principal.getName() : "HR";
        service.approveLeave(id, by);
        return buildRedirectToLeaves(search, dateFrom, dateTo);
    }

    @PostMapping("/hr/leaves/reject")
    public String rejectLeaveAction(
            @RequestParam int id,
            @RequestParam(required = false) String search,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate dateFrom,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate dateTo,
            java.security.Principal principal) {
        String by = principal != null ? principal.getName() : "HR";
        service.rejectLeave(id, by);
        return buildRedirectToLeaves(search, dateFrom, dateTo);
    }

    private String buildRedirectToLeaves(String search, LocalDate dateFrom, LocalDate dateTo) {
        UriComponentsBuilder b = UriComponentsBuilder.fromPath("/hr/leaves");
        if (ListSearchUtil.isActiveKeyword(search)) {
            b.queryParam("search", search);
        }
        if (dateFrom != null) {
            b.queryParam("dateFrom", dateFrom.toString());
        }
        if (dateTo != null) {
            b.queryParam("dateTo", dateTo.toString());
        }
        return "redirect:" + b.build().toUriString();
    }

    @GetMapping("/employee/leaves")
    public String employeeLeaves(
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate dateFrom,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate dateTo,
            java.security.Principal principal,
            Model model) {
        OfficialEmployee emp = officialEmployeeRepository.findByCustomEmployeeId(principal.getName()).orElse(null);
        if (emp == null) {
            return "redirect:/employee-login";
        }
        LocalDate today = LocalDate.now();
        if (dateFrom == null) {
            dateFrom = today.withDayOfYear(1);
        }
        if (dateTo == null) {
            dateTo = today;
        }
        if (dateFrom.isAfter(dateTo)) {
            LocalDate t = dateFrom;
            dateFrom = dateTo;
            dateTo = t;
        }
        model.addAttribute("employee", emp);
        model.addAttribute("dateFrom", dateFrom);
        model.addAttribute("dateTo", dateTo);
        model.addAttribute("leaveRows", service.buildEmployeeLeaveList(emp.getId().intValue(), dateFrom, dateTo));
        model.addAttribute("allLeaves", leaveRequestRepository.findByEmployeeIdOrderByIdDesc(emp.getId().intValue()));
        model.addAttribute("leaveMonthStats", service.getLeaveStatsForEmployeeMonth(emp.getId().intValue(), YearMonth.from(today)));
        model.addAttribute("allLeaveTypes", service.getLeaveTypeFilterOptions());
        return "employee-leaves";
    }

    @GetMapping("/403")
    public String accessDenied() {
        return "403";
    }

    @GetMapping({"/employee-login", "/login"})
    public String showEmployeeLogin() {
        return "employee-login";
    }

    @GetMapping("/hr/holidays")
    public String viewHolidays(Model model, @RequestParam(required = false) String search) {
        java.util.List<com.example.employee.model.Holiday> holidays = holidayRepository.findAll();
        if (ListSearchUtil.isActiveKeyword(search)) {
            holidays = holidays.stream()
                .filter(h -> ListSearchUtil.matchesHoliday(h, search))
                .toList();
        }
        holidays = new java.util.ArrayList<>(holidays);
        holidays.sort(java.util.Comparator.comparing(com.example.employee.model.Holiday::getDate));
        model.addAttribute("holidays", holidays);
        model.addAttribute("searchKeyword", search);
        return "hr-holidays"; 
    }

    @GetMapping("/employee/dashboard")
    public String viewEmployeeDashboard(java.security.Principal principal, Model model) {
        String loggedInId = principal.getName(); 
        OfficialEmployee emp = officialEmployeeRepository.findByCustomEmployeeId(loggedInId).orElse(null);
        
        if (emp != null) {
            model.addAttribute("employee", emp);
            model.addAttribute("myLeaves", leaveRequestRepository.findByEmployeeIdOrderByIdDesc(emp.getId().intValue()));
            model.addAttribute("otCutoffKey", service.getCurrentPayrollCutoffKey());
            model.addAttribute("otLines", service.getEmployeeOvertimeReviewLines(emp.getId().intValue(), service.getCurrentPayrollCutoffKey()));
        }
        return "employee-dashboard"; 
    }

    @GetMapping("/employee/my-record")
    public String employeePersonnel201(java.security.Principal principal, Model model) {
        OfficialEmployee emp = officialEmployeeRepository.findByCustomEmployeeId(principal.getName()).orElse(null);
        if (emp == null) {
            return "redirect:/employee/dashboard";
        }
        model.addAttribute("employee", emp);
        model.addAttribute("recordMode", "employee");
        return "personnel-record";
    }

    @GetMapping("/hr/employees/{id}/record")
    public String hrPersonnel201(@PathVariable Long id, Model model) {
        OfficialEmployee emp = officialEmployeeRepository.findById(id).orElse(null);
        if (emp == null) {
            return "redirect:/hr/employees";
        }
        model.addAttribute("employee", emp);
        model.addAttribute("recordMode", "hr");
        return "personnel-record";
    }

    @GetMapping("/hr/employees/{id}")
    public String hrEmployeeWorkspace(@PathVariable Long id,
                                      @RequestParam(required = false, defaultValue = "info") String tab,
                                      Model model) {
        OfficialEmployee emp = officialEmployeeRepository.findById(id).orElse(null);
        if (emp == null) {
            return "redirect:/hr/employees";
        }
        String activeTab = switch (tab == null ? "" : tab.trim().toLowerCase(Locale.ROOT)) {
            case "201" -> "201";
            case "dtr" -> "dtr";
            case "eac" -> "eac";
            default -> "info";
        };
        model.addAttribute("employee", emp);
        model.addAttribute("recordMode", "hr");
        model.addAttribute("activeTab", activeTab);
        model.addAttribute("logs", service.getEmployeeAttendanceHistory(emp.getId().intValue()));
        return "hr-employee-workspace";
    }

    @PostMapping("/hr/employees/{id}/eac/update")
    public String updateEmployeeEacSettings(
            java.security.Principal principal,
            @PathVariable Long id,
            @RequestParam(required = false) String department,
            @RequestParam(required = false) String position,
            @RequestParam(required = false) String status,
            @RequestParam(required = false) String employmentType,
            @RequestParam(required = false) String expectedShift,
            @RequestParam(required = false) String dateHired,
            @RequestParam(required = false) String probationEndDate,
            @RequestParam(required = false) String contractEndDate,
            @RequestParam(required = false) String separationDate,
            @RequestParam(required = false) Integer biometricId,
            @RequestParam(required = false) String paymentType,
            @RequestParam(required = false) String basicSalary,
            @RequestParam(required = false) String dailyWage,
            @RequestParam int vlBalance,
            @RequestParam int slBalance,
            @RequestParam int mlBalance,
            @RequestParam int plBalance,
            @RequestParam int splBalance,
            @RequestParam int blBalance,
            @RequestParam int incentiveLeaveBalance,
            @RequestParam int studyLeaveBalance,
            RedirectAttributes ra) {

        OfficialEmployee emp = officialEmployeeRepository.findById(id).orElse(null);
        if (emp == null) {
            ra.addFlashAttribute("errorMessage", "Employee not found.");
            return "redirect:/hr/employees";
        }

        if (department != null && !department.isBlank()) {
            emp.setDepartment(departmentCodeService.toCanonicalCode(department));
        } else {
            emp.setDepartment(null);
        }
        emp.setPosition(blankToNull(position));
        emp.setStatus(blankToNull(status));
        emp.setEmploymentType(blankToNull(employmentType));
        emp.setExpectedShift(blankToNull(expectedShift));
        emp.setDateHired(parseOptionalDate(dateHired));
        emp.setProbationEndDate(parseOptionalDate(probationEndDate));
        emp.setContractEndDate(parseOptionalDate(contractEndDate));
        emp.setSeparationDate(parseOptionalDate(separationDate));
        emp.setBiometricId(biometricId);
        emp.setPaymentType(blankToNull(paymentType));
        emp.setBasicSalary(parseOptionalDouble(basicSalary));
        emp.setDailyWage(parseOptionalDouble(dailyWage));

        emp.setVlBalance(vlBalance);
        emp.setSlBalance(slBalance);
        emp.setMlBalance(mlBalance);
        emp.setPlBalance(plBalance);
        emp.setSplBalance(splBalance);
        emp.setBlBalance(blBalance);
        emp.setIncentiveLeaveBalance(incentiveLeaveBalance);
        emp.setStudyLeaveBalance(studyLeaveBalance);

        officialEmployeeRepository.save(emp);
        String actor = principal != null ? principal.getName() : "HR";
        service.recordHrAudit(actor, "UPDATE_EMPLOYEE_EAC", "OfficialEmployee", id, "inline EAC tab update");
        ra.addFlashAttribute("successMessage", "EAC settings updated.");
        return "redirect:/hr/employees/" + id + "?tab=eac";
    }

    @PostMapping("/hr/employees/{id}/profile/photo")
    public String hrUploadEmployeeProfilePhoto(
            @PathVariable Long id,
            @RequestParam("photo") MultipartFile photo,
            org.springframework.web.servlet.mvc.support.RedirectAttributes ra) {
        OfficialEmployee emp = officialEmployeeRepository.findById(id).orElse(null);
        if (emp == null) {
            ra.addFlashAttribute("errorMessage", "Employee not found.");
            return "redirect:/hr/employees";
        }
        try {
            service.saveEmployeeProfilePhoto(emp, photo);
            ra.addFlashAttribute("successMessage", "Profile photo updated for " + emp.getCustomEmployeeId() + ".");
        } catch (IllegalArgumentException e) {
            ra.addFlashAttribute("errorMessage", e.getMessage());
        } catch (IOException e) {
            log.warn("HR profile photo upload failed for employee {}: {}", id, e.getMessage());
            ra.addFlashAttribute("errorMessage", "Could not save photo. Try a smaller image (JPEG/PNG/WebP).");
        } catch (Exception e) {
            ra.addFlashAttribute("errorMessage", "Could not upload photo. Try a smaller image (JPEG/PNG).");
        }
        return "redirect:/hr/employees/" + id + "?tab=201";
    }

    @PostMapping("/hr/employees/{id}/201/update")
    public String updateEmployee201Inline(
            java.security.Principal principal,
            @PathVariable Long id,
            @RequestParam(required = false) String phone,
            @RequestParam(required = false) String gender,
            @RequestParam(required = false) String civilStatus,
            @RequestParam(required = false) String birthDate,
            @RequestParam(required = false) String birthPlace,
            @RequestParam(required = false) String nationality,
            @RequestParam(required = false) String presentAddress,
            @RequestParam(required = false) String permanentAddress,
            @RequestParam(required = false) String sssNumber,
            @RequestParam(required = false) String tinNumber,
            @RequestParam(required = false) String philhealthNumber,
            @RequestParam(required = false) String pagibigNumber,
            @RequestParam(required = false) String highestDegree,
            @RequestParam(required = false) String yearsExperience,
            @RequestParam(required = false) String previousEmployer,
            @RequestParam(required = false) String experienceText,
            @RequestParam(required = false) String emergencyContactName,
            @RequestParam(required = false) String emergencyContactPhone,
            @RequestParam(required = false) String emergencyContactRelationship,
            @RequestParam(required = false) String secondaryEmergencyContactName,
            @RequestParam(required = false) String secondaryEmergencyContactPhone,
            RedirectAttributes ra) {

        OfficialEmployee emp = officialEmployeeRepository.findById(id).orElse(null);
        if (emp == null) {
            ra.addFlashAttribute("errorMessage", "Employee not found.");
            return "redirect:/hr/employees";
        }

        emp.setPhone(blankToNull(phone));
        emp.setGender(blankToNull(gender));
        emp.setCivilStatus(blankToNull(civilStatus));
        emp.setBirthDate(parseOptionalDate(birthDate));
        emp.setBirthPlace(blankToNull(birthPlace));
        emp.setNationality(blankToNull(nationality));
        emp.setPresentAddress(blankToNull(presentAddress));
        emp.setPermanentAddress(blankToNull(permanentAddress));

        emp.setSssNumber(blankToNull(sssNumber));
        emp.setTinNumber(blankToNull(tinNumber));
        emp.setPhilhealthNumber(blankToNull(philhealthNumber));
        emp.setPagibigNumber(blankToNull(pagibigNumber));

        emp.setHighestDegree(blankToNull(highestDegree));
        try {
            emp.setYearsExperience(yearsExperience == null || yearsExperience.isBlank() ? 0 : Integer.parseInt(yearsExperience.trim()));
        } catch (Exception ignored) {
            emp.setYearsExperience(0);
        }
        emp.setPreviousEmployer(blankToNull(previousEmployer));
        emp.setExperienceText(blankToNull(experienceText));

        emp.setEmergencyContactName(blankToNull(emergencyContactName));
        emp.setEmergencyContactPhone(blankToNull(emergencyContactPhone));
        emp.setEmergencyContactRelationship(blankToNull(emergencyContactRelationship));
        emp.setSecondaryEmergencyContactName(blankToNull(secondaryEmergencyContactName));
        emp.setSecondaryEmergencyContactPhone(blankToNull(secondaryEmergencyContactPhone));

        officialEmployeeRepository.save(emp);
        String actor = principal != null ? principal.getName() : "HR";
        service.recordHrAudit(actor, "UPDATE_EMPLOYEE_201", "OfficialEmployee", id, "inline 201 tab update");
        ra.addFlashAttribute("successMessage", "201 record updated.");
        return "redirect:/hr/employees/" + id + "?tab=201";
    }

    @PostMapping("/employee/leave/submit")
    public String submitLeaveRequest(@ModelAttribute LeaveRequest req, java.security.Principal principal, org.springframework.web.servlet.mvc.support.RedirectAttributes redirectAttributes) {
        OfficialEmployee emp = officialEmployeeRepository.findByCustomEmployeeId(principal.getName()).orElse(null);
        
        if (emp != null) {
            req.setEmployeeId(emp.getId().intValue()); 
            String result = service.processLeaveRequest(req);
            
            if (result.contains("Error")) {
                redirectAttributes.addFlashAttribute("errorMessage", result);
            } else {
                redirectAttributes.addFlashAttribute("successMessage", result);
            }
        }
        return "redirect:/employee/leaves";
    }

    @GetMapping("/hr/attendance")
    public String showAttendanceAdmin(
            @RequestParam(required = false) Integer id,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate dateFrom,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate dateTo,
            @RequestParam(required = false, defaultValue = "attendance") String tab,
            Model model) {
        if (id != null) {
            return "redirect:/hr/attendance/record?id=" + id;
        }
        LocalDate today = LocalDate.now();
        if (dateFrom == null) {
            dateFrom = today;
        }
        if (dateTo == null) {
            dateTo = today;
        }
        if (dateFrom.isAfter(dateTo)) {
            LocalDate tmp = dateFrom;
            dateFrom = dateTo;
            dateTo = tmp;
        }
        model.addAttribute("dateFrom", dateFrom);
        model.addAttribute("dateTo", dateTo);
        model.addAttribute("adminTab", tab);
        model.addAttribute("dayStats", service.getAttendanceAdminDayStats(dateTo));
        model.addAttribute("attendanceRows", service.buildAdminAttendanceRows(dateFrom, dateTo));
        model.addAttribute("otPending", service.getOvertimePendingForHr(service.getCurrentPayrollCutoffKey(), null));
        model.addAttribute("allDepartments", service.getDistinctDepartments());
        model.addAttribute("allBranches", service.getDistinctCampusCodes());
        model.addAttribute("allDesignations", service.getDistinctDesignationsForActiveEmployees());
        return "hr-attendance-admin";
    }

    @GetMapping("/hr/attendance/record")
    public String showHrAttendanceRecord(@RequestParam int id, Model model) {
        OfficialEmployee employee = officialEmployeeRepository.findById((long) id).orElse(null);
        model.addAttribute("employee", employee);
        model.addAttribute("logs", service.getEmployeeAttendanceHistory(id));
        return "hr-attendance-record";
    }

    @GetMapping("/employee/attendance")
    public String employeeAttendancePage(java.security.Principal principal, Model model) {
        OfficialEmployee emp = officialEmployeeRepository.findByCustomEmployeeId(principal.getName()).orElse(null);
        if (emp == null) {
            return "redirect:/employee-login";
        }
        model.addAttribute("employee", emp);
        model.addAttribute("logs", service.getEmployeeAttendanceHistory(emp.getId().intValue()));
        model.addAttribute("statSummary", service.buildEmployeeAttendanceStats(emp));
        var todayLog = service.findAttendanceLogForToday(emp.getId().intValue());
        com.example.employee.model.AttendanceLog tlog = todayLog.orElse(null);
        model.addAttribute("todayLog", tlog);
        boolean clockedIn = tlog != null
            && tlog.getTimeIn() != null
            && tlog.getTimeOut() == null;
        model.addAttribute("isClockedIn", clockedIn);
        return "employee-attendance";
    }

    @PostMapping("/employee/attendance/tap")
    public String employeeAttendanceTap(
            java.security.Principal principal,
            @RequestParam String action,
            org.springframework.web.servlet.mvc.support.RedirectAttributes ra) {
        String msg = service.processBiometrics(principal.getName(), action);
        if (msg != null && msg.startsWith("Error")) {
            ra.addFlashAttribute("errorMessage", msg);
        } else {
            ra.addFlashAttribute("successMessage", msg);
        }
        return "redirect:/employee/attendance";
    }

    @GetMapping("/hr/attendance/download")
    public void downloadDTR(@RequestParam int id, @RequestParam(required = false) String cutoffPeriod, HttpServletResponse response) {
        response.setContentType("application/pdf");
        response.setHeader("Content-Disposition", "attachment; filename=DTR_EAC-" + String.format("%05d", id) + ".pdf");
        service.exportDTRToPDF(id, cutoffPeriod, response);
    }

    @GetMapping("/hr/biometrics")
    public String viewBiometricsDashboard(
            Model model,
            @RequestParam(required = false) String cutoffPeriod,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate customFrom,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate customTo,
            @RequestParam(required = false) String applyMode,
            @RequestParam(required = false) String search) {
        java.util.Map<String, String> cutoffOptions = service.getCutoffOptionsWithPresets(6);

        String key = service.effectivePayrollCutoffKey(cutoffPeriod, customFrom, customTo, applyMode);
        if (key == null || key.isEmpty()) {
            key = cutoffOptions.keySet().iterator().next();
        }

        model.addAttribute("employees", service.getPayrollData(key, search));
        model.addAttribute("searchKeyword", search);
        model.addAttribute("lastFile", service.getLastUploadedFileName());
        model.addAttribute("lastTime", service.getLastUploadTime());

        model.addAttribute("cutoffOptions", cutoffOptions);
        model.addAttribute("selectedCutoff", key);
        enrichCutoffModel(model, key);
        return "hr-biometrics";
    }

    @PostMapping("/hr/biometrics/upload")
    public String uploadBiometricsCsv(@RequestParam("file") org.springframework.web.multipart.MultipartFile file, org.springframework.web.servlet.mvc.support.RedirectAttributes redirectAttributes) {
        try {
            if (file.isEmpty()) {
                redirectAttributes.addFlashAttribute("errorMessage", "Please select a CSV file to upload.");
                return "redirect:/hr/biometrics"; 
            }
            AdmissionService.BiometricsImportResult result = service.processBiometricsCsv(file);
            redirectAttributes.addFlashAttribute("successMessage",
                    String.format("TCMS import: %d row(s) saved. Rows read from file: %d. Skipped — too few columns: %d, unknown EAC/employee: %d, date/time/parse: %d.",
                            result.imported(), result.dataRows(), result.skippedTooFewColumns(),
                            result.skippedUnknownEmployee(), result.skippedParseError()));
        } catch (Exception e) {
            log.error("Biometrics CSV import failed", e);
            redirectAttributes.addFlashAttribute("errorMessage",
                    "Import failed. Use UTF-8 CSV with comma or semicolon, quoted fields allowed. See help text on this page. Server log has details.");
        }
        return "redirect:/hr/biometrics"; 
    }

    @GetMapping("/hr/employees/biometrics")
    public String viewSingleEmployeeBiometrics(
            @RequestParam("id") Long id,
            @RequestParam(required = false) String cutoffPeriod,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate customFrom,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate customTo,
            @RequestParam(required = false) String applyMode,
            Model model) {
        OfficialEmployee emp = officialEmployeeRepository.findById(id).orElse(null);
        if (emp != null) {
            String key = service.effectivePayrollCutoffKey(cutoffPeriod, customFrom, customTo, applyMode);
            if (key == null || key.isBlank()) {
                key = service.getCurrentPayrollCutoffKey();
            }
            model.addAttribute("employee", emp);

            java.util.List<com.example.employee.model.AttendanceLog> logs = service.getEmployeeAttendanceHistoryByCutoff(id.intValue(), key);
            model.addAttribute("logs", logs);
            
            double totalRaw = 0;
            double totalReg = 0;
            double totalOt = 0;
            
            for (com.example.employee.model.AttendanceLog dayLog : logs) {
                double worked = (dayLog.getTotalHours() != null) ? dayLog.getTotalHours() : 0.0; 
                totalRaw += worked;
                
                double dayReg = Math.min(worked, 8.0);
                totalReg += dayReg;
                
                int approvedOt = dayLog.getOvertimeHours() != null ? dayLog.getOvertimeHours() : 0;
                totalOt += approvedOt;
            }
            
            model.addAttribute("totalRawText", formatHoursMins(totalRaw));
            model.addAttribute("totalRegText", formatHoursMins(totalReg));
            model.addAttribute("totalOtText", formatHoursMins(totalOt));

            model.addAttribute("cutoffOptions", service.getCutoffOptionsWithPresets(6));
            model.addAttribute("selectedCutoff", key);
            enrichCutoffModel(model, key);
        }
        return "hr-employee-biometrics";
    }

    private String formatHoursMins(double totalHours) {
        int h = (int) totalHours;
        int m = (int) Math.round((totalHours - h) * 60);
        if (m >= 60) {
            h += 1;
            m = 0;
        }
        return h + " hrs " + m + " mins";
    }

    @GetMapping("/hr/employees/delete")
    public String deleteEmployee(@RequestParam("id") Long id, org.springframework.web.servlet.mvc.support.RedirectAttributes redirectAttributes) {
        service.deleteEmployee(id);
        redirectAttributes.addFlashAttribute("successMessage", "Employee successfully deleted from the database.");
        return "redirect:/hr/employees";
    }

    @PostMapping("/hr/holidays/add")
    public String addHoliday(
            @RequestParam @org.springframework.format.annotation.DateTimeFormat(iso = org.springframework.format.annotation.DateTimeFormat.ISO.DATE) java.time.LocalDate date,
            @RequestParam String name,
            @RequestParam String type,
            org.springframework.web.servlet.mvc.support.RedirectAttributes redirectAttributes) {
        try {
            com.example.employee.model.Holiday newHoliday = new com.example.employee.model.Holiday(date, name, type);
            holidayRepository.save(newHoliday);
            redirectAttributes.addFlashAttribute("successMessage", "Holiday added successfully!");
        } catch (Exception e) {
            redirectAttributes.addFlashAttribute("errorMessage", "Error adding holiday: " + e.getMessage());
        }
        return "redirect:/hr/holidays";
    }

    @PostMapping("/hr/holidays/delete")
    public String deleteHoliday(@RequestParam int id, org.springframework.web.servlet.mvc.support.RedirectAttributes redirectAttributes) {
        try {
            holidayRepository.deleteById(id);
            redirectAttributes.addFlashAttribute("successMessage", "Holiday successfully removed.");
        } catch (Exception e) {
            redirectAttributes.addFlashAttribute("errorMessage", "Error deleting holiday.");
        }
        return "redirect:/hr/holidays";
    }
    
    @GetMapping("/hr/attendance-board")
    public String showAttendanceBoard(Model model, @RequestParam(required = false) String search) {
        List<OfficialEmployee> list = new ArrayList<>();
        officialEmployeeRepository.findByStatus("Active").forEach(list::add);
        if (ListSearchUtil.isActiveKeyword(search)) {
            list = list.stream().filter(e -> ListSearchUtil.matchesEmployee(e, search)).toList();
        }
        model.addAttribute("employees", list);
        model.addAttribute("searchKeyword", search);
        return "hr-attendance-board";
    }

    @GetMapping("/employee/dtr")
    public String employeeDtr(
            @RequestParam(required = false) Integer year,
            @RequestParam(required = false) Integer month,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate dtrPdfFrom,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate dtrPdfTo,
            @RequestParam(required = false) String dtrPdfApply,
            java.security.Principal principal,
            Model model) {
        OfficialEmployee emp = officialEmployeeRepository.findByCustomEmployeeId(principal.getName()).orElse(null);
        if (emp == null) {
            return "redirect:/employee-login";
        }
        java.time.YearMonth ym;
        if (year != null && month != null && month >= 1 && month <= 12) {
            ym = java.time.YearMonth.of(year, month);
        } else {
            ym = java.time.YearMonth.now();
        }
        java.time.LocalDate from = ym.atDay(1);
        java.time.LocalDate to = ym.atEndOfMonth();
        model.addAttribute("employee", emp);
        model.addAttribute("dtrRows", service.buildDtrRows(emp.getId().intValue(), from, to));
        model.addAttribute("dtrYear", ym.getYear());
        model.addAttribute("dtrMonth", ym.getMonthValue());
        model.addAttribute("dtrPeriodLabel", ym.getMonth().toString() + " " + ym.getYear());
        model.addAttribute("employmentDisplay", emp.getEmploymentType() != null && !emp.getEmploymentType().isBlank()
            ? emp.getEmploymentType() : "—");
        String pdfCutoff = service.effectivePayrollCutoffKey(null, dtrPdfFrom, dtrPdfTo, dtrPdfApply);
        if (pdfCutoff == null || pdfCutoff.isBlank()) {
            pdfCutoff = service.getCurrentPayrollCutoffKey();
        }
        model.addAttribute("currentCutoff", pdfCutoff);
        PayrollPeriodUtil.PayrollPeriod pdfP = PayrollPeriodUtil.resolve(pdfCutoff);
        model.addAttribute("dtrWindowStart", pdfP.start().toString());
        model.addAttribute("dtrWindowEnd", pdfP.end().toString());
        return "employee-dtr";
    }

    @GetMapping("/employee/dtr/download")
    public void employeeDtrDownload(
            @RequestParam String cutoffPeriod,
            java.security.Principal principal,
            HttpServletResponse response) {
        OfficialEmployee emp = officialEmployeeRepository.findByCustomEmployeeId(principal.getName()).orElse(null);
        if (emp == null) {
            return;
        }
        response.setContentType("application/pdf");
        String safe = emp.getCustomEmployeeId() != null ? emp.getCustomEmployeeId().replaceAll("[^0-9A-Za-z-]", "_") : String.valueOf(emp.getId());
        response.setHeader("Content-Disposition", "attachment; filename=DTR_" + safe + ".pdf");
        service.exportDTRToPDF(emp.getId().intValue(), cutoffPeriod, response);
    }

    @GetMapping("/employee/payslip")
    public String employeePayslip(
            @RequestParam(required = false) String cutoffPeriod,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate customFrom,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate customTo,
            @RequestParam(required = false) String applyMode,
            java.security.Principal principal,
            Model model) {
        OfficialEmployee emp = officialEmployeeRepository.findByCustomEmployeeId(principal.getName()).orElse(null);
        if (emp == null) {
            return "redirect:/employee-login";
        }
        String key = service.effectivePayrollCutoffKey(cutoffPeriod, customFrom, customTo, applyMode);
        if (key == null || key.isBlank()) {
            key = service.getCurrentPayrollCutoffKey();
        }
        model.addAttribute("cutoffOptions", service.getCutoffOptionsWithPresets(6));
        model.addAttribute("selectedCutoff", key);
        enrichCutoffModel(model, key);
        java.util.List<OfficialEmployee> payroll = service.getPayrollData(key, null);
        OfficialEmployee row = payroll.stream()
            .filter(e -> e.getId().equals(emp.getId()))
            .findFirst()
            .orElse(emp);
        model.addAttribute("employee", row);
        return "employee-payslip";
    }

    @GetMapping("/employee/payslip/download")
    public void employeePayslipDownload(
            @RequestParam String cutoffPeriod,
            java.security.Principal principal,
            HttpServletResponse response) {
        OfficialEmployee emp = officialEmployeeRepository.findByCustomEmployeeId(principal.getName()).orElse(null);
        if (emp == null) {
            return;
        }
        response.setContentType("application/pdf");
        String safe = emp.getCustomEmployeeId() != null ? emp.getCustomEmployeeId().replaceAll("[^0-9A-Za-z-]", "_") : String.valueOf(emp.getId());
        response.setHeader("Content-Disposition", "attachment; filename=Payslip_" + safe + ".pdf");
        service.exportPayslipToPDF(emp.getId().intValue(), cutoffPeriod, response);
    }

    @PostMapping("/employee/profile/photo")
    public String uploadProfilePhoto(
            @RequestParam("photo") org.springframework.web.multipart.MultipartFile photo,
            java.security.Principal principal,
            org.springframework.web.servlet.mvc.support.RedirectAttributes ra) {
        OfficialEmployee emp = officialEmployeeRepository.findByCustomEmployeeId(principal.getName()).orElse(null);
        if (emp == null) {
            return "redirect:/employee-login";
        }
        try {
            service.saveEmployeeProfilePhoto(emp, photo);
            ra.addFlashAttribute("successMessage", "Profile photo updated.");
        } catch (IllegalArgumentException e) {
            ra.addFlashAttribute("errorMessage", e.getMessage());
        } catch (Exception e) {
            ra.addFlashAttribute("errorMessage", "Could not upload photo. Try a smaller image (JPEG/PNG).");
        }
        return "redirect:/employee/my-record";
    }

    @GetMapping("/api/attendance/logs")
    @org.springframework.web.bind.annotation.ResponseBody
    public java.util.List<com.example.employee.model.AttendanceLog> getAttendanceLogsApi(@RequestParam int employeeId) {
        return service.getEmployeeAttendanceHistory(employeeId);
    }

    @GetMapping("/api/holidays")
    @org.springframework.web.bind.annotation.ResponseBody
    public java.util.List<com.example.employee.model.Holiday> getHolidaysApi() {
        return holidayRepository.findAll();
    }

    @GetMapping("/hr/audit-log")
    public String hrAuditLog(Model model) {
        model.addAttribute("events", service.listRecentHrAuditEvents(200));
        return "hr-audit-log";
    }

    @GetMapping("/hr/payroll-periods")
    public String hrPayrollPeriods(Model model) {
        model.addAttribute("presets", service.listAllPayrollPeriodPresets());
        return "hr-payroll-periods";
    }

    @PostMapping("/hr/payroll-periods/add")
    public String addPayrollPeriod(
            @RequestParam String name,
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate rangeFrom,
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate rangeTo) {
        String key = PayrollPeriodUtil.toCutoffKey(rangeFrom, rangeTo);
        service.savePayrollPeriodPreset(name, key);
        return "redirect:/hr/payroll-periods";
    }

    @PostMapping("/hr/payroll-periods/delete")
    public String deletePayrollPeriod(@RequestParam long id) {
        service.deletePayrollPeriodPreset(id);
        return "redirect:/hr/payroll-periods";
    }

    /** Populates custom date pickers and human-readable range for payroll cutoff UIs. */
    private void enrichCutoffModel(Model model, String selectedCutoffKey) {
        if (selectedCutoffKey == null || selectedCutoffKey.isBlank()) {
            selectedCutoffKey = service.getCurrentPayrollCutoffKey();
        }
        PayrollPeriodUtil.PayrollPeriod p = PayrollPeriodUtil.resolve(selectedCutoffKey);
        model.addAttribute("customRangeFrom", p.start().toString());
        model.addAttribute("customRangeTo", p.end().toString());
        model.addAttribute("cutoffRangeLabel", PayrollPeriodUtil.formatEnglishRangeLabel(p));
        model.addAttribute("isCustomRangeCutoff", Boolean.valueOf(PayrollPeriodUtil.isCustomRangeKey(selectedCutoffKey)));
    }
}