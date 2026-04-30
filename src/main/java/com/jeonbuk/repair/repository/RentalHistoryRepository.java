package com.jeonbuk.repair.repository;

import com.jeonbuk.repair.model.RentalHistory;
import com.jeonbuk.repair.util.Tx;

import java.util.List;
import java.util.Optional;

public class RentalHistoryRepository {

    public RentalHistory save(RentalHistory r) {
        return Tx.call(s -> {
            if (r.getId() == null) {
                s.persist(r);
                return r;
            }
            return s.merge(r);
        });
    }

    public Optional<RentalHistory> findById(Long id) {
        return Tx.read(s -> Optional.ofNullable(s.get(RentalHistory.class, id)));
    }

    public List<RentalHistory> findAll() {
        return Tx.read(s -> s.createQuery("""
                        select r from RentalHistory r
                        join fetch r.intake
                        join fetch r.rentalVehicle
                        order by r.rentalStartDate desc, r.id desc
                        """, RentalHistory.class)
                .getResultList());
    }

    public List<RentalHistory> findActive() {
        return Tx.read(s -> s.createQuery("""
                        select r from RentalHistory r
                        join fetch r.intake
                        join fetch r.rentalVehicle
                        where r.rentalEndDate is null
                        order by r.rentalStartDate desc, r.id desc
                        """, RentalHistory.class)
                .getResultList());
    }

    public List<RentalHistory> findByIntakeId(Long intakeId) {
        return Tx.read(s -> s.createQuery("""
                        select r from RentalHistory r
                        join fetch r.intake
                        join fetch r.rentalVehicle
                        where r.intake.id = :id
                        order by r.rentalStartDate desc, r.id desc
                        """, RentalHistory.class)
                .setParameter("id", intakeId)
                .getResultList());
    }

    /** 대시보드용 — 현재 대차중(종료일 NULL) 건수만 가볍게 조회. */
    public long countActive() {
        return Tx.read(s -> s.createQuery(
                        "select count(r) from RentalHistory r where r.rentalEndDate is null", Long.class)
                .uniqueResult());
    }

    /** 해당 차량이 현재 대차중인지 */
    public boolean isVehicleInUse(Long rentalVehicleId) {
        Long c = Tx.read(s -> s.createQuery("""
                        select count(*) from RentalHistory
                        where rentalVehicle.id = :v and rentalEndDate is null
                        """, Long.class)
                .setParameter("v", rentalVehicleId)
                .uniqueResult());
        return c != null && c > 0;
    }

    public void delete(Long id) {
        Tx.run(s -> {
            RentalHistory e = s.get(RentalHistory.class, id);
            if (e != null) s.remove(e);
        });
    }
}
