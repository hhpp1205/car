package com.jeonbuk.repair.util;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.util.HashSet;
import java.util.Set;

import static org.junit.jupiter.api.Assertions.*;

class VehicleNameCatalogTest {

    @Test
    @DisplayName("카탈로그는 비어있지 않다")
    void not_empty() {
        assertFalse(VehicleNameCatalog.NAMES.isEmpty());
    }

    @Test
    @DisplayName("중복된 차량명이 없다")
    void no_duplicates() {
        Set<String> seen = new HashSet<>();
        for (String n : VehicleNameCatalog.NAMES) {
            assertTrue(seen.add(n), "중복: " + n);
        }
    }

    @Test
    @DisplayName("국산 대표 모델이 포함된다")
    void contains_korean_classics() {
        assertTrue(VehicleNameCatalog.NAMES.contains("쏘나타"));
        assertTrue(VehicleNameCatalog.NAMES.contains("아반떼"));
        assertTrue(VehicleNameCatalog.NAMES.contains("그랜저"));
        assertTrue(VehicleNameCatalog.NAMES.contains("K5"));
        assertTrue(VehicleNameCatalog.NAMES.contains("쏘렌토"));
        assertTrue(VehicleNameCatalog.NAMES.contains("토레스"));
        assertTrue(VehicleNameCatalog.NAMES.contains("SM6"));
        assertTrue(VehicleNameCatalog.NAMES.contains("스파크"));
    }

    @Test
    @DisplayName("한국에서 흔한 외국 모델이 포함된다")
    void contains_popular_foreign() {
        assertTrue(VehicleNameCatalog.NAMES.contains("BMW 5시리즈"));
        assertTrue(VehicleNameCatalog.NAMES.contains("벤츠 E클래스"));
        assertTrue(VehicleNameCatalog.NAMES.contains("아우디 A6"));
        assertTrue(VehicleNameCatalog.NAMES.contains("테슬라 모델3"));
        assertTrue(VehicleNameCatalog.NAMES.contains("렉서스 ES"));
    }

    @Test
    @DisplayName("모든 항목은 비어있지 않은 trim 문자열")
    void all_non_blank_and_trimmed() {
        for (String n : VehicleNameCatalog.NAMES) {
            assertFalse(n == null || n.isBlank(), "공백 항목 발견");
            assertEquals(n.trim(), n, "앞뒤 공백 항목: '" + n + "'");
        }
    }
}
