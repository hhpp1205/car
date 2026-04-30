package com.jeonbuk.repair.controller;

import com.jeonbuk.repair.model.AccidentType;
import com.jeonbuk.repair.model.ClaimSide;
import com.jeonbuk.repair.model.CustomerIntake;
import com.jeonbuk.repair.model.InsuranceClaim;
import com.jeonbuk.repair.model.RentalHistory;
import com.jeonbuk.repair.model.RentalVehicle;
import com.jeonbuk.repair.model.RepairType;
import com.jeonbuk.repair.service.CustomerIntakeService;
import com.jeonbuk.repair.service.IntakeFilter;
import com.jeonbuk.repair.service.IntakeWorkflowService;
import com.jeonbuk.repair.service.ServiceRegistry;
import com.jeonbuk.repair.util.Dialogs;
import com.jeonbuk.repair.util.Formatters;
import com.jeonbuk.repair.util.HangulIme;
import com.jeonbuk.repair.util.VehicleNameCatalog;
import javafx.beans.property.BooleanProperty;
import javafx.beans.property.SimpleBooleanProperty;
import javafx.beans.property.SimpleObjectProperty;
import javafx.beans.property.SimpleStringProperty;
import javafx.beans.value.ObservableValue;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.fxml.FXML;
import javafx.geometry.Side;
import javafx.scene.Node;
import javafx.scene.control.*;
import javafx.scene.control.cell.CheckBoxTableCell;
import javafx.scene.input.KeyCode;
import javafx.scene.input.KeyEvent;
import javafx.scene.input.MouseEvent;
import javafx.scene.layout.GridPane;
import javafx.scene.layout.VBox;
import javafx.util.StringConverter;

import java.time.LocalDate;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.EnumSet;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Optional;
import java.util.Set;

/**
 * 통합 입력 화면 — 입고 + 대차 + 자차/상대 보험청구를 한 폼에서 처리.
 */
public class CustomerIntakeController {

    private final CustomerIntakeService   intakeService   = ServiceRegistry.get().intakeService();
    private final IntakeWorkflowService   workflowService = ServiceRegistry.get().workflowService();

    // ----- 레이아웃 -----
    @FXML private SplitPane mainSplit;
    @FXML private VBox      formPane;
    /** 저장 직후 selectByIntakeNo() 가 row selection 을 트리거할 때 폼이 다시 뜨지 않도록 막는 플래그. */
    private boolean suppressFormShow = false;
    /** mouse press 시점의 선택 — click 시점엔 selection 이 이미 갱신돼있어 같은-row 재클릭 판별용. */
    private CustomerIntake selectionBeforeClick;

    // ----- 좌측 테이블 -----
    @FXML private TextField searchField;
    @FXML private RadioButton filterActive;
    @FXML private RadioButton filterClosed;
    @FXML private RadioButton filterAll;
    @FXML private Button closeBtn;
    @FXML private Button reopenBtn;
    @FXML private TableView<CustomerIntake> intakeTable;
    @FXML private TableColumn<CustomerIntake, Boolean>   colSelect;
    @FXML private TableColumn<CustomerIntake, String>    colIntakeNo;
    @FXML private TableColumn<CustomerIntake, LocalDate> colIntakeDate;
    @FXML private TableColumn<CustomerIntake, String>    colVehicleName;
    @FXML private TableColumn<CustomerIntake, String>    colVehicleNumber;
    @FXML private TableColumn<CustomerIntake, String>    colRepairType;
    @FXML private TableColumn<CustomerIntake, LocalDate> colReleaseDate;
    @FXML private TableColumn<CustomerIntake, String>    colSelfPayAmount;
    @FXML private TableColumn<CustomerIntake, LocalDate> colSelfPayDate;
    @FXML private TableColumn<CustomerIntake, String>    colTowDriver;
    @FXML private TableColumn<CustomerIntake, String>    colTowAmount;
    @FXML private TableColumn<CustomerIntake, String>    colRentalVehicle;
    @FXML private TableColumn<CustomerIntake, String>    colOwnCompany;
    @FXML private TableColumn<CustomerIntake, String>    colOwnClaimAmount;
    @FXML private TableColumn<CustomerIntake, String>    colOwnReceivedAmount;
    @FXML private TableColumn<CustomerIntake, LocalDate> colOwnReceivedDate;
    @FXML private TableColumn<CustomerIntake, String>    colOwnDifference;
    @FXML private TableColumn<CustomerIntake, String>    colOpponentCompany;
    @FXML private TableColumn<CustomerIntake, String>    colOpponentClaimAmount;
    @FXML private TableColumn<CustomerIntake, String>    colOpponentReceivedAmount;
    @FXML private TableColumn<CustomerIntake, LocalDate> colOpponentReceivedDate;
    @FXML private TableColumn<CustomerIntake, String>    colOpponentDifference;
    @FXML private TableColumn<CustomerIntake, String>    colStatus;

    // ----- 1. 입고 -----
    @FXML private TextField  intakeNoField;
    @FXML private DatePicker intakeDatePicker;
    @FXML private TextField  vehicleNameField;
    @FXML private TextField  vehicleNumberField;
    @FXML private TextField  phoneField;
    @FXML private ChoiceBox<RepairType> repairTypeChoice;
    @FXML private CheckBox cbSelf, cbOpponent, cbGeneral, cbFault;
    @FXML private DatePicker releaseDatePicker;
    @FXML private TextField  selfPayAmountField;
    @FXML private DatePicker selfPayDatePicker;
    @FXML private TextField  towDriverField;
    @FXML private TextField  towAmountField;
    @FXML private TextArea   memoArea;

    // ----- 2. 대차 -----
    @FXML private CheckBox useRentalCheck;
    @FXML private GridPane rentalGrid;
    @FXML private ComboBox<RentalVehicle> rentalVehicleCombo;
    @FXML private DatePicker rentalStartPicker;
    @FXML private DatePicker rentalEndPicker;
    @FXML private TextArea   rentalMemoArea;
    @FXML private Label      rentalNotice;

    // ----- 3. 자차 청구 -----
    @FXML private CheckBox useOwnClaimCheck;
    @FXML private GridPane ownClaimGrid;
    @FXML private ComboBox<String> ownCompanyCombo;
    @FXML private DatePicker ownClaimDatePicker;
    @FXML private TextField  ownClaimAmountField;
    @FXML private TextField  ownReceivedAmountField;
    @FXML private DatePicker ownReceivedDatePicker;
    @FXML private TextArea   ownClaimMemoArea;

