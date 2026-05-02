package com.example.employee.service;

import com.example.employee.model.EacDepartment;
import com.example.employee.repository.EacDepartmentRepository;
import com.example.employee.repository.OfficialEmployeeRepository;
import com.example.employee.util.PayrollDepartmentOptions;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.context.event.ApplicationReadyEvent;
import org.springframework.context.event.EventListener;
import org.springframework.core.annotation.Order;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.stream.Stream;

@Service
public class DepartmentService {

    private static final Logger log = LoggerFactory.getLogger(DepartmentService.class);

    private static final String DEFAULT_BRANCH = "Main (Manila)";

    /** Suggested campus or site labels for EAC. */
    public static final List<String> BRANCH_CHOICES = List.of(
        "Main (Manila)",
        "Dasmariñas",
        "Cavite",
        "Batangas",
        "Other / satellite"
    );

    @Autowired
    private EacDepartmentRepository departmentRepository;

    @Autowired
    private OfficialEmployeeRepository officialEmployeeRepository;

    @Autowired
    private JdbcTemplate jdbcTemplate;

    public record DepartmentListItem(
        long id, String name, String code, long activeEmployeeCount,
        String headName, String branch, String status) {}

    public List<DepartmentListItem> getDepartmentListItems() {
        return departmentRepository.findAll().stream()
            .map(d -> {
                long n = 0L;
                String c = d.getCode();
                if (c != null && !c.isBlank()) {
                    n = officialEmployeeRepository.countActiveByDepartmentCode(
                        c.trim().toUpperCase(Locale.ROOT), "Active");
                }
                return new DepartmentListItem(
                    d.getId() != null ? d.getId() : 0L,
                    d.getName() != null ? d.getName() : "",
                    c != null ? c : "",
                    n,
                    d.getHeadName() != null && !d.getHeadName().isBlank() ? d.getHeadName() : null,
                    d.getBranch() != null ? d.getBranch() : DEFAULT_BRANCH,
                    d.getStatus() != null ? d.getStatus() : "Active"
                );
            })
            .sorted((a, b) -> a.name().compareToIgnoreCase(b.name()))
            .toList();
    }

    @Transactional
    public void add(String name, String code, String headName, String branch, String status) {
        EacDepartment d = new EacDepartment();
        d.setName(name.trim());
        d.setCode(normalizeCode(code));
        d.setHeadName(blankToNull(headName));
        d.setBranch(branch == null || branch.isBlank() ? DEFAULT_BRANCH : branch.trim());
        d.setStatus(status == null || status.isBlank() ? "Active" : status.trim());
        departmentRepository.save(d);
    }

    @Transactional
    public void update(long id, String name, String code, String headName, String branch, String status) {
        EacDepartment d = departmentRepository.findById(id)
            .orElseThrow(() -> new IllegalArgumentException("Department not found."));
        d.setName(name.trim());
        d.setCode(normalizeCode(code));
        d.setHeadName(blankToNull(headName));
        d.setBranch(branch == null || branch.isBlank() ? DEFAULT_BRANCH : branch.trim());
        d.setStatus(status == null || status.isBlank() ? "Active" : status.trim());
        departmentRepository.save(d);
    }

    @Transactional
    public void deleteById(long id) {
        EacDepartment d = departmentRepository.findById(id)
            .orElseThrow(() -> new IllegalArgumentException("Department not found."));
        String c = d.getCode();
        if (c != null && !c.isBlank()) {
            long n = officialEmployeeRepository.countActiveByDepartmentCode(
                c.trim().toUpperCase(Locale.ROOT), "Active");
            if (n > 0) {
                throw new IllegalStateException(
                    "This department is assigned to " + n + " active employee(s). Reassign or update employees first.");
            }
        }
        departmentRepository.deleteById(id);
    }

    public void validateAdd(String code) {
        String c = normalizeCode(code);
        if (departmentRepository.existsByCodeIgnoreCase(c)) {
            throw new IllegalArgumentException("A department with code " + c + " already exists.");
        }
    }

    public void validateUpdate(long id, String code) {
        String c = normalizeCode(code);
        if (departmentRepository.existsByCodeIgnoreCaseAndIdNot(c, id)) {
            throw new IllegalArgumentException("Another department already uses code " + c + ".");
        }
    }

