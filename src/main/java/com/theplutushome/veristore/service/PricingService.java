package com.theplutushome.veristore.service;

import com.theplutushome.veristore.model.SubServiceRecord;
import com.theplutushome.veristore.model.catalog.EnrollmentSku;
import com.theplutushome.veristore.model.catalog.ProductKey;
import com.theplutushome.veristore.model.catalog.VerificationSku;
import com.theplutushome.veristore.model.Currency;
import com.theplutushome.veristore.model.Price;
import com.theplutushome.veristore.service.atlas.AtlasService;

import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;

import java.io.Serial;
import java.io.Serializable;
import java.math.BigDecimal;
import java.text.NumberFormat;
import java.util.Locale;
import java.util.logging.Level;
import java.util.logging.Logger;

@ApplicationScoped
public class PricingService implements Serializable {

    @Serial
    private static final long serialVersionUID = 1L;

    private static final Logger LOGGER = Logger.getLogger(PricingService.class.getName());

    @Inject
    private AtlasService atlasService;

    public Price get(ProductKey key) {
        if (key == null) {
            throw new IllegalArgumentException("key must not be null");
        }
        return switch (key.family()) {
            case VERIFICATION -> VerificationSku.bySku(key.sku())
                .orElseThrow(() -> new IllegalArgumentException("Unknown verification SKU: " + key.sku()))
                .price();
            case ENROLLMENT -> fetchEnrollmentPrice(key.sku());
        };
    }

    private Price fetchEnrollmentPrice(String sku) {
        EnrollmentSku enrollment = EnrollmentSku.bySku(sku)
            .orElseThrow(() -> new IllegalArgumentException("Unknown enrollment SKU: " + sku));

        SubServiceRecord serviceInfo = atlasService.getServiceInfo(enrollment.name());
        if (serviceInfo == null) {
            throw new IllegalStateException("Atlas did not return pricing for " + enrollment.name());
        }

        BigDecimal amount = BigDecimal.valueOf(serviceInfo.amount());
        return Price.of(serviceInfo.currency(), amount);
    }

    public String format(Price price) {
        NumberFormat formatter = NumberFormat.getCurrencyInstance(localeFor(price.currency()));
        formatter.setCurrency(java.util.Currency.getInstance(isoCode(price.currency())));
        BigDecimal major = BigDecimal.valueOf(price.amountMinor(), 2);
        return formatter.format(major);
    }

    private Locale localeFor(Currency currency) {
        return switch (currency) {
            case USD -> Locale.US;
            case GHS -> new Locale("en", "GH");
        };
    }

    private String isoCode(Currency currency) {
        return switch (currency) {
            case USD -> "USD";
            case GHS -> "GHS";
        };
    }
}
