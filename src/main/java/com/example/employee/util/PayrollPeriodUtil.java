package com.example.employee.util;

import java.time.LocalDate;
import java.time.YearMonth;
import java.time.format.DateTimeFormatter;
import java.time.temporal.ChronoUnit;
import java.util.Locale;

/**
 * Resolves pay window start/end from a cutoff string:
 * <ul>
 *   <li><b>Legacy</b> {@code YYYY-M-H} where H is 1 (1st–15th) or 2 (16th–EOM)</li>
 *   <li><b>Custom</b> {@code YYYY-MM-DD_YYYY-MM-DD} (inclusive, ISO-8601, underscore between dates)</li>
 * </ul>
 * Matches {@code getPayrollData} / {@code SP_ProcessRegularPayroll} period inputs.
 */
public final class PayrollPeriodUtil {

    public static final int MAX_CUSTOM_RANGE_DAYS = 400;

    private static final DateTimeFormatter EN_FMT =
        DateTimeFormatter.ofPattern("MMM d, yyyy", Locale.ENGLISH);

    public record PayrollPeriod(LocalDate start, LocalDate end) {
        public int inclusiveDayCount() {
            return (int) ChronoUnit.DAYS.between(start, end) + 1;
        }
    }

    private PayrollPeriodUtil() {}

    /**
     * @param cutoffKey e.g. {@code 2026-4-1}, {@code 2026-4-2}, or {@code 2025-04-10_2025-04-25}; if null/blank/invalid, uses current calendar semi-month.
     */
    public static PayrollPeriod resolve(String cutoffKey) {
        if (cutoffKey == null || cutoffKey.isBlank()) {
            return currentCalendarSemiMonth();
        }
        String trimmed = cutoffKey.trim();

        if (trimmed.contains("_")) {
            PayrollPeriod custom = tryParseCustomRange(trimmed);
            if (custom != null) {
                return custom;
            }
            return currentCalendarSemiMonth();
        }

        if (trimmed.contains("-")) {
            String[] parts = trimmed.split("-");
            if (parts.length >= 3) {
                try {
                    int year = Integer.parseInt(parts[0]);
                    int month = Integer.parseInt(parts[1]);
                    int half = Integer.parseInt(parts[2]);
                    if (half == 1) {
                        return new PayrollPeriod(
                            LocalDate.of(year, month, 1),
                            LocalDate.of(year, month, 15));
                    }
                    if (half == 2) {
                        return new PayrollPeriod(
                            LocalDate.of(year, month, 16),
                            LocalDate.of(year, month, java.time.YearMonth.of(year, month).lengthOfMonth()));
                    }
                } catch (Exception ignored) {
                    // fall through
                }
            }
        }

        return currentCalendarSemiMonth();
    }

    private static PayrollPeriod tryParseCustomRange(String trimmed) {
        int u = trimmed.indexOf('_');
        if (u <= 0 || u >= trimmed.length() - 1) {
            return null;
        }
        String a = trimmed.substring(0, u).trim();
        String b = trimmed.substring(u + 1).trim();
        try {
            LocalDate s = LocalDate.parse(a);
            LocalDate e = LocalDate.parse(b);
            if (s.isAfter(e)) {
                LocalDate t = s;
                s = e;
                e = t;
            }
            long days = ChronoUnit.DAYS.between(s, e) + 1;
            if (days < 1 || days > MAX_CUSTOM_RANGE_DAYS) {
                return null;
            }
            return new PayrollPeriod(s, e);
        } catch (Exception ex) {
            return null;
        }
    }

    public static String toCutoffKey(LocalDate start, LocalDate end) {
        if (start == null || end == null) {
            return "";
        }
        LocalDate s = start;
        LocalDate e = end;
        if (s.isAfter(e)) {
            LocalDate t = s;
            s = e;
            e = t;
        }
        return s + "_" + e;
    }

    public static String formatEnglishRangeLabel(PayrollPeriod period) {
        if (period == null) {
            return "";
        }
        return period.start().format(EN_FMT) + " - " + period.end().format(EN_FMT);
    }

    /**
     * True if the range is a calendar 1st–15th or 16th–last, same month/year (typical “semi-monthly” in PH payroll).
     */
    public static boolean isStandardSemimonthlyWindow(PayrollPeriod p) {
        if (p == null) {
            return false;
        }
        LocalDate s = p.start();
        LocalDate e = p.end();
        if (!YearMonth.from(s).equals(YearMonth.from(e))) {
            return false;
        }
        int last = s.lengthOfMonth();
        return (s.getDayOfMonth() == 1 && e.getDayOfMonth() == 15)
            || (s.getDayOfMonth() == 16 && e.getDayOfMonth() == last);
    }

    public static boolean isCustomRangeKey(String cutoffKey) {
        return cutoffKey != null && cutoffKey.contains("_");
    }

    private static PayrollPeriod currentCalendarSemiMonth() {
        LocalDate today = LocalDate.now();
        LocalDate startDate = today.getDayOfMonth() <= 15 ? today.withDayOfMonth(1) : today.withDayOfMonth(16);
        LocalDate endDate = today.getDayOfMonth() <= 15
            ? today.withDayOfMonth(15)
            : today.withDayOfMonth(today.lengthOfMonth());
        return new PayrollPeriod(startDate, endDate);
    }
}
