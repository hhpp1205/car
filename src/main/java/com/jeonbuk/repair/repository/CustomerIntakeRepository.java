package com.jeonbuk.repair.repository;

import com.jeonbuk.repair.model.CustomerIntake;
import com.jeonbuk.repair.service.IntakeFilter;
import com.jeonbuk.repair.util.Tx;
import org.hibernate.Session;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.Collection;
import java.util.HashSet;
import java.util.List;
import java.util.Objects;
import java.util.Optional;
import java.util.Set;

public class CustomerIntakeRepository {

    public CustomerIntake save(CustomerIntake intake) {
        return Tx.call(s -> {
            if (intake.getId() == null) {
                s.persist(intake);
                return intake;
            }
            return s.merge(intake);
        });
    }

    public Optional<CustomerIntake> findById(Long id) {
        return Tx.read(s -> Optional.ofNullable(s.get(CustomerIntake.class, id)));
    }

    public Optional<CustomerIntake> findByIntakeNo(String intakeNo) {
        return Tx.read(s -> s.createQuery(
                        "from CustomerIntake where intakeNo = :no", CustomerIntake.class)
                .setParameter("no", intakeNo)
                .uniqueResultOptional());
    }

    public List<CustomerIntake> findAll() {
        return findAll(IntakeFilter.ALL);
    }

    public List<CustomerIntake> findAll(IntakeFilter filter) {
        String where = closedClause(filter, "ci");
        String hql = "select distinct ci from CustomerIntake ci"
                + " left join fetch ci.claims"
                + where
                + " order by ci.intakeDate desc, ci.id desc";
        return Tx.read(s -> s.createQuery(hql, CustomerIntake.class).getResultList());
    }

    public List<CustomerIntake> search(String keyword) {
        return search(keyword, IntakeFilter.ALL);
    }

    public List<CustomerIntake> search(String keyword, IntakeFilter filter) {
        if (keyword == null || keyword.isBlank()) return findAll(filter);
        String like = "%" + keyword.trim() + "%";
        String closed = closedClause(filter, "ci");
        String prefix = closed.isEmpty() ? " where " : closed + " and ";
        String hql = "select distinct ci from CustomerIntake ci"
                + " left join fetch ci.claims"
                + prefix
                + "(ci.intakeNo like :k or ci.vehicleName like :k or ci.vehicleNumber like :k or ci.phone like :k)"
                + " order by ci.intakeDate desc, ci.id desc";
        return Tx.read(s -> s.createQuery(hql, CustomerIntake.class)
                .setParameter("k", like)
                .getResultList());
    }

    /** 필터에 맞는 HQL where 절(앞 공백 포함) 또는 빈 문자열. */
    private static String closedClause(IntakeFilter filter, String alias) {
        return switch (filter) {
            case ACTIVE -> " where " + alias + ".closedAt is null";
            case CLOSED -> " where " + alias + ".closedAt is not null";
            case ALL    -> "";
        };
    }

    /** 같은 날짜의 마지막 입고번호 (YYMMDD-NN) — 새 번호 생성용. */
    public Optional<String> findLastIntakeNoOfDate(LocalDate date) {
        String prefix = formatDatePrefix(date) + "%";
        return Tx.read(s -> s.createQuery("""
                        select intakeNo from CustomerIntake
                        where intakeNo like :p
                        order by intakeNo desc
                        """, String.class)
                .setParameter("p", prefix)
                .setMaxResults(1)
                .uniqueResultOptional());
    }

    public void delete(Long id) {
        Tx.run(s -> {
            CustomerIntake e = s.get(CustomerIntake.class, id);
            if (e != null) s.remove(e);
        });
    }

    public long count() {
        return Tx.read(s -> s.createQuery("select count(*) from CustomerIntake", Long.class).uniqueResult());
    }

    /** 대시보드용 — 출고일이 비어있는 진행 중 입고 건수. */
    public long countInProgress(boolean includeClosed) {
        String base = "select count(ci) from CustomerIntake ci where ci.releaseDate is null";
        final String hql = includeClosed ? base : base + " and ci.closedAt is null";
        return Tx.read(s -> s.createQuery(hql, Long.class).uniqueResult());
    }

