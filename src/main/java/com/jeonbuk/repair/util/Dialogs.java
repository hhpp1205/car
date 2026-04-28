package com.jeonbuk.repair.util;

import javafx.scene.control.Alert;
import javafx.scene.control.Alert.AlertType;
import javafx.scene.control.ButtonType;

import java.util.Optional;

/**
 * 간단한 알림/확인 대화상자.
 */
public final class Dialogs {

    private Dialogs() {}

    public static void info(String title, String content) {
        show(AlertType.INFORMATION, title, content);
    }

    public static void warn(String title, String content) {
        show(AlertType.WARNING, title, content);
    }

    public static void error(String title, String content) {
        show(AlertType.ERROR, title, content);
    }

    public static boolean confirm(String title, String content) {
        Alert a = new Alert(AlertType.CONFIRMATION, content, ButtonType.OK, ButtonType.CANCEL);
        a.setHeaderText(null);
        a.setTitle(title);
        Optional<ButtonType> res = a.showAndWait();
        return res.isPresent() && res.get() == ButtonType.OK;
    }

    private static void show(AlertType type, String title, String content) {
        Alert a = new Alert(type, content, ButtonType.OK);
        a.setHeaderText(null);
        a.setTitle(title);
        a.showAndWait();
    }
}
