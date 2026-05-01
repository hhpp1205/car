package com.jeonbuk.repair.ui;

import javafx.scene.Scene;
import javafx.scene.control.CheckBox;
import javafx.scene.control.TextField;
import javafx.scene.layout.VBox;
import javafx.stage.Stage;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.testfx.api.FxRobot;
import org.testfx.framework.junit5.ApplicationExtension;
import org.testfx.framework.junit5.Start;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * 자기부담금 입력 칸의 styleClass 토글 검증.
 *
 * <p>CLAUDE.md "자차/일반수리 시 자기부담금 칸 색상 변경" — 사고유형이 자차 또는
 * 일반수리일 때만 손님이 직접 결제할 자기부담금이 발생하므로, 해당 입력칸을
 * amber 톤으로 강조해 입력 누락을 막는다. 색상 자체는 CSS 의 `self-pay-active`
 * 클래스 (app.css) 에서 정의.
 *
 * <p>검증 방식: 컨트롤러의 listener/헬퍼 동작을 standalone Scene 에 재현.
 * 컨트롤러 변경 시 본 테스트 setup() 도 갱신할 것.
 *
 * @see com.jeonbuk.repair.controller.CustomerIntakeController#updateSelfPayHighlight()
 *      (CustomerIntakeController.java 의 updateSelfPayHighlight 와 동일 동작)
 */
@ExtendWith(ApplicationExtension.class)
class SelfPayHighlightUiTest {

    private static final String ACTIVE = "self-pay-active";

    private CheckBox cbSelf, cbOpponent, cbGeneral, cbFault;
    private TextField selfPayAmountField;

    @Start
    void start(Stage stage) {
        cbSelf     = id(new CheckBox("자차"),     "cbSelf");
        cbOpponent = id(new CheckBox("대물"),     "cbOpponent");
        cbGeneral  = id(new CheckBox("일반수리"), "cbGeneral");
        cbFault    = id(new CheckBox("과실"),     "cbFault");
        selfPayAmountField = id(new TextField(), "selfPayAmountField");

        // ───── CustomerIntakeController.updateSelfPayHighlight 와 동일 동작 재현
        Runnable update = () -> {
            boolean active = cbSelf.isSelected() || cbGeneral.isSelected();
            if (active) {
                if (!selfPayAmountField.getStyleClass().contains(ACTIVE)) {
                    selfPayAmountField.getStyleClass().add(ACTIVE);
                }
            } else {
                selfPayAmountField.getStyleClass().remove(ACTIVE);
            }
        };
        cbSelf.selectedProperty().addListener((obs, o, n) -> update.run());
        cbGeneral.selectedProperty().addListener((obs, o, n) -> update.run());

        VBox root = new VBox(6,
                cbSelf, cbOpponent, cbGeneral, cbFault, selfPayAmountField);
        stage.setScene(new Scene(root, 320, 200));
        stage.show();
    }

    private static <T extends javafx.scene.Node> T id(T node, String id) {
        node.setId(id);
        return node;
    }

    @Test
    @DisplayName("초기 상태 — self-pay-active 클래스 없음")
    void initial_state_no_active_class(FxRobot robot) {
        assertFalse(selfPayAmountField.getStyleClass().contains(ACTIVE));
    }

    @Test
    @DisplayName("자차 체크 → self-pay-active 부착")
    void self_checked_adds_active_class(FxRobot robot) {
        robot.clickOn(cbSelf);

        assertTrue(selfPayAmountField.getStyleClass().contains(ACTIVE));
    }

    @Test
    @DisplayName("자차 해제 → self-pay-active 제거")
    void self_unchecked_removes_active_class(FxRobot robot) {
        robot.clickOn(cbSelf);
        robot.clickOn(cbSelf);

        assertFalse(selfPayAmountField.getStyleClass().contains(ACTIVE));
    }

    @Test
    @DisplayName("일반수리 체크 → self-pay-active 부착")
    void general_checked_adds_active_class(FxRobot robot) {
        robot.clickOn(cbGeneral);

        assertTrue(selfPayAmountField.getStyleClass().contains(ACTIVE));
    }

    @Test
    @DisplayName("자차+일반수리 동시 체크 — 클래스가 중복 추가되지 않음")
    void no_duplicate_class_when_both_checked(FxRobot robot) {
        robot.clickOn(cbSelf);
        robot.clickOn(cbGeneral);

        long count = selfPayAmountField.getStyleClass().stream()
                .filter(ACTIVE::equals)
                .count();
        assertEquals(1, count);
    }

    @Test
    @DisplayName("자차 해제했지만 일반수리는 켜진 상태 → 강조 유지")
    void general_alone_keeps_active_after_self_unchecked(FxRobot robot) {
        robot.clickOn(cbSelf);
        robot.clickOn(cbGeneral);
        robot.clickOn(cbSelf);   // 자차만 해제

        assertTrue(selfPayAmountField.getStyleClass().contains(ACTIVE));
    }

    @Test
    @DisplayName("대물/과실은 자기부담금 강조에 영향 없음")
    void opponent_and_fault_do_not_affect_highlight(FxRobot robot) {
        robot.clickOn(cbOpponent);
        robot.clickOn(cbFault);

        assertFalse(selfPayAmountField.getStyleClass().contains(ACTIVE));
    }
}
