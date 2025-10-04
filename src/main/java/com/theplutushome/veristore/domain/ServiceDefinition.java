package com.theplutushome.veristore.domain;

import java.io.Serializable;
import java.math.BigDecimal;
import java.util.Objects;

public final class ServiceDefinition implements Serializable {

    private final ServiceKey key;
    private final String name;
    private final String description;
    private final BigDecimal unitPrice;

    public ServiceDefinition(ServiceKey key, String name, String description, BigDecimal unitPrice) {
        this.key = Objects.requireNonNull(key, "key");
        this.name = Objects.requireNonNull(name, "name");
        this.description = Objects.requireNonNull(description, "description");
        this.unitPrice = Objects.requireNonNull(unitPrice, "unitPrice");
    }

    public ServiceKey getKey() {
        return key;
    }

    public String getName() {
        return name;
    }

    public String getDescription() {
        return description;
    }

    public BigDecimal getUnitPrice() {
        return unitPrice;
    }
}
