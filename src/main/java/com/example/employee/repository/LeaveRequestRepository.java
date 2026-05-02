package com.example.employee.repository;

import com.example.employee.model.LeaveRequest;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.time.LocalDate;
import java.util.List;

public interface LeaveRequestRepository extends JpaRepository<LeaveRequest, Integer> {

    List<LeaveRequest> findByStatusOrderByIdDesc(String status);
    List<LeaveRequest> findByEmployeeIdOrderByIdDesc(int employeeId);

    List<LeaveRequest> findAllByOrderByStartDateDesc();

    /** Overlap with [rangeStart, rangeEnd]: startDate <= rangeEnd AND endDate >= rangeStart */
    @Query("SELECT l FROM LeaveRequest l WHERE l.startDate IS NOT NULL AND l.endDate IS NOT NULL"
        + " AND l.startDate <= :rangeEnd AND l.endDate >= :rangeStart ORDER BY l.startDate DESC, l.id DESC")
    List<LeaveRequest> findOverlappingDateRange(
        @Param("rangeStart") LocalDate rangeStart,
        @Param("rangeEnd") LocalDate rangeEnd);

    @Query("SELECT l FROM LeaveRequest l WHERE l.employeeId = :empId AND l.startDate IS NOT NULL AND l.endDate IS NOT NULL"
        + " AND l.startDate <= :rangeEnd AND l.endDate >= :rangeStart ORDER BY l.startDate DESC, l.id DESC")
    List<LeaveRequest> findByEmployeeIdOverlapping(
        @Param("empId") int employeeId,
        @Param("rangeStart") LocalDate rangeStart,
        @Param("rangeEnd") LocalDate rangeEnd);

    @Query("SELECT DISTINCT l.leaveType FROM LeaveRequest l WHERE l.leaveType IS NOT NULL AND l.leaveType <> '' ORDER BY l.leaveType")
    List<String> findDistinctLeaveTypes();
}