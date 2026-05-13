package com.example.employee.service;

import com.example.employee.model.Applicant;
import com.example.employee.model.HrAuditEvent;
import com.example.employee.model.OfficialEmployee;
import com.example.employee.model.PayrollPeriodPreset;
import com.example.employee.model.AttendanceLog;
import com.example.employee.model.DtrDayRow;
import com.example.employee.model.LeaveRequest;
import com.example.employee.model.LeaveRequestDisplayRow;
import com.example.employee.model.AppUser;
import com.example.employee.model.OvertimeApprovalListItem;
import com.example.employee.model.ShiftAssignment;
import com.example.employee.model.ShiftSchedule;
import com.example.employee.model.AttendanceViewModels.AdminAttendanceLogRow;
import com.example.employee.model.AttendanceViewModels.AttendanceDayStats;
import com.example.employee.model.AttendanceViewModels.EmployeeStatSummary;
import com.example.employee.util.ListSearchUtil;
import com.example.employee.util.PayrollDoleMath;
import com.example.employee.util.PayrollPeriodUtil;
import com.example.employee.util.PayrollDepartmentOptions;
import com.example.employee.model.SalaryAuditRow;
import com.example.employee.model.EacDepartment;
import com.example.employee.repository.ApplicantRepository;
import com.example.employee.repository.HrAuditEventRepository;
import com.example.employee.repository.OfficialEmployeeRepository;
import com.example.employee.repository.PayrollPeriodPresetRepository;
import com.example.employee.repository.AttendanceRepository;
import com.example.employee.repository.LeaveRequestRepository;
import com.example.employee.repository.AppUserRepository;
import com.example.employee.repository.ShiftAssignmentRepository;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.data.domain.PageRequest;
import org.springframework.web.multipart.MultipartFile;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.mail.SimpleMailMessage;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import jakarta.servlet.http.HttpServletResponse;
import com.lowagie.text.*;
import com.lowagie.text.pdf.*;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.time.YearMonth;
import java.time.temporal.ChronoUnit;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.UUID;
import java.util.stream.Collectors;
import java.util.stream.StreamSupport;
import java.time.format.DateTimeFormatter;
import java.time.format.DateTimeFormatterBuilder;
import java.time.format.DateTimeParseException;
import java.util.Locale;
import java.util.regex.Pattern;

@Service
public class AdmissionService {

    private static final Logger log = LoggerFactory.getLogger(AdmissionService.class);

    private static final List<String> STANDARD_LEAVE_TYPES = List.of(
        "Vacation Leave",
        "Sick Leave",
        "Service Incentive Leave",
        "Study Leave",
        "Bereavement Leave",
        "Solo Parent Leave",
        "Maternity Leave",
        "Paternity Leave",
        "Terminal (VL)"
    );

    @Autowired
    private ApplicantRepository repository;
    
    @Autowired
    private OfficialEmployeeRepository officialEmployeeRepository;

    @Autowired
    private DepartmentCodeService departmentCodeService;
    
    @Autowired
    private AttendanceRepository attendanceRepository;

    @Autowired
    private LeaveRequestRepository leaveRequestRepository;

    @Autowired
    private PayrollHolidaySuspensionService payrollHolidaySuspensionService;
    
    @Autowired
    private JdbcTemplate jdbcTemplate;

    @Autowired
    private JavaMailSender mailSender;

    @Value("${spring.mail.username:}")
    private String mailFromConfigured;
    
    @Autowired 
    private AppUserRepository appUserRepository;
    
    @Autowired 
    private PasswordEncoder passwordEncoder;

    @Autowired
    private HrAuditEventRepository hrAuditEventRepository;

    @Autowired
    private PayrollPeriodPresetRepository payrollPeriodPresetRepository;

    @Autowired
    private ShiftAssignmentRepository shiftAssignmentRepository;

    @Value("${app.upload.resume-dir:uploads/applicant-resume}")
    private String resumeUploadDir;

    @Value("${app.upload.employee-photo-dir:uploads/employee-photos}")
    private String employeePhotoUploadDir;

    private static final DateTimeFormatter DTR_US_DATE = DateTimeFormatter.ofPattern("MM/dd/yyyy");
    private static final DateTimeFormatter DTR_TIME_12 = DateTimeFormatter.ofPattern("h:mm a");

    private String lastUploadedFileName = "No file uploaded yet";
    private String lastUploadTime = "--";

    public String getLastUploadedFileName() { return lastUploadedFileName; }
    public String getLastUploadTime() { return lastUploadTime; }

    /** Counters for HR biometrics CSV import (TCMS / spreadsheet exports). */
    public record BiometricsImportResult(
            int dataRows,
            int imported,
            int skippedTooFewColumns,
            int skippedUnknownEmployee,
            int skippedParseError) {}

    /**
     * TCMS / Excel row layout (0-based, comma or semicolon). Matches the standard export:
     * 0 User ID · 1 First name · 2 Last name · 3 Employee ID (EAC) · 4 Date · 5 In · 6 Out ·
     * 7 Short · 8 OT · 9 LeaveType · 10 Work code · 11 Day type (e.g. Workday, Restday, Holiday).
     */
    private static final int TCMS_COL_USER = 0;
    private static final int TCMS_COL_EAC = 3;
    private static final int TCMS_COL_DATE = 4;
    private static final int TCMS_COL_IN = 5;
    private static final int TCMS_COL_OUT = 6;
    private static final int TCMS_COL_LEAVE = 9;
    private static final int TCMS_COL_DAY_TYPE = 11;
    /** Through column 9 (LeaveType) required; 10–11 optional (defaults). */
    private static final int TCMS_CSV_MIN_COLUMNS = 10;

    private static final DateTimeFormatter[] BIOMETRIC_DATE_FORMATTERS = {
            DateTimeFormatter.ofPattern("d/M/yyyy"),
            DateTimeFormatter.ofPattern("dd/MM/yyyy"),
            DateTimeFormatter.ofPattern("M/d/yyyy"),
            DateTimeFormatter.ofPattern("yyyy/M/d"),
            DateTimeFormatter.ofPattern("yyyy-MM-d"),
            DateTimeFormatter.ISO_LOCAL_DATE
    };

    private static final DateTimeFormatter[] BIOMETRIC_TIME_24 = {
            DateTimeFormatter.ofPattern("H:mm:ss"),
            DateTimeFormatter.ofPattern("H:mm")
    };

    private static final DateTimeFormatter BIOMETRIC_TIME_12 = new DateTimeFormatterBuilder()
            .parseCaseInsensitive()
            .appendPattern("h:mm[:ss] a")
            .toFormatter(Locale.ENGLISH);

    private static LocalDate parseBiometricCsvDate(String raw) {
        if (raw == null || raw.isBlank()) {
            throw new DateTimeParseException("empty date", raw == null ? "" : raw, 0);
        }
        String s = raw.trim();
        for (DateTimeFormatter f : BIOMETRIC_DATE_FORMATTERS) {
            try {
                return LocalDate.parse(s, f);
            } catch (java.time.DateTimeException ignored) {
                // try next format
            }
        }
        throw new DateTimeParseException("Unparseable date: " + s, s, 0);
    }

    private static LocalTime parseBiometricCsvTime(String raw) {
        if (raw == null || raw.isBlank()) {
            return null;
        }
        String s = raw.trim();
        for (DateTimeFormatter f : BIOMETRIC_TIME_24) {
            try {
                return LocalTime.parse(s, f);
            } catch (java.time.DateTimeException ignored) {
                // try next format
            }
        }
        return LocalTime.parse(s, BIOMETRIC_TIME_12);
    }

    private static String stripUtf8Bom(String line) {
        if (line == null || line.isEmpty()) {
            return line;
        }
        if (line.charAt(0) == '\uFEFF') {
            return line.substring(1);
        }
        return line;
    }

    /** Prefer comma; use semicolon when it clearly dominates (e.g. Excel in some locales). */
    private static char detectCsvDelimiter(String sampleLine) {
        if (sampleLine == null) {
            return ',';
        }
        long commas = sampleLine.chars().filter(c -> c == ',').count();
        long semis = sampleLine.chars().filter(c -> c == ';').count();
        return semis > commas ? ';' : ',';
    }

    /**
     * RFC-4180-style one-line parse: delimiter may be {@code ,} or {@code ;}, fields may be
     * quoted; {@code ""} inside quotes becomes {@code "}. No external dependency (avoids
     * {@code NoClassDefFoundError} for commons-csv in some deployments).
     */
    private static String[] parseTcmsCsvLine(String line, char delimiter) {
        if (line == null) {
            return new String[0];
        }
        List<String> fields = new ArrayList<>();
        StringBuilder field = new StringBuilder(32);
        boolean inQuotes = false;
        for (int i = 0; i < line.length(); i++) {
            char c = line.charAt(i);
            if (inQuotes) {
                if (c == '"') {
                    if (i + 1 < line.length() && line.charAt(i + 1) == '"') {
                        field.append('"');
                        i++;
                    } else {
                        inQuotes = false;
                    }
                } else {
                    field.append(c);
                }
            } else {
                if (c == '"') {
                    inQuotes = true;
                } else if (c == delimiter) {
                    fields.add(field.toString().trim());
                    field.setLength(0);
                } else {
                    field.append(c);
                }
            }
        }
        fields.add(field.toString().trim());
        return fields.toArray(new String[0]);
    }

    private static final Pattern EAC_ID_LIKE = Pattern.compile("\\d+\\s*-\\s*\\d+");

    private static boolean looksLikeHeaderInCol3(String c3) {
        String t = c3.trim().toLowerCase(Locale.ROOT);
        if (t.equals("eac id") || t.equals("eac_id") || t.equals("employee id") || t.equals("employeeid")) {
            return true;
        }
        return t.length() < 40 && t.contains("employee") && t.contains("id");
    }

    /**
     * If true, the row is likely a data row (TCMS id in col 0, EAC or id text in col 3), not a header line.
     */
    private static boolean looksLikeTcmsDataRow(String[] cols) {
        if (cols == null || cols.length < TCMS_CSV_MIN_COLUMNS) {
            return false;
        }
        String c0 = cols[TCMS_COL_USER].trim();
        String c3 = cols[TCMS_COL_EAC].trim();
        if (c3.isEmpty() || looksLikeHeaderInCol3(c3)) {
            return false;
        }
        try {
            Integer.parseInt(c0);
        } catch (NumberFormatException e) {
            return false;
        }
        if (EAC_ID_LIKE.matcher(c3).find()) {
            return true;
        }
        return c3.chars().anyMatch(Character::isDigit) && c3.length() >= 3;
    }

    private static String normalizeTcmsDayTypeString(String s) {
        if (s == null) {
            return "";
        }
        return s.replace('\u00a0', ' ').trim();
    }

    /**
     * True if the string looks like the TCMS "Day type" column (not work codes like "Normal").
     * Used when a row has 11 fields and index 10 might be day type.
     */
    private static boolean isLikelyTcmsDayTypeString(String t) {
        if (t == null || t.isBlank()) {
            return false;
        }
        String s = t.toLowerCase(Locale.ROOT);
        return s.contains("workday")
                || s.contains("restday")
                || s.contains("rest day")
                || s.equals("rest")
                || s.contains("holiday")
                || s.contains("rdo")
                || s.contains("day off")
                || s.contains("non-working");
    }

    /**
     * Reads column 11; if empty or the row is short (omitted trailing column), recovers "Restday", etc. when possible.
     * Many exports have exactly 11 fields; some omit an empty 12th field, so "Day type" was lost and defaulted to Workday.
     */
    private static String readTcmsDayTypeField(String[] data) {
        if (data == null) {
            return "Workday";
        }
        if (data.length > TCMS_COL_DAY_TYPE) {
            String t = normalizeTcmsDayTypeString(data[TCMS_COL_DAY_TYPE]);
            if (!t.isEmpty()) {
                return t;
            }
            String fromPrev = normalizeTcmsDayTypeString(data[10]);
            if (isLikelyTcmsDayTypeString(fromPrev)) {
                return fromPrev;
            }
        }
        if (data.length == 11) {
            String candidate = normalizeTcmsDayTypeString(data[10]);
            if (isLikelyTcmsDayTypeString(candidate)) {
                return candidate;
            }
        }
        return "Workday";
    }

    /** True when CSV "Day type" is rest / non-work or holiday (not undertime target). */
    private static boolean isTcmsRestOrHolidayDayType(String dayType) {
        if (dayType == null || dayType.isBlank()) {
            return false;
        }
        String d = dayType.toLowerCase(Locale.ROOT);
        if (d.contains("holiday")) {
            return true;
        }
        if (d.contains("restday") || d.contains("rest day")) {
            return true;
        }
        if ("rest".equals(d) || d.equals("rdo") || d.contains("day off") || d.contains("non-working")) {
            return true;
        }
        if (d.contains("weekly") && d.contains("rest")) {
            return true;
        }
        return false;
    }

    public void submitApplication(Applicant app, MultipartFile resumePdf) throws IOException {
        Path root = Path.of(resumeUploadDir).toAbsolutePath().normalize();
        Files.createDirectories(root);
        String filename = UUID.randomUUID() + ".pdf";
        Path target = root.resolve(filename);
        try (InputStream in = resumePdf.getInputStream()) {
            Files.copy(in, target, StandardCopyOption.REPLACE_EXISTING);
        }
        app.setResumeLink("/uploads/applicant-resume/" + filename);
        app.setYearsExperience(0);
        finishSubmitApplication(app);
    }

    private void finishSubmitApplication(Applicant app) {
        app.setAdmissionType("System Admission");
        applyStandardLeaveBalances(app);
        repository.save(app); 
        sendEmail(app.getEmail(), "EAC HR: Application Received", 
            "Dear " + app.getFirstName() + ",\n\nWe received your application. Status: PENDING REVIEW.");
    }

    /**
     * Standard policy leave credits (new application defaults + gender-based ML/PL). Used for admin bulk reset.
     */
    public void applyStandardLeaveBalances(OfficialEmployee e) {
        e.setVlBalance(15);
        e.setSlBalance(15);
        e.setBlBalance(3);
        e.setSplBalance(7);
        if (e.getGender() != null && "Female".equalsIgnoreCase(e.getGender().trim())) {
            e.setMlBalance(105);
            e.setPlBalance(0);
        } else {
            e.setMlBalance(0);
            e.setPlBalance(7);
        }
        e.setIncentiveLeaveBalance(5);
        e.setStudyLeaveBalance(0);
    }

    private void applyStandardLeaveBalances(Applicant app) {
        app.setVlBalance(15);
        app.setSlBalance(15);
        app.setBlBalance(3);
        app.setSplBalance(7);
        if ("Female".equalsIgnoreCase(app.getGender())) {
            app.setMlBalance(105);
            app.setPlBalance(0);
        } else {
            app.setMlBalance(0);
            app.setPlBalance(7);
        }
        app.setIncentiveLeaveBalance(5);
        app.setStudyLeaveBalance(0);
    }

