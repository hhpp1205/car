package com.jeonbuk.repair.repository;

import com.jeonbuk.repair.model.CustomerIntake;
import com.jeonbuk.repair.service.IntakeFilter;
import com.jeonbuk.repair.util.Tx;
import org.hibernate.Session;

import java.time.LocalDate;
import java.util.List;
import java.util.Optional;

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
        String where = closedClause(filter);
        String hql = "from CustomerIntake" + where + " order by intakeDate desc, id desc";
        return Tx.read(s -> s.createQuery(hql, CustomerIntake.class).getResultList());
    }

    public List<CustomerIntake> search(String keyword) {
        return search(keyword, IntakeFilter.ALL);
    }

    public List<CustomerIntake> search(String keyword, IntakeFilter filter) {
        if (keyword == null || keyword.isBlank()) return findAll(filter);
        String like = "%" + keyword.trim() + "%";
        String closed = closedClause(filter);
        String prefix = closed.isEmpty() ? " where " : closed + " and ";
        String hql = "from CustomerIntake"
                + prefix
                + "(intakeNo like :k or vehicleName like :k or vehicleNumber like :k or phone like :k)"
                + " order by intakeDate desc, id desc";
        return Tx.read(s -> s.createQuery(hql, CustomerIntake.class)
                .setParameter("k", like)
                .getResultList());
    }

    /** 필터에 맞는 HQL where 절(앞 공백 포함) 또는 빈 문자열. */
    private static String closedClause(IntakeFilter filter) {
        return switch (filter) {
            case ACTIVE -> " where closedAt is null";
            case CLOSED -> " where closedAt is not null";
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
}
