package com.jeonbuk.repair;

import com.jeonbuk.repair.util.HibernateUtil;
import com.jeonbuk.repair.view.ViewLoader;
import javafx.application.Application;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.stage.Stage;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class Main extends Application {

    private static final Logger log = LoggerFactory.getLogger(Main.class);

    @Override
    public void start(Stage stage) {
        // SessionFactory 부트스트랩 — DB 마이그레이션·시드 적용
        HibernateUtil.getSessionFactory();

        Parent root = ViewLoader.load("/fxml/main_view.fxml");
        Scene scene = new Scene(root);
        var css = getClass().getResource("/css/app.css");
        if (css != null) scene.getStylesheets().add(css.toExternalForm());

        stage.setTitle("전북공업사 통합 관리 시스템");
        stage.setScene(scene);
        stage.setOnCloseRequest(e -> HibernateUtil.shutdown());
        stage.show();
        log.info("애플리케이션 기동 완료");
    }

    @Override
    public void stop() {
        HibernateUtil.shutdown();
    }

    public static void main(String[] args) {
        launch(args);
    }
}