    /**
     * 대시보드용 — 최근 입고 N건. claims 도 fetch join 하여 진행상태 산출 시 N+1 회피.
     * setMaxResults + collection fetch 는 Hibernate 가 in-memory 페이징을 경고하지만
     * limit 이 작아 실용상 영향 없음.
     */
    public List<CustomerIntake> findRecent(int limit, boolean includeClosed) {
        String hql = "select distinct ci from CustomerIntake ci"
                + " left join fetch ci.claims";
        if (!includeClosed) hql += " where ci.closedAt is null";
        hql += " order by ci.intakeDate desc, ci.id desc";
        String finalHql = hql;
        return Tx.read(s -> s.createQuery(finalHql, CustomerIntake.class)
                .setMaxResults(limit)
                .getResultList());
    }

    /** 자동완성용 — 등록된 차량명 distinct 목록 (오름차순). */
    public List<String> findDistinctVehicleNames() {
        return Tx.read(s -> s.createQuery("""
                        select distinct vehicleName from CustomerIntake
                        where vehicleName is not null
                        order by vehicleName
                        """, String.class)
                .getResultList());
    }

    /** YYMMDD */
    public static String formatDatePrefix(LocalDate date) {
        return String.format("%02d%02d%02d",
                date.getYear() % 100, date.getMonthValue(), date.getDayOfMonth());
    }

    /**
     * 일괄 종결/종결취소 — SELECT 1회 + UPDATE 1회 (단일 트랜잭션) 로 처리.
     * <ul>
     *   <li>{@code processed} : 실제 상태가 변경된 건수</li>
     *   <li>{@code skipped}   : 이미 목표 상태였던 건수</li>
     *   <li>{@code missingIds}: DB 에 존재하지 않는 id 들 (호출자가 실패 메시지로 변환)</li>
     * </ul>
     * 기존엔 id 마다 findById + save 를 반복해 N×3 회 트랜잭션이었던 것을 단일 호출로 압축.
     *
     * @param close true=종결(closedAt=ts 로 set), false=종결취소(closedAt=null)
     * @param ts    종결 시각. close=false 일 때도 updatedAt 갱신용으로 사용.
     */
    public BulkClosedUpdate bulkSetClosed(Collection<Long> ids, boolean close, LocalDateTime ts) {
        // 정규화 — null/중복 제거
        List<Long> normalized = (ids == null) ? List.of()
                : ids.stream().filter(Objects::nonNull).distinct().toList();
        if (normalized.isEmpty()) return new BulkClosedUpdate(0, 0, List.of());

        return Tx.call(s -> {
            // 1. 현재 상태를 한 번에 조회 — id, closedAt 만
            List<Object[]> rows = s.createQuery("""
                            select ci.id, ci.closedAt from CustomerIntake ci
                            where ci.id in (:ids)
                            """, Object[].class)
                    .setParameter("ids", normalized)
                    .getResultList();

            Set<Long> existing = new HashSet<>(rows.size());
            List<Long> toChange = new ArrayList<>(rows.size());
            int skipped = 0;
            for (Object[] row : rows) {
                Long id = (Long) row[0];
                LocalDateTime closedAt = (LocalDateTime) row[1];
                existing.add(id);
                boolean alreadyClosed = closedAt != null;
                if (alreadyClosed == close) {
                    skipped++;
                } else {
                    toChange.add(id);
                }
            }

            // 2. 누락 ID — 호출자가 실패 메시지로 변환
            List<Long> missing = new ArrayList<>();
            for (Long id : normalized) {
                if (!existing.contains(id)) missing.add(id);
            }

            // 3. 실제 변경이 필요한 건들만 단일 UPDATE
            int processed = 0;
            if (!toChange.isEmpty()) {
                String hql = close
                        ? "update CustomerIntake ci set ci.closedAt = :ts, ci.updatedAt = :ts where ci.id in (:ids)"
                        : "update CustomerIntake ci set ci.closedAt = null, ci.updatedAt = :ts where ci.id in (:ids)";
                processed = s.createMutationQuery(hql)
                        .setParameter("ts", ts)
                        .setParameter("ids", toChange)
                        .executeUpdate();
            }

            return new BulkClosedUpdate(processed, skipped, missing);
        });
    }

    /** {@link #bulkSetClosed} 결과. {@code missingIds} 는 호출자가 실패 메시지 포맷으로 변환. */
    public record BulkClosedUpdate(int processed, int skipped, List<Long> missingIds) {}
}
