package com.example.employee.model;

/**
 * Read-only view row for the HR salary rules audit page (Thymeleaf).
 */
public record SalaryAuditRow(
    long id,
    String customEmployeeId,
    String fullName,
    String department,
    String employmentType,
    boolean partTimeOrFlexiPath,
    /** basic_salary column */
    Double basicSalary,
    /** admin_pay column (monthly, used by SP as v_monthly) */
    Double adminPay,
    Double dailyWageStored,
    Double hourlyRateStored,
    String monthlySourceLabel,
    double effectiveMonthly,
    double doleYearly,
    double doleDaily,
    double doleHourly,
    double doleMinute,
    int periodDayCount,
    String periodStartIso,
    String periodEndIso,
    String periodLabel,
    double periodBasicPay,
    String periodRuleLabel,
    /** empty if no mismatch: implied monthly from daily_wage vs admin_pay */
    String consistencyMessage,
    boolean consistencyMismatch
) {}
