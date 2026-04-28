package com.jeonbuk.repair.util;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.Comparator;
import java.util.List;
import java.util.stream.Stream;

/**
 * DB 일일 백업.
 *
 * 앱 시작 시 마이그레이션 실행 직전에 호출 — 만약 신규 마이그레이션이 잘못 짜여
 * 데이터가 깨지더라도 {@code backup/repair-YYYYMMDD.db} 로 복구 가능하다.
 * 하루 1회만 생성하고 최근 {@value #KEEP} 개만 남긴다.
 */
public final class BackupUtil {

    private static final Logger log = LoggerFactory.getLogger(BackupUtil.class);
    private static final DateTimeFormatter STAMP = DateTimeFormatter.ofPattern("yyyyMMdd");
    static final int KEEP = 10;
    private static final String PREFIX = "repair-";
    private static final String SUFFIX = ".db";

    private BackupUtil() {}

    /** 오늘자 백업이 없으면 생성하고, 오래된 것은 KEEP 개만 남기고 삭제. */
    public static void backupIfNeeded(Path db) {
        backupIfNeeded(db, AppPaths.backupDir(), LocalDate.now());
    }

    static void backupIfNeeded(Path db, Path backupDir, LocalDate today) {
        if (db == null || !Files.exists(db)) return;
        try {
            Files.createDirectories(backupDir);
            Path target = backupDir.resolve(PREFIX + today.format(STAMP) + SUFFIX);
            if (Files.exists(target)) return;
            Files.copy(db, target, StandardCopyOption.REPLACE_EXISTING);
            log.info("DB 백업 생성: {}", target);
            prune(backupDir);
        } catch (IOException e) {
            log.warn("DB 백업 실패 (실행은 계속) — {}", e.getMessage());
        }
    }

    private static void prune(Path dir) throws IOException {
        try (Stream<Path> s = Files.list(dir)) {
            List<Path> snapshots = s
                    .filter(p -> {
                        String n = p.getFileName().toString();
                        return n.startsWith(PREFIX) && n.endsWith(SUFFIX);
                    })
                    .sorted(Comparator.reverseOrder())
                    .toList();
            for (int i = KEEP; i < snapshots.size(); i++) {
                try {
                    Files.deleteIfExists(snapshots.get(i));
                } catch (IOException e) {
                    log.warn("오래된 백업 삭제 실패 ({}): {}", snapshots.get(i), e.getMessage());
                }
            }
        }
    }
}
