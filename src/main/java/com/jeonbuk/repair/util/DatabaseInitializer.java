package com.jeonbuk.repair.util;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

/**
 * SQLite DB 파일 + 마이그레이션 SQL을 실행해 스키마/시드를 보장한다.
 * Hibernate SessionFactory 부트스트랩 전에 호출.
 */
public final class DatabaseInitializer {

    private static final Logger log = LoggerFactory.getLogger(DatabaseInitializer.class);

    /** SQLite-JDBC 의 timestamp 처리를 텍스트 모드로 강제 — Hibernate write/read 형식 일치. */
    private static final String URL_OPTIONS = "?foreign_keys=on&date_class=TEXT&date_string_format=yyyy-MM-dd HH:mm:ss.SSS";

    /** classpath 의 db/migration 아래 V*.sql 들 (실행 순서 보장) */
    private static final List<String> MIGRATIONS = List.of(
            "db/migration/V001__init.sql",
            "db/migration/V002__seed.sql",
            "db/migration/V003__add_tow_info.sql",
            "db/migration/V004__add_closed_status.sql"
    );

    private DatabaseInitializer() {}

    /** 운영 DB 파일 경로 — {@link AppPaths#dbFile()} 기준. */
    public static String defaultDbFile() {
        return AppPaths.dbFile().toString();
    }

    /** 운영용 — 사용자 영역의 repair.db 에 마이그레이션 적용. */
    public static void init() {
        init(defaultDbFile());
    }

    /** 테스트용 — 임의 경로의 DB 파일에 마이그레이션 적용. */
    public static void init(String dbFile) {
        ensureParentDir(dbFile);
        String url = jdbcUrl(dbFile);
        try (Connection conn = DriverManager.getConnection(url)) {
            for (String resource : MIGRATIONS) {
                runScript(conn, resource);
            }
            log.info("DB 초기화 완료: {}", dbFile);
        } catch (SQLException | IOException e) {
            throw new IllegalStateException("DB 초기화 실패: " + e.getMessage(), e);
        }
    }

    public static String jdbcUrl(String dbFile) {
        return "jdbc:sqlite:" + dbFile + URL_OPTIONS;
    }

    private static void ensureParentDir(String dbFile) {
        try {
            Path p = Path.of(dbFile).toAbsolutePath();
            if (p.getParent() != null) {
                Files.createDirectories(p.getParent());
            }
        } catch (IOException e) {
            throw new IllegalStateException("데이터 디렉토리 생성 실패: " + dbFile, e);
        }
    }

    private static void runScript(Connection conn, String resourcePath) throws IOException, SQLException {
        List<String> statements = readStatements(resourcePath);
        try (Statement stmt = conn.createStatement()) {
            for (String sql : statements) {
                if (sql.isBlank()) continue;
                try {
                    stmt.execute(sql);
                } catch (SQLException e) {
                    // SQLite 의 ALTER TABLE ADD COLUMN 은 IF NOT EXISTS 가 없어서
                    // 재실행 시 "duplicate column name" 으로 실패한다 — 멱등 보장 위해 무시.
                    String msg = e.getMessage() == null ? "" : e.getMessage().toLowerCase(java.util.Locale.ROOT);
                    if (msg.contains("duplicate column name")) {
                        log.debug("이미 적용된 ALTER 무시: {}", sql);
                        continue;
                    }
                    throw e;
                }
            }
        }
        log.debug("마이그레이션 적용: {} ({}건)", resourcePath, statements.size());
    }

    /** 단순 ; 분리 + 라인 주석(--) 제거. SQLite 마이그레이션용 최소 파서. */
    private static List<String> readStatements(String resourcePath) throws IOException {
        InputStream in = DatabaseInitializer.class.getClassLoader().getResourceAsStream(resourcePath);
        if (in == null) {
            log.warn("마이그레이션 파일 없음 — 건너뜀: {}", resourcePath);
            return Collections.emptyList();
        }
        StringBuilder buf = new StringBuilder();
        try (BufferedReader r = new BufferedReader(new InputStreamReader(in, StandardCharsets.UTF_8))) {
            String line;
            while ((line = r.readLine()) != null) {
                int idx = line.indexOf("--");
                if (idx >= 0) line = line.substring(0, idx);
                buf.append(line).append('\n');
            }
        }
        List<String> out = new ArrayList<>();
        for (String s : buf.toString().split(";")) {
            String trimmed = s.trim();
            if (!trimmed.isEmpty()) out.add(trimmed);
        }
        return out;
    }
}
