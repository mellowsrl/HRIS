package com.example.employee.util;

import com.example.employee.model.Applicant;
import com.example.employee.model.Holiday;
import com.example.employee.model.JobPosting;
import com.example.employee.model.OfficialEmployee;
import com.example.employee.model.Suspension;

/**
 * Consistent case-insensitive search: every whitespace-separated token must appear somewhere in the haystack.
 */
public final class ListSearchUtil {

    private ListSearchUtil() {}

    public static boolean isActiveKeyword(String raw) {
        return raw != null && !raw.trim().isEmpty();
    }

    public static String buildHaystack(String... parts) {
        StringBuilder sb = new StringBuilder();
        for (String p : parts) {
            if (p != null) {
                sb.append(' ').append(p);
            }
        }
        return sb.toString().toLowerCase();
    }

    public static boolean matchesTokens(String rawKeyword, String haystackLower) {
        if (!isActiveKeyword(rawKeyword)) {
            return true;
        }
        String h = haystackLower;
        for (String token : rawKeyword.trim().toLowerCase().split("\\s+")) {
            if (!token.isEmpty() && !h.contains(token)) {
                return false;
            }
        }
        return true;
    }

    public static boolean matchesEmployee(OfficialEmployee e, String rawKeyword) {
        if (!isActiveKeyword(rawKeyword)) {
            return true;
        }
        String hay = buildHaystack(
            e.getCustomEmployeeId(),
            e.getFirstName(),
            e.getLastName(),
            e.getEmail(),
            e.getDepartment(),
            e.getPosition()
        );
        return matchesTokens(rawKeyword, hay);
    }

    public static boolean matchesApplicant(Applicant a, String rawKeyword) {
        if (!isActiveKeyword(rawKeyword)) {
            return true;
        }
        String hay = buildHaystack(
            String.valueOf(a.getId()),
            a.getFirstName(),
            a.getLastName(),
            a.getEmail(),
            a.getPhone(),
            a.getDepartment(),
            a.getPositionApplied(),
            a.getStatus(),
            a.getExperienceText()
        );
        return matchesTokens(rawKeyword, hay);
    }

    public static boolean matchesJobPosting(JobPosting j, String rawKeyword) {
        return matchesJobPostingWithDeptAlias(j, rawKeyword, null);
    }

    /**
     * @param extraDepartmentText optional display name for the job's department (e.g. from {@code eac_department.name})
     *        so search terms like "Nursing" still match a row stored with code {@code SON}.
     */
    public static boolean matchesJobPostingWithDeptAlias(JobPosting j, String rawKeyword, String extraDepartmentText) {
        if (!isActiveKeyword(rawKeyword)) {
            return true;
        }
        String hay = buildHaystack(
            j.getTitle() != null ? j.getTitle() : "",
            j.getDepartment(),
            extraDepartmentText,
            j.getEmploymentType(),
            j.getDescription(),
            j.getRequirements()
        );
        return matchesTokens(rawKeyword, hay);
    }

    public static boolean matchesSuspension(Suspension s, String rawKeyword) {
        if (!isActiveKeyword(rawKeyword)) {
            return true;
        }
        String hay = buildHaystack(
            String.valueOf(s.getId()),
            s.getDate() != null ? s.getDate().toString() : "",
            s.getReason(),
            s.getStartTime() != null ? s.getStartTime().toString() : "",
            s.getEmployeeId() != null ? String.valueOf(s.getEmployeeId()) : ""
        );
        return matchesTokens(rawKeyword, hay);
    }

    public static boolean matchesHoliday(Holiday h, String rawKeyword) {
        if (!isActiveKeyword(rawKeyword)) {
            return true;
        }
        String hay = buildHaystack(
            h.getDate() != null ? h.getDate().toString() : "",
            h.getName(),
            h.getType()
        );
        return matchesTokens(rawKeyword, hay);
    }

    public static boolean matchesMyApplication(Applicant a, String rawKeyword) {
        if (!isActiveKeyword(rawKeyword)) {
            return true;
        }
        String jpTitle = a.getJobPosting() != null && a.getJobPosting().getTitle() != null
            ? a.getJobPosting().getTitle() : "";
        String jpDept = a.getJobPosting() != null && a.getJobPosting().getDepartment() != null
            ? a.getJobPosting().getDepartment() : "";
        String hay = buildHaystack(
            String.valueOf(a.getId()),
            a.getStatus(),
            a.getPositionApplied(),
            jpTitle,
            jpDept
        );
        return matchesTokens(rawKeyword, hay);
    }
}