    // ----- 4. 상대 청구 -----
    @FXML private CheckBox useOpponentClaimCheck;
    @FXML private GridPane opponentClaimGrid;
    @FXML private ComboBox<String> opponentCompanyCombo;
    @FXML private DatePicker opponentClaimDatePicker;
    @FXML private TextField  opponentClaimAmountField;
    @FXML private TextField  opponentReceivedAmountField;
    @FXML private DatePicker opponentReceivedDatePicker;
    @FXML private TextArea   opponentClaimMemoArea;

    // ----- 상태 -----
    private final ObservableList<CustomerIntake> data = FXCollections.observableArrayList();
    /** 입고 id → 가장 최근 대차차량 표시명. 테이블 렌더링 시 빠른 조회용. reload() 마다 갱신. */
    private final Map<Long, String> rentalNameByIntakeId = new HashMap<>();
    /** 입고 id → 자차 청구. 테이블 컬럼 표시용. reload() 마다 갱신. */
    private final Map<Long, InsuranceClaim> ownClaimByIntakeId = new HashMap<>();
    /** 입고 id → 상대 청구. 테이블 컬럼 표시용. reload() 마다 갱신. */
    private final Map<Long, InsuranceClaim> opponentClaimByIntakeId = new HashMap<>();
    private CustomerIntake editing;
    private RentalHistory  editingRental;
    private InsuranceClaim editingOwnClaim;
    private InsuranceClaim editingOpponentClaim;

    /** 차량명 자동완성용 — 등록된 차량명 캐시. 저장 시점마다 갱신. */
    private final List<String> vehicleNameSuggestions = new ArrayList<>();
    private ContextMenu vehicleNameAutoComplete;

    /**
     * 입고 id → 체크 상태 BooleanProperty.
     * CheckBoxTableCell 이 이 property 를 양방향 바인딩하므로 사용자 클릭이 즉시 반영된다.
     * 단일 진실 공급원 — checkedIds 같은 별도 Set 없이 property 의 .get() 으로 판정.
     */
    private final Map<Long, BooleanProperty> checkedProps = new HashMap<>();
    /** 헤더의 전체선택 체크박스 — 체크/해제 시 현재 표시된 모든 행에 일괄 적용. */
    private CheckBox headerSelectAll;
    /** 헤더 동기화 시 발생하는 setSelected 가 onAction 을 다시 트리거하지 않도록 가드. */
    private boolean syncingHeader = false;

    @FXML
    public void initialize() {
        configureTable();
        configureForm();
        reload();
        hideForm();  // 첫 진입 시 입력 폼은 숨김 — 신규/row 클릭 시에만 표시
        installEscToCloseForm();
    }

    /**
     * 폼이 떠있을 때 ESC 키로 닫는다.
     *
     * scene 이 붙는 시점은 컨트롤러 initialize 이후이므로 sceneProperty 리스너로 지연 등록.
     * bubble 단계(addEventHandler)로 등록 — DatePicker/ComboBox 팝업이 ESC 를 먼저 소비하면
     * 우리 핸들러는 호출되지 않아 팝업 닫기 동작이 보존된다.
     */
    private void installEscToCloseForm() {
        intakeTable.sceneProperty().addListener((obs, oldScene, scene) -> {
            if (scene != null) {
                scene.addEventHandler(KeyEvent.KEY_PRESSED, e -> {
                    if (e.getCode() == KeyCode.ESCAPE && isFormVisible()) {
                        onCloseForm();
                        e.consume();
                    }
                });
            }
        });
    }

    // -------------------- 테이블 --------------------

    private void configureTable() {
        configureSelectColumn();
        colIntakeNo.setCellValueFactory(c -> new SimpleStringProperty(c.getValue().getIntakeNo()));
        colIntakeDate.setCellValueFactory(c -> new SimpleObjectProperty<>(c.getValue().getIntakeDate()));
        colIntakeDate.setCellFactory(c -> dateCell());
        colVehicleName.setCellValueFactory(c -> new SimpleStringProperty(c.getValue().getVehicleName()));
        colVehicleNumber.setCellValueFactory(c -> new SimpleStringProperty(c.getValue().getVehicleNumber()));
        // 보험여부 — 자차/대물 보험사명. 둘 다 있으면 "자차사/대물사" 형식.
        // 보험사가 하나도 없으면 기존 수리구분 라벨(일반수리/보험수리) 로 폴백.
        colRepairType.setCellValueFactory(c -> new SimpleStringProperty(
                insuranceLabel(c.getValue())));
        colReleaseDate.setCellValueFactory(c -> new SimpleObjectProperty<>(c.getValue().getReleaseDate()));
        colReleaseDate.setCellFactory(c -> dateCell());
        colSelfPayAmount.setCellValueFactory(c -> new SimpleStringProperty(
                Formatters.money(c.getValue().getSelfPayAmount())));
        colSelfPayDate.setCellValueFactory(c -> new SimpleObjectProperty<>(c.getValue().getSelfPayDate()));
        colSelfPayDate.setCellFactory(c -> dateCell());
        colTowDriver.setCellValueFactory(c -> new SimpleStringProperty(
                c.getValue().getTowDriver() == null ? "" : c.getValue().getTowDriver()));
        colTowAmount.setCellValueFactory(c -> new SimpleStringProperty(
                Formatters.money(c.getValue().getTowAmount())));
        colRentalVehicle.setCellValueFactory(c -> new SimpleStringProperty(
                rentalNameByIntakeId.getOrDefault(c.getValue().getId(), "")));
        colOwnCompany.setCellValueFactory(c -> new SimpleStringProperty(
                claimCompany(ownClaimByIntakeId.get(c.getValue().getId()))));
        colOwnClaimAmount.setCellValueFactory(c -> new SimpleStringProperty(
                claimAmount(ownClaimByIntakeId.get(c.getValue().getId()))));
        colOwnReceivedAmount.setCellValueFactory(c -> new SimpleStringProperty(
                claimReceived(ownClaimByIntakeId.get(c.getValue().getId()))));
        colOwnReceivedDate.setCellValueFactory(c -> new SimpleObjectProperty<>(
                claimReceivedDate(ownClaimByIntakeId.get(c.getValue().getId()))));
        colOwnReceivedDate.setCellFactory(c -> dateCell());
        colOwnDifference.setCellValueFactory(c -> new SimpleStringProperty(
                claimDifference(ownClaimByIntakeId.get(c.getValue().getId()))));
        colOpponentCompany.setCellValueFactory(c -> new SimpleStringProperty(
                claimCompany(opponentClaimByIntakeId.get(c.getValue().getId()))));
        colOpponentClaimAmount.setCellValueFactory(c -> new SimpleStringProperty(
                claimAmount(opponentClaimByIntakeId.get(c.getValue().getId()))));
        colOpponentReceivedAmount.setCellValueFactory(c -> new SimpleStringProperty(
                claimReceived(opponentClaimByIntakeId.get(c.getValue().getId()))));
        colOpponentReceivedDate.setCellValueFactory(c -> new SimpleObjectProperty<>(
                claimReceivedDate(opponentClaimByIntakeId.get(c.getValue().getId()))));
        colOpponentReceivedDate.setCellFactory(c -> dateCell());
        colOpponentDifference.setCellValueFactory(c -> new SimpleStringProperty(
                claimDifference(opponentClaimByIntakeId.get(c.getValue().getId()))));
        colStatus.setCellValueFactory(c -> new SimpleStringProperty(
                intakeService.computeStatus(c.getValue()).getLabel()));
        colStatus.setCellFactory(c -> statusCell());

        // 종결된 row 는 전체 톤을 dim 처리해 활성 건과 시각 구분
        intakeTable.setRowFactory(tv -> new TableRow<>() {
            @Override
            protected void updateItem(CustomerIntake item, boolean empty) {
                super.updateItem(item, empty);
                getStyleClass().remove("row-closed");
                if (!empty && item != null && item.isClosed()) {
                    getStyleClass().add("row-closed");
                }
            }
        });

        intakeTable.setItems(data);
        intakeTable.getSelectionModel().selectedItemProperty().addListener((obs, oldV, newV) -> {
            updateBulkButtons();
            if (newV != null) {
                loadToForm(newV);
                if (!suppressFormShow) showForm();
            }
        });
        // 초기 진입 — 선택 없음 상태에서 버튼 비활성
        updateBulkButtons();

        // 테이블의 빈 영역(데이터 없는 row, placeholder, 스크롤 여백) 클릭 시 폼 닫기.
        // 헤더 클릭(정렬)은 native 동작을 유지하기 위해 제외.
        //
        // press 시점에 직전 선택을 저장 — click 시점엔 selection 이 이미 갱신돼 있어 비교가 의미 없다.
        // setOnMousePressed (event handler) 는 JavaFX 의 default selection update 보다 *후에* 실행되므로
        // capturing phase 의 addEventFilter 로 등록해야 selection 갱신 전 시점을 잡을 수 있다.
        intakeTable.addEventFilter(MouseEvent.MOUSE_PRESSED, e ->
                selectionBeforeClick = intakeTable.getSelectionModel().getSelectedItem());

        intakeTable.setOnMouseClicked(e -> {
            Node picked = e.getPickResult().getIntersectedNode();
            if (isInsideTableHeader(picked)) return;
            TableRow<?> row = findTableRow(picked);
            if (row == null || row.isEmpty()) {
                intakeTable.getSelectionModel().clearSelection();
                hideForm();
                return;
            }
            CustomerIntake clicked = (CustomerIntake) row.getItem();
            boolean sameRowReclick = clicked != null
                    && selectionBeforeClick != null
                    && java.util.Objects.equals(clicked.getId(), selectionBeforeClick.getId());
            if (sameRowReclick) {
                // 토글 — 폼이 떠있으면 닫고 선택 해제, 닫혀있으면 다시 띄움
                if (isFormVisible()) {
                    intakeTable.getSelectionModel().clearSelection();
                    hideForm();
                } else {
                    loadToForm(clicked);
                    showForm();
                }
            }
            // 다른 row 클릭 → selection listener 가 알아서 loadToForm + showForm 처리
        });
    }

