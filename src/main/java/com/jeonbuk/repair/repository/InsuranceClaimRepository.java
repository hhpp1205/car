package com.jeonbuk.repair.repository;

import com.jeonbuk.repair.model.ClaimSide;
import com.jeonbuk.repair.model.InsuranceClaim;
import com.jeonbuk.repair.util.Tx;

import java.util.List;
import java.util.Optional;

public class InsuranceClaimRepository {

    public InsuranceClaim save(InsuranceClaim c) {
        return Tx.call(s -> {
            if (c.getId() == null) {
                s.persist(c);
                return c;
            }
            return s.merge(c);
        });
    }

    public Optional<InsuranceClaim> findById(Long id) {
        return Tx.read(s -> Optional.ofNullable(s.get(InsuranceClaim.class, id)));
    }

    public List<InsuranceClaim> findByIntakeId(Long intakeId) {
        return Tx.read(s -> s.createQuery(
                        "from InsuranceClaim where intake.id = :id order by claimSide", InsuranceClaim.class)
                .setParameter("id", intakeId)
                .getResultList());
    }

    public Optional<InsuranceClaim> findByIntakeAndSide(Long intakeId, ClaimSide side) {
        return Tx.read(s -> s.createQuery(
                        "from InsuranceClaim where intake.id = :id and claimSide = :side", InsuranceClaim.class)
                .setParameter("id", intakeId)
                .setParameter("side", side)
                .uniqueResultOptional());
    }

    public List<InsuranceClaim> findAll() {
        return Tx.read(s -> s.createQuery("""
                        from InsuranceClaim
                        order by claimDate desc nulls last, id desc
                        """, InsuranceClaim.class)
                .getResultList());
    }

    public void delete(Long id) {
        Tx.run(s -> {
            InsuranceClaim e = s.get(InsuranceClaim.class, id);
            if (e != null) s.remove(e);
        });
    }
}
