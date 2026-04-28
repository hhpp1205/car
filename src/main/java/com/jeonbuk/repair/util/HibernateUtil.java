package com.jeonbuk.repair.util;

import org.hibernate.SessionFactory;
import org.hibernate.cfg.Configuration;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * SessionFactory 싱글턴. 운영 모드와 테스트 모드(임의 DB 경로) 둘 다 지원.
 */
public final class HibernateUtil {

    private static final Logger log = LoggerFactory.getLogger(HibernateUtil.class);

    private static volatile SessionFactory sessionFactory;

    private HibernateUtil() {}

    public static SessionFactory getSessionFactory() {
        SessionFactory local = sessionFactory;
        if (local == null) {
            synchronized (HibernateUtil.class) {
                local = sessionFactory;
                if (local == null) {
                    String db = DatabaseInitializer.defaultDbFile();
                    // 마이그레이션 실행 전 일일 백업 — 신규 마이그레이션이 잘못 짜여도 복구 가능.
                    BackupUtil.backupIfNeeded(java.nio.file.Path.of(db));
                    sessionFactory = local = build(db);
                }
            }
        }
        return local;
    }

    /**
     * 테스트 전용 — 기존 SessionFactory 를 닫고 지정한 DB 경로로 재구성.
     * @param dbFile 경로 (절대/상대 모두 가능)
     */
    public static synchronized void replaceWithDatabase(String dbFile) {
        shutdown();
        sessionFactory = build(dbFile);
    }

    private static SessionFactory build(String dbFile) {
        try {
            DatabaseInitializer.init(dbFile);
            Configuration cfg = new Configuration().configure();
            cfg.setProperty("hibernate.connection.url", DatabaseInitializer.jdbcUrl(dbFile));
            SessionFactory sf = cfg.buildSessionFactory();
            log.info("Hibernate SessionFactory 생성 완료 (db={})", dbFile);
            return sf;
        } catch (RuntimeException e) {
            log.error("SessionFactory 생성 실패", e);
            throw e;
        }
    }

    public static synchronized void shutdown() {
        SessionFactory sf = sessionFactory;
        if (sf != null && !sf.isClosed()) {
            sf.close();
            log.info("Hibernate SessionFactory 종료");
        }
        sessionFactory = null;
    }
}
