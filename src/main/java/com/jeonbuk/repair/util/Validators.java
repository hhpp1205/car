package com.jeonbuk.repair.util;

import java.util.regex.Pattern;

public final class Validators {

    private static final Pattern HANGUL = Pattern.compile(".*[가-힣]+.*");

    private Validators() {}

    /** 차량번호는 한글이 반드시 1자 이상 포함되어야 함. */
    public static boolean isValidVehicleNumber(String s) {
        return s != null && !s.isBlank() && HANGUL.matcher(s).matches();
    }

    public static boolean isBlank(String s) {
        return s == null || s.isBlank();
    }
}
