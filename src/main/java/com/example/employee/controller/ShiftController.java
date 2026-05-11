package com.example.employee.controller;

import com.example.employee.model.OfficialEmployee;
import com.example.employee.model.ShiftAssignment;
import com.example.employee.model.ShiftSchedule;
import com.example.employee.service.AdmissionService;
import com.example.employee.service.ShiftAssignmentService;
import com.example.employee.service.ShiftScheduleService;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.ModelAttribute;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

import java.security.Principal;
import java.time.DayOfWeek;
import java.time.LocalDate;
import java.time.LocalTime;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.HashSet;
import java.util.stream.Collectors;

@Controller
public class ShiftController {
    private final ShiftScheduleService shiftScheduleService;
    private final ShiftAssignmentService shiftAssignmentService;
    private final AdmissionService admissionService;

    public ShiftController(
        ShiftScheduleService shiftScheduleService,
        ShiftAssignmentService shiftAssignmentService,
        AdmissionService admissionService
    ) {
        this.shiftScheduleService = shiftScheduleService;
        this.shiftAssignmentService = shiftAssignmentService;
        this.admissionService = admissionService;
    }

    @GetMapping("/hr/shifts")
    public String shiftList(Model model) {
        model.addAttribute("shifts", shiftScheduleService.listAll());
        model.addAttribute("newShift", new ShiftSchedule());
        return "hr-shift-list";
    }

    @PostMapping("/hr/shifts/save")
    public String saveShift(
        @ModelAttribute("newShift") ShiftSchedule shift,
        @RequestParam(value = "applyStrictNoGraceOt60", required = false) Boolean applyStrictNoGraceOt60,
        Principal principal,
        RedirectAttributes ra
    ) {
        String username = principal != null ? principal.getName() : "system";
        if (Boolean.TRUE.equals(applyStrictNoGraceOt60)) {
            shift.setGraceMinutes(0);
            shift.setMinimumMinutesForOt(60);
        }
        if (shift.getId() == null) {
            shift.setCreatedBy(username);
        }
        shift.setUpdatedBy(username);
        shiftScheduleService.save(shift);
        ra.addFlashAttribute("okMessage", "Shift saved.");
        return "redirect:/hr/shifts";
    }

    @PostMapping("/hr/shifts/archive")
    public String archiveShift(@RequestParam("id") Long id, Principal principal, RedirectAttributes ra) {
        String username = principal != null ? principal.getName() : "system";
        shiftScheduleService.archive(id, username);
        ra.addFlashAttribute("okMessage", "Shift archived.");
        return "redirect:/hr/shifts";
    }

