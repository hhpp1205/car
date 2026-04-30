package com.jeonbuk.repair.controller;

import com.jeonbuk.repair.model.ClaimSide;
import com.jeonbuk.repair.model.InsuranceClaim;
import com.jeonbuk.repair.service.InsuranceStatsService;
import com.jeonbuk.repair.service.InsuranceStatsService.CompanyStat;
import com.jeonbuk.repair.service.ServiceRegistry;
import com.jeonbuk.repair.util.Dialogs;
import com.jeonbuk.repair.util.Formatters;
import com.jeonbuk.repair.view.ViewLoader;
import javafx.application.Platform;
import javafx.beans.property.SimpleObjectProperty;
import javafx.beans.property.SimpleStringProperty;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.fxml.FXML;
import javafx.scene.Node;
import javafx.scene.Scene;
import javafx.scene.chart.BarChart;
import javafx.scene.chart.CategoryAxis;
import javafx.scene.chart.NumberAxis;
import javafx.scene.chart.XYChart;
import javafx.scene.control.CheckBox;
import javafx.scene.control.ChoiceBox;
import javafx.scene.control.DatePicker;
import javafx.scene.control.Label;
import javafx.scene.control.TableCell;
import javafx.scene.control.TableColumn;
import javafx.scene.control.TableRow;
import javafx.scene.control.TableView;
import javafx.scene.control.Tooltip;
import javafx.stage.Modality;
import javafx.stage.Stage;
import javafx.stage.Window;
import javafx.util.Duration;
import javafx.util.StringConverter;

import java.time.LocalDate;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.function.Consumer;

public class InsuranceStatsController {

    /** 화면 진입 시 기본 기간 — 최근 6개월. */
    private static final int DEFAULT_RANGE_MONTHS = 6;

    @FXML private DatePicker fromDatePicker;
    @FXML private DatePicker toDatePicker;
    @FXML private ChoiceBox<SideOption> sideChoice;
    @FXML private CheckBox hideEmptyCheck;
    @FXML private Label periodSummaryLabel;

    @FXML private Label totalCountValue;
    @FXML private Label totalClaimValue;
    @FXML private Label totalReceivedValue;
    @FXML private Label totalReceiveRate;
    @FXML private Label totalOutstandingValue;
    @FXML private Label totalOutstandingFootnote;

    @FXML private TableView<CompanyStat> statsTable;
    @FXML private TableColumn<CompanyStat, String> colCompany;
    @FXML private TableColumn<CompanyStat, Number> colCount;
    @FXML private TableColumn<CompanyStat, Number> colClaim;
    @FXML private TableColumn<CompanyStat, Number> colReceived;
    @FXML private TableColumn<CompanyStat, Number> colOutstanding;
    @FXML private TableColumn<CompanyStat, Number> colReceiveRate;
    @FXML private TableColumn<CompanyStat, Number> colAvgDays;
    @FXML private TableColumn<CompanyStat, Number> colOverdue;

    @FXML private BarChart<String, Number> chart;
    @FXML private CategoryAxis chartXAxis;
    @FXML private NumberAxis chartYAxis;

    private final ObservableList<CompanyStat> rows = FXCollections.observableArrayList();

    /** 입고번호 클릭/행 더블클릭 시 입/출고관리로 점프. MainController 가 주입. */
    private Consumer<String> intakeNavigator;

    @FXML
    public void initialize() {
        configureFilters();
        configureTable();
        reload();
    }

    /**
     * MainController 가 호출 — 보험사 더블클릭 → 상세 다이얼로그 → 입고번호 클릭/행 더블클릭 시
     * 입/출고관리 화면으로 점프하는 콜백.
     */
    public void setIntakeNavigator(Consumer<String> navigator) {
        this.intakeNavigator = navigator;
    }

    /** 사이드바에서 통계 화면으로 진입할 때마다 호출 — DB 변경사항 반영. */
    public void refresh() {
        reload();
    }

    @FXML
    private void onRefresh() {
        reload();
    }

    @FXML
    private void onApplyFilter() {
        reload();
    }

    @FXML private void onPeriod3M() { setPeriodMonths(3); }
    @FXML private void onPeriod6M() { setPeriodMonths(6); }

    @FXML
    private void onPeriodYTD() {
        LocalDate today = LocalDate.now();
        fromDatePicker.setValue(today.withDayOfYear(1));
        toDatePicker.setValue(today);
        reload();
    }

