package com.jeonbuk.repair.controller;

import com.jeonbuk.repair.model.ClaimSide;
import com.jeonbuk.repair.model.CustomerIntake;
import com.jeonbuk.repair.model.InsuranceClaim;
import com.jeonbuk.repair.util.Formatters;
import javafx.beans.property.SimpleObjectProperty;
import javafx.beans.property.SimpleStringProperty;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.fxml.FXML;
import javafx.scene.control.Hyperlink;
import javafx.scene.control.Label;
import javafx.scene.control.TableCell;
import javafx.scene.control.TableColumn;
import javafx.scene.control.TableRow;
import javafx.scene.control.TableView;
import javafx.scene.input.KeyCode;
import javafx.scene.input.KeyEvent;
import javafx.stage.Stage;

import java.time.LocalDate;
import java.util.List;
import java.util.function.Consumer;

/**
 * 통계 행 더블클릭 → 해당 보험사의 청구 상세 목록을 모달로 표시.
 */
public class InsuranceClaimDetailController {

    @FXML private Label titleLabel;
    @FXML private Label subtitleLabel;
    @FXML private Label footerSummary;

    @FXML private TableView<InsuranceClaim> claimTable;
    @FXML private TableColumn<InsuranceClaim, String>    colIntakeNo;
    @FXML private TableColumn<InsuranceClaim, String>    colVehicleName;
    @FXML private TableColumn<InsuranceClaim, String>    colVehicleNo;
    @FXML private TableColumn<InsuranceClaim, String>    colSide;
    @FXML private TableColumn<InsuranceClaim, LocalDate> colClaimDate;
    @FXML private TableColumn<InsuranceClaim, Number>    colClaimAmount;
    @FXML private TableColumn<InsuranceClaim, Number>    colReceived;
    @FXML private TableColumn<InsuranceClaim, Number>    colOutstanding;
    @FXML private TableColumn<InsuranceClaim, LocalDate> colReceivedDate;
    @FXML private TableColumn<InsuranceClaim, String>    colMemo;

    private final ObservableList<InsuranceClaim> rows = FXCollections.observableArrayList();

    /** 입고번호 클릭 / 행 더블클릭 시 호출되는 콜백. 다이얼로그를 닫고 입/출고관리 화면으로 점프. */
    private Consumer<String> onIntakeNavigate;

    @FXML
    public void initialize() {
        configureTable();
    }

    /**
     * 다이얼로그를 띄우기 전에 호스트(통계 컨트롤러)가 호출.
     *
     * @param companyLabel    헤더 표시용 회사명 (예: "삼성화재", "(미지정)")
     * @param subtitle        서브타이틀 (필터 표시)
     * @param claims          표시할 청구 목록 (이미 정렬된 상태)
     * @param navigator       입고번호 클릭/행 더블클릭 시 호출. null 이면 점프 비활성.
     */
    public void load(String companyLabel, String subtitle,
                     List<InsuranceClaim> claims,
                     Consumer<String> navigator) {
        this.onIntakeNavigate = navigator;
        titleLabel.setText(companyLabel + " — " + claims.size() + "건");
        subtitleLabel.setText(subtitle);
        rows.setAll(claims);
        updateFooter(claims);
    }

    /** ESC 키로도 닫기 — Scene 부착 후 등록. */
    public void installEscToClose(Stage stage) {
        stage.getScene().addEventHandler(KeyEvent.KEY_PRESSED, e -> {
            if (e.getCode() == KeyCode.ESCAPE) {
                stage.close();
                e.consume();
            }
        });
    }

    @FXML
    private void onClose() {
        ((Stage) claimTable.getScene().getWindow()).close();
    }

    // ─── 테이블 구성 ────────────────────────────────────────────

    private void configureTable() {
        // 입고번호 — 하이퍼링크 셀 (단일 클릭 → 점프)
        colIntakeNo.setCellValueFactory(c -> new SimpleStringProperty(intakeNo(c.getValue())));
        colIntakeNo.setCellFactory(c -> intakeLinkCell());

        colVehicleName.setCellValueFactory(c -> new SimpleStringProperty(vehicleName(c.getValue())));
        colVehicleNo.setCellValueFactory(c -> new SimpleStringProperty(vehicleNo(c.getValue())));

        colSide.setCellValueFactory(c -> {
            ClaimSide s = c.getValue().getClaimSide();
            return new SimpleStringProperty(s == null ? "" : s.getLabel());
        });

        colClaimDate.setCellValueFactory(c -> new SimpleObjectProperty<>(c.getValue().getClaimDate()));
        colClaimDate.setCellFactory(c -> dateCell());

        colClaimAmount.setCellValueFactory(c -> new SimpleObjectProperty<>(nz(c.getValue().getClaimAmount())));
        colClaimAmount.setCellFactory(c -> moneyCell(false));

        colReceived.setCellValueFactory(c -> new SimpleObjectProperty<>(nz(c.getValue().getReceivedAmount())));
        colReceived.setCellFactory(c -> moneyCell(false));

        colOutstanding.setCellValueFactory(c -> new SimpleObjectProperty<>(outstanding(c.getValue())));
        colOutstanding.setCellFactory(c -> moneyCell(true));

        colReceivedDate.setCellValueFactory(c -> new SimpleObjectProperty<>(c.getValue().getReceivedDate()));
        colReceivedDate.setCellFactory(c -> receivedDateCell());

        colMemo.setCellValueFactory(c -> new SimpleStringProperty(
                c.getValue().getMemo() == null ? "" : c.getValue().getMemo()));

        claimTable.setItems(rows);

        // 행 더블클릭 → 점프 (입고번호 셀 클릭과 동일 동작)
        claimTable.setRowFactory(tv -> {
            TableRow<InsuranceClaim> row = new TableRow<>();
            row.setOnMouseClicked(e -> {
                if (e.getClickCount() == 2 && !row.isEmpty()) {
                    navigate(row.getItem());
                }
            });
            return row;
        });
    }

