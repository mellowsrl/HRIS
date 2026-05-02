package com.example.employee.model;

import jakarta.persistence.*;
import java.time.LocalDate;
import java.time.LocalTime;

@Entity
@Table(name = "suspension")
public class Suspension {
    
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "suspension_date", nullable = false)
    private LocalDate date;
    
    @Column(nullable = false)
    private String reason;

    // NULL means full-day suspension. A specific time (e.g., 10:00 AM) handles partial suspensions.
    @Column(name = "start_time")
    private LocalTime startTime;

    /** When null, suspension applies to all employees (e.g. typhoon). Otherwise only this employee. */
    @Column(name = "employee_id")
    private Long employeeId;

    public Suspension() {}

    public Suspension(LocalDate date, String reason, LocalTime startTime, Long employeeId) {
        this.date = date;
        this.reason = reason;
        this.startTime = startTime;
        this.employeeId = employeeId;
    }

    // ==========================================
    // GETTERS AND SETTERS
    // ==========================================
    public Long getId() { 
        return id; 
    }
    public void setId(Long id) { 
        this.id = id; 
    }
    
    public LocalDate getDate() { 
        return date; 
    }
    public void setDate(LocalDate date) { 
        this.date = date; 
    }
    
    public String getReason() { 
        return reason; 
    }
    public void setReason(String reason) { 
        this.reason = reason; 
    }
    
    public LocalTime getStartTime() { 
        return startTime; 
    }
    public void setStartTime(LocalTime startTime) { 
        this.startTime = startTime; 
    }

    public Long getEmployeeId() {
        return employeeId;
    }

    public void setEmployeeId(Long employeeId) {
        this.employeeId = employeeId;
    }
}