package com.jeonbuk.repair.service;

import com.jeonbuk.repair.model.CustomerIntake;
import com.jeonbuk.repair.model.ProgressStatus;
import com.jeonbuk.repair.model.RepairType;
import com.jeonbuk.repair.repository.CustomerIntakeRepository;
import com.jeonbuk.repair.util.Validators;

import java.util.List;
import java.util.Optional;

public class CustomerIntakeService {

    private final CustomerIntakeRepository repo;
    private final IntakeNumberGenerator numberGenerator;

    public CustomerIntakeService(CustomerIntakeRepository repo) {
        this.repo = repo;
        this.numberGenerator = new IntakeNumberGenerator(repo);
    }

    /**
     * 신규 입고. intakeNo 가 비어있으면 자동 생성.
     */
    public CustomerIntake create(CustomerIntake intake) {
        validate(intake);
        if (intake.getIntakeNo() == null || intake.getIntakeNo().isBlank()) {
            intake.setIntakeNo(numberGenerator.next(intake.getIntakeDate()));
        }
        return repo.save(intake);
    }

    public CustomerIntake update(CustomerIntake intake) {
        if (intake.getId() == null) {
            throw new IllegalArgumentException("수정 대상 id가 없습니다.");
        }
        validate(intake);
        return repo.save(intake);
    }

    public void delete(Long id) {
        repo.delete(id);
    }

    public Optional<CustomerIntake> findById(Long id) {
        return repo.findById(id);
    }

    public List<CustomerIntake> findAll() {
        return repo.findAll();
    }

    public List<CustomerIntake> search(String keyword) {
        return repo.search(keyword);
    }

    /**
     * 진행상태 자동 산출.
     * - 청구액 > 0 && 실수령액 >= 청구액 → 수령완료
     * - 청구액 > 0                       → 청구완료
     * - 출고일 있음                      → 출고완료
     * - 그 외                            → 수리중
     */
    public ProgressStatus computeStatus(CustomerIntake intake) {
        long totalClaim = intake.getClaims().stream()
                .mapToLong(c -> c.getClaimAmount() == null ? 0L : c.getClaimAmount())
                .sum();
        long totalReceived = intake.getClaims().stream()
                .mapToLong(c -> c.getReceivedAmount() == null ? 0L : c.getReceivedAmount())
                .sum();

        if (totalClaim > 0 && totalReceived >= totalClaim) return ProgressStatus.SETTLED;
        if (totalClaim > 0)                                return ProgressStatus.CLAIMED;
        if (intake.getReleaseDate() != null)               return ProgressStatus.RELEASED;
        return ProgressStatus.REPAIRING;
    }

    private void validate(CustomerIntake intake) {
        if (intake.getIntakeDate() == null) {
            throw new IllegalArgumentException("입고일을 입력해 주세요.");
        }
        if (Validators.isBlank(intake.getVehicleName())) {
            throw new IllegalArgumentException("차량명을 입력해 주세요.");
        }
        if (!Validators.isValidVehicleNumber(intake.getVehicleNumber())) {
            throw new IllegalArgumentException("차량번호는 한글이 포함되어야 합니다 (예: 12가3456).");
        }
        if (intake.getRepairType() == null) {
            intake.setRepairType(RepairType.GENERAL);
        }
    }
}
