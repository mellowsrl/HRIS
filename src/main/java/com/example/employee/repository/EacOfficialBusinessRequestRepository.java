package com.example.employee.repository;

import com.example.employee.model.EacOfficialBusinessRequest;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.time.LocalDate;
import java.util.List;

@Repository
public interface EacOfficialBusinessRequestRepository extends JpaRepository<EacOfficialBusinessRequest, Long> {

    List<EacOfficialBusinessRequest> findByBusinessDateBetweenOrderByBusinessDateDescIdDesc(
        LocalDate from, LocalDate to);

    List<EacOfficialBusinessRequest> findByEmployeeIdAndBusinessDateBetweenOrderByBusinessDateDescIdDesc(
        Long employeeId, LocalDate from, LocalDate to);

    long countByStatusAndBusinessDateBetween(String status, LocalDate from, LocalDate to);
}
