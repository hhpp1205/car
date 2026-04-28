package com.jeonbuk.repair.util;

import org.hibernate.Session;
import org.hibernate.Transaction;

import java.util.function.Consumer;
import java.util.function.Function;

/**
 * 트랜잭션 헬퍼 — 단일 사용자 / 단일 PC 환경.
 * 매 호출마다 Session open / commit or rollback / close.
 */
public final class Tx {

    private Tx() {}

    public static <T> T call(Function<Session, T> work) {
        try (Session session = HibernateUtil.getSessionFactory().openSession()) {
            Transaction tx = session.beginTransaction();
            try {
                T result = work.apply(session);
                tx.commit();
                return result;
            } catch (RuntimeException e) {
                if (tx.isActive()) tx.rollback();
                throw e;
            }
        }
    }

    public static void run(Consumer<Session> work) {
        call(session -> { work.accept(session); return null; });
    }

    /** 읽기 전용 — 트랜잭션 없이 Session 만 열어 조회. */
    public static <T> T read(Function<Session, T> work) {
        try (Session session = HibernateUtil.getSessionFactory().openSession()) {
            return work.apply(session);
        }
    }
}
