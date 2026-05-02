package com.example.employee.repository;

import com.example.employee.model.HrAuditEvent;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface HrAuditEventRepository extends JpaRepository<HrAuditEvent, Long> {
    List<HrAuditEvent> findByOrderByIdDesc(Pageable pageable);
}
