package com.example.employee.repository;

import com.example.employee.model.ShiftSchedule;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface ShiftScheduleRepository extends JpaRepository<ShiftSchedule, Long> {
    List<ShiftSchedule> findByActiveTrueOrderByNameAsc();
    List<ShiftSchedule> findAllByOrderByNameAsc();
}

