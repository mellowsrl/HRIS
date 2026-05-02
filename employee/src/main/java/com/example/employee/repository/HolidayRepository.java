package com.example.employee.repository;

import com.example.employee.model.Holiday;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;
import java.time.LocalDate;
import java.util.Optional;

@Repository
public interface HolidayRepository extends JpaRepository<Holiday, Integer> {
    
    // This custom method allows us to instantly check if a specific date is a holiday
    Optional<Holiday> findByDate(LocalDate date);
    
}