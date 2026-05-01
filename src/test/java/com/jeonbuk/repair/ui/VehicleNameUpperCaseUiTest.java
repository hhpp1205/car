package com.jeonbuk.repair.ui;

import javafx.scene.Scene;
import javafx.scene.control.TextField;
import javafx.scene.layout.VBox;
import javafx.stage.Stage;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.testfx.api.FxRobot;
import org.testfx.framework.junit5.ApplicationExtension;
import org.testfx.framework.junit5.Start;

import java.util.Locale;

import static org.junit.jupiter.api.Assertions.assertEquals;

/**
 * 차량명 영문 자동 대문자 변환 검증.
 *
 * <p>입고 폼의 차량명 칸은 포커스가 빠질 때 영문 소문자를 일괄 대문자로 변환한다
 * (CLAUDE.md "차량명 영문 입력 시 자동 대문자 변환"). 한글은 toUpperCase 영향이 없어
 * 그대로. 이 흐름이 깨지면 차량명 표기가 불일치해 검색·중복 판단이 어그러진다.
 *
 * <p>검증 방식: 컨트롤러 통째 (FXML/Service/DB) 를 띄우는 대신, 같은 focusedProperty
 * 리스너를 standalone Scene 에 재현. 컨트롤러 변경 시 본 테스트의 setup() 도 함께
 * 갱신할 것.
 *
 * @see com.jeonbuk.repair.controller.CustomerIntakeController#wireVehicleNameAutoComplete()
 *      (CustomerIntakeController.java:1456-1466 과 동일한 listener)
 */
@ExtendWith(ApplicationExtension.class)
class VehicleNameUpperCaseUiTest {

    private TextField vehicleNameField;
    private TextField otherField;

    @Start
    void start(Stage stage) {
        vehicleNameField = new TextField();
        vehicleNameField.setId("vehicleNameField");
        otherField = new TextField();
        otherField.setId("otherField");

        // ───── CustomerIntakeController.java:1456-1466 과 동일
        vehicleNameField.focusedProperty().addListener((obs, was, focused) -> {
            if (!focused) {
                String t = vehicleNameField.getText();
                if (t != null && !t.isEmpty()) {
                    String upper = t.toUpperCase(Locale.ROOT);
                    if (!upper.equals(t)) vehicleNameField.setText(upper);
                }
            }
        });

        VBox root = new VBox(8, vehicleNameField, otherField);
        stage.setScene(new Scene(root, 320, 120));
        stage.show();
    }

    @Test
    @DisplayName("영문 소문자 입력 후 포커스 이탈 → 자동 대문자")
    void english_lowercase_uppercased_on_focus_lost(FxRobot robot) {
        robot.clickOn(vehicleNameField).write("avante");

        // 포커스 이탈 트리거
        robot.clickOn(otherField);

        assertEquals("AVANTE", vehicleNameField.getText());
    }

    @Test
    @DisplayName("이미 대문자 — 멱등 (변화 없음)")
    void already_uppercase_stays_same(FxRobot robot) {
        robot.clickOn(vehicleNameField).write("AVANTE");
        robot.clickOn(otherField);

        assertEquals("AVANTE", vehicleNameField.getText());
    }

    @Test
    @DisplayName("한글 — toUpperCase 영향 없음")
    void korean_unchanged(FxRobot robot) {
        // robot.write 한글은 OS IME 의존이라 setText 로 직접 set 후 포커스 흐름만 시뮬레이션
        robot.interact(() -> vehicleNameField.setText("아반떼"));
        robot.clickOn(vehicleNameField);
        robot.clickOn(otherField);

        assertEquals("아반떼", vehicleNameField.getText());
    }

    @Test
    @DisplayName("영문+숫자 — 영문만 대문자")
    void english_with_digits_uppercased(FxRobot robot) {
        robot.clickOn(vehicleNameField).write("bmw3");
        robot.clickOn(otherField);

        assertEquals("BMW3", vehicleNameField.getText());
    }

    @Test
    @DisplayName("영문+한글 혼합 — 영문 부분만 대문자, 한글은 그대로")
    void mixed_only_english_uppercased(FxRobot robot) {
        robot.interact(() -> vehicleNameField.setText("kia 모닝"));
        robot.clickOn(vehicleNameField);
        robot.clickOn(otherField);

        assertEquals("KIA 모닝", vehicleNameField.getText());
    }

    @Test
    @DisplayName("빈 문자열 — 변화 없음")
    void empty_unchanged(FxRobot robot) {
        robot.clickOn(vehicleNameField);
        robot.clickOn(otherField);

        assertEquals("", vehicleNameField.getText());
    }

    @Test
    @DisplayName("포커스 유지 중에는 변환 안 됨 — 이탈 시점에만 동작")
    void no_uppercase_while_focused(FxRobot robot) {
        robot.clickOn(vehicleNameField).write("avante");

        // 포커스 유지 — 아직 변환 전
        assertEquals("avante", vehicleNameField.getText());
    }
}
