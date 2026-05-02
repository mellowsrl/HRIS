package com.example.employee.util;

import java.time.LocalTime;
import java.time.format.DateTimeFormatter;
import java.time.format.DateTimeParseException;
import java.util.Locale;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * Parses {@code employee.expected_shift} for payroll suspension math.
 * Falls back to 08:00–17:00 when missing or unparseable (matches common TCMS defaults).
 */
public final class ShiftTimeParser {

    private static final DateTimeFormatter AM_PM = DateTimeFormatter.ofPattern("h:mm a", Locale.ENGLISH);

    private ShiftTimeParser() {}

    public static LocalTime defaultShiftStart() {
        return LocalTime.of(8, 0);
    }

    public static LocalTime defaultShiftEnd() {
        return LocalTime.of(17, 0);
    }

    /**
     * @return array {@code [shiftStart, shiftEnd]}
     */
    public static LocalTime[] parseShiftBounds(String expectedShift) {
        if (expectedShift == null || expectedShift.isBlank()) {
            return new LocalTime[] { defaultShiftStart(), defaultShiftEnd() };
        }
        String s = expectedShift.trim();

        Pattern amPmRange = Pattern.compile(
            "(\\d{1,2}:\\d{2}\\s*[AaPp][Mm])\\s*-\\s*(\\d{1,2}:\\d{2}\\s*[AaPp][Mm])",
            Pattern.CASE_INSENSITIVE);
        Matcher m1 = amPmRange.matcher(s);
        if (m1.find()) {
            try {
                String a = m1.group(1).replaceAll("\\s+", " ").trim();
                String b = m1.group(2).replaceAll("\\s+", " ").trim();
                LocalTime st = LocalTime.parse(a.toUpperCase(Locale.ROOT), AM_PM);
                LocalTime en = LocalTime.parse(b.toUpperCase(Locale.ROOT), AM_PM);
                if (en.isAfter(st)) {
                    return new LocalTime[] { st, en };
                }
            } catch (DateTimeParseException ignored) {
                // fall through
            }
        }

        Pattern h24Range = Pattern.compile("(\\d{1,2}):(\\d{2})\\s*-\\s*(\\d{1,2}):(\\d{2})");
        Matcher m2 = h24Range.matcher(s);
        if (m2.find()) {
            try {
                int h1 = Integer.parseInt(m2.group(1));
                int mi1 = Integer.parseInt(m2.group(2));
                int h2 = Integer.parseInt(m2.group(3));
                int mi2 = Integer.parseInt(m2.group(4));
                LocalTime st = LocalTime.of(h1, mi1);
                LocalTime en = LocalTime.of(h2, mi2);
                if (en.isAfter(st)) {
                    return new LocalTime[] { st, en };
                }
            } catch (Exception ignored) {
                // fall through
            }
        }

        return new LocalTime[] { defaultShiftStart(), defaultShiftEnd() };
    }
}
