package com.jeonbuk.repair;

import atlantafx.base.theme.CupertinoLight;
import com.jeonbuk.repair.util.DisplayPreferences;
import com.jeonbuk.repair.util.Fonts;
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
        // 번들된 Pretendard 폰트 등록 — 지인 PC 에 폰트가 설치돼있지 않아도 동일 룩 보장
        Fonts.registerAll();

        // 모던 테마 — Primer (GitHub 스타일). 다른 옵션: PrimerDark, NordLight, NordDark, CupertinoLight, CupertinoDark, Dracula
        Application.setUserAgentStylesheet(new CupertinoLight().getUserAgentStylesheet());

        // SessionFactory 부트스트랩 — DB 마이그레이션·시드 적용
        HibernateUtil.getSessionFactory();

        Parent root = ViewLoader.load("/fxml/main_view.fxml");
        Scene scene = new Scene(root);
        var css = getClass().getResource("/css/app.css");
        if (css != null) scene.getStylesheets().add(css.toExternalForm());

        // 사용자 표시 설정(글자 크기·테이블 줄 높이) — 마지막에 들어가 cascade override
        DisplayPreferences.apply(scene);

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
        // 레이아웃 스냅샷 모드 — `./gradlew run --args="snapshot"`
        if (args.length > 0 && "snapshot".equals(args[0])) {
            Application.launch(com.jeonbuk.repair.util.LayoutSnapshot.class, args);
            return;
        }
        launch(args);
    }
}
