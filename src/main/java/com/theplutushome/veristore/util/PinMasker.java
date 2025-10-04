package com.theplutushome.veristore.util;

public final class PinMasker {

    private PinMasker() {
    }

    public static String mask(String pin) {
        if (pin == null || pin.isEmpty()) {
            return "";
        }
        int length = pin.length();
        if (length <= 4) {
            return "*".repeat(length);
        }
        int maskedCount = Math.max(0, length - 4);
        StringBuilder sb = new StringBuilder();
        sb.append(pin, 0, Math.min(2, length));
        sb.append("*".repeat(maskedCount));
        sb.append(pin.substring(length - Math.min(2, length)));
        return sb.toString();
    }
}
