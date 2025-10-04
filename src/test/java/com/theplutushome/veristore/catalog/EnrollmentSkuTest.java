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
    void cfsIsFree() {
        var cfs = EnrollmentSku.bySku("CFS");
        assertTrue(cfs.isPresent(), "CFS SKU should exist");
        assertEquals(0L, cfs.orElseThrow().price().amountMinor(), "Citizen Standard First Issuance must be free");
    }

    @Test
    void nonCitizenRenewalsSorted() {
        List<EnrollmentSku> renewals = EnrollmentSku.renewalsFor(CitizenshipType.NON_CITIZEN);
        List<String> actualOrder = renewals.stream().map(sku -> sku.sku).collect(Collectors.toList());
        List<String> expectedOrder = List.of("RO", "RW", "RH", "RF");
        assertEquals(expectedOrder, actualOrder, "Non-citizen renewals must be sorted by duration");
    }
}
