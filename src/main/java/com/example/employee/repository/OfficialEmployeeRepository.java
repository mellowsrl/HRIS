package com.example.employee.repository;

import com.example.employee.model.OfficialEmployee;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.Optional;

@Repository
public interface OfficialEmployeeRepository extends JpaRepository<OfficialEmployee, Long> {
    
    // Finds the employee using your 1-00001 ID format
    Optional<OfficialEmployee> findByCustomEmployeeId(String customEmployeeId);
    
    // Finds all active employees (their SQL uses 'Active' instead of 'HIRED')
    Iterable<OfficialEmployee> findByStatus(String status);

    /** Per-campus sequence for EAC ID format: 1-xxxxx, 2-xxxxx */
    long countByCustomEmployeeIdStartingWith(String prefix);

    @Query("""
        SELECT COUNT(e)
        FROM OfficialEmployee e
        WHERE e.status = :status
          AND e.department IS NOT NULL
          AND (
                UPPER(TRIM(e.department)) = UPPER(:code)
             OR UPPER(TRIM(e.department)) IN (
                    SELECT UPPER(TRIM(d.name))
                    FROM EacDepartment d
                    WHERE UPPER(TRIM(d.code)) = UPPER(:code)
                )
          )
        """)
    long countActiveByDepartmentCode(@Param("code") String code, @Param("status") String status);

    @Query("SELECT e FROM OfficialEmployee e WHERE e.status = 'Active' AND e.birthDate IS NOT NULL AND FUNCTION('MONTH', e.birthDate) = :month AND FUNCTION('DAY', e.birthDate) = :day ORDER BY e.firstName ASC")
    java.util.List<OfficialEmployee> findActiveByBirthMonthDay(@Param("month") int month, @Param("day") int day);

    @Query("SELECT e FROM OfficialEmployee e WHERE e.status = 'Active' AND e.birthDate IS NOT NULL AND FUNCTION('MONTH', e.birthDate) = :month AND FUNCTION('DAY', e.birthDate) BETWEEN :dayStart AND :dayEnd ORDER BY FUNCTION('DAY', e.birthDate) ASC, e.firstName ASC")
    java.util.List<OfficialEmployee> findActiveByBirthMonthDayRange(@Param("month") int month, @Param("dayStart") int dayStart, @Param("dayEnd") int dayEnd);
}