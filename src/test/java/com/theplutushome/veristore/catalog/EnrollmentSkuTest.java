package com.theplutushome.veristore.catalog;

import com.theplutushome.veristore.domain.CitizenshipType;

import org.junit.jupiter.api.Test;

import java.util.Arrays;
import java.util.List;
import java.util.stream.Collectors;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

class EnrollmentSkuTest {

    @Test
    void allSkusUnique() {
        long unique = Arrays.stream(EnrollmentSku.values())
            .map(sku -> sku.sku)
            .distinct()
            .count();
        assertEquals(EnrollmentSku.values().length, unique, "SKUs must be unique");
    }

    @Test
    void citizenFirstIssuanceSkusPresent() {
        var regular = EnrollmentSku.bySku("CA");
        assertTrue(regular.isPresent(), "CA SKU should exist");
        assertTrue(regular.orElseThrow().active, "Regular first issuance must be active");
        assertTrue(regular.orElseThrow().price().amountMinor() > 0, "Regular first issuance must cost more than zero");

        var premium = EnrollmentSku.bySku("PA");
        assertTrue(premium.isPresent(), "PA SKU should exist");
        assertTrue(premium.orElseThrow().active, "Premium first issuance must be active");
        assertTrue(premium.orElseThrow().price().amountMinor() >= regular.orElseThrow().price().amountMinor(),
            "Premium first issuance must be priced at least as high as regular");
    }

    @Test
    void nonCitizenRenewalsSorted() {
        List<EnrollmentSku> renewals = EnrollmentSku.renewalsFor(CitizenshipType.NON_CITIZEN);
        List<String> actualOrder = renewals.stream().map(sku -> sku.sku).collect(Collectors.toList());
        List<String> expectedOrder = List.of("RO", "RW", "RH", "RF");
        assertEquals(expectedOrder, actualOrder, "Non-citizen renewals must be sorted by duration");
    }
}
