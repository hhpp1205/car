package com.jeonbuk.repair.util;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.NullAndEmptySource;
import org.junit.jupiter.params.provider.ValueSource;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class ValidatorsTest {

    @ParameterizedTest(name = "valid — \"{0}\"")
    @ValueSource(strings = {
            "12가3456",
            "전북98사3693",
            "377어6916",
            "001가0001"
    })
    @DisplayName("차량번호 — 한글이 포함되면 valid")
    void vehicleNumber_valid(String s) {
        assertTrue(Validators.isValidVehicleNumber(s));
    }

    @ParameterizedTest(name = "invalid — \"{0}\"")
    @ValueSource(strings = {
            "1234567",       // 숫자만
            "ABCD123",       // 영문/숫자
            "----",          // 한글 없음
            "12 3456"        // 공백 + 숫자, 한글 없음
    })
    @DisplayName("차량번호 — 한글 미포함이면 invalid")
    void vehicleNumber_invalid_no_hangul(String s) {
        assertFalse(Validators.isValidVehicleNumber(s));
    }

    @ParameterizedTest
    @NullAndEmptySource
    @DisplayName("차량번호 — null/빈 문자열은 invalid")
    void vehicleNumber_null_or_empty(String s) {
        assertFalse(Validators.isValidVehicleNumber(s));
    }

    @Test
    @DisplayName("isBlank — null/공백만은 true")
    void isBlank_true() {
        assertTrue(Validators.isBlank(null));
        assertTrue(Validators.isBlank(""));
        assertTrue(Validators.isBlank("   "));
        assertTrue(Validators.isBlank("\t\n"));
    }

    @Test
    @DisplayName("isBlank — 글자 하나라도 있으면 false")
    void isBlank_false() {
        assertFalse(Validators.isBlank("a"));
        assertFalse(Validators.isBlank(" a "));
    }
}
