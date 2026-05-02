-- =============================================================================
-- One-time migration: copy rows from legacy `attendance_logs` into `attendance`
-- ONLY if both tables exist and employee_id in legacy means the same as this app:
--   internal employee.id as VARCHAR (e.g. '5'), NOT EAC id '1-00001'.
--
-- If your legacy table uses `log_date` and `official_employees` (see eac_hr dump), use
--   migrate_eac_hr_attendance_logs_to_attendance.sql instead of the blocks below.
-- BACK UP FIRST. Test on a copy of the database.
-- If schemas differ, edit column lists to match SHOW CREATE TABLE on BOTH tables.
-- =============================================================================

-- --- Prerequisite: If legacy stored EAC/custom id instead of internal id, use the
-- --- block at the bottom (INSERT...SELECT with JOIN employee) instead of the simple block.

-- Simple case: attendance_logs columns match attendance (same employee_id semantics)
/*
INSERT INTO attendance (
  employee_id,
  `date`,
  time_in,
  time_out,
  total_hours,
  minutes_late,
  undertime_hours,
  overtime_hours,
  overtime_reported,
  ot_approval_status,
  minutes_early_out
)
SELECT
  l.employee_id,
  l.`date`,
  l.time_in,
  l.time_out,
  l.total_hours,
  l.minutes_late,
  l.undertime_hours,
  l.overtime_hours,
  l.overtime_reported,
  l.ot_approval_status,
  l.minutes_early_out
FROM attendance_logs l
WHERE NOT EXISTS (
  SELECT 1 FROM attendance a
  WHERE a.employee_id = l.employee_id AND a.`date` = l.`date`
);
*/

-- If legacy table is missing OT columns, use COALESCE (uncomment and match your columns):
/*
INSERT INTO attendance (
  employee_id, `date`, time_in, time_out, total_hours, minutes_late, undertime_hours,
  overtime_hours, overtime_reported, ot_approval_status, minutes_early_out
)
SELECT
  l.employee_id,
  l.`date`,
  l.time_in,
  l.time_out,
  l.total_hours,
  COALESCE(l.minutes_late, 0),
  l.undertime_hours,
  l.overtime_hours,
  COALESCE(l.overtime_reported, 0),
  COALESCE(l.ot_approval_status, 'NONE'),
  l.minutes_early_out
FROM attendance_logs l
WHERE NOT EXISTS (
  SELECT 1 FROM attendance a
  WHERE a.employee_id = l.employee_id AND a.`date` = l.`date`
);
*/

-- Case: legacy employee_id is custom_employee_id (EAC string) — map to internal id:
/*
INSERT INTO attendance (
  employee_id, `date`, time_in, time_out, total_hours, minutes_late, undertime_hours,
  overtime_hours, overtime_reported, ot_approval_status, minutes_early_out
)
SELECT
  CAST(e.id AS CHAR) AS employee_id,
  l.`date`,
  l.time_in,
  l.time_out,
  l.total_hours,
  COALESCE(l.minutes_late, 0),
  l.undertime_hours,
  l.overtime_hours,
  COALESCE(l.overtime_reported, 0),
  COALESCE(l.ot_approval_status, 'NONE'),
  l.minutes_early_out
FROM attendance_logs l
JOIN employee e ON e.custom_employee_id = l.employee_id
WHERE NOT EXISTS (
  SELECT 1 FROM attendance a
  WHERE a.employee_id = CAST(e.id AS CHAR) AND a.`date` = l.`date`
);
*/

SELECT 'Uncomment ONE block above after verifying schema; this line is a no-op.' AS instruction;
