package com.example.employee.service;

import com.example.employee.model.EacOfficialBusinessRequest;
import com.example.employee.model.OfficialBusinessDisplayRow;
import com.example.employee.model.OfficialEmployee;
import com.example.employee.repository.EacOfficialBusinessRequestRepository;
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
import java.time.LocalTime;
import java.time.YearMonth;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Service
public class OfficialBusinessService {

    @Value("${app.upload.official-business-dir:uploads/official-business-attachments}")
    private String obUploadDir;

    @Autowired
    private EacOfficialBusinessRequestRepository obRepository;

    @Autowired
    private OfficialEmployeeRepository officialEmployeeRepository;

    public record ObMonthStats(long pending, long approved, long rejected, long total) {}

    public ObMonthStats getStatsForEmployeeMonth(long internalEmployeeId, YearMonth ym) {
        LocalDate a = ym.atDay(1);
        LocalDate b = ym.atEndOfMonth();
        List<OfficialBusinessDisplayRow> list = buildEmployeeList(internalEmployeeId, a, b);
        long p = 0, ap = 0, rj = 0;
        for (OfficialBusinessDisplayRow row : list) {
            String s = row.status() != null ? row.status() : "";
            if (s.toLowerCase().contains("pend")) {
                p++;
            } else if (s.toLowerCase().contains("appr")) {
                ap++;
            } else if (s.toLowerCase().contains("reject")) {
                rj++;
            }
        }
        return new ObMonthStats(p, ap, rj, list.size());
    }

    public ObMonthStats getStatsForMonth(YearMonth ym) {
        LocalDate a = ym.atDay(1);
        LocalDate b = ym.atEndOfMonth();
        long pe = obRepository.countByStatusAndBusinessDateBetween("PENDING", a, b);
        long ae = obRepository.countByStatusAndBusinessDateBetween("APPROVED", a, b);
        long re = obRepository.countByStatusAndBusinessDateBetween("REJECTED", a, b);
        long total = pe + ae + re;
        return new ObMonthStats(pe, ae, re, total);
    }

    public List<OfficialBusinessDisplayRow> buildAdminList(LocalDate from, LocalDate to) {
        if (from == null || to == null || from.isAfter(to)) {
            return List.of();
        }
        List<OfficialBusinessDisplayRow> rows = new ArrayList<>();
        for (EacOfficialBusinessRequest r : obRepository.findByBusinessDateBetweenOrderByBusinessDateDescIdDesc(from, to)) {
            Optional<OfficialEmployee> op = officialEmployeeRepository.findById(r.getEmployeeId());
            if (op.isEmpty()) {
                continue;
            }
            OfficialEmployee emp = op.get();
            int h = r.getObHours() != null ? r.getObHours() : 0;
            String src = "MANUAL".equalsIgnoreCase(r.getRequestSource()) ? "HR manual" : "Employee request";
            rows.add(toRow(emp, r, src, h + " h"));
        }
        rows.sort(Comparator.comparing(OfficialBusinessDisplayRow::businessDate).reversed()
            .thenComparing(OfficialBusinessDisplayRow::obRequestId));
        return rows;
    }

    public List<OfficialBusinessDisplayRow> buildEmployeeList(long internalEmployeeId, LocalDate from, LocalDate to) {
        if (from == null || to == null || from.isAfter(to)) {
            return List.of();
        }
        List<OfficialBusinessDisplayRow> rows = new ArrayList<>();
        for (EacOfficialBusinessRequest r
            : obRepository.findByEmployeeIdAndBusinessDateBetweenOrderByBusinessDateDescIdDesc(internalEmployeeId, from, to)) {
            Optional<OfficialEmployee> op = officialEmployeeRepository.findById(r.getEmployeeId());
            if (op.isEmpty()) {
                continue;
            }
            OfficialEmployee emp = op.get();
            int h = r.getObHours() != null ? r.getObHours() : 0;
            String src = "MANUAL".equalsIgnoreCase(r.getRequestSource()) ? "HR manual" : "My request";
            rows.add(toRow(emp, r, src, h + " h"));
        }
        return rows;
    }

