package com.jeonbuk.repair.repository;

import com.jeonbuk.repair.model.CustomerIntake;
import com.jeonbuk.repair.model.RentalHistory;
import com.jeonbuk.repair.model.RentalVehicle;
import com.jeonbuk.repair.support.DatabaseExtension;
import com.jeonbuk.repair.support.TestFixtures;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;

import java.time.LocalDate;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

@ExtendWith(DatabaseExtension.class)
class RentalHistoryRepositoryTest {

    private final CustomerIntakeRepository intakeRepo = new CustomerIntakeRepository();
    private final RentalHistoryRepository  rentalRepo  = new RentalHistoryRepository();
    private final RentalVehicleRepository  vehicleRepo = new RentalVehicleRepository();

    @Test
    @DisplayName("findActive — 종료일 null 인 행만")
    void find_active_only() {
        CustomerIntake intake = saveIntake();
        RentalVehicle v1 = vehicleRepo.findAll().get(0);
        RentalVehicle v2 = vehicleRepo.findAll().get(1);

        rentalRepo.save(rental(intake, v1, LocalDate.of(2026, 4, 24), null));         // 대차중
        rentalRepo.save(rental(intake, v2, LocalDate.of(2026, 4, 20), LocalDate.of(2026, 4, 22))); // 종료

        List<RentalHistory> active = rentalRepo.findActive();
        assertEquals(1, active.size());
        assertNull(active.get(0).getRentalEndDate());
    }

    @Test
    @DisplayName("isVehicleInUse — 종료일 null 인 행이 있으면 true")
    void vehicle_in_use_when_open() {
        CustomerIntake intake = saveIntake();
        RentalVehicle v = vehicleRepo.findAll().get(0);

        rentalRepo.save(rental(intake, v, LocalDate.of(2026, 4, 24), null));

        assertTrue(rentalRepo.isVehicleInUse(v.getId()));
    }

    @Test
    @DisplayName("isVehicleInUse — 모두 반납됐으면 false")
    void vehicle_in_use_false_when_all_returned() {
        CustomerIntake intake = saveIntake();
        RentalVehicle v = vehicleRepo.findAll().get(0);

        rentalRepo.save(rental(intake, v, LocalDate.of(2026, 4, 20), LocalDate.of(2026, 4, 22)));

        assertFalse(rentalRepo.isVehicleInUse(v.getId()));
    }

    @Test
    @DisplayName("findByIntakeId — 특정 입고의 대차이력")
    void find_by_intake() {
        CustomerIntake a = saveIntake("260424-01", "11가1111");
        CustomerIntake b = saveIntake("260424-02", "22나2222");
        RentalVehicle v1 = vehicleRepo.findAll().get(0);
        RentalVehicle v2 = vehicleRepo.findAll().get(1);

        rentalRepo.save(rental(a, v1, LocalDate.of(2026, 4, 24), null));
        rentalRepo.save(rental(b, v2, LocalDate.of(2026, 4, 24), null));

        List<RentalHistory> aList = rentalRepo.findByIntakeId(a.getId());
        assertEquals(1, aList.size());
        assertEquals(a.getId(), aList.get(0).getIntake().getId());
    }

    private CustomerIntake saveIntake() {
        return saveIntake("260424-01", "12가3456");
    }

    private CustomerIntake saveIntake(String intakeNo, String vehicleNumber) {
        CustomerIntake i = TestFixtures.intake(LocalDate.of(2026, 4, 24), vehicleNumber);
        i.setIntakeNo(intakeNo);
        return intakeRepo.save(i);
    }

    private RentalHistory rental(CustomerIntake intake, RentalVehicle v, LocalDate start, LocalDate end) {
        RentalHistory r = new RentalHistory();
        r.setIntake(intake);
        r.setRentalVehicle(v);
        r.setRentalStartDate(start);
        r.setRentalEndDate(end);
        return r;
    }
}
