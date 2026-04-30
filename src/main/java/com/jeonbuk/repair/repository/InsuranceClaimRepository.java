package com.jeonbuk.repair.repository;

import com.jeonbuk.repair.model.ClaimSide;
import com.jeonbuk.repair.model.InsuranceClaim;
import com.jeonbuk.repair.util.Tx;

import java.time.LocalDate;
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
        return Tx.read(s -> s.createQuery("""
                        select c from InsuranceClaim c
                        join fetch c.intake
                        where c.intake.id = :id
                        order by c.claimSide
                        """, InsuranceClaim.class)
                .setParameter("id", intakeId)
                .getResultList());
    }

    public Optional<InsuranceClaim> findByIntakeAndSide(Long intakeId, ClaimSide side) {
        return Tx.read(s -> s.createQuery("""
                        select c from InsuranceClaim c
                        join fetch c.intake
                        where c.intake.id = :id and c.claimSide = :side
                        """, InsuranceClaim.class)
                .setParameter("id", intakeId)
                .setParameter("side", side)
                .uniqueResultOptional());
    }

    public List<InsuranceClaim> findAll() {
        return Tx.read(s -> s.createQuery("""
                        select c from InsuranceClaim c
                        join fetch c.intake
                        order by c.claimDate desc nulls last, c.id desc
                        """, InsuranceClaim.class)
                .getResultList());
    }

    public void delete(Long id) {
        Tx.run(s -> {
            InsuranceClaim e = s.get(InsuranceClaim.class, id);
            if (e != null) s.remove(e);
        });
    }

    /**
     * 통계 화면용 — 청구일/측면 조건을 DB 에서 필터링.
     * <p>{@code claimDate is not null} 은 통계 의미상 항상 강제 (집계 대상에서 미입력 청구는 제외).
     */
    public List<InsuranceClaim> findFiltered(LocalDate from, LocalDate to, ClaimSide side) {
        StringBuilder hql = new StringBuilder("""
                select c from InsuranceClaim c
                join fetch c.intake
                where c.claimDate is not null
                """);
        if (from != null) hql.append(" and c.claimDate >= :from");
        if (to   != null) hql.append(" and c.claimDate <= :to");
        if (side != null) hql.append(" and c.claimSide = :side");
        hql.append(" order by c.claimDate desc nulls last, c.id desc");
        return Tx.read(s -> {
            var q = s.createQuery(hql.toString(), InsuranceClaim.class);
            if (from != null) q.setParameter("from", from);
            if (to   != null) q.setParameter("to",   to);
            if (side != null) q.setParameter("side", side);
            return q.getResultList();
        });
    }

    /**
     * 대시보드용 — 미수금 합계 (claim_amount - received_amount, 음수는 0).
     * @param includeClosed false 면 종결된 입고의 청구는 집계에서 제외.
     */
    public long sumOutstanding(boolean includeClosed) {
        String base = """
                select coalesce(sum(
                    case when (coalesce(c.claimAmount, 0) - coalesce(c.receivedAmount, 0)) > 0
                         then (coalesce(c.claimAmount, 0) - coalesce(c.receivedAmount, 0))
                         else 0 end), 0L)
                from InsuranceClaim c
                """;
        final String hql = includeClosed ? base : base + " where c.intake.closedAt is null";
        return Tx.read(s -> {
            Long v = s.createQuery(hql, Long.class).uniqueResult();
            return v == null ? 0L : v;
        });
    }

    /**
     * 대시보드용 — 30일 경과 미수령 건수.
     * @param threshold 이 날짜보다 청구일이 이전이면 overdue. (호출자가 LocalDate.now().minusDays(30) 등 전달)
     */
    public long countOverdue(boolean includeClosed, LocalDate threshold) {
        String base = """
                select count(c) from InsuranceClaim c
                where c.claimDate is not null
                  and c.receivedDate is null
                  and c.claimDate < :threshold
                """;
        final String hql = includeClosed ? base : base + " and c.intake.closedAt is null";
        return Tx.read(s -> s.createQuery(hql, Long.class)
                .setParameter("threshold", threshold)
                .uniqueResult());
    }
}
