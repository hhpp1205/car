package com.jeonbuk.repair.service;

import com.jeonbuk.repair.model.ClaimSide;
import com.jeonbuk.repair.model.CustomerIntake;
import com.jeonbuk.repair.model.InsuranceClaim;
import com.jeonbuk.repair.model.RentalHistory;
import com.jeonbuk.repair.model.RentalVehicle;
import com.jeonbuk.repair.repository.CustomerIntakeRepository;
import com.jeonbuk.repair.repository.InsuranceClaimRepository;
import com.jeonbuk.repair.repository.RentalHistoryRepository;
import com.jeonbuk.repair.repository.RentalVehicleRepository;
import com.jeonbuk.repair.support.DatabaseExtension;
import com.jeonbuk.repair.support.TestFixtures;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;

import java.time.LocalDate;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;

@ExtendWith(DatabaseExtension.class)
class IntakeWorkflowServiceTest {

    private final CustomerIntakeRepository intakeRepo = new CustomerIntakeRepository();
    private final InsuranceClaimRepository claimRepo  = new InsuranceClaimRepository();
    private final RentalHistoryRepository  rentalRepo = new RentalHistoryRepository();
    private final RentalVehicleRepository  vehicleRepo = new RentalVehicleRepository();
    private final IntakeWorkflowService    service     = new IntakeWorkflowService(intakeRepo);

    @Test
    @DisplayName("신규 입고 + 대차 + 자차/상대 청구를 한 번에 저장")
    void save_all_at_once() {
        RentalVehicle v = vehicleRepo.findAll().get(0);

        IntakeWorkflowService.Form form = new IntakeWorkflowService.Form();
        form.intake = TestFixtures.intake();           // intakeNo 자동생성
        form.rental = rental(v, LocalDate.of(2026, 4, 24), null);
        form.ownClaim = claimDraft(500_000, 0);
        form.opponentClaim = claimDraft(700_000, 200_000);

        CustomerIntake saved = service.save(form);

        // 입고 — intakeNo 자동 생성
        assertNotNull(saved.getId());
        assertEquals("260424-01", saved.getIntakeNo());

        // 대차
        var rentals = rentalRepo.findByIntakeId(saved.getId());
        assertEquals(1, rentals.size());
        assertEquals(v.getId(), rentals.get(0).getRentalVehicle().getId());

        // 청구 양쪽
        InsuranceClaim own = claimRepo.findByIntakeAndSide(saved.getId(), ClaimSide.OWN).orElseThrow();
        InsuranceClaim opp = claimRepo.findByIntakeAndSide(saved.getId(), ClaimSide.OPPONENT).orElseThrow();
        assertEquals(500_000, own.getClaimAmount());
        assertEquals(700_000, opp.getClaimAmount());
        assertEquals(200_000, opp.getReceivedAmount());
    }

    @Test
    @DisplayName("입고만 저장하고 대차/청구는 비워두면 입고만 들어감")
    void save_intake_only() {
        IntakeWorkflowService.Form form = new IntakeWorkflowService.Form();
        form.intake = TestFixtures.intake();

        CustomerIntake saved = service.save(form);

        assertEquals(0, rentalRepo.findByIntakeId(saved.getId()).size());
        assertTrue(claimRepo.findByIntakeAndSide(saved.getId(), ClaimSide.OWN).isEmpty());
        assertTrue(claimRepo.findByIntakeAndSide(saved.getId(), ClaimSide.OPPONENT).isEmpty());
    }

    @Test
    @DisplayName("기존 입고에 청구를 추가/수정")
    void update_existing_intake_with_claim() {
        // 1차 — 입고만
        IntakeWorkflowService.Form first = new IntakeWorkflowService.Form();
        first.intake = TestFixtures.intake();
        CustomerIntake saved = service.save(first);
        Long intakeId = saved.getId();

        // 2차 — 같은 입고에 자차 청구 추가
        IntakeWorkflowService.Form second = new IntakeWorkflowService.Form();
        second.intake = intakeRepo.findById(intakeId).orElseThrow();
        second.ownClaim = claimDraft(1_000_000, 0);
        service.save(second);

        InsuranceClaim own = claimRepo.findByIntakeAndSide(intakeId, ClaimSide.OWN).orElseThrow();
        assertEquals(1_000_000, own.getClaimAmount());

        // 3차 — 청구 금액 수정
        IntakeWorkflowService.Form third = new IntakeWorkflowService.Form();
        third.intake = intakeRepo.findById(intakeId).orElseThrow();
        InsuranceClaim existing = claimRepo.findByIntakeAndSide(intakeId, ClaimSide.OWN).orElseThrow();
        existing.setClaimAmount(1_500_000);
        existing.setReceivedAmount(500_000);
        third.ownClaim = existing;
        service.save(third);

        InsuranceClaim updated = claimRepo.findByIntakeAndSide(intakeId, ClaimSide.OWN).orElseThrow();
        assertEquals(1_500_000, updated.getClaimAmount());
        assertEquals(500_000, updated.getReceivedAmount());

        // 청구 1건만 (중복 생성되지 않음)
        assertEquals(1, claimRepo.findByIntakeId(intakeId).size());
    }

