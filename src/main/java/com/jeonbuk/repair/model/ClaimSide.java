package com.jeonbuk.repair.model;

/**
 * 보험청구 구분 — 자차 / 상대.
 */
public enum ClaimSide {
    OWN("자차"),
    OPPONENT("상대방");

    private final String label;

    ClaimSide(String label) {
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
