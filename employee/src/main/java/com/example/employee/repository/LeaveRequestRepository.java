package com.example.employee.repository;

import com.example.employee.model.LeaveRequest;
import org.springframework.data.jpa.repository.JpaRepository;

public interface LeaveRequestRepository extends JpaRepository<LeaveRequest, Integer> {
    
    java.util.List<LeaveRequest> findByStatusOrderByIdDesc(String status);
    
    // THE NEW LINE YOU JUST ADDED:
    java.util.List<LeaveRequest> findByEmployeeIdOrderByIdDesc(int employeeId);
}