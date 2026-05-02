package com.example.employee.controller;

import com.example.employee.model.OfficialEmployee;
import com.example.employee.repository.OfficialEmployeeRepository;
import com.example.employee.service.AdmissionService;
import com.example.employee.service.OvertimeRequestService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.multipart.MultipartFile;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;
import org.springframework.web.util.UriComponentsBuilder;

import java.security.Principal;
import java.time.LocalDate;
import java.time.YearMonth;
import java.util.ArrayList;
import java.util.List;

@Controller
public class OvertimeRequestController {

    @Autowired
    private OvertimeRequestService overtimeRequestService;

    @Autowired
    private AdmissionService admissionService;

    @Autowired
    private OfficialEmployeeRepository officialEmployeeRepository;

    @GetMapping("/hr/overtime")
    public String hrOvertime(
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate dateFrom,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate dateTo,
            Model model) {
        LocalDate today = LocalDate.now();
        if (dateFrom == null) {
            dateFrom = today.withDayOfMonth(1);
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
        model.addAttribute("otRows", overtimeRequestService.buildAdminList(dateFrom, dateTo));
        model.addAttribute("otMonthStats", overtimeRequestService.getStatsForMonth(YearMonth.from(today)));
        model.addAttribute("allDepartments", admissionService.getDistinctDepartments());
        model.addAttribute("allBranches", admissionService.getDistinctCampusCodes());
        List<String> des = new ArrayList<>();
        officialEmployeeRepository.findByStatus("Active").forEach(e -> {
            if (e.getPosition() != null && !e.getPosition().isBlank()) {
                des.add(e.getPosition().trim());
            }
        });
        model.addAttribute("allDesignations", des.stream().distinct().sorted(String.CASE_INSENSITIVE_ORDER).toList());
        List<OfficialEmployee> emps = new ArrayList<>();
        officialEmployeeRepository.findByStatus("Active").forEach(emps::add);
        model.addAttribute("activeEmployees", emps);
        return "hr-overtime";
    }

    @GetMapping("/hr/overtime-approvals")
    public String redirectLegacyOvertime() {
        return "redirect:/hr/overtime";
    }

    @PostMapping("/hr/overtime/eac/approve")
    public String approveEac(
            @RequestParam long eacRequestId,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate dateFrom,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate dateTo,
            Principal principal,
            RedirectAttributes ra) {
        try {
            String by = principal != null ? principal.getName() : "HR";
            overtimeRequestService.approveEacRequest(eacRequestId, by);
            ra.addFlashAttribute("successMessage", "EAC overtime request approved.");
        } catch (Exception e) {
            ra.addFlashAttribute("errorMessage", e.getMessage() != null ? e.getMessage() : "Could not approve.");
        }
        return buildRedirectOvertime(dateFrom, dateTo);
    }

    @PostMapping("/hr/overtime/eac/reject")
    public String rejectEac(
            @RequestParam long eacRequestId,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate dateFrom,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate dateTo,
            Principal principal,
            RedirectAttributes ra) {
        try {
            String by = principal != null ? principal.getName() : "HR";
            overtimeRequestService.rejectEacRequest(eacRequestId, by);
            ra.addFlashAttribute("successMessage", "EAC overtime request rejected.");
        } catch (Exception e) {
            ra.addFlashAttribute("errorMessage", e.getMessage() != null ? e.getMessage() : "Could not reject.");
        }
        return buildRedirectOvertime(dateFrom, dateTo);
    }

    @PostMapping("/hr/overtime/manual")
    public String adminManualOvertime(
            @RequestParam long employeeId,
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate workDate,
            @RequestParam int overtimeHours,
            @RequestParam(defaultValue = "REGULAR") String otType,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate offsetDate,
            @RequestParam(required = false) String notes,
            @RequestParam(value = "attachment", required = false) MultipartFile attachment,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate dateFrom,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate dateTo,
            RedirectAttributes ra) {
        try {
            overtimeRequestService.submitEacRequest(
                employeeId, workDate, overtimeHours, otType, offsetDate, notes, "MANUAL", attachment);
            ra.addFlashAttribute("successMessage", "Overtime entry saved (pending).");
        } catch (Exception e) {
            ra.addFlashAttribute("errorMessage", e.getMessage() != null ? e.getMessage() : "Could not save entry.");
        }
        return buildRedirectOvertime(dateFrom, dateTo);
    }

    private String buildRedirectOvertime(LocalDate dateFrom, LocalDate dateTo) {
        UriComponentsBuilder b = UriComponentsBuilder.fromPath("/hr/overtime");
        if (dateFrom != null) {
            b.queryParam("dateFrom", dateFrom.toString());
        }
        if (dateTo != null) {
            b.queryParam("dateTo", dateTo.toString());
        }
        return "redirect:" + b.build().toUriString();
    }

    @GetMapping("/employee/overtime")
    public String employeeOvertime(
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate dateFrom,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate dateTo,
            Principal principal,
            Model model) {
        OfficialEmployee emp = officialEmployeeRepository.findByCustomEmployeeId(principal.getName()).orElse(null);
        if (emp == null) {
            return "redirect:/employee-login";
        }
        LocalDate today = LocalDate.now();
        if (dateFrom == null) {
            dateFrom = today.withDayOfMonth(1);
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
        model.addAttribute("otRows", overtimeRequestService.buildEmployeeList(emp.getId(), dateFrom, dateTo));
        model.addAttribute("otMonthStats", overtimeRequestService.getStatsForEmployeeMonth(emp.getId(), YearMonth.from(today)));
        return "employee-overtime";
    }

    @PostMapping("/employee/overtime/submit")
    public String employeeSubmitOvertime(
            Principal principal,
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate workDate,
            @RequestParam int overtimeHours,
            @RequestParam(defaultValue = "REGULAR") String otType,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate offsetDate,
            @RequestParam(required = false) String notes,
            @RequestParam(defaultValue = "EMPLOYEE") String requestMode,
            @RequestParam(value = "attachment", required = false) MultipartFile attachment,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate dateFrom,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate dateTo,
            RedirectAttributes ra) {
        OfficialEmployee emp = officialEmployeeRepository.findByCustomEmployeeId(principal.getName()).orElse(null);
        if (emp == null) {
            return "redirect:/employee-login";
        }
        String src = "CLOCK_IN".equals(requestMode) || "CLOCK_OUT".equals(requestMode) ? requestMode : "EMPLOYEE";
        try {
            overtimeRequestService.submitEacRequest(
                emp.getId(), workDate, overtimeHours, otType, offsetDate, notes, src, attachment);
            ra.addFlashAttribute("successMessage", "Overtime request submitted for HR review.");
        } catch (Exception e) {
            ra.addFlashAttribute("errorMessage", e.getMessage() != null ? e.getMessage() : "Could not submit.");
        }
        if (dateFrom == null) {
            dateFrom = LocalDate.now().withDayOfMonth(1);
        }
        if (dateTo == null) {
            dateTo = LocalDate.now();
        }
        return "redirect:" + UriComponentsBuilder.fromPath("/employee/overtime")
            .queryParam("dateFrom", dateFrom.toString())
            .queryParam("dateTo", dateTo.toString())
            .build()
            .toUriString();
    }
}
