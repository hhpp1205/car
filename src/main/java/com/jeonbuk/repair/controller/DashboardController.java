package com.jeonbuk.repair.controller;

import com.jeonbuk.repair.model.CustomerIntake;
import com.jeonbuk.repair.model.InsuranceClaim;
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
import java.util.Comparator;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;

public class DashboardController {

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

    /** 사이드바에서 대시보드로 진입할 때마다 호출 — DB 변경사항 반영. */
    public void refresh() {
        List<CustomerIntake> intakes = ServiceRegistry.get().intakeRepo().findAll();
        List<InsuranceClaim> claims  = ServiceRegistry.get().claimRepo().findAll();

        // "종결 건 포함" 체크 — 기본값 true. 미체크 시 종결된 입고와 그 청구를 집계에서 제외.
        boolean includeClosed = includeClosedCheck == null || includeClosedCheck.isSelected();
        Set<Long> closedIntakeIds = intakes.stream()
                .filter(CustomerIntake::isClosed)
                .map(CustomerIntake::getId)
                .collect(Collectors.toSet());

        List<CustomerIntake> intakeScope = includeClosed
                ? intakes
                : intakes.stream().filter(i -> !i.isClosed()).toList();
        List<InsuranceClaim> claimScope = includeClosed
                ? claims
                : claims.stream()
                        .filter(c -> c.getIntake() == null || !closedIntakeIds.contains(c.getIntake().getId()))
                        .toList();

        // 진행 중 입고 — 출고일 비어있는 건수
        long inProgress = intakeScope.stream().filter(i -> i.getReleaseDate() == null).count();
        inProgressValue.setText(String.valueOf(inProgress));

        // 미수금 — claim_amount 가 있고 received_amount 가 부족한 분의 합계
        long outstanding = claimScope.stream()
                .mapToLong(c -> {
                    int amt = c.getClaimAmount() == null ? 0 : c.getClaimAmount();
                    int rcv = c.getReceivedAmount() == null ? 0 : c.getReceivedAmount();
                    return Math.max(0, amt - rcv);
                })
                .sum();
        outstandingValue.setText(Formatters.money((int) outstanding) + " 원");

        // 대차 중 — 종결 토글과 무관 (대차는 종료일로 판정)
        long active = ServiceRegistry.get().rentalService().findActive().size();
        rentalActiveValue.setText(String.valueOf(active));

        // 30일 경과 미수령
        InsuranceClaimService claimSvc = ServiceRegistry.get().claimService();
        long overdue = claimScope.stream().filter(claimSvc::isOverdue).count();
        overdueValue.setText(String.valueOf(overdue));
        overdueFootnote.setText(overdue > 0
                ? "확인 필요"
                : "청구 후 30일+ 경과");

        // 최근 입고
        List<CustomerIntake> recent = intakeScope.stream()
                .sorted(Comparator
                        .comparing(CustomerIntake::getIntakeDate, Comparator.nullsLast(Comparator.reverseOrder()))
                        .thenComparing(CustomerIntake::getId, Comparator.nullsLast(Comparator.reverseOrder())))
                .limit(RECENT_LIMIT)
                .toList();
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
