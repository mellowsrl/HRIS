package com.example.employee.util;

/**
 * DOLE-style rates using 313 working days in a year (common Philippine payroll convention).
 * <p>
 * Aligns with {@code employee.admin_pay} usage in MySQL {@code SP_ProcessRegularPayroll}:
 * it treats {@code admin_pay} as the monthly amount and derives
 * {@code v_daily = (v_monthly * 12) / 313}. The inverse for converting a <em>known daily rate</em>
 * to an equivalent monthly is {@code daily × (313 / 12)}.
 */
public final class PayrollDoleMath {

    public static final int DOLE_WORKING_DAYS_PER_YEAR = 313;
    public static final double MONTHS_PER_YEAR = 12.0;

    private PayrollDoleMath() {}

    /**
     * Monthly rate from daily rate: daily × (313 ÷ 12). Use when only daily wage is given.
     * Inverse of {@link #dailyFromMonthlyBasic(double)}.
     */
    public static double monthlyFromDaily(double daily) {
        return daily * (DOLE_WORKING_DAYS_PER_YEAR / MONTHS_PER_YEAR);
    }

    public static Double monthlyFromDaily(Double daily) {
        if (daily == null) {
            return null;
        }
        return monthlyFromDaily(daily.doubleValue());
    }

    /**
     * Daily rate from monthly basic: (monthly × 12) ÷ 313. Matches the SP’s daily computation from {@code admin_pay}.
     */
    public static double dailyFromMonthlyBasic(double monthly) {
        return (monthly * MONTHS_PER_YEAR) / DOLE_WORKING_DAYS_PER_YEAR;
    }
}
