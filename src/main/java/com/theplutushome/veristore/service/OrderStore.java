package com.theplutushome.veristore.service;

import com.theplutushome.veristore.domain.Contact;
import com.theplutushome.veristore.domain.DeliveryPrefs;
import com.theplutushome.veristore.domain.InvoiceStatus;
import com.theplutushome.veristore.domain.ServiceKey;

import javax.enterprise.context.ApplicationScoped;
import java.io.Serial;
import java.io.Serializable;
import java.time.Instant;
import java.time.Year;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicInteger;

@ApplicationScoped
public class OrderStore implements Serializable {

    @Serial
    private static final long serialVersionUID = 1L;

    private final Map<String, Order> ordersById = new ConcurrentHashMap<>();
    private final Map<String, Invoice> invoicesByNo = new ConcurrentHashMap<>();
    private final AtomicInteger orderSequence = new AtomicInteger();
    private final AtomicInteger invoiceSequence = new AtomicInteger();

    public String createOrder(ServiceKey key,
                              int quantity,
                              Contact contact,
                              DeliveryPrefs deliveryPrefs,
                              long totalMinor,
                              String currency,
                              List<String> codes) {
        Objects.requireNonNull(key, "key");
        Objects.requireNonNull(contact, "contact");
        Objects.requireNonNull(deliveryPrefs, "deliveryPrefs");
        Objects.requireNonNull(codes, "codes");
        if (quantity <= 0) {
            throw new IllegalArgumentException("quantity must be positive");
        }
        String id = nextOrderId();
        Order order = new Order(id, key, quantity, contact, deliveryPrefs, totalMinor, currency, Instant.now(), codes);
        ordersById.put(id, order);
        return id;
    }

    public String createInvoice(ServiceKey key,
                                int quantity,
                                Contact contact,
                                DeliveryPrefs deliveryPrefs,
                                long totalMinor,
                                String currency) {
        Objects.requireNonNull(key, "key");
        Objects.requireNonNull(contact, "contact");
        Objects.requireNonNull(deliveryPrefs, "deliveryPrefs");
        if (quantity <= 0) {
            throw new IllegalArgumentException("quantity must be positive");
        }
        String invoiceNo = nextInvoiceNo();
        Invoice invoice = new Invoice(invoiceNo, key, quantity, contact, deliveryPrefs, totalMinor, currency, InvoiceStatus.PENDING, Instant.now(), Collections.emptyList());
        invoicesByNo.put(invoiceNo, invoice);
        return invoiceNo;
    }

    public Optional<Invoice> findInvoice(String invoiceNo) {
        if (invoiceNo == null) {
            return Optional.empty();
        }
        return Optional.ofNullable(invoicesByNo.get(invoiceNo));
    }

    public List<Order> findOrdersByContact(Contact contact) {
        Objects.requireNonNull(contact, "contact");
        List<Order> matches = new ArrayList<>();
        for (Order order : ordersById.values()) {
            if (matches(contact, order.getContact())) {
                matches.add(order);
            }
        }
        matches.sort((a, b) -> b.getCreated().compareTo(a.getCreated()));
        return List.copyOf(matches);
    }

    public Optional<Invoice> markInvoicePaid(String invoiceNo, List<String> deliveredCodes) {
        Objects.requireNonNull(deliveredCodes, "deliveredCodes");
        return findInvoice(invoiceNo).map(invoice -> {
            synchronized (invoice) {
                if (invoice.getStatus() != InvoiceStatus.PAID) {
                    invoice.setStatus(InvoiceStatus.PAID);
                    invoice.setCodesIfDelivered(deliveredCodes);
                }
            }
            return invoice;
        });
    }

    public Optional<Invoice> markInvoiceCancelled(String invoiceNo) {
        return findInvoice(invoiceNo).map(invoice -> {
            synchronized (invoice) {
                invoice.setStatus(InvoiceStatus.CANCELLED);
            }
            return invoice;
        });
    }

    public Optional<Order> findOrder(String orderId) {
        if (orderId == null) {
            return Optional.empty();
        }
        return Optional.ofNullable(ordersById.get(orderId));
    }

    private boolean matches(Contact query, Contact stored) {
        if (stored == null) {
            return false;
        }
        boolean emailMatch = equalsIgnoreCaseAndWhitespace(query.email(), stored.email());
        boolean phoneMatch = equalsIgnoreCaseAndWhitespace(query.msisdn(), stored.msisdn());
        return emailMatch || phoneMatch;
    }

