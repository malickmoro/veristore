package com.theplutushome.veristore.domain;

import java.time.LocalDateTime;
import java.util.List;

/**
 * Represents a delivery event containing masked PINs sent to a customer.
 */
public record DeliveryRecord(String orderId, LocalDateTime timestamp, List<String> maskedPins) {

    public DeliveryRecord {
        maskedPins = List.copyOf(maskedPins);
    }
}
