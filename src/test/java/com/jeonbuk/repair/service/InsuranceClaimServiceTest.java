package com.jeonbuk.repair.service;

import com.jeonbuk.repair.model.ClaimSide;
import com.jeonbuk.repair.model.InsuranceClaim;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.time.LocalDate;

import static org.junit.jupiter.api.Assertions.*;

/**
 * isOverdue() 와 save() 의 검증 로직만 테스트 — DB 불필요.
 * 실제 저장은 InsuranceClaimRepositoryTest 가 책임.
 */
class InsuranceClaimServiceTest {

    private final InsuranceClaimService service = new InsuranceClaimService(null);

    @Test
    @DisplayName("isOverdue — 청구일 없으면 false")
    void overdue_no_claim_date() {
        InsuranceClaim c = new InsuranceClaim();
        assertFalse(service.isOverdue(c));
    }

    @Test
    @DisplayName("isOverdue — 입금일이 있으면 false (이미 받음)")
    void overdue_already_received() {
        InsuranceClaim c = new InsuranceClaim();
        c.setClaimDate(LocalDate.now().minusDays(60));
        c.setReceivedDate(LocalDate.now().minusDays(1));
        assertFalse(service.isOverdue(c));
    }

    @Test
    @DisplayName("isOverdue — 청구 후 30일 이내는 false")
    void overdue_within_30_days() {
        InsuranceClaim c = new InsuranceClaim();
        c.setClaimDate(LocalDate.now().minusDays(15));
        assertFalse(service.isOverdue(c));
    }

    @Test
    @DisplayName("isOverdue — 청구 후 30일 정확히는 아직 false (.isBefore 기준)")
    void overdue_exactly_30_days() {
        InsuranceClaim c = new InsuranceClaim();
        c.setClaimDate(LocalDate.now().minusDays(30));
        assertFalse(service.isOverdue(c));
    }

    @Test
    @DisplayName("isOverdue — 청구 후 31일 이상이면 true")
    void overdue_over_30_days() {
        InsuranceClaim c = new InsuranceClaim();
        c.setClaimDate(LocalDate.now().minusDays(31));
        assertTrue(service.isOverdue(c));
    }

    @Test
    @DisplayName("save — intake 가 없으면 IllegalArgumentException")
    void save_requires_intake() {
        InsuranceClaim c = new InsuranceClaim();
        c.setClaimSide(ClaimSide.OWN);
        IllegalArgumentException ex = assertThrows(IllegalArgumentException.class, () -> service.save(c));
        assertTrue(ex.getMessage().contains("입고"));
    }
}