    public void acceptApplicant(int id, String interviewDate, String interviewTime) {
        Optional<Applicant> result = repository.findById(id);
        if (result.isPresent()) {
            Applicant app = result.get();
            app.setStatus("ACCEPTED");
            repository.save(app); 
            String subject = "EAC HR: Application APPROVED - Interview Invitation";
            String body = "Dear " + app.getFirstName() + ",\n\nCongratulations! Your application has been APPROVED.\n\nDATE: " + interviewDate + "\nTIME: " + interviewTime + "\n\nEAC HR Team";
            sendEmail(app.getEmail(), subject, body);
        }
    }

    public void rejectApplicant(int id) {
        Optional<Applicant> result = repository.findById(id);
        if (result.isPresent()) {
            Applicant app = result.get();
            app.setStatus("REJECTED");
            repository.save(app);
        }
    }

    public void hireApplicant(int id, String contractDate, String contractTime) {
        Optional<Applicant> result = repository.findById(id);
        if (result.isPresent()) {
            Applicant app = result.get();
            
            OfficialEmployee newEmp = new OfficialEmployee();
            String officialId = generateEacEmployeeId(app);
            
            newEmp.setCustomEmployeeId(officialId);
            newEmp.setFirstName(app.getFirstName());
            newEmp.setLastName(app.getLastName());
            newEmp.setEmail(app.getEmail());
            newEmp.setPhone(app.getPhone());
            newEmp.setGender(app.getGender());
            newEmp.setPosition(app.getPositionApplied());
            newEmp.setDepartment(departmentCodeService.toCanonicalCode(app.getDepartment()));
            newEmp.setEmploymentType(app.getEmploymentType());
            newEmp.setPaymentType(app.getPaymentType());
            newEmp.setExpectedShift(app.getExpectedShift());
            newEmp.setDailyWage(app.getDailyWage());
            
            if (app.getDailyWage() != null) {
                newEmp.setAdminPay(PayrollDoleMath.monthlyFromDaily(app.getDailyWage()));
                newEmp.setHourlyRate(app.getDailyWage() / 8.0);
            }
            if (app.getHonorarium() != null) {
                newEmp.setHonorarium(app.getHonorarium());
            }
            if (app.getLongevity() != null) {
                newEmp.setLongevity(app.getLongevity());
            }
            
            newEmp.setVlBalance(app.getVlBalance());
            newEmp.setSlBalance(app.getSlBalance());
            newEmp.setMlBalance(app.getMlBalance());
            newEmp.setPlBalance(app.getPlBalance());
            newEmp.setSplBalance(app.getSplBalance());
            newEmp.setBlBalance(app.getBlBalance());
            newEmp.setIncentiveLeaveBalance(app.getIncentiveLeaveBalance());
            newEmp.setStudyLeaveBalance(app.getStudyLeaveBalance());
            newEmp.setEmergencyContactName(app.getEmergencyContactName());
            newEmp.setEmergencyContactPhone(app.getEmergencyContactPhone());
            newEmp.setHighestDegree(app.getHighestDegree());
            newEmp.setYearsExperience(app.getYearsExperience());
            newEmp.setExperienceText(app.getExperienceText());
            newEmp.setPreviousEmployer(app.getPreviousEmployer());
            newEmp.setResumeLink(app.getResumeLink());
            newEmp.setSssNumber(app.getSssNumber());
            newEmp.setTinNumber(app.getTinNumber());
            newEmp.setPhilhealthNumber(app.getPhilhealthNumber());
            newEmp.setPagibigNumber(app.getPagibigNumber());
            newEmp.setStatus("Active");

            LocalDate hiredOn = parseOptionalDate(contractDate);
            if (hiredOn != null) {
                newEmp.setDateHired(hiredOn);
            }

            officialEmployeeRepository.save(newEmp);

            if (appUserRepository.findByUsername(officialId).isEmpty()) {
                AppUser newEmployeeAccount = new AppUser();
                newEmployeeAccount.setUsername(officialId);
                newEmployeeAccount.setPassword(passwordEncoder.encode("password"));
                newEmployeeAccount.setRole("EMPLOYEE");
                appUserRepository.save(newEmployeeAccount);
            }
            
            repository.delete(app);
            
            String subject = "Job Offer: " + app.getPositionApplied() + " at Emilio Aguinaldo College";
            String body = "Dear " + app.getFirstName() + ",\n\n" +
                          "We are pleased to inform you that you have passed the selection process. " +
                          "We would like to formally offer you the position of " + app.getPositionApplied() + ".\n\n" +
                          "Please report to the Human Resource Management Office on " + contractDate + " at " + contractTime
                          + " for your final interview and employment onboarding (IDs, account setup, and next steps).\n\n" +
                          "IMPORTANT: Your Employee Portal Login has been generated.\n" +
                          "Username: " + officialId + "\n" +
                          "Password: password\n\n" +
                          "Welcome to the Emilio Aguinaldo College family!\n" +
                          "Virtus, Excelentia, Servitium.";
            
            sendEmail(app.getEmail(), subject, body);
        }
    }

    public void addEmployeeManually(OfficialEmployee app) {
        String officialId = generateEacEmployeeIdFallback(app);
        app.setCustomEmployeeId(officialId);
        app.setStatus("Active"); 
        app.setDepartment(departmentCodeService.toCanonicalCode(app.getDepartment()));
        
        if (app.getBasicSalary() != null && app.getBasicSalary() > 0) {
            app.setAdminPay(app.getBasicSalary());
            double d = PayrollDoleMath.dailyFromMonthlyBasic(app.getBasicSalary());
            app.setDailyWage(d);
            app.setHourlyRate(d / 8.0);
        } else if (app.getDailyWage() != null) {
            app.setAdminPay(PayrollDoleMath.monthlyFromDaily(app.getDailyWage()));
            app.setHourlyRate(app.getDailyWage() / 8.0);
        }

        officialEmployeeRepository.save(app); 

        if (appUserRepository.findByUsername(officialId).isEmpty()) {
            AppUser newEmployeeAccount = new AppUser();
            newEmployeeAccount.setUsername(officialId);
            newEmployeeAccount.setPassword(passwordEncoder.encode("password"));
            newEmployeeAccount.setRole("EMPLOYEE");
            appUserRepository.save(newEmployeeAccount);
        }
    }

    /**
     * Resets leave balances to standard policy for active employees in the given scope. Returns how many rows were updated.
     */
    @Transactional
    public int resetLeaveCredits(LeaveCreditResetMode mode, String departmentCode, List<Long> employeeIds) {
        final List<OfficialEmployee> targets = switch (mode) {
            case ALL -> {
                List<OfficialEmployee> list = new ArrayList<>();
                officialEmployeeRepository.findByStatus("Active").forEach(list::add);
                yield list;
            }
            case DEPARTMENT -> {
                if (departmentCode == null || departmentCode.isBlank()) {
                    yield List.of();
                }
                String deptF = departmentCodeService.toCanonicalCode(departmentCode);
                yield StreamSupport.stream(officialEmployeeRepository.findByStatus("Active").spliterator(), false)
                    .filter(e -> departmentCodeService.matchesFilter(e.getDepartment(), deptF))
                    .toList();
            }
            case SELECTED -> {
                if (employeeIds == null || employeeIds.isEmpty()) {
                    yield List.of();
                }
                yield officialEmployeeRepository.findAllById(employeeIds).stream()
                    .filter(e -> e.getStatus() != null && "Active".equalsIgnoreCase(e.getStatus().trim()))
                    .toList();
            }
        };
        for (OfficialEmployee e : targets) {
            applyStandardLeaveBalances(e);
        }
        if (!targets.isEmpty()) {
            officialEmployeeRepository.saveAll(targets);
        }
        return targets.size();
    }

    public List<Applicant> getDashboardApplicants(String statusFilter, String search) {
        List<Applicant> list;
        if (statusFilter == null || statusFilter.isEmpty() || statusFilter.equals("ALL")) {
            list = repository.findAllByOrderByIdDesc();
        } else {
            list = repository.findByStatusOrderByIdDesc(statusFilter);
        }
        if (!ListSearchUtil.isActiveKeyword(search)) {
            return list;
        }
        return list.stream().filter(a -> ListSearchUtil.matchesApplicant(a, search)).toList();
    }

    public long countApplicantsByStatus(String status) {
        return repository.countByStatus(status);
    }

    public record EmployeeListStats(long total, long active, long inactive, long newJoiners) {}

    /**
     * Probation / contract dates for the HR dashboard: within {@code upcomingDays} ahead,
     * or up to {@code overdueGraceDays} in the past (still active — follow up).
     */
    public record EmploymentMilestoneAlert(
        long employeeId,
        String customEmployeeId,
        String displayName,
        String milestoneKind,
        LocalDate eventDate,
        long daysFromToday
    ) {}

    public List<EmploymentMilestoneAlert> getEmploymentMilestoneAlerts(int upcomingDays, int overdueGraceDays) {
        LocalDate today = LocalDate.now();
        LocalDate upcomingLimit = today.plusDays(Math.max(0, upcomingDays));
        int grace = Math.max(0, overdueGraceDays);
        List<EmploymentMilestoneAlert> out = new ArrayList<>();
        for (OfficialEmployee e : officialEmployeeRepository.findByStatus("Active")) {
            String name = (e.getFirstName() != null ? e.getFirstName() : "") + " "
                + (e.getLastName() != null ? e.getLastName() : "");
            name = name.trim();
            addMilestoneIfRelevant(out, e, e.getProbationEndDate(), "Probation ends", name, today, upcomingLimit, grace);
            addMilestoneIfRelevant(out, e, e.getContractEndDate(), "Contract ends", name, today, upcomingLimit, grace);
        }
        out.sort(Comparator
            .comparing(EmploymentMilestoneAlert::eventDate)
            .thenComparing(EmploymentMilestoneAlert::displayName, String.CASE_INSENSITIVE_ORDER));
        return out.size() > 30 ? out.subList(0, 30) : out;
    }

    private static void addMilestoneIfRelevant(
            List<EmploymentMilestoneAlert> out,
            OfficialEmployee e,
            LocalDate eventDate,
            String kind,
            String displayName,
            LocalDate today,
            LocalDate upcomingLimit,
            int overdueGraceDays) {
        if (eventDate == null) {
            return;
        }
        long days = ChronoUnit.DAYS.between(today, eventDate);
        if (days > 0 && !eventDate.isAfter(upcomingLimit)) {
            out.add(new EmploymentMilestoneAlert(
                e.getId() != null ? e.getId() : 0L,
                e.getCustomEmployeeId(),
                displayName,
                kind,
                eventDate,
                days));
        } else if (days < 0 && days >= -overdueGraceDays) {
            out.add(new EmploymentMilestoneAlert(
                e.getId() != null ? e.getId() : 0L,
                e.getCustomEmployeeId(),
                displayName,
                kind,
                eventDate,
                days));
        }
    }

    public static LocalDate parseOptionalDate(String raw) {
        if (raw == null || raw.isBlank()) {
            return null;
        }
        try {
            return LocalDate.parse(raw.trim());
        } catch (DateTimeParseException e) {
            return null;
        }
    }

    private static String blankToNull(String value) {
        if (value == null) {
            return null;
        }
        String trimmed = value.trim();
        return trimmed.isEmpty() ? null : trimmed;
    }

    public EmployeeListStats getEmployeeListStats() {
        List<OfficialEmployee> all = new java.util.ArrayList<>();
        officialEmployeeRepository.findAll().forEach(all::add);
        long act = all.stream()
            .filter(e -> e.getStatus() != null && "Active".equalsIgnoreCase(e.getStatus().trim()))
            .count();
        long tot = all.size();
        long ina = tot - act;
        long maxId = all.stream().mapToLong(OfficialEmployee::getId).max().orElse(0L);
        long newJ = 0L;
        if (maxId > 0) {
            long cut = Math.max(1L, maxId - 14L);
            newJ = all.stream()
                .filter(e -> e.getStatus() != null && "Active".equalsIgnoreCase(e.getStatus().trim()))
                .filter(e -> e.getId() != null && e.getId() >= cut)
                .count();
        }
        return new EmployeeListStats(tot, act, ina, newJ);
    }

    // ── Dashboard today stats ────────────────────────────────────────────────

    public record TodayAttendanceStats(long present, long late, long onLeave, long absent,
                                       int presentPct, int latePct, int onLeavePct, int absentPct) {}

    public TodayAttendanceStats getTodayAttendanceStats() {
        LocalDate today = LocalDate.now();
        long present = attendanceRepository.countPresentToday(today);
        long late    = attendanceRepository.countLateToday(today);
        long onLeave = leaveRequestRepository.countOnLeaveToday(today);
        List<OfficialEmployee> allActive = new ArrayList<>();
        officialEmployeeRepository.findByStatus("Active").forEach(allActive::add);
        long totalActive = Math.max(allActive.size(), 1);
        long absent = Math.max(0, totalActive - present - onLeave);
        int presentPct  = (int) Math.round(100.0 * present  / totalActive);
        int latePct     = (int) Math.round(100.0 * late     / totalActive);
        int onLeavePct  = (int) Math.round(100.0 * onLeave  / totalActive);
        int absentPct   = Math.max(0, 100 - presentPct - onLeavePct);
        return new TodayAttendanceStats(present, late, onLeave, absent, presentPct, latePct, onLeavePct, absentPct);
    }

    public record BirthdayEntry(String name, String department, LocalDate birthDate, int daysUntil) {}

    public List<BirthdayEntry> getTodayBirthdays() {
        LocalDate today = LocalDate.now();
        return officialEmployeeRepository
            .findActiveByBirthMonthDay(today.getMonthValue(), today.getDayOfMonth())
            .stream()
            .map(e -> new BirthdayEntry(
                e.getFirstName() + " " + e.getLastName(),
                e.getDepartment(),
                e.getBirthDate(),
                0))
            .collect(Collectors.toList());
    }

    public List<BirthdayEntry> getUpcomingBirthdays(int days) {
        LocalDate today = LocalDate.now();
        List<BirthdayEntry> result = new ArrayList<>();
        for (int i = 1; i <= days; i++) {
            LocalDate d = today.plusDays(i);
            officialEmployeeRepository
                .findActiveByBirthMonthDay(d.getMonthValue(), d.getDayOfMonth())
                .forEach(e -> result.add(new BirthdayEntry(
                    e.getFirstName() + " " + e.getLastName(),
                    e.getDepartment(),
                    e.getBirthDate(),
                    (int) ChronoUnit.DAYS.between(today, d))));
        }
        result.sort(Comparator.comparingInt(BirthdayEntry::daysUntil).thenComparing(BirthdayEntry::name));
        return result;
    }

    public long countPendingLeaveRequests() {
        return leaveRequestRepository.countPendingLeaveRequests();
    }

    public long countPendingOvertimeThisMonth() {
        LocalDate start = LocalDate.now().withDayOfMonth(1);
        LocalDate end   = start.plusMonths(1).minusDays(1);
        return attendanceRepository.countTcmsOvertimePendingMonth(start, end);
    }

