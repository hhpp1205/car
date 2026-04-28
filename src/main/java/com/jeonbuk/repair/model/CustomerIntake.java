package com.jeonbuk.repair.model;

import jakarta.persistence.*;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.EnumSet;
import java.util.List;
import java.util.Set;

/**
 * 입고관리 — 차량 1대 입고 1건 = 1행.
 */
@Entity
@Table(name = "customer_intake",
        indexes = {
                @Index(name = "idx_intake_date",    columnList = "intake_date"),
                @Index(name = "idx_intake_vehicle", columnList = "vehicle_number")
        })
public class CustomerIntake {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    /** 자동 생성 — YYMMDD-NN (예: 260424-01) */
    @Column(name = "intake_no", nullable = false, unique = true, length = 32)
    private String intakeNo;

    @Column(name = "intake_date", nullable = false)
    private LocalDate intakeDate;

    @Column(name = "vehicle_name", nullable = false, length = 64)
    private String vehicleName;

    @Column(name = "vehicle_number", nullable = false, length = 32)
    private String vehicleNumber;

    @Column(length = 32)
    private String phone;

    @Column(name = "repair_type", nullable = false, length = 16)
    @Enumerated(EnumType.STRING)
    private RepairType repairType;

    /** 콤마-구분 CSV (자차,대물,일반수리,과실) */
    @Column(name = "accident_type", length = 64)
    private String accidentTypeRaw;

    @Column(name = "release_date")
    private LocalDate releaseDate;

    @Column(name = "self_pay_amount")
    private Integer selfPayAmount;

    @Column(name = "self_pay_date")
    private LocalDate selfPayDate;

    @Column(name = "tow_driver", length = 64)
    private String towDriver;

    @Column(name = "tow_amount")
    private Integer towAmount;

    @Column(length = 500)
    private String memo;

    @Column(name = "created_at", nullable = false, updatable = false)
    private LocalDateTime createdAt = LocalDateTime.now();

    @Column(name = "updated_at", nullable = false)
    private LocalDateTime updatedAt = LocalDateTime.now();

    /** 진행상태 자동 산출에 늘 필요해서 EAGER. 단일 사용자·소규모 DB라 성능 영향 미미. */
    @OneToMany(mappedBy = "intake", cascade = CascadeType.ALL, orphanRemoval = true, fetch = FetchType.EAGER)
    private List<InsuranceClaim> claims = new ArrayList<>();

    /** rentals 는 별도 화면에서만 조회 — LAZY 유지 (단, 컬렉션을 EAGER 로 둘 경우 MultipleBagFetchException 회피 위해 한쪽만 EAGER). */
    @OneToMany(mappedBy = "intake", cascade = CascadeType.ALL, orphanRemoval = true, fetch = FetchType.LAZY)
    private List<RentalHistory> rentals = new ArrayList<>();

    public CustomerIntake() {}

    @PreUpdate
    void onUpdate() {
        this.updatedAt = LocalDateTime.now();
    }

    // 사고유형 편의 메서드
    public Set<AccidentType> getAccidentTypes() {
        return AccidentType.parse(accidentTypeRaw);
    }

    public void setAccidentTypes(Set<AccidentType> set) {
        this.accidentTypeRaw = AccidentType.join(set == null ? EnumSet.noneOf(AccidentType.class) : set);
    }

    // ----- getters / setters -----

    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }

    public String getIntakeNo() { return intakeNo; }
    public void setIntakeNo(String intakeNo) { this.intakeNo = intakeNo; }

    public LocalDate getIntakeDate() { return intakeDate; }
    public void setIntakeDate(LocalDate intakeDate) { this.intakeDate = intakeDate; }

    public String getVehicleName() { return vehicleName; }
    public void setVehicleName(String vehicleName) { this.vehicleName = vehicleName; }

    public String getVehicleNumber() { return vehicleNumber; }
    public void setVehicleNumber(String vehicleNumber) { this.vehicleNumber = vehicleNumber; }

    public String getPhone() { return phone; }
    public void setPhone(String phone) { this.phone = phone; }

    public RepairType getRepairType() { return repairType; }
    public void setRepairType(RepairType repairType) { this.repairType = repairType; }

    public String getAccidentTypeRaw() { return accidentTypeRaw; }
    public void setAccidentTypeRaw(String accidentTypeRaw) { this.accidentTypeRaw = accidentTypeRaw; }

    public LocalDate getReleaseDate() { return releaseDate; }
    public void setReleaseDate(LocalDate releaseDate) { this.releaseDate = releaseDate; }

    public Integer getSelfPayAmount() { return selfPayAmount; }
    public void setSelfPayAmount(Integer selfPayAmount) { this.selfPayAmount = selfPayAmount; }

    public LocalDate getSelfPayDate() { return selfPayDate; }
    public void setSelfPayDate(LocalDate selfPayDate) { this.selfPayDate = selfPayDate; }

    public String getTowDriver() { return towDriver; }
    public void setTowDriver(String towDriver) { this.towDriver = towDriver; }

    public Integer getTowAmount() { return towAmount; }
    public void setTowAmount(Integer towAmount) { this.towAmount = towAmount; }

    public String getMemo() { return memo; }
    public void setMemo(String memo) { this.memo = memo; }

    public LocalDateTime getCreatedAt() { return createdAt; }
    public void setCreatedAt(LocalDateTime createdAt) { this.createdAt = createdAt; }

    public LocalDateTime getUpdatedAt() { return updatedAt; }
    public void setUpdatedAt(LocalDateTime updatedAt) { this.updatedAt = updatedAt; }

    public List<InsuranceClaim> getClaims() { return claims; }
    public void setClaims(List<InsuranceClaim> claims) { this.claims = claims; }

    public List<RentalHistory> getRentals() { return rentals; }
    public void setRentals(List<RentalHistory> rentals) { this.rentals = rentals; }
}
