package com.example.employee.repository;

import com.example.employee.model.OfficialEmployee;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.jpa.repository.query.Procedure;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.LocalDate;
import java.util.List;
import java.util.Map;
import java.util.Optional;

@Repository
public interface OfficialEmployeeRepository extends JpaRepository<OfficialEmployee, Integer> {
    
    Optional<OfficialEmployee> findByCustomEmployeeId(String customEmployeeId);
    
    List<OfficialEmployee> findByStatus(String status);
    List<OfficialEmployee> findByStatusIn(List<String> statuses);

    @Procedure(name = "OfficialEmployee.computePayroll")
    Map<String, Object> computePayroll(
            @Param("p_emp_id") Integer empId,
            @Param("p_start_date") LocalDate startDate,
            @Param("p_end_date") LocalDate endDate
    );
}