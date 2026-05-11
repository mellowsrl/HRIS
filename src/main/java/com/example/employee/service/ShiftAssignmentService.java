package com.example.employee.service;

import com.example.employee.model.OfficialEmployee;
import com.example.employee.model.ShiftAssignment;
import com.example.employee.model.ShiftSchedule;
import com.example.employee.repository.OfficialEmployeeRepository;
import com.example.employee.repository.ShiftAssignmentRepository;
import com.example.employee.repository.ShiftScheduleRepository;
import org.springframework.stereotype.Service;

import java.time.LocalDate;
import java.time.LocalTime;
import java.util.List;

@Service
public class ShiftAssignmentService {
    private final ShiftAssignmentRepository assignmentRepository;
    private final OfficialEmployeeRepository employeeRepository;
    private final ShiftScheduleRepository shiftRepository;

    public ShiftAssignmentService(
        ShiftAssignmentRepository assignmentRepository,
        OfficialEmployeeRepository employeeRepository,
        ShiftScheduleRepository shiftRepository
    ) {
        this.assignmentRepository = assignmentRepository;
        this.employeeRepository = employeeRepository;
        this.shiftRepository = shiftRepository;
    }

    public List<ShiftAssignment> listByRange(LocalDate from, LocalDate to) {
        return assignmentRepository.findByWorkDateBetween(from, to);
    }

    public void assign(
        Long employeeId,
        Long shiftId,
        LocalDate workDate,
        String username,
        String source,
        String notes,
        LocalTime overrideTimeIn,
        LocalTime overrideTimeOut,
        Integer overrideBreakMinutes,
        Integer overrideGraceMinutes
    ) {
        OfficialEmployee emp = employeeRepository.findById(employeeId).orElseThrow();
        ShiftSchedule shift = shiftRepository.findById(shiftId).orElseThrow();
        ShiftAssignment row = assignmentRepository.findByEmployeeIdAndWorkDate(employeeId, workDate).orElseGet(ShiftAssignment::new);
        row.setEmployee(emp);
        row.setShift(shift);
        row.setWorkDate(workDate);
        row.setSource(source == null || source.isBlank() ? "manual" : source.trim());
        row.setNotes(notes != null && !notes.isBlank() ? notes.trim() : null);
        row.setOverrideTimeIn(overrideTimeIn);
        row.setOverrideTimeOut(overrideTimeOut);
        row.setOverrideBreakMinutes(overrideBreakMinutes);
        row.setOverrideGraceMinutes(overrideGraceMinutes);
        if (row.getId() == null) {
            row.setCreatedBy(username);
        }
        row.setUpdatedBy(username);
        assignmentRepository.save(row);
        emp.setExpectedShift(shift.getName());
        employeeRepository.save(emp);
    }
}