    /**
     * Non-active (archived) personnel: any status not equal to "Active" (e.g. HIRED, PENDING, REJECTED, Inactive).
     */
    public List<OfficialEmployee> getArchivedEmployees(String department, String searchKeyword) {
        List<OfficialEmployee> arch = officialEmployeeRepository.findAll().stream()
            .filter(e -> e.getStatus() == null || !"Active".equalsIgnoreCase(e.getStatus().trim()))
            .toList();
        if (department != null && !department.isEmpty()) {
            String deptF = departmentCodeService.toCanonicalCode(department);
            arch = arch.stream()
                .filter(e -> departmentCodeService.matchesFilter(e.getDepartment(), deptF))
                .toList();
        }
        if (ListSearchUtil.isActiveKeyword(searchKeyword)) {
            arch = arch.stream().filter(e -> ListSearchUtil.matchesEmployee(e, searchKeyword)).toList();
        }
        return arch;
    }

    public java.util.List<String> getDistinctDepartments() {
        return officialEmployeeRepository.findAll().stream()
            .map(OfficialEmployee::getDepartment)
            .filter(d -> d != null && !d.isBlank())
            .distinct()
            .sorted(String.CASE_INSENSITIVE_ORDER)
            .toList();
    }

    public List<EacDepartment> getEacDepartmentsForFilter() {
        return departmentCodeService.listAllForUi();
    }

    private static Set<String> facultyCodesFromMasterList() {
        Set<String> fac = new HashSet<>();
        for (PayrollDepartmentOptions.Row r : PayrollDepartmentOptions.ALL) {
            fac.add(r.code().toUpperCase(Locale.ROOT));
        }
        fac.add("SHS");
        return fac;
    }

    public List<Map<String, String>> buildFacultyDepartmentOptions() {
        Set<String> fac = facultyCodesFromMasterList();
        return departmentCodeService.listAllForUi().stream()
            .filter(d -> d.getCode() != null && fac.contains(d.getCode().toUpperCase(Locale.ROOT)))
            .map(d -> {
                Map<String, String> m = new LinkedHashMap<>();
                m.put("code", d.getCode());
                m.put("name", d.getName());
                return m;
            })
            .toList();
    }

    public List<Map<String, String>> buildNonFacultyDepartmentOptions() {
        Set<String> fac = facultyCodesFromMasterList();
        return departmentCodeService.listAllForUi().stream()
            .filter(d -> d.getCode() != null && !fac.contains(d.getCode().toUpperCase(Locale.ROOT)))
            .map(d -> {
                Map<String, String> m = new LinkedHashMap<>();
                m.put("code", d.getCode());
                m.put("name", d.getName());
                return m;
            })
            .toList();
    }

    public java.util.List<String> getDistinctCampusCodes() {
        return officialEmployeeRepository.findAll().stream()
            .map(OfficialEmployee::getCampusCode)
            .filter(c -> c != null && !c.isBlank())
            .distinct()
            .sorted()
            .toList();
    }

    public List<OfficialEmployee> getEmployees(String department, String searchKeyword) {
        List<OfficialEmployee> employees = (List<OfficialEmployee>) officialEmployeeRepository.findByStatus("Active");
        String deptFilter = null;
        if (department != null && !department.isEmpty()) {
            deptFilter = departmentCodeService.toCanonicalCode(department);
        }
        if (deptFilter != null && !deptFilter.isEmpty()) {
            String finalFilter = deptFilter;
            employees = employees.stream()
                .filter(e -> departmentCodeService.matchesFilter(e.getDepartment(), finalFilter))
                .toList();
        }
        if (ListSearchUtil.isActiveKeyword(searchKeyword)) {
            employees = employees.stream().filter(e -> ListSearchUtil.matchesEmployee(e, searchKeyword)).toList();
        }

        LocalDate today = LocalDate.now();
        for (OfficialEmployee emp : employees) {
            Optional<AttendanceLog> log = attendanceRepository.findByEmployeeIdAndDate(String.valueOf(emp.getId()), today);
            if (log.isEmpty()) {
                emp.setTodayStatus("Absent");
            } else if (log.get().getTimeOut() == null) {
                emp.setTodayStatus("Clocked In");
            } else {
                emp.setTodayStatus("Clocked Out");
            }
        }
        return employees;
    }

    public void updateEmployee(Long id, String department, String position, String status, Double dailyWage,
            Double basicSalary,
            int vlBalance, int slBalance, int mlBalance, int plBalance, int splBalance, int blBalance,
            int incentiveLeaveBalance, int studyLeaveBalance,
            String employmentType, String paymentType, String expectedShift, Integer biometricId,
            String dateHiredStr, String probationEndStr, String contractEndStr,
            String separationDateStr, String separationNoteStr,
            String gender, String civilStatus, String birthDateStr, String birthPlace, String nationality,
            String presentAddress, String permanentAddress, String emergencyContactName, String emergencyContactPhone,
            String emergencyContactRelationship, String secondaryEmergencyContactName, String secondaryEmergencyContactPhone) {

        Optional<OfficialEmployee> opt = officialEmployeeRepository.findById(id);
        if (opt.isPresent()) {
            OfficialEmployee app = opt.get();
            app.setDepartment(departmentCodeService.toCanonicalCode(department));
            app.setPosition(position);
            app.setStatus(status);
            app.setBasicSalary(basicSalary);
            app.setDateHired(parseOptionalDate(dateHiredStr));
            app.setProbationEndDate(parseOptionalDate(probationEndStr));
            app.setContractEndDate(parseOptionalDate(contractEndStr));
            app.setSeparationDate(parseOptionalDate(separationDateStr));
            if (separationNoteStr == null || separationNoteStr.isBlank()) {
                app.setSeparationNote(null);
            } else {
                String t = separationNoteStr.trim();
                app.setSeparationNote(t.length() > 500 ? t.substring(0, 500) : t);
            }
            if (basicSalary != null && basicSalary > 0) {
                app.setAdminPay(basicSalary);
                double d = PayrollDoleMath.dailyFromMonthlyBasic(basicSalary);
                app.setDailyWage(d);
                app.setHourlyRate(d / 8.0);
            } else if (dailyWage != null) {
                app.setDailyWage(dailyWage);
                app.setAdminPay(PayrollDoleMath.monthlyFromDaily(dailyWage));
                app.setHourlyRate(dailyWage / 8.0);
            }
            
            app.setVlBalance(vlBalance);
            app.setSlBalance(slBalance);
            app.setMlBalance(mlBalance);
            app.setPlBalance(plBalance);
            app.setSplBalance(splBalance);
            app.setBlBalance(blBalance);
            app.setIncentiveLeaveBalance(incentiveLeaveBalance); 
            app.setStudyLeaveBalance(studyLeaveBalance);         

            app.setEmploymentType(employmentType);
            app.setPaymentType(paymentType);
            app.setExpectedShift(expectedShift);
            app.setBiometricId(biometricId);
            app.setGender(blankToNull(gender));
            app.setCivilStatus(blankToNull(civilStatus));
            app.setBirthDate(parseOptionalDate(birthDateStr));
            app.setBirthPlace(blankToNull(birthPlace));
            app.setNationality(blankToNull(nationality));
            app.setPresentAddress(blankToNull(presentAddress));
            app.setPermanentAddress(blankToNull(permanentAddress));
            app.setEmergencyContactName(blankToNull(emergencyContactName));
            app.setEmergencyContactPhone(blankToNull(emergencyContactPhone));
            app.setEmergencyContactRelationship(blankToNull(emergencyContactRelationship));
            app.setSecondaryEmergencyContactName(blankToNull(secondaryEmergencyContactName));
            app.setSecondaryEmergencyContactPhone(blankToNull(secondaryEmergencyContactPhone));
            
            officialEmployeeRepository.save(app);
        }
    }

    public void deleteEmployee(Long id) {
        Optional<OfficialEmployee> opt = officialEmployeeRepository.findById(id);
        if (opt.isPresent()) {
            OfficialEmployee app = opt.get();
            appUserRepository.findByUsername(app.getCustomEmployeeId()).ifPresent(user -> {
                appUserRepository.delete(user);
            });
            officialEmployeeRepository.deleteById(id);
        }
    }

    public String processBiometrics(String employeeId, String action) {
        Optional<OfficialEmployee> optEmp = officialEmployeeRepository.findByCustomEmployeeId(employeeId);
        if (optEmp.isEmpty() || !"Active".equals(optEmp.get().getStatus())) {
            return "Error: Invalid or Inactive Employee ID.";
        }

        OfficialEmployee emp = optEmp.get();
        LocalDate today = LocalDate.now();
        LocalTime now = LocalTime.now();

        try {
            if ("TIME_IN".equalsIgnoreCase(action)) {
                jdbcTemplate.update("CALL SP_RecordTimeIn(?, ?, ?)", String.valueOf(emp.getId()), now, today);
                return "Time In successful for " + emp.getFirstName();
            } else if ("TIME_OUT".equalsIgnoreCase(action)) {
                jdbcTemplate.update("CALL SP_RecordTimeOut(?, ?, ?)", String.valueOf(emp.getId()), now, today);
                return "Time Out successful for " + emp.getFirstName();
            } else {
                return "Error: Invalid action.";
            }
        } catch (Exception e) {
            return "Error processing attendance: " + e.getMessage();
        }
    }

    @Scheduled(cron = "0 59 23 * * ?")
    public void midnightAutoTimeout() {
        LocalDate today = LocalDate.now();
        List<AttendanceLog> missingTimeOuts = attendanceRepository.findByTimeOutIsNullAndDate(today);
        
        for (AttendanceLog log : missingTimeOuts) {
            log.setTimeOut(LocalTime.of(23, 59)); 
            attendanceRepository.save(log);
        }
        System.out.println("EAC SYSTEM: Midnight Auto-Timeout executed. Flagged " + missingTimeOuts.size() + " records.");
    }

    public String processLeaveRequest(LeaveRequest req) {
        Optional<OfficialEmployee> optEmp = officialEmployeeRepository.findById((long)req.getEmployeeId());
        if (optEmp.isEmpty() || !"Active".equals(optEmp.get().getStatus())) {
            return "Error: Invalid or Inactive Employee ID.";
        }
        OfficialEmployee emp = optEmp.get();
        
        if ("Maternity Leave".equals(req.getLeaveType()) && !"Female".equalsIgnoreCase(emp.getGender())) {
            return "Error: Maternity Leave is strictly for female employees.";
        }
        if ("Paternity Leave".equals(req.getLeaveType()) && !"Male".equalsIgnoreCase(emp.getGender())) {
            return "Error: Paternity Leave is strictly for male employees.";
        }
        
        req.setStatus("PENDING");
        if (req.getNextApprover() == null || req.getNextApprover().isBlank()) {
            req.setNextApprover("HR");
        }
        req.setLastActionBy(null);
        req.setDecidedAt(null);
        leaveRequestRepository.save(req);
        return "Leave Request Submitted Successfully! Awaiting HR Approval.";
    }

    public record LeaveMonthStats(long approved, long rejected, long pending) {}

    public LeaveMonthStats getLeaveStatsForMonth(YearMonth ym) {
        LocalDate a = ym.atDay(1);
        LocalDate b = ym.atEndOfMonth();
        List<LeaveRequest> list = leaveRequestRepository.findOverlappingDateRange(a, b);
        long ap = 0, rj = 0, pe = 0;
        for (LeaveRequest r : list) {
            String s = r.getStatus() != null ? r.getStatus().trim() : "";
            if ("APPROVED".equalsIgnoreCase(s)) {
                ap++;
            } else if ("REJECTED".equalsIgnoreCase(s)) {
                rj++;
            } else if ("PENDING".equalsIgnoreCase(s)) {
                pe++;
            }
        }
        return new LeaveMonthStats(ap, rj, pe);
    }

    public LeaveMonthStats getLeaveStatsForEmployeeMonth(int employeeId, YearMonth ym) {
        LocalDate a = ym.atDay(1);
        LocalDate b = ym.atEndOfMonth();
        List<LeaveRequest> list = leaveRequestRepository.findByEmployeeIdOverlapping(employeeId, a, b);
        long ap = 0, rj = 0, pe = 0;
        for (LeaveRequest r : list) {
            String s = r.getStatus() != null ? r.getStatus().trim() : "";
            if ("APPROVED".equalsIgnoreCase(s)) {
                ap++;
            } else if ("REJECTED".equalsIgnoreCase(s)) {
                rj++;
            } else if ("PENDING".equalsIgnoreCase(s)) {
                pe++;
            }
        }
        return new LeaveMonthStats(ap, rj, pe);
    }

    public List<LeaveRequestDisplayRow> buildAdminLeaveList(LocalDate from, LocalDate to) {
        if (from == null || to == null || from.isAfter(to)) {
            return List.of();
        }
        List<LeaveRequestDisplayRow> rows = new ArrayList<>();
        for (LeaveRequest req : leaveRequestRepository.findOverlappingDateRange(from, to)) {
            Optional<OfficialEmployee> op = officialEmployeeRepository.findById((long) req.getEmployeeId());
            if (op.isEmpty()) {
                continue;
            }
            rows.add(toLeaveRow(req, op.get()));
        }
        return rows;
    }

    public List<LeaveRequestDisplayRow> buildEmployeeLeaveList(int employeeId, LocalDate from, LocalDate to) {
        if (from == null || to == null || from.isAfter(to)) {
            return List.of();
        }
        List<LeaveRequestDisplayRow> rows = new ArrayList<>();
        for (LeaveRequest req : leaveRequestRepository.findByEmployeeIdOverlapping(employeeId, from, to)) {
            Optional<OfficialEmployee> op = officialEmployeeRepository.findById((long) req.getEmployeeId());
            if (op.isEmpty()) {
                continue;
            }
            rows.add(toLeaveRow(req, op.get()));
        }
        return rows;
    }

    private LeaveRequestDisplayRow toLeaveRow(LeaveRequest req, OfficialEmployee emp) {
        int days = 0;
        if (req.getStartDate() != null && req.getEndDate() != null) {
            days = (int) ChronoUnit.DAYS.between(req.getStartDate(), req.getEndDate()) + 1;
        }
        String na = (req.getStatus() != null && "PENDING".equalsIgnoreCase(req.getStatus().trim()))
            ? (req.getNextApprover() != null && !req.getNextApprover().isBlank() ? req.getNextApprover() : "HR")
            : "—";
        String la = (req.getLastActionBy() != null && !req.getLastActionBy().isBlank()) ? req.getLastActionBy() : "—";
        return new LeaveRequestDisplayRow(
            req.getId(),
            emp.getId(),
            emp.getCustomEmployeeId() != null ? emp.getCustomEmployeeId() : "",
            emp.getFirstName() + " " + emp.getLastName(),
            emp.getDepartment() != null ? emp.getDepartment() : "—",
            emp.getPosition() != null ? emp.getPosition() : "—",
            (emp.getCampusCode() != null && !emp.getCampusCode().isBlank()) ? emp.getCampusCode().trim() : "—",
            req.getLeaveType() != null ? req.getLeaveType() : "—",
            req.getStartDate(),
            req.getEndDate(),
            days,
            labelLeaveStatus(req.getStatus()),
            na,
            la
        );
    }

