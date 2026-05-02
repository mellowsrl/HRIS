package com.example.employee.service;

import com.example.employee.model.OfficialEmployee;
import com.example.employee.model.TeachingPay;
import com.example.employee.repository.OfficialEmployeeRepository;
import com.example.employee.repository.TeachingPayRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.*;

@Service
public class TeachingPayService {

    @Autowired
    private TeachingPayRepository teachingPayRepository;
    @Autowired
    private OfficialEmployeeRepository officialEmployeeRepository;

    public List<TeachingPay> listAll(Long employeeId) {
        if (employeeId != null && employeeId > 0) {
            return teachingPayRepository.findByEmployeeIdOrderByPeriodStartDesc(employeeId);
        }
        return teachingPayRepository.findAllByOrderByPeriodStartDescIdDesc();
    }

    public Map<Long, String> buildEmployeeIdToLabel() {
        Map<Long, String> map = new HashMap<>();
        for (OfficialEmployee e : officialEmployeeRepository.findAll()) {
            if (e.getId() == null) continue;
            String name = (e.getFirstName() != null ? e.getFirstName() : "") + " " + (e.getLastName() != null ? e.getLastName() : "");
            String cid = e.getCustomEmployeeId() != null ? e.getCustomEmployeeId() : String.valueOf(e.getId());
            map.put(e.getId(), (cid + " — " + name).trim());
        }
        return map;
    }

    public Optional<TeachingPay> findById(long id) {
        return teachingPayRepository.findById(id);
    }

    public void validateForSave(TeachingPay t) {
        if (t.getEmployeeId() == null) {
            throw new IllegalArgumentException("Employee is required.");
        }
        if (officialEmployeeRepository.findById(t.getEmployeeId()).isEmpty()) {
            throw new IllegalArgumentException("Invalid employee_id.");
        }
        if (t.getPeriodStart() == null || t.getPeriodEnd() == null) {
            throw new IllegalArgumentException("Pay period start and end are required.");
        }
        if (t.getPeriodEnd().isBefore(t.getPeriodStart())) {
            throw new IllegalArgumentException("Period end must be on or after period start.");
        }
        if (t.getTotalTeachingPay() == null) {
            t.setTotalTeachingPay(0.0);
        }
    }

    @Transactional
    public TeachingPay save(TeachingPay row) {
        validateForSave(row);
        return teachingPayRepository.save(row);
    }

    @Transactional
    public void delete(long id) {
        teachingPayRepository.deleteById(id);
    }

    @Transactional
    public TeachingPay update(long id, TeachingPay incoming) {
        TeachingPay existing = teachingPayRepository.findById(id)
            .orElseThrow(() -> new IllegalArgumentException("Record not found."));
        validateForSave(incoming);
        existing.setEmployeeId(incoming.getEmployeeId());
        existing.setPeriodStart(incoming.getPeriodStart());
        existing.setPeriodEnd(incoming.getPeriodEnd());
        existing.setTotalTeachingPay(incoming.getTotalTeachingPay());
        existing.setHourlyRate(incoming.getHourlyRate());
        existing.setLecPay(incoming.getLecPay());
        existing.setLabPay(incoming.getLabPay());
        existing.setLabRate(incoming.getLabRate());
        existing.setExcessLecHours(incoming.getExcessLecHours());
        existing.setExcessLabHours(incoming.getExcessLabHours());
        existing.setExcessLecUnits(incoming.getExcessLecUnits());
        existing.setExcessLabUnits(incoming.getExcessLabUnits());
        existing.setTotalLecUnits(incoming.getTotalLecUnits());
        existing.setTotalLabUnits(incoming.getTotalLabUnits());
        existing.setTotalHours(incoming.getTotalHours());
        existing.setTotalLabHours(incoming.getTotalLabHours());
        existing.setTotalLecHours(incoming.getTotalLecHours());
        existing.setHolidayPay(incoming.getHolidayPay());
        existing.setSuspensionDeduction(incoming.getSuspensionDeduction());
        existing.setTotalExcessHours(incoming.getTotalExcessHours());
        existing.setAdjustmentHours(incoming.getAdjustmentHours());
        existing.setAdjustmentPay(incoming.getAdjustmentPay());
        existing.setAdminPay(incoming.getAdminPay());
        existing.setDeductionHours(incoming.getDeductionHours());
        existing.setHonorarium(incoming.getHonorarium());
        existing.setRlePay(incoming.getRlePay());
        existing.setRleRate(incoming.getRleRate());
        existing.setSgdHours(incoming.getSgdHours());
        existing.setSgdPay(incoming.getSgdPay());
        existing.setSubstituteHours(incoming.getSubstituteHours());
        existing.setSubstitutePay(incoming.getSubstitutePay());
        existing.setSupplementalPay(incoming.getSupplementalPay());
        existing.setTotalRleHours(incoming.getTotalRleHours());
        existing.setTutorialLabHours(incoming.getTutorialLabHours());
        existing.setTutorialLabPay(incoming.getTutorialLabPay());
        existing.setTutorialLecHours(incoming.getTutorialLecHours());
        existing.setTutorialLecPay(incoming.getTutorialLecPay());
        existing.setWorkloadClassification(incoming.getWorkloadClassification());
        existing.setAbsentDeductionHours(incoming.getAbsentDeductionHours());
        existing.setExcessRleHours(incoming.getExcessRleHours());
        return teachingPayRepository.save(existing);
    }
}
