package com.theplutushome.veristore.model.catalog;

import com.theplutushome.veristore.model.Currency;
import com.theplutushome.veristore.model.Price;

import java.io.Serializable;
import java.util.Arrays;
import java.util.Map;
import java.util.Optional;
import java.util.stream.Collectors;

public enum VerificationSku implements Serializable {
    Y1("Y1", "Verification PIN — 1 Year", Currency.USD, 500, VerificationSkuCategory.DURATION),
    Y2("Y2", "Verification PIN — 2 Years", Currency.USD, 900, VerificationSkuCategory.DURATION),
    Y3("Y3", "Verification PIN — 3 Years", Currency.USD, 1200, VerificationSkuCategory.DURATION),
    MOBILE("MOBILE", "Mobile Verification PIN", Currency.USD, 150, VerificationSkuCategory.CHANNEL),
    WEB("WEB", "Web Verification PIN", Currency.USD, 150, VerificationSkuCategory.CHANNEL);

    public final String sku;
    public final String displayName;
    public final Currency currency;
    public final int priceMajor;
    public final VerificationSkuCategory category;

    VerificationSku(String sku, String displayName, Currency currency, int priceMajor, VerificationSkuCategory category) {
        this.sku = sku;
        this.displayName = displayName;
        this.currency = currency;
        this.priceMajor = priceMajor;
        this.category = category;
    }

    public String getSku() {
        return sku;
    }

    public String getDisplayName() {
        return displayName;
    }

    public Currency getCurrency() {
        return currency;
    }

    public int getPriceMajor() {
        return priceMajor;
    }

    public VerificationSkuCategory getCategory() {
        return category;
    }

    public Price price() {
        return Price.ofMajor(currency, priceMajor);
    }

    private static final Map<String, VerificationSku> BY_SKU = Arrays.stream(values())
        .collect(Collectors.toUnmodifiableMap(v -> v.sku, v -> v));

    public static Optional<VerificationSku> bySku(String sku) {
        return Optional.ofNullable(BY_SKU.get(sku));
    }

    public enum VerificationSkuCategory {
        DURATION,
        CHANNEL
    }
}
