package com.example.employee.service;

import com.example.employee.model.ShiftSchedule;
import com.example.employee.repository.ShiftScheduleRepository;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.Optional;

@Service
public class ShiftScheduleService {
    private final ShiftScheduleRepository repository;

    public ShiftScheduleService(ShiftScheduleRepository repository) {
        this.repository = repository;
    }

    public List<ShiftSchedule> listAll() {
        return repository.findAllByOrderByNameAsc();
    }

    public List<ShiftSchedule> listActive() {
        return repository.findByActiveTrueOrderByNameAsc();
    }

    public Optional<ShiftSchedule> findById(Long id) {
        return repository.findById(id);
    }

    public ShiftSchedule save(ShiftSchedule shift) {
        if (shift.getName() != null) {
            shift.setName(shift.getName().trim());
        }
        if (shift.getBranchCode() == null || shift.getBranchCode().isBlank()) {
            shift.setBranchCode("ALL");
        } else {
            shift.setBranchCode(shift.getBranchCode().trim());
        }
        if (shift.getMissingPunchPolicy() == null || shift.getMissingPunchPolicy().isBlank()) {
            shift.setMissingPunchPolicy("REVIEW_REQUIRED");
        } else {
            shift.setMissingPunchPolicy(shift.getMissingPunchPolicy().trim().toUpperCase());
        }
        return repository.save(shift);
    }

    public void archive(Long id, String username) {
        repository.findById(id).ifPresent(shift -> {
            shift.setActive(false);
            shift.setUpdatedBy(username);
            repository.save(shift);
        });
    }
}

