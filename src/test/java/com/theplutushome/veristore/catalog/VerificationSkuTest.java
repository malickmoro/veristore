package com.theplutushome.veristore.catalog;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertTrue;

class VerificationSkuTest {

    @Test
    void vSkusPresent() {
        assertSkuPresent("Y1");
        assertSkuPresent("Y2");
        assertSkuPresent("Y3");
    }

    private void assertSkuPresent(String sku) {
        var match = VerificationSku.bySku(sku);
        assertTrue(match.isPresent(), () -> "Expected verification SKU " + sku + " to exist");
        assertTrue(match.orElseThrow().price().amountMinor() > 0, () -> "SKU " + sku + " should have non-zero price");
    }
}
