package com.jeonbuk.repair.util;

import javafx.scene.text.Font;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.InputStream;
import java.util.List;

/**
 * 앱 번들에 포함된 Pretendard 폰트를 JavaFX 에 등록한다.
 *
 * 시스템에 Pretendard 가 설치되어 있지 않은 PC 에서도 동일한 한글 룩을 보장하기 위함이다.
 * Main.start() 초반에 한 번 호출.
 *
 * 폰트 라이선스: SIL Open Font License 1.1 — {@code resources/fonts/LICENSE-Pretendard.txt}
 */
public final class Fonts {

    private static final Logger log = LoggerFactory.getLogger(Fonts.class);

    private static final List<String> RESOURCES = List.of(
            "/fonts/Pretendard-Regular.otf",
            "/fonts/Pretendard-Medium.otf",
            "/fonts/Pretendard-Bold.otf"
    );

    private static volatile boolean loaded = false;

    private Fonts() {}

    public static synchronized void registerAll() {
        if (loaded) return;
        for (String r : RESOURCES) {
            try (InputStream in = Fonts.class.getResourceAsStream(r)) {
                if (in == null) {
                    log.warn("폰트 리소스 누락 — 시스템 폴백 사용: {}", r);
                    continue;
                }
                Font font = Font.loadFont(in, 13);
                if (font == null) {
                    log.warn("폰트 로드 실패 (Font.loadFont 가 null): {}", r);
                } else {
                    log.debug("폰트 등록: {} ({})", font.getName(), r);
                }
            } catch (Exception e) {
                log.warn("폰트 로드 예외: {} — {}", r, e.getMessage());
            }
        }
        loaded = true;
    }
}