    private static String labelLeaveStatus(String raw) {
        if (raw == null || raw.isBlank()) {
            return "—";
        }
        return switch (raw.trim().toUpperCase()) {
            case "PENDING" -> "Pending";
            case "APPROVED" -> "Approved";
            case "REJECTED" -> "Rejected";
            default -> raw;
        };
    }

    public List<String> getLeaveTypeFilterOptions() {
        LinkedHashSet<String> set = new LinkedHashSet<>(STANDARD_LEAVE_TYPES);
        for (String s : leaveRequestRepository.findDistinctLeaveTypes()) {
            if (s != null && !s.isBlank()) {
                set.add(s.trim());
            }
        }
        return new ArrayList<>(set);
    }

    public List<LeaveRequest> getPendingLeaves() {
        return getPendingLeaves(null);
    }

    public List<LeaveRequest> getPendingLeaves(String search) {
        List<LeaveRequest> all = leaveRequestRepository.findByStatusOrderByIdDesc("PENDING");
        if (!ListSearchUtil.isActiveKeyword(search)) {
            return all;
        }
        return all.stream().filter(req -> {
            Optional<OfficialEmployee> op = officialEmployeeRepository.findById((long) req.getEmployeeId());
            String name = op.map(e -> e.getFirstName() + " " + e.getLastName()).orElse("");
            String cid = op.map(OfficialEmployee::getCustomEmployeeId).orElse("");
            String hay = ListSearchUtil.buildHaystack(
                String.valueOf(req.getId()),
                String.valueOf(req.getEmployeeId()),
                cid,
                name,
                req.getLeaveType(),
                String.valueOf(req.getStartDate()),
                String.valueOf(req.getEndDate()),
                req.getReason()
            );
            return ListSearchUtil.matchesTokens(search, hay);
        }).toList();
    }

    public void approveLeave(int leaveId) {
        approveLeave(leaveId, "HR");
    }

    public void approveLeave(int leaveId, String approverLabel) {
        Optional<LeaveRequest> optReq = leaveRequestRepository.findById(leaveId);
        if (optReq.isEmpty()) {
            return;
        }
        LeaveRequest req = optReq.get();
        if (!"PENDING".equalsIgnoreCase(req.getStatus() != null ? req.getStatus().trim() : "")) {
            return;
        }
        req.setStatus("APPROVED");
        String by = (approverLabel != null && !approverLabel.isBlank()) ? approverLabel.trim() : "HR";
        req.setLastActionBy(by);
        req.setDecidedAt(LocalDateTime.now());
        req.setNextApprover("—");
        leaveRequestRepository.save(req);

        OfficialEmployee emp = officialEmployeeRepository.findById((long) req.getEmployeeId()).orElse(null);
            if (emp != null) {
                int days = (int) ChronoUnit.DAYS.between(req.getStartDate(), req.getEndDate()) + 1;
                
                String type = req.getLeaveType();
                if ("Vacation Leave".equals(type) || "Terminal (VL)".equals(type)) emp.setVlBalance(emp.getVlBalance() - days);
                else if ("Sick Leave".equals(type)) emp.setSlBalance(emp.getSlBalance() - days);
                else if ("Maternity Leave".equals(type)) emp.setMlBalance(emp.getMlBalance() - days);
                else if ("Paternity Leave".equals(type)) emp.setPlBalance(emp.getPlBalance() - days);
                else if ("Solo Parent Leave".equals(type)) emp.setSplBalance(emp.getSplBalance() - days);
                else if ("Bereavement Leave".equals(type)) emp.setBlBalance(emp.getBlBalance() - days);
                
                officialEmployeeRepository.save(emp);

                LocalDate currentDate = req.getStartDate();
                while (!currentDate.isAfter(req.getEndDate())) {
                    Optional<AttendanceLog> existingLogOpt = attendanceRepository.findByEmployeeIdAndDate(String.valueOf(emp.getId()), currentDate);
                    AttendanceLog paidLeaveLog = existingLogOpt.orElse(new AttendanceLog());

                    paidLeaveLog.setEmployeeId(String.valueOf(emp.getId()));
                    paidLeaveLog.setDate(currentDate);
                    paidLeaveLog.setTimeIn(LocalTime.of(8, 0)); 
                    paidLeaveLog.setTimeOut(LocalTime.of(17, 0));
                    paidLeaveLog.setTotalHours(8);
                    /** Same column as TCMS col 9 — must be set so attendance board + biometrics show "leave", not only a full shift. */
                    paidLeaveLog.setTcmsLeaveType(eacLeaveTypeToTcmsStatusAbbrev(req.getLeaveType()));
                    if (paidLeaveLog.getDayType() == null || paidLeaveLog.getDayType().isBlank()) {
                        paidLeaveLog.setDayType("Workday");
                    }
                    attendanceRepository.save(paidLeaveLog);
                    currentDate = currentDate.plusDays(1);
                }
            }
        if (emp != null && emp.getEmail() != null && !emp.getEmail().isBlank()) {
            sendEmail(emp.getEmail(),
                "EAC HR: Leave request APPROVED",
                "Your leave request was approved.\n\nType: " + req.getLeaveType()
                    + "\nDates: " + req.getStartDate() + " to " + req.getEndDate()
                    + "\n\n— EAC HR");
        }
        recordHrAudit(by, "LEAVE_APPROVED", "LeaveRequest", (long) leaveId,
            "type=" + req.getLeaveType() + " empId=" + req.getEmployeeId()
                + (emp != null ? " " + emp.getCustomEmployeeId() : ""));
    }

    public void rejectLeave(int leaveId) {
        rejectLeave(leaveId, "HR");
    }

    public void rejectLeave(int leaveId, String approverLabel) {
        Optional<LeaveRequest> optReq = leaveRequestRepository.findById(leaveId);
        if (optReq.isEmpty()) {
            return;
        }
        LeaveRequest req = optReq.get();
        if (!"PENDING".equalsIgnoreCase(req.getStatus() != null ? req.getStatus().trim() : "")) {
            return;
        }
        req.setStatus("REJECTED");
        String by = (approverLabel != null && !approverLabel.isBlank()) ? approverLabel.trim() : "HR";
        req.setLastActionBy(by);
        req.setDecidedAt(LocalDateTime.now());
        req.setNextApprover("—");
        leaveRequestRepository.save(req);
        OfficialEmployee remp = officialEmployeeRepository.findById((long) req.getEmployeeId()).orElse(null);
        if (remp != null && remp.getEmail() != null && !remp.getEmail().isBlank()) {
            sendEmail(remp.getEmail(),
                "EAC HR: Leave request not approved",
                "Your leave request was not approved.\n\nType: " + req.getLeaveType()
                    + "\nDates: " + req.getStartDate() + " to " + req.getEndDate()
                    + "\n\n— EAC HR");
        }
        recordHrAudit(by, "LEAVE_REJECTED", "LeaveRequest", (long) leaveId,
            "type=" + req.getLeaveType() + " empId=" + req.getEmployeeId()
                + (remp != null ? " " + remp.getCustomEmployeeId() : ""));
    }

    public java.util.Map<String, String> getRecentCutoffPeriods(int numberOfPeriods) {
        java.util.Map<String, String> periods = new java.util.LinkedHashMap<>();
        LocalDate current = LocalDate.now();
        int year = current.getYear();
        int month = current.getMonthValue();
        int half = current.getDayOfMonth() <= 15 ? 1 : 2;

        java.time.format.DateTimeFormatter formatter = java.time.format.DateTimeFormatter.ofPattern("MMM d");
        
        for (int i = 0; i < numberOfPeriods; i++) {
            String key = year + "-" + month + "-" + half;
            LocalDate startDate;
            LocalDate endDate;
            if (half == 1) {
                startDate = LocalDate.of(year, month, 1);
                endDate = LocalDate.of(year, month, 15);
            } else {
                startDate = LocalDate.of(year, month, 16);
                endDate = LocalDate.of(year, month, java.time.YearMonth.of(year, month).lengthOfMonth());
            }
            
            String label = (i == 0 ? "Current: " : "Historical: ") + 
                           startDate.format(formatter) + " - " + 
                           endDate.format(java.time.format.DateTimeFormatter.ofPattern("MMM d, yyyy"));
            periods.put(key, label);
            
            if (half == 2) {
                half = 1;
            } else {
                half = 2;
                month--;
                if (month == 0) { month = 12; year--; }
            }
        }
        return periods;
    }

    /**
     * Semi-monthly presets from {@link #getRecentCutoffPeriods(int)} plus saved named pay windows
     * (duplicates: saved entries win on label order first).
     */
    public java.util.Map<String, String> getCutoffOptionsWithPresets(int numberOfPeriods) {
        java.util.LinkedHashMap<String, String> m = new java.util.LinkedHashMap<>();
        for (PayrollPeriodPreset p : payrollPeriodPresetRepository.findByActiveTrueOrderByNameAsc()) {
            if (p.getCutoffKey() == null || p.getCutoffKey().isBlank() || p.getName() == null || p.getName().isBlank()) {
                continue;
            }
            m.put(p.getCutoffKey().trim(), "Saved: " + p.getName().trim());
        }
        m.putAll(getRecentCutoffPeriods(numberOfPeriods));
        return m;
    }

    public java.util.List<PayrollPeriodPreset> listAllPayrollPeriodPresets() {
        return payrollPeriodPresetRepository.findAll().stream()
            .sorted(Comparator.comparing(PayrollPeriodPreset::getName, String.CASE_INSENSITIVE_ORDER))
            .toList();
    }

    @Transactional
    public void savePayrollPeriodPreset(String name, String cutoffKey) {
        if (name == null || name.isBlank() || cutoffKey == null || cutoffKey.isBlank()) {
            return;
        }
        String key = cutoffKey.trim();
        PayrollPeriodUtil.resolve(key);
        PayrollPeriodPreset p = new PayrollPeriodPreset();
        p.setName(name.trim());
        p.setCutoffKey(key);
        p.setActive(true);
        p.setCreatedAt(java.time.Instant.now());
        payrollPeriodPresetRepository.save(p);
    }

    @Transactional
    public void deletePayrollPeriodPreset(long id) {
        payrollPeriodPresetRepository.deleteById(id);
    }

    @Transactional
    public void recordHrAudit(String actorUsername, String action, String entityType, Long entityId, String detail) {
        if (action == null || action.isBlank()) {
            return;
        }
        String d = detail;
        if (d != null && d.length() > 2000) {
            d = d.substring(0, 1997) + "...";
        }
        HrAuditEvent e = new HrAuditEvent(
            actorUsername != null && !actorUsername.isBlank() ? actorUsername.trim() : "—",
            action.trim(),
            entityType,
            entityId,
            d
        );
        hrAuditEventRepository.save(e);
    }

    public java.util.List<HrAuditEvent> listRecentHrAuditEvents(int maxRows) {
        int n = Math.min(Math.max(maxRows, 1), 500);
        return hrAuditEventRepository.findByOrderByIdDesc(PageRequest.of(0, n));
    }

    /** Current semi-monthly key, e.g. {@code 2026-4-1} (first half) or {@code 2026-4-2} (second half). */
    public String getCurrentPayrollCutoffKey() {
        LocalDate current = LocalDate.now();
        int year = current.getYear();
        int month = current.getMonthValue();
        int half = current.getDayOfMonth() <= 15 ? 1 : 2;
        return year + "-" + month + "-" + half;
    }

    /**
     * Read-only DOLE + semi-monthly breakdown for the salary audit page (no DB writes).
     */
    public List<SalaryAuditRow> listSalaryAudit(String cutoffKey, String search, String department) {
        PayrollPeriodUtil.PayrollPeriod period = PayrollPeriodUtil.resolve(cutoffKey);
        LocalDate start = period.start();
        LocalDate end = period.end();
        int n = period.inclusiveDayCount();
        String periodStartIso = start.toString();
        String periodEndIso = end.toString();
        java.time.format.DateTimeFormatter fmt = java.time.format.DateTimeFormatter.ofPattern("MMM d, yyyy", Locale.ENGLISH);
        String periodLabelNice = start.format(fmt) + " – " + end.format(fmt);
        final double periodFactor = n <= 16 ? 0.5 : 1.0;
        String periodRule;
        if (PayrollPeriodUtil.isStandardSemimonthlyWindow(period)) {
            periodRule = n <= 16
                ? "≤16 days in pay window: period basic = ½ × monthly (matches SP when days ≤ 16)"
                : ">16 days: period basic = 1 × monthly";
        } else {
            periodRule = String.format(Locale.ENGLISH,
                "Custom pay window: %d calendar day(s) from %s to %s. SP still uses ½× monthly if days in window ≤ 16, else 1× monthly (see SP_ProcessRegularPayroll).",
                n, periodStartIso, periodEndIso);
        }

        final double tolPeso = 0.5;
        List<OfficialEmployee> list = (List<OfficialEmployee>) officialEmployeeRepository.findByStatus("Active");
        if (department != null && !department.isBlank()) {
            String deptF = departmentCodeService.toCanonicalCode(department);
            list = list.stream()
                .filter(e -> departmentCodeService.matchesFilter(e.getDepartment(), deptF))
                .toList();
        }
        if (ListSearchUtil.isActiveKeyword(search)) {
            list = list.stream().filter(e -> ListSearchUtil.matchesEmployee(e, search)).toList();
        }

        List<SalaryAuditRow> out = new ArrayList<>(list.size());
        for (OfficialEmployee emp : list) {
            Double basic = emp.getBasicSalary();
            Double admin = emp.getAdminPay();
            double effective = 0.0;
            String src = "—";
            if (basic != null && basic > 0) {
                effective = basic;
                src = "basic_salary";
            } else if (admin != null && admin > 0) {
                effective = admin;
                src = "admin_pay";
            }
            double yearly = effective > 0 ? effective * 12.0 : 0.0;
            double dDaily = 0.0;
            double dHour = 0.0;
            double dMin = 0.0;
            if (effective > 0) {
                dDaily = PayrollDoleMath.dailyFromMonthlyBasic(effective);
                dHour = dDaily / 8.0;
                dMin = dHour / 60.0;
            }
            double periodBasic = effective * periodFactor;
            boolean ptf = isPartTimeOrFlexiPath(emp);
            String consistency;
            boolean mismatch = false;
            if (emp.getDailyWage() != null && emp.getAdminPay() != null) {
                double implied = PayrollDoleMath.monthlyFromDaily(emp.getDailyWage());
                double ap = emp.getAdminPay();
                if (Math.abs(implied - ap) > tolPeso) {
                    mismatch = true;
                    consistency = String.format(Locale.ENGLISH,
                        "Mismatch: DOLE implied monthly from daily_wage (%.2f) vs admin_pay (%.2f)", implied, ap);
                } else {
                    consistency = "OK (implied from daily_wage ≈ admin_pay, tol ₱" + tolPeso + ")";
                }
            } else if (emp.getDailyWage() == null) {
                consistency = "— (no daily_wage to cross-check admin_pay)";
            } else {
                consistency = "— (no admin_pay to cross-check from daily_wage)";
            }
            out.add(new SalaryAuditRow(
                emp.getId() != null ? emp.getId() : 0L,
                emp.getCustomEmployeeId() != null ? emp.getCustomEmployeeId() : "",
                ((emp.getFirstName() != null ? emp.getFirstName() : "")
                    + " " + (emp.getLastName() != null ? emp.getLastName() : "")).trim(),
                emp.getDepartment() != null ? emp.getDepartment() : "",
                emp.getEmploymentType() != null ? emp.getEmploymentType() : "",
                ptf,
                basic,
                admin,
                emp.getDailyWage(),
                emp.getHourlyRate(),
                src,
                effective,
                yearly,
                dDaily,
                dHour,
                dMin,
                n,
                periodStartIso,
                periodEndIso,
                periodLabelNice,
                periodBasic,
                periodRule,
                consistency,
                mismatch
            ));
        }
        return out;
    }

