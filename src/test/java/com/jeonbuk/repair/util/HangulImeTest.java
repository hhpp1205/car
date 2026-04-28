package com.jeonbuk.repair.util;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

class HangulImeTest {

    @Test
    @DisplayName("두벌식 QWERTY 차량번호 — rk → 가, 12rk3456 → 12가3456")
    void converts_qwerty_to_hangul_in_vehicle_number() {
        assertEquals("12가3456", HangulIme.filterVehicleNumber("12rk3456"));
        assertEquals("12나3456", HangulIme.filterVehicleNumber("12sk3456"));
        assertEquals("12다3456", HangulIme.filterVehicleNumber("12ek3456"));
        assertEquals("12라3456", HangulIme.filterVehicleNumber("12fk3456"));
        assertEquals("12마3456", HangulIme.filterVehicleNumber("12ak3456"));
        assertEquals("12바3456", HangulIme.filterVehicleNumber("12qk3456"));
        assertEquals("12사3456", HangulIme.filterVehicleNumber("12tk3456"));
        assertEquals("12아3456", HangulIme.filterVehicleNumber("12dk3456"));
        assertEquals("12자3456", HangulIme.filterVehicleNumber("12wk3456"));
        assertEquals("12하3456", HangulIme.filterVehicleNumber("12gk3456"));
    }

    @Test
    @DisplayName("이미 한글로 들어온 문자열은 그대로 통과")
    void already_hangul_passes_through() {
        assertEquals("12가3456", HangulIme.filterVehicleNumber("12가3456"));
        assertEquals("123거4567", HangulIme.filterVehicleNumber("123거4567"));
        assertEquals("전북98사3693", HangulIme.filterVehicleNumber("전북98사3693"));
    }

    @Test
    @DisplayName("멱등 — filter(filter(x)) == filter(x)")
    void idempotent() {
        String input = "12rk3456";
        String once  = HangulIme.filterVehicleNumber(input);
        String twice = HangulIme.filterVehicleNumber(once);
        assertEquals(once, twice);
    }

    @Test
    @DisplayName("연속 자모는 모두 음절로 조합 — fkrk → 라가")
    void composes_consecutive_syllables() {
        assertEquals("라가", HangulIme.filterVehicleNumber("fkrk"));
        assertEquals("다라", HangulIme.filterVehicleNumber("ekfk"));
        assertEquals("가나다", HangulIme.filterVehicleNumber("rkskek"));
    }

    @Test
    @DisplayName("종성 다음에 모음이 오면 그 자음은 다음 음절 초성")
    void final_consonant_followed_by_vowel_becomes_next_initial() {
        // ekfk → ㄷ ㅏ ㄹ ㅏ — 'ㄹ' 뒤에 ㅏ 가 있으므로 종성으로 묶이지 않고 다음 음절 초성
        assertEquals("다라", HangulIme.filterVehicleNumber("ekfk"));
    }

    @Test
    @DisplayName("허용 외 문자는 제거")
    void strips_disallowed_chars() {
        assertEquals("12가3456", HangulIme.filterVehicleNumber("12@rk#3456!"));
        assertEquals("123456", HangulIme.filterVehicleNumber("12$%3456"));
    }

    @Test
    @DisplayName("하이픈·공백은 유지")
    void keeps_hyphen_and_space() {
        assertEquals("12-가-3456", HangulIme.filterVehicleNumber("12-rk-3456"));
        assertEquals("12 가 3456", HangulIme.filterVehicleNumber("12 rk 3456"));
    }

    @Test
    @DisplayName("대소문자 영문 모두 매핑 (대문자가 별도 매핑이 없으면 소문자로 폴백)")
    void uppercase_letters_fall_back_to_lowercase() {
        // 'A' = 'a' = ㅁ, 'K' = 'k' = ㅏ → 마
        assertEquals("12마3456", HangulIme.filterVehicleNumber("12AK3456"));
        // R 은 대문자 매핑 ㄲ, K 는 소문자 매핑 ㅏ → 까
        assertEquals("12까3456", HangulIme.filterVehicleNumber("12RK3456"));
    }

    @Test
    @DisplayName("자음만/모음만 입력 시 조합 안 되고 자모로 남음")
    void unmatched_jamo_stays_as_jamo() {
        // "rr" → ㄱㄱ — 둘 다 초성 자음, 모음 없음 → 조합 안됨
        String result = HangulIme.filterVehicleNumber("rr");
        assertEquals("ㄱㄱ", result);
    }

    @Test
    @DisplayName("null·빈 문자열 처리")
    void null_and_empty() {
        assertNull(HangulIme.filterVehicleNumber(null));
        assertEquals("", HangulIme.filterVehicleNumber(""));
    }

    // -------- convertChunk: IME 가 commit 한 chunk 는 재변환되지 않아야 --------

    @Test
    @DisplayName("convertChunk — 이미 한글 자모는 그대로 (IME 입력 보호)")
    void convertChunk_keeps_hangul_jamo_as_is() {
        assertEquals("ㄱ", HangulIme.convertChunk("ㄱ"));
        assertEquals("ㄲ", HangulIme.convertChunk("ㄲ"));
        assertEquals("ㅏ", HangulIme.convertChunk("ㅏ"));
    }

    @Test
    @DisplayName("convertChunk — 한글 음절은 그대로")
    void convertChunk_keeps_hangul_syllable_as_is() {
        assertEquals("가", HangulIme.convertChunk("가"));
        assertEquals("12가3456", HangulIme.convertChunk("12가3456"));
    }

    @Test
    @DisplayName("convertChunk — 영문은 자모로 변환")
    void convertChunk_maps_latin_to_jamo() {
        assertEquals("ㄱ", HangulIme.convertChunk("r"));
        assertEquals("ㄲ", HangulIme.convertChunk("R"));
        assertEquals("ㅏ", HangulIme.convertChunk("k"));
        assertEquals("ㄱㅏ", HangulIme.convertChunk("rk"));
    }

    @Test
    @DisplayName("compose — 인접 자모만 음절로, 그 외는 그대로")
    void compose_combines_only_adjacent_jamo() {
        assertEquals("가", HangulIme.compose("ㄱㅏ"));
        assertEquals("ㄱ", HangulIme.compose("ㄱ"));
        assertEquals("12가3", HangulIme.compose("12ㄱㅏ3"));
    }
}
