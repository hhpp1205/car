package com.jeonbuk.repair.view;

import javafx.fxml.FXMLLoader;
import javafx.scene.Parent;

import java.io.IOException;
import java.net.URL;
import java.nio.charset.StandardCharsets;

/**
 * FXML 로더 헬퍼.
 */
public final class ViewLoader {

    private ViewLoader() {}

    public static Parent load(String fxmlPath) {
        try {
            URL url = ViewLoader.class.getResource(fxmlPath);
            if (url == null) throw new IllegalStateException("FXML 없음: " + fxmlPath);
            FXMLLoader loader = new FXMLLoader(url);
            loader.setCharset(StandardCharsets.UTF_8);
            return loader.load();
        } catch (IOException e) {
            throw new IllegalStateException("FXML 로딩 실패: " + fxmlPath, e);
        }
    }

    public static <T> Loaded<T> loadWithController(String fxmlPath) {
        try {
            URL url = ViewLoader.class.getResource(fxmlPath);
            if (url == null) throw new IllegalStateException("FXML 없음: " + fxmlPath);
            FXMLLoader loader = new FXMLLoader(url);
            loader.setCharset(StandardCharsets.UTF_8);
            Parent root = loader.load();
            return new Loaded<>(root, loader.getController());
        } catch (IOException e) {
            throw new IllegalStateException("FXML 로딩 실패: " + fxmlPath, e);
        }
    }

    public record Loaded<T>(Parent root, T controller) {}
}
