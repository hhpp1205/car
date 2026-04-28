package com.jeonbuk.repair.controller;

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

    private Parent dashboardView;
    private DashboardController dashboardCtrl;

    private Parent intakeView;

    private Parent rentalView;
    private RentalHistoryController rentalCtrl;

    private Button activeNav;

    @FXML
    public void initialize() {
        // 화면들을 미리 로드해두고 사이드바 버튼 클릭 시 StackPane 의 활성 자식만 교체.
        ViewLoader.Loaded<DashboardController> dash = ViewLoader.loadWithController("/fxml/dashboard_view.fxml");
        dashboardView = dash.root();
        dashboardCtrl = dash.controller();

        intakeView = ViewLoader.load("/fxml/customer_intake_view.fxml");

        ViewLoader.Loaded<RentalHistoryController> rental = ViewLoader.loadWithController("/fxml/rental_history_view.fxml");
        rentalView = rental.root();
        rentalCtrl = rental.controller();

        // 첫 진입은 대시보드
        showDashboard();
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