    private boolean isFormVisible() {
        return formPane != null && mainSplit != null && mainSplit.getItems().contains(formPane);
    }

    private static boolean isInsideTableHeader(Node node) {
        for (Node n = node; n != null; n = n.getParent()) {
            String name = n.getClass().getSimpleName();
            if (name.contains("ColumnHeader") || name.contains("HeaderRow")) return true;
            if (n.getStyleClass().contains("column-header-background")
                || n.getStyleClass().contains("column-header")) return true;
        }
        return false;
    }

    private static TableRow<?> findTableRow(Node node) {
        for (Node n = node; n != null; n = n.getParent()) {
            if (n instanceof TableRow<?> tr) return tr;
        }
        return null;
    }

    /**
     * 체크박스 컬럼.
     *
     * CheckBoxTableCell 은 체크박스의 disable 을
     *   not( tableView.editable AND column.editable AND cell.editable )
     * 에 바인딩하므로 tableView/column 둘 다 editable=true 보장.
     *
     * cell factory 는 row 인덱스 → BooleanProperty 콜백 시그니처를 직접 사용 —
     * cellValueFactory 우회 경로 없이 명확하게 양방향 바인딩이 걸린다.
     */
    private void configureSelectColumn() {
        intakeTable.setEditable(true);
        colSelect.setEditable(true);

        headerSelectAll = new CheckBox();
        headerSelectAll.setOnAction(e -> {
            boolean wantAll = headerSelectAll.isSelected();
            for (CustomerIntake i : data) {
                if (i.getId() == null) continue;
                checkPropertyOf(i.getId()).set(wantAll);
            }
            syncHeaderCheckbox();
        });
        colSelect.setGraphic(headerSelectAll);

        colSelect.setCellFactory(CheckBoxTableCell.forTableColumn(rowIdx -> {
            if (rowIdx == null || rowIdx < 0 || rowIdx >= data.size()) return null;
            Long id = data.get(rowIdx).getId();
            if (id == null) return null;
            return checkPropertyOf(id);
        }));
    }

    /** 해당 id 의 체크 상태 property 를 가져오거나 새로 만든다 (변경 시 헤더/버튼 자동 갱신). */
    @SuppressWarnings("unchecked")
    private BooleanProperty checkPropertyOf(Long id) {
        return checkedProps.computeIfAbsent(id, k -> {
            SimpleBooleanProperty p = new SimpleBooleanProperty(false);
            p.addListener((obs, ov, nv) -> {
                syncHeaderCheckbox();
                updateBulkButtons();
            });
            return p;
        });
    }

    private boolean isChecked(Long id) {
        BooleanProperty p = checkedProps.get(id);
        return p != null && p.get();
    }

    /** 현재 체크된 입고 id 들 (visible/hidden 무관). */
    private List<Long> currentCheckedIds() {
        List<Long> out = new ArrayList<>();
        for (var e : checkedProps.entrySet()) {
            if (e.getValue().get()) out.add(e.getKey());
        }
        return out;
    }

    /** 모든 체크 해제. */
    private void clearAllChecks() {
        for (BooleanProperty p : checkedProps.values()) {
            if (p.get()) p.set(false);
        }
    }

    private void syncHeaderCheckbox() {
        if (headerSelectAll == null || syncingHeader) return;
        long visibleCount = data.stream().filter(i -> i.getId() != null).count();
        long checkedVisible = data.stream()
                .filter(i -> i.getId() != null && isChecked(i.getId()))
                .count();
        syncingHeader = true;
        try {
            headerSelectAll.setIndeterminate(checkedVisible > 0 && checkedVisible < visibleCount);
            headerSelectAll.setSelected(visibleCount > 0 && checkedVisible == visibleCount);
        } finally {
            syncingHeader = false;
        }
    }

