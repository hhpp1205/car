package com.jeonbuk.repair.util;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

import java.time.LocalDate;

import static org.junit.jupiter.api.Assertions.*;

class FormattersTest {

    @Nested
    @DisplayName("date / parseDate")
    class DateTests {

        @Test
        @DisplayName("date — null 은 빈 문자열로 포매팅")
        void date_null() {
            assertEquals("", Formatters.date(null));
        }

        @Test
        @DisplayName("date — yyyy-MM-dd 형식")
        void date_iso() {
            assertEquals("2026-04-24", Formatters.date(LocalDate.of(2026, 4, 24)));
        }

        @Test
        @DisplayName("parseDate — null/빈 문자열은 null")
        void parseDate_blank_returns_null() {
            assertNull(Formatters.parseDate(null));
            assertNull(Formatters.parseDate(""));
            assertNull(Formatters.parseDate("   "));
        }

        @Test
        @DisplayName("parseDate — 정상 ISO 문자열")
        void parseDate_iso() {
            assertEquals(LocalDate.of(2026, 4, 24), Formatters.parseDate("2026-04-24"));
        }
    }

    @Nested
    @DisplayName("money / parseMoney")
    class MoneyTests {

        @Test
        @DisplayName("money(Integer) — null 은 빈 문자열")
        void money_null() {
            assertEquals("", Formatters.money((Integer) null));
        }

        @Test
        @DisplayName("money — 한국어 천단위 콤마")
        void money_grouping() {
            assertEquals("1,234,567", Formatters.money(1234567));
            assertEquals("0", Formatters.money(0));
            assertEquals("-1,000", Formatters.money(-1000));
        }

        @Test
        @DisplayName("parseMoney — 콤마 제거")
        void parseMoney_with_comma() {
            assertEquals(1234567, Formatters.parseMoney("1,234,567"));
        }

        @Test
        @DisplayName("parseMoney — 그대로 숫자")
        void parseMoney_plain_digits() {
            assertEquals(1234567, Formatters.parseMoney("1234567"));
        }

        @Test
        @DisplayName("parseMoney — 음수 부호 보존")
        void parseMoney_negative() {
            assertEquals(-500, Formatters.parseMoney("-500"));
        }

        @Test
        @DisplayName("parseMoney — 통화 기호 등 비숫자 제거")
        void parseMoney_strip_non_digits() {
            assertEquals(1234, Formatters.parseMoney("₩1,234"));
        }

        @Test
        @DisplayName("parseMoney — null/빈 문자열/숫자 없음 → null")
        void parseMoney_null_returns() {
            assertNull(Formatters.parseMoney(null));
            assertNull(Formatters.parseMoney(""));
            assertNull(Formatters.parseMoney("   "));
            assertNull(Formatters.parseMoney("abc"));
        }
    }

    @Nested
    @DisplayName("formatPhone")
    class PhoneTests {

        @Test
        @DisplayName("11자리 숫자 → 010-XXXX-XXXX")
        void format_11_digits() {
            assertEquals("010-1234-5678", Formatters.formatPhone("01012345678"));
        }

        @Test
        @DisplayName("이미 하이픈 들어간 11자리도 정상 포매팅")
        void format_already_hyphenated() {
            assertEquals("010-1234-5678", Formatters.formatPhone("010-1234-5678"));
        }

        @Test
        @DisplayName("10자리는 3-3-4 분할")
        void format_10_digits() {
            assertEquals("021-234-5678", Formatters.formatPhone("0212345678"));
        }

        @Test
        @DisplayName("그 외 길이는 원문 유지")
        void format_other_lengths_kept_as_is() {
            assertEquals("12345", Formatters.formatPhone("12345"));
            assertEquals("abc", Formatters.formatPhone("abc"));
        }

        @Test
        @DisplayName("null → null")
        void format_null() {
            assertNull(Formatters.formatPhone(null));
        }
    }

    @Nested
    @DisplayName("formatPhoneAsType — 입력 즉시 포매팅")
    class PhoneAsTypeTests {

        @Test
        @DisplayName("3자리 이하는 그대로")
        void up_to_3_digits() {
            assertEquals("",   Formatters.formatPhoneAsType(""));
            assertEquals("0",  Formatters.formatPhoneAsType("0"));
            assertEquals("01", Formatters.formatPhoneAsType("01"));
            assertEquals("010", Formatters.formatPhoneAsType("010"));
        }

        @Test
        @DisplayName("4~7자리 → 010-XXXX")
        void four_to_seven_digits() {
            assertEquals("010-1",    Formatters.formatPhoneAsType("0101"));
            assertEquals("010-12",   Formatters.formatPhoneAsType("01012"));
            assertEquals("010-123",  Formatters.formatPhoneAsType("010123"));
            assertEquals("010-1234", Formatters.formatPhoneAsType("0101234"));
        }

        @Test
        @DisplayName("8~11자리 → 010-XXXX-XXXX")
        void eight_to_eleven_digits() {
            assertEquals("010-1234-5",    Formatters.formatPhoneAsType("01012345"));
            assertEquals("010-1234-56",   Formatters.formatPhoneAsType("010123456"));
            assertEquals("010-1234-567",  Formatters.formatPhoneAsType("0101234567"));
            assertEquals("010-1234-5678", Formatters.formatPhoneAsType("01012345678"));
        }

        @Test
        @DisplayName("11자리 초과는 잘림")
        void truncates_over_11_digits() {
            assertEquals("010-1234-5678", Formatters.formatPhoneAsType("0101234567899"));
        }

        @Test
        @DisplayName("이미 하이픈이 있어도 정상 (멱등)")
        void idempotent_with_hyphens() {
            assertEquals("010-1234-5678", Formatters.formatPhoneAsType("010-1234-5678"));
            // 두 번 적용해도 같은 결과
            assertEquals("010-1234-5678",
                Formatters.formatPhoneAsType(Formatters.formatPhoneAsType("01012345678")));
        }

        @Test
        @DisplayName("숫자가 아닌 문자는 무시")
        void strips_non_digits() {
            assertEquals("010-1234-5678", Formatters.formatPhoneAsType("010 1234 5678"));
            assertEquals("010-1234-5678", Formatters.formatPhoneAsType("abc010!@#1234%5678"));
        }

        @Test
        @DisplayName("null → null, 빈 문자열 → 빈 문자열")
        void null_and_empty() {
            assertNull(Formatters.formatPhoneAsType(null));
            assertEquals("", Formatters.formatPhoneAsType(""));
        }
    }
}
