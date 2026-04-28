package com.jeonbuk.repair.model;

import jakarta.persistence.*;

/**
 * 회사 보유 대차 차량 마스터.
 */
@Entity
@Table(name = "rental_vehicle")
public class RentalVehicle {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false, length = 64)
    private String name;

    @Column(nullable = false, unique = true, length = 32)
    private String number;

    @Column(length = 32)
    private String fuel;

    @Column(length = 32)
    private String insurance;

    @Column(name = "age_limit", length = 32)
    private String ageLimit;

    @Column(length = 32)
    private String owner;

    @Column(nullable = false)
    private boolean active = true;

    public RentalVehicle() {}

    public RentalVehicle(String name, String number, String fuel, String insurance, String ageLimit, String owner) {
        this.name = name;
        this.number = number;
        this.fuel = fuel;
        this.insurance = insurance;
        this.ageLimit = ageLimit;
        this.owner = owner;
    }

    @Override
    public String toString() {
        return name + " (" + number + ")";
    }

    // ----- getters / setters -----

    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }

    public String getName() { return name; }
    public void setName(String name) { this.name = name; }

    public String getNumber() { return number; }
    public void setNumber(String number) { this.number = number; }

    public String getFuel() { return fuel; }
    public void setFuel(String fuel) { this.fuel = fuel; }

    public String getInsurance() { return insurance; }
    public void setInsurance(String insurance) { this.insurance = insurance; }

    public String getAgeLimit() { return ageLimit; }
    public void setAgeLimit(String ageLimit) { this.ageLimit = ageLimit; }

    public String getOwner() { return owner; }
    public void setOwner(String owner) { this.owner = owner; }

    public boolean isActive() { return active; }
    public void setActive(boolean active) { this.active = active; }
}