    /** 체크된 항목들의 종결 상태를 보고 [종결]/[종결취소] 버튼 활성/비활성 갱신. */
    private void updateBulkButtons() {
        if (closeBtn == null || reopenBtn == null) return;
        boolean anyChecked = false;
        boolean anyActive = false;
        boolean anyClosed = false;
        for (CustomerIntake i : data) {
            if (i.getId() == null || !isChecked(i.getId())) continue;
            anyChecked = true;
            if (i.isClosed()) anyClosed = true;
            else              anyActive = true;
        }
        if (!anyChecked) {
            // 체크가 없으면 단건 폴백 — 선택된 행 기준
            CustomerIntake sel = intakeTable.getSelectionModel().getSelectedItem();
            if (sel == null) {
                closeBtn.setDisable(true);
                reopenBtn.setDisable(true);
            } else {
                closeBtn.setDisable(sel.isClosed());
                reopenBtn.setDisable(!sel.isClosed());
            }
            return;
        }
        closeBtn.setDisable(!anyActive);
        reopenBtn.setDisable(!anyClosed);
    }

    private TableCell<CustomerIntake, LocalDate> dateCell() {
        return new TableCell<>() {
            @Override
            protected void updateItem(LocalDate item, boolean empty) {
                super.updateItem(item, empty);
                setText(empty ? "" : Formatters.date(item));
            }
        };
    }

    private TableCell<CustomerIntake, String> statusCell() {
        return new TableCell<>() {
            @Override
            protected void updateItem(String item, boolean empty) {
                super.updateItem(item, empty);
                getStyleClass().removeAll(
                        "badge-repairing", "badge-released", "badge-claimed",
                        "badge-settled", "badge-closed");
                if (empty || item == null) { setText(""); return; }
                setText(item);
                switch (item) {
                    case "수리중"   -> getStyleClass().add("badge-repairing");
                    case "출고완료" -> getStyleClass().add("badge-released");
                    case "청구완료" -> getStyleClass().add("badge-claimed");
                    case "수령완료" -> getStyleClass().add("badge-settled");
                    case "종결"     -> getStyleClass().add("badge-closed");
                    default -> { /* no-op */ }
                }
            }
        };
    }

    // -------------------- 폼 초기화 --------------------

    private void configureForm() {
        // 수리구분
        repairTypeChoice.getItems().setAll(RepairType.values());
        repairTypeChoice.setValue(RepairType.GENERAL);
        intakeDatePicker.setValue(LocalDate.now());

        // 차량명: 영문 대문자 변환은 포커스를 잃을 때 일괄 처리 — 입력 중엔 IME 와 충돌하지 않게 둔다.
        // (inline TextFormatter 는 한글 IME 조합 단계에 호출돼 "스" 입력 시 ㅡ 가 결합되지 않는 이슈가 있음)
        setupVehicleNameAutoComplete();

        // 전화번호: 숫자만 쳐도 010-1234-5678 형식으로 자동 포매팅 (최대 11자리).
        phoneField.setTextFormatter(new TextFormatter<>(change -> {
            if (!change.isContentChange()) return change;
            String proposed = change.getControlNewText();
            String formatted = Formatters.formatPhoneAsType(proposed);
            if (formatted.equals(proposed)) return change;
            change.setRange(0, change.getControlText().length());
            change.setText(formatted);
            change.setCaretPosition(formatted.length());
            change.setAnchor(formatted.length());
            return change;
        }));

        // 차량번호: 영문 키 입력만 한글로 변환 (rk → 가). 허용 문자: 한글·숫자·하이픈·공백.
        // delta(새로 들어오는 글자)만 변환 — IME 가 이미 commit 한 한글은 건드리지 않음.
        vehicleNumberField.setTextFormatter(new TextFormatter<>(change -> {
            if (!change.isContentChange()) return change;
            String inserted = change.getText();
            if (inserted.isEmpty()) return change;  // 삭제만 — 통과

            String convertedInsert = HangulIme.convertChunk(inserted);

            String existing = change.getControlText();
            String before = existing.substring(0, change.getRangeStart());
            String after  = existing.substring(change.getRangeEnd());
            String newText = HangulIme.compose(before + convertedInsert + after);

            if (newText.equals(change.getControlNewText())) return change;

            change.setRange(0, existing.length());
            change.setText(newText);
            change.setCaretPosition(newText.length());
            change.setAnchor(newText.length());
            return change;
        }));

        // 대차차량
        rentalVehicleCombo.getItems().setAll(ServiceRegistry.get().rentalService().listVehicles());
        rentalVehicleCombo.setConverter(new StringConverter<>() {
            @Override public String toString(RentalVehicle v) {
                return v == null ? "" : v.getName() + " (" + v.getNumber() + ")";
            }
            @Override public RentalVehicle fromString(String s) { return null; }
        });

        // 보험사
        var companies = ServiceRegistry.get().companyRepo().findAllNames();
        ownCompanyCombo.getItems().setAll(companies);
        opponentCompanyCombo.getItems().setAll(companies);
        // 보험사명 영문은 입력 즉시 대문자 변환 (DB, KB, MG, AXA … 일관 표기)
        ownCompanyCombo.getEditor().setTextFormatter(upperCaseFormatter());
        opponentCompanyCombo.getEditor().setTextFormatter(upperCaseFormatter());

        // 섹션 활성화 바인딩
        rentalGrid.disableProperty().bind(useRentalCheck.selectedProperty().not());
        rentalMemoArea.disableProperty().bind(useRentalCheck.selectedProperty().not());
        ownClaimGrid.disableProperty().bind(useOwnClaimCheck.selectedProperty().not());
        ownClaimMemoArea.disableProperty().bind(useOwnClaimCheck.selectedProperty().not());
        opponentClaimGrid.disableProperty().bind(useOpponentClaimCheck.selectedProperty().not());
        opponentClaimMemoArea.disableProperty().bind(useOpponentClaimCheck.selectedProperty().not());

        // 사고유형 → 청구 섹션 자동 활성화 (CLAUDE.md 요구사항)
        cbSelf.selectedProperty().addListener((obs, o, n) -> {
            if (n) useOwnClaimCheck.setSelected(true);
        });
        cbOpponent.selectedProperty().addListener((obs, o, n) -> {
            if (n) useOpponentClaimCheck.setSelected(true);
        });

        useRentalCheck.selectedProperty().addListener((obs, o, n) -> {
            if (n && rentalStartPicker.getValue() == null) rentalStartPicker.setValue(LocalDate.now());
        });

        // 입고일은 신규 시 자동으로 오늘로 설정 — 그 외 날짜는 사용자가 캘린더에서 직접 선택.
        // "오늘" 체크박스 패턴은 UI 가 비좁아져 제거 (입고일은 intakeDatePicker.setValue(LocalDate.now()) 로 처리).
    }

