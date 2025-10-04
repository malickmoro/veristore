package com.theplutushome.veristore.service;

import com.theplutushome.veristore.domain.CustomerProfile;
import com.theplutushome.veristore.domain.InvoiceRecord;
import com.theplutushome.veristore.domain.InvoiceStatus;
import com.theplutushome.veristore.domain.OrderRecord;
import com.theplutushome.veristore.domain.PaymentStatus;
import com.theplutushome.veristore.domain.ServiceDefinition;
import com.theplutushome.veristore.domain.ServiceKey;

import javax.enterprise.context.ApplicationScoped;
import javax.inject.Inject;
import java.io.Serializable;
import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.time.temporal.ChronoUnit;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.logging.Logger;

@ApplicationScoped
public class OrderService implements Serializable {

    private static final Logger LOGGER = Logger.getLogger(OrderService.class.getName());

    @Inject
    private PinVault pinVault;

    @Inject
    private ServiceCatalog serviceCatalog;

    @Inject
    private DeliveryOutbox deliveryOutbox;

    private final Map<String, OrderRecord> orders = new ConcurrentHashMap<>();
    private final Map<String, InvoiceRecord> invoices = new ConcurrentHashMap<>();

    private final AtomicInteger orderSequence = new AtomicInteger(1000);
    private final AtomicInteger invoiceSequence = new AtomicInteger(5000);

    public OrderRecord completePurchase(ServiceKey key, CustomerProfile customer, int quantity) {
        validateQuantity(quantity);
        ServiceDefinition definition = serviceCatalog.getDefinition(key);
        BigDecimal unitPrice = definition.getUnitPrice();
        BigDecimal total = unitPrice.multiply(BigDecimal.valueOf(quantity));
        List<String> pins = pinVault.dispense(key, quantity);
        LocalDateTime now = LocalDateTime.now().truncatedTo(ChronoUnit.SECONDS);
        OrderRecord order = new OrderRecord(generateOrderId(), customer, definition, quantity, unitPrice, total, now, PaymentStatus.PAID, pins);
        orders.put(order.getOrderId(), order);
        deliveryOutbox.record(order.getOrderId(), order.getMaskedPins());
        LOGGER.info(() -> String.format("Order %s completed for %s", order.getOrderId(), customer.getFullName()));
        return order;
    }

    public InvoiceRecord createInvoice(ServiceKey key, CustomerProfile customer, int quantity) {
        validateQuantity(quantity);
        ServiceDefinition definition = serviceCatalog.getDefinition(key);
        BigDecimal unitPrice = definition.getUnitPrice();
        BigDecimal total = unitPrice.multiply(BigDecimal.valueOf(quantity));
        LocalDateTime now = LocalDateTime.now().truncatedTo(ChronoUnit.SECONDS);
        LocalDateTime due = now.plusDays(7);
        InvoiceRecord invoice = new InvoiceRecord(generateInvoiceNumber(), customer, definition, quantity, unitPrice, total, now, due);
        invoices.put(invoice.getInvoiceNumber(), invoice);
        LOGGER.info(() -> String.format("Invoice %s created for %s", invoice.getInvoiceNumber(), customer.getFullName()));
        return invoice;
    }

    public Optional<OrderRecord> redeemInvoice(String invoiceNumber) {
        InvoiceRecord invoice = invoices.get(invoiceNumber);
        if (invoice == null) {
            return Optional.empty();
        }
        if (invoice.getStatus() != InvoiceStatus.PENDING) {
            return Optional.ofNullable(invoice.getOrderId()).map(orders::get);
        }
        ServiceDefinition definition = invoice.getServiceDefinition();
        List<String> pins = pinVault.dispense(definition.getKey(), invoice.getQuantity());
        LocalDateTime now = LocalDateTime.now().truncatedTo(ChronoUnit.SECONDS);
        OrderRecord order = new OrderRecord(generateOrderId(), invoice.getCustomer(), definition, invoice.getQuantity(), invoice.getUnitPrice(), invoice.getTotalAmount(), now, PaymentStatus.PAID, pins);
        orders.put(order.getOrderId(), order);
        invoice.markPaid(order.getOrderId(), now);
        deliveryOutbox.record(order.getOrderId(), order.getMaskedPins());
        LOGGER.info(() -> String.format("Invoice %s redeemed and order %s completed", invoiceNumber, order.getOrderId()));
        return Optional.of(order);
    }

    public Optional<InvoiceRecord> findInvoice(String invoiceNumber) {
        return Optional.ofNullable(invoiceNumber)
            .map(invoices::get);
    }

    public Optional<OrderRecord> findOrder(String orderId) {
        return Optional.ofNullable(orderId)
            .map(orders::get);
    }

    public List<OrderRecord> findHistory(String email, String phone) {
        String normalizedEmail = normalize(email);
        String normalizedPhone = normalize(phone);
        List<OrderRecord> matches = new ArrayList<>();
        for (OrderRecord order : orders.values()) {
            boolean emailMatches = normalizedEmail != null && normalize(order.getCustomer().getEmail()).equals(normalizedEmail);
            boolean phoneMatches = normalizedPhone != null && normalize(order.getCustomer().getPhone()).equals(normalizedPhone);
            if ((normalizedEmail != null && emailMatches) || (normalizedPhone != null && phoneMatches)) {
                matches.add(order);
            }
        }
        matches.sort(Comparator.comparing(OrderRecord::getCreatedAt).reversed());
        return matches;
    }

    private void validateQuantity(int quantity) {
        if (quantity <= 0) {
            throw new IllegalArgumentException("Quantity must be positive");
        }
    }

    private String normalize(String value) {
        if (value == null) {
            return null;
        }
        return value.replaceAll("\\s+", "").toLowerCase();
    }

    private String generateOrderId() {
        return String.format("ORD-%05d", orderSequence.incrementAndGet());
    }

    private String generateInvoiceNumber() {
        return String.format("INV-%05d", invoiceSequence.incrementAndGet());
    }
}
