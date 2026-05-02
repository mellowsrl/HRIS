package com.example.employee.repository;

import com.example.employee.model.JobPosting; // Make sure this matches where your JobPosting.java is saved!
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface JobPostingRepository extends JpaRepository<JobPosting, Integer> {

    // SPRING BOOT MAGIC: Just by naming the method "findByStatus", 
    // Spring automatically writes the SQL: SELECT * FROM job_postings WHERE status = ?
    List<JobPosting> findByStatus(String status);
    
    // Optional: If you ever want to order the open jobs by newest first
    List<JobPosting> findByStatusOrderByDatePostedDesc(String status);
}