    /** 체크 시 날짜 칸을 LocalDate.now() 로 채우고 체크박스는 즉시 해제. RentalHistoryController 에서 사용. */
    static void wireTodayCheck(CheckBox cb, DatePicker dp) {
        cb.setOnAction(e -> {
            if (cb.isSelected()) {
                dp.setValue(LocalDate.now());
                cb.setSelected(false);
            }
        });
    }

    // -------------------- 액션 --------------------

    @FXML
    private void onSearch() {
        refreshRentalMap();
        refreshClaimMap();
        data.setAll(intakeService.search(searchField.getText(), currentFilter()));
        syncHeaderCheckbox();
        updateBulkButtons();
    }

    @FXML
    private void onResetSearch() {
        searchField.clear();
        reload();
    }

    @FXML
    private void onFilterChanged() {
        // 필터를 바꾸면 검색어는 유지한 채 결과만 새로고침. 체크 상태는 reload() 에서 visible 기준으로 정리.
        if (searchField.getText() == null || searchField.getText().isBlank()) {
            reload();
        } else {
            onSearch();
        }
        // 선택된 행이 새 필터에서 사라질 수 있으므로 폼 정리
        if (intakeTable.getSelectionModel().getSelectedItem() == null) {
            hideForm();
        }
    }

    private IntakeFilter currentFilter() {
        if (filterClosed != null && filterClosed.isSelected()) return IntakeFilter.CLOSED;
        if (filterAll    != null && filterAll.isSelected())    return IntakeFilter.ALL;
        return IntakeFilter.ACTIVE;
    }

    @FXML
    private void onCloseIntake() {
        // 체크된 게 있으면 일괄 처리, 없으면 현재 선택 행으로 폴백.
        // wantClosed=true → "처리 후 종결 상태로 만들고 싶다" → 현재 진행 중인 것이 대상.
        List<CustomerIntake> targets = currentTargets(/*wantClosed*/ true);
        if (targets.isEmpty()) {
            Dialogs.warn("종결", "종결할 입고를 체크하거나 선택해 주세요.");
            return;
        }

        // 경고 요약 — 미출고 / 미수령 잔액
        int noRelease = 0;
        long outstandingTotal = 0;
        int outstandingCount = 0;
        for (CustomerIntake t : targets) {
            if (t.getReleaseDate() == null) noRelease++;
            long out = t.getClaims().stream()
                    .mapToLong(c -> {
                        int amt = c.getClaimAmount() == null ? 0 : c.getClaimAmount();
                        int rcv = c.getReceivedAmount() == null ? 0 : c.getReceivedAmount();
                        return Math.max(0, amt - rcv);
                    })
                    .sum();
            if (out > 0) {
                outstandingTotal += out;
                outstandingCount++;
            }
        }

        StringBuilder msg = new StringBuilder();
        msg.append("선택한 ").append(targets.size()).append("건을 종결하시겠습니까?\n");
        if (noRelease > 0 || outstandingCount > 0) {
            msg.append('\n');
            if (noRelease > 0) {
                msg.append("• 미출고 ").append(noRelease).append("건\n");
            }
            if (outstandingCount > 0) {
                msg.append("• 미수령 잔액 있는 건 ").append(outstandingCount)
                   .append("건 (총 ").append(Formatters.money((int) outstandingTotal)).append(" 원)\n");
            }
        }
        if (!Dialogs.confirm("종결 확인", msg.toString())) return;

        List<Long> ids = targets.stream().map(CustomerIntake::getId).toList();
        CustomerIntakeService.BulkResult result = intakeService.closeAll(ids);
        afterBulk("종결", result);
    }

    @FXML
    private void onReopenIntake() {
        // wantClosed=false → "처리 후 진행 상태로 만들고 싶다" → 현재 종결인 것이 대상.
        List<CustomerIntake> targets = currentTargets(/*wantClosed*/ false);
        if (targets.isEmpty()) {
            Dialogs.warn("종결취소", "종결취소할 입고를 체크하거나 선택해 주세요.");
            return;
        }
        if (!Dialogs.confirm("종결취소 확인",
                "선택한 " + targets.size() + "건의 종결을 취소하시겠습니까?")) {
            return;
        }
        List<Long> ids = targets.stream().map(CustomerIntake::getId).toList();
        CustomerIntakeService.BulkResult result = intakeService.reopenAll(ids);
        afterBulk("종결취소", result);
    }

    /**
     * 일괄 처리 대상 — 체크된 게 있으면 그 중 wantClosed 와 반대 상태인 항목들,
     * 체크가 없으면 현재 선택 행을 (조건에 맞을 때만) 단건으로 반환.
     */
    private List<CustomerIntake> currentTargets(boolean wantClosed) {
        List<CustomerIntake> out = new ArrayList<>();
        boolean hasChecked = false;
        for (CustomerIntake i : data) {
            if (i.getId() != null && isChecked(i.getId())) {
                hasChecked = true;
                if (i.isClosed() != wantClosed) out.add(i);
            }
        }
        if (hasChecked) return out;

        CustomerIntake sel = intakeTable.getSelectionModel().getSelectedItem();
        if (sel != null && sel.getId() != null && sel.isClosed() != wantClosed) {
            out.add(sel);
        }
        return out;
    }

    /** 일괄 처리 후 공통 마무리 — reload + 결과 보고 + 폼/체크 정리. */
    private void afterBulk(String label, CustomerIntakeService.BulkResult result) {
        clearAllChecks();
        reload();
        hideForm();
        intakeTable.getSelectionModel().clearSelection();

        StringBuilder sb = new StringBuilder();
        sb.append(label).append(" 결과: ").append(result.processed()).append("건 처리");
        if (result.skipped() > 0) sb.append(", ").append(result.skipped()).append("건 건너뜀");
        if (!result.failures().isEmpty()) {
            sb.append(", ").append(result.failures().size()).append("건 실패\n\n실패 사유:\n");
            for (String f : result.failures()) sb.append("• ").append(f).append('\n');
            Dialogs.error(label + " 일부 실패", sb.toString());
        } else {
            Dialogs.info(label + " 완료", sb.toString());
        }
    }

    @FXML
    private void onNew() {
        clearForm();
        editing = null;
        editingRental = null;
        editingOwnClaim = null;
        editingOpponentClaim = null;
        intakeTable.getSelectionModel().clearSelection();
        showForm();
        intakeDatePicker.requestFocus();
    }