    @FXML
    private void onPeriodAll() {
        fromDatePicker.setValue(null);
        toDatePicker.setValue(null);
        reload();
    }

    private void setPeriodMonths(int months) {
        LocalDate today = LocalDate.now();
        fromDatePicker.setValue(today.minusMonths(months));
        toDatePicker.setValue(today);
        reload();
    }

    private void configureFilters() {
        // 기본 기간 — 최근 6개월
        LocalDate today = LocalDate.now();
        toDatePicker.setValue(today);
        fromDatePicker.setValue(today.minusMonths(DEFAULT_RANGE_MONTHS));

        sideChoice.getItems().setAll(
                new SideOption("전체", null),
                new SideOption("자차", ClaimSide.OWN),
                new SideOption("상대", ClaimSide.OPPONENT));
        sideChoice.setConverter(new StringConverter<>() {
            @Override public String toString(SideOption o) { return o == null ? "" : o.label(); }
            @Override public SideOption fromString(String s) { return null; }
        });
        sideChoice.getSelectionModel().selectFirst();
    }

    private void configureTable() {
        colCompany.setCellValueFactory(c -> new SimpleStringProperty(c.getValue().company()));
        colCount.setCellValueFactory(c -> new SimpleObjectProperty<>(c.getValue().count()));

        colClaim.setCellValueFactory(c -> new SimpleObjectProperty<>(c.getValue().claimSum()));
        colClaim.setCellFactory(c -> moneyCell());

        colReceived.setCellValueFactory(c -> new SimpleObjectProperty<>(c.getValue().receivedSum()));
        colReceived.setCellFactory(c -> moneyCell());

        colOutstanding.setCellValueFactory(c -> new SimpleObjectProperty<>(c.getValue().outstandingSum()));
        colOutstanding.setCellFactory(c -> outstandingCell());

        colReceiveRate.setCellValueFactory(c -> new SimpleObjectProperty<>(c.getValue().receiveRate()));
        colReceiveRate.setCellFactory(c -> percentCell());

        colAvgDays.setCellValueFactory(c -> new SimpleObjectProperty<>(c.getValue().avgDaysToReceive()));
        colAvgDays.setCellFactory(c -> avgDaysCell());

        colOverdue.setCellValueFactory(c -> new SimpleObjectProperty<>(c.getValue().overdueCount()));
        colOverdue.setCellFactory(c -> overdueCell());

        statsTable.setItems(rows);

        // 행 더블클릭 → 상세 다이얼로그. 빈 행과 건수 0 인 행(실적 없는 보험사) 은 무시.
        statsTable.setRowFactory(tv -> {
            TableRow<CompanyStat> row = new TableRow<>();
            row.setOnMouseClicked(e -> {
                if (e.getClickCount() == 2 && !row.isEmpty() && row.getItem().count() > 0) {
                    openDetailDialog(row.getItem());
                }
            });
            return row;
        });
    }

    private void reload() {
        LocalDate from = fromDatePicker.getValue();
        LocalDate to   = toDatePicker.getValue();
        SideOption side = sideChoice.getValue();
        ClaimSide sideFilter = side == null ? null : side.value();

        InsuranceStatsService svc = ServiceRegistry.get().statsService();
        List<CompanyStat> stats = svc.aggregateByCompany(from, to, sideFilter);

        // "실적 없는 보험사 숨김" 해제 시 — 마스터 12개 보험사 중 실적 없는 회사도 0 행으로 노출
        if (!hideEmptyCheck.isSelected()) {
            stats = mergeWithKnownCompanies(stats);
        }

        rows.setAll(stats);
        updateSummaryCards(stats);
        updateChart(stats);
        updatePeriodSummary(from, to);
    }

    /** 필터바 아래에 적용된 기간을 명시 — 비어있을 때는 "전체" 로 표기. */
    private void updatePeriodSummary(LocalDate from, LocalDate to) {
        if (periodSummaryLabel == null) return;
        if (from == null && to == null) {
            periodSummaryLabel.setText("표시 기간: 전체 (제한 없음)");
            return;
        }
        String fromText = from == null ? "처음"   : Formatters.date(from);
        String toText   = to   == null ? "오늘"   : Formatters.date(to);
        periodSummaryLabel.setText("표시 기간: " + fromText + " ~ " + toText);
    }

