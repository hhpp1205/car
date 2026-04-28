package com.jeonbuk.repair.model;

import java.util.Arrays;
import java.util.EnumSet;
import java.util.Set;
import java.util.stream.Collectors;

/**
 * 사고유형 (다중 선택 가능).
 * DB에는 콤마로 합쳐 TEXT로 저장.
 */
public enum AccidentType {
    SELF("자차"),
    OPPONENT("대물"),
    GENERAL("일반수리"),
    FAULT("과실");

    private final String label;

    AccidentType(String label) {
        this.label = label;
    }

    public String getLabel() {
        return label;
    }

    public static AccidentType fromLabel(String label) {
        for (AccidentType v : values()) {
            if (v.label.equals(label)) return v;
        }
        throw new IllegalArgumentException("Unknown AccidentType: " + label);
    }

    public static String join(Set<AccidentType> set) {
        if (set == null || set.isEmpty()) return "";
        return set.stream().map(AccidentType::getLabel).collect(Collectors.joining(","));
    }

    public static Set<AccidentType> parse(String csv) {
        if (csv == null || csv.isBlank()) return EnumSet.noneOf(AccidentType.class);
        return Arrays.stream(csv.split(","))
                .map(String::trim)
                .filter(s -> !s.isEmpty())
                .map(AccidentType::fromLabel)
                .collect(Collectors.toCollection(() -> EnumSet.noneOf(AccidentType.class)));
    }

    @Override
    public String toString() {
        return label;
    }
}