    /** 입력 폼의 값만 비우고 닫지는 않음. (이전엔 onNew 로 위임) */
    @FXML
    private void onClear() {
        clearForm();
        editing = null;
        editingRental = null;
        editingOwnClaim = null;
        editingOpponentClaim = null;
        intakeTable.getSelectionModel().clearSelection();
        intakeDatePicker.requestFocus();
    }

    @FXML
    private void onCloseForm() {
        hideForm();
        intakeTable.getSelectionModel().clearSelection();
    }

    @FXML
    private void onSave() {
        try {
            IntakeWorkflowService.Form form = buildForm();
            CustomerIntake saved = workflowService.save(form);
            Dialogs.info("저장", "입고번호 " + saved.getIntakeNo() + " 저장되었습니다.");
            reload();
            reloadVehicleNameSuggestions();
            // 저장된 row 를 강조하되 listener 가 폼을 다시 띄우지 않도록 막은 후 닫기
            suppressFormShow = true;
            try {
                selectByIntakeNo(saved.getIntakeNo());
            } finally {
                suppressFormShow = false;
            }
            hideForm();
        } catch (IllegalArgumentException e) {
            Dialogs.warn("입력 오류", e.getMessage());
        } catch (RuntimeException e) {
            Dialogs.error("저장 실패", e.getMessage());
        }
    }

    @FXML
    private void onDelete() {
        CustomerIntake sel = intakeTable.getSelectionModel().getSelectedItem();
        if (sel == null) {
            Dialogs.warn("삭제", "삭제할 입고를 먼저 선택해 주세요.");
            return;
        }
        if (!Dialogs.confirm("삭제 확인",
                "입고번호 " + sel.getIntakeNo() + " 을(를) 삭제하시겠습니까?\n관련 보험청구·대차이력도 함께 삭제됩니다.")) {
            return;
        }
        intakeService.delete(sel.getId());
        clearForm();
        editing = null;
        editingRental = null;
        editingOwnClaim = null;
        editingOpponentClaim = null;
        intakeTable.getSelectionModel().clearSelection();
        hideForm();
        reload();
    }

    private void showForm() {
        if (formPane == null || mainSplit == null) return;
        if (!mainSplit.getItems().contains(formPane)) {
            mainSplit.getItems().add(formPane);
            // 세로 분할 — 상단 테이블 55% / 하단 폼 45%
            mainSplit.setDividerPositions(0.55);
        }
    }

    private void hideForm() {
        if (formPane == null || mainSplit == null) return;
        mainSplit.getItems().remove(formPane);
    }

    // -------------------- 헬퍼 --------------------

    private void reload() {
        refreshRentalMap();
        refreshClaimMap();
        data.setAll(intakeService.findAll(currentFilter()));
        // 체크 상태는 BooleanProperty 에 보존되어 있어 같은 id 가 다시 보이면 그대로 복원된다.
        // 헤더와 버튼 상태만 현재 표시 기준으로 갱신.
        syncHeaderCheckbox();
        updateBulkButtons();
    }

    /**
     * 외부 화면(보험사별 통계 등)에서 특정 입고를 노출하고자 할 때 호출.
     * 데이터 새로고침 후 해당 입고번호의 행을 선택·스크롤한다 — selection listener 가 폼을 열어줌.
     */
    public void revealByIntakeNo(String intakeNo) {
        if (intakeNo == null || intakeNo.isBlank()) return;
        reload();
        selectByIntakeNo(intakeNo);
    }

    /**
     * 입고 id → 가장 최근 대차차량 이름 맵 갱신.
     * findAll() 이 rentalStartDate desc, id desc 정렬이라 같은 입고의 첫 항목이 가장 최근.
     */
    private void refreshRentalMap() {
        rentalNameByIntakeId.clear();
        for (RentalHistory r : ServiceRegistry.get().rentalRepo().findAll()) {
            Long intakeId = r.getIntake().getId();
            rentalNameByIntakeId.putIfAbsent(intakeId, r.getRentalVehicle().getName());
        }
    }

    /** 입고 id → 자차/상대 청구 맵 갱신. */
    private void refreshClaimMap() {
        ownClaimByIntakeId.clear();
        opponentClaimByIntakeId.clear();
        for (InsuranceClaim c : ServiceRegistry.get().claimRepo().findAll()) {
            Long intakeId = c.getIntake().getId();
            if (c.getClaimSide() == ClaimSide.OWN) {
                ownClaimByIntakeId.put(intakeId, c);
            } else if (c.getClaimSide() == ClaimSide.OPPONENT) {
                opponentClaimByIntakeId.put(intakeId, c);
            }
        }
    }

    private static String claimCompany(InsuranceClaim c) {
        return c == null || c.getInsuranceCompany() == null ? "" : c.getInsuranceCompany();
    }

    private static String claimAmount(InsuranceClaim c) {
        return c == null ? "" : Formatters.money(c.getClaimAmount());
    }

    private static String claimReceived(InsuranceClaim c) {
        return c == null ? "" : Formatters.money(c.getReceivedAmount());
    }

    private static LocalDate claimReceivedDate(InsuranceClaim c) {
        return c == null ? null : c.getReceivedDate();
    }

    private static String claimDifference(InsuranceClaim c) {
        return c == null ? "" : Formatters.money(c.getOutstanding());
    }

    /**
     * 보험여부 컬럼 표시값 — 자차/대물 보험사명. 둘 다 있으면 "자차사/대물사", 한쪽만 있으면 그 쪽만.
     * 보험사가 하나도 없으면 기존 수리구분 라벨(일반수리/보험수리) 로 폴백.
     */
    private String insuranceLabel(CustomerIntake intake) {
        String own = companyName(ownClaimByIntakeId.get(intake.getId()));
        String opp = companyName(opponentClaimByIntakeId.get(intake.getId()));
        if (own != null && opp != null) return own + "/" + opp;
        if (own != null) return own;
        if (opp != null) return opp;
        return intake.getRepairType() == null ? "" : intake.getRepairType().getLabel();
    }

    private static String companyName(InsuranceClaim c) {
        if (c == null) return null;
        String name = c.getInsuranceCompany();
        return (name == null || name.isBlank()) ? null : name.trim();
    }

    private void selectByIntakeNo(String intakeNo) {
        for (CustomerIntake i : data) {
            if (intakeNo.equals(i.getIntakeNo())) {
                intakeTable.getSelectionModel().select(i);
                intakeTable.scrollTo(i);
                return;
            }
        }
    }

