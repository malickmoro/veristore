package com.theplutushome.veristore.util;

public final class Masker {

    private Masker() {
    }

    public static String mask(String value) {
        if (value == null || value.isBlank()) {
            return "";
        }
        String trimmed = value.trim();
        int length = trimmed.length();
        if (length <= 4) {
            return "*".repeat(length);
        }
        int visible = Math.min(4, length);
        String visiblePart = trimmed.substring(length - visible);
        String masked = "*".repeat(length - visible);
        return masked + visiblePart;
    }
}
