-- =============================================================================
-- EAC: Verify single source of truth = table `attendance` (JPA AttendanceLog)
-- Run in: USE eac_hr_db;   (or your DB name from application.properties)
-- REST: GET /api/attendance/logs does NOT imply a table named attendance_logs.
-- Sample legacy shape (if your server has old `attendance_logs`): employee/eac_hr_db_attendance_logs.sql
-- =============================================================================

-- 1) Canonical table used by Spring (see com.example.employee.model.AttendanceLog)
SHOW CREATE TABLE attendance;

-- 2) Optional: legacy table (uncomment if it exists on your server)
-- SHOW CREATE TABLE attendance_logs;

-- 3) Row counts (compare after CSV import or migration)
SELECT 'attendance' AS tbl, COUNT(*) AS n FROM attendance;
-- SELECT 'attendance_logs' AS tbl, COUNT(*) AS n FROM attendance_logs;

-- 4) Recent imports (adjust employee_id to your internal id as string, e.g. '5')
-- SELECT id, employee_id, `date`, time_in, time_out, total_hours
-- FROM attendance WHERE employee_id = '5' ORDER BY `date` DESC LIMIT 15;

-- 5) Find internal id from EAC custom id (for step 4)
-- SELECT id, custom_employee_id FROM employee WHERE custom_employee_id = '1-00001';

-- 6) Optional: detect if legacy table exists (MySQL 5.7+ / 8)
-- SELECT TABLE_NAME, TABLE_ROWS
-- FROM information_schema.TABLES
-- WHERE TABLE_SCHEMA = DATABASE()
--   AND TABLE_NAME IN ('attendance', 'attendance_logs');

-- Expected JPA columns on `attendance` (Hibernate may add columns beyond payroll.sql dump):
-- id, employee_id, date, time_in, time_out, total_hours, minutes_late, undertime_hours,
-- overtime_hours, overtime_reported, ot_approval_status, minutes_early_out
