package com.example.employee.repository;

import com.example.employee.model.EacDepartment;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;

@Repository
public interface EacDepartmentRepository extends JpaRepository<EacDepartment, Long> {

    Optional<EacDepartment> findByCodeIgnoreCase(String code);
    Optional<EacDepartment> findByNameIgnoreCase(String name);

    boolean existsByCodeIgnoreCase(String code);

    boolean existsByCodeIgnoreCaseAndIdNot(String code, long id);
}
