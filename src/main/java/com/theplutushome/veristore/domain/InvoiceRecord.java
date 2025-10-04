package com.theplutushome.veristore.domain;

import java.io.Serializable;
import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.Objects;

public final class InvoiceRecord implements Serializable {

    private final String invoiceNumber;
    private final CustomerProfile customer;
    private final ServiceDefinition serviceDefinition;
    private final int quantity;
    private final BigDecimal unitPrice;
    private final BigDecimal totalAmount;
    private final LocalDateTime createdAt;
    private final LocalDateTime dueAt;
    private InvoiceStatus status;
    private LocalDateTime paidAt;
    private String orderId;

    public InvoiceRecord(String invoiceNumber,
                         CustomerProfile customer,
                         ServiceDefinition serviceDefinition,
                         int quantity,
                         BigDecimal unitPrice,
                         BigDecimal totalAmount,
                         LocalDateTime createdAt,
                         LocalDateTime dueAt) {
        this.invoiceNumber = Objects.requireNonNull(invoiceNumber, "invoiceNumber");
        this.customer = Objects.requireNonNull(customer, "customer");
        this.serviceDefinition = Objects.requireNonNull(serviceDefinition, "serviceDefinition");
        this.quantity = quantity;
        this.unitPrice = Objects.requireNonNull(unitPrice, "unitPrice");
        this.totalAmount = Objects.requireNonNull(totalAmount, "totalAmount");
        this.createdAt = Objects.requireNonNull(createdAt, "createdAt");
        this.dueAt = Objects.requireNonNull(dueAt, "dueAt");
        this.status = InvoiceStatus.PENDING;
    }

    public String getInvoiceNumber() {
        return invoiceNumber;
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

    public LocalDateTime getDueAt() {
        return dueAt;
    }

    public InvoiceStatus getStatus() {
        return status;
    }

    public LocalDateTime getPaidAt() {
        return paidAt;
    }

    public String getOrderId() {
        return orderId;
    }

    public void markPaid(String orderId, LocalDateTime paidAt) {
        this.status = InvoiceStatus.PAID;
        this.orderId = orderId;
        this.paidAt = paidAt;
    }
}