    private static boolean isPartTimeOrFlexiPath(OfficialEmployee e) {
        String s = e.getEmploymentType();
        if (s == null || s.isBlank()) {
            return false;
        }
        String l = s.toLowerCase(Locale.ROOT);
        return l.contains("part") || l.contains("flexi");
    }

    public List<OfficialEmployee> getPayrollData(String cutoffCode) {
        return getPayrollData(cutoffCode, null);
    }

    public List<OfficialEmployee> getPayrollData(String cutoffCode, String search) {
        List<OfficialEmployee> activeEmployees = (List<OfficialEmployee>) officialEmployeeRepository.findByStatus("Active");
        if (ListSearchUtil.isActiveKeyword(search)) {
            activeEmployees = activeEmployees.stream()
                .filter(e -> ListSearchUtil.matchesEmployee(e, search))
                .toList();
        }

        PayrollPeriodUtil.PayrollPeriod pr = PayrollPeriodUtil.resolve(cutoffCode);
        LocalDate startDate = pr.start();
        LocalDate endDate = pr.end();

        PayrollHolidaySuspensionService.PeriodContext holidaySuspensionCtx =
            payrollHolidaySuspensionService.loadPeriodContext(startDate, endDate);

        for (OfficialEmployee emp : activeEmployees) {
            emp.setPayrollWarning(null);
            double[] loanParts = sumActiveLoansSssHdmf(emp.getId());
            double appliedSssLoan = loanParts[0];
            double appliedHdmfLoan = loanParts[1];
            PayrollHolidaySuspensionService.PayrollExtras hx =
                payrollHolidaySuspensionService.computeForEmployee(emp, holidaySuspensionCtx);
            double adjustment = hx.adjustmentForSp();
            double holidayPay = hx.holidayPay();
            double teachingPayTotal = sumTeachingPayForPeriod(emp.getId(), startDate, endDate);
            int absentDays = 0;

            String empIdStr = String.valueOf(emp.getId());
            PayrollAttendanceAggregate att = aggregateAttendanceForPayroll(empIdStr, startDate, endDate);
            int lateMins = att.lateMins;
            int utMins = att.utMins;
            double otHours = att.otHours;
            double totalHoursWorked = att.totalHoursWorked;

            LeaveDaySplit leaveSplit = sumLeaveDaysForPeriod(emp.getId().intValue(), startDate, endDate);
            int leaveWithPayDays = leaveSplit.withPay;
            int leaveWithoutPayDays = leaveSplit.withoutPay;

            emp.setTotalHours(totalHoursWorked);
            emp.setOtHours(otHours);
            
            double honorarium = emp.getHonorarium() != null ? emp.getHonorarium() : 0.0;
            double longevity = emp.getLongevity() != null ? emp.getLongevity() : 0.0;

            try {
                jdbcTemplate.update("CALL SP_ProcessRegularPayroll(?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?)",
                    emp.getId(), startDate, endDate, appliedSssLoan, appliedHdmfLoan, adjustment, honorarium, longevity,
                    lateMins, utMins, absentDays, otHours, holidayPay, leaveWithPayDays, leaveWithoutPayDays, teachingPayTotal, totalHoursWorked);
                    
                // Columns must match the live `payroll` table. Older DBs may lack taxable_income / withholding_tax; add via sql/alter_payroll_tax_columns.sql then extend this SELECT.
                String sql = "SELECT gross_income, sss_deduction AS gov_sss, philhealth_deduction, pagibig_deduction, net_pay, "
                           + "total_earnings, loan_deductions "
                           + "FROM payroll WHERE employee_id = ? AND pay_period_start = ? AND pay_period_end = ?";
                             
                List<Map<String, Object>> rows = jdbcTemplate.queryForList(sql, emp.getId(), startDate, endDate);
                
                if (!rows.isEmpty()) {
                    Map<String, Object> row = rows.get(0);
                    emp.setGrossPay(toDoubleAmount(row.get("gross_income")));
                    emp.setSssDeduction(toDoubleAmount(row.get("gov_sss")));
                    emp.setPhilhealthDeduction(toDoubleAmount(row.get("philhealth_deduction")));
                    emp.setPagibigDeduction(toDoubleAmount(row.get("pagibig_deduction")));
                    emp.setNetPay(toDoubleAmount(row.get("net_pay")));
                    emp.setPayrollTotalEarnings(toDoubleAmount(row.get("total_earnings")));
                    emp.setPayrollLoanDeductions(toDoubleAmount(row.get("loan_deductions")));
                    emp.setPayrollTaxableIncome(0.0);
                    emp.setPayrollWithholdingTax(0.0);
                } else {
                    emp.setGrossPay(0.0);
                    emp.setNetPay(0.0);
                    emp.setPayrollTotalEarnings(0.0);
                    emp.setPayrollTaxableIncome(0.0);
                    emp.setPayrollWithholdingTax(0.0);
                    emp.setPayrollLoanDeductions(0.0);
                    emp.setPayrollWarning("No payroll row was found in the database for this period after the payroll run. "
                        + "Ask HR to confirm that SP_ProcessRegularPayroll is installed and that the payroll table is updated for this window.");
                }

                if (emp.getGrossPay() == 0 && totalHoursWorked > 0) {
                    boolean noRate = (emp.getAdminPay() == null || emp.getAdminPay() <= 0)
                        && (emp.getHourlyRate() == null || emp.getHourlyRate() <= 0);
                    if (noRate && emp.getPayrollWarning() == null) {
                        emp.setPayrollWarning("Time was recorded for this period, but your employee record has no admin pay and no hourly rate, so the payroll run can show zero pay. Ask HR to set your rate.");
                    }
                }
                
                double monthBase = 0.0;
                if (emp.getBasicSalary() != null && emp.getBasicSalary() > 0) {
                    monthBase = emp.getBasicSalary();
                } else if (emp.getAdminPay() != null) {
                    monthBase = emp.getAdminPay();
                }
                emp.setMonthlySalary(monthBase);
                
            } catch (Exception e) {
                log.error("Payroll Error for Employee {}: {}", emp.getId(), e.getMessage(), e);
                emp.setPayrollWarning("Payroll could not be calculated (database or stored procedure error). "
                    + "Contact HR with your EAC ID and cutoff period, or have IT check server logs for this message: Payroll Error for Employee "
                    + emp.getId());
            }
        }
        
        return activeEmployees;
    }

    /**
     * Sums {@code total_teaching_pay} from {@code teaching_pay} for the same pay window passed to
     * {@code SP_ProcessRegularPayroll} ({@code payroll.sql} <code>teaching_pay</code> table: {@code period_start},
     * {@code period_end}).
     */
    private double sumTeachingPayForPeriod(long employeeId, LocalDate periodStart, LocalDate periodEnd) {
        try {
            String sql = "SELECT COALESCE(SUM(COALESCE(total_teaching_pay, 0)), 0) AS t "
                       + "FROM teaching_pay "
                       + "WHERE employee_id = ? AND period_start = ? AND period_end = ?";
            Map<String, Object> row = jdbcTemplate.queryForMap(sql, employeeId, periodStart, periodEnd);
            return toDoubleAmount(row.get("t"));
        } catch (Exception e) {
            log.debug("teaching_pay sum for employee {} period {}-{}: {}", employeeId, periodStart, periodEnd, e.getMessage());
            return 0.0;
        }
    }

    private static double toDoubleAmount(Object v) {
        if (v == null) return 0.0;
        if (v instanceof java.math.BigDecimal) return ((java.math.BigDecimal) v).doubleValue();
        if (v instanceof Number) return ((Number) v).doubleValue();
        return 0.0;
    }

    /**
     * Maps {@code loans} rows to SSS vs HDMF payment amounts for SP_ProcessRegularPayroll.
     * Linked by email to {@code employee}; classify by {@code loan_type} keywords; excludes rejected-like statuses.
     * Returns [sssLoan, hdmfLoan] amounts (per-period amounts as stored in loan rows).
     */
    private double[] sumActiveLoansSssHdmf(long employeeId) {
        double[] out = {0.0, 0.0};
        try {
            String sql = "SELECT "
                + "COALESCE(SUM(CASE WHEN LOWER(IFNULL(l.loan_type, '')) LIKE '%sss%' "
                + "  OR LOWER(IFNULL(l.loan_type, '')) LIKE '%s.s.s%' "
                + "  OR LOWER(IFNULL(l.loan_type, '')) LIKE '%social security%' "
                + "  THEN l.amount ELSE 0 END), 0) AS loan_sss_sum, "
                + "COALESCE(SUM(CASE WHEN LOWER(IFNULL(l.loan_type, '')) LIKE '%hdmf%' "
                + "  OR LOWER(IFNULL(l.loan_type, '')) LIKE '%pag-ibig%' "
                + "  OR LOWER(IFNULL(l.loan_type, '')) LIKE '%pagibig%' "
                + "  THEN l.amount ELSE 0 END), 0) AS loan_hdmf_sum "
                + "FROM loans l "
                + "INNER JOIN employee e ON e.email = l.email "
                + "WHERE e.employee_id = ? "
                + "AND l.email IS NOT NULL AND TRIM(l.email) <> '' "
                + "AND (l.status IS NULL OR LOWER(TRIM(l.status)) NOT IN ("
                + "'rejected', 'denied', 'cancelled', 'closed'))";
            Map<String, Object> row = jdbcTemplate.queryForMap(sql, employeeId);
            out[0] = toDoubleAmount(row.get("loan_sss_sum"));
            out[1] = toDoubleAmount(row.get("loan_hdmf_sum"));
        } catch (Exception e) {
            log.debug("Loan lookup skipped for employee {}: {}", employeeId, e.getMessage());
        }
        return out;
    }

    private static final class PayrollAttendanceAggregate {
        int lateMins;
        int utMins;
        double otHours;
        double totalHoursWorked;
    }

    private PayrollAttendanceAggregate aggregateAttendanceForPayroll(String employeeIdStr, LocalDate start, LocalDate end) {
        PayrollAttendanceAggregate a = new PayrollAttendanceAggregate();
        try {
            String sql = "SELECT COALESCE(SUM(COALESCE(minutes_late, 0)), 0) AS late_mins, "
                       + "COALESCE(SUM(COALESCE(undertime_hours, 0) * 60), 0) AS ut_mins, "
                       + "COALESCE(SUM(COALESCE(overtime_hours, 0)), 0) AS ot_sum, "
                       + "COALESCE(SUM(COALESCE(total_hours, 0)), 0) AS th_sum "
                       + "FROM attendance WHERE employee_id = ? AND `date` >= ? AND `date` <= ?";
            Map<String, Object> row = jdbcTemplate.queryForMap(sql, employeeIdStr, start, end);
            a.lateMins = toIntAmount(row.get("late_mins"));
            a.utMins = toIntAmount(row.get("ut_mins"));
            a.otHours = toDoubleAmount(row.get("ot_sum"));
            a.totalHoursWorked = toDoubleAmount(row.get("th_sum"));
        } catch (Exception e) {
            log.warn("Attendance aggregate failed for employee {}: {}", employeeIdStr, e.getMessage());
        }
        return a;
    }

    private static int toIntAmount(Object v) {
        if (v == null) return 0;
        if (v instanceof java.math.BigDecimal) return ((java.math.BigDecimal) v).intValue();
        if (v instanceof Number) return ((Number) v).intValue();
        return 0;
    }

    private record LeaveDaySplit(int withPay, int withoutPay) {}

    private LeaveDaySplit sumLeaveDaysForPeriod(int employeeId, LocalDate periodStart, LocalDate periodEnd) {
        int withPay = 0;
        int withoutPay = 0;
        for (LeaveRequest req : leaveRequestRepository.findByEmployeeIdOrderByIdDesc(employeeId)) {
            if (!"APPROVED".equals(req.getStatus())) continue;
            int days = countOverlapDays(req.getStartDate(), req.getEndDate(), periodStart, periodEnd);
            if (days <= 0) continue;
            if (isPaidLeaveType(req.getLeaveType())) {
                withPay += days;
            } else {
                withoutPay += days;
            }
        }
        return new LeaveDaySplit(withPay, withoutPay);
    }

    private static int countOverlapDays(LocalDate leaveStart, LocalDate leaveEnd, LocalDate periodStart, LocalDate periodEnd) {
        if (leaveStart == null || leaveEnd == null) return 0;
        LocalDate s = leaveStart.isBefore(periodStart) ? periodStart : leaveStart;
        LocalDate e = leaveEnd.isAfter(periodEnd) ? periodEnd : leaveEnd;
        if (s.isAfter(e)) return 0;
        return (int) ChronoUnit.DAYS.between(s, e) + 1;
    }