    private List<CompanyStat> mergeWithKnownCompanies(List<CompanyStat> existing) {
        Set<String> seen = new HashSet<>();
        for (CompanyStat s : existing) seen.add(s.company());
        List<CompanyStat> merged = new ArrayList<>(existing);
        for (String name : ServiceRegistry.get().companyRepo().findAllNames()) {
            if (!seen.contains(name)) {
                merged.add(new CompanyStat(name, 0, 0L, 0L, 0L, Double.NaN, 0));
            }
        }
        return merged;
    }

    private void updateSummaryCards(List<CompanyStat> stats) {
        int totalCount = stats.stream().mapToInt(CompanyStat::count).sum();
        long totalClaim = stats.stream().mapToLong(CompanyStat::claimSum).sum();
        long totalReceived = stats.stream().mapToLong(CompanyStat::receivedSum).sum();
        long totalOutstanding = Math.max(0L, totalClaim - totalReceived);
        double rate = totalClaim <= 0 ? 0.0 : (double) totalReceived / (double) totalClaim;

        totalCountValue.setText(String.valueOf(totalCount));
        totalClaimValue.setText(Formatters.money((int) Math.min(Integer.MAX_VALUE, totalClaim)) + " 원");
        totalReceivedValue.setText(Formatters.money((int) Math.min(Integer.MAX_VALUE, totalReceived)) + " 원");
        totalReceiveRate.setText(String.format("수령률 %.1f%%", rate * 100));
        totalOutstandingValue.setText(Formatters.money((int) Math.min(Integer.MAX_VALUE, totalOutstanding)) + " 원");
        totalOutstandingFootnote.setText(totalOutstanding > 0 ? "청구 - 수령" : "전액 수령 완료");
    }

    private void openDetailDialog(CompanyStat stat) {
        LocalDate from = fromDatePicker.getValue();
        LocalDate to   = toDatePicker.getValue();
        SideOption side = sideChoice.getValue();
        ClaimSide sideFilter = side == null ? null : side.value();

        InsuranceStatsService svc = ServiceRegistry.get().statsService();
        List<InsuranceClaim> claims = svc.findClaimsForCompany(stat.company(), from, to, sideFilter);

        try {
            ViewLoader.Loaded<InsuranceClaimDetailController> loaded =
                    ViewLoader.loadWithController("/fxml/insurance_claim_detail_dialog.fxml");
            InsuranceClaimDetailController ctrl = loaded.controller();

            String subtitle = formatSubtitle(from, to, side);
            ctrl.load(stat.company(), subtitle, claims, intakeNavigator);

            Stage dialog = new Stage();
            dialog.setTitle("보험사 상세 — " + stat.company());
            dialog.initOwner(ownerWindow());
            dialog.initModality(Modality.WINDOW_MODAL);
            Scene scene = new Scene(loaded.root());
            // 메인 윈도우의 스타일시트(card·badge·table 격자선 등)를 그대로 적용.
            Window owner = ownerWindow();
            if (owner != null && owner.getScene() != null) {
                scene.getStylesheets().setAll(owner.getScene().getStylesheets());
            }
            dialog.setScene(scene);
            ctrl.installEscToClose(dialog);
            dialog.showAndWait();
        } catch (RuntimeException ex) {
            Dialogs.error("상세 보기 실패", ex.getMessage());
        }
    }

    private Window ownerWindow() {
        return statsTable.getScene() == null ? null : statsTable.getScene().getWindow();
    }

    private static String formatSubtitle(LocalDate from, LocalDate to, SideOption side) {
        String period = (from == null ? "처음" : Formatters.date(from))
                + " ~ "
                + (to == null ? "오늘" : Formatters.date(to));
        String sideLabel = (side == null || side.value() == null) ? "전체" : side.label();
        return period + " · " + sideLabel + " (청구일 기준)";
    }

