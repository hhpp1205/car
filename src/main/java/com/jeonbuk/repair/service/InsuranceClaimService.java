package com.jeonbuk.repair.service;

import com.jeonbuk.repair.model.InsuranceClaim;
import com.jeonbuk.repair.repository.InsuranceClaimRepository;

import java.time.LocalDate;
import java.util.List;

public class InsuranceClaimService {

    /** 청구 후 30일 경과 미수령 알림 임계값 */
    public static final int OVERDUE_DAYS = 30;

    private final InsuranceClaimRepository repo;

    public InsuranceClaimService(InsuranceClaimRepository repo) {
        this.repo = repo;
    }

    public InsuranceClaim save(InsuranceClaim claim) {
        if (claim.getIntake() == null) {
            throw new IllegalArgumentException("연결된 입고가 없습니다.");
        }
        if (claim.getClaimSide() == null) {
            throw new IllegalArgumentException("자차/상대 구분을 선택해 주세요.");
        }
        return repo.save(claim);
    }

    public void delete(Long id) {
        repo.delete(id);
    }

    public List<InsuranceClaim> findByIntakeId(Long intakeId) {
        return repo.findByIntakeId(intakeId);
    }

    public List<InsuranceClaim> findAll() {
        return repo.findAll();
    }

    /**
     * 30일 경과 미수령 여부.
     * 청구일이 있고, 입금일이 비어있고, claimDate + 30일 < 오늘.
     */
    public boolean isOverdue(InsuranceClaim claim) {
        if (claim.getClaimDate() == null) return false;
        if (claim.getReceivedDate() != null) return false;
        return claim.getClaimDate().plusDays(OVERDUE_DAYS).isBefore(LocalDate.now());
    }
}
