package com.example.employee.config;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.core.annotation.Order;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Component;

/**
 * One-time alignment after adding overtime_reported and ot_approval_status:
 * legacy rows only had overtime_hours; treat that as already approved and copy to reported.
 */
@Component
@Order(100)
public class OvertimeAttendanceMigrationRunner implements ApplicationRunner {

    private static final Logger log = LoggerFactory.getLogger(OvertimeAttendanceMigrationRunner.class);

    private final JdbcTemplate jdbcTemplate;

    public OvertimeAttendanceMigrationRunner(JdbcTemplate jdbcTemplate) {
        this.jdbcTemplate = jdbcTemplate;
    }

    @Override
    public void run(ApplicationArguments args) {
        try {
            int u1 = jdbcTemplate.update(
                "UPDATE attendance SET overtime_reported = IFNULL(overtime_reported, IFNULL(overtime_hours, 0)) "
                    + "WHERE overtime_reported IS NULL"
            );
            int u2 = jdbcTemplate.update(
                "UPDATE attendance SET ot_approval_status = 'APPROVED' "
                    + "WHERE (ot_approval_status IS NULL OR TRIM(ot_approval_status) = '') AND IFNULL(overtime_hours, 0) > 0"
            );
            int u3 = jdbcTemplate.update(
                "UPDATE attendance SET ot_approval_status = 'NONE' "
                    + "WHERE (ot_approval_status IS NULL OR TRIM(ot_approval_status) = '') "
                    + "AND IFNULL(overtime_hours, 0) = 0 AND IFNULL(overtime_reported, 0) = 0"
            );
            if (u1 + u2 + u3 > 0) {
                log.info("Overtime column migration: updated rows (reported={}, statusApproved={}, statusNone={})", u1, u2, u3);
            }
        } catch (Exception e) {
            log.warn("Overtime migration skipped: {}", e.getMessage());
        }
    }
}
