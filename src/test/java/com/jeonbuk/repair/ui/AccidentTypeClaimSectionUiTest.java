package com.jeonbuk.repair.ui;

import javafx.scene.Scene;
import javafx.scene.control.CheckBox;
import javafx.scene.layout.GridPane;
import javafx.scene.layout.VBox;
import javafx.stage.Stage;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.testfx.api.FxRobot;
import org.testfx.framework.junit5.ApplicationExtension;
import org.testfx.framework.junit5.Start;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * 사고유형 체크박스 → 보험 청구 섹션 자동 활성화 검증.
 *
 * <p>입고 폼의 핵심 자동화 (CLAUDE.md "사고유형 선택 시 보험청구 입력칸 자동 활성화")
 * — 사용자가 자차/대물 체크 시 굳이 청구 섹션 활성화를 따로 누르지 않아도
 * 입력 가능 상태가 된다. 이 흐름이 깨지면 사용자는 매번 두 번 클릭해야 한다.
 *
 * <p><b>검증 방식</b>: 컨트롤러 통째 (FXML/Service/DB) 를 띄우는 대신,
 * 같은 binding/listener 코드를 standalone Scene 에 재현. 가볍고 빠르며 핵심
 * 동작에 집중. 컨트롤러 수정 시 본 테스트의 setup() 도 함께 갱신해야 한다.
 *
 * @see com.jeonbuk.repair.controller.CustomerIntakeController#initialize()
 *      (CustomerIntakeController.java:626-639 와 동일한 binding/listener 부착)
 */
@ExtendWith(ApplicationExtension.class)
class AccidentTypeClaimSectionUiTest {

    private CheckBox cbSelf, cbOpponent, cbGeneral, cbFault;
    private CheckBox useOwnClaimCheck, useOpponentClaimCheck;
    private GridPane ownClaimGrid, opponentClaimGrid;

    @Start
    void start(Stage stage) {
        // 사고유형 4개 (자차/대물/일반수리/과실) — CLAUDE.md 도메인 정의 그대로
        cbSelf     = id(new CheckBox("자차"),     "cbSelf");
        cbOpponent = id(new CheckBox("대물"),     "cbOpponent");
        cbGeneral  = id(new CheckBox("일반수리"), "cbGeneral");
        cbFault    = id(new CheckBox("과실"),     "cbFault");

        // 청구 섹션 활성화 토글 (사용자가 직접 끄고 켤 수도 있는 별도 컨트롤)
        useOwnClaimCheck      = id(new CheckBox("자차 청구"), "useOwnClaimCheck");
        useOpponentClaimCheck = id(new CheckBox("상대 청구"), "useOpponentClaimCheck");

        // 청구 섹션 자체 — 실제 컨트롤러는 GridPane 에 입력 필드들이 들어있다
        ownClaimGrid      = id(new GridPane(), "ownClaimGrid");
        opponentClaimGrid = id(new GridPane(), "opponentClaimGrid");

        // ───── CustomerIntakeController.java:628-630 와 동일 — 청구 토글이 섹션 disable 결정
        ownClaimGrid.disableProperty()
                .bind(useOwnClaimCheck.selectedProperty().not());
        opponentClaimGrid.disableProperty()
                .bind(useOpponentClaimCheck.selectedProperty().not());

        // ───── CustomerIntakeController.java:634-639 와 동일 — 사고유형 체크 시 청구 토글 자동 ON
        cbSelf.selectedProperty().addListener((obs, o, n) -> {
            if (n) useOwnClaimCheck.setSelected(true);
        });
        cbOpponent.selectedProperty().addListener((obs, o, n) -> {
            if (n) useOpponentClaimCheck.setSelected(true);
        });

        VBox root = new VBox(6,
                cbSelf, cbOpponent, cbGeneral, cbFault,
                useOwnClaimCheck, ownClaimGrid,
                useOpponentClaimCheck, opponentClaimGrid);
        stage.setScene(new Scene(root, 320, 400));
        stage.show();
    }

