package com.jeonbuk.repair.ui;

import com.jeonbuk.repair.util.Formatters;
import javafx.scene.Scene;
import javafx.scene.control.TextField;
import javafx.scene.layout.StackPane;
import javafx.stage.Stage;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.testfx.api.FxRobot;
import org.testfx.framework.junit5.ApplicationExtension;
import org.testfx.framework.junit5.Start;

import static org.junit.jupiter.api.Assertions.assertEquals;

/**
 * TestFX 셋업 검증 — 입력 즉시 포매팅 listener 가 UI 단까지 통합되어 동작하는지 확인.
 *
 * <p>입고 폼의 전화번호 칸에는 {@link Formatters#formatPhoneAsType} 가 textProperty 리스너로
 * 붙는다 (CustomerIntakeController). 이 테스트는 그 리스너가 실제 키 입력 → 화면 텍스트
 * 반영까지 끊김 없이 동작하는지를 격리된 작은 Scene 으로 검증한다.
 *
 * <p>실행 시 잠깐 창이 뜬다 (headed 모드). CI 에서는 xvfb 가상 디스플레이로 동작.
 */
@ExtendWith(ApplicationExtension.class)
class PhoneFieldFormatterUiTest {

    private TextField phoneField;

    @Start
    void start(Stage stage) {
        phoneField = new TextField();
        phoneField.setId("phoneField");
        phoneField.textProperty().addListener((obs, old, val) -> {
            String formatted = Formatters.formatPhoneAsType(val);
            if (!formatted.equals(val)) {
                phoneField.setText(formatted);
            }
        });
        stage.setScene(new Scene(new StackPane(phoneField), 240, 80));
        stage.show();
    }

    @Test
    @DisplayName("11자리 숫자 타이핑 시 010-1234-5678 형식으로 자동 포매팅")
    void typing_eleven_digits_formats_as_phone(FxRobot robot) {
        robot.clickOn("#phoneField").write("01012345678");

        assertEquals("010-1234-5678", phoneField.getText());
    }
}
