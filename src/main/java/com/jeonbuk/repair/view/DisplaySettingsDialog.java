package com.jeonbuk.repair.view;

import com.jeonbuk.repair.util.DisplayPreferences;
import com.jeonbuk.repair.util.DisplayPreferences.FontSize;
import com.jeonbuk.repair.util.DisplayPreferences.RowHeight;
import javafx.collections.FXCollections;
import javafx.geometry.Insets;
import javafx.scene.Scene;
import javafx.scene.control.Button;
import javafx.scene.control.ChoiceBox;
import javafx.scene.control.Label;
import javafx.scene.layout.HBox;
import javafx.scene.layout.Priority;
import javafx.scene.layout.Region;
import javafx.scene.layout.VBox;
import javafx.stage.Modality;
import javafx.stage.Stage;
import javafx.stage.Window;
import javafx.util.StringConverter;

/**
 * [표시 설정] 모달 다이얼로그 — 글자 크기와 테이블 줄 높이 두 가지를 ChoiceBox 로 노출.
 * 변경 즉시 메인 Scene 에 적용되고 다음 실행에도 유지됨(Preferences).
 */
public final class DisplaySettingsDialog {

    private DisplaySettingsDialog() {}

    public static void show(Window owner) {
        Stage stage = new Stage();
        stage.initOwner(owner);
        stage.initModality(Modality.APPLICATION_MODAL);
        stage.setTitle("표시 설정");
        stage.setResizable(false);

        ChoiceBox<FontSize>  fontChoice = new ChoiceBox<>(FXCollections.observableArrayList(FontSize.values()));
        fontChoice.setConverter(new StringConverter<>() {
            @Override public String toString(FontSize fs) {
                return fs == null ? "" : fs.label + "  (" + fs.rootPx + "px)";
            }
            @Override public FontSize fromString(String s) { return null; }
        });
        fontChoice.setValue(DisplayPreferences.getFontSize());
        fontChoice.setMaxWidth(Double.MAX_VALUE);

        ChoiceBox<RowHeight> rowChoice = new ChoiceBox<>(FXCollections.observableArrayList(RowHeight.values()));
        rowChoice.setConverter(new StringConverter<>() {
            @Override public String toString(RowHeight rh) {
                return rh == null ? "" : rh.label + "  (" + rh.px + "px)";
            }
            @Override public RowHeight fromString(String s) { return null; }
        });
        rowChoice.setValue(DisplayPreferences.getRowHeight());
        rowChoice.setMaxWidth(Double.MAX_VALUE);

        // 변경 즉시 메인 Scene 에 적용
        fontChoice.valueProperty().addListener((obs, o, n) -> {
            DisplayPreferences.setFontSize(n);
            if (owner != null && owner.getScene() != null) DisplayPreferences.apply(owner.getScene());
        });
        rowChoice.valueProperty().addListener((obs, o, n) -> {
            DisplayPreferences.setRowHeight(n);
            if (owner != null && owner.getScene() != null) DisplayPreferences.apply(owner.getScene());
        });

        Button reset = new Button("기본값으로");
        reset.setOnAction(e -> {
            fontChoice.setValue(FontSize.NORMAL);
            rowChoice.setValue(RowHeight.COMPACT);
        });

        Button close = new Button("닫기");
        close.getStyleClass().add("primary");
        close.setDefaultButton(true);
        close.setOnAction(e -> stage.close());

        Label fontLabel = new Label("글자 크기");
        fontLabel.getStyleClass().add("section-title");
        Label rowLabel  = new Label("테이블 줄 높이");
        rowLabel.getStyleClass().add("section-title");
        Label hint = new Label("변경 사항은 즉시 반영되며 다음 실행에도 유지됩니다.");
        hint.getStyleClass().add("hint");
        hint.setWrapText(true);

        Region spacer = new Region();
        HBox.setHgrow(spacer, Priority.ALWAYS);
        HBox bottomBar = new HBox(8, reset, spacer, close);
        bottomBar.setPadding(new Insets(12, 0, 0, 0));

        VBox root = new VBox(8,
                fontLabel, fontChoice,
                rowLabel,  rowChoice,
                hint,
                bottomBar);
        root.setPadding(new Insets(20));
        root.setPrefWidth(360);

        Scene scene = new Scene(root);
        if (owner != null && owner.getScene() != null) {
            scene.getStylesheets().addAll(owner.getScene().getStylesheets());
        }
        stage.setScene(scene);
        stage.showAndWait();
    }
}
