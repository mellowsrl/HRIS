package com.example.employee.controller;

import com.example.employee.model.OfficialEmployee;
import com.example.employee.repository.OfficialEmployeeRepository;
import com.example.employee.service.AdmissionService;
import com.example.employee.service.OfficialBusinessService;
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
import java.time.LocalTime;
import java.time.YearMonth;
import java.util.ArrayList;
import java.util.List;

@Controller
public class OfficialBusinessRequestController {

    @Autowired
    private OfficialBusinessService officialBusinessService;

    @Autowired
    private AdmissionService admissionService;

    @Autowired
    private OfficialEmployeeRepository officialEmployeeRepository;

    @GetMapping("/hr/official-business")
    public String hrOfficialBusiness(
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
        model.addAttribute("obRows", officialBusinessService.buildAdminList(dateFrom, dateTo));
        model.addAttribute("obMonthStats", officialBusinessService.getStatsForMonth(YearMonth.from(today)));
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
        return "hr-official-business";
    }

    @PostMapping("/hr/official-business/approve")
    public String approve(
            @RequestParam long obRequestId,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate dateFrom,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate dateTo,
            Principal principal,
            RedirectAttributes ra) {
        try {
            String by = principal != null ? principal.getName() : "HR";
            officialBusinessService.approveRequest(obRequestId, by);
            ra.addFlashAttribute("successMessage", "Official Business request approved.");
        } catch (Exception e) {
            ra.addFlashAttribute("errorMessage", e.getMessage() != null ? e.getMessage() : "Could not approve.");
        }
        return buildRedirectOb(dateFrom, dateTo, "/hr/official-business");
    }

    @PostMapping("/hr/official-business/reject")
    public String reject(
            @RequestParam long obRequestId,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate dateFrom,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate dateTo,
            Principal principal,
            RedirectAttributes ra) {
        try {
            String by = principal != null ? principal.getName() : "HR";
            officialBusinessService.rejectRequest(obRequestId, by);
            ra.addFlashAttribute("successMessage", "Official Business request rejected.");
        } catch (Exception e) {
            ra.addFlashAttribute("errorMessage", e.getMessage() != null ? e.getMessage() : "Could not reject.");
        }
        return buildRedirectOb(dateFrom, dateTo, "/hr/official-business");
    }

    @PostMapping("/hr/official-business/manual")
    public String adminManual(
            @RequestParam long employeeId,
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate businessDate,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.TIME) LocalTime startTime,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.TIME) LocalTime endTime,
            @RequestParam int obHours,
            @RequestParam String purpose,
            @RequestParam(required = false) String notes,
            @RequestParam(value = "attachment", required = false) MultipartFile attachment,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate dateFrom,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate dateTo,
            RedirectAttributes ra) {
        try {
            officialBusinessService.submitRequest(
                employeeId, businessDate, startTime, endTime, obHours, purpose, notes, "MANUAL", attachment);
            ra.addFlashAttribute("successMessage", "OB entry saved (pending).");
        } catch (Exception e) {
            ra.addFlashAttribute("errorMessage", e.getMessage() != null ? e.getMessage() : "Could not save entry.");
        }
        return buildRedirectOb(dateFrom, dateTo, "/hr/official-business");
    }

    @GetMapping("/employee/official-business")
    public String employeePage(
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
        model.addAttribute("obRows", officialBusinessService.buildEmployeeList(emp.getId(), dateFrom, dateTo));
        model.addAttribute("obMonthStats", officialBusinessService.getStatsForEmployeeMonth(emp.getId(), YearMonth.from(today)));
        return "employee-official-business";
    }

    @PostMapping("/employee/official-business/submit")
    public String employeeSubmit(
            Principal principal,
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate businessDate,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.TIME) LocalTime startTime,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.TIME) LocalTime endTime,
            @RequestParam int obHours,
            @RequestParam String purpose,
            @RequestParam(required = false) String notes,
            @RequestParam(value = "attachment", required = false) MultipartFile attachment,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate dateFrom,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate dateTo,
            RedirectAttributes ra) {
        OfficialEmployee emp = officialEmployeeRepository.findByCustomEmployeeId(principal.getName()).orElse(null);
        if (emp == null) {
            return "redirect:/employee-login";
        }
        try {
            officialBusinessService.submitRequest(
                emp.getId(), businessDate, startTime, endTime, obHours, purpose, notes, "EMPLOYEE", attachment);
            ra.addFlashAttribute("successMessage", "Request submitted for HR review.");
        } catch (Exception e) {
            ra.addFlashAttribute("errorMessage", e.getMessage() != null ? e.getMessage() : "Could not submit.");
        }
        if (dateFrom == null) {
            dateFrom = LocalDate.now().withDayOfYear(1);
        }
        if (dateTo == null) {
            dateTo = LocalDate.now();
        }
        return buildRedirectOb(dateFrom, dateTo, "/employee/official-business");
    }

    private String buildRedirectOb(LocalDate dateFrom, LocalDate dateTo, String path) {
        UriComponentsBuilder b = UriComponentsBuilder.fromPath(path);
        if (dateFrom != null) {
            b.queryParam("dateFrom", dateFrom.toString());
        }
        if (dateTo != null) {
            b.queryParam("dateTo", dateTo.toString());
        }
        return "redirect:" + b.build().toUriString();
    }
}
