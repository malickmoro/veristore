package com.theplutushome.veristore.model.catalog;

import java.io.Serializable;
import java.util.Objects;

public record ProductKey(ProductFamily family, String sku) implements Serializable {

    public ProductKey {
        Objects.requireNonNull(family, "family");
        Objects.requireNonNull(sku, "sku");
    }
}
