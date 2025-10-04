package com.theplutushome.veristore.domain;

import com.theplutushome.veristore.util.PinMasker;

import java.io.Serializable;
import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.Collections;
import java.util.List;
import java.util.Objects;
import java.util.stream.Collectors;

public final class OrderRecord implements Serializable {

    private final String orderId;
    private final CustomerProfile customer;
    private final ServiceDefinition serviceDefinition;
    private final int quantity;
    private final BigDecimal unitPrice;
    private final BigDecimal totalAmount;
    private final LocalDateTime createdAt;
    private final PaymentStatus paymentStatus;
    private final List<String> pins;
    private final List<String> maskedPins;

    public OrderRecord(String orderId,
                       CustomerProfile customer,
                       ServiceDefinition serviceDefinition,
                       int quantity,
                       BigDecimal unitPrice,
                       BigDecimal totalAmount,
                       LocalDateTime createdAt,
                       PaymentStatus paymentStatus,
                       List<String> pins) {
        this.orderId = Objects.requireNonNull(orderId, "orderId");
        this.customer = Objects.requireNonNull(customer, "customer");
        this.serviceDefinition = Objects.requireNonNull(serviceDefinition, "serviceDefinition");
        this.quantity = quantity;
        this.unitPrice = Objects.requireNonNull(unitPrice, "unitPrice");
        this.totalAmount = Objects.requireNonNull(totalAmount, "totalAmount");
        this.createdAt = Objects.requireNonNull(createdAt, "createdAt");
        this.paymentStatus = Objects.requireNonNull(paymentStatus, "paymentStatus");
        List<String> safePins = List.copyOf(Objects.requireNonNull(pins, "pins"));
        this.pins = Collections.unmodifiableList(safePins);
        this.maskedPins = Collections.unmodifiableList(safePins.stream().map(PinMasker::mask).collect(Collectors.toList()));
    }

    public String getOrderId() {
        return orderId;
    }

    public CustomerProfile getCustomer() {
        return customer;
    }

    public ServiceDefinition getServiceDefinition() {
        return serviceDefinition;
    }

    public int getQuantity() {
        return quantity;
    }

    public BigDecimal getUnitPrice() {
        return unitPrice;
    }

    public BigDecimal getTotalAmount() {
        return totalAmount;
    }

    public LocalDateTime getCreatedAt() {
        return createdAt;
    }

    public PaymentStatus getPaymentStatus() {
        return paymentStatus;
    }

    public List<String> getMaskedPins() {
        return maskedPins;
    }
}
