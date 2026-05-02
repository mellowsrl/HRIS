package com.example.employee.repository;

import com.example.employee.model.AttendanceLog;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.LocalDate;
import java.util.List;
import java.util.Optional;

@Repository
public interface AttendanceRepository extends JpaRepository<AttendanceLog, Long> {

    // FIXED: employeeId is now a String
    @Query("SELECT a FROM AttendanceLog a WHERE a.employeeId = :employeeId AND a.date = :date")
    Optional<AttendanceLog> findByEmployeeIdAndDate(@Param("employeeId") String employeeId, @Param("date") LocalDate date);

    // FIXED: employeeId is now a String
    @Query("SELECT a FROM AttendanceLog a WHERE a.employeeId = :employeeId ORDER BY a.date DESC")
    List<AttendanceLog> findByEmployeeIdOrderByDateDesc(@Param("employeeId") String employeeId);

    // FIXED: Uses 'timeOut' instead of 'timeOutIsNull' based on the new model
    @Query("SELECT a FROM AttendanceLog a WHERE a.timeOut IS NULL AND a.date = :date")
    List<AttendanceLog> findByTimeOutIsNullAndDate(@Param("date") LocalDate date);

    @Query("SELECT a FROM AttendanceLog a WHERE a.otApprovalStatus = 'PENDING' AND a.date >= :start AND a.date <= :end ORDER BY a.date DESC, a.id DESC")
    List<AttendanceLog> findPendingOvertimeBetween(@Param("start") LocalDate start, @Param("end") LocalDate end);

    @Query("SELECT a FROM AttendanceLog a WHERE a.employeeId = :eid AND a.date >= :start AND a.date <= :end AND "
        + "(COALESCE(a.overtimeReported, 0) > 0 OR COALESCE(a.overtimeHours, 0) > 0 "
        + "OR a.otApprovalStatus = 'PENDING' OR a.otApprovalStatus = 'REJECTED') "
        + "ORDER BY a.date DESC")
    List<AttendanceLog> findOvertimeReviewLinesForEmployee(
        @Param("eid") String employeeId,
        @Param("start") LocalDate start,
        @Param("end") LocalDate end
    );

    @Query("SELECT a FROM AttendanceLog a WHERE a.date >= :from AND a.date <= :to ORDER BY a.date DESC, a.id DESC")
    List<AttendanceLog> findByDateRange(@Param("from") LocalDate from, @Param("to") LocalDate to);

    @Query("SELECT a FROM AttendanceLog a WHERE a.date >= :f AND a.date <= :t AND "
        + "(COALESCE(a.overtimeReported, 0) > 0 OR COALESCE(a.overtimeHours, 0) > 0 "
        + "OR (a.otApprovalStatus IS NOT NULL AND UPPER(TRIM(a.otApprovalStatus)) <> 'NONE' AND a.otApprovalStatus <> ''))")
    List<AttendanceLog> findOvertimeRelatedInDateRange(@Param("f") LocalDate f, @Param("t") LocalDate t);

    @Query("SELECT COUNT(a) FROM AttendanceLog a WHERE a.date >= :a AND a.date <= :b "
        + "AND a.otApprovalStatus = 'PENDING' AND COALESCE(a.overtimeReported, 0) > 0")
    long countTcmsOvertimePendingMonth(@Param("a") LocalDate a, @Param("b") LocalDate b);

    @Query("SELECT COUNT(a) FROM AttendanceLog a WHERE a.date >= :a AND a.date <= :b "
        + "AND a.otApprovalStatus = 'APPROVED' AND COALESCE(a.overtimeHours, 0) > 0")
    long countTcmsOvertimeApprovedMonth(@Param("a") LocalDate a, @Param("b") LocalDate b);

    @Query("SELECT COUNT(a) FROM AttendanceLog a WHERE a.date >= :a AND a.date <= :b AND a.otApprovalStatus = 'REJECTED'")
    long countTcmsOvertimeRejectedMonth(@Param("a") LocalDate a, @Param("b") LocalDate b);
}