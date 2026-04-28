package com.jeonbuk.repair.model;

/**
 * 수리구분.
 */
public enum RepairType {
    GENERAL("일반수리"),
    INSURANCE("보험수리");

    private final String label;

    RepairType(String label) {
        this.label = label;
    }

    public String getLabel() {
        return label;
    }

    public static RepairType fromLabel(String label) {
        for (RepairType v : values()) {
            if (v.label.equals(label)) return v;
        }
        throw new IllegalArgumentException("Unknown RepairType: " + label);
    }

    @Override
    public String toString() {
        return label;
    }
}