    private TableCell<InsuranceClaim, String> intakeLinkCell() {
        return new TableCell<>() {
            private final Hyperlink link = new Hyperlink();
            {
                link.setOnAction(e -> {
                    InsuranceClaim claim = getTableView().getItems().get(getIndex());
                    navigate(claim);
                });
            }

            @Override
            protected void updateItem(String item, boolean empty) {
                super.updateItem(item, empty);
                if (empty || item == null || item.isBlank()) {
                    setText(null);
                    setGraphic(null);
                } else {
                    link.setText(item);
                    setGraphic(link);
                    setText(null);
                }
            }
        };
    }

    private void navigate(InsuranceClaim claim) {
        if (claim == null) return;
        String no = intakeNo(claim);
        if (no == null || no.isBlank()) return;
        // 다이얼로그 먼저 닫고 → 점프 (closing 도중 stage 가 disposed 되어도 navigator 가 우선 실행되지 않도록 순서 주의)
        Consumer<String> nav = onIntakeNavigate;
        ((Stage) claimTable.getScene().getWindow()).close();
        if (nav != null) nav.accept(no);
    }

    private TableCell<InsuranceClaim, LocalDate> dateCell() {
        return new TableCell<>() {
            @Override
            protected void updateItem(LocalDate item, boolean empty) {
                super.updateItem(item, empty);
                setText(empty || item == null ? "" : Formatters.date(item));
            }
        };
    }

    private TableCell<InsuranceClaim, LocalDate> receivedDateCell() {
        return new TableCell<>() {
            @Override
            protected void updateItem(LocalDate item, boolean empty) {
                super.updateItem(item, empty);
                if (empty) { setText(""); setStyle(""); return; }
                if (item == null) {
                    setText("미수령");
                    setStyle("-fx-text-fill: #b91c1c; -fx-font-weight: bold;");
                } else {
                    setText(Formatters.date(item));
                    setStyle("-fx-text-fill: #15803d;");
                }
            }
        };
    }

    /** moneyCell — highlightOutstanding=true 인 컬럼은 0 초과일 때 빨간색. */
    private TableCell<InsuranceClaim, Number> moneyCell(boolean highlightOutstanding) {
        return new TableCell<>() {
            @Override
            protected void updateItem(Number item, boolean empty) {
                super.updateItem(item, empty);
                if (empty || item == null) { setText(""); setStyle(""); return; }
                long v = item.longValue();
                setText(Formatters.money((int) Math.min(Integer.MAX_VALUE, v)));
                if (highlightOutstanding && v > 0) {
                    setStyle("-fx-alignment: CENTER-RIGHT; -fx-text-fill: #b91c1c; -fx-font-weight: bold;");
                } else if (highlightOutstanding) {
                    setStyle("-fx-alignment: CENTER-RIGHT; -fx-text-fill: #15803d;");
                } else {
                    setStyle("-fx-alignment: CENTER-RIGHT;");
                }
            }
        };
    }

    private void updateFooter(List<InsuranceClaim> claims) {
        long claimSum    = claims.stream().mapToLong(c -> nz(c.getClaimAmount())).sum();
        long receivedSum = claims.stream().mapToLong(c -> nz(c.getReceivedAmount())).sum();
        long outstanding = Math.max(0L, claimSum - receivedSum);
        footerSummary.setText(String.format("청구액 합계: %s 원   |   수령액 합계: %s 원   |   미수금: %s 원",
                Formatters.money((int) Math.min(Integer.MAX_VALUE, claimSum)),
                Formatters.money((int) Math.min(Integer.MAX_VALUE, receivedSum)),
                Formatters.money((int) Math.min(Integer.MAX_VALUE, outstanding))));
    }

    // ─── helpers ───────────────────────────────────────────────

    private static String intakeNo(InsuranceClaim c) {
        CustomerIntake i = c.getIntake();
        return i == null ? "" : i.getIntakeNo();
    }

    private static String vehicleName(InsuranceClaim c) {
        CustomerIntake i = c.getIntake();
        return i == null || i.getVehicleName() == null ? "" : i.getVehicleName();
    }

    private static String vehicleNo(InsuranceClaim c) {
        CustomerIntake i = c.getIntake();
        return i == null || i.getVehicleNumber() == null ? "" : i.getVehicleNumber();
    }

    private static long nz(Integer v) {
        return v == null ? 0L : v;
    }

    private static long outstanding(InsuranceClaim c) {
        return Math.max(0L, nz(c.getClaimAmount()) - nz(c.getReceivedAmount()));
    }
}
