package com.jeonbuk.repair.model;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.util.EnumSet;
import java.util.Set;

import static org.junit.jupiter.api.Assertions.*;

class AccidentTypeTest {

    @Test
    @DisplayName("join — null/빈 set → 빈 문자열")
    void join_empty() {
        assertEquals("", AccidentType.join(null));
        assertEquals("", AccidentType.join(EnumSet.noneOf(AccidentType.class)));
    }

    @Test
    @DisplayName("join — 콤마 구분으로 라벨 결합")
    void join_csv() {
        Set<AccidentType> set = EnumSet.of(AccidentType.SELF, AccidentType.OPPONENT, AccidentType.FAULT);
        String csv = AccidentType.join(set);
        // EnumSet 순서 보장 → 자차,대물,과실
        assertEquals("자차,대물,과실", csv);
    }

    @Test
    @DisplayName("parse — 빈 문자열 → 빈 set")
    void parse_empty() {
        assertTrue(AccidentType.parse(null).isEmpty());
        assertTrue(AccidentType.parse("").isEmpty());
        assertTrue(AccidentType.parse("   ").isEmpty());
    }

    @Test
    @DisplayName("parse — 콤마 구분 라벨")
    void parse_csv() {
        Set<AccidentType> result = AccidentType.parse("자차,대물");
        assertEquals(EnumSet.of(AccidentType.SELF, AccidentType.OPPONENT), result);
    }

    @Test
    @DisplayName("parse — 공백/빈 토큰 무시")
    void parse_trim_and_skip_empty() {
        Set<AccidentType> result = AccidentType.parse(" 자차 , ,대물 ");
        assertEquals(EnumSet.of(AccidentType.SELF, AccidentType.OPPONENT), result);
    }

    @Test
    @DisplayName("parse — 알 수 없는 라벨 → IllegalArgumentException")
    void parse_unknown() {
        assertThrows(IllegalArgumentException.class, () -> AccidentType.parse("자차,XYZ"));
    }

    @Test
    @DisplayName("join → parse 라운드트립")
    void roundtrip() {
        Set<AccidentType> original = EnumSet.of(AccidentType.SELF, AccidentType.GENERAL, AccidentType.FAULT);
        Set<AccidentType> after = AccidentType.parse(AccidentType.join(original));
        assertEquals(original, after);
    }
}
