package com.example.employee.repository;

import com.example.employee.model.PayrollPeriodPreset;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface PayrollPeriodPresetRepository extends JpaRepository<PayrollPeriodPreset, Long> {
    List<PayrollPeriodPreset> findByActiveTrueOrderByNameAsc();
}
