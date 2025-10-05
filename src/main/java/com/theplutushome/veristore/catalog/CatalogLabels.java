package com.theplutushome.veristore.catalog;

import com.theplutushome.veristore.domain.CitizenTier;
import com.theplutushome.veristore.domain.UpdateType;
import jakarta.annotation.Nullable;

public final class CatalogLabels {

    private CatalogLabels() {
    }

    public static String citizenTierLabel(@Nullable CitizenTier tier) {
        if (tier == null) {
            return "";
        }
        return switch (tier) {
            case STANDARD -> "Standard";
            case PREMIUM -> "Premium";
        };
    }

    public static String updateLabel(UpdateType type) {
        return switch (type) {
            case PERSONAL_INFORMATION -> "Personal Information Update";
            case DOB -> "Date of Birth Update";
            case PICTURE -> "Picture Update";
            case NATIONALITY -> "Nationality Update";
            case SECONDARY_DATA -> "Secondary Data Update";
        };
    }
}
