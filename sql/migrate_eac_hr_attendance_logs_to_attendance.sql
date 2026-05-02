-- =============================================================================
-- EAC: Legacy `attendance_logs` (eac_hr_db_attendance_logs.sql) → JPA `attendance`
-- Legacy columns: log_date, employee_id INT, time_in, time_out, undertime_minutes, …
-- App: attendance.employee_id = CAST(employee.employee_id AS CHAR) (see OfficialEmployee).
-- If legacy.employee_id matches that PK, the CAST is correct. Verify JOIN sample rows first.
-- BACK UP. Test on a copy. See migrate_attendance_logs_to_attendance.sql for same-column schemas.
-- =============================================================================
-- t = minutes between time_in and time_out; if t < 0, +1440; if t >= 540, −60 (lunch), /60 = hours
-- (mirrors AdmissionService.processBiometricsCsv core math).
-- =============================================================================

/*
INSERT INTO attendance (
  employee_id, `date`, time_in, time_out, total_hours, minutes_late, undertime_hours,
  overtime_hours, overtime_reported, ot_approval_status, minutes_early_out
)
SELECT
  CAST(l.employee_id AS CHAR(255)) AS employee_id,
  l.log_date AS `date`,
  l.time_in,
  l.time_out,
  CASE
    WHEN l.time_in IS NULL OR l.time_out IS NULL THEN NULL
    ELSE FLOOR((
      (l.t + IF(l.t < 0, 1440, 0)) - IF((l.t + IF(l.t < 0, 1440, 0)) >= 540, 60, 0)
    ) / 60)
  END AS total_hours,
  CASE
    WHEN l.time_in IS NULL THEN 0
    WHEN TIME(l.time_in) > '08:00:00' THEN GREATEST(0, TIMESTAMPDIFF(MINUTE, '08:00:00', l.time_in))
    ELSE 0
  END,
  CASE
    WHEN l.undertime_minutes IS NULL THEN NULL
    ELSE FLOOR(l.undertime_minutes / 60)
  END,
  0, 0, 'NONE',
  CASE
    WHEN l.time_out IS NULL THEN 0
    WHEN TIME(l.time_out) < '17:00:00' THEN GREATEST(0, TIMESTAMPDIFF(MINUTE, l.time_out, '17:00:00'))
    ELSE 0
  END
FROM (
  SELECT
    al.*,
    TIMESTAMPDIFF(MINUTE, al.time_in, al.time_out) AS t
  FROM attendance_logs al
) l
WHERE NOT EXISTS (
  SELECT 1 FROM attendance a
  WHERE a.employee_id = CAST(l.employee_id AS CHAR(255)) AND a.`date` = l.log_date
);
*/

SELECT 'Uncomment INSERT after JOIN verification; use same DB user as app. Optional: add AND l.day_type = ''Workday'' in WHERE for migration scope.' AS instruction;