    private static String blankToNull(String s) {
        if (s == null) return null;
        String t = s.trim();
        return t.isEmpty() ? null : t;
    }

    private static String normalizeCode(String code) {
        if (code == null) throw new IllegalArgumentException("Code is required.");
        String t = code.trim().toUpperCase(Locale.ROOT);
        if (t.isEmpty()) throw new IllegalArgumentException("Code is required.");
        if (t.length() > 20) throw new IllegalArgumentException("Code must be 20 characters or less.");
        return t;
    }

    /**
     * First-time seed: {@link PayrollDepartmentOptions} (schools) plus common EAC administrative offices.
     * Runs before {@link #backfillDepartmentNamesToCodes()} so the master list exists.
     */
    @EventListener(ApplicationReadyEvent.class)
    @Order(100)
    @Transactional
    public void seedIfEmpty() {
        if (departmentRepository.count() > 0) {
            // Normalize old default/main labels from earlier builds.
            List<EacDepartment> all = departmentRepository.findAll();
            boolean changed = false;
            for (EacDepartment d : all) {
                String b = d.getBranch();
                if ("Main (Dasmariñas)".equalsIgnoreCase(b)
                    || "Main (Dasmarinas)".equalsIgnoreCase(b)
                    || "Manila".equalsIgnoreCase(b)) {
                    d.setBranch(DEFAULT_BRANCH);
                    changed = true;
                }
            }
            if (changed) {
                departmentRepository.saveAll(all);
            }
            return;
        }
        List<EacDepartment> list = new ArrayList<>();
        for (PayrollDepartmentOptions.Row row : PayrollDepartmentOptions.ALL) {
            list.add(new EacDepartment(row.name(), row.code().toUpperCase(Locale.ROOT), null, DEFAULT_BRANCH, "Active"));
        }
        Stream.of(
            new EacDepartment("Office of the Registrar", "REG", null, DEFAULT_BRANCH, "Active"),
            new EacDepartment("Office of Admissions and Scholarships", "ADM", null, DEFAULT_BRANCH, "Active"),
            new EacDepartment("Finance and Administration Office", "FAO", null, DEFAULT_BRANCH, "Active"),
            new EacDepartment("Human Resource Management Office", "HRO", null, DEFAULT_BRANCH, "Active"),
            new EacDepartment("Information and Communications Technology", "ITS", null, DEFAULT_BRANCH, "Active"),
            new EacDepartment("University Library", "LIB", null, DEFAULT_BRANCH, "Active"),
            new EacDepartment("Office of Student Affairs and Services", "OSA", null, DEFAULT_BRANCH, "Active")
        ).forEach(list::add);
        departmentRepository.saveAll(list);
    }

    /**
     * Idempotent: rows where the stored value matches {@code eac_department.name} are rewritten to
     * {@code eac_department.code} (employee.department_code, job_postings.department).
     */
    @EventListener(ApplicationReadyEvent.class)
    @Order(200)
    public void backfillDepartmentNamesToCodes() {
        try {
            int emp = jdbcTemplate.update(
                "UPDATE employee e "
                    + "INNER JOIN eac_department d ON UPPER(TRIM(e.department_code)) = UPPER(TRIM(d.name)) "
                    + "SET e.department_code = d.code "
                    + "WHERE e.department_code IS NOT NULL "
                    + "AND UPPER(TRIM(e.department_code)) != UPPER(TRIM(d.code))");
            int job = jdbcTemplate.update(
                "UPDATE job_postings jp "
                    + "INNER JOIN eac_department d ON UPPER(TRIM(jp.department)) = UPPER(TRIM(d.name)) "
                    + "SET jp.department = d.code "
                    + "WHERE jp.department IS NOT NULL "
                    + "AND UPPER(TRIM(jp.department)) != UPPER(TRIM(d.code))");
            if (emp + job > 0) {
                log.info("Department name \u2192 code backfill: employee rows updated={}, job_posting rows={}", emp, job);
            }
        } catch (Exception e) {
            log.warn("Department name \u2192 code backfill skipped: {}", e.getMessage());
        }
    }
}
