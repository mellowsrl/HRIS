-- Run against eac_hr_db to diagnose payslip hours vs ₱0 (see plan: getPayrollData + SP_ProcessRegularPayroll).
-- mysql -u root -p eac_hr_db < sql/verify_payslip_prerequisites.sql

-- 1) Stored procedure exists
SHOW PROCEDURE STATUS WHERE Db = DATABASE() AND Name = 'SP_ProcessRegularPayroll';

-- 2) Sample: compensation fields (replace 1 with employee_id PK from employee table)
SELECT employee_id, custom_employee_id, admin_pay, hourly_rate, employee_status
FROM employee
WHERE employee_id = 1;

-- 3) Sample: payroll row for first half of April 2026 (cutoff key 2026-4-1)
SELECT employee_id, pay_period_start, pay_period_end, gross_income, net_pay, total_earnings
FROM payroll
WHERE employee_id = 1
  AND pay_period_start = '2026-04-01'
  AND pay_period_end   = '2026-04-15';
