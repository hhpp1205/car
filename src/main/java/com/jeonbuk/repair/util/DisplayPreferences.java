package com.jeonbuk.repair.util;

import javafx.scene.Scene;

import java.nio.charset.StandardCharsets;
import java.util.Base64;
import java.util.prefs.Preferences;

/**
 * 사용자가 [표시 설정] 다이얼로그에서 고른 글자 크기·테이블 줄 높이를 영구 저장하고,
 * 실행 중인 Scene 에 동적 CSS 로 즉시 반영한다.
 *
 * 동작: 메인 Scene 의 stylesheets 에 base64 data URL 한 장을 추가/교체.
 *      data URL 은 내용이 바뀌면 URL 자체가 달라지므로 JavaFX CSS 캐시 우회가 자동으로 됨.
 *      app.css 보다 늦게 들어가 cascade 마지막에 위치 → 기본값을 안전하게 override.
 */
public final class DisplayPreferences {

    public enum FontSize {
        SMALL ("작게", 11, 8),
        NORMAL("보통", 13, 9),
        LARGE ("크게", 15, 11),
        XLARGE("아주 크게", 17, 13);

        public final String label;
        public final int rootPx;   // .root 폰트
        public final int tablePx;  // .table-view 폰트 (배지도 동일)

        FontSize(String label, int rootPx, int tablePx) {
            this.label = label;
            this.rootPx = rootPx;
            this.tablePx = tablePx;
        }
    }

    public enum RowHeight {
        COMPACT("콤팩트", 19),
        NORMAL ("보통", 24),
        ROOMY  ("넓게", 30);

        public final String label;
        public final int px;

        RowHeight(String label, int px) {
            this.label = label;
            this.px = px;
        }
    }

    private static final Preferences PREFS =
            Preferences.userNodeForPackage(DisplayPreferences.class).node("display");
    private static final String K_FONT_SIZE  = "fontSize";
    private static final String K_ROW_HEIGHT = "rowHeight";
    private static final String DATA_URL_PREFIX = "data:text/css;base64,";

    private DisplayPreferences() {}

    public static FontSize getFontSize() {
        try { return FontSize.valueOf(PREFS.get(K_FONT_SIZE, FontSize.NORMAL.name())); }
        catch (IllegalArgumentException e) { return FontSize.NORMAL; }
    }

    public static void setFontSize(FontSize v) {
        if (v != null) PREFS.put(K_FONT_SIZE, v.name());
    }

    public static RowHeight getRowHeight() {
        try { return RowHeight.valueOf(PREFS.get(K_ROW_HEIGHT, RowHeight.COMPACT.name())); }
        catch (IllegalArgumentException e) { return RowHeight.COMPACT; }
    }

    public static void setRowHeight(RowHeight v) {
        if (v != null) PREFS.put(K_ROW_HEIGHT, v.name());
    }

    /** 현재 저장된 설정을 scene 에 동적 stylesheet 로 적용. 기존 동적 시트는 교체된다. */
    public static void apply(Scene scene) {
        if (scene == null) return;
        FontSize fs  = getFontSize();
        RowHeight rh = getRowHeight();
        // .toss-header 자손에 명시 override — AtlantaFX 테마가 .button/.toggle-button/.text-field 에
        // 자체 폰트 사이즈를 박아둬서 .root 상속만으론 헤더가 스케일되지 않는다.
        String css = String.format(
                ".root { -fx-font-size: %1$dpx; } " +
                ".table-view { -fx-font-size: %2$dpx; } " +
                ".status-badge { -fx-font-size: %2$dpx; } " +
                ".table-view .table-row-cell { -fx-cell-size: %3$d; } " +
                ".toss-header .button, " +
                ".toss-header .toggle-button, " +
                ".toss-header .text-field { -fx-font-size: %1$dpx; }",
                fs.rootPx, fs.tablePx, rh.px
        );
        String url = DATA_URL_PREFIX +
                Base64.getEncoder().encodeToString(css.getBytes(StandardCharsets.UTF_8));
        scene.getStylesheets().removeIf(s -> s.startsWith(DATA_URL_PREFIX));
        scene.getStylesheets().add(url);
    }
}
