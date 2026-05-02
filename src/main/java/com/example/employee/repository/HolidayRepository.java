package com.example.employee.repository;

import com.example.employee.model.Holiday;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;
import java.time.LocalDate;
import java.util.List;
import java.util.Optional;

@Repository
public interface HolidayRepository extends JpaRepository<Holiday, Integer> {

    Optional<Holiday> findByDate(LocalDate date);

    List<Holiday> findByDateBetweenOrderByDateAsc(LocalDate startInclusive, LocalDate endInclusive);
}