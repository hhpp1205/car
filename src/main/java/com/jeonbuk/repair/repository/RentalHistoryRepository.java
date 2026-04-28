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
                        from RentalHistory
                        order by rentalStartDate desc, id desc
                        """, RentalHistory.class)
                .getResultList());
    }

    public List<RentalHistory> findActive() {
        return Tx.read(s -> s.createQuery("""
                        from RentalHistory
                        where rentalEndDate is null
                        order by rentalStartDate desc, id desc
                        """, RentalHistory.class)
                .getResultList());
    }

    public List<RentalHistory> findByIntakeId(Long intakeId) {
        return Tx.read(s -> s.createQuery("""
                        from RentalHistory
                        where intake.id = :id
                        order by rentalStartDate desc, id desc
                        """, RentalHistory.class)
                .setParameter("id", intakeId)
                .getResultList());
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
