package com.example.employee.repository;

import com.example.employee.model.Applicant;
import com.example.employee.model.AppUser;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface ApplicantRepository extends JpaRepository<Applicant, Integer> {
    
    // For the Applicant Dashboard
    List<Applicant> findByUser(AppUser user);
    
    // FIXED: For the HR Dashboard (All Applicants, newest first)
    List<Applicant> findAllByOrderByIdDesc();
    
    // FIXED: For the HR Dashboard (Filtered by Status, newest first)
    List<Applicant> findByStatusOrderByIdDesc(String status);

    long countByStatus(String status);
}