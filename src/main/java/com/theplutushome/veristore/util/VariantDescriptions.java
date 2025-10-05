package com.theplutushome.veristore.util;

import com.theplutushome.veristore.catalog.EnrollmentSku;
import com.theplutushome.veristore.catalog.ProductFamily;
import com.theplutushome.veristore.catalog.VerificationSku;

import java.util.Locale;
import java.util.Objects;
import java.util.Optional;

public final class VariantDescriptions {

    private VariantDescriptions() {
    }

    public static String describe(ProductFamily family, String sku) {
        if (family == null || sku == null) {
            return "";
        }
        return switch (family) {
            case VERIFICATION -> describeVerification(sku);
            case ENROLLMENT -> describeEnrollment(sku);
        };
    }

    private static String describeVerification(String sku) {
        return VerificationSku.bySku(sku)
            .map(v -> v.displayName)
            .orElse(Objects.toString(sku, ""));
    }

    private static String describeEnrollment(String sku) {
        Optional<EnrollmentSku> match = EnrollmentSku.bySku(sku);
        return match.map(value -> value.displayName)
            .orElse(prettify(sku));
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
