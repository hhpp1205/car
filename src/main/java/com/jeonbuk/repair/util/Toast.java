package com.jeonbuk.repair.util;

import javafx.animation.FadeTransition;
import javafx.animation.PauseTransition;
import javafx.animation.SequentialTransition;
import javafx.geometry.Bounds;
import javafx.scene.Node;
import javafx.scene.control.Label;
import javafx.scene.layout.StackPane;
import javafx.stage.Popup;
import javafx.stage.Window;
import javafx.util.Duration;

import java.util.Objects;

/**
 * 우측 하단 토스트(스낵바) — 짧은 성공 메시지를 비차단으로 표시.
 *
 * <p>저장 같은 빈번한 액션의 "OK 클릭" 마찰을 줄이기 위함. 실패는 여전히 모달 다이얼로그.
 *
 * <p>사용 예: {@code Toast.show(anyNodeInScene, "저장되었습니다");}
 */
public final class Toast {

    private Toast() {}

    private static final Duration FADE_IN  = Duration.millis(150);
    private static final Duration HOLD     = Duration.seconds(2.5);
    private static final Duration FADE_OUT = Duration.millis(400);
    private static final double MARGIN = 24;

    /** 진행 중 토스트 — 새 토스트가 오면 즉시 교체. */
    private static Popup current;
    private static SequentialTransition currentAnim;

    /**
     * anchor 가 속한 창의 우측 하단에 메시지 토스트를 띄운다.
     *
     * @param anchor  scene 에 부착된 노드 (owner Window 결정용). null/scene 없음 → 조용히 무시.
     * @param message 표시 문자열.
     */
    public static void show(Node anchor, String message) {
        Objects.requireNonNull(message, "message");
        if (anchor == null || anchor.getScene() == null) return;
        Window owner = anchor.getScene().getWindow();
        if (owner == null || !owner.isShowing()) return;

        // 이전 토스트 정리 — 중첩 방지
        if (currentAnim != null) currentAnim.stop();
        if (current != null && current.isShowing()) current.hide();

        Label label = new Label(message);
        label.getStyleClass().add("toast-label");

        StackPane root = new StackPane(label);
        root.getStyleClass().add("toast-root");
        // Popup 의 scene 은 owner 의 stylesheet 를 자동 상속하지 않음 — 명시적으로 추가
        root.getStylesheets().add(Toast.class.getResource("/css/app.css").toExternalForm());
        root.setOpacity(0);

        Popup popup = new Popup();
        popup.getContent().add(root);
        popup.setAutoFix(false);
        popup.setAutoHide(false);
        // 일단 owner 좌표에 띄워 layout 발생 → bounds 측정 후 우측 하단으로 재배치
        popup.show(owner, owner.getX(), owner.getY());

        Bounds b = root.getBoundsInLocal();
        popup.setX(owner.getX() + owner.getWidth()  - b.getWidth()  - MARGIN);
        popup.setY(owner.getY() + owner.getHeight() - b.getHeight() - MARGIN);

        FadeTransition fadeIn = new FadeTransition(FADE_IN, root);
        fadeIn.setFromValue(0);
        fadeIn.setToValue(1);

        PauseTransition hold = new PauseTransition(HOLD);

        FadeTransition fadeOut = new FadeTransition(FADE_OUT, root);
        fadeOut.setFromValue(1);
        fadeOut.setToValue(0);

        SequentialTransition seq = new SequentialTransition(fadeIn, hold, fadeOut);
        seq.setOnFinished(e -> {
            popup.hide();
            if (current == popup) {
                current = null;
                currentAnim = null;
            }
        });

        current = popup;
        currentAnim = seq;
        seq.play();
    }
}
