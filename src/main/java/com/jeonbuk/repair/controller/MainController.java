package com.jeonbuk.repair.controller;

import com.jeonbuk.repair.service.IntakeFilter;
import com.jeonbuk.repair.view.DisplaySettingsDialog;
import com.jeonbuk.repair.view.ViewLoader;
import javafx.fxml.FXML;
import javafx.scene.Parent;
import javafx.scene.control.Button;
import javafx.scene.layout.StackPane;

public class MainController {

    @FXML private StackPane content;
    @FXML private Button navDashboard;
    @FXML private Button navIntake;
    @FXML private Button navRental;
    @FXML private Button navStats;
    @FXML private Button navSettings;

    private Parent dashboardView;
    private DashboardController dashboardCtrl;

    private Parent intakeView;
    private CustomerIntakeController intakeCtrl;

    private Parent rentalView;
    private RentalHistoryController rentalCtrl;

    private Parent statsView;
    private InsuranceStatsController statsCtrl;

    private Button activeNav;

    @FXML
    public void initialize() {
        // 화면들을 미리 로드해두고 사이드바 버튼 클릭 시 StackPane 의 활성 자식만 교체.
        ViewLoader.Loaded<DashboardController> dash = ViewLoader.loadWithController("/fxml/dashboard_view.fxml");
        dashboardView = dash.root();
        dashboardCtrl = dash.controller();

        ViewLoader.Loaded<CustomerIntakeController> intake = ViewLoader.loadWithController("/fxml/customer_intake_view.fxml");
        intakeView = intake.root();
        intakeCtrl = intake.controller();

        ViewLoader.Loaded<RentalHistoryController> rental = ViewLoader.loadWithController("/fxml/rental_history_view.fxml");
        rentalView = rental.root();
        rentalCtrl = rental.controller();

        ViewLoader.Loaded<InsuranceStatsController> stats = ViewLoader.loadWithController("/fxml/insurance_stats_view.fxml");
        statsView = stats.root();
        statsCtrl = stats.controller();
        // 통계 화면 → 입/출고관리 점프 콜백 주입
        statsCtrl.setIntakeNavigator(this::navigateToIntake);

        // 대시보드 카드 클릭 → 해당 화면+필터로 점프
        dashboardCtrl.setNavigator(new DashboardController.DashboardNavigator() {
            @Override public void goToInProgressIntakes() { navigateToIntakes(IntakeFilter.ACTIVE); }
            @Override public void goToOutstandingIntakes() { navigateToIntakes(IntakeFilter.ALL); }
            @Override public void goToActiveRentals() { navigateToActiveRentals(); }
            @Override public void goToOverdueIntakes() { navigateToIntakes(IntakeFilter.ALL); }
        });

        // 첫 진입은 대시보드
        showDashboard();
    }

    private void navigateToIntakes(IntakeFilter filter) {
        intakeCtrl.navigateWithFilter(filter);
        switchTo(intakeView, navIntake);
    }

    private void navigateToActiveRentals() {
        rentalCtrl.refreshIntakeList();
        rentalCtrl.showActiveOnly();
        switchTo(rentalView, navRental);
    }

    /** 통계 상세 다이얼로그에서 입고번호를 클릭했을 때 호출 — 입/출고관리로 전환 후 해당 행 노출. */
    public void navigateToIntake(String intakeNo) {
        intakeCtrl.revealByIntakeNo(intakeNo);
        switchTo(intakeView, navIntake);
    }

    @FXML private void onNavDashboard() { showDashboard(); }

    @FXML
    private void onNavIntake() {
        switchTo(intakeView, navIntake);
    }

    @FXML
    private void onNavRental() {
        rentalCtrl.refreshIntakeList();
        switchTo(rentalView, navRental);
    }

    @FXML
    private void onNavStats() {
        statsCtrl.refresh();
        switchTo(statsView, navStats);
    }

    @FXML
    private void onNavSettings() {
        DisplaySettingsDialog.show(navSettings.getScene().getWindow());
    }

    private void showDashboard() {
        dashboardCtrl.refresh();
        switchTo(dashboardView, navDashboard);
    }

    private void switchTo(Parent view, Button nav) {
        content.getChildren().setAll(view);
        if (activeNav != null) activeNav.getStyleClass().remove("active");
        nav.getStyleClass().add("active");
        activeNav = nav;
    }
}
