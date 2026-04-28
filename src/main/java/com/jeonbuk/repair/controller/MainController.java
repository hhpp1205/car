package com.jeonbuk.repair.controller;

import com.jeonbuk.repair.view.ViewLoader;
import javafx.fxml.FXML;
import javafx.scene.control.Tab;
import javafx.scene.control.TabPane;

public class MainController {

    @FXML private TabPane tabs;
    @FXML private Tab intakeTab;
    @FXML private Tab rentalTab;

    private RentalHistoryController rentalCtrl;

    @FXML
    public void initialize() {
        intakeTab.setContent(ViewLoader.load("/fxml/customer_intake_view.fxml"));

        ViewLoader.Loaded<RentalHistoryController> rental =
                ViewLoader.loadWithController("/fxml/rental_history_view.fxml");
        rentalCtrl = rental.controller();
        rentalTab.setContent(rental.root());

        // 탭 전환 시 입고 목록을 다시 불러와 통합 폼에서 새로 추가된 입고가 보이도록 한다.
        rentalTab.setOnSelectionChanged(e -> {
            if (rentalTab.isSelected()) rentalCtrl.refreshIntakeList();
        });
    }
}
