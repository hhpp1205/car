package com.jeonbuk.repair.service;

import com.jeonbuk.repair.model.CustomerIntake;
import com.jeonbuk.repair.model.ProgressStatus;
import com.jeonbuk.repair.model.RepairType;
import com.jeonbuk.repair.repository.CustomerIntakeRepository;
import com.jeonbuk.repair.util.Validators;

import java.time.LocalDateTime;
import java.time.temporal.ChronoUnit;
import java.util.ArrayList;
import java.util.Collection;
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

    public List<CustomerIntake> findAll(IntakeFilter filter) {
        return repo.findAll(filter);
    }

    public List<CustomerIntake> search(String keyword) {
        return repo.search(keyword);
    }

    public List<CustomerIntake> search(String keyword, IntakeFilter filter) {
        return repo.search(keyword, filter);
    }

    /** 종결 처리 — closedAt 을 현재 시각으로 채운다. 이미 종결된 건이면 no-op. */
    public CustomerIntake close(Long id) {
        CustomerIntake intake = repo.findById(id)
                .orElseThrow(() -> new IllegalArgumentException("입고 정보를 찾을 수 없습니다 (id=" + id + ")"));
        if (intake.isClosed()) return intake;
        // SQLite TEXT 저장은 ms 정밀도 — in-memory ns 와 차이가 나면 재조회 시 값이 달라 보임.
        intake.setClosedAt(LocalDateTime.now().truncatedTo(ChronoUnit.MILLIS));
        return repo.save(intake);
    }

    /** 종결 취소 — closedAt 을 NULL 로 되돌린다. 종결되지 않은 건이면 no-op. */
    public CustomerIntake reopen(Long id) {
        CustomerIntake intake = repo.findById(id)
                .orElseThrow(() -> new IllegalArgumentException("입고 정보를 찾을 수 없습니다 (id=" + id + ")"));
        if (!intake.isClosed()) return intake;
        intake.setClosedAt(null);
        return repo.save(intake);
    }

    /** 일괄 처리 결과 — 처리됨 / 건너뜀(이미 그 상태) / 실패 카운트와 실패 사유. */
    public record BulkResult(int processed, int skipped, List<String> failures) {
        public int total() { return processed + skipped + failures.size(); }
    }

    /** 일괄 종결 — 한 건 실패해도 나머지 진행. 이미 종결된 건은 skipped. */
    public BulkResult closeAll(Collection<Long> ids) {
        return bulk(ids, this::close, /*targetIsClosed*/ true);
    }

    /** 일괄 종결취소 — 한 건 실패해도 나머지 진행. 이미 진행 중인 건은 skipped. */
    public BulkResult reopenAll(Collection<Long> ids) {
        return bulk(ids, this::reopen, /*targetIsClosed*/ false);
    }

    private BulkResult bulk(Collection<Long> ids,
                            java.util.function.LongFunction<CustomerIntake> op,
                            boolean targetIsClosed) {
        int processed = 0;
        int skipped = 0;
        List<String> failures = new ArrayList<>();
        if (ids == null) return new BulkResult(0, 0, failures);
        for (Long id : ids) {
            if (id == null) continue;
            try {
                CustomerIntake before = repo.findById(id).orElse(null);
                if (before == null) {
                    failures.add("id=" + id + ": 입고 정보를 찾을 수 없습니다.");
                    continue;
                }
                if (before.isClosed() == targetIsClosed) {
                    skipped++;
                    continue;
                }
                op.apply(id);
                processed++;
            } catch (RuntimeException e) {
                failures.add("id=" + id + ": " + e.getMessage());
            }
        }
        return new BulkResult(processed, skipped, failures);
    }

    /**
     * 진행상태 자동 산출.
     * - closedAt 있음                    → 종결 (사용자 수동 처리, 모든 자동 산출보다 우선)
     * - 청구액 > 0 && 실수령액 >= 청구액 → 수령완료
     * - 청구액 > 0                       → 청구완료
     * - 출고일 있음                      → 출고완료
     * - 그 외                            → 수리중
     */
    public ProgressStatus computeStatus(CustomerIntake intake) {
        if (intake.isClosed()) return ProgressStatus.CLOSED;

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
