-- Optional: add columns expected by a full `payroll` schema (see payroll.sql).
-- If your table already has these, skip the matching line (MySQL will error on duplicate column).
-- Run: mysql -u root -p eac_hr_db
--
-- After applying, you may extend `getPayrollData` in AdmissionService.java to SELECT
-- taxable_income, withholding_tax and map them instead of using 0.

ALTER TABLE payroll ADD COLUMN taxable_income decimal(38,2) NULL;
ALTER TABLE payroll ADD COLUMN withholding_tax decimal(38,2) NULL;