    @Test
    @DisplayName("기존 청구를 idToDelete 로 비우면 삭제됨")
    void delete_existing_claim_via_idToDelete() {
        // 입고 + 자차 청구
        IntakeWorkflowService.Form first = new IntakeWorkflowService.Form();
        first.intake = TestFixtures.intake();
        first.ownClaim = claimDraft(500_000, 0);
        CustomerIntake saved = service.save(first);

        InsuranceClaim ownBefore = claimRepo.findByIntakeAndSide(saved.getId(), ClaimSide.OWN).orElseThrow();

        // 자차 청구 비움
        IntakeWorkflowService.Form second = new IntakeWorkflowService.Form();
        second.intake = intakeRepo.findById(saved.getId()).orElseThrow();
        second.ownClaim = null;
        second.ownClaimIdToDelete = ownBefore.getId();
        service.save(second);

        assertTrue(claimRepo.findByIntakeAndSide(saved.getId(), ClaimSide.OWN).isEmpty());
    }

    @Test
    @DisplayName("기존 대차이력을 idToDelete 로 비우면 삭제됨")
    void delete_existing_rental_via_idToDelete() {
        RentalVehicle v = vehicleRepo.findAll().get(0);

        IntakeWorkflowService.Form first = new IntakeWorkflowService.Form();
        first.intake = TestFixtures.intake();
        first.rental = rental(v, LocalDate.of(2026, 4, 24), null);
        CustomerIntake saved = service.save(first);

        Long rentalId = rentalRepo.findByIntakeId(saved.getId()).get(0).getId();

        IntakeWorkflowService.Form second = new IntakeWorkflowService.Form();
        second.intake = intakeRepo.findById(saved.getId()).orElseThrow();
        second.rental = null;
        second.rentalIdToDelete = rentalId;
        service.save(second);

        assertEquals(0, rentalRepo.findByIntakeId(saved.getId()).size());
    }

    @Test
    @DisplayName("청구일보다 입금일이 빠르면 거부")
    void received_before_claim_rejected() {
        IntakeWorkflowService.Form form = new IntakeWorkflowService.Form();
        form.intake = TestFixtures.intake();
        InsuranceClaim c = claimDraft(500_000, 100_000);
        c.setClaimDate(LocalDate.of(2026, 4, 24));
        c.setReceivedDate(LocalDate.of(2026, 4, 20));
        form.ownClaim = c;

        IllegalArgumentException ex = assertThrows(IllegalArgumentException.class, () -> service.save(form));
        assertTrue(ex.getMessage().contains("입금일"));
    }

    @Test
    @DisplayName("종료일이 시작일보다 빠른 대차는 거부")
    void rental_end_before_start_rejected() {
        RentalVehicle v = vehicleRepo.findAll().get(0);

        IntakeWorkflowService.Form form = new IntakeWorkflowService.Form();
        form.intake = TestFixtures.intake();
        form.rental = rental(v, LocalDate.of(2026, 4, 24), LocalDate.of(2026, 4, 20));

        IllegalArgumentException ex = assertThrows(IllegalArgumentException.class, () -> service.save(form));
        assertTrue(ex.getMessage().contains("종료일"));
    }

    @Test
    @DisplayName("차량번호에 한글이 없으면 입고 검증 실패 — 트랜잭션 롤백되어 청구도 저장 안 됨")
    void invalid_vehicle_number_rolls_back_everything() {
        IntakeWorkflowService.Form form = new IntakeWorkflowService.Form();
        form.intake = TestFixtures.intake();
        form.intake.setVehicleNumber("12345"); // 한글 없음
        form.ownClaim = claimDraft(500_000, 0);

        assertThrows(IllegalArgumentException.class, () -> service.save(form));

        // 입고/청구 모두 저장 안 됨 (검증 단계에서 fail)
        assertEquals(0, intakeRepo.findAll().size());
        assertEquals(0, claimRepo.findAll().size());
    }

    @Test
    @DisplayName("intake 저장 후 같은 화면에서 청구만 추가하는 패턴 (id 가진 form.intake 재사용)")
    void reuse_persisted_intake() {
        IntakeWorkflowService.Form first = new IntakeWorkflowService.Form();
        first.intake = TestFixtures.intake();
        CustomerIntake saved = service.save(first);

        IntakeWorkflowService.Form second = new IntakeWorkflowService.Form();
        second.intake = saved; // 같은 객체 재사용 (id != null)
        second.opponentClaim = claimDraft(300_000, 0);
        CustomerIntake savedAgain = service.save(second);

        assertEquals(saved.getId(), savedAgain.getId());
        assertEquals(saved.getIntakeNo(), savedAgain.getIntakeNo()); // intakeNo 변경 없음
        Optional<InsuranceClaim> opp = claimRepo.findByIntakeAndSide(saved.getId(), ClaimSide.OPPONENT);
        assertTrue(opp.isPresent());
    }

    // -------- helpers --------

    private RentalHistory rental(RentalVehicle v, LocalDate start, LocalDate end) {
        RentalHistory r = new RentalHistory();
        r.setRentalVehicle(v);
        r.setRentalStartDate(start);
        r.setRentalEndDate(end);
        return r;
    }

    private InsuranceClaim claimDraft(Integer claim, Integer received) {
        InsuranceClaim c = new InsuranceClaim();
        c.setInsuranceCompany("삼성화재");
        c.setClaimDate(LocalDate.of(2026, 4, 24));
        c.setClaimAmount(claim);
        c.setReceivedAmount(received);
        return c;
    }
}
