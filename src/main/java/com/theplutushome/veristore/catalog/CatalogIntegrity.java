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
        var standard = EnrollmentSku.bySku("CFS").orElseThrow();
        if (standard.price().amountMinor() != 0) {
            throw new IllegalStateException("Citizen Standard First Issuance must be free.");
        }
    }
}
