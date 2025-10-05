package com.theplutushome.veristore.catalog;

import jakarta.annotation.PostConstruct;
import jakarta.enterprise.context.ApplicationScoped;

import java.io.Serializable;
import java.util.HashSet;
import java.util.Set;

@ApplicationScoped
public class CatalogIntegrity implements Serializable {

    @PostConstruct
    public void verify() {
        Set<String> seen = new HashSet<>();
        for (EnrollmentSku sku : EnrollmentSku.values()) {
            if (!seen.add(sku.sku)) {
                throw new IllegalStateException("Duplicate Enrollment SKU: " + sku.sku);
            }
        }
        for (VerificationSku sku : VerificationSku.values()) {
            if (!seen.add(sku.sku)) {
                throw new IllegalStateException("SKU clashes with Enrollment: " + sku.sku);
            }
        }
        var regular = EnrollmentSku.bySku("CA").orElseThrow(() ->
            new IllegalStateException("Missing citizen regular first issuance SKU"));
        var premium = EnrollmentSku.bySku("PA").orElseThrow(() ->
            new IllegalStateException("Missing citizen premium first issuance SKU"));
        if (!regular.active || !premium.active) {
            throw new IllegalStateException("Citizen first issuance SKUs must be active.");
        }
        if (regular.price().amountMinor() <= 0) {
            throw new IllegalStateException("Citizen regular first issuance must have a positive fee.");
        }
        if (premium.price().amountMinor() < regular.price().amountMinor()) {
            throw new IllegalStateException("Premium first issuance must cost at least the regular tier.");
        }
    }
}
