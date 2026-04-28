package com.jeonbuk.repair.model;

import jakarta.persistence.*;

@Entity
@Table(name = "insurance_company")
public class InsuranceCompany {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false, unique = true, length = 64)
    private String name;

    public InsuranceCompany() {}

    public InsuranceCompany(String name) {
        this.name = name;
    }

    @Override
    public String toString() {
        return name;
    }

    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }

    public String getName() { return name; }
    public void setName(String name) { this.name = name; }
}
