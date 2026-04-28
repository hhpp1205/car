package com.jeonbuk.repair.service;

import com.jeonbuk.repair.model.ClaimSide;
import com.jeonbuk.repair.model.CustomerIntake;
import com.jeonbuk.repair.model.InsuranceClaim;
import com.jeonbuk.repair.model.ProgressStatus;
import com.jeonbuk.repair.model.RepairType;
import com.jeonbuk.repair.support.TestFixtures;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.time.LocalDate;

import static org.junit.jupiter.api.Assertions.*;

/**
 * 진행상태 산출 + 검증 로직만 테스트 — DB 불필요.
 * Repository를 만지는 시나리오(create + 자동 입고번호) 는 IntakeNumberGeneratorTest 가 통합 테스트.
 */
class CustomerIntakeServiceTest {

    private final CustomerIntakeService service = new CustomerIntakeService(null);

    @Test
    @DisplayName("computeStatus — 청구도 출고도 없으면 수리중")
    void status_repairing() {
        CustomerIntake i = TestFixtures.intake();
        assertEquals(ProgressStatus.REPAIRING, service.computeStatus(i));
    }

    @Test
    @DisplayName("computeStatus — 출고일만 있으면 출고완료")
    void status_released() {
        CustomerIntake i = TestFixtures.intake();
        i.setReleaseDate(LocalDate.of(2026, 4, 26));
        assertEquals(ProgressStatus.RELEASED, service.computeStatus(i));
    }

    @Test
    @DisplayName("computeStatus — 청구액 > 0 이면 청구완료")
    void status_claimed() {
        CustomerIntake i = TestFixtures.intake();
        i.setReleaseDate(LocalDate.of(2026, 4, 26));
        i.getClaims().add(TestFixtures.claim(i, ClaimSide.OWN, 1_000_000, 0));
        assertEquals(ProgressStatus.CLAIMED, service.computeStatus(i));
    }

    @Test
    @DisplayName("computeStatus — 실수령 합계 ≥ 청구액 합계면 수령완료")
    void status_settled() {
        CustomerIntake i = TestFixtures.intake();
        i.getClaims().add(TestFixtures.claim(i, ClaimSide.OWN, 600_000, 600_000));
        i.getClaims().add(TestFixtures.claim(i, ClaimSide.OPPONENT, 400_000, 400_000));
        assertEquals(ProgressStatus.SETTLED, service.computeStatus(i));
    }

    @Test
    @DisplayName("computeStatus — 부분 수령은 청구완료")
    void status_partial_received_is_claimed() {
        CustomerIntake i = TestFixtures.intake();
        i.getClaims().add(TestFixtures.claim(i, ClaimSide.OWN, 1_000_000, 500_000));
        assertEquals(ProgressStatus.CLAIMED, service.computeStatus(i));
    }

    @Test
    @DisplayName("create — 입고일 null 이면 예외")
    void create_requires_intake_date() {
        CustomerIntake i = new CustomerIntake();
        i.setVehicleName("쏘나타");
        i.setVehicleNumber("12가3456");
        IllegalArgumentException ex = assertThrows(IllegalArgumentException.class, () -> service.create(i));
        assertTrue(ex.getMessage().contains("입고일"));
    }

    @Test
    @DisplayName("create — 차량명이 비어있으면 예외")
    void create_requires_vehicle_name() {
        CustomerIntake i = new CustomerIntake();
        i.setIntakeDate(LocalDate.of(2026, 4, 24));
        i.setVehicleNumber("12가3456");
        IllegalArgumentException ex = assertThrows(IllegalArgumentException.class, () -> service.create(i));
        assertTrue(ex.getMessage().contains("차량명"));
    }

    @Test
    @DisplayName("create — 차량번호에 한글 없으면 예외")
    void create_requires_hangul_vehicle_number() {
        CustomerIntake i = new CustomerIntake();
        i.setIntakeDate(LocalDate.of(2026, 4, 24));
        i.setVehicleName("쏘나타");
        i.setVehicleNumber("1234567");
        IllegalArgumentException ex = assertThrows(IllegalArgumentException.class, () -> service.create(i));
        assertTrue(ex.getMessage().contains("한글"));
    }

    @Test
    @DisplayName("update — id 가 없으면 예외")
    void update_requires_id() {
        CustomerIntake i = TestFixtures.intake();
        IllegalArgumentException ex = assertThrows(IllegalArgumentException.class, () -> service.update(i));
        assertTrue(ex.getMessage().contains("id"));
    }
}
