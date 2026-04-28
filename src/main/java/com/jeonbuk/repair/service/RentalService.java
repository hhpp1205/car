package com.jeonbuk.repair.service;

import com.jeonbuk.repair.model.RentalHistory;
import com.jeonbuk.repair.model.RentalVehicle;
import com.jeonbuk.repair.repository.RentalHistoryRepository;
import com.jeonbuk.repair.repository.RentalVehicleRepository;

import java.util.List;
import java.util.Optional;

public class RentalService {

    private final RentalHistoryRepository historyRepo;
    private final RentalVehicleRepository vehicleRepo;

    public RentalService(RentalHistoryRepository historyRepo, RentalVehicleRepository vehicleRepo) {
        this.historyRepo = historyRepo;
        this.vehicleRepo = vehicleRepo;
    }

    public List<RentalVehicle> listVehicles() {
        return vehicleRepo.findActive();
    }

    public List<RentalHistory> findAll() {
        return historyRepo.findAll();
    }

    public List<RentalHistory> findActive() {
        return historyRepo.findActive();
    }

    public List<RentalHistory> findByIntakeId(Long intakeId) {
        return historyRepo.findByIntakeId(intakeId);
    }

    public RentalHistory save(RentalHistory r) {
        if (r.getIntake() == null) throw new IllegalArgumentException("연결된 입고가 없습니다.");
        if (r.getRentalVehicle() == null) throw new IllegalArgumentException("대차차량을 선택해 주세요.");
        if (r.getRentalStartDate() == null) throw new IllegalArgumentException("대차 시작일을 입력해 주세요.");
        if (r.getRentalEndDate() != null && r.getRentalEndDate().isBefore(r.getRentalStartDate())) {
            throw new IllegalArgumentException("종료일은 시작일보다 빠를 수 없습니다.");
        }
        // 신규 대차 시 차량이 이미 사용중이면 거부 (편집 시는 자기 자신 제외)
        if (r.getId() == null && r.getRentalEndDate() == null) {
            if (historyRepo.isVehicleInUse(r.getRentalVehicle().getId())) {
                throw new IllegalArgumentException(r.getRentalVehicle().getName() + " 은(는) 현재 대차중입니다.");
            }
        }
        return historyRepo.save(r);
    }

    public void delete(Long id) {
        historyRepo.delete(id);
    }

    public Optional<RentalHistory> findById(Long id) {
        return historyRepo.findById(id);
    }
}
