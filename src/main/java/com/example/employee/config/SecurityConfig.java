package com.example.employee.config;

import com.example.employee.model.AppUser;
import com.example.employee.repository.AppUserRepository;
import org.springframework.boot.CommandLineRunner;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.HttpMethod;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.authentication.AuthenticationFailureHandler;
import org.springframework.security.web.authentication.AuthenticationSuccessHandler;

@Configuration
public class SecurityConfig {

    @Bean
    public SecurityFilterChain securityFilterChain(HttpSecurity http) throws Exception {
        http
            .authorizeHttpRequests(auth -> auth
                .requestMatchers(
                    "/", 
                    "/apply",          
                    "/submit",         
                    "/admin-login",    // <-- NEW Admin Door
                    "/applicant-login",// <-- NEW Applicant Door
                    "/employee-login", // <-- Existing Employee Door
                    "/register",       
                    "/kiosk",
                    "/kiosk/tap",
                    "/css/**", 
                    "/image/**", 
                    "/js/**",
                    "/uploads/applicant-resume/**",
                    "/uploads/overtime-attachments/**",
                    "/uploads/official-business-attachments/**",
                    "/uploads/employee-photos/**"
                ).permitAll()

                .requestMatchers(HttpMethod.POST, "/hr/employees/*/profile/photo").hasAnyAuthority("ADMIN", "HR")

                // Leave credits reset: ADMIN/HR only (not FINANCE)
                .requestMatchers("/hr/leave-credits/reset").hasAnyAuthority("ADMIN", "HR")

                // Audit + saved pay presets: sensitive / operational — ADMIN/HR only (not FINANCE)
                .requestMatchers("/hr/audit-log", "/hr/payroll-periods", "/hr/payroll-periods/**")
                    .hasAnyAuthority("ADMIN", "HR")

                .requestMatchers("/hr/**").hasAnyAuthority("ADMIN", "HR", "FINANCE")
                .requestMatchers("/employee/**").hasAuthority("EMPLOYEE")
                .requestMatchers("/applicant/**").hasAuthority("APPLICANT")
                
                .anyRequest().authenticated()
            )
            .formLogin(form -> form
                .loginPage("/admin-login") // Default fallback page
                .loginProcessingUrl("/process-login") // <-- CRITICAL: All 3 forms must submit here
                .successHandler(customAuthenticationSuccessHandler()) 
                .failureHandler(customAuthenticationFailureHandler()) 
                .permitAll()
            )
            .logout(logout -> logout
                .logoutSuccessUrl("/?logout")
                .permitAll()
            )
            .exceptionHandling(exception -> exception
                .accessDeniedPage("/403")
                .authenticationEntryPoint((request, response, authException) -> {
                    // Smart redirect based on what they were trying to access
                    if (request.getRequestURI().startsWith("/employee/")) {
                        response.sendRedirect("/employee-login");
                    } else if (request.getRequestURI().startsWith("/applicant/")) {
                        response.sendRedirect("/applicant-login");
                    } else {
                        response.sendRedirect("/admin-login");
                    }
                })
            )
            .csrf(csrf -> csrf.disable());

        return http.build();
    }

    // ==========================================
    // THE STRICT TRIPLE-BOUNCER (ON SUCCESS)
    // ==========================================
    @Bean
    public AuthenticationSuccessHandler customAuthenticationSuccessHandler() {
        return (request, response, authentication) -> {
            
            // 1. Identify Who They Are
        	// INSIDE your customAuthenticationSuccessHandler method...
        	boolean isFinance = authentication.getAuthorities().stream().anyMatch(g -> g.getAuthority().equals("FINANCE"));
        	boolean isHR = authentication.getAuthorities().stream().anyMatch(g -> g.getAuthority().equals("HR"));
        	boolean isAdmin = authentication.getAuthorities().stream().anyMatch(g -> g.getAuthority().equals("ADMIN"));
        	boolean isManagement = isFinance || isHR || isAdmin;

        	boolean isEmployee = authentication.getAuthorities().stream().anyMatch(g -> g.getAuthority().equals("EMPLOYEE"));
        	boolean isApplicant = authentication.getAuthorities().stream().anyMatch(g -> g.getAuthority().equals("APPLICANT"));
            // 2. Identify Which Door They Used
            String referer = request.getHeader("Referer");
            boolean fromEmployee = referer != null && referer.contains("/employee-login");
            boolean fromApplicant = referer != null && referer.contains("/applicant-login");
            boolean fromAdmin = referer != null && referer.contains("/admin-login");

            // 3. ENFORCE STRICT ISOLATION RULES
            if (fromEmployee && !isEmployee) {
                request.getSession().invalidate(); 
                response.sendRedirect("/employee-login?error=unauthorized"); 
                return;
            }
            if (fromApplicant && !isApplicant) {
                request.getSession().invalidate(); 
                response.sendRedirect("/applicant-login?error=unauthorized"); 
                return;
            }
            if (fromAdmin && !isManagement) {
                request.getSession().invalidate(); 
                response.sendRedirect("/admin-login?error=unauthorized"); 
                return;
            }

            // 4. Send Them to Their Specific Dashboard
            if (isFinance) {
                response.sendRedirect("/hr/payroll");
            } else if (isManagement) {
                response.sendRedirect("/hr/dashboard");
            } else if (isEmployee) {
                response.sendRedirect("/employee/dashboard");
            } else if (isApplicant) {
                response.sendRedirect("/applicant/dashboard"); 
            } else {
                response.sendRedirect("/");
            }
        };
    }

    // ==========================================
    // THE TRAFFIC COP (ON FAILURE)
    // ==========================================
    @Bean
    public AuthenticationFailureHandler customAuthenticationFailureHandler() {
        return (request, response, exception) -> {
            String referer = request.getHeader("Referer");
            // Send them back exactly where they failed
            if (referer != null && referer.contains("/employee-login")) {
                response.sendRedirect("/employee-login?error");
            } else if (referer != null && referer.contains("/applicant-login")) {
                response.sendRedirect("/applicant-login?error");
            } else {
                response.sendRedirect("/admin-login?error");
            }
        };
    }

    @Bean
    public PasswordEncoder passwordEncoder() {
        return new BCryptPasswordEncoder();
    }

    @Bean
    public CommandLineRunner initDatabaseUsers(AppUserRepository repo, PasswordEncoder encoder) {
        return args -> {
            if (repo.findByUsername("admin").isEmpty()) {
                AppUser admin = new AppUser(); admin.setUsername("admin"); admin.setPassword(encoder.encode("adminpassword")); admin.setRole("ADMIN"); repo.save(admin);
            }
            if (repo.findByUsername("hr").isEmpty()) {
                AppUser hr = new AppUser(); hr.setUsername("hr"); hr.setPassword(encoder.encode("hrpassword")); hr.setRole("HR"); repo.save(hr);
            }
            if (repo.findByUsername("finance").isEmpty()) {
                AppUser finance = new AppUser(); finance.setUsername("finance"); finance.setPassword(encoder.encode("financepassword")); finance.setRole("FINANCE"); repo.save(finance);
            }
        };
    }
}