package com.example.employee.controller;

import com.example.employee.model.AppUser;
import com.example.employee.repository.AppUserRepository;
import com.example.employee.service.EmailService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.ModelAttribute;
import org.springframework.web.bind.annotation.PostMapping;

import java.util.regex.Pattern;

@Controller
public class AuthController {

    private static final Pattern EMAIL_PATTERN = Pattern.compile(
            "^[A-Za-z0-9+._%+-]+@[A-Za-z0-9.-]+\\.[A-Za-z]{2,}$");

    @Autowired
    private AppUserRepository appUserRepository;

    @Autowired
    private PasswordEncoder passwordEncoder;
    
    @Autowired
    private EmailService emailService;

    @GetMapping("/admin-login")
    public String viewAdminLoginPage() {
        return "admin-login"; 
    }

    @GetMapping("/applicant-login")
    public String viewApplicantLoginPage() {
        return "applicant-login"; 
    }

    @GetMapping("/register")
    public String showRegistrationForm(Model model) {
        model.addAttribute("newUser", new AppUser());
        return "register"; 
    }

    @PostMapping("/register")
    public String processRegistration(@ModelAttribute("newUser") AppUser newUser, Model model) {
        if (newUser.getUsername() != null) {
            newUser.setUsername(newUser.getUsername().trim());
        }

        if (newUser.getUsername() == null || newUser.getUsername().isEmpty()) {
            return registerError(model, newUser, "Please enter your email address.");
        }
        if (!EMAIL_PATTERN.matcher(newUser.getUsername()).matches()) {
            return registerError(model, newUser, "Please enter a valid email address (e.g. name@gmail.com).");
        }
        if (newUser.getPassword() == null || newUser.getPassword().length() < 6) {
            return registerError(model, newUser, "Password must be at least 6 characters.");
        }

        if (appUserRepository.findByUsername(newUser.getUsername()).isPresent()) {
            return registerError(model, newUser, "This email is already registered. Please log in.");
        }

        // Save the user securely
        // FIXED: Removed "ROLE_" so it perfectly matches your database structure!
        newUser.setRole("APPLICANT");
        
        String scrambledPassword = passwordEncoder.encode(newUser.getPassword());
        newUser.setPassword(scrambledPassword);
        appUserRepository.save(newUser); 
        
        // 3. SEND THE REAL EMAIL
        try {
            emailService.sendRegistrationEmail(newUser.getUsername());
        } catch (Exception e) {
            System.out.println("Failed to send welcome email: " + e.getMessage());
        }
        
        return "redirect:/applicant-login?registered=true";
    }

    private String registerError(Model model, AppUser newUser, String message) {
        newUser.setPassword(null);
        model.addAttribute("error", message);
        model.addAttribute("newUser", newUser);
        return "register";
    }
}