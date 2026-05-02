package com.example.employee.service;

import com.example.employee.model.AttendanceLog;
import com.example.employee.model.EacOvertimeRequest;
import com.example.employee.model.OfficialEmployee;
import com.example.employee.model.OvertimeDisplayRow;
import com.example.employee.repository.AttendanceRepository;
import com.example.employee.repository.EacOvertimeRequestRepository;
import com.example.employee.repository.OfficialEmployeeRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.YearMonth;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Service
public class OvertimeRequestService {

    @Value("${app.upload.overtime-dir:uploads/overtime-attachments}")
    private String overtimeUploadDir;

    @Autowired
    private EacOvertimeRequestRepository eacOvertimeRequestRepository;

    @Autowired
    private AttendanceRepository attendanceRepository;

    @Autowired
    private OfficialEmployeeRepository officialEmployeeRepository;

    public record OvertimeMonthStats(long pending, long approved, long rejected, long total) {}

    public OvertimeMonthStats getStatsForEmployeeMonth(long internalEmployeeId, YearMonth ym) {
        LocalDate a = ym.atDay(1);
        LocalDate b = ym.atEndOfMonth();
        List<OvertimeDisplayRow> list = buildEmployeeList(internalEmployeeId, a, b);
        long p = 0, ap = 0, rj = 0;
        for (OvertimeDisplayRow row : list) {
            String s = row.status() != null ? row.status() : "";
            if (s.toLowerCase().contains("pend")) {
                p++;
            } else if (s.toLowerCase().contains("appr")) {
                ap++;
            } else if (s.toLowerCase().contains("reject")) {
                rj++;
            }
        }
        return new OvertimeMonthStats(p, ap, rj, list.size());
    }

    public OvertimeMonthStats getStatsForMonth(YearMonth ym) {
        LocalDate a = ym.atDay(1);
        LocalDate b = ym.atEndOfMonth();
        long pe = eacOvertimeRequestRepository.countByStatusAndWorkDateBetween("PENDING", a, b);
        long ae = eacOvertimeRequestRepository.countByStatusAndWorkDateBetween("APPROVED", a, b);
        long re = eacOvertimeRequestRepository.countByStatusAndWorkDateBetween("REJECTED", a, b);

        long pt = attendanceRepository.countTcmsOvertimePendingMonth(a, b);
        long at = attendanceRepository.countTcmsOvertimeApprovedMonth(a, b);
        long rt = attendanceRepository.countTcmsOvertimeRejectedMonth(a, b);

        long pending = pe + pt;
        long approved = ae + at;
        long rejected = re + rt;
        long total = pending + approved + rejected;
        return new OvertimeMonthStats(pending, approved, rejected, total);
    }

    public List<OvertimeDisplayRow> buildAdminList(LocalDate from, LocalDate to) {
        if (from == null || to == null || from.isAfter(to)) {
            return List.of();
        }
        List<OvertimeDisplayRow> rows = new ArrayList<>();

        for (AttendanceLog log : attendanceRepository.findOvertimeRelatedInDateRange(from, to)) {
            Optional<OfficialEmployee> op = parseEmployee(log.getEmployeeId());
            if (op.isEmpty()) {
                continue;
            }
            OfficialEmployee emp = op.get();
            String st = log.getOtApprovalStatus() != null ? log.getOtApprovalStatus().trim() : "NONE";
            if ("NONE".equalsIgnoreCase(st) && (log.getOvertimeReported() == null || log.getOvertimeReported() <= 0)
                && (log.getOvertimeHours() == null || log.getOvertimeHours() <= 0)) {
                continue;
            }
            int rpt = log.getOvertimeReported() != null ? log.getOvertimeReported() : 0;
            int pay = log.getOvertimeHours() != null ? log.getOvertimeHours() : 0;
            String hrs = rpt > 0 ? (rpt + " h rpt") : (pay > 0 ? (pay + " h pay") : "0 h");
            String statusLabel = "PENDING".equalsIgnoreCase(st) ? "Pending"
                : ("APPROVED".equalsIgnoreCase(st) ? "Approved" : ("REJECTED".equalsIgnoreCase(st) ? "Rejected" : st));
            rows.add(new OvertimeDisplayRow(
                "TCMS-" + log.getId(),
                "TCMS / biometrics",
                emp.getId(),
                emp.getCustomEmployeeId() != null ? emp.getCustomEmployeeId() : "",
                emp.getFirstName() + " " + emp.getLastName(),
                emp.getDepartment() != null ? emp.getDepartment() : "—",
                emp.getPosition() != null ? emp.getPosition() : "—",
                campusOf(emp),
                blankToDash(emp.getCampusCode()),
                log.getDate(),
                log.getTimeIn() != null ? log.getTimeIn().toString() : "—",
                log.getTimeOut() != null ? log.getTimeOut().toString() : "—",
                hrs,
                "—",
                statusLabel,
                "REGULAR",
                "HR",
                "—",
                null,
                log.getId(),
                0L
            ));
        }

        for (EacOvertimeRequest r : eacOvertimeRequestRepository.findByWorkDateBetweenOrderByWorkDateDescIdDesc(from, to)) {
            Optional<OfficialEmployee> op = officialEmployeeRepository.findById(r.getEmployeeId());
            if (op.isEmpty()) {
                continue;
            }
            OfficialEmployee emp = op.get();
            int h = r.getOvertimeHours() != null ? r.getOvertimeHours() : 0;
            rows.add(new OvertimeDisplayRow(
                "EAC-" + r.getId(),
                "EAC request",
                emp.getId(),
                emp.getCustomEmployeeId() != null ? emp.getCustomEmployeeId() : "",
                emp.getFirstName() + " " + emp.getLastName(),
                emp.getDepartment() != null ? emp.getDepartment() : "—",
                emp.getPosition() != null ? emp.getPosition() : "—",
                campusOf(emp),
                blankToDash(emp.getCampusCode()),
                r.getWorkDate(),
                r.getStartTime() != null ? r.getStartTime().toString() : "—",
                r.getEndTime() != null ? r.getEndTime().toString() : "—",
                h + " h",
                r.getOffsetDate() != null ? r.getOffsetDate().toString() : "—",
                r.getStatus(),
                r.getOtType() != null ? r.getOtType() : "REGULAR",
                "HR",
                r.getLastActionBy() != null ? r.getLastActionBy() : "—",
                r.getAttachmentPath(),
                0L,
                r.getId()
            ));
        }

        rows.sort(Comparator.comparing(OvertimeDisplayRow::workDate).reversed()
            .thenComparing(OvertimeDisplayRow::eacId));
        return rows;
    }

