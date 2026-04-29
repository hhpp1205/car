package com.jeonbuk.repair.util;

import atlantafx.base.theme.CupertinoLight;
import com.jeonbuk.repair.view.ViewLoader;
import javafx.animation.PauseTransition;
import javafx.application.Application;
import javafx.application.Platform;
import javafx.embed.swing.SwingFXUtils;
import javafx.scene.Node;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.scene.control.Button;
import javafx.scene.image.WritableImage;
import javafx.stage.Stage;
import javafx.util.Duration;

import javax.imageio.ImageIO;
import java.io.File;
import java.nio.file.Files;
import java.nio.file.Path;

/**
 * 레이아웃 점검용 스냅샷 도구. 입/출고관리 폼이 표시된 상태의 PNG 를 저장.
 *
 * <p>실행: {@code ./gradlew snapshot} → {@code build/snapshots/layout.png}
 *
 * <p>임시 DB 디렉터리를 사용하므로 사용자 영역의 repair.db 에 영향을 주지 않는다.
 */
public class LayoutSnapshot extends Application {

    private static final int WIDTH  = 1600;
    private static final int HEIGHT = 900;
    private static final String OUT_PATH = "build/snapshots/layout.png";

    public static void main(String[] args) {
        launch(args);
    }

    @Override
    public void start(Stage stage) throws Exception {
        // 임시 DB — 사용자 데이터 영향 없음
        Path tmpDir = Files.createTempDirectory("jeonbuk_snapshot");
        String dbFile = tmpDir.resolve("repair.db").toString();
        HibernateUtil.replaceWithDatabase(dbFile);

        Fonts.registerAll();
        Application.setUserAgentStylesheet(new CupertinoLight().getUserAgentStylesheet());

        Parent root = ViewLoader.load("/fxml/main_view.fxml");
        Scene scene = new Scene(root, WIDTH, HEIGHT);
        var css = getClass().getResource("/css/app.css");
        if (css != null) scene.getStylesheets().add(css.toExternalForm());

        stage.setScene(scene);
        stage.setWidth(WIDTH);
        stage.setHeight(HEIGHT);
        stage.setTitle("Layout Snapshot");
        stage.show();

        // 1) 입/출고관리 nav 클릭 → 2) 신규 클릭 → 3) 레이아웃 안정화 → 4) 스냅샷
        Platform.runLater(() -> {
            Node navIntake = scene.lookup("#navIntake");
            if (navIntake instanceof Button b) b.fire();

            Platform.runLater(() -> {
                Button newBtn = findButton(scene.getRoot(), "신규");
                if (newBtn != null) newBtn.fire();

                PauseTransition pause = new PauseTransition(Duration.millis(500));
                pause.setOnFinished(e -> {
                    saveSnapshot(scene);
                    Platform.exit();
                });
                pause.play();
            });
        });
    }

    private static void saveSnapshot(Scene scene) {
        WritableImage img = scene.snapshot(null);
        File out = new File(OUT_PATH);
        out.getParentFile().mkdirs();
        try {
            ImageIO.write(SwingFXUtils.fromFXImage(img, null), "png", out);
            System.out.println("Snapshot saved: " + out.getAbsolutePath());
        } catch (Exception ex) {
            ex.printStackTrace();
        }
    }

    /** 자식 트리에서 주어진 텍스트의 Button 을 찾는다 (DFS). */
    private static Button findButton(Parent parent, String text) {
        for (Node n : parent.getChildrenUnmodifiable()) {
            if (n instanceof Button b && text.equals(b.getText())) return b;
            if (n instanceof Parent p) {
                Button b = findButton(p, text);
                if (b != null) return b;
            }
        }
        return null;
    }
}