    private boolean equalsIgnoreCaseAndWhitespace(String left, String right) {
        if (left == null || right == null) {
            return false;
        }
        return normalize(left).equals(normalize(right));
    }

    private String normalize(String value) {
        return value == null ? "" : value.replaceAll("\\s+", "").toLowerCase();
    }

    private String nextOrderId() {
        int sequence = orderSequence.incrementAndGet();
        return String.format("ORD-%d-%05d", Year.now().getValue(), sequence);
    }

    private String nextInvoiceNo() {
        int sequence = invoiceSequence.incrementAndGet();
        return String.format("INV-%d-%05d", Year.now().getValue(), sequence);
    }

    public static final class Order implements Serializable {

        @Serial
        private static final long serialVersionUID = 1L;

        private final String id;
        private final ServiceKey key;
        private final int qty;
        private final Contact contact;
        private final DeliveryPrefs deliveryPrefs;
        private final long totalMinor;
        private final String currency;
        private final Instant created;
        private final List<String> codes;

        Order(String id,
              ServiceKey key,
              int qty,
              Contact contact,
              DeliveryPrefs deliveryPrefs,
              long totalMinor,
              String currency,
              Instant created,
              List<String> codes) {
            this.id = Objects.requireNonNull(id, "id");
            this.key = Objects.requireNonNull(key, "key");
            this.qty = qty;
            this.contact = Objects.requireNonNull(contact, "contact");
            this.deliveryPrefs = Objects.requireNonNull(deliveryPrefs, "deliveryPrefs");
            this.totalMinor = totalMinor;
            this.currency = Objects.requireNonNull(currency, "currency");
            this.created = Objects.requireNonNull(created, "created");
            this.codes = List.copyOf(Objects.requireNonNull(codes, "codes"));
        }

        public String getId() {
            return id;
        }

        public ServiceKey getKey() {
            return key;
        }

        public int getQty() {
            return qty;
        }

        public Contact getContact() {
            return contact;
        }

        public DeliveryPrefs getDeliveryPrefs() {
            return deliveryPrefs;
        }

        public long getTotalMinor() {
            return totalMinor;
        }

        public String getCurrency() {
            return currency;
        }

        public Instant getCreated() {
            return created;
        }

        public List<String> getCodes() {
            return codes;
        }
    }

    public static final class Invoice implements Serializable {

        @Serial
        private static final long serialVersionUID = 1L;

        private final String invoiceNo;
        private final ServiceKey key;
        private final int qty;
        private final Contact contact;
        private final DeliveryPrefs deliveryPrefs;
        private final long totalMinor;
        private final String currency;
        private final Instant created;
        private volatile InvoiceStatus status;
        private volatile List<String> codesIfDelivered;

        Invoice(String invoiceNo,
                ServiceKey key,
                int qty,
                Contact contact,
                DeliveryPrefs deliveryPrefs,
                long totalMinor,
                String currency,
                InvoiceStatus status,
                Instant created,
                List<String> codesIfDelivered) {
            this.invoiceNo = Objects.requireNonNull(invoiceNo, "invoiceNo");
            this.key = Objects.requireNonNull(key, "key");
            this.qty = qty;
            this.contact = Objects.requireNonNull(contact, "contact");
            this.deliveryPrefs = Objects.requireNonNull(deliveryPrefs, "deliveryPrefs");
            this.totalMinor = totalMinor;
            this.currency = Objects.requireNonNull(currency, "currency");
            this.status = Objects.requireNonNull(status, "status");
            this.created = Objects.requireNonNull(created, "created");
            this.codesIfDelivered = List.copyOf(Objects.requireNonNull(codesIfDelivered, "codesIfDelivered"));
        }

        public String getInvoiceNo() {
            return invoiceNo;
        }

        public ServiceKey getKey() {
            return key;
        }

        public int getQty() {
            return qty;
        }

        public Contact getContact() {
            return contact;
        }

        public DeliveryPrefs getDeliveryPrefs() {
            return deliveryPrefs;
        }

        public long getTotalMinor() {
            return totalMinor;
        }

        public String getCurrency() {
            return currency;
        }

        public InvoiceStatus getStatus() {
            return status;
        }

        void setStatus(InvoiceStatus status) {
            this.status = Objects.requireNonNull(status, "status");
        }

        public Instant getCreated() {
            return created;
        }

        public List<String> getCodesIfDelivered() {
            return codesIfDelivered;
        }

        void setCodesIfDelivered(List<String> codes) {
            this.codesIfDelivered = List.copyOf(Objects.requireNonNull(codes, "codes"));
        }
    }
}
