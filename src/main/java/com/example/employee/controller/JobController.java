package com.example.employee.controller;

import com.example.employee.model.EacDepartment;
import com.example.employee.model.JobPosting;
import com.example.employee.repository.EacDepartmentRepository;
import com.example.employee.repository.JobPostingRepository;
import com.example.employee.service.DepartmentCodeService;
import com.example.employee.util.ListSearchUtil;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.ModelAttribute;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestParam;

import java.time.LocalDate;
import java.util.Comparator;
import java.util.List;
import java.util.Locale;
import java.util.Map;

@Controller
public class JobController {

    @Autowired
    private JobPostingRepository jobPostingRepository;

    @Autowired
    private EacDepartmentRepository eacDepartmentRepository;

    @Autowired
    private DepartmentCodeService departmentCodeService;

    @GetMapping("/hr/jobs")
    public String viewJobPostings(
            Model model,
            @RequestParam(required = false) String search,
            @RequestParam(required = false, defaultValue = "all") String role,
            @RequestParam(required = false) String error) {

        Map<String, String> deptNameByCode = departmentCodeService.codeToNameMap();
        List<JobPosting> jobs = jobPostingRepository.findAll(Sort.by(Sort.Direction.DESC, "id"));

        if (ListSearchUtil.isActiveKeyword(search)) {
            jobs = jobs.stream()
                .filter(j -> {
                    String dep = j.getDepartment();
                    String extra = (dep == null) ? null : deptNameByCode.get(dep.toUpperCase(Locale.ROOT));
                    return ListSearchUtil.matchesJobPostingWithDeptAlias(j, search, extra);
                })
                .toList();
        }

        if ("faculty".equalsIgnoreCase(role)) {
            jobs = jobs.stream().filter(JobController::isFacultyType).toList();
        } else if ("nonfaculty".equalsIgnoreCase(role)) {
            jobs = jobs.stream().filter(JobController::isNonFacultyType).toList();
        }

        model.addAttribute("jobs", jobs);
        model.addAttribute("searchKeyword", search);
        model.addAttribute("roleFilter", role != null && !role.isEmpty() ? role : "all");
        model.addAttribute("notFound", "notfound".equals(error));
        return "hr-jobs";
    }

    @GetMapping("/hr/jobs/new")
    public String showCreateJobForm(Model model) {
        model.addAttribute("job", new JobPosting());
        addJobFormDepartments(model);
        model.addAttribute("formPageTitle", "HR — Create job");
        return "hr-job-form";
    }

    @GetMapping("/hr/jobs/{id}/edit")
    public String showEditJobForm(@PathVariable Integer id, Model model) {
        return jobPostingRepository.findById(id)
                .map(job -> {
                    model.addAttribute("job", job);
                    addJobFormDepartments(model);
                    model.addAttribute("formPageTitle", "HR — Edit job");
                    return "hr-job-form";
                })
                .orElse("redirect:/hr/jobs?error=notfound");
    }

    @PostMapping("/hr/jobs/save")
    public String saveJobPosting(@ModelAttribute("job") JobPosting job) {
        if (job.getStatus() == null || job.getStatus().isEmpty()) {
            job.setStatus("OPEN");
        }
        String dep = departmentCodeService.toCanonicalCode(job.getDepartment());
        job.setDepartment(dep);
        if (job.getId() == null) {
            job.setDatePosted(LocalDate.now());
            jobPostingRepository.save(job);
        } else {
            var opt = jobPostingRepository.findById(job.getId());
            if (opt.isEmpty()) {
                return "redirect:/hr/jobs?error=notfound";
            }
            JobPosting existing = opt.get();
            existing.setTitle(job.getTitle());
            existing.setDepartment(dep);
            existing.setDescription(job.getDescription());
            existing.setRequirements(job.getRequirements());
            existing.setEmploymentType(job.getEmploymentType());
            existing.setStatus(job.getStatus());
            jobPostingRepository.save(existing);
        }
        return "redirect:/hr/jobs?success=true";
    }

    private void addJobFormDepartments(Model model) {
        List<EacDepartment> eacDepartments = eacDepartmentRepository.findAll().stream()
            .sorted(Comparator.comparing(
                d -> d.getName() != null ? d.getName() : "", String.CASE_INSENSITIVE_ORDER))
            .toList();
        model.addAttribute("eacDepartments", eacDepartments);
    }

    private static boolean isFacultyType(JobPosting j) {
        String t = j.getEmploymentType();
        if (t == null) {
            return false;
        }
        return t.contains("Faculty") && !t.contains("Non-Faculty");
    }

    private static boolean isNonFacultyType(JobPosting j) {
        String t = j.getEmploymentType();
        return t != null && t.contains("Non-Faculty");
    }
}
