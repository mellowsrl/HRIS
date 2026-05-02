package com.example.employee.repository;

import com.example.employee.model.EacOvertimeRequest;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.time.LocalDate;
import java.util.List;

@Repository
public interface EacOvertimeRequestRepository extends JpaRepository<EacOvertimeRequest, Long> {

    List<EacOvertimeRequest> findByWorkDateBetweenOrderByWorkDateDescIdDesc(LocalDate from, LocalDate to);

    List<EacOvertimeRequest> findByEmployeeIdAndWorkDateBetweenOrderByWorkDateDescIdDesc(
        Long employeeId, LocalDate from, LocalDate to);

    long countByStatusAndWorkDateBetween(String status, LocalDate from, LocalDate to);

    long countByEmployeeIdAndStatusAndWorkDateBetween(Long employeeId, String status, LocalDate from, LocalDate to);
}
