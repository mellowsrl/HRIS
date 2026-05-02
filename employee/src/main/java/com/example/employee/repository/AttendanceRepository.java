package com.example.employee.repository;

import com.example.employee.model.AttendanceLog;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.time.LocalDate;
import java.util.List; // THIS WAS MISSING!
import java.util.Optional;

@Repository
public interface AttendanceRepository extends JpaRepository<AttendanceLog, Integer> {
    Optional<AttendanceLog> findByEmployeeIdAndLogDate(int employeeId, LocalDate logDate);
    int countByEmployeeId(int employeeId);
    
    // THIS FIXES THE MIDNIGHT SCRIPT ERROR
    List<AttendanceLog> findByTimeOutIsNullAndLogDate(LocalDate logDate);
    List<AttendanceLog> findByEmployeeIdOrderByLogDateDesc(int employeeId);
}