    private void updateChart(List<CompanyStat> stats) {
        // 카테고리축 카테고리 명시 — JavaFX BarChart 가 X 라벨 정렬을 표시 순서대로 유지하게 함.
        // (실적 0건 회사가 섞여있을 때도 일관된 순서)
        List<String> categories = stats.stream().map(CompanyStat::company).toList();
        chartXAxis.getCategories().setAll(categories);

        XYChart.Series<String, Number> claimSeries = new XYChart.Series<>();
        claimSeries.setName("청구액");
        XYChart.Series<String, Number> recvSeries = new XYChart.Series<>();
        recvSeries.setName("수령액");

        for (CompanyStat s : stats) {
            claimSeries.getData().add(new XYChart.Data<>(s.company(), s.claimSum()));
            recvSeries.getData().add(new XYChart.Data<>(s.company(), s.receivedSum()));
        }
        chart.getData().setAll(List.of(claimSeries, recvSeries));

        // 막대 노드는 setData 후 layout 패스에서 attach 되므로 runLater 로 툴팁 부착 연기
        Platform.runLater(this::attachBarTooltips);
    }

    /** 각 막대에 hover 시 정확한 금액 Tooltip 부착 — 막대 자체엔 라벨을 그리지 않고 호버에만 노출. */
    private void attachBarTooltips() {
        for (XYChart.Series<String, Number> series : chart.getData()) {
            for (XYChart.Data<String, Number> data : series.getData()) {
                Node bar = data.getNode();
                if (bar == null) continue;
                long value = data.getYValue() == null ? 0L : data.getYValue().longValue();
                Tooltip tt = new Tooltip(
                        data.getXValue() + "\n" + series.getName() + ": "
                                + Formatters.money((int) Math.min(Integer.MAX_VALUE, value)) + " 원");
                tt.setShowDelay(Duration.millis(120));
                Tooltip.install(bar, tt);
            }
        }
    }

    // ─── 셀 팩토리 ──────────────────────────────────────────────

    private static TableCell<CompanyStat, Number> moneyCell() {
        return new TableCell<>() {
            @Override
            protected void updateItem(Number item, boolean empty) {
                super.updateItem(item, empty);
                if (empty || item == null) { setText(""); return; }
                setText(Formatters.money((int) Math.min(Integer.MAX_VALUE, item.longValue())));
                setStyle("-fx-alignment: CENTER-RIGHT;");
            }
        };
    }

    private static TableCell<CompanyStat, Number> outstandingCell() {
        return new TableCell<>() {
            @Override
            protected void updateItem(Number item, boolean empty) {
                super.updateItem(item, empty);
                if (empty || item == null) { setText(""); setStyle(""); return; }
                long v = item.longValue();
                setText(Formatters.money((int) Math.min(Integer.MAX_VALUE, v)));
                if (v > 0) {
                    setStyle("-fx-alignment: CENTER-RIGHT; -fx-text-fill: #b91c1c; -fx-font-weight: bold;");
                } else {
                    setStyle("-fx-alignment: CENTER-RIGHT; -fx-text-fill: #15803d;");
                }
            }
        };
    }

    private static TableCell<CompanyStat, Number> percentCell() {
        return new TableCell<>() {
            @Override
            protected void updateItem(Number item, boolean empty) {
                super.updateItem(item, empty);
                if (empty || item == null) { setText(""); return; }
                setText(String.format("%.1f%%", item.doubleValue() * 100));
                setStyle("-fx-alignment: CENTER-RIGHT;");
            }
        };
    }

    private static TableCell<CompanyStat, Number> avgDaysCell() {
        return new TableCell<>() {
            @Override
            protected void updateItem(Number item, boolean empty) {
                super.updateItem(item, empty);
                if (empty || item == null || Double.isNaN(item.doubleValue())) {
                    setText("-");
                    setStyle("-fx-alignment: CENTER;");
                    return;
                }
                setText(String.format("%.1f 일", item.doubleValue()));
                setStyle("-fx-alignment: CENTER-RIGHT;");
            }
        };
    }

    private static TableCell<CompanyStat, Number> overdueCell() {
        return new TableCell<>() {
            @Override
            protected void updateItem(Number item, boolean empty) {
                super.updateItem(item, empty);
                if (empty || item == null) { setText(""); setStyle(""); return; }
                int v = item.intValue();
                setText(v == 0 ? "0" : v + "건");
                if (v > 0) {
                    setStyle("-fx-alignment: CENTER; -fx-text-fill: #b91c1c; -fx-font-weight: bold;");
                } else {
                    setStyle("-fx-alignment: CENTER; -fx-text-fill: -color-fg-muted;");
                }
            }
        };
    }

    /** 청구 구분 ChoiceBox 항목. */
    private record SideOption(String label, ClaimSide value) {}
}