    private void loadToForm(CustomerIntake i) {
        editing = i;

        // 입고
        intakeNoField.setText(i.getIntakeNo());
        intakeDatePicker.setValue(i.getIntakeDate());
        vehicleNameField.setText(i.getVehicleName());
        vehicleNumberField.setText(i.getVehicleNumber());
        phoneField.setText(i.getPhone());
        repairTypeChoice.setValue(i.getRepairType());

        Set<AccidentType> set = i.getAccidentTypes();
        cbSelf.setSelected(set.contains(AccidentType.SELF));
        cbOpponent.setSelected(set.contains(AccidentType.OPPONENT));
        cbGeneral.setSelected(set.contains(AccidentType.GENERAL));
        cbFault.setSelected(set.contains(AccidentType.FAULT));

        releaseDatePicker.setValue(i.getReleaseDate());
        selfPayAmountField.setText(i.getSelfPayAmount() == null ? "" : i.getSelfPayAmount().toString());
        selfPayDatePicker.setValue(i.getSelfPayDate());
        towDriverField.setText(i.getTowDriver() == null ? "" : i.getTowDriver());
        towAmountField.setText(i.getTowAmount() == null ? "" : i.getTowAmount().toString());
        memoArea.setText(i.getMemo());

        // 대차 — 가장 최근(시작일 desc, id desc) 1건
        List<RentalHistory> rentals = ServiceRegistry.get().rentalRepo().findByIntakeId(i.getId());
        editingRental = rentals.stream()
                .max(Comparator.<RentalHistory, LocalDate>comparing(RentalHistory::getRentalStartDate)
                        .thenComparing(RentalHistory::getId))
                .orElse(null);
        if (editingRental != null) {
            useRentalCheck.setSelected(true);
            findVehicleInCombo(editingRental.getRentalVehicle().getId()).ifPresent(rentalVehicleCombo::setValue);
            rentalStartPicker.setValue(editingRental.getRentalStartDate());
            rentalEndPicker.setValue(editingRental.getRentalEndDate());
            rentalMemoArea.setText(editingRental.getMemo());
            rentalNotice.setText(rentals.size() > 1
                    ? "대차이력이 " + rentals.size() + "건 있습니다. 가장 최근 1건만 이 화면에서 편집 가능 — 나머지는 대차관리 탭에서."
                    : "");
        } else {
            useRentalCheck.setSelected(false);
            clearRentalFields();
            rentalNotice.setText("");
        }

        // 자차 청구
        editingOwnClaim = ServiceRegistry.get().claimRepo()
                .findByIntakeAndSide(i.getId(), ClaimSide.OWN).orElse(null);
        if (editingOwnClaim != null) {
            useOwnClaimCheck.setSelected(true);
            ownCompanyCombo.setValue(editingOwnClaim.getInsuranceCompany());
            ownClaimDatePicker.setValue(editingOwnClaim.getClaimDate());
            ownClaimAmountField.setText(toMoneyText(editingOwnClaim.getClaimAmount()));
            ownReceivedAmountField.setText(toMoneyText(editingOwnClaim.getReceivedAmount()));
            ownReceivedDatePicker.setValue(editingOwnClaim.getReceivedDate());
            ownClaimMemoArea.setText(editingOwnClaim.getMemo());
        } else {
            useOwnClaimCheck.setSelected(false);
            clearOwnClaimFields();
        }

        // 상대 청구
        editingOpponentClaim = ServiceRegistry.get().claimRepo()
                .findByIntakeAndSide(i.getId(), ClaimSide.OPPONENT).orElse(null);
        if (editingOpponentClaim != null) {
            useOpponentClaimCheck.setSelected(true);
            opponentCompanyCombo.setValue(editingOpponentClaim.getInsuranceCompany());
            opponentClaimDatePicker.setValue(editingOpponentClaim.getClaimDate());
            opponentClaimAmountField.setText(toMoneyText(editingOpponentClaim.getClaimAmount()));
            opponentReceivedAmountField.setText(toMoneyText(editingOpponentClaim.getReceivedAmount()));
            opponentReceivedDatePicker.setValue(editingOpponentClaim.getReceivedDate());
            opponentClaimMemoArea.setText(editingOpponentClaim.getMemo());
        } else {
            useOpponentClaimCheck.setSelected(false);
            clearOpponentClaimFields();
        }
    }

    private Optional<RentalVehicle> findVehicleInCombo(Long id) {
        return rentalVehicleCombo.getItems().stream().filter(v -> v.getId().equals(id)).findFirst();
    }

    private IntakeWorkflowService.Form buildForm() {
        IntakeWorkflowService.Form form = new IntakeWorkflowService.Form();

        // 입고
        CustomerIntake target = (editing == null) ? new CustomerIntake() : editing;
        target.setIntakeDate(intakeDatePicker.getValue());
        target.setVehicleName(trim(vehicleNameField.getText()));
        target.setVehicleNumber(trim(vehicleNumberField.getText()));
        // phoneField 는 TextFormatter 가 실시간 포매팅 — 그대로 저장
        target.setPhone(trim(phoneField.getText()));
        target.setRepairType(repairTypeChoice.getValue());

        Set<AccidentType> set = EnumSet.noneOf(AccidentType.class);
        if (cbSelf.isSelected())     set.add(AccidentType.SELF);
        if (cbOpponent.isSelected()) set.add(AccidentType.OPPONENT);
        if (cbGeneral.isSelected())  set.add(AccidentType.GENERAL);
        if (cbFault.isSelected())    set.add(AccidentType.FAULT);
        target.setAccidentTypes(set);

        target.setReleaseDate(releaseDatePicker.getValue());
        target.setSelfPayAmount(Formatters.parseMoney(selfPayAmountField.getText()));
        target.setSelfPayDate(selfPayDatePicker.getValue());
        target.setTowDriver(trim(towDriverField.getText()));
        target.setTowAmount(Formatters.parseMoney(towAmountField.getText()));
        target.setMemo(trim(memoArea.getText()));
        form.intake = target;

        // 대차
        if (useRentalCheck.isSelected()) {
            RentalHistory r = (editingRental == null) ? new RentalHistory() : editingRental;
            r.setRentalVehicle(rentalVehicleCombo.getValue());
            r.setRentalStartDate(rentalStartPicker.getValue());
            r.setRentalEndDate(rentalEndPicker.getValue());
            r.setMemo(trim(rentalMemoArea.getText()));
            form.rental = r;
        } else if (editingRental != null) {
            form.rentalIdToDelete = editingRental.getId();
        }

        // 자차 청구
        if (useOwnClaimCheck.isSelected()) {
            InsuranceClaim c = (editingOwnClaim == null) ? new InsuranceClaim() : editingOwnClaim;
            applyClaimFields(c, ownCompanyCombo, ownClaimDatePicker,
                    ownClaimAmountField, ownReceivedAmountField,
                    ownReceivedDatePicker, ownClaimMemoArea);
            form.ownClaim = c;
        } else if (editingOwnClaim != null) {
            form.ownClaimIdToDelete = editingOwnClaim.getId();
        }

        // 상대 청구
        if (useOpponentClaimCheck.isSelected()) {
            InsuranceClaim c = (editingOpponentClaim == null) ? new InsuranceClaim() : editingOpponentClaim;
            applyClaimFields(c, opponentCompanyCombo, opponentClaimDatePicker,
                    opponentClaimAmountField, opponentReceivedAmountField,
                    opponentReceivedDatePicker, opponentClaimMemoArea);
            form.opponentClaim = c;
        } else if (editingOpponentClaim != null) {
            form.opponentClaimIdToDelete = editingOpponentClaim.getId();
        }

        return form;
    }

