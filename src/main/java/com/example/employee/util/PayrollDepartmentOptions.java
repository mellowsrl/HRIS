package com.example.employee.util;

import java.util.List;

/**
 * Rows from <code>department</code> in payroll.sql (department_code, department_name).
 * Employee.department_code must match one of these for FK compliance.
 */
public final class PayrollDepartmentOptions {

    public record Row(String code, String name) {}

    /** Same order and spelling as <code>INSERT INTO department</code> in payroll.sql. */
    public static final List<Row> ALL = List.of(
            new Row("SAS", "School of Arts and Sciences"),
            new Row("SBE", "School of Business Education"),
            new Row("SCR", "School of Criminology"),
            new Row("SET", "School of Engineering and Technology"),
            new Row("SHTM", "School of Hospitality and Tourism Management"),
            new Row("SMC", "School of Midwifery & Caregiving"),
            new Row("SMD", "School of Medicine"),
            new Row("SMT", "School of Medical Technology"),
            new Row("SND", "School of Nutrition Dietretian"),
            new Row("SON", "School of Nursing"),
            new Row("SOT", "School of PTOTRT"),
            new Row("SPH", "School of Pharmacy")
    );

    private PayrollDepartmentOptions() {}
}
