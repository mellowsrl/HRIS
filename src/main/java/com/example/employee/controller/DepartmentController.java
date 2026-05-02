package com.example.employee.controller;

import com.example.employee.service.DepartmentService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

@Controller
public class DepartmentController {

    @Autowired
    private DepartmentService departmentService;

    @GetMapping("/hr/departments")
    public String list(Model model) {
        model.addAttribute("departments", departmentService.getDepartmentListItems());
        model.addAttribute("branchChoices", DepartmentService.BRANCH_CHOICES);
        return "hr-departments";
    }

    @PostMapping("/hr/departments/add")
    public String add(
        @RequestParam String name,
        @RequestParam String code,
        @RequestParam(required = false) String headName,
        @RequestParam String branch,
        @RequestParam(defaultValue = "Active") String status,
        RedirectAttributes ra) {
        try {
            departmentService.validateAdd(code);
            departmentService.add(name, code, headName, branch, status);
            ra.addFlashAttribute("successMessage", "Department added successfully.");
        } catch (Exception e) {
            ra.addFlashAttribute("errorMessage", e.getMessage() != null ? e.getMessage() : "Could not add department.");
        }
        return "redirect:/hr/departments";
    }

    @PostMapping("/hr/departments/edit")
    public String edit(
        @RequestParam long id,
        @RequestParam String name,
        @RequestParam String code,
        @RequestParam(required = false) String headName,
        @RequestParam String branch,
        @RequestParam(defaultValue = "Active") String status,
        RedirectAttributes ra) {
        try {
            departmentService.validateUpdate(id, code);
            departmentService.update(id, name, code, headName, branch, status);
            ra.addFlashAttribute("successMessage", "Department updated successfully.");
        } catch (Exception e) {
            ra.addFlashAttribute("errorMessage", e.getMessage() != null ? e.getMessage() : "Could not update department.");
        }
        return "redirect:/hr/departments";
    }

    @GetMapping("/hr/departments/delete")
    public String delete(@RequestParam long id, RedirectAttributes ra) {
        try {
            departmentService.deleteById(id);
            ra.addFlashAttribute("successMessage", "Department removed from the list.");
        } catch (Exception e) {
            ra.addFlashAttribute("errorMessage", e.getMessage() != null ? e.getMessage() : "Could not remove department.");
        }
        return "redirect:/hr/departments";
    }
}
