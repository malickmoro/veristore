package com.theplutushome.veristore.util;

import com.theplutushome.veristore.domain.EnrollmentType;
import com.theplutushome.veristore.domain.PinCategory;

import java.util.Locale;
import java.util.Objects;

public final class VariantDescriptions {

    private VariantDescriptions() {
    }

    public static String describe(PinCategory category, String variant) {
        if (category == null || variant == null) {
            return "";
        }
        return switch (category) {
            case VERIFICATION -> describeVerification(variant);
            case ENROLLMENT -> describeEnrollment(variant);
        };
    }

    public static String describe(EnrollmentType type) {
        if (type == null) {
            return "";
        }
        return prettify(type.name());
    }

    private static String describeVerification(String variant) {
        return switch (variant) {
            case "Y1" -> "Verification PIN (1 year)";
            case "Y2" -> "Verification PIN (2 years)";
            case "Y3" -> "Verification PIN (3 years)";
            default -> Objects.toString(variant, "");
        };
    }

    private static String describeEnrollment(String variant) {
        try {
            EnrollmentType type = EnrollmentType.valueOf(variant);
            return describe(type);
        } catch (IllegalArgumentException ex) {
            return prettify(variant);
        }
    }

    private static String prettify(String value) {
        if (value == null) {
            return "";
        }
        String normalized = value.toLowerCase(Locale.ROOT).replace('_', ' ');
        String[] parts = normalized.split(" ");
        StringBuilder builder = new StringBuilder();
        for (String part : parts) {
            if (part.isBlank()) {
                continue;
            }
            if (builder.length() > 0) {
                builder.append(' ');
            }
            builder.append(part.substring(0, 1).toUpperCase(Locale.ROOT));
            if (part.length() > 1) {
                builder.append(part.substring(1));
            }
        }
        return builder.toString();
    }
}
