package com.theplutushome.veristore.domain;

import java.io.Serializable;
import java.util.Objects;

public record Price(String currency, long amountMinor) implements Serializable {

    public Price {
        Objects.requireNonNull(currency, "currency");
    }
}
