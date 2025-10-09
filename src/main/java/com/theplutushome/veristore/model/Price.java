package com.theplutushome.veristore.model;

import java.io.Serializable;
import java.util.Objects;

public record Price(Currency currency, long amountMinor) implements Serializable {

    public Price {
        Objects.requireNonNull(currency, "currency");
    }

    public static Price ofMajor(Currency currency, int majorUnits) {
        return ofMajor(currency, (long) majorUnits);
    }

    public static Price ofMajor(Currency currency, long majorUnits) {
        Objects.requireNonNull(currency, "currency");
        return new Price(currency, majorUnits * 100L);
    }
}
