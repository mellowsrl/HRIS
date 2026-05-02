package com.example.employee.repository;

import com.example.employee.model.TeachingPay;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface TeachingPayRepository extends JpaRepository<TeachingPay, Long> {

    List<TeachingPay> findAllByOrderByPeriodStartDescIdDesc();

    List<TeachingPay> findByEmployeeIdOrderByPeriodStartDesc(Long employeeId);
}
