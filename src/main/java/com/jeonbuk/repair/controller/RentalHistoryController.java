package com.jeonbuk.repair.controller;

import com.jeonbuk.repair.model.CustomerIntake;
import com.jeonbuk.repair.model.RentalHistory;
import com.jeonbuk.repair.model.RentalVehicle;
import com.jeonbuk.repair.service.RentalService;
import com.jeonbuk.repair.service.ServiceRegistry;
import com.jeonbuk.repair.util.Dialogs;
import com.jeonbuk.repair.util.Formatters;
import com.jeonbuk.repair.util.Toast;
import javafx.beans.property.SimpleObjectProperty;
import javafx.beans.property.SimpleStringProperty;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.fxml.FXML;
import javafx.scene.control.*;
import javafx.util.StringConverter;

import java.time.LocalDate;
import java.time.temporal.ChronoUnit;

public class RentalHistoryController {

    private final RentalService rentalService = ServiceRegistry.get().rentalService();

    @FXML private CheckBox onlyActiveCheck;
    @FXML private TableView<RentalHistory> rentalTable;
    @FXML private TableColumn<RentalHistory, String>    colIntakeNo;
    @FXML private TableColumn<RentalHistory, String>    colVehicle;
    @FXML private TableColumn<RentalHistory, LocalDate> colStartDate;
    @FXML private TableColumn<RentalHistory, LocalDate> colEndDate;
    @FXML private TableColumn<RentalHistory, Long>      colDays;
    @FXML private TableColumn<RentalHistory, String>    colMemo;

    @FXML private ChoiceBox<CustomerIntake> intakeChoice;
    @FXML private ComboBox<RentalVehicle>   vehicleCombo;
    @FXML private DatePicker startDatePicker;
    @FXML private DatePicker endDatePicker;
    @FXML private CheckBox   cbStartDateToday;
    @FXML private CheckBox   cbEndDateToday;
    @FXML private TextArea   memoArea;

    private final ObservableList<RentalHistory> data = FXCollections.observableArrayList();
    private RentalHistory editing;

    @FXML
    public void initialize() {
        configureTable();
        configureForm();
        reload();
    }

    private void configureTable() {
        colIntakeNo.setCellValueFactory(c -> new SimpleStringProperty(c.getValue().getIntake().getIntakeNo()));
        colVehicle.setCellValueFactory(c -> new SimpleStringProperty(
                c.getValue().getRentalVehicle().getName() + " (" + c.getValue().getRentalVehicle().getNumber() + ")"));
        colStartDate.setCellValueFactory(c -> new SimpleObjectProperty<>(c.getValue().getRentalStartDate()));
        colStartDate.setCellFactory(c -> dateCell());
        colEndDate.setCellValueFactory(c -> new SimpleObjectProperty<>(c.getValue().getRentalEndDate()));
        colEndDate.setCellFactory(c -> new TableCell<>() {
            @Override
            protected void updateItem(LocalDate item, boolean empty) {
                super.updateItem(item, empty);
                setText(empty ? "" : (item == null ? "대차중" : Formatters.date(item)));
            }
        });
        colDays.setCellValueFactory(c -> {
            RentalHistory r = c.getValue();
            LocalDate end = r.getRentalEndDate() == null ? LocalDate.now() : r.getRentalEndDate();
            long d = ChronoUnit.DAYS.between(r.getRentalStartDate(), end) + 1;
            return new SimpleObjectProperty<>(d);
        });
        colMemo.setCellValueFactory(c -> new SimpleStringProperty(c.getValue().getMemo()));

        rentalTable.setItems(data);
        rentalTable.getSelectionModel().selectedItemProperty().addListener((obs, oldV, newV) -> {
            if (newV != null) loadToForm(newV);
        });
    }

    private TableCell<RentalHistory, LocalDate> dateCell() {
        return new TableCell<>() {
            @Override
            protected void updateItem(LocalDate item, boolean empty) {
                super.updateItem(item, empty);
                setText(empty ? "" : Formatters.date(item));
            }
        };
    }

