package com.jeonbuk.repair.util;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.nio.file.Path;

import static org.junit.jupiter.api.Assertions.*;

class AppPathsTest {

    @Test
    @DisplayName("home — JEONBUK_REPAIR_HOME 미설정 시 OS 기본 경로를 반환한다")
    void home_falls_back_to_os_default() {
        Path home = AppPaths.home();
        assertNotNull(home);
        // Windows 면 APPDATA 하위에, 그 외엔 user.home 하위에 위치
        String os = System.getProperty("os.name", "").toLowerCase();
        if (os.contains("win")) {
            String appData = System.getenv("APPDATA");
            if (appData != null && !appData.isBlank()) {
                assertTrue(home.toString().startsWith(appData),
                        "Windows 에서는 APPDATA 하위여야 함: " + home);
            }
        } else {
            assertTrue(home.toString().startsWith(System.getProperty("user.home")));
        }
    }

    @Test
    @DisplayName("dbFile — home 디렉토리 아래 repair.db 를 가리킨다")
    void db_file_is_under_home() {
        assertEquals("repair.db", AppPaths.dbFile().getFileName().toString());
        assertEquals(AppPaths.home(), AppPaths.dbFile().getParent());
    }

    @Test
    @DisplayName("backupDir — home 디렉토리 아래 backup 폴더를 가리킨다")
    void backup_dir_is_under_home() {
        assertEquals("backup", AppPaths.backupDir().getFileName().toString());
        assertEquals(AppPaths.home(), AppPaths.backupDir().getParent());
    }
}