    private OfficialBusinessDisplayRow toRow(OfficialEmployee emp, EacOfficialBusinessRequest r, String sourceLabel, String hoursLabel) {
        return new OfficialBusinessDisplayRow(
            sourceLabel,
            emp.getId(),
            emp.getCustomEmployeeId() != null ? emp.getCustomEmployeeId() : "",
            emp.getFirstName() + " " + emp.getLastName(),
            emp.getDepartment() != null ? emp.getDepartment() : "—",
            emp.getPosition() != null ? emp.getPosition() : "—",
            campusOf(emp),
            blankToDash(emp.getCampusCode()),
            r.getBusinessDate(),
            r.getStartTime() != null ? r.getStartTime().toString() : "—",
            r.getEndTime() != null ? r.getEndTime().toString() : "—",
            hoursLabel,
            r.getPurpose() != null ? r.getPurpose() : "—",
            labelStatus(r.getStatus()),
            r.getNextApprover() != null && !r.getNextApprover().isBlank() ? r.getNextApprover() : "HR",
            r.getLastActionBy() != null ? r.getLastActionBy() : "—",
            r.getAttachmentPath(),
            r.getId()
        );
    }

    private static String labelStatus(String raw) {
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

    private static String blankToDash(String c) {
        return c != null && !c.isBlank() ? c.trim() : "—";
    }

    private static String campusOf(OfficialEmployee emp) {
        String c = emp.getCampusCode();
        return c != null && !c.isBlank() ? "Campus " + c : "—";
    }

    @Transactional
    public void submitRequest(
        long employeeInternalId,
        LocalDate businessDate,
        LocalTime startTime,
        LocalTime endTime,
        Integer obHours,
        String purpose,
        String notes,
        String requestSource,
        MultipartFile attachment) throws IOException {

        if (businessDate == null) {
            throw new IllegalArgumentException("Date is required.");
        }
        if (purpose == null || purpose.isBlank()) {
            throw new IllegalArgumentException("Purpose is required.");
        }
        int oh = obHours != null ? Math.max(0, obHours) : 0;
        if (oh <= 0) {
            throw new IllegalArgumentException("OB hours must be greater than zero.");
        }
        EacOfficialBusinessRequest r = new EacOfficialBusinessRequest();
        r.setEmployeeId(employeeInternalId);
        r.setBusinessDate(businessDate);
        r.setStartTime(startTime);
        r.setEndTime(endTime);
        r.setObHours(oh);
        r.setPurpose(purpose.trim());
        r.setNotes(notes);
        r.setRequestSource(requestSource != null ? requestSource : "EMPLOYEE");
        r.setStatus("PENDING");
        r.setNextApprover("HR");
        r.setCreatedAt(LocalDateTime.now());
        if (attachment != null && !attachment.isEmpty()) {
            saveAttachment(r, attachment);
        }
        obRepository.save(r);
    }

    private void saveAttachment(EacOfficialBusinessRequest r, MultipartFile file) throws IOException {
        String orig = file.getOriginalFilename() != null ? file.getOriginalFilename() : "file";
        String ext = orig.contains(".") ? orig.substring(orig.lastIndexOf('.')) : "";
        if (ext.length() > 8) {
            ext = "";
        }
        String name = UUID.randomUUID() + ext;
        Path dir = Path.of(obUploadDir).toAbsolutePath().normalize();
        Files.createDirectories(dir);
        Path target = dir.resolve(name);
        Files.copy(file.getInputStream(), target, java.nio.file.StandardCopyOption.REPLACE_EXISTING);
        r.setAttachmentPath("/uploads/official-business-attachments/" + name);
    }

    @Transactional
    public void approveRequest(long id, String approverLabel) {
        EacOfficialBusinessRequest r = obRepository.findById(id)
            .orElseThrow(() -> new IllegalArgumentException("Request not found."));
        if (!"PENDING".equalsIgnoreCase(r.getStatus())) {
            throw new IllegalStateException("Only pending requests can be approved.");
        }
        r.setStatus("APPROVED");
        r.setLastActionBy(approverLabel);
        r.setDecidedAt(LocalDateTime.now());
        obRepository.save(r);
    }

    @Transactional
    public void rejectRequest(long id, String approverLabel) {
        EacOfficialBusinessRequest r = obRepository.findById(id)
            .orElseThrow(() -> new IllegalArgumentException("Request not found."));
        if (!"PENDING".equalsIgnoreCase(r.getStatus())) {
            throw new IllegalStateException("Only pending requests can be rejected.");
        }
        r.setStatus("REJECTED");
        r.setLastActionBy(approverLabel);
        r.setDecidedAt(LocalDateTime.now());
        obRepository.save(r);
    }
}
