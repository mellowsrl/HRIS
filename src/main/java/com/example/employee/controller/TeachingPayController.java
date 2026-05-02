package com.example.employee.controller;

import com.example.employee.model.TeachingPay;
import com.example.employee.model.OfficialEmployee;
import com.example.employee.service.TeachingPayService;
import com.example.employee.repository.OfficialEmployeeRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.propertyeditors.CustomNumberEditor;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.WebDataBinder;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.InitBinder;
import org.springframework.web.bind.annotation.ModelAttribute;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

import java.util.List;
import java.util.stream.StreamSupport;

@Controller
public class TeachingPayController {

    @Autowired
    private TeachingPayService teachingPayService;
    @Autowired
    private OfficialEmployeeRepository officialEmployeeRepository;

    @InitBinder
    public void numberBinder(WebDataBinder binder) {
        binder.registerCustomEditor(Double.class, new CustomNumberEditor(Double.class, true));
    }

    @GetMapping("/hr/teaching-pay")
    public String list(
            @RequestParam(required = false) Long employeeId,
            Model model) {
        model.addAttribute("rows", teachingPayService.listAll(employeeId));
        model.addAttribute("employeeLabels", teachingPayService.buildEmployeeIdToLabel());
        model.addAttribute("filterEmployeeId", employeeId);
        return "hr-teaching-pay";
    }

    @GetMapping("/hr/teaching-pay/new")
    public String newForm(Model model) {
        TeachingPay t = new TeachingPay();
        model.addAttribute("teachingPay", t);
        model.addAttribute("isEdit", false);
        addEmployeeDropdown(model);
        return "hr-teaching-pay-form";
    }

    @GetMapping("/hr/teaching-pay/edit")
    public String editForm(@RequestParam long id, Model model) {
        TeachingPay t = teachingPayService.findById(id)
            .orElseThrow(() -> new IllegalArgumentException("Not found"));
        model.addAttribute("teachingPay", t);
        model.addAttribute("isEdit", true);
        addEmployeeDropdown(model);
        return "hr-teaching-pay-form";
    }

    @PostMapping("/hr/teaching-pay")
    public String create(@ModelAttribute TeachingPay teachingPay, RedirectAttributes ra) {
        try {
            teachingPayService.save(teachingPay);
            ra.addFlashAttribute("successMessage", "Teaching pay record created.");
        } catch (Exception e) {
            ra.addFlashAttribute("errorMessage", e.getMessage() != null ? e.getMessage() : "Could not save.");
        }
        return "redirect:/hr/teaching-pay";
    }

    @PostMapping("/hr/teaching-pay/update")
    public String update(@ModelAttribute TeachingPay teachingPay, RedirectAttributes ra) {
        if (teachingPay.getId() == null) {
            ra.addFlashAttribute("errorMessage", "Invalid record to update.");
            return "redirect:/hr/teaching-pay";
        }
        try {
            teachingPayService.update(teachingPay.getId(), teachingPay);
            ra.addFlashAttribute("successMessage", "Teaching pay record updated.");
        } catch (Exception e) {
            ra.addFlashAttribute("errorMessage", e.getMessage() != null ? e.getMessage() : "Could not update.");
        }
        return "redirect:/hr/teaching-pay";
    }

    @PostMapping("/hr/teaching-pay/delete")
    public String delete(@RequestParam long id, RedirectAttributes ra) {
        try {
            teachingPayService.delete(id);
            ra.addFlashAttribute("successMessage", "Teaching pay record deleted.");
        } catch (Exception e) {
            ra.addFlashAttribute("errorMessage", e.getMessage() != null ? e.getMessage() : "Could not delete.");
        }
        return "redirect:/hr/teaching-pay";
    }

    private void addEmployeeDropdown(Model model) {
        List<OfficialEmployee> all = StreamSupport.stream(officialEmployeeRepository.findAll().spliterator(), false)
            .filter(e -> e.getStatus() == null || "Active".equalsIgnoreCase(e.getStatus().trim()))
            .toList();
        model.addAttribute("employees", all);
    }
}
