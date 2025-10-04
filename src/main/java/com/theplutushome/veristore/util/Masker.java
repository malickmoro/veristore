package com.theplutushome.veristore.util;

public final class Masker {

    private static final char MASK_CHAR = '\u2022';

    private Masker() {
    }

    public static String mask(String code) {
        if (code == null) {
            return "";
        }
        String trimmed = code.trim();
        int length = trimmed.length();
        if (length == 0) {
            return "";
        }
        if (length <= 4) {
            return String.valueOf(MASK_CHAR).repeat(length);
        }
        String prefix = trimmed.substring(0, 2);
        String suffix = trimmed.substring(length - 2);
        String middle = String.valueOf(MASK_CHAR).repeat(length - 4);
        return prefix + middle + suffix;
    }
}
