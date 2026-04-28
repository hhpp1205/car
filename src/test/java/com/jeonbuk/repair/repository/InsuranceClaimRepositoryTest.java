package com.jeonbuk.repair.repository;

import com.jeonbuk.repair.model.ClaimSide;
import com.jeonbuk.repair.model.CustomerIntake;
import com.jeonbuk.repair.model.InsuranceClaim;
import com.jeonbuk.repair.support.DatabaseExtension;
import com.jeonbuk.repair.support.TestFixtures;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;

import java.time.LocalDate;
import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;

@ExtendWith(DatabaseExtension.class)
class InsuranceClaimRepositoryTest {

    private final CustomerIntakeRepository intakeRepo = new CustomerIntakeRepository();
    private final InsuranceClaimRepository claimRepo  = new InsuranceClaimRepository();

    @Test
    @DisplayName("findByIntakeAndSide — 자차/상대 각각 따로 저장 가능")
    void find_by_side() {
        CustomerIntake intake = saveIntake();

        InsuranceClaim own = TestFixtures.claim(intake, ClaimSide.OWN, 500_000, 0);
        own.setClaimDate(LocalDate.of(2026, 4, 24));
        claimRepo.save(own);

        InsuranceClaim opp = TestFixtures.claim(intake, ClaimSide.OPPONENT, 700_000, 700_000);
        opp.setClaimDate(LocalDate.of(2026, 4, 24));
        claimRepo.save(opp);

        Optional<InsuranceClaim> ownFound = claimRepo.findByIntakeAndSide(intake.getId(), ClaimSide.OWN);
        Optional<InsuranceClaim> oppFound = claimRepo.findByIntakeAndSide(intake.getId(), ClaimSide.OPPONENT);

        assertTrue(ownFound.isPresent());
        assertTrue(oppFound.isPresent());
        assertEquals(500_000, ownFound.get().getClaimAmount());
        assertEquals(700_000, oppFound.get().getClaimAmount());
    }

    @Test
    @DisplayName("findByIntakeId — 해당 입고의 모든 청구 반환")
    void find_by_intake() {
        CustomerIntake intake = saveIntake();
        claimRepo.save(TestFixtures.claim(intake, ClaimSide.OWN, 500_000, 0));
        claimRepo.save(TestFixtures.claim(intake, ClaimSide.OPPONENT, 700_000, 0));

        List<InsuranceClaim> claims = claimRepo.findByIntakeId(intake.getId());
        assertEquals(2, claims.size());
    }

    @Test
    @DisplayName("intake 삭제 시 cascade 로 청구도 삭제")
    void cascade_delete_from_intake() {
        CustomerIntake intake = saveIntake();
        InsuranceClaim claim = TestFixtures.claim(intake, ClaimSide.OWN, 500_000, 0);
        claimRepo.save(claim);

        intakeRepo.delete(intake.getId());

        assertTrue(claimRepo.findById(claim.getId()).isEmpty());
    }

    @Test
    @DisplayName("getOutstanding — 미수금 = 청구 - 수령")
    void outstanding_calc() {
        CustomerIntake intake = saveIntake();
        InsuranceClaim claim = TestFixtures.claim(intake, ClaimSide.OWN, 1_000_000, 300_000);
        InsuranceClaim saved = claimRepo.save(claim);

        assertEquals(700_000, saved.getOutstanding());
    }

    private CustomerIntake saveIntake() {
        CustomerIntake i = TestFixtures.intake();
        i.setIntakeNo("260424-01");
        return intakeRepo.save(i);
    }
}
