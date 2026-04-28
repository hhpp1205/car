package com.jeonbuk.repair.service;

import com.jeonbuk.repair.model.ClaimSide;
import com.jeonbuk.repair.model.CustomerIntake;
import com.jeonbuk.repair.model.InsuranceClaim;
import com.jeonbuk.repair.model.RentalHistory;
import com.jeonbuk.repair.model.RepairType;
import com.jeonbuk.repair.repository.CustomerIntakeRepository;
import com.jeonbuk.repair.util.Tx;
import com.jeonbuk.repair.util.Validators;
import org.hibernate.Session;

/**
 * 입고 + 대차 + 자차/상대 보험청구를 한 화면에서 한 번에 저장.
 *
 * <p>모든 변경을 하나의 Hibernate 트랜잭션에 묶어서 부분 실패를 막는다.
 * (입고는 저장됐는데 청구는 실패 — 같은 데이터를 두 번 넣는 사고 방지)
 */
public class IntakeWorkflowService {

    private final IntakeNumberGenerator numberGenerator;

    public IntakeWorkflowService(CustomerIntakeRepository intakeRepo) {
        this.numberGenerator = new IntakeNumberGenerator(intakeRepo);
    }

    /** 워크플로우 입력값. null 필드는 "이번에 저장 안함". */
    public static class Form {
        /** 항상 필수. id 가 null 이면 신규, 있으면 수정. */
        public CustomerIntake intake;

        /** 저장할 대차. null 이면 이번 저장에서 대차 처리 안 함. */
        public RentalHistory rental;
        /** 기존에 있던 대차이력을 이번에 비울 때 그 id. null 이면 삭제 없음. */
        public Long rentalIdToDelete;

        /** 자차 청구 (없으면 null). */
        public InsuranceClaim ownClaim;
        public Long ownClaimIdToDelete;

        /** 상대방 청구 (없으면 null). */
        public InsuranceClaim opponentClaim;
        public Long opponentClaimIdToDelete;
    }

    public CustomerIntake save(Form form) {
        validateIntake(form.intake);
        if (form.rental != null) validateRental(form.rental);
        if (form.ownClaim != null) validateClaim(form.ownClaim, "자차");
        if (form.opponentClaim != null) validateClaim(form.opponentClaim, "상대");

        // 입고번호 생성은 Tx 밖에서 — 조회 트랜잭션이 별도여도 괜찮음
        if (form.intake.getId() == null
                && (form.intake.getIntakeNo() == null || form.intake.getIntakeNo().isBlank())) {
            form.intake.setIntakeNo(numberGenerator.next(form.intake.getIntakeDate()));
        }

        return Tx.call(s -> {
            CustomerIntake savedIntake = upsertIntake(s, form.intake);

            // 대차
            if (form.rentalIdToDelete != null) {
                RentalHistory existing = s.get(RentalHistory.class, form.rentalIdToDelete);
                if (existing != null) {
                    savedIntake.getRentals().remove(existing);
                    s.remove(existing);
                }
            }
            if (form.rental != null) {
                form.rental.setIntake(savedIntake);
                if (form.rental.getId() == null) s.persist(form.rental);
                else s.merge(form.rental);
            }

            // 자차/상대 청구
            handleClaim(s, savedIntake, form.ownClaim, form.ownClaimIdToDelete, ClaimSide.OWN);
            handleClaim(s, savedIntake, form.opponentClaim, form.opponentClaimIdToDelete, ClaimSide.OPPONENT);

            return savedIntake;
        });
    }

    private CustomerIntake upsertIntake(Session s, CustomerIntake input) {
        if (input.getId() == null) {
            s.persist(input);
            return input;
        }
        // 기존 입고 — DB 에서 managed 인스턴스를 가져와 필드만 복사한다.
        // form.intake 의 claims/rentals 컬렉션이 cascade·orphan-removal 로
        // 의도치 않게 반영되는 것을 막기 위해 merge 대신 필드 단위 갱신.
        CustomerIntake managed = s.get(CustomerIntake.class, input.getId());
        if (managed == null) {
            throw new IllegalArgumentException("입고 정보를 찾을 수 없습니다 (id=" + input.getId() + ")");
        }
        managed.setIntakeDate(input.getIntakeDate());
        managed.setVehicleName(input.getVehicleName());
        managed.setVehicleNumber(input.getVehicleNumber());
        managed.setPhone(input.getPhone());
        managed.setRepairType(input.getRepairType());
        managed.setAccidentTypeRaw(input.getAccidentTypeRaw());
        managed.setReleaseDate(input.getReleaseDate());
        managed.setSelfPayAmount(input.getSelfPayAmount());
        managed.setSelfPayDate(input.getSelfPayDate());
        managed.setMemo(input.getMemo());
        return managed;
    }

    private void handleClaim(Session s, CustomerIntake intake,
                             InsuranceClaim claim, Long idToDelete, ClaimSide side) {
        if (idToDelete != null) {
            InsuranceClaim existing = s.get(InsuranceClaim.class, idToDelete);
            if (existing != null) {
                // claims 컬렉션이 EAGER + cascade=ALL 이라 컬렉션에서도 제거하지 않으면
                // flush 시점에 cascade 가 다시 저장하려 한다.
                intake.getClaims().remove(existing);
                s.remove(existing);
            }
        }
        if (claim != null) {
            claim.setIntake(intake);
            claim.setClaimSide(side);
            if (claim.getId() == null) s.persist(claim);
            else s.merge(claim);
        }
    }

    private void validateIntake(CustomerIntake i) {
        if (i == null) throw new IllegalArgumentException("입고 정보가 비어 있습니다.");
        if (i.getIntakeDate() == null) throw new IllegalArgumentException("입고일을 입력해 주세요.");
        if (Validators.isBlank(i.getVehicleName())) throw new IllegalArgumentException("차량명을 입력해 주세요.");
        if (!Validators.isValidVehicleNumber(i.getVehicleNumber())) {
            throw new IllegalArgumentException("차량번호는 한글이 포함되어야 합니다 (예: 12가3456).");
        }
        if (i.getRepairType() == null) i.setRepairType(RepairType.GENERAL);
    }

    private void validateRental(RentalHistory r) {
        if (r.getRentalVehicle() == null) throw new IllegalArgumentException("대차차량을 선택해 주세요.");
        if (r.getRentalStartDate() == null) throw new IllegalArgumentException("대차 시작일을 입력해 주세요.");
        if (r.getRentalEndDate() != null && r.getRentalEndDate().isBefore(r.getRentalStartDate())) {
            throw new IllegalArgumentException("종료일은 시작일보다 빠를 수 없습니다.");
        }
    }

    private void validateClaim(InsuranceClaim c, String label) {
        if (c.getClaimAmount() != null && c.getClaimAmount() < 0) {
            throw new IllegalArgumentException(label + " 청구액은 음수일 수 없습니다.");
        }
        if (c.getReceivedAmount() != null && c.getReceivedAmount() < 0) {
            throw new IllegalArgumentException(label + " 실수령액은 음수일 수 없습니다.");
        }
        if (c.getReceivedDate() != null
                && c.getClaimDate() != null
                && c.getReceivedDate().isBefore(c.getClaimDate())) {
            throw new IllegalArgumentException(label + " 입금일은 청구일보다 빠를 수 없습니다.");
        }
    }
}
