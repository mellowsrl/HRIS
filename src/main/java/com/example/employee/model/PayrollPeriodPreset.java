package com.example.employee.model;

import jakarta.persistence.*;

import java.time.Instant;

/**
 * Optional named pay window: stores the same {@code cutoffKey} string used elsewhere
 * ({@code YYYY-M-H} or {@code YYYY-MM-DD_YYYY-MM-DD}).
 */
@Entity
@Table(name = "payroll_period_preset")
public class PayrollPeriodPreset {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false, length = 120)
    private String name;

    @Column(name = "cutoff_key", nullable = false, length = 64)
    private String cutoffKey;

    @Column(nullable = false)
    private boolean active = true;

    @Column(name = "created_at", nullable = false)
    private Instant createdAt = Instant.now();

    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }
    public String getName() { return name; }
    public void setName(String name) { this.name = name; }
    public String getCutoffKey() { return cutoffKey; }
    public void setCutoffKey(String cutoffKey) { this.cutoffKey = cutoffKey; }
    public boolean isActive() { return active; }
    public void setActive(boolean active) { this.active = active; }
    public Instant getCreatedAt() { return createdAt; }
    public void setCreatedAt(Instant createdAt) { this.createdAt = createdAt; }
}
