package com.jeonbuk.repair.model;

/**
 * 진행상태 — 청구·수령·출고 데이터로부터 자동 산출.
 */
public enum ProgressStatus {
    REPAIRING("수리중"),
    RELEASED("출고완료"),
    CLAIMED("청구완료"),
    SETTLED("수령완료");

    private final String label;

    ProgressStatus(String label) {
        this.label = label;
    }

    public String getLabel() {
        return label;
    }

    @Override
    public String toString() {
        return label;
    }
}