    public void exportPayslipToPDF(int employeeId, String cutoffCode, HttpServletResponse response) {
        try {
            String code = (cutoffCode != null && !cutoffCode.isEmpty()) ? cutoffCode : null;
            OfficialEmployee emp = getPayrollData(code).stream().filter(a -> a.getId() == employeeId).findFirst().orElse(null);
            if (emp == null) return;
            
            Document document = new Document(PageSize.A4);
            PdfWriter.getInstance(document, response.getOutputStream());
            document.open();

            java.awt.Color eacRed = new java.awt.Color(214, 0, 0);
            java.awt.Color eacDark = new java.awt.Color(45, 52, 54);
            Font titleFont = FontFactory.getFont(FontFactory.HELVETICA_BOLD, 24, eacRed);
            Font subFont = FontFactory.getFont(FontFactory.HELVETICA_BOLD, 12, eacDark);
            Font headerFont = FontFactory.getFont(FontFactory.HELVETICA_BOLD, 10, java.awt.Color.WHITE);
            Font normalFont = FontFactory.getFont(FontFactory.HELVETICA, 10, eacDark);
            Font boldFont = FontFactory.getFont(FontFactory.HELVETICA_BOLD, 10, eacDark);

            Paragraph title = new Paragraph("EMILIO AGUINALDO COLLEGE", titleFont);
            title.setAlignment(Element.ALIGN_CENTER);
            document.add(title);
            
            Paragraph subTitle = new Paragraph("OFFICIAL EMPLOYEE PAYSLIP", subFont);
            subTitle.setAlignment(Element.ALIGN_CENTER);
            subTitle.setSpacingAfter(20f);
            document.add(subTitle);

            PdfPTable infoTable = new PdfPTable(4);
            infoTable.setWidthPercentage(100);
            infoTable.setSpacingAfter(20f);
            
            infoTable.addCell(createCell("Employee ID:", boldFont, false));
            infoTable.addCell(createCell(emp.getCustomEmployeeId(), normalFont, false));
            infoTable.addCell(createCell("Department:", boldFont, false));
            infoTable.addCell(createCell(emp.getDepartment(), normalFont, false));
            
            infoTable.addCell(createCell("Name:", boldFont, false));
            infoTable.addCell(createCell(emp.getFirstName() + " " + emp.getLastName(), normalFont, false));
            infoTable.addCell(createCell("Position:", boldFont, false));
            infoTable.addCell(createCell(emp.getPosition(), normalFont, false));
            document.add(infoTable);

            PdfPTable finTable = new PdfPTable(2);
            finTable.setWidthPercentage(100);
            finTable.setWidths(new float[]{3f, 1f});

            finTable.addCell(createHeaderCell("EARNINGS & HOURS", headerFont, eacDark));
            finTable.addCell(createHeaderCell("AMOUNT (PHP)", headerFont, eacDark));

            finTable.addCell(createCell("Base Wage Rate", normalFont, true));
            finTable.addCell(createRightCell(String.format("%.2f", emp.getDailyWage() != null ? emp.getDailyWage() : 0.0), normalFont));
            finTable.addCell(createCell("Total Regular Hours Logged (" + String.format("%.2f", emp.getTotalHours()) + " hrs)", normalFont, true));
            finTable.addCell(createRightCell(String.format("%.2f", Math.max(0, emp.getTotalHours() - emp.getOtHours())), normalFont)); 
            finTable.addCell(createCell("Overtime Hours Logged (" + String.format("%.2f", emp.getOtHours()) + " hrs)", normalFont, true));
            finTable.addCell(createRightCell(String.format("%.2f", emp.getOtHours()), normalFont));

            finTable.addCell(createCell("GROSS EARNINGS", boldFont, true));
            finTable.addCell(createRightCell(String.format("%.2f", emp.getGrossPay()), boldFont));

            finTable.addCell(createHeaderCell("MANDATORY DEDUCTIONS", headerFont, eacRed));
            finTable.addCell(createHeaderCell("AMOUNT (PHP)", headerFont, eacRed));

            finTable.addCell(createCell("SSS Contribution", normalFont, true));
            finTable.addCell(createRightCell("-" + String.format("%.2f", emp.getSssDeduction()), normalFont));
            finTable.addCell(createCell("PhilHealth Contribution", normalFont, true));
            finTable.addCell(createRightCell("-" + String.format("%.2f", emp.getPhilhealthDeduction()), normalFont));
            finTable.addCell(createCell("Pag-IBIG Fund", normalFont, true));
            finTable.addCell(createRightCell("-" + String.format("%.2f", emp.getPagibigDeduction()), normalFont));
            finTable.addCell(createCell("Withholding tax", normalFont, true));
            finTable.addCell(createRightCell("-" + String.format("%.2f", emp.getPayrollWithholdingTax()), normalFont));
            finTable.addCell(createCell("Loan deductions (per payroll row)", normalFont, true));
            finTable.addCell(createRightCell("-" + String.format("%.2f", emp.getPayrollLoanDeductions()), normalFont));

            finTable.addCell(createCell("SP total earnings", normalFont, true));
            finTable.addCell(createRightCell(String.format("%.2f", emp.getPayrollTotalEarnings()), normalFont));

            document.add(finTable);

            Paragraph netPay = new Paragraph("NET TAKE-HOME PAY: PHP " + String.format("%.2f", emp.getNetPay()), FontFactory.getFont(FontFactory.HELVETICA_BOLD, 16, new java.awt.Color(0, 150, 0)));
            netPay.setAlignment(Element.ALIGN_RIGHT);
            netPay.setSpacingBefore(15f);
            document.add(netPay);

            document.close();
        } catch (Exception e) { System.err.println("PDF Error: " + e.getMessage()); }
    }

    public List<AttendanceLog> getEmployeeAttendanceHistory(int employeeId) {
        return attendanceRepository.findByEmployeeIdOrderByDateDesc(String.valueOf(employeeId));
    }

    public List<DtrDayRow> buildDtrRows(int employeeId, LocalDate from, LocalDate to) {
        if (from == null || to == null || from.isAfter(to)) {
            return List.of();
        }
        Map<LocalDate, AttendanceLog> byDate = getEmployeeAttendanceHistory(employeeId).stream()
            .filter(l -> l.getDate() != null)
            .collect(Collectors.toMap(AttendanceLog::getDate, l -> l, (a, b) -> a));
        List<DtrDayRow> rows = new ArrayList<>();
        for (LocalDate d = from; !d.isAfter(to); d = d.plusDays(1)) {
            AttendanceLog log = byDate.get(d);
            if (log == null) {
                rows.add(new DtrDayRow(
                    d.format(DTR_US_DATE),
                    false,
                    "—", "—", "—", "—",
                    "—", "—", "—",
                    "NO_LOG",
                    "—"
                ));
            } else {
                rows.add(toDtrDayRow(log));
            }
        }
        return rows;
    }

    private DtrDayRow toDtrDayRow(AttendanceLog log) {
        String in1 = log.getTimeIn() != null ? log.getTimeIn().format(DTR_TIME_12) : "—";
        String out1 = log.getTimeOut() != null ? log.getTimeOut().format(DTR_TIME_12) : "—";
        String in2 = "—";
        String out2 = "—";
        int lateM = log.getMinutesLate() != null ? log.getMinutesLate() : 0;
        String late = lateM > 0 ? lateM + " mins" : "—";
        int utH = log.getUndertimeHours() != null ? log.getUndertimeHours() : 0;
        int earlyM = log.getMinutesEarlyOut() != null ? log.getMinutesEarlyOut() : 0;
        String ut = "—";
        if (utH > 0) {
            ut = utH + (utH == 1 ? " hr" : " hrs");
        } else if (earlyM > 0) {
            ut = earlyM + " mins";
        }
        int oth = log.getOvertimeHours() != null ? log.getOvertimeHours() : 0;
        int otr = log.getOvertimeReported() != null ? log.getOvertimeReported() : 0;
        int otH = oth > 0 ? oth : otr;
        String ots = otH > 0 ? otH + (otH == 1 ? " hr" : " hrs") : "—";
        boolean penalty = lateM > 0 || utH > 0 || earlyM > 0;
        String rem = penalty ? "PENALTY" : "OK";
        String dayLine = log.getCalendarDayStatusLine();
        return new DtrDayRow(
            log.getDate() != null ? log.getDate().format(DTR_US_DATE) : "—",
            true,
            in1, out1, in2, out2,
            late, ut, ots,
            rem,
            dayLine != null ? dayLine : "—"
        );
    }

    public void saveEmployeeProfilePhoto(OfficialEmployee emp, MultipartFile file) throws IOException {
        if (file == null || file.isEmpty() || emp == null) {
            return;
        }
        String contentType = file.getContentType();
        if (contentType == null
            || (!contentType.equalsIgnoreCase("image/jpeg")
                && !contentType.equalsIgnoreCase("image/png")
                && !contentType.equalsIgnoreCase("image/webp"))) {
            throw new IllegalArgumentException("Please upload a JPEG, PNG, or WebP image.");
        }
        String orig = file.getOriginalFilename() != null ? file.getOriginalFilename() : "photo";
        String ext = orig.contains(".") ? orig.substring(orig.lastIndexOf('.')) : ".jpg";
        if (ext.length() > 8) {
            ext = ".jpg";
        }
        String name = UUID.randomUUID() + ext;
        Path dir = Path.of(employeePhotoUploadDir).toAbsolutePath().normalize();
        Files.createDirectories(dir);
        Path target = dir.resolve(name);
        Files.copy(file.getInputStream(), target, StandardCopyOption.REPLACE_EXISTING);
        emp.setProfilePhotoPath("/uploads/employee-photos/" + name);
        officialEmployeeRepository.save(emp);
    }

    public List<AttendanceLog> getEmployeeAttendanceHistoryByCutoff(int employeeId, String cutoffCode) {
        List<AttendanceLog> allLogs = getEmployeeAttendanceHistory(employeeId);
        LocalDate[] range = resolveCutoffRange(cutoffCode);
        LocalDate startDate = range[0];
        LocalDate endDate = range[1];

        return allLogs.stream()
            .filter(log -> !log.getDate().isBefore(startDate) && !log.getDate().isAfter(endDate))
            .toList();
    }

    private LocalDate[] resolveCutoffRange(String cutoffCode) {
        PayrollPeriodUtil.PayrollPeriod p = PayrollPeriodUtil.resolve(cutoffCode);
        return new LocalDate[] { p.start(), p.end() };
    }

    /**
     * When {@code applyMode} is {@code "custom"} and both dates are set, builds {@code YYYY-MM-DD_YYYY-MM-DD};
     * otherwise uses {@code selectedCutoff} if non-blank, or current calendar semi-month.
     */
    public String effectivePayrollCutoffKey(String selectedCutoff,
            java.time.LocalDate customFrom, java.time.LocalDate customTo, String applyMode) {
        if ("custom".equals(applyMode) && customFrom != null && customTo != null) {
            java.time.LocalDate s = customFrom;
            java.time.LocalDate e = customTo;
            if (s.isAfter(e)) {
                java.time.LocalDate t = s;
                s = e;
                e = t;
            }
            long days = java.time.temporal.ChronoUnit.DAYS.between(s, e) + 1;
            if (days >= 1 && days <= PayrollPeriodUtil.MAX_CUSTOM_RANGE_DAYS) {
                return PayrollPeriodUtil.toCutoffKey(s, e);
            }
        }
        if (selectedCutoff != null && !selectedCutoff.isBlank()) {
            return selectedCutoff.trim();
        }
        return getCurrentPayrollCutoffKey();
    }

    public List<AttendanceLog> getEmployeeOvertimeReviewLines(int employeeId, String cutoffCode) {
        LocalDate[] r = resolveCutoffRange(cutoffCode);
        return attendanceRepository.findOvertimeReviewLinesForEmployee(String.valueOf(employeeId), r[0], r[1]);
    }

    public List<OvertimeApprovalListItem> getOvertimePendingForHr(String cutoffCode, String search) {
        LocalDate[] r = resolveCutoffRange(cutoffCode);
        List<AttendanceLog> logs = attendanceRepository.findPendingOvertimeBetween(r[0], r[1]);
        List<OvertimeApprovalListItem> items = new ArrayList<>();
        for (AttendanceLog log : logs) {
            long eid;
            try {
                eid = Long.parseLong(log.getEmployeeId());
            } catch (NumberFormatException ex) {
                continue;
            }
            Optional<OfficialEmployee> op = officialEmployeeRepository.findById(eid);
            if (op.isEmpty()) {
                continue;
            }
            OfficialEmployee emp = op.get();
            int rep = log.getOvertimeReported() != null ? log.getOvertimeReported() : 0;
            OvertimeApprovalListItem row = new OvertimeApprovalListItem(
                log.getId(),
                eid,
                emp.getCustomEmployeeId(),
                emp.getFirstName() + " " + emp.getLastName(),
                log.getDate(),
                rep,
                "PENDING"
            );
            items.add(row);
        }
        if (!ListSearchUtil.isActiveKeyword(search)) {
            return items;
        }
        return items.stream().filter(row -> {
            String hay = ListSearchUtil.buildHaystack(
                row.getCustomEmployeeId(),
                row.getEmployeeName(),
                String.valueOf(row.getWorkDate()),
                String.valueOf(row.getReportedOtHours()),
                String.valueOf(row.getAttendanceId())
            );
            return ListSearchUtil.matchesTokens(search, hay);
        }).toList();
    }

    public void approveOvertimeAttendance(long attendanceId) {
        AttendanceLog log = attendanceRepository.findById(attendanceId).orElse(null);
        if (log == null || !"PENDING".equalsIgnoreCase(log.getOtApprovalStatus())) {
            return;
        }
        int reported = log.getOvertimeReported() != null ? log.getOvertimeReported() : 0;
        log.setOvertimeHours(reported);
        log.setOtApprovalStatus("APPROVED");
        attendanceRepository.save(log);
    }

    public void rejectOvertimeAttendance(long attendanceId) {
        AttendanceLog log = attendanceRepository.findById(attendanceId).orElse(null);
        if (log == null || !"PENDING".equalsIgnoreCase(log.getOtApprovalStatus())) {
            return;
        }
        log.setOvertimeHours(0);
        log.setOtApprovalStatus("REJECTED");
        attendanceRepository.save(log);
    }

