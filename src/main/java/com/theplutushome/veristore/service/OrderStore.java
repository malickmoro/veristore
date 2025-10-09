package com.theplutushome.veristore.service;

import com.theplutushome.veristore.model.catalog.ProductKey;
import com.theplutushome.veristore.model.Contact;
import com.theplutushome.veristore.model.Currency;
import com.theplutushome.veristore.model.DeliveryPrefs;
import com.theplutushome.veristore.model.InvoiceStatus;

import jakarta.enterprise.context.ApplicationScoped;

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

    public String createOrder(ProductKey key,
                              int quantity,
                              Contact contact,
                              DeliveryPrefs deliveryPrefs,
                              long totalMinor,
                              Currency currency,
                              List<String> codes) {
        OrderLine line = new OrderLine(key, quantity, totalMinor, currency, codes);
        return createOrder(List.of(line), contact, deliveryPrefs);
    }

    public String createOrder(List<OrderLine> lines,
                              Contact contact,
                              DeliveryPrefs deliveryPrefs) {
        Objects.requireNonNull(lines, "lines");
        Objects.requireNonNull(contact, "contact");
        Objects.requireNonNull(deliveryPrefs, "deliveryPrefs");
        if (lines.isEmpty()) {
            throw new IllegalArgumentException("lines must not be empty");
        }
        Currency currency = ensureCommonCurrency(lines.stream().map(OrderLine::getCurrency).toList());
        long totalMinor = lines.stream().mapToLong(OrderLine::getTotalMinor).sum();
        String id = nextOrderId();
        Order order = new Order(id, contact, deliveryPrefs, totalMinor, currency, Instant.now(), lines);
        ordersById.put(id, order);
        return id;
    }

    public String createInvoice(ProductKey key,
                                int quantity,
                                Contact contact,
                                DeliveryPrefs deliveryPrefs,
                                long totalMinor,
                                Currency currency,
                                String invoiceNo,
                                String checkoutUrl) {
        InvoiceLine line = new InvoiceLine(key, quantity, totalMinor, currency, Collections.emptyList());
        return createInvoice(List.of(line), contact, deliveryPrefs, invoiceNo, checkoutUrl);
    }

    public String createInvoice(List<InvoiceLine> lines,
                                Contact contact,
                                DeliveryPrefs deliveryPrefs,
                                String invoiceNo,
                                String checkoutUrl) {
        Objects.requireNonNull(lines, "lines");
        Objects.requireNonNull(contact, "contact");
        Objects.requireNonNull(deliveryPrefs, "deliveryPrefs");
        if (lines.isEmpty()) {
            throw new IllegalArgumentException("lines must not be empty");
        }
        Currency currency = ensureCommonCurrency(lines.stream().map(InvoiceLine::getCurrency).toList());
        long totalMinor = lines.stream().mapToLong(InvoiceLine::getTotalMinor).sum();
        String number = invoiceNo == null || invoiceNo.isBlank() ? nextInvoiceNo() : invoiceNo.trim();
        if (invoicesByNo.containsKey(number)) {
            throw new IllegalArgumentException("Invoice already exists: " + number);
        }
        Invoice invoice = new Invoice(number, contact, deliveryPrefs, totalMinor, currency, InvoiceStatus.PENDING, Instant.now(), checkoutUrl, lines);
        invoicesByNo.put(number, invoice);
        return number;
    }

    public Optional<Invoice> findInvoice(String invoiceNo) {
        if (invoiceNo == null) {
            return Optional.empty();
        }
        return Optional.ofNullable(invoicesByNo.get(invoiceNo));
    }

    public Optional<Order> findOrder(String orderId) {
        if (orderId == null) {
            return Optional.empty();
        }
        return Optional.ofNullable(ordersById.get(orderId));
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

    public Optional<Invoice> markInvoicePaid(String invoiceNo, Map<ProductKey, List<String>> deliveredCodes) {
        Objects.requireNonNull(deliveredCodes, "deliveredCodes");
        return findInvoice(invoiceNo).map(invoice -> {
            synchronized (invoice) {
                if (invoice.getStatus() != InvoiceStatus.PAID) {
                    invoice.setStatus(InvoiceStatus.PAID);
                    invoice.setDeliveredCodes(deliveredCodes);
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

    private Currency ensureCommonCurrency(List<Currency> currencies) {
        if (currencies.isEmpty()) {
            throw new IllegalArgumentException("At least one currency required");
        }
        Currency first = currencies.get(0);
        boolean mismatch = currencies.stream().anyMatch(currency -> currency != first);
        if (mismatch) {
            throw new IllegalArgumentException("All lines must share the same currency");
        }
        return first;
    }

    public static final class Order implements Serializable {

        @Serial
        private static final long serialVersionUID = 1L;

        private final String id;
        private final Contact contact;
        private final DeliveryPrefs deliveryPrefs;
        private final Instant created;
        private final long totalMinor;
        private final Currency currency;
        private final List<OrderLine> lines;

        Order(String id,
              Contact contact,
              DeliveryPrefs deliveryPrefs,
              long totalMinor,
              Currency currency,
              Instant created,
              List<OrderLine> lines) {
            this.id = Objects.requireNonNull(id, "id");
            this.contact = Objects.requireNonNull(contact, "contact");
            this.deliveryPrefs = Objects.requireNonNull(deliveryPrefs, "deliveryPrefs");
            this.created = Objects.requireNonNull(created, "created");
            this.totalMinor = totalMinor;
            this.currency = Objects.requireNonNull(currency, "currency");
            this.lines = List.copyOf(Objects.requireNonNull(lines, "lines"));
        }

        public String getId() {
            return id;
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

        public Currency getCurrency() {
            return currency;
        }

        public Instant getCreated() {
            return created;
        }

        public List<OrderLine> getLines() {
            return lines;
        }

        public int getQty() {
            return lines.stream().mapToInt(OrderLine::getQuantity).sum();
        }

        public List<String> getCodes() {
            return lines.stream()
                    .flatMap(line -> line.getCodes().stream())
                    .toList();
        }
    }

    public static final class Invoice implements Serializable {

        @Serial
        private static final long serialVersionUID = 1L;

        private final String invoiceNo;
        private final Contact contact;
        private final DeliveryPrefs deliveryPrefs;
        private InvoiceStatus status;
        private final String checkoutUrl;
        private final Instant created;
        private final long totalMinor;
        private final Currency currency;
        private final List<InvoiceLine> lines;

        Invoice(String invoiceNo,
                Contact contact,
                DeliveryPrefs deliveryPrefs,
                long totalMinor,
                Currency currency,
                InvoiceStatus status,
                Instant created,
                String checkoutUrl,
                List<InvoiceLine> lines) {
            this.invoiceNo = Objects.requireNonNull(invoiceNo, "invoiceNo");
            this.contact = Objects.requireNonNull(contact, "contact");
            this.deliveryPrefs = Objects.requireNonNull(deliveryPrefs, "deliveryPrefs");
            this.status = Objects.requireNonNull(status, "status");
            this.created = Objects.requireNonNull(created, "created");
            this.checkoutUrl = checkoutUrl;
            this.totalMinor = totalMinor;
            this.currency = Objects.requireNonNull(currency, "currency");
            this.lines = new ArrayList<>(Objects.requireNonNull(lines, "lines"));
        }

        public String getInvoiceNo() {
            return invoiceNo;
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

        public Currency getCurrency() {
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

        public List<InvoiceLine> getLines() {
            return List.copyOf(lines);
        }

        public int getQty() {
            return lines.stream().mapToInt(InvoiceLine::getQuantity).sum();
        }

        public List<String> getCodesIfDelivered() {
            return lines.stream()
                    .flatMap(line -> line.getDeliveredCodes().stream())
                    .toList();
        }

        void setDeliveredCodes(Map<ProductKey, List<String>> deliveredCodes) {
            Objects.requireNonNull(deliveredCodes, "deliveredCodes");
            for (InvoiceLine line : lines) {
                List<String> codes = deliveredCodes.getOrDefault(line.getKey(), List.of());
                line.setDeliveredCodes(codes);
            }
        }

        public String getCheckoutUrl() {
            return checkoutUrl;
        }
    }

    public static final class OrderLine implements Serializable {

        @Serial
        private static final long serialVersionUID = 1L;

        private final ProductKey key;
        private final int quantity;
        private final long totalMinor;
        private final Currency currency;
        private final List<String> codes;

        public OrderLine(ProductKey key,
                         int quantity,
                         long totalMinor,
                         Currency currency,
                         List<String> codes) {
            this.key = Objects.requireNonNull(key, "key");
            if (quantity <= 0) {
                throw new IllegalArgumentException("quantity must be positive");
            }
            this.quantity = quantity;
            this.totalMinor = totalMinor;
            this.currency = Objects.requireNonNull(currency, "currency");
            this.codes = List.copyOf(Objects.requireNonNull(codes, "codes"));
        }

        public ProductKey getKey() {
            return key;
        }

        public int getQuantity() {
            return quantity;
        }

        public long getTotalMinor() {
            return totalMinor;
        }

        public Currency getCurrency() {
            return currency;
        }

        public List<String> getCodes() {
            return codes;
        }
    }

    public static final class InvoiceLine implements Serializable {

        @Serial
        private static final long serialVersionUID = 1L;

        private final ProductKey key;
        private final int quantity;
        private final long totalMinor;
        private final Currency currency;
        private List<String> deliveredCodes;

        public InvoiceLine(ProductKey key,
                           int quantity,
                           long totalMinor,
                           Currency currency,
                           List<String> deliveredCodes) {
            this.key = Objects.requireNonNull(key, "key");
            if (quantity <= 0) {
                throw new IllegalArgumentException("quantity must be positive");
            }
            this.quantity = quantity;
            this.totalMinor = totalMinor;
            this.currency = Objects.requireNonNull(currency, "currency");
            this.deliveredCodes = List.copyOf(Objects.requireNonNull(deliveredCodes, "deliveredCodes"));
        }

        public ProductKey getKey() {
            return key;
        }

        public int getQuantity() {
            return quantity;
        }

        public long getTotalMinor() {
            return totalMinor;
        }

        public Currency getCurrency() {
            return currency;
        }

        public List<String> getDeliveredCodes() {
            return deliveredCodes;
        }

        void setDeliveredCodes(List<String> deliveredCodes) {
            this.deliveredCodes = List.copyOf(Objects.requireNonNull(deliveredCodes, "deliveredCodes"));
        }
    }
}