    private void configureForm() {
        intakeChoice.getItems().setAll(ServiceRegistry.get().intakeRepo().findAll());
        intakeChoice.setConverter(new StringConverter<>() {
            @Override public String toString(CustomerIntake i) {
                return i == null ? "" : i.getIntakeNo() + " · " + i.getVehicleName() + " " + i.getVehicleNumber();
            }
            @Override public CustomerIntake fromString(String s) { return null; }
        });

        vehicleCombo.getItems().setAll(rentalService.listVehicles());
        vehicleCombo.setConverter(new StringConverter<>() {
            @Override public String toString(RentalVehicle v) {
                return v == null ? "" : v.getName() + " (" + v.getNumber() + ")";
            }
            @Override public RentalVehicle fromString(String s) { return null; }
        });

        startDatePicker.setValue(LocalDate.now());

        onlyActiveCheck.selectedProperty().addListener((obs, o, n) -> reload());

        // "오늘" 체크박스 — 클릭 시 옆 날짜 칸을 오늘로 채우고 즉시 체크 해제
        CustomerIntakeController.wireTodayCheck(cbStartDateToday, startDatePicker);
        CustomerIntakeController.wireTodayCheck(cbEndDateToday,   endDatePicker);
    }

    @FXML
    private void onReload() {
        refreshIntakeList();
        reload();
    }

    /** 탭 전환 등 외부 트리거에 의해 입고 목록과 대차 테이블을 다시 불러올 때 호출. */
    public void refreshIntakeList() {
        CustomerIntake selected = intakeChoice.getValue();
        intakeChoice.getItems().setAll(ServiceRegistry.get().intakeRepo().findAll());
        if (selected != null) {
            intakeChoice.getItems().stream()
                    .filter(i -> i.getId().equals(selected.getId()))
                    .findFirst().ifPresent(intakeChoice::setValue);
        }
        reload();
    }

    /**
     * 외부 화면(대시보드 카드)에서 "대차 중" 항목만 보고 싶을 때 호출.
     * 활성 필터 체크박스를 켜고 selection 을 비운다.
     */
    public void showActiveOnly() {
        if (onlyActiveCheck != null && !onlyActiveCheck.isSelected()) {
            onlyActiveCheck.setSelected(true);  // listener 가 reload 트리거
        } else {
            reload();
        }
        if (rentalTable != null) rentalTable.getSelectionModel().clearSelection();
    }

    @FXML
    private void onNew() {
        clearForm();
        editing = null;
        rentalTable.getSelectionModel().clearSelection();
        // 입고 목록 갱신 (다른 화면에서 새 입고 추가됐을 수도)
        intakeChoice.getItems().setAll(ServiceRegistry.get().intakeRepo().findAll());
    }

    @FXML
    private void onClear() {
        clearForm();
        editing = null;
        rentalTable.getSelectionModel().clearSelection();
    }

    @FXML
    private void onSave() {
        try {
            RentalHistory target = (editing == null) ? new RentalHistory() : editing;
            applyForm(target);
            rentalService.save(target);
            reload();
            Toast.show(rentalTable, "대차이력이 저장되었습니다");
        } catch (IllegalArgumentException e) {
            Dialogs.warn("입력 오류", e.getMessage());
        } catch (RuntimeException e) {
            Dialogs.error("저장 실패", e.getMessage());
        }
    }

    @FXML
    private void onDelete() {
        RentalHistory sel = rentalTable.getSelectionModel().getSelectedItem();
        if (sel == null) {
            Dialogs.warn("삭제", "삭제할 대차이력을 선택해 주세요.");
            return;
        }
        if (!Dialogs.confirm("삭제 확인", "선택한 대차이력을 삭제하시겠습니까?")) return;
        rentalService.delete(sel.getId());
        clearForm();
        editing = null;
        reload();
    }

    private void reload() {
        data.setAll(onlyActiveCheck.isSelected() ? rentalService.findActive() : rentalService.findAll());
    }

    private void loadToForm(RentalHistory r) {
        editing = r;
        // intakeChoice 안에 같은 id 의 객체가 있다면 그것을 선택
        intakeChoice.getItems().stream()
                .filter(i -> i.getId().equals(r.getIntake().getId()))
                .findFirst().ifPresent(intakeChoice::setValue);

        vehicleCombo.getItems().stream()
                .filter(v -> v.getId().equals(r.getRentalVehicle().getId()))
                .findFirst().ifPresent(vehicleCombo::setValue);

        startDatePicker.setValue(r.getRentalStartDate());
        endDatePicker.setValue(r.getRentalEndDate());
        memoArea.setText(r.getMemo());
    }

    private void applyForm(RentalHistory r) {
        r.setIntake(intakeChoice.getValue());
        r.setRentalVehicle(vehicleCombo.getValue());
        r.setRentalStartDate(startDatePicker.getValue());
        r.setRentalEndDate(endDatePicker.getValue());
        String m = memoArea.getText();
        r.setMemo(m == null || m.isBlank() ? null : m.trim());
    }

    private void clearForm() {
        intakeChoice.setValue(null);
        vehicleCombo.setValue(null);
        startDatePicker.setValue(LocalDate.now());
        endDatePicker.setValue(null);
        memoArea.clear();
    }
}
