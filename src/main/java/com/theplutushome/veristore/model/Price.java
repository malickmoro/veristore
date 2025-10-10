package com.theplutushome.veristore.model;

import java.io.Serializable;
import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.Objects;

public record Price(Currency currency, long amountMinor) implements Serializable {

    public Price  {
        Objects.requireNonNull(currency, "currency");
    }

    public static Price ofMajor(Currency currency, int majorUnits) {
        return ofMajor(currency, (long) majorUnits);
    }

    public static Price ofMajor(Currency currency, long majorUnits) {
        Objects.requireNonNull(currency, "currency");
        return new Price(currency, majorUnits * 100L);
    }

    public static Price of(Currency currency, BigDecimal majorUnits) {
        Objects.requireNonNull(currency, "currency");
        Objects.requireNonNull(majorUnits, "majorUnits");
        BigDecimal normalized = majorUnits.setScale(2, RoundingMode.HALF_UP);
        long minorUnits = normalized.movePointRight(2).longValueExact();
        return new Price(currency, minorUnits);
    }
}
