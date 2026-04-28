package com.jeonbuk.repair.service;

import com.jeonbuk.repair.model.CustomerIntake;
import com.jeonbuk.repair.repository.CustomerIntakeRepository;
import com.jeonbuk.repair.support.DatabaseExtension;
import com.jeonbuk.repair.support.TestFixtures;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;

import java.time.LocalDate;

import static org.junit.jupiter.api.Assertions.*;

@ExtendWith(DatabaseExtension.class)
class CustomerIntakeServiceIntegrationTest {

    private final CustomerIntakeRepository repo = new CustomerIntakeRepository();
    private final CustomerIntakeService service = new CustomerIntakeService(repo);

    @Test
    @DisplayName("create — 입고번호 미지정 시 자동 부여")
    void create_assigns_intake_no() {
        CustomerIntake i = TestFixtures.intake(LocalDate.of(2026, 4, 24), "12가3456");
        CustomerIntake saved = service.create(i);

        assertNotNull(saved.getId());
        assertEquals("260424-01", saved.getIntakeNo());
    }

    @Test
    @DisplayName("create — 같은 날짜 연속 생성은 -01, -02")
    void create_increments_intake_no() {
        CustomerIntake a = service.create(TestFixtures.intake(LocalDate.of(2026, 4, 24), "11가1111"));
        CustomerIntake b = service.create(TestFixtures.intake(LocalDate.of(2026, 4, 24), "22나2222"));

        assertEquals("260424-01", a.getIntakeNo());
        assertEquals("260424-02", b.getIntakeNo());
    }

    @Test
    @DisplayName("update — id 가 있으면 같은 입고번호 유지하며 갱신")
    void update_keeps_intake_no() {
        CustomerIntake saved = service.create(TestFixtures.intake(LocalDate.of(2026, 4, 24), "12가3456"));
        saved.setVehicleName("그랜저");
        CustomerIntake updated = service.update(saved);

        assertEquals(saved.getId(), updated.getId());
        assertEquals("260424-01", updated.getIntakeNo());
        assertEquals("그랜저", updated.getVehicleName());
    }

    @Test
    @DisplayName("delete — cascade 로 보험청구도 함께 삭제")
    void delete_cascades() {
        CustomerIntake saved = service.create(TestFixtures.intake(LocalDate.of(2026, 4, 24), "12가3456"));
        Long id = saved.getId();
        service.delete(id);

        assertTrue(service.findById(id).isEmpty());
    }
}
