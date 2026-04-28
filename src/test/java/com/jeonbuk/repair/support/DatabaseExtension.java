package com.jeonbuk.repair.support;

import com.jeonbuk.repair.util.HibernateUtil;
import com.jeonbuk.repair.util.Tx;
import org.junit.jupiter.api.extension.AfterAllCallback;
import org.junit.jupiter.api.extension.AfterEachCallback;
import org.junit.jupiter.api.extension.BeforeAllCallback;
import org.junit.jupiter.api.extension.ExtensionContext;
import org.junit.jupiter.api.extension.ExtensionContext.Namespace;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;

/**
 * 통합 테스트용 — 임시 SQLite 파일을 만들고 전역 SessionFactory 를 교체한다.
 * 테스트 클래스마다 격리된 DB.
 *
 * <pre>
 * &#64;ExtendWith(DatabaseExtension.class)
 * class FooRepositoryTest { ... }
 * </pre>
 *
 * 각 테스트 메서드 종료 시 customer/insurance/rental 테이블의 유저 데이터를 비우고
 * 시드(rental_vehicle/insurance_company)는 보존한다.
 */
public class DatabaseExtension implements BeforeAllCallback, AfterAllCallback, AfterEachCallback {

    private static final Namespace NS = Namespace.create(DatabaseExtension.class);
    private static final String KEY_DB = "tempDb";

    @Override
    public void beforeAll(ExtensionContext ctx) throws IOException {
        Path tempDb = Files.createTempFile("test-repair-", ".db");
        Files.delete(tempDb); // 빈 파일이 아니라 새 DB 가 생성되도록 미리 제거
        ctx.getStore(NS).put(KEY_DB, tempDb);
        HibernateUtil.replaceWithDatabase(tempDb.toString());
    }

    @Override
    public void afterEach(ExtensionContext ctx) {
        // 트랜잭션 데이터만 비우기 — 시드(rental_vehicle, insurance_company)는 유지
        Tx.run(s -> {
            s.createNativeMutationQuery("DELETE FROM insurance_claim").executeUpdate();
            s.createNativeMutationQuery("DELETE FROM rental_history").executeUpdate();
            s.createNativeMutationQuery("DELETE FROM customer_intake").executeUpdate();
        });
    }

    @Override
    public void afterAll(ExtensionContext ctx) throws IOException {
        HibernateUtil.shutdown();
        Path tempDb = ctx.getStore(NS).get(KEY_DB, Path.class);
        if (tempDb != null) {
            Files.deleteIfExists(tempDb);
            // SQLite WAL/journal 잔여물도 정리
            Files.deleteIfExists(tempDb.resolveSibling(tempDb.getFileName() + "-journal"));
            Files.deleteIfExists(tempDb.resolveSibling(tempDb.getFileName() + "-wal"));
            Files.deleteIfExists(tempDb.resolveSibling(tempDb.getFileName() + "-shm"));
        }
    }
}