    public void exportDTRToPDF(int employeeId, String cutoffCode, HttpServletResponse response) {
        try {
            OfficialEmployee emp = officialEmployeeRepository.findById((long) employeeId).orElse(null);
            if (emp == null) return;

            List<AttendanceLog> cutoffLogs = getEmployeeAttendanceHistoryByCutoff(employeeId, cutoffCode);
            LocalDate[] range = resolveCutoffRange(cutoffCode);
            LocalDate startDate = range[0];
            LocalDate endDate = range[1];

            Document document = new Document(PageSize.A4);
            PdfWriter.getInstance(document, response.getOutputStream());
            document.open();

            java.awt.Color eacRed = new java.awt.Color(214, 0, 0);
            java.awt.Color eacDark = new java.awt.Color(45, 52, 54);
            Font titleFont = FontFactory.getFont(FontFactory.HELVETICA_BOLD, 24, eacRed);
            Font subFont = FontFactory.getFont(FontFactory.HELVETICA_BOLD, 12, eacDark);
            Font headerFont = FontFactory.getFont(FontFactory.HELVETICA_BOLD, 10, java.awt.Color.WHITE);
            Font normalFont = FontFactory.getFont(FontFactory.HELVETICA, 9, eacDark);
            Font boldFont = FontFactory.getFont(FontFactory.HELVETICA_BOLD, 10, eacDark);

            Paragraph title = new Paragraph("EMILIO AGUINALDO COLLEGE", titleFont);
            title.setAlignment(Element.ALIGN_CENTER);
            document.add(title);
            
            Paragraph subTitle = new Paragraph("DAILY TIME RECORD (DTR)", subFont);
            subTitle.setAlignment(Element.ALIGN_CENTER);
            subTitle.setSpacingAfter(20f);
            document.add(subTitle);

            PdfPTable infoTable = new PdfPTable(4);
            infoTable.setWidthPercentage(100);
            infoTable.setSpacingAfter(15f);
            infoTable.addCell(createCell("Employee ID:", boldFont, false));
            infoTable.addCell(createCell(emp.getCustomEmployeeId(), normalFont, false));
            infoTable.addCell(createCell("Cutoff Period:", boldFont, false));
            infoTable.addCell(createCell(startDate + " to " + endDate, normalFont, false));
            infoTable.addCell(createCell("Name:", boldFont, false));
            infoTable.addCell(createCell(emp.getFirstName() + " " + emp.getLastName(), normalFont, false));
            infoTable.addCell(createCell("Department:", boldFont, false));
            infoTable.addCell(createCell(emp.getDepartment(), normalFont, false));
            document.add(infoTable);

            PdfPTable table = new PdfPTable(8);
            table.setWidthPercentage(100);
            table.setWidths(new float[]{1.35f, 1.15f, 1.0f, 1.0f, 0.9f, 0.85f, 0.85f, 1.15f});

            String[] headers = {"Date", "Day / leave", "Time In", "Time Out", "Reg. Hrs", "OT (payable)", "OT (from biometrics)", "OT status"};
            for (String header : headers) {
                table.addCell(createHeaderCell(header, headerFont, eacDark));
            }

            double totalReg = 0, totalOTPay = 0;

            for (AttendanceLog log : cutoffLogs) {
                table.addCell(createCenterCell(log.getDate().toString(), normalFont));
                String dayStatus = log.getCalendarDayStatusLine();
                table.addCell(createCenterCell(dayStatus != null ? dayStatus : "—", normalFont));
                table.addCell(createCenterCell(log.getTimeIn() != null ? log.getTimeIn().toString() : "--:--", normalFont));
                table.addCell(createCenterCell(log.getTimeOut() != null ? log.getTimeOut().toString() : "--:--", normalFont));

                double hours = log.getTotalHours() != null ? log.getTotalHours() : 0.0;
                double reg = Math.min(hours, 8.0);
                double otPay = log.getOvertimeHours() != null ? log.getOvertimeHours() : 0.0;
                double otRpt = log.getOvertimeReported() != null ? log.getOvertimeReported() : 0.0;
                String otStat = log.getOtApprovalStatus() != null && !log.getOtApprovalStatus().isEmpty()
                    ? log.getOtApprovalStatus() : (otRpt > 0 || otPay > 0 ? "—" : "—");
                totalReg += reg;
                totalOTPay += otPay;

                table.addCell(createCenterCell(String.format("%.2f", reg), normalFont));
                table.addCell(createCenterCell(String.format("%.0f", otPay), normalFont));
                table.addCell(createCenterCell(String.format("%.0f", otRpt), normalFont));
                table.addCell(createCenterCell(otStat, normalFont));
            }
            document.add(table);

            Paragraph totals = new Paragraph("\nTotal Regular Hours: " + String.format("%.2f", totalReg) + " hrs\n" +
                                             "Total payable overtime (after HR approval): " + String.format("%.2f", totalOTPay) + " hrs", boldFont);
            totals.setAlignment(Element.ALIGN_RIGHT);
            totals.setSpacingAfter(40f);
            document.add(totals);

            PdfPTable sigTable = new PdfPTable(2);
            sigTable.setWidthPercentage(100);
            sigTable.addCell(createCenterCell("__________________________\nEmployee Signature", normalFont));
            sigTable.addCell(createCenterCell("__________________________\nDean / Head Signature", normalFont));
            document.add(sigTable);

            document.close();
        } catch (Exception e) { System.err.println("PDF Error: " + e.getMessage()); }
    }

    private PdfPCell createCell(String text, Font font, boolean border) {
        PdfPCell cell = new PdfPCell(new Phrase(text, font));
        cell.setBorder(border ? Rectangle.BOTTOM : Rectangle.NO_BORDER);
        cell.setPadding(6f);
        cell.setBorderColor(new java.awt.Color(200, 200, 200));
        return cell;
    }
    private PdfPCell createRightCell(String text, Font font) {
        PdfPCell cell = createCell(text, font, true);
        cell.setHorizontalAlignment(Element.ALIGN_RIGHT);
        return cell;
    }
    private PdfPCell createCenterCell(String text, Font font) {
        PdfPCell cell = createCell(text, font, true);
        cell.setHorizontalAlignment(Element.ALIGN_CENTER);
        return cell;
    }
    private PdfPCell createHeaderCell(String text, Font font, java.awt.Color bgColor) {
        PdfPCell cell = new PdfPCell(new Phrase(text, font));
        cell.setBackgroundColor(bgColor);
        cell.setHorizontalAlignment(Element.ALIGN_CENTER);
        cell.setPadding(8f);
        return cell;
    }

    public BiometricsImportResult processBiometricsCsv(org.springframework.web.multipart.MultipartFile file) throws Exception {
        this.lastUploadedFileName = file.getOriginalFilename();
        java.time.format.DateTimeFormatter formatter = java.time.format.DateTimeFormatter.ofPattern("MMM dd, yyyy - hh:mm a");
        this.lastUploadTime = java.time.LocalDateTime.now().format(formatter);

        int dataRows = 0;
        int imported = 0;
        int skippedTooFewColumns = 0;
        int skippedUnknownEmployee = 0;
        int skippedParseError = 0;

        List<String> lines = new ArrayList<>();
        try (BufferedReader br = new BufferedReader(new InputStreamReader(file.getInputStream(), StandardCharsets.UTF_8))) {
            String line;
            while ((line = br.readLine()) != null) {
                if (line.isBlank()) {
                    continue;
                }
                lines.add(line);
            }
        }
        if (lines.isEmpty()) {
            return new BiometricsImportResult(0, 0, 0, 0, 0);
        }
        lines.set(0, stripUtf8Bom(lines.get(0)));
        char delimiter = detectCsvDelimiter(lines.get(0));
        String[] firstCols = parseTcmsCsvLine(lines.get(0), delimiter);
        int start = looksLikeTcmsDataRow(firstCols) ? 0 : 1;
        if (start >= lines.size()) {
            return new BiometricsImportResult(0, 0, 0, 0, 0);
        }

        for (int i = start; i < lines.size(); i++) {
            String line = lines.get(i);
            dataRows++;
            String[] data = parseTcmsCsvLine(line, delimiter);
            if (data.length < TCMS_CSV_MIN_COLUMNS) {
                skippedTooFewColumns++;
                continue;
            }

            try {
                int tcmsUserId = Integer.parseInt(data[TCMS_COL_USER].trim());
                String officialEmpIdStr = data[TCMS_COL_EAC].trim().replaceAll("\\s+", "");

                if (officialEmpIdStr.isEmpty()) {
                    skippedUnknownEmployee++;
                    continue;
                }

                Optional<OfficialEmployee> optEmp = officialEmployeeRepository.findByCustomEmployeeId(officialEmpIdStr);
                if (optEmp.isEmpty()) {
                    skippedUnknownEmployee++;
                    continue;
                }

                OfficialEmployee emp = optEmp.get();

                if (emp.getBiometricId() == null || !emp.getBiometricId().equals(tcmsUserId)) {
                    emp.setBiometricId(tcmsUserId);
                    officialEmployeeRepository.save(emp);
                }

                long internalDbId = emp.getId();

                String dateStr = data[TCMS_COL_DATE].trim();
                String timeInStr = data[TCMS_COL_IN].trim();
                String timeOutStr = data[TCMS_COL_OUT].trim();

                String leaveType = (data.length > TCMS_COL_LEAVE) ? data[TCMS_COL_LEAVE].trim() : "";
                String dayType = readTcmsDayTypeField(data);

                LocalDate logDate = parseBiometricCsvDate(dateStr);
                LocalTime timeIn = parseBiometricCsvTime(timeInStr);
                LocalTime timeOut = parseBiometricCsvTime(timeOutStr);

                Optional<AttendanceLog> existingLogOpt = attendanceRepository.findByEmployeeIdAndDate(String.valueOf(internalDbId), logDate);
                AttendanceLog log = existingLogOpt.orElse(new AttendanceLog());

                log.setEmployeeId(String.valueOf(internalDbId));
                log.setDate(logDate);

                EffectiveShiftRule effectiveShift = resolveEffectiveShiftForDate(emp, logDate);
                if (!leaveType.isEmpty() && !"None".equalsIgnoreCase(leaveType)) {
                    if (isPaidLeaveType(leaveType)) {
                        log.setTimeIn(effectiveShift.expectedIn());
                        log.setTimeOut(effectiveShift.expectedOut());
                    } else {
                        log.setTimeIn(timeIn);
                        log.setTimeOut(timeOut);
                    }
                } else {
                    log.setTimeIn(timeIn);
                    log.setTimeOut(timeOut);
                }

                LocalTime calcIn = log.getTimeIn();
                LocalTime calcOut = log.getTimeOut();
                boolean ignoreMissingPunch = false;
                if (!isPaidLeaveType(leaveType) && (calcIn == null || calcOut == null)) {
                    if ("AUTO_CLOSE".equals(effectiveShift.missingPunchPolicy())) {
                        if (calcIn == null && calcOut != null) {
                            calcIn = effectiveShift.expectedIn();
                            log.setTimeIn(calcIn);
                        } else if (calcOut == null && calcIn != null) {
                            calcOut = effectiveShift.expectedOut();
                            log.setTimeOut(calcOut);
                        }
                    } else if ("IGNORE".equals(effectiveShift.missingPunchPolicy())) {
                        ignoreMissingPunch = true;
                    }
                }
                int minutesLate = 0;
                int minutesEarlyOut = 0;
                int totalHoursInt = 0;
                int undertimeHoursInt = 0;
                int reportedOtHours = 0;

                if (!ignoreMissingPunch && calcIn != null && calcOut != null) {
                    if (!effectiveShift.flexibleHours()) {
                        LocalTime lateThreshold = addMinutesToTime(effectiveShift.expectedIn(), effectiveShift.graceMinutes());
                        if (calcIn.isAfter(lateThreshold)) {
                            minutesLate = (int) minutesBetween(lateThreshold, calcIn);
                        }
                    } else if (effectiveShift.flexiInWindowEnd() != null) {
                        LocalTime flexiThreshold = addMinutesToTime(effectiveShift.flexiInWindowEnd(), effectiveShift.graceMinutes());
                        if (calcIn.isAfter(flexiThreshold)) {
                            minutesLate = (int) minutesBetween(flexiThreshold, calcIn);
                        }
                    }
                    if (calcOut.isBefore(effectiveShift.expectedOut())) {
                        minutesEarlyOut = (int) minutesBetween(calcOut, effectiveShift.expectedOut());
                    }
                    long elapsedMinutes = minutesBetween(calcIn, calcOut);
                    if (elapsedMinutes > 0) {
                        elapsedMinutes = Math.max(0, elapsedMinutes - effectiveShift.breakMinutes());
                    }

                    totalHoursInt = (int) (elapsedMinutes / 60);

                    long scheduledWorkMins = effectiveShift.requiredNetWorkMinutes() > 0
                        ? effectiveShift.requiredNetWorkMinutes()
                        : Math.max(0, minutesBetween(effectiveShift.expectedIn(), effectiveShift.expectedOut()) - effectiveShift.breakMinutes());
                    boolean specialDay = isTcmsRestOrHolidayDayType(dayType);
                    if (specialDay && effectiveShift.treatRestOrHolidayAsOt()) {
                        long otMins = elapsedMinutes;
                        if (otMins >= effectiveShift.minimumOtMinutes()) {
                            reportedOtHours = (int) (otMins / 60);
                        }
                    } else if (effectiveShift.allowExtraHours()) {
                        long otMins;
                        if (effectiveShift.flexibleHours()) {
                            otMins = Math.max(0, elapsedMinutes - scheduledWorkMins);
                        } else if (calcOut.isAfter(effectiveShift.expectedOut())) {
                            otMins = minutesBetween(effectiveShift.expectedOut(), calcOut);
                        } else {
                            otMins = 0;
                        }
                        if (otMins >= effectiveShift.minimumOtMinutes()) {
                            reportedOtHours = (int) (otMins / 60);
                        }
                    }

                    if (elapsedMinutes < scheduledWorkMins && !specialDay && !isPaidLeaveType(leaveType)) {
                        undertimeHoursInt = (int) ((scheduledWorkMins - elapsedMinutes) / 60);
                    }
                }

                log.setTotalHours(totalHoursInt);
                log.setMinutesLate(minutesLate);
                log.setUndertimeHours(undertimeHoursInt);
                log.setMinutesEarlyOut(minutesEarlyOut);

                if (existingLogOpt.isPresent() && "APPROVED".equalsIgnoreCase(existingLogOpt.get().getOtApprovalStatus())) {
                    AttendanceLog prev = existingLogOpt.get();
                    log.setOvertimeReported(prev.getOvertimeReported());
                    log.setOvertimeHours(prev.getOvertimeHours());
                    log.setOtApprovalStatus(prev.getOtApprovalStatus());
                } else {
                    if (reportedOtHours > 0) {
                        log.setOvertimeReported(reportedOtHours);
                        if (effectiveShift.requireOtApproval()) {
                            log.setOtApprovalStatus("PENDING");
                            log.setOvertimeHours(0);
                        } else {
                            log.setOtApprovalStatus("APPROVED");
                            log.setOvertimeHours(reportedOtHours);
                        }
                    } else {
                        log.setOvertimeReported(0);
                        log.setOtApprovalStatus("NONE");
                        log.setOvertimeHours(0);
                    }
                }

                if (dayType == null || dayType.isBlank()) {
                    log.setDayType("Workday");
                } else {
                    log.setDayType(dayType.length() > 64 ? dayType.substring(0, 64) : dayType);
                }
                if (leaveType == null || leaveType.isBlank() || "None".equalsIgnoreCase(leaveType.trim())) {
                    if (existingLogOpt.isPresent()) {
                        String prevLt = existingLogOpt.get().getTcmsLeaveType();
                        log.setTcmsLeaveType((prevLt != null && !prevLt.isBlank()) ? prevLt : null);
                    } else {
                        log.setTcmsLeaveType(null);
                    }
                } else {
                    String lt = leaveType.length() > 100 ? leaveType.substring(0, 100) : leaveType;
                    log.setTcmsLeaveType(lt.trim());
                }

                attendanceRepository.save(log);
                imported++;
            } catch (Exception e) {
                skippedParseError++;
                String preview = line.length() > 240 ? line.substring(0, 240) + "…" : line;
                log.warn("Skipped biometrics CSV row: {} -> {}", preview, e.getMessage());
            }
        }
        return new BiometricsImportResult(dataRows, imported, skippedTooFewColumns, skippedUnknownEmployee, skippedParseError);
    }

    // ---- Attendance admin & self-service views ----

