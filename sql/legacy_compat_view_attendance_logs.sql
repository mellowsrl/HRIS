-- =============================================================================
-- Optional compatibility for OLD tools that only need the *same* columns as `attendance`
-- and still run: SELECT ... FROM attendance_logs. If the old app expected log_date, status,
-- day_type, or other fields from the legacy eac `attendance_logs` (see
-- eac_hr_db_attendance_logs.sql), a simple SELECT from `attendance` cannot match — update that
-- tool to use `attendance` and map columns, or build a custom view.
-- This Spring app reads/writes table `attendance` only. Do not create this view until you
-- handle the existing physical table `attendance_logs` (if any):
--
--   A) If attendance_logs is empty or disposable:
--        RENAME TABLE attendance_logs TO attendance_logs_backup;
--        Then run the CREATE VIEW below.
--
--   B) If you must keep the old table, do NOT use this name — point your tool at `attendance`
--      or a different view name (e.g. v_legacy_logs) that you configure in the other app.
--
-- View column list must match what legacy SQL expects. Adjust aliases if your old table differed.
-- =============================================================================

-- CREATE OR REPLACE VIEW attendance_logs AS
-- SELECT
--   id,
--   employee_id,
--   `date`,
--   time_in,
--   time_out,
--   total_hours,
--   minutes_late,
--   undertime_hours,
--   overtime_hours,
--   overtime_reported,
--   ot_approval_status,
--   minutes_early_out
-- FROM attendance;

-- To remove later: DROP VIEW IF EXISTS attendance_logs;

SELECT 'View is commented: edit to match your legacy column expectations, then uncomment.' AS instruction;
