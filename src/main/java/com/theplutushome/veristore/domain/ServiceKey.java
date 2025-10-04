package com.theplutushome.veristore.domain;

import java.io.Serializable;
import java.util.Objects;

public record ServiceKey(PinCategory category, String variant) implements Serializable {

    public ServiceKey {
        Objects.requireNonNull(category, "category");
        Objects.requireNonNull(variant, "variant");
    }

    public String code() {
        return category.name() + "-" + variant;
    }
}
