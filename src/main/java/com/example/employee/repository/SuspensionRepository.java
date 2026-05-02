package com.example.employee.repository; // MUST BE THIS

import com.example.employee.model.Suspension;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.time.LocalDate;
import java.util.List;

@Repository
public interface SuspensionRepository extends JpaRepository<Suspension, Long> {
    List<Suspension> findAllByOrderByDateDesc();

    List<Suspension> findByDateBetweenOrderByDateAsc(LocalDate startInclusive, LocalDate endInclusive);
}