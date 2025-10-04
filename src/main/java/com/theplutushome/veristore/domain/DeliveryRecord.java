package com.theplutushome.veristore.domain;

import java.io.Serializable;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Objects;

public final class DeliveryRecord implements Serializable {

    private final String orderId;
    private final LocalDateTime deliveredAt;
    private final List<String> maskedPins;

    public DeliveryRecord(String orderId, LocalDateTime deliveredAt, List<String> maskedPins) {
        this.orderId = Objects.requireNonNull(orderId, "orderId");
        this.deliveredAt = Objects.requireNonNull(deliveredAt, "deliveredAt");
        this.maskedPins = List.copyOf(Objects.requireNonNull(maskedPins, "maskedPins"));
    }

    public String getOrderId() {
        return orderId;
    }

    public LocalDateTime getDeliveredAt() {
        return deliveredAt;
    }

    public List<String> getMaskedPins() {
        return maskedPins;
    }
}
