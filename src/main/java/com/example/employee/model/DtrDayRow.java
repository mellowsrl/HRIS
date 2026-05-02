package com.example.employee.model;

/**
 * One calendar day on the employee Daily Time Record (DTR) grid.
 */
public record DtrDayRow(
    String dateDisplay,
    boolean hasLog,
    String in1,
    String out1,
    String in2,
    String out2,
    String late,
    String under,
    String ot,
    /** REST_DAY, PENALTY, OK, NO_LOG */
    String remarkType,
    /** TCMS: Holiday, Rest day, Leave (VL), etc.; "—" when none */
    String dayStatus
) {}
