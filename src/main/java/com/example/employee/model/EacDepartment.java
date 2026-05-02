package com.example.employee.model;

import jakarta.persistence.*;

/**
 * Collegiate and administrative units for Emilio Aguinaldo College.
 * {@link OfficialEmployee#department} stores the same value as {@link #code} (e.g. SON, SPH).
 */
@Entity
@Table(name = "eac_department")
public class EacDepartment {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false, length = 200)
    private String name;

    /** Short code; matches employee.department_code */
    @Column(nullable = false, unique = true, length = 20)
    private String code;

    @Column(name = "head_name", length = 200)
    private String headName;

    /** Campus / site label, e.g. Main (Manila), Dasmariñas */
    @Column(nullable = false, length = 120)
    private String branch = "Main (Manila)";

    @Column(nullable = false, length = 20)
    private String status = "Active";

    public EacDepartment() {}

    public EacDepartment(String name, String code, String headName, String branch, String status) {
        this.name = name;
        this.code = code;
        this.headName = headName;
        this.branch = branch;
        this.status = status;
    }

    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }
    public String getName() { return name; }
    public void setName(String name) { this.name = name; }
    public String getCode() { return code; }
    public void setCode(String code) { this.code = code; }
    public String getHeadName() { return headName; }
    public void setHeadName(String headName) { this.headName = headName; }
    public String getBranch() { return branch; }
    public void setBranch(String branch) { this.branch = branch; }
    public String getStatus() { return status; }
    public void setStatus(String status) { this.status = status; }
}
