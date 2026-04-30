package com.jeonbuk.repair.controller;

import com.jeonbuk.repair.model.CustomerIntake;
import com.jeonbuk.repair.model.ProgressStatus;
import com.jeonbuk.repair.service.InsuranceClaimService;
import com.jeonbuk.repair.service.ServiceRegistry;
import com.jeonbuk.repair.util.Formatters;
import javafx.beans.property.SimpleObjectProperty;
import javafx.beans.property.SimpleStringProperty;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.fxml.FXML;
import javafx.scene.control.CheckBox;
import javafx.scene.control.Label;
import javafx.scene.control.TableCell;
import javafx.scene.control.TableColumn;
import javafx.scene.control.TableView;

import java.time.LocalDate;
import java.util.List;

public class DashboardController {

    /** 카드 클릭 시 어느 화면+필터로 점프할지 — MainController 가 주입. */
    public interface DashboardNavigator {
        void goToInProgressIntakes();
        void goToOutstandingIntakes();
        void goToActiveRentals();
        void goToOverdueIntakes();
    }

    private DashboardNavigator navigator;

    private static final int RECENT_LIMIT = 12;

    @FXML private CheckBox includeClosedCheck;
    @FXML private Label inProgressValue;
    @FXML private Label inProgressFootnote;
    @FXML private Label outstandingValue;
    @FXML private Label outstandingFootnote;
    @FXML private Label rentalActiveValue;
    @FXML private Label rentalActiveFootnote;
    @FXML private Label overdueValue;
    @FXML private Label overdueFootnote;

    @FXML private TableView<CustomerIntake> recentTable;
    @FXML private TableColumn<CustomerIntake, String>    colIntakeNo;
    @FXML private TableColumn<CustomerIntake, LocalDate> colIntakeDate;
    @FXML private TableColumn<CustomerIntake, String>    colVehicleName;
    @FXML private TableColumn<CustomerIntake, String>    colVehicleNo;
    @FXML private TableColumn<CustomerIntake, String>    colRepairType;
    @FXML private TableColumn<CustomerIntake, String>    colStage;
    @FXML private TableColumn<CustomerIntake, LocalDate> colReleaseDate;

    private final ObservableList<CustomerIntake> recentRows = FXCollections.observableArrayList();

    @FXML
    public void initialize() {
        configureRecentTable();
    }

    @FXML
    private void onRefresh() {
        refresh();
    }

    public void setNavigator(DashboardNavigator navigator) {
        this.navigator = navigator;
    }

    @FXML
    private void onInProgressCardClicked() {
        if (navigator != null) navigator.goToInProgressIntakes();
    }

    @FXML
    private void onOutstandingCardClicked() {
        if (navigator != null) navigator.goToOutstandingIntakes();
    }

    @FXML
    private void onActiveRentalCardClicked() {
        if (navigator != null) navigator.goToActiveRentals();
    }

    @FXML
    private void onOverdueCardClicked() {
        if (navigator != null) navigator.goToOverdueIntakes();
    }

    /** 사이드바에서 대시보드로 진입할 때마다 호출 — DB 변경사항 반영. */
    public void refresh() {
        // "종결 건 포함" 체크 — 기본값 true. 미체크 시 종결된 입고와 그 청구를 집계에서 제외.
        boolean includeClosed = includeClosedCheck == null || includeClosedCheck.isSelected();

        ServiceRegistry reg = ServiceRegistry.get();

        // 진행 중 입고 — 출고일 비어있는 건수
        long inProgress = reg.intakeRepo().countInProgress(includeClosed);
        inProgressValue.setText(String.valueOf(inProgress));

        // 미수금 — claim_amount - received_amount 의 합계 (음수 클램프, DB 에서 처리)
        long outstanding = reg.claimRepo().sumOutstanding(includeClosed);
        outstandingValue.setText(Formatters.money((int) Math.min(Integer.MAX_VALUE, outstanding)) + " 원");

        // 대차 중 — 종결 토글과 무관 (대차는 종료일로 판정)
        long active = reg.rentalRepo().countActive();
        rentalActiveValue.setText(String.valueOf(active));

        // 30일 경과 미수령 — DB 에서 임계 날짜로 비교
        LocalDate overdueThreshold = LocalDate.now().minusDays(InsuranceClaimService.OVERDUE_DAYS);
        long overdue = reg.claimRepo().countOverdue(includeClosed, overdueThreshold);
        overdueValue.setText(String.valueOf(overdue));
        overdueFootnote.setText(overdue > 0
                ? "확인 필요"
                : "청구 후 30일+ 경과");

        // 최근 입고 — claims fetch join 으로 진행상태 산출 시 N+1 회피
        List<CustomerIntake> recent = reg.intakeRepo().findRecent(RECENT_LIMIT, includeClosed);
        recentRows.setAll(recent);
    }

    private void configureRecentTable() {
        colIntakeNo.setCellValueFactory(c -> new SimpleStringProperty(c.getValue().getIntakeNo()));
        colIntakeDate.setCellValueFactory(c -> new SimpleObjectProperty<>(c.getValue().getIntakeDate()));
        colIntakeDate.setCellFactory(c -> dateCell());
        colVehicleName.setCellValueFactory(c -> new SimpleStringProperty(c.getValue().getVehicleName()));
        colVehicleNo.setCellValueFactory(c -> new SimpleStringProperty(c.getValue().getVehicleNumber()));
        colRepairType.setCellValueFactory(c -> new SimpleStringProperty(
                c.getValue().getRepairType() == null ? "" : c.getValue().getRepairType().getLabel()));
        colStage.setCellValueFactory(c -> {
            ProgressStatus s = ServiceRegistry.get().intakeService().computeStatus(c.getValue());
            return new SimpleStringProperty(s == null ? "" : s.getLabel());
        });
        colStage.setCellFactory(c -> stageCell());
        colReleaseDate.setCellValueFactory(c -> new SimpleObjectProperty<>(c.getValue().getReleaseDate()));
        colReleaseDate.setCellFactory(c -> dateCell());

        recentTable.setItems(recentRows);
    }

    private TableCell<CustomerIntake, LocalDate> dateCell() {
        return new TableCell<>() {
            @Override
            protected void updateItem(LocalDate item, boolean empty) {
                super.updateItem(item, empty);
                setText(empty || item == null ? "" : Formatters.date(item));
            }
        };
    }

    private TableCell<CustomerIntake, String> stageCell() {
        return new TableCell<>() {
            private final Label badge = new Label();
            { badge.getStyleClass().add("status-badge"); }

            @Override
            protected void updateItem(String item, boolean empty) {
                super.updateItem(item, empty);
                if (empty || item == null || item.isEmpty()) {
                    setGraphic(null);
                    setText(null);
                    return;
                }
                badge.setText(item);
                badge.getStyleClass().removeAll(
                        "badge-repairing", "badge-released", "badge-claimed",
                        "badge-settled", "badge-closed");
                switch (item) {
                    case "수리중"     -> badge.getStyleClass().add("badge-repairing");
                    case "출고완료"   -> badge.getStyleClass().add("badge-released");
                    case "청구완료"   -> badge.getStyleClass().add("badge-claimed");
                    case "수령완료"   -> badge.getStyleClass().add("badge-settled");
                    case "종결"       -> badge.getStyleClass().add("badge-closed");
                    default -> { /* no-op */ }
                }
                setGraphic(badge);
                setText(null);
            }
        };
    }
}
