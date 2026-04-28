package com.jeonbuk.repair.model;

import jakarta.persistence.*;

import java.time.LocalDate;
import java.time.LocalDateTime;

/**
 * 대차이력 — 차종 변경 시 행 추가 (1 입고에 N 대차).
 */
@Entity
@Table(name = "rental_history",
        indexes = {
                @Index(name = "idx_rental_intake",  columnList = "intake_id"),
                @Index(name = "idx_rental_vehicle", columnList = "rental_vehicle_id"),
                @Index(name = "idx_rental_active",  columnList = "rental_end_date")
        })
public class RentalHistory {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    /** 대차 목록 화면에서 입고번호를 표시해야 하므로 EAGER. */
    @ManyToOne(fetch = FetchType.EAGER, optional = false)
    @JoinColumn(name = "intake_id", nullable = false)
    private CustomerIntake intake;

    @ManyToOne(fetch = FetchType.EAGER, optional = false)
    @JoinColumn(name = "rental_vehicle_id", nullable = false)
    private RentalVehicle rentalVehicle;

    @Column(name = "rental_start_date", nullable = false)
    private LocalDate rentalStartDate;

    /** NULL = 대차중 */
    @Column(name = "rental_end_date")
    private LocalDate rentalEndDate;

    @Column(length = 500)
    private String memo;

    @Column(name = "created_at", nullable = false, updatable = false)
    private LocalDateTime createdAt = LocalDateTime.now();

    @Column(name = "updated_at", nullable = false)
    private LocalDateTime updatedAt = LocalDateTime.now();

    public RentalHistory() {}

    @PreUpdate
    void onUpdate() {
        this.updatedAt = LocalDateTime.now();
    }

    public boolean isActive() {
        return rentalEndDate == null;
    }

    // ----- getters / setters -----

    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }

    public CustomerIntake getIntake() { return intake; }
    public void setIntake(CustomerIntake intake) { this.intake = intake; }

    public RentalVehicle getRentalVehicle() { return rentalVehicle; }
    public void setRentalVehicle(RentalVehicle rentalVehicle) { this.rentalVehicle = rentalVehicle; }

    public LocalDate getRentalStartDate() { return rentalStartDate; }
    public void setRentalStartDate(LocalDate rentalStartDate) { this.rentalStartDate = rentalStartDate; }

    public LocalDate getRentalEndDate() { return rentalEndDate; }
    public void setRentalEndDate(LocalDate rentalEndDate) { this.rentalEndDate = rentalEndDate; }

    public String getMemo() { return memo; }
    public void setMemo(String memo) { this.memo = memo; }

    public LocalDateTime getCreatedAt() { return createdAt; }
    public LocalDateTime getUpdatedAt() { return updatedAt; }
}