    private static <T extends javafx.scene.Node> T id(T node, String id) {
        node.setId(id);
        return node;
    }

    @Test
    @DisplayName("초기 상태 — 두 청구 섹션 모두 disabled")
    void initial_state_both_sections_disabled(FxRobot robot) {
        assertTrue(ownClaimGrid.isDisabled());
        assertTrue(opponentClaimGrid.isDisabled());
        assertFalse(useOwnClaimCheck.isSelected());
        assertFalse(useOpponentClaimCheck.isSelected());
    }

    @Test
    @DisplayName("자차 체크 → 자차 청구 섹션 enable, 상대 섹션은 disabled 유지")
    void self_checked_enables_own_section_only(FxRobot robot) {
        robot.clickOn("#cbSelf");

        assertTrue(useOwnClaimCheck.isSelected());
        assertFalse(ownClaimGrid.isDisabled());
        // 상대 청구는 영향 없음
        assertFalse(useOpponentClaimCheck.isSelected());
        assertTrue(opponentClaimGrid.isDisabled());
    }

    @Test
    @DisplayName("대물 체크 → 상대 청구 섹션 enable, 자차 섹션은 disabled 유지")
    void opponent_checked_enables_opponent_section_only(FxRobot robot) {
        robot.clickOn("#cbOpponent");

        assertTrue(useOpponentClaimCheck.isSelected());
        assertFalse(opponentClaimGrid.isDisabled());
        assertFalse(useOwnClaimCheck.isSelected());
        assertTrue(ownClaimGrid.isDisabled());
    }

    @Test
    @DisplayName("자차+대물 동시 체크 → 두 청구 섹션 모두 enable")
    void both_checked_enables_both_sections(FxRobot robot) {
        // CSS selector lookup 보다 Node 직접 인자가 좌표 계산이 안정적 — 연속 클릭에선 후자 사용.
        robot.clickOn(cbSelf);
        robot.clickOn(cbOpponent);

        assertFalse(ownClaimGrid.isDisabled());
        assertFalse(opponentClaimGrid.isDisabled());
    }

    @Test
    @DisplayName("일반수리/과실은 청구 섹션에 영향 없음")
    void general_and_fault_do_not_affect_claim_sections(FxRobot robot) {
        robot.clickOn("#cbGeneral");
        robot.clickOn("#cbFault");

        assertFalse(useOwnClaimCheck.isSelected());
        assertFalse(useOpponentClaimCheck.isSelected());
        assertTrue(ownClaimGrid.isDisabled());
        assertTrue(opponentClaimGrid.isDisabled());
    }

    @Test
    @DisplayName("자차 체크 후 해제해도 청구 섹션은 enable 유지 — 사용자가 명시적으로 끄지 않는 한 닫히지 않음")
    void unchecking_self_keeps_own_section_open(FxRobot robot) {
        robot.clickOn("#cbSelf");   // ON → 자차 청구 섹션 enable
        robot.clickOn("#cbSelf");   // OFF

        // listener 는 'true 로 바뀔 때만' 동작. 해제는 자동 영향 없음.
        // (사용자가 자차 체크를 잘못 눌렀다 풀어도, 이미 입력한 청구 데이터를
        //  실수로 disable 시켜 잃지 않도록 한 의도된 동작)
        assertTrue(useOwnClaimCheck.isSelected());
        assertFalse(ownClaimGrid.isDisabled());
    }

    @Test
    @DisplayName("청구 토글을 사용자가 직접 끄면 섹션이 disable 됨 — bind 동작 확인")
    void manually_unchecking_claim_toggle_disables_section(FxRobot robot) {
        robot.clickOn("#cbSelf");                  // 자동으로 useOwnClaimCheck ON
        assertFalse(ownClaimGrid.isDisabled());

        robot.clickOn("#useOwnClaimCheck");        // 사용자가 직접 청구 토글 OFF

        assertFalse(useOwnClaimCheck.isSelected());
        assertTrue(ownClaimGrid.isDisabled());
    }
}
