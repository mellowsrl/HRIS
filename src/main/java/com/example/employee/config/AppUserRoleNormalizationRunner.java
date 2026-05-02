package com.example.employee.config;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.core.annotation.Order;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Component;

/**
 * Align legacy {@code ROLE_*} values in {@code app_users.role} with {@code hasAuthority('ADMIN')} etc.
 */
@Component
@Order(50)
public class AppUserRoleNormalizationRunner implements ApplicationRunner {

    private static final Logger log = LoggerFactory.getLogger(AppUserRoleNormalizationRunner.class);

    private final JdbcTemplate jdbcTemplate;

    public AppUserRoleNormalizationRunner(JdbcTemplate jdbcTemplate) {
        this.jdbcTemplate = jdbcTemplate;
    }

    @Override
    public void run(ApplicationArguments args) {
        try {
            int n = 0;
            n += jdbcTemplate.update("UPDATE app_users SET role = 'ADMIN' WHERE role = 'ROLE_ADMIN'");
            n += jdbcTemplate.update("UPDATE app_users SET role = 'HR' WHERE role = 'ROLE_HR'");
            n += jdbcTemplate.update("UPDATE app_users SET role = 'FINANCE' WHERE role = 'ROLE_FINANCE'");
            n += jdbcTemplate.update("UPDATE app_users SET role = 'EMPLOYEE' WHERE role = 'ROLE_EMPLOYEE'");
            n += jdbcTemplate.update("UPDATE app_users SET role = 'APPLICANT' WHERE role = 'ROLE_APPLICANT'");
            if (n > 0) {
                log.info("Normalized {} app_users role row(s) from ROLE_* to plain authority names.", n);
            }
        } catch (Exception e) {
            log.debug("Role normalization skipped: {}", e.getMessage());
        }
    }
}
