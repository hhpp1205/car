package com.jeonbuk.repair.util;

import java.nio.file.Path;
import java.nio.file.Paths;

/**
 * 사용자 데이터(DB · 백업) 저장 위치를 결정한다.
 *
 * 우선순위
 *   1. 환경변수 {@code JEONBUK_REPAIR_HOME} 가 설정돼 있으면 그 경로
 *   2. Windows: {@code %APPDATA%\전북공업사}
 *   3. 그 외 OS: {@code ~/.jeonbuk-repair}
 *
 * 이 분리 덕분에 jpackage 로 설치된 .exe 가
 * Program Files (쓰기 권한 없음) 가 아닌 사용자 영역에 DB 를 저장하므로,
 * 재설치/업데이트 시에도 데이터가 보존된다.
 */
public final class AppPaths {

    public static final String ENV_OVERRIDE = "JEONBUK_REPAIR_HOME";
    private static final String APP_FOLDER = "전북공업사";

    private AppPaths() {}

    public static Path home() {
        String env = System.getenv(ENV_OVERRIDE);
        if (env != null && !env.isBlank()) {
            return Paths.get(env);
        }
        String os = System.getProperty("os.name", "").toLowerCase();
        if (os.contains("win")) {
            String appData = System.getenv("APPDATA");
            if (appData == null || appData.isBlank()) {
                appData = System.getProperty("user.home");
            }
            return Paths.get(appData, APP_FOLDER);
        }
        return Paths.get(System.getProperty("user.home"), ".jeonbuk-repair");
    }

    public static Path dbFile() {
        return home().resolve("repair.db");
    }

    public static Path backupDir() {
        return home().resolve("backup");
    }
}
