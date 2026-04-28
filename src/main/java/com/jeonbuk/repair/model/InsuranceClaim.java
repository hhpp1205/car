package com.jeonbuk.repair.model;

import jakarta.persistence.*;

import java.time.LocalDate;
import java.time.LocalDateTime;

/**
 * 보험청구 — 1건의 입고에 대해 자차/상대 각각 0..1건.
 */
@Entity
@Table(name = "insurance_claim",
        indexes = {@Index(name = "idx_claim_intake", columnList = "intake_id")})
public class InsuranceClaim {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    /** 청구 목록 화면에서 입고번호를 표시해야 하므로 EAGER. */
    @ManyToOne(fetch = FetchType.EAGER, optional = false)
    @JoinColumn(name = "intake_id", nullable = false)
    private CustomerIntake intake;

    @Column(name = "claim_side", nullable = false, length = 16)
    @Enumerated(EnumType.STRING)
    private ClaimSide claimSide;

    @Column(name = "insurance_company", length = 64)
    private String insuranceCompany;

    @Column(name = "claim_date")
    private LocalDate claimDate;

    @Column(name = "claim_amount")
    private Integer claimAmount;

    @Column(name = "received_amount")
    private Integer receivedAmount;

    @Column(name = "received_date")
    private LocalDate receivedDate;

    @Column(length = 500)
    private String memo;

    @Column(name = "created_at", nullable = false, updatable = false)
    private LocalDateTime createdAt = LocalDateTime.now();

    @Column(name = "updated_at", nullable = false)
    private LocalDateTime updatedAt = LocalDateTime.now();

    public InsuranceClaim() {}

    @PreUpdate
    void onUpdate() {
        this.updatedAt = LocalDateTime.now();
    }

    /** 미수금 = 청구액 - 실수령액 (NULL은 0 처리) */
    public int getOutstanding() {
        int claim = claimAmount == null ? 0 : claimAmount;
        int recv  = receivedAmount == null ? 0 : receivedAmount;
        return claim - recv;
    }

    // ----- getters / setters -----

    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }

    public CustomerIntake getIntake() { return intake; }
    public void setIntake(CustomerIntake intake) { this.intake = intake; }

    public ClaimSide getClaimSide() { return claimSide; }
    public void setClaimSide(ClaimSide claimSide) { this.claimSide = claimSide; }

    public String getInsuranceCompany() { return insuranceCompany; }
    public void setInsuranceCompany(String insuranceCompany) { this.insuranceCompany = insuranceCompany; }

    public LocalDate getClaimDate() { return claimDate; }
    public void setClaimDate(LocalDate claimDate) { this.claimDate = claimDate; }

    public Integer getClaimAmount() { return claimAmount; }
    public void setClaimAmount(Integer claimAmount) { this.claimAmount = claimAmount; }

    public Integer getReceivedAmount() { return receivedAmount; }
    public void setReceivedAmount(Integer receivedAmount) { this.receivedAmount = receivedAmount; }

    public LocalDate getReceivedDate() { return receivedDate; }
    public void setReceivedDate(LocalDate receivedDate) { this.receivedDate = receivedDate; }

    public String getMemo() { return memo; }
    public void setMemo(String memo) { this.memo = memo; }

    public LocalDateTime getCreatedAt() { return createdAt; }
    public LocalDateTime getUpdatedAt() { return updatedAt; }
}
