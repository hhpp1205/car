package com.jeonbuk.repair.repository;

import com.jeonbuk.repair.model.RentalVehicle;
import com.jeonbuk.repair.util.Tx;

import java.util.List;
import java.util.Optional;

public class RentalVehicleRepository {

    public RentalVehicle save(RentalVehicle v) {
        return Tx.call(s -> {
            if (v.getId() == null) {
                s.persist(v);
                return v;
            }
            return s.merge(v);
        });
    }

    public Optional<RentalVehicle> findById(Long id) {
        return Tx.read(s -> Optional.ofNullable(s.get(RentalVehicle.class, id)));
    }

    public List<RentalVehicle> findAll() {
        return Tx.read(s -> s.createQuery(
                        "from RentalVehicle order by id", RentalVehicle.class)
                .getResultList());
    }

    public List<RentalVehicle> findActive() {
        return Tx.read(s -> s.createQuery(
                        "from RentalVehicle where active = true order by id", RentalVehicle.class)
                .getResultList());
    }
}
