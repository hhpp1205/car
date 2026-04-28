package com.jeonbuk.repair.util;

import java.text.NumberFormat;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.Locale;

/**
 * 화면 표시용 포매터.
 */
public final class Formatters {

    public static final DateTimeFormatter DATE  = DateTimeFormatter.ofPattern("yyyy-MM-dd");
    private static final NumberFormat KRW = NumberFormat.getNumberInstance(Locale.KOREA);

    private Formatters() {}

    public static String date(LocalDate d) {
        return d == null ? "" : DATE.format(d);
    }

    public static LocalDate parseDate(String s) {
        if (s == null || s.isBlank()) return null;
        return LocalDate.parse(s.trim(), DATE);
    }

    public static String money(Integer v) {
        return v == null ? "" : KRW.format(v);
    }

    public static String money(int v) {
        return KRW.format(v);
    }

    /** 숫자 외 문자 제거 후 Integer 파싱. 빈값 → null */
    public static Integer parseMoney(String s) {
        if (s == null) return null;
        String digits = s.replaceAll("[^0-9-]", "");
        if (digits.isBlank()) return null;
        return Integer.valueOf(digits);
    }

    /** 11자리 숫자 → 010-0000-0000. 그 외 길이는 그대로 반환. */
    public static String formatPhone(String input) {
        if (input == null) return null;
        String digits = input.replaceAll("\\D", "");
        if (digits.length() == 11) {
            return digits.substring(0, 3) + "-" + digits.substring(3, 7) + "-" + digits.substring(7);
        }
        if (digits.length() == 10) {
            return digits.substring(0, 3) + "-" + digits.substring(3, 6) + "-" + digits.substring(6);
        }
        return input;
    }

    /**
     * 입력 즉시 포매팅용. 숫자만 추출 후 진행형으로 하이픈 삽입.
     * 최대 11자리, 그 이상은 잘림. 멱등(idempotent).
     *
     * <ul>
     *   <li>1~3자리 → "010"</li>
     *   <li>4~7자리 → "010-1234"</li>
     *   <li>8~11자리 → "010-1234-5678"</li>
     * </ul>
     */
    public static String formatPhoneAsType(String input) {
        if (input == null) return null;
        String digits = input.replaceAll("\\D", "");
        if (digits.length() > 11) digits = digits.substring(0, 11);
        if (digits.isEmpty()) return "";
        if (digits.length() <= 3) return digits;
        if (digits.length() <= 7) {
            return digits.substring(0, 3) + "-" + digits.substring(3);
        }
        return digits.substring(0, 3) + "-" + digits.substring(3, 7) + "-" + digits.substring(7);
    }
}
