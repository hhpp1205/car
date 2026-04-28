package com.jeonbuk.repair.service;

import com.jeonbuk.repair.model.RentalHistory;
import com.jeonbuk.repair.model.RentalVehicle;
import com.jeonbuk.repair.support.TestFixtures;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.time.LocalDate;

import static org.junit.jupiter.api.Assertions.*;

/**
 * RentalService 의 save() 검증 로직만 테스트 — DB 미접근 케이스.
 * 차량 충돌 검증(isVehicleInUse 호출) 은 RentalHistoryRepositoryTest 에서 통합 테스트.
 */
class RentalServiceTest {

    private final RentalService service = new RentalService(null, null);

    @Test
    @DisplayName("save — intake null 이면 예외")
    void save_requires_intake() {
        RentalHistory r = new RentalHistory();
        r.setRentalVehicle(new RentalVehicle("SM3", "12가3456", null, null, null, null));
        r.setRentalStartDate(LocalDate.now());
        IllegalArgumentException ex = assertThrows(IllegalArgumentException.class, () -> service.save(r));
        assertTrue(ex.getMessage().contains("입고"));
    }

    @Test
    @DisplayName("save — 대차차량 null 이면 예외")
    void save_requires_vehicle() {
        RentalHistory r = new RentalHistory();
        r.setIntake(TestFixtures.intake());
        r.setRentalStartDate(LocalDate.now());
        IllegalArgumentException ex = assertThrows(IllegalArgumentException.class, () -> service.save(r));
        assertTrue(ex.getMessage().contains("대차차량"));
    }

    @Test
    @DisplayName("save — 시작일 null 이면 예외")
    void save_requires_start_date() {
        RentalHistory r = new RentalHistory();
        r.setIntake(TestFixtures.intake());
        r.setRentalVehicle(new RentalVehicle("SM3", "12가3456", null, null, null, null));
        IllegalArgumentException ex = assertThrows(IllegalArgumentException.class, () -> service.save(r));
        assertTrue(ex.getMessage().contains("시작일"));
    }

    @Test
    @DisplayName("save — 종료일이 시작일보다 빠르면 예외")
    void save_end_before_start() {
        RentalHistory r = new RentalHistory();
        r.setIntake(TestFixtures.intake());
        r.setRentalVehicle(new RentalVehicle("SM3", "12가3456", null, null, null, null));
        r.setRentalStartDate(LocalDate.of(2026, 4, 24));
        r.setRentalEndDate(LocalDate.of(2026, 4, 23));
        IllegalArgumentException ex = assertThrows(IllegalArgumentException.class, () -> service.save(r));
        assertTrue(ex.getMessage().contains("종료일"));
    }
}
