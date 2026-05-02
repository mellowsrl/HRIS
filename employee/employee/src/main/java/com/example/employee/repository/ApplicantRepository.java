package com.example.employee.repository;

import com.example.employee.model.Applicant;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.query.Procedure;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.LocalDate;
import java.util.List;
import java.util.Map;

@Repository
public interface ApplicantRepository extends JpaRepository<Applicant, Integer> {

    List<Applicant> findByStatus(String status);
    List<Applicant> findByStatusIn(List<String> statuses);
    List<Applicant> findAllByOrderByIdDesc();
    List<Applicant> findByStatusOrderByIdDesc(String status);
 // Add this to your existing repository methods!
    java.util.Optional<com.example.employee.model.Applicant> findByCustomEmployeeId(String customEmployeeId);
    // UPDATED WITH DATES
    @Procedure(name = "Applicant.computePayroll")
    Map<String, Object> computePayroll(
        @Param("p_emp_id") Integer empId,
        @Param("p_start_date") LocalDate startDate,
        @Param("p_end_date") LocalDate endDate
    );
}