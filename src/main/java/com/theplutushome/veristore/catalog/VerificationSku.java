package com.theplutushome.veristore.catalog;

import com.theplutushome.veristore.domain.Currency;
import com.theplutushome.veristore.domain.Price;

import java.io.Serializable;
import java.util.Arrays;
import java.util.Map;
import java.util.Optional;
import java.util.stream.Collectors;

public enum VerificationSku implements Serializable {
    Y1("Y1", "Verification PIN — 1 Year", Currency.USD, 500),
    Y2("Y2", "Verification PIN — 2 Years", Currency.USD, 900),
    Y3("Y3", "Verification PIN — 3 Years", Currency.USD, 1200);

    public final String sku;
    public final String displayName;
    public final Currency currency;
    public final int priceMajor;

    VerificationSku(String sku, String displayName, Currency currency, int priceMajor) {
        this.sku = sku;
        this.displayName = displayName;
        this.currency = currency;
        this.priceMajor = priceMajor;
    }

    public Price price() {
        return Price.ofMajor(currency, priceMajor);
    }

    private static final Map<String, VerificationSku> BY_SKU = Arrays.stream(values())
        .collect(Collectors.toUnmodifiableMap(v -> v.sku, v -> v));

    public static Optional<VerificationSku> bySku(String sku) {
        return Optional.ofNullable(BY_SKU.get(sku));
    }
}
