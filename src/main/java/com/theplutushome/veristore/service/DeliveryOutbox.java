package com.theplutushome.veristore.service;

import com.theplutushome.veristore.model.DeliveryRecord;

import jakarta.enterprise.context.ApplicationScoped;
import java.io.Serializable;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

@ApplicationScoped
public class DeliveryOutbox implements Serializable {

    private final List<DeliveryRecord> deliveries = new ArrayList<>();

    public synchronized void record(String orderId, List<String> maskedPins) {
        deliveries.add(new DeliveryRecord(orderId, LocalDateTime.now(), maskedPins));
    }

    public synchronized List<DeliveryRecord> getDeliveries() {
        return Collections.unmodifiableList(new ArrayList<>(deliveries));
    }
}
