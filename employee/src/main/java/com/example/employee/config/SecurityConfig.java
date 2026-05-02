package com.example.employee.config;

import com.example.employee.model.AppUser;
import com.example.employee.repository.AppUserRepository;
import org.springframework.boot.CommandLineRunner;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
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
            .authorizeHttpRequests((requests) -> requests
                .requestMatchers("/", "/apply", "/submit", "/login", "/employee-login", "/kiosk/**", "/css/**", "/image/**", "/js/**").permitAll()
                .requestMatchers("/hr/payroll/**").hasAnyRole("ADMIN", "FINANCE")
                .requestMatchers("/hr/**").hasAnyRole("ADMIN", "HR")
                .requestMatchers("/employee/**").hasRole("EMPLOYEE")
                .anyRequest().authenticated()
            )
            .formLogin((form) -> form
                .loginPage("/login") 
                .loginProcessingUrl("/login") 
                .successHandler(customAuthenticationSuccessHandler()) 
                .failureHandler(customAuthenticationFailureHandler()) 
                .permitAll()
            )
            .logout((logout) -> logout
                .logoutSuccessUrl("/?logout")
                .permitAll()
            )
            .exceptionHandling(exception -> exception
                .accessDeniedPage("/403")
                .authenticationEntryPoint((request, response, authException) -> {
                    if (request.getRequestURI().startsWith("/employee/")) {
                        response.sendRedirect("/employee-login");
                    } else {
                        response.sendRedirect("/login");
                    }
                })
            )
            .csrf(csrf -> csrf.disable());

        return http.build();
    }

    // ==========================================
    // THE STRICT BOUNCER (ON SUCCESS)
    // ==========================================
    @Bean
    public AuthenticationSuccessHandler customAuthenticationSuccessHandler() {
        return (request, response, authentication) -> {
            
            // 1. Check their actual roles
            boolean isFinance = authentication.getAuthorities().stream().anyMatch(g -> g.getAuthority().equals("ROLE_FINANCE"));
            boolean isEmployee = authentication.getAuthorities().stream().anyMatch(g -> g.getAuthority().equals("ROLE_EMPLOYEE"));
            
            // 2. Check WHICH door they used to log in
            String referer = request.getHeader("Referer");
            boolean fromEmployeeLogin = referer != null && referer.contains("/employee-login");

            // 3. STRICT ISOLATION RULES
            if (fromEmployeeLogin && !isEmployee) {
                // An Admin/HR/Finance tried to use the Blue Employee Portal!
                request.getSession().invalidate(); // Kill their session instantly
                response.sendRedirect("/employee-login?error"); // Kick them back to the blue login
                return;
            }

            if (!fromEmployeeLogin && isEmployee) {
                // An Employee tried to use the Red HR Portal!
                request.getSession().invalidate(); // Kill their session instantly
                response.sendRedirect("/login?error"); // Kick them back to the red login
                return;
            }

            // 4. If they used the correct door, route them properly!
            if (isFinance) {
                response.sendRedirect("/hr/payroll");
            } else if (isEmployee) {
                response.sendRedirect("/employee/dashboard");
            } else {
                response.sendRedirect("/hr/dashboard");
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
            if (referer != null && referer.contains("/employee-login")) {
                response.sendRedirect("/employee-login?error");
            } else {
                response.sendRedirect("/login?error");
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