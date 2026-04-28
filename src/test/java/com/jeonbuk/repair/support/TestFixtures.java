package com.jeonbuk.repair.support;

import com.jeonbuk.repair.model.AccidentType;
import com.jeonbuk.repair.model.ClaimSide;
import com.jeonbuk.repair.model.CustomerIntake;
import com.jeonbuk.repair.model.InsuranceClaim;
import com.jeonbuk.repair.model.RepairType;

import java.time.LocalDate;
import java.util.EnumSet;

/**
 * 테스트용 도메인 객체 팩토리. 필요한 필드만 세팅한 최소 인스턴스.
 */
public final class TestFixtures {

    private TestFixtures() {}

    public static CustomerIntake intake(LocalDate intakeDate, String vehicleNumber) {
        CustomerIntake i = new CustomerIntake();
        i.setIntakeDate(intakeDate);
        i.setVehicleName("쏘나타");
        i.setVehicleNumber(vehicleNumber);
        i.setRepairType(RepairType.GENERAL);
        i.setAccidentTypes(EnumSet.of(AccidentType.GENERAL));
        return i;
    }

    public static CustomerIntake intake() {
        return intake(LocalDate.of(2026, 4, 24), "12가3456");
    }

    public static InsuranceClaim claim(CustomerIntake intake, ClaimSide side,
                                       Integer claimAmount, Integer receivedAmount) {
        InsuranceClaim c = new InsuranceClaim();
        c.setIntake(intake);
        c.setClaimSide(side);
        c.setInsuranceCompany("삼성화재");
        c.setClaimAmount(claimAmount);
        c.setReceivedAmount(receivedAmount);
        return c;
    }
}