    public List<OvertimeDisplayRow> buildEmployeeList(long internalEmployeeId, LocalDate from, LocalDate to) {
        List<OvertimeDisplayRow> all = buildAdminList(from, to);
        return all.stream().filter(r -> r.employeeInternalId() == internalEmployeeId).toList();
    }

    private static String blankToDash(String c) {
        return c != null && !c.isBlank() ? c.trim() : "—";
    }

    private static String campusOf(OfficialEmployee emp) {
        String c = emp.getCampusCode();
        return c != null && !c.isBlank() ? "Campus " + c : "—";
    }

    private Optional<OfficialEmployee> parseEmployee(String employeeIdStr) {
        try {
            long id = Long.parseLong(employeeIdStr);
            return officialEmployeeRepository.findById(id);
        } catch (Exception e) {
            return Optional.empty();
        }
    }

    @Transactional
    public void submitEacRequest(
        long employeeInternalId,
        LocalDate workDate,
        Integer overtimeHours,
        String otType,
        LocalDate offsetDate,
        String notes,
        String requestSource,
        MultipartFile attachment) throws IOException {

        if (workDate == null) {
            throw new IllegalArgumentException("Work date is required.");
        }
        int oh = overtimeHours != null ? Math.max(0, overtimeHours) : 0;
        if (oh <= 0) {
            throw new IllegalArgumentException("Overtime hours must be greater than zero.");
        }
        EacOvertimeRequest r = new EacOvertimeRequest();
        r.setEmployeeId(employeeInternalId);
        r.setWorkDate(workDate);
        r.setOvertimeHours(oh);
        r.setOtType(otType != null && !otType.isBlank() ? otType.trim() : "REGULAR");
        r.setOffsetDate(offsetDate);
        r.setNotes(notes);
        r.setRequestSource(requestSource != null ? requestSource : "EMPLOYEE");
        r.setStatus("PENDING");
        r.setCreatedAt(LocalDateTime.now());
        if (attachment != null && !attachment.isEmpty()) {
            saveAttachment(r, attachment);
        }
        eacOvertimeRequestRepository.save(r);
    }

    private void saveAttachment(EacOvertimeRequest r, MultipartFile file) throws IOException {
        String orig = file.getOriginalFilename() != null ? file.getOriginalFilename() : "file";
        String ext = orig.contains(".") ? orig.substring(orig.lastIndexOf('.')) : "";
        if (ext.length() > 8) {
            ext = "";
        }
        String name = UUID.randomUUID() + ext;
        Path dir = Path.of(overtimeUploadDir).toAbsolutePath().normalize();
        Files.createDirectories(dir);
        Path target = dir.resolve(name);
        Files.copy(file.getInputStream(), target, java.nio.file.StandardCopyOption.REPLACE_EXISTING);
        r.setAttachmentPath("/uploads/overtime-attachments/" + name);
    }

    @Transactional
    public void approveEacRequest(long id, String approverLabel) {
        EacOvertimeRequest r = eacOvertimeRequestRepository.findById(id)
            .orElseThrow(() -> new IllegalArgumentException("Request not found."));
        if (!"PENDING".equalsIgnoreCase(r.getStatus())) {
            throw new IllegalStateException("Only pending requests can be approved.");
        }
        r.setStatus("APPROVED");
        r.setLastActionBy(approverLabel);
        r.setDecidedAt(LocalDateTime.now());
        eacOvertimeRequestRepository.save(r);
    }

    @Transactional
    public void rejectEacRequest(long id, String approverLabel) {
        EacOvertimeRequest r = eacOvertimeRequestRepository.findById(id)
            .orElseThrow(() -> new IllegalArgumentException("Request not found."));
        if (!"PENDING".equalsIgnoreCase(r.getStatus())) {
            throw new IllegalStateException("Only pending requests can be rejected.");
        }
        r.setStatus("REJECTED");
        r.setLastActionBy(approverLabel);
        r.setDecidedAt(LocalDateTime.now());
        eacOvertimeRequestRepository.save(r);
    }
}