    @GetMapping("/hr/shift-management")
    public String shiftManagement(
        @RequestParam(value = "weekStart", required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate weekStart,
        @RequestParam(value = "branch", required = false) String branch,
        @RequestParam(value = "department", required = false) String department,
        @RequestParam(value = "designation", required = false) String designation,
        @RequestParam(value = "search", required = false) String search,
        Model model
    ) {
        LocalDate start = weekStart != null ? weekStart : LocalDate.now().with(DayOfWeek.MONDAY);
        LocalDate end = start.plusDays(6);
        List<LocalDate> weekDates = start.datesUntil(end.plusDays(1)).collect(Collectors.toList());

        List<ShiftAssignment> assignments = shiftAssignmentService.listByRange(start, end);
        Map<String, ShiftAssignment> assignmentMap = new LinkedHashMap<>();
        List<OfficialEmployee> allActiveEmployees = admissionService.getEmployees(null, null);
        Map<Long, OfficialEmployee> employeeById = allActiveEmployees.stream()
            .collect(Collectors.toMap(OfficialEmployee::getId, e -> e, (a, b) -> a, LinkedHashMap::new));
        Set<Long> visibleEmployeeIds = new HashSet<>();
        for (ShiftAssignment row : assignments) {
            OfficialEmployee emp = employeeById.get(row.getEmployee().getId());
            if (emp == null || !matchesFilters(emp, branch, department, designation, search)) {
                continue;
            }
            assignmentMap.put(row.getEmployee().getId() + "|" + row.getWorkDate(), row);
            visibleEmployeeIds.add(row.getEmployee().getId());
        }
        List<OfficialEmployee> visibleEmployees = allActiveEmployees.stream()
            .filter(e -> visibleEmployeeIds.contains(e.getId()))
            .sorted((a, b) -> (a.getLastName() + " " + a.getFirstName()).compareToIgnoreCase(b.getLastName() + " " + b.getFirstName()))
            .collect(Collectors.toList());

        model.addAttribute("employees", visibleEmployees);
        model.addAttribute("allEmployees", allActiveEmployees);
        model.addAttribute("shifts", shiftScheduleService.listActive());
        model.addAttribute("weekStart", start);
        model.addAttribute("weekEnd", end);
        model.addAttribute("weekDates", weekDates);
        model.addAttribute("assignmentMap", assignmentMap);
        model.addAttribute("selectedBranch", branch);
        model.addAttribute("selectedDepartment", department);
        model.addAttribute("selectedDesignation", designation);
        model.addAttribute("searchKeyword", search);
        List<String> allDesignations = allActiveEmployees.stream()
            .map(OfficialEmployee::getPosition)
            .filter(Objects::nonNull)
            .map(String::trim)
            .filter(v -> !v.isBlank())
            .distinct()
            .sorted()
            .collect(Collectors.toList());
        model.addAttribute("allBranches", admissionService.getDistinctCampusCodes());
        model.addAttribute("allDepartments", admissionService.getEacDepartmentsForFilter());
        model.addAttribute("allDesignations", allDesignations);
        return "hr-shift-management";
    }

    private boolean matchesFilters(OfficialEmployee e, String branch, String department, String designation, String search) {
        if (branch != null && !branch.isBlank() && !branch.equalsIgnoreCase(e.getCampusCode())) {
            return false;
        }
        if (department != null && !department.isBlank()) {
            String empDept = e.getDepartment() == null ? "" : e.getDepartment().trim();
            if (!department.equalsIgnoreCase(empDept)) {
                return false;
            }
        }
        if (designation != null && !designation.isBlank()) {
            String pos = e.getPosition() == null ? "" : e.getPosition().trim();
            if (!designation.equalsIgnoreCase(pos)) {
                return false;
            }
        }
        if (search != null && !search.isBlank()) {
            String q = search.trim().toLowerCase();
            String hay = ((e.getCustomEmployeeId() == null ? "" : e.getCustomEmployeeId()) + " "
                + (e.getFirstName() == null ? "" : e.getFirstName()) + " "
                + (e.getLastName() == null ? "" : e.getLastName()) + " "
                + (e.getDepartment() == null ? "" : e.getDepartment())).toLowerCase();
            return hay.contains(q);
        }
        return true;
    }

    @PostMapping("/hr/shift-management/assign")
    public String assignSingle(
        @RequestParam("employeeId") Long employeeId,
        @RequestParam("shiftId") Long shiftId,
        @RequestParam("workDate") @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate workDate,
        @RequestParam(value = "source", required = false) String source,
        @RequestParam(value = "notes", required = false) String notes,
        @RequestParam(value = "overrideTimeIn", required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.TIME) LocalTime overrideTimeIn,
        @RequestParam(value = "overrideTimeOut", required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.TIME) LocalTime overrideTimeOut,
        @RequestParam(value = "overrideBreakMinutes", required = false) Integer overrideBreakMinutes,
        @RequestParam(value = "overrideGraceMinutes", required = false) Integer overrideGraceMinutes,
        @RequestParam("weekStart") @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate weekStart,
        Principal principal
    ) {
        String username = principal != null ? principal.getName() : "system";
        shiftAssignmentService.assign(
            employeeId,
            shiftId,
            workDate,
            username,
            source,
            notes,
            overrideTimeIn,
            overrideTimeOut,
            overrideBreakMinutes,
            overrideGraceMinutes
        );
        return "redirect:/hr/shift-management?weekStart=" + weekStart;
    }
}

