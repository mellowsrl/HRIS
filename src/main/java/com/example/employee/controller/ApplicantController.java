package com.example.employee.controller;

import com.example.employee.model.Applicant;
import com.example.employee.model.AppUser;
import com.example.employee.model.JobPosting;
import com.example.employee.repository.ApplicantRepository;
import com.example.employee.repository.AppUserRepository;
import com.example.employee.repository.JobPostingRepository;
import com.example.employee.service.DepartmentCodeService;
import com.example.employee.util.ListSearchUtil;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;

import java.util.List;
import java.util.Locale;
import java.util.Map;

@Controller
@RequestMapping("/applicant")
public class ApplicantController {

    @Autowired
    private JobPostingRepository jobPostingRepository;

    @Autowired
    private ApplicantRepository applicantRepository;

    @Autowired
    private AppUserRepository appUserRepository;

    @Autowired
    private DepartmentCodeService departmentCodeService;

    @GetMapping("/dashboard")
    public String viewDashboard(@AuthenticationPrincipal UserDetails userDetails, Model model, @RequestParam(required = false) String search) {
        // 1. Fetch all OPEN jobs for the job board
        List<JobPosting> openJobs = jobPostingRepository.findByStatus("OPEN");
        if (ListSearchUtil.isActiveKeyword(search)) {
            Map<String, String> deptNameByCode = departmentCodeService.codeToNameMap();
            openJobs = openJobs.stream()
                .filter(j -> {
                    String dep = j.getDepartment();
                    String extra = (dep == null) ? null : deptNameByCode.get(dep.toUpperCase(Locale.ROOT));
                    return ListSearchUtil.matchesJobPostingWithDeptAlias(j, search, extra);
                })
                .toList();
        }
        model.addAttribute("jobs", openJobs);
        
        // 2. Fetch the logged-in user's specific applications for the Status Tracker
        AppUser currentUser = appUserRepository.findByUsername(userDetails.getUsername()).orElseThrow();
        List<Applicant> myApplications = applicantRepository.findByUser(currentUser);
        if (ListSearchUtil.isActiveKeyword(search)) {
            myApplications = myApplications.stream().filter(a -> ListSearchUtil.matchesMyApplication(a, search)).toList();
        }
        model.addAttribute("myApplications", myApplications);
        model.addAttribute("searchKeyword", search);
        
        // 3. Return the flat file name (no folders!)
        return "applicant-dashboard"; 
    }
}