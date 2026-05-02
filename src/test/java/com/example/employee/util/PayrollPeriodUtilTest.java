package com.example.employee.util;

import org.junit.jupiter.api.Test;

import java.time.LocalDate;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class PayrollPeriodUtilTest {

    @Test
    void legacyKeyFirstHalf() {
        var p = PayrollPeriodUtil.resolve("2026-4-1");
        assertEquals(LocalDate.of(2026, 4, 1), p.start());
        assertEquals(LocalDate.of(2026, 4, 15), p.end());
        assertTrue(PayrollPeriodUtil.isStandardSemimonthlyWindow(p));
    }

    @Test
    void legacyKeySecondHalfFebruary() {
        var p = PayrollPeriodUtil.resolve("2025-2-2");
        assertEquals(LocalDate.of(2025, 2, 16), p.start());
        assertEquals(LocalDate.of(2025, 2, 28), p.end());
        assertTrue(PayrollPeriodUtil.isStandardSemimonthlyWindow(p));
    }

    @Test
    void customRangeKey() {
        var p = PayrollPeriodUtil.resolve("2025-04-10_2025-04-25");
        assertEquals(LocalDate.of(2025, 4, 10), p.start());
        assertEquals(LocalDate.of(2025, 4, 25), p.end());
        assertEquals(16, p.inclusiveDayCount());
        assertFalse(PayrollPeriodUtil.isStandardSemimonthlyWindow(p));
        assertTrue(PayrollPeriodUtil.isCustomRangeKey("2025-04-10_2025-04-25"));
    }

    @Test
    void customRangeSwapsReversedInput() {
        var p = PayrollPeriodUtil.resolve("2025-04-25_2025-04-10");
        assertEquals(LocalDate.of(2025, 4, 10), p.start());
        assertEquals(LocalDate.of(2025, 4, 25), p.end());
    }

    @Test
    void toCutoffKeyRoundTrip() {
        LocalDate a = LocalDate.of(2025, 1, 5);
        LocalDate b = LocalDate.of(2025, 1, 20);
        String k = PayrollPeriodUtil.toCutoffKey(a, b);
        var p = PayrollPeriodUtil.resolve(k);
        assertEquals(a, p.start());
        assertEquals(b, p.end());
    }
}
