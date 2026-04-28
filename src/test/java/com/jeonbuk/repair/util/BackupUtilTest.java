package com.jeonbuk.repair.util;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.LocalDate;

import static org.junit.jupiter.api.Assertions.*;

class BackupUtilTest {

    @Test
    @DisplayName("backupIfNeeded — DB 파일이 없으면 아무것도 하지 않는다")
    void no_op_when_db_missing(@TempDir Path tmp) {
        Path db = tmp.resolve("repair.db");
        Path backup = tmp.resolve("backup");
        BackupUtil.backupIfNeeded(db, backup, LocalDate.of(2026, 4, 29));
        assertFalse(Files.exists(backup), "DB 가 없으면 backup 디렉토리도 만들지 않아야 한다");
    }

    @Test
    @DisplayName("backupIfNeeded — 오늘자 백업 파일을 만든다")
    void creates_today_snapshot(@TempDir Path tmp) throws IOException {
        Path db = tmp.resolve("repair.db");
        Files.writeString(db, "fake-db-content");
        Path backup = tmp.resolve("backup");

        BackupUtil.backupIfNeeded(db, backup, LocalDate.of(2026, 4, 29));

        Path snapshot = backup.resolve("repair-20260429.db");
        assertTrue(Files.exists(snapshot));
        assertEquals("fake-db-content", Files.readString(snapshot));
    }

    @Test
    @DisplayName("backupIfNeeded — 같은 날 두 번 호출해도 한 번만 만든다")
    void same_day_is_idempotent(@TempDir Path tmp) throws IOException {
        Path db = tmp.resolve("repair.db");
        Files.writeString(db, "v1");
        Path backup = tmp.resolve("backup");
        LocalDate today = LocalDate.of(2026, 4, 29);

        BackupUtil.backupIfNeeded(db, backup, today);
        Files.writeString(db, "v2");
        BackupUtil.backupIfNeeded(db, backup, today);

        Path snapshot = backup.resolve("repair-20260429.db");
        assertEquals("v1", Files.readString(snapshot), "이미 오늘 백업이 있으면 덮어쓰지 않아야 한다");
    }

    @Test
    @DisplayName("backupIfNeeded — KEEP 보다 오래된 백업은 삭제된다")
    void prunes_old_backups(@TempDir Path tmp) throws IOException {
        Path db = tmp.resolve("repair.db");
        Files.writeString(db, "x");
        Path backup = tmp.resolve("backup");
        Files.createDirectories(backup);

        // KEEP+5 개 가짜 백업 (전부 다른 날짜)
        for (int i = 0; i < BackupUtil.KEEP + 5; i++) {
            LocalDate d = LocalDate.of(2026, 1, 1).plusDays(i);
            Files.writeString(backup.resolve("repair-" + d.toString().replace("-", "") + ".db"), "old");
        }

        // 새 백업 — 오늘자 추가 시 가장 오래된 5개가 삭제되어야 함
        BackupUtil.backupIfNeeded(db, backup, LocalDate.of(2026, 4, 29));

        long count;
        try (var s = Files.list(backup)) {
            count = s.count();
        }
        assertEquals(BackupUtil.KEEP, count,
                "KEEP=" + BackupUtil.KEEP + " 개만 남아야 한다");

        // 가장 최신 KEEP 개가 보존되었는지 (오늘자 포함)
        assertTrue(Files.exists(backup.resolve("repair-20260429.db")));
    }
}
