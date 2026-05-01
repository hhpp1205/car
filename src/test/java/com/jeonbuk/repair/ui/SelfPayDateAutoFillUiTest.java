package com.jeonbuk.repair.ui;

import javafx.scene.Scene;
import javafx.scene.control.DatePicker;
import javafx.scene.control.TextField;
import javafx.scene.layout.VBox;
import javafx.stage.Stage;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.testfx.api.FxRobot;
import org.testfx.framework.junit5.ApplicationExtension;
import org.testfx.framework.junit5.Start;

import java.time.LocalDate;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;

/**
 * 자기부담금 입력 시 자부담수납일 자동 채움 검증.
 *
 * <p>CLAUDE.md "Excel 에서 막혔던 부분 — 금액 입력 시 자동으로 오늘 날짜 못 채움" 의
 * 해결책. 자기부담금 칸이 빈 → 채워짐 으로 전이될 때 자부담수납일이 비어있으면
 * 자동으로 오늘 날짜로 채운다. 이미 사용자가 다른 날짜를 입력해뒀다면 덮어쓰지
 * 않는다 (사용자 의도 존중).
 *
 * <p>검증 방식: 컨트롤러의 textProperty listener 를 standalone Scene 에 재현.
 * 컨트롤러 변경 시 본 테스트 setup() 도 갱신할 것.
 *
 * @see com.jeonbuk.repair.controller.CustomerIntakeController
 *      (selfPayAmountField.textProperty().addListener — 동일 동작)
 */
@ExtendWith(ApplicationExtension.class)
class SelfPayDateAutoFillUiTest {

    private TextField selfPayAmountField;
    private DatePicker selfPayDatePicker;

    @Start
    void start(Stage stage) {
        selfPayAmountField = new TextField();
        selfPayAmountField.setId("selfPayAmountField");
        selfPayDatePicker = new DatePicker();
        selfPayDatePicker.setId("selfPayDatePicker");

        // ───── CustomerIntakeController 의 자부담수납일 자동 채움 listener 와 동일
        selfPayAmountField.textProperty().addListener((obs, oldV, newV) -> {
            boolean wasBlank = oldV == null || oldV.isBlank();
            boolean nowFilled = newV != null && !newV.isBlank();
            if (wasBlank && nowFilled && selfPayDatePicker.getValue() == null) {
                selfPayDatePicker.setValue(LocalDate.now());
            }
        });

        VBox root = new VBox(6, selfPayAmountField, selfPayDatePicker);
        stage.setScene(new Scene(root, 320, 120));
        stage.show();
    }

    @Test
    @DisplayName("초기 상태 — 자기부담금/수납일 모두 비어있음")
    void initial_state_both_empty(FxRobot robot) {
        assertEquals("", selfPayAmountField.getText());
        assertNull(selfPayDatePicker.getValue());
    }

    @Test
    @DisplayName("자기부담금 입력 → 자부담수납일이 오늘 날짜로 자동 채워짐")
    void typing_amount_auto_fills_today(FxRobot robot) {
        robot.clickOn(selfPayAmountField).write("300000");

        assertEquals(LocalDate.now(), selfPayDatePicker.getValue());
    }

    @Test
    @DisplayName("이미 수납일이 다른 날짜로 채워져 있으면 덮어쓰지 않음")
    void preexisting_date_is_preserved(FxRobot robot) {
        LocalDate yesterday = LocalDate.now().minusDays(1);
        robot.interact(() -> selfPayDatePicker.setValue(yesterday));

        robot.clickOn(selfPayAmountField).write("300000");

        assertEquals(yesterday, selfPayDatePicker.getValue());
    }

    @Test
    @DisplayName("자기부담금 입력 후 지워도 한 번 채워진 수납일은 유지")
    void clearing_amount_keeps_date(FxRobot robot) {
        robot.clickOn(selfPayAmountField).write("300000");
        LocalDate filled = selfPayDatePicker.getValue();

        robot.interact(() -> selfPayAmountField.clear());

        assertEquals(filled, selfPayDatePicker.getValue());
    }

    @Test
    @DisplayName("기존 금액에 추가 입력 — 수납일이 갱신되지 않음 (멱등)")
    void appending_to_existing_amount_does_not_overwrite_date(FxRobot robot) {
        // 첫 입력 → 수납일 자동 채움
        robot.clickOn(selfPayAmountField).write("100000");
        LocalDate firstFill = selfPayDatePicker.getValue();
        assertEquals(LocalDate.now(), firstFill);

        // 수납일을 다른 날짜로 수동 변경 (사용자가 정정한 상황)
        LocalDate manuallySet = LocalDate.now().minusDays(3);
        robot.interact(() -> selfPayDatePicker.setValue(manuallySet));

        // 자기부담금 추가 입력 — 수납일은 그대로 유지되어야 함
        robot.clickOn(selfPayAmountField).write("0");

        assertEquals(manuallySet, selfPayDatePicker.getValue());
    }

    @Test
    @DisplayName("값을 setText 로 직접 set 해도 동일 동작")
    void setText_triggers_auto_fill(FxRobot robot) {
        robot.interact(() -> selfPayAmountField.setText("500000"));

        assertEquals(LocalDate.now(), selfPayDatePicker.getValue());
    }
}