    public AttendanceDayStats getAttendanceAdminDayStats(LocalDate day) {
        List<OfficialEmployee> actives = new ArrayList<>();
        officialEmployeeRepository.findByStatus("Active").forEach(actives::add);
        long activeN = actives.size();

        List<AttendanceLog> dayLogs = attendanceRepository.findByDateRange(day, day);
        Set<String> withTimeIn = new HashSet<>();
        for (AttendanceLog log : dayLogs) {
            if (log.getTimeIn() != null) {
                withTimeIn.add(log.getEmployeeId());
            }
        }
        int present = withTimeIn.size();
        long lateUnique = dayLogs.stream()
            .filter(l -> l.getTimeIn() != null && l.getMinutesLate() != null && l.getMinutesLate() > 0)
            .map(AttendanceLog::getEmployeeId)
            .distinct()
            .count();
        int absent = (int) Math.max(0, activeN - present);
        return new AttendanceDayStats(present, (int) lateUnique, absent);
    }

    public List<AdminAttendanceLogRow> buildAdminAttendanceRows(LocalDate from, LocalDate to) {
        if (from == null || to == null || from.isAfter(to)) {
            return List.of();
        }
        List<AttendanceLog> logs = attendanceRepository.findByDateRange(from, to);
        List<AdminAttendanceLogRow> rows = new ArrayList<>();
        for (AttendanceLog log : logs) {
            long internalId;
            try {
                internalId = Long.parseLong(log.getEmployeeId());
            } catch (Exception e) {
                continue;
            }
            Optional<OfficialEmployee> op = officialEmployeeRepository.findById(internalId);
            if (op.isEmpty()) {
                continue;
            }
            OfficialEmployee emp = op.get();
            String shift = resolveShiftLabelForDate(emp, log.getDate());
            String statusLabel = describeAttendanceStatus(log);
            String inStr = log.getTimeIn() != null ? log.getTimeIn().toString() : "—";
            String outStr = log.getTimeOut() != null ? log.getTimeOut().toString()
                : (log.getTimeIn() != null ? "Open" : "—");
            int th = log.getTotalHours() != null ? log.getTotalHours() : 0;
            String worked = th + " h (reg.)";
            int lateMin = log.getMinutesLate() != null ? log.getMinutesLate() : 0;
            String lateInfo = lateMin > 0 ? lateMin + " min" : "—";
            String cc = emp.getCampusCode() != null && !emp.getCampusCode().isBlank() ? emp.getCampusCode().trim() : "";
            String campusLabel = !cc.isEmpty() ? "Campus " + cc : "—";
            rows.add(new AdminAttendanceLogRow(
                log.getId(), internalId,
                emp.getCustomEmployeeId() != null ? emp.getCustomEmployeeId() : "",
                emp.getFirstName() + " " + emp.getLastName(),
                emp.getDepartment() != null ? emp.getDepartment() : "—",
                emp.getPosition() != null ? emp.getPosition() : "—",
                campusLabel, cc,
                log.getDate(), shift, statusLabel, inStr, outStr, "—", lateInfo, worked,
                "EAC", "TCMS / import"
            ));
        }
        rows.sort(Comparator.comparing(AdminAttendanceLogRow::workDate).reversed()
            .thenComparing(AdminAttendanceLogRow::eacId));
        return rows;
    }

    private String resolveShiftLabelForDate(OfficialEmployee emp, LocalDate workDate) {
        if (emp == null || emp.getId() == null || workDate == null) {
            return "—";
        }
        Optional<ShiftAssignment> maybe = shiftAssignmentRepository.findByEmployeeIdAndWorkDate(emp.getId(), workDate);
        if (maybe.isPresent()) {
            ShiftAssignment a = maybe.get();
            String base = (a.getShift() != null && a.getShift().getName() != null && !a.getShift().getName().isBlank())
                ? a.getShift().getName().trim()
                : "Assigned shift";
            if (a.getOverrideTimeIn() != null && a.getOverrideTimeOut() != null) {
                return base + " (" + a.getOverrideTimeIn() + "-" + a.getOverrideTimeOut() + ")";
            }
            return base;
        }
        if (emp.getExpectedShift() != null && !emp.getExpectedShift().isBlank()) {
            return emp.getExpectedShift();
        }
        return "—";
    }

    private record EffectiveShiftRule(
        LocalTime expectedIn,
        LocalTime expectedOut,
        int graceMinutes,
        int breakMinutes,
        int minimumOtMinutes,
        boolean allowExtraHours,
        boolean flexibleHours,
        boolean treatRestOrHolidayAsOt,
        LocalTime flexiInWindowStart,
        LocalTime flexiInWindowEnd,
        int requiredNetWorkMinutes,
        boolean requireOtApproval,
        String missingPunchPolicy
    ) {}

    private EffectiveShiftRule resolveEffectiveShiftForDate(OfficialEmployee emp, LocalDate workDate) {
        LocalTime defaultIn = LocalTime.of(7, 0);
        LocalTime defaultOut = LocalTime.of(16, 0);
        int defaultBreak = 60;
        if (emp == null || emp.getId() == null || workDate == null) {
            return new EffectiveShiftRule(defaultIn, defaultOut, 0, defaultBreak, 0, true, false, false, null, null, 0, false, "REVIEW_REQUIRED");
        }
        Optional<ShiftAssignment> maybe = shiftAssignmentRepository.findByEmployeeIdAndWorkDate(emp.getId(), workDate);
        if (maybe.isEmpty()) {
            return new EffectiveShiftRule(defaultIn, defaultOut, 0, defaultBreak, 0, true, false, false, null, null, 0, false, "REVIEW_REQUIRED");
        }
        ShiftAssignment a = maybe.get();
        ShiftSchedule s = a.getShift();
        LocalTime expectedIn = a.getOverrideTimeIn() != null ? a.getOverrideTimeIn() : (s != null && s.getStartTime() != null ? s.getStartTime() : defaultIn);
        LocalTime expectedOut = a.getOverrideTimeOut() != null ? a.getOverrideTimeOut() : (s != null && s.getEndTime() != null ? s.getEndTime() : defaultOut);
        int grace = a.getOverrideGraceMinutes() != null ? Math.max(0, a.getOverrideGraceMinutes()) : (s != null && s.getGraceMinutes() != null ? Math.max(0, s.getGraceMinutes()) : 0);
        int breakMinutes = a.getOverrideBreakMinutes() != null ? Math.max(0, a.getOverrideBreakMinutes()) : (s != null && s.getBreakMinutes() != null ? Math.max(0, s.getBreakMinutes()) : defaultBreak);
        int minOt = (s != null && s.getMinimumMinutesForOt() != null) ? Math.max(0, s.getMinimumMinutesForOt()) : 0;
        boolean allowExtra = s == null || s.isAllowExtraHours();
        boolean flexible = s != null && s.isFlexibleHours();
        boolean restOrHolidayAsOt = s != null && (Boolean.TRUE.equals(s.getTreatRestdayAsOt()) || Boolean.TRUE.equals(s.getTreatHolidayAsOt()) || Boolean.TRUE.equals(s.getTreatOffdayAsOt()));
        LocalTime flexiStart = s != null ? s.getFlexiInWindowStart() : null;
        LocalTime flexiEnd = s != null ? s.getFlexiInWindowEnd() : null;
        int requiredNet = (s != null && s.getRequiredNetWorkMinutes() != null) ? Math.max(0, s.getRequiredNetWorkMinutes()) : 0;
        boolean requireOtApproval = s != null && Boolean.TRUE.equals(s.getRequireOtApproval());
        String missingPunchPolicy = (s != null && s.getMissingPunchPolicy() != null && !s.getMissingPunchPolicy().isBlank())
            ? s.getMissingPunchPolicy().trim().toUpperCase(Locale.ROOT)
            : "REVIEW_REQUIRED";
        return new EffectiveShiftRule(
            expectedIn, expectedOut, grace, breakMinutes, minOt, allowExtra, flexible, restOrHolidayAsOt,
            flexiStart, flexiEnd, requiredNet, requireOtApproval, missingPunchPolicy
        );
    }

    private static LocalTime addMinutesToTime(LocalTime base, int minutes) {
        if (base == null || minutes <= 0) {
            return base;
        }
        return base.plusMinutes(minutes);
    }

    private static long minutesBetween(LocalTime start, LocalTime end) {
        if (start == null || end == null) {
            return 0;
        }
        long mins = ChronoUnit.MINUTES.between(start, end);
        if (mins < 0) {
            mins += 1440;
        }
        return Math.max(0, mins);
    }

    private static String describeAttendanceStatus(AttendanceLog log) {
        String rawLt = log.getTcmsLeaveType();
        if (rawLt != null && isPaidLeaveType(rawLt.trim())) {
            String code = rawLt.trim();
            if (log.getTimeIn() == null) {
                return "On leave (" + code + ")";
            }
            if (log.getTimeOut() == null) {
                return "On leave (open out)";
            }
            return "Paid leave (" + code + ")";
        }
        if (log.getTimeIn() == null) {
            return "No in";
        }
        if (log.getTimeOut() == null) {
            return "Open shift";
        }
        if (log.getMinutesLate() != null && log.getMinutesLate() > 0) {
            return "Late";
        }
        return "Present";
    }

    public Optional<AttendanceLog> findAttendanceLogForToday(int internalEmployeeId) {
        return attendanceRepository.findByEmployeeIdAndDate(
            String.valueOf(internalEmployeeId), LocalDate.now());
    }

    public EmployeeStatSummary buildEmployeeAttendanceStats(OfficialEmployee emp) {
        LocalDate today = LocalDate.now();
        LocalDate startWeek = today.minusDays(6);
        LocalDate startMonth = today.withDayOfMonth(1);
        LocalDate endMonth = today.withDayOfMonth(today.lengthOfMonth());

        List<AttendanceLog> monthLogs = attendanceRepository.findByDateRange(startMonth, endMonth).stream()
            .filter(l -> String.valueOf(emp.getId()).equals(l.getEmployeeId()))
            .toList();

        int todayH = monthLogs.stream().filter(l -> l.getDate().equals(today))
            .mapToInt(l -> l.getTotalHours() != null ? l.getTotalHours() : 0).sum();

        int weekH = attendanceRepository.findByDateRange(startWeek, today).stream()
            .filter(l -> String.valueOf(emp.getId()).equals(l.getEmployeeId()))
            .mapToInt(l -> l.getTotalHours() != null ? l.getTotalHours() : 0).sum();

        int monthH = monthLogs.stream()
            .mapToInt(l -> l.getTotalHours() != null ? l.getTotalHours() : 0).sum();

        int lateMin = monthLogs.stream()
            .mapToInt(l -> l.getMinutesLate() != null ? l.getMinutesLate() : 0).sum();
        int utH = monthLogs.stream()
            .mapToInt(l -> l.getUndertimeHours() != null ? l.getUndertimeHours() : 0).sum();

        return new EmployeeStatSummary(
            todayH + " h",
            weekH + " h",
            monthH + " h",
            "0 min",
            lateMin > 0 ? lateMin + " min" : "0 min",
            utH > 0 ? utH + " h" : "0 h"
        );
    }

    public List<String> getDistinctDesignationsForActiveEmployees() {
        return StreamSupport.stream(officialEmployeeRepository.findByStatus("Active").spliterator(), false)
            .map(OfficialEmployee::getPosition)
            .filter(p -> p != null && !p.isBlank())
            .map(String::trim)
            .distinct()
            .sorted(String.CASE_INSENSITIVE_ORDER)
            .toList();
    }

    /**
     * TCMS col 9 (and EAC full leave names). Abbreviation-only codes (e.g. {@code STL} for
     * study leave) are treated as paid so CSV rows with no punches get 8:00–17:00 credit like VL/SL.
     */
    private static boolean isPaidLeaveType(String leaveType) {
        if (leaveType == null) {
            return false;
        }
        String u = leaveType.toUpperCase(Locale.ROOT).trim();
        if (u.isEmpty() || "NONE".equals(u) || "-".equals(u)) {
            return false;
        }
        if ("STL".equals(u) || "STDL".equals(u) || "SIL".equals(u)) {
            return true;
        }
        return u.contains("VL") || u.contains("VACATION") ||
               u.contains("SL") || u.contains("SICK") ||
               u.contains("BL") || u.contains("BEREAVEMENT") ||
               u.contains("SPL") || u.contains("SOLO PARENT") ||
               u.contains("ML") || u.contains("MATERNITY") ||
               u.contains("PL") || u.contains("PATERNITY") ||
               u.contains("SERVICE INCENTIVE") || u.contains("STUDY") ||
               u.contains("TERMINAL");
    }

    /**
     * Shorthand stored in {@code attendance.tcms_leave_type} for days created from
     * approved EAC leave — matches TCMS import behaviour and
     * {@code hr-attendance-board.html} (isTcmsLeave / "Approved leave (…)").
     */
    private static String eacLeaveTypeToTcmsStatusAbbrev(String fullLeaveType) {
        if (fullLeaveType == null || fullLeaveType.isBlank()) {
            return "Leave";
        }
        String u = fullLeaveType.toUpperCase(Locale.ROOT);
        if (u.contains("VACATION") || u.contains("TERMINAL")) {
            return "VL";
        }
        if (u.contains("SICK")) {
            return "SL";
        }
        if (u.contains("MATERNITY")) {
            return "ML";
        }
        if (u.contains("PATERNITY")) {
            return "PL";
        }
        if (u.contains("BEREAV")) {
            return "BL";
        }
        if (u.contains("SOLO")) {
            return "SPL";
        }
        if (u.contains("SERVICE") && u.contains("INCENTIVE")) {
            return "SIL";
        }
        if (u.contains("STUDY")) {
            return "STDL";
        }
        return fullLeaveType.length() > 32 ? fullLeaveType.substring(0, 32).trim() : fullLeaveType.trim();
    }

    public String generateEacEmployeeId(Applicant emp) {
        String campusPrefix = resolveCampusPrefix(emp.getDepartment());
        long inCampus = officialEmployeeRepository.countByCustomEmployeeIdStartingWith(campusPrefix + "-");
        String sequence = String.format("%05d", inCampus + 1);
        return campusPrefix + "-" + sequence;
    }

    public String generateEacEmployeeIdFallback(OfficialEmployee emp) {
        String campusPrefix = resolveCampusPrefix(emp.getDepartment());
        long inCampus = officialEmployeeRepository.countByCustomEmployeeIdStartingWith(campusPrefix + "-");
        String sequence = String.format("%05d", inCampus + 1);
        return campusPrefix + "-" + sequence;
    }

    private static String resolveCampusPrefix(String department) {
        if (department != null && department.toLowerCase().contains("cavite")) {
            return "2";
        }
        return "1";
    }
    
    private void sendEmail(String to, String subject, String text) {
        if (to == null || to.isBlank()) {
            return;
        }
        if (mailFromConfigured == null || mailFromConfigured.isBlank()) {
            log.debug("Skipping email (mail not configured): subject={}", subject);
            return;
        }
        try {
            SimpleMailMessage message = new SimpleMailMessage();
            message.setTo(to);
            message.setSubject(subject);
            message.setText(text);
            mailSender.send(message);
        } catch (Exception e) {
            log.warn("Email send failed: {}", e.getMessage());
        }
    }
}