    private void applyClaimFields(InsuranceClaim c, ComboBox<String> companyCombo,
                                  DatePicker claimDate, TextField claimAmount, TextField receivedAmount,
                                  DatePicker receivedDate, TextArea memo) {
        String company = companyCombo.getValue();
        c.setInsuranceCompany(company == null || company.isBlank() ? null : company.trim());
        c.setClaimDate(claimDate.getValue());
        c.setClaimAmount(Formatters.parseMoney(claimAmount.getText()));
        c.setReceivedAmount(Formatters.parseMoney(receivedAmount.getText()));
        c.setReceivedDate(receivedDate.getValue());
        c.setMemo(trim(memo.getText()));
    }

    private void clearForm() {
        // 입고
        intakeNoField.clear();
        intakeDatePicker.setValue(LocalDate.now());
        vehicleNameField.clear();
        vehicleNumberField.clear();
        phoneField.clear();
        repairTypeChoice.setValue(RepairType.GENERAL);
        cbSelf.setSelected(false);
        cbOpponent.setSelected(false);
        cbGeneral.setSelected(false);
        cbFault.setSelected(false);
        releaseDatePicker.setValue(null);
        selfPayAmountField.clear();
        selfPayDatePicker.setValue(null);
        towDriverField.clear();
        towAmountField.clear();
        memoArea.clear();

        useRentalCheck.setSelected(false);
        clearRentalFields();
        rentalNotice.setText("");

        useOwnClaimCheck.setSelected(false);
        clearOwnClaimFields();

        useOpponentClaimCheck.setSelected(false);
        clearOpponentClaimFields();
    }

    private void clearRentalFields() {
        rentalVehicleCombo.setValue(null);
        rentalStartPicker.setValue(null);
        rentalEndPicker.setValue(null);
        rentalMemoArea.clear();
    }

    private void clearOwnClaimFields() {
        ownCompanyCombo.setValue(null);
        ownClaimDatePicker.setValue(null);
        ownClaimAmountField.clear();
        ownReceivedAmountField.clear();
        ownReceivedDatePicker.setValue(null);
        ownClaimMemoArea.clear();
    }

    private void clearOpponentClaimFields() {
        opponentCompanyCombo.setValue(null);
        opponentClaimDatePicker.setValue(null);
        opponentClaimAmountField.clear();
        opponentReceivedAmountField.clear();
        opponentReceivedDatePicker.setValue(null);
        opponentClaimMemoArea.clear();
    }

    private static String trim(String s) {
        return s == null ? null : (s.isBlank() ? null : s.trim());
    }

    /**
     * 영문만 대문자로 즉시 변환 (한글·숫자·기호는 영향 없음). 매 호출마다 새 인스턴스.
     *
     * <p>한글 IME 호환성 — 변환 결과가 원본과 동일하면 {@code change.setText()} 를 호출하지 않는다.
     * 한글 조합 중에는 IME 가 매 키 입력을 composition 상태로 유지하는데, 무조건 setText 를 호출하면
     * 조합 상태가 끊겨 "스" 처럼 자음+모음 결합이 깨진다(자음만 보이고 모음이 사라지는 현상).
     */
    static TextFormatter<String> upperCaseFormatter() {
        return new TextFormatter<>(change -> {
            if (change.isContentChange()) {
                String original = change.getText();
                String upper = original.toUpperCase(java.util.Locale.ROOT);
                if (!upper.equals(original)) {
                    change.setText(upper);
                }
            }
            return change;
        });
    }

    private static String toMoneyText(Integer amount) {
        return amount == null ? "" : amount.toString();
    }

    // -------------------- 차량명 자동완성 --------------------

    private void setupVehicleNameAutoComplete() {
        vehicleNameAutoComplete = new ContextMenu();
        vehicleNameAutoComplete.setAutoHide(true);
        reloadVehicleNameSuggestions();

        vehicleNameField.textProperty().addListener((obs, oldV, newV) -> updateVehicleNameAutoComplete(newV));
        vehicleNameField.focusedProperty().addListener((obs, was, focused) -> {
            if (!focused) {
                // 포커스 빠질 때 영문을 대문자로 일괄 변환. 한글은 toUpperCase 영향 없음.
                String t = vehicleNameField.getText();
                if (t != null && !t.isEmpty()) {
                    String upper = t.toUpperCase(Locale.ROOT);
                    if (!upper.equals(t)) vehicleNameField.setText(upper);
                }
                vehicleNameAutoComplete.hide();
            }
        });
    }

    private void reloadVehicleNameSuggestions() {
        // DB 이름이 우선순위 (실제 정비 이력) — 그 뒤에 카탈로그를 합쳐 중복 제거.
        Set<String> merged = new LinkedHashSet<>(ServiceRegistry.get().intakeRepo().findDistinctVehicleNames());
        merged.addAll(VehicleNameCatalog.NAMES);
        vehicleNameSuggestions.clear();
        vehicleNameSuggestions.addAll(merged);
    }

    private void updateVehicleNameAutoComplete(String input) {
        if (input == null || input.isBlank()) {
            vehicleNameAutoComplete.hide();
            return;
        }
        String q = input.trim().toUpperCase(Locale.ROOT);
        List<MenuItem> items = new ArrayList<>();
        for (String name : vehicleNameSuggestions) {
            if (items.size() >= 8) break;
            String upper = name.toUpperCase(Locale.ROOT);
            if (!upper.contains(q)) continue;
            if (upper.equals(q)) continue;  // 입력과 동일하면 제안 의미 없음
            MenuItem mi = new MenuItem(name);
            mi.setOnAction(e -> {
                vehicleNameField.setText(name);
                vehicleNameField.positionCaret(name.length());
                vehicleNameAutoComplete.hide();
            });
            items.add(mi);
        }
        vehicleNameAutoComplete.getItems().setAll(items);
        if (items.isEmpty()) {
            vehicleNameAutoComplete.hide();
        } else if (!vehicleNameAutoComplete.isShowing()) {
            vehicleNameAutoComplete.show(vehicleNameField, Side.BOTTOM, 0, 0);
        }
    }
}
