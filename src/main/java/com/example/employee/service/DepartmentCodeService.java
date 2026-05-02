package com.example.employee.service;

import com.example.employee.model.EacDepartment;
import com.example.employee.repository.EacDepartmentRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.Comparator;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.stream.Collectors;

/**
 * Resolves {@code employee.department_code} and form inputs that may be either
 * a canonical code or a legacy full name, and maps codes to display names.
 */
@Service
public class DepartmentCodeService {

    @Autowired
    private EacDepartmentRepository eacDepartmentRepository;

    public String toCanonicalCode(String departmentInput) {
        if (departmentInput == null || departmentInput.isBlank()) {
            return departmentInput;
        }
        String raw = departmentInput.trim();
        return eacDepartmentRepository.findByCodeIgnoreCase(raw)
                .map(EacDepartment::getCode)
                .or(() -> eacDepartmentRepository.findByNameIgnoreCase(raw).map(EacDepartment::getCode))
                .orElse(raw);
    }

    public boolean matchesFilter(String storedDepartment, String filterParam) {
        if (filterParam == null || filterParam.isBlank()) {
            return true;
        }
        if (storedDepartment == null || storedDepartment.isBlank()) {
            return false;
        }
        String a = toCanonicalCode(storedDepartment);
        String b = toCanonicalCode(filterParam);
        return a != null && a.equalsIgnoreCase(b);
    }

    public String getDisplayName(String codeOrNameOrNull) {
        if (codeOrNameOrNull == null || codeOrNameOrNull.isBlank()) {
            return "—";
        }
        String t = codeOrNameOrNull.trim();
        return eacDepartmentRepository.findByCodeIgnoreCase(t)
                .map(EacDepartment::getName)
                .or(() -> eacDepartmentRepository.findByNameIgnoreCase(t).map(EacDepartment::getName))
                .orElse(t);
    }

    public List<EacDepartment> listAllForUi() {
        return eacDepartmentRepository.findAll().stream()
                .sorted(Comparator.comparing(
                        d -> d.getName() != null ? d.getName() : "", String.CASE_INSENSITIVE_ORDER))
                .toList();
    }

    public Map<String, String> codeToNameMap() {
        return eacDepartmentRepository.findAll().stream()
                .filter(d -> d.getCode() != null && !d.getCode().isBlank())
                .collect(Collectors.toMap(
                        d -> d.getCode().toUpperCase(Locale.ROOT), EacDepartment::getName, (a, b) -> a));
    }
}
