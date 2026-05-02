package com.example.employee.service;

/**
 * Scope for bulk leave credit reset (active employees only, except all/archive views do not include archive tab rows).
 */
public enum LeaveCreditResetMode {
    /** Checkbox-selected employee IDs (active only). */
    SELECTED,
    /** Every employee with status Active. */
    ALL,
    /** Active employees in the chosen department (code). */
    DEPARTMENT
}
