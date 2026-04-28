package com.jeonbuk.repair.util;

import com.jeonbuk.repair.model.CustomerIntake;
import com.jeonbuk.repair.model.InsuranceClaim;
import com.jeonbuk.repair.model.RepairType;
import com.jeonbuk.repair.service.ServiceRegistry;
import com.jeonbuk.repair.support.DatabaseExtension;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;

import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

@ExtendWith(DatabaseExtension.class)
class TestDataSeederTest {

    @Test
    @DisplayName("run — 100건 입고가 적재되고 다양성이 확보된다")
    void seeds_100_intakes_with_variety() {
        int created = TestDataSeeder.run();
        assertEquals(100, created, "100건이 모두 저장되어야 한다");

        List<CustomerIntake> all = ServiceRegistry.get().intakeRepo().findAll();
        assertEquals(100, all.size());

        // 차량명·차량번호·전화번호가 채워져 있다
        for (CustomerIntake i : all) {
            assertNotNull(i.getIntakeNo());
            assertNotNull(i.getIntakeDate());
            assertFalse(i.getVehicleName() == null || i.getVehicleName().isBlank());
            assertTrue(Validators.isValidVehicleNumber(i.getVehicleNumber()),
                    "차량번호 형식 오류: " + i.getVehicleNumber());
            assertNotNull(i.getRepairType());
        }

        long insurance = all.stream().filter(i -> i.getRepairType() == RepairType.INSURANCE).count();
        long general   = all.stream().filter(i -> i.getRepairType() == RepairType.GENERAL).count();
        // 60/40 분포 — 양쪽 다 최소 한 자리수 이상이면 충분
        assertTrue(insurance > 30, "보험수리 분포 부족: " + insurance);
        assertTrue(general   > 15, "일반수리 분포 부족: " + general);

        long withRelease = all.stream().filter(i -> i.getReleaseDate() != null).count();
        assertTrue(withRelease > 50, "출고일 있는 건수 부족: " + withRelease);

        long withTow = all.stream().filter(i -> i.getTowDriver() != null).count();
        assertTrue(withTow > 10, "견인 데이터 분포 부족: " + withTow);

        // 청구 데이터도 적재됐는지
        List<InsuranceClaim> claims = ServiceRegistry.get().claimRepo().findAll();
        assertTrue(claims.size() >= 20, "청구 데이터 부족: " + claims.size());

        // 입고번호 중복 없음
        long distinctNos = all.stream().map(CustomerIntake::getIntakeNo).distinct().count();
        assertEquals(100, distinctNos, "입고번호 중복 발생");
    }
}
