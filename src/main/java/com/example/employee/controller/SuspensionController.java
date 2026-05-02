package com.example.employee.controller;

import com.example.employee.model.OfficialEmployee;
import com.example.employee.model.Suspension;
import com.example.employee.repository.OfficialEmployeeRepository;
import com.example.employee.repository.SuspensionRepository;
import com.example.employee.util.ListSearchUtil;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

import java.util.Comparator;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.StreamSupport;
import java.time.LocalDate;
import java.time.LocalTime;

@Controller
public class SuspensionController {

    @Autowired
    private SuspensionRepository suspensionRepository;
    @Autowired
    private OfficialEmployeeRepository officialEmployeeRepository;

    // 1. View the Suspensions Board
    @GetMapping("/hr/suspensions")
    public String viewSuspensions(Model model, @RequestParam(required = false) String search) {
        List<Suspension> list = suspensionRepository.findAllByOrderByDateDesc();
        if (ListSearchUtil.isActiveKeyword(search)) {
            list = list.stream().filter(s -> ListSearchUtil.matchesSuspension(s, search)).toList();
        }
        model.addAttribute("suspensions", list);
        model.addAttribute("searchKeyword", search);
        model.addAttribute("employees", activeEmployeesSorted());
        model.addAttribute("employeeLabels", buildEmployeeLabels());
        return "hr-suspensions";
    }

    // 2. Add a New Suspension
    @PostMapping("/hr/suspensions/add")
    public String addSuspension(
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate date,
            @RequestParam String reason,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.TIME) LocalTime startTime,
            @RequestParam(required = false, name = "appliesTo") String appliesTo,
            RedirectAttributes redirectAttributes) {

        Long employeeId = null;
        if (appliesTo != null && !appliesTo.isBlank() && !"ALL".equalsIgnoreCase(appliesTo.trim())) {
            try {
                employeeId = Long.parseLong(appliesTo.trim());
            } catch (NumberFormatException ex) {
                redirectAttributes.addFlashAttribute("errorMessage", "Invalid employee selection.");
                return "redirect:/hr/suspensions";
            }
            if (officialEmployeeRepository.findById(employeeId).isEmpty()) {
                redirectAttributes.addFlashAttribute("errorMessage", "Selected employee was not found.");
                return "redirect:/hr/suspensions";
            }
        }

        try {
            Suspension suspension = new Suspension(date, reason, startTime, employeeId);
            suspensionRepository.save(suspension);
            redirectAttributes.addFlashAttribute("successMessage", "Suspension recorded successfully!");
        } catch (Exception e) {
            redirectAttributes.addFlashAttribute("errorMessage", "Error saving suspension: " + e.getMessage());
        }
        return "redirect:/hr/suspensions";
    }

    // 3. Delete a Suspension
    @PostMapping("/hr/suspensions/delete")
    public String deleteSuspension(@RequestParam Long id, RedirectAttributes redirectAttributes) {
        try {
            suspensionRepository.deleteById(id);
            redirectAttributes.addFlashAttribute("successMessage", "Suspension successfully removed.");
        } catch (Exception e) {
            redirectAttributes.addFlashAttribute("errorMessage", "Error deleting suspension.");
        }
        return "redirect:/hr/suspensions";
    }

    private List<OfficialEmployee> activeEmployeesSorted() {
        return StreamSupport.stream(officialEmployeeRepository.findByStatus("Active").spliterator(), false)
            .sorted(Comparator.comparing(OfficialEmployee::getLastName, Comparator.nullsLast(String::compareToIgnoreCase))
                .thenComparing(OfficialEmployee::getFirstName, Comparator.nullsLast(String::compareToIgnoreCase)))
            .toList();
    }

    private Map<Long, String> buildEmployeeLabels() {
        Map<Long, String> map = new HashMap<>();
        for (OfficialEmployee e : activeEmployeesSorted()) {
            if (e.getId() == null) {
                continue;
            }
            String cid = e.getCustomEmployeeId() != null ? e.getCustomEmployeeId() : String.valueOf(e.getId());
            String name = ((e.getFirstName() != null ? e.getFirstName() : "") + " "
                + (e.getLastName() != null ? e.getLastName() : "")).trim();
            map.put(e.getId(), (cid + " — " + name).trim());
        }
        return map;
    }
}