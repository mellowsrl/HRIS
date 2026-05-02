# TCMS v3 Attendance Configuration Backup

This file captures the schedule/roster setup visible in the provided TCMS screenshots.
Use this as the source of truth before removing schedules inside TCMS.

## Scope Captured

- Clocking Schedules:
  - `1 - full time` (listed, detailed tabs not shown)
  - `2 - Part Time Non-faculty` (Flexi)
  - `3 - Full Time Faculty` (Weekly)
  - `4 - Full Time Non-Faculty` (Weekly)
  - `5 - Flexi - Non Faculty` (Flexi)
  - `6 - Flexi - Faculty` (Flexi)
- Group Duty Roster:
  - `1 - New Roster`
  - `2 - Part Time`
  - `3 - Full Time Faculty`
  - `4 - Full Time Non-Faculty`
- Attendance sheet sample (March 2026) with computed columns visible (Work, Overtime, Short, etc.).

---

## A) Clocking Schedule: 2 - Part Time Non-faculty

### Basic

- Schedule ID: `2`
- Name: `Part Time Non-faculty`
- Work Schedule: `Flexi`

### Clocking Time Tab

- Monday-Friday: `Workday`
- Saturday-Sunday: `Restday`
- Time slots (In/Out columns): blank in screenshot (Flexi style)
- Enable attendance records from selected devices only: `Any` to `Any`
- Round to nearest minutes: appears `15`

### General Tab

- Capture first and last records as attendance: `Yes`
- Max in/out docking pairs for this flexi-hour schedule: `3`
- Other checkboxes/fields: not checked/blank in screenshot

### Rounding Tab

- Rounding fields visible but values not clearly filled (mostly blank)

### Break Time Tab

- Most options appear disabled/unchecked in screenshot

### Overtime Tab

- Overtime if total flexi-work hour exceeds workhour of: `8.00`
- Minimum minutes must work to qualify for overtime: `60`
- Maximum no. of hours allowed to claim overtime: `24.00`
- Work treat as Overtime:
  - Restday: checked
  - Offday: checked
  - Holiday: checked
- "Exclude short time if any" for those rows: unchecked

---

## B) Clocking Schedule: 3 - Full Time Faculty

### Basic

- Schedule ID: `3`
- Name: `Full Time Faculty`
- Work Schedule: `Weekly`

### Clocking Time Tab

- Monday-Friday: `Workday`
- Saturday-Sunday: `Restday`
- Monday-Friday `In`: `07:00 AM`
- Monday-Friday `Out`: `06:00 PM`
- Break/Resume/OT/Done in row cells: blank in screenshot
- Flexible break time in minutes: `60`
- Exclude break time from working hour: checked

### Clocking Range Tab

- Replace with latest clocking:
  - `Out`: checked
  - `Done`: checked
  - Others appear unchecked
- Selected devices for each slot: `Any`

### General / Rounding / Break Time / Overtime Tabs

- Not fully visible for this specific schedule in provided screenshots

---

## C) Clocking Schedule: 4 - Full Time Non-Faculty

### Basic

- Schedule ID: `4`
- Name: `Full Time Non-Faculty`
- Work Schedule: `Weekly`

### Clocking Time Tab

- Monday-Friday: `Workday`
- Saturday-Sunday: `Restday`
- Monday-Friday times:
  - In: `08:00 AM`
  - Break: `12:00 PM`
  - Resume: `01:00 PM`
  - Out: `05:00 PM`
- Allow grace period in minutes: `15`
- Exclude break time from working hour: checked

---

## D) Clocking Schedule: 5 - Flexi - Non Faculty

### Basic

- Schedule ID: `5`
- Name: `Flexi - Non Faculty`
- Work Schedule: `Flexi`

### Clocking Time Tab

- Monday-Friday: `Workday`
- Saturday-Sunday: `Restday`
- Time slots mostly blank (flexi style)
- Enable attendance records from selected devices only: `Any` to `Any`
- Flexible break time in minutes: `60`

### Other Tabs

- Not fully visible in provided screenshots

---

## E) Clocking Schedule: 6 - Flexi - Faculty

### Basic

- Schedule ID: `6`
- Name: `Flexi - Faculty`
- Work Schedule: `Flexi`

### Clocking Time Tab

- Monday-Friday: `Workday`
- Saturday-Sunday: `Restday`
- Time slots blank (flexi style)
- Device selectors visible as `Any` for slot filters

### Other Tabs

- Not fully visible in provided screenshots

---

## F) Group Duty Roster Summary

From roster list screenshot:

- Group 1: `New Roster` | Roster: `Weekly` | Total User: `1`
- Group 2: `Part Time` | Roster: `Weekly` | Total User: `1`
- Group 3: `Full Time Faculty` | Roster: `Weekly` | Total User: `2`
- Group 4: `Full Time Non-Faculty` | Roster: `Weekly` | Total User: `2`

From detailed monthly grid (Group ID 1 shown):

- Year shown: `2026`
- Grid uses numeric schedule/day codes per date (values like `1` appear across dates)
- Colored cells indicate special statuses/overrides (exact legend not visible in screenshot)

---

## G) Attendance Sheet Snapshot (Reference)

- Date filter shown: `Last Payroll Cycle`
- User filter: `ALL`
- Day Type filter: `ALL`
- Columns visible include:
  - Date, User ID, Name, Sched, Day Type
  - In, Break, Resume, Out, OT, Done
  - Work, Overtime, Short
  - Leave Type, RAW Hour, Work Code, Remark
- Example rows show restdays and weekdays, with computed Work/Overtime values.

---

## H) Import Priority Into Your Own System

When rebuilding from raw attendance + preserved policy:

1. Import `employees` (with TCMS user/enroll IDs)
2. Import `shifts` (schedule headers)
3. Import `shift_day_rules` (Mon-Sun day types and times)
4. Import `shift_policy` (grace, rounding, break, overtime rules)
5. Import `roster_assignments` (employee -> roster/shift by date)
6. Import raw attendance logs (timestamp punches)
7. Compute daily attendance output in your app

---

## I) Final Safety Checklist Before Deleting in TCMS

- Export all attendance logs (raw punches) for at least last 12 months
- Export users with enroll/user ID mapping
- Export or capture screenshots for every tab of each schedule (General/Rounding/Break/OT)
- Export roster assignment by employee and date
- Keep a copy of this backup file and raw CSV exports in cloud + local drive

