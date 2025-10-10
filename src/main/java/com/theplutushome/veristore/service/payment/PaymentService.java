package com.theplutushome.veristore.service.payment;

import com.theplutushome.veristore.model.PinRecord;
import com.theplutushome.veristore.model.TransactionRecord;
import com.theplutushome.veristore.model.catalog.EnrollmentSku;
import com.theplutushome.veristore.model.catalog.ProductFamily;
import com.theplutushome.veristore.model.catalog.ProductKey;
import com.theplutushome.veristore.model.Contact;
import com.theplutushome.veristore.model.Currency;
import com.theplutushome.veristore.model.DeliveryPrefs;
import com.theplutushome.veristore.model.InvoiceStatus;
import com.theplutushome.veristore.model.Price;
import com.theplutushome.veristore.payload.request.TransactionRequest;
import com.theplutushome.veristore.service.email.EmailService;
import com.theplutushome.veristore.service.atlas.AtlasService;
import com.theplutushome.veristore.service.OrderStore;
import com.theplutushome.veristore.service.PinVault;
import com.theplutushome.veristore.service.PricingService;
import com.theplutushome.veristore.util.VariantDescriptions;

import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;

import java.io.Serial;
import java.io.Serializable;
import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.logging.Level;
import java.util.logging.Logger;
import java.util.stream.Collectors;

@ApplicationScoped
public class PaymentService implements Serializable {

    @Serial
    private static final long serialVersionUID = 1L;

    private static final Logger LOGGER = Logger.getLogger(PaymentService.class.getName());
    private static final DateTimeFormatter PAYMENT_TIMESTAMP_FORMAT = DateTimeFormatter.ofPattern("dd/MM/yyyy hh:mm:ss a");
    private static final String DEFAULT_PAYMENT_MODE = "ONLINE";
    private static final String DEFAULT_SOURCE = "MOBILE_APP";
    private static final String DEFAULT_TELLER = "Veristore";

    @Inject
    private PinVault pinVault;

    @Inject
    private PricingService pricingService;

    @Inject
    private OrderStore orderStore;

    @Inject
    private AtlasService atlasService;

    @Inject
    private EmailService emailService;

    public CheckoutInitiation payNow(ProductKey key, int quantity, Contact contact, DeliveryPrefs deliveryPrefs) {
        return payNow(List.of(new Purchase(key, quantity)), contact, deliveryPrefs);
    }

    public CheckoutInitiation payNow(List<Purchase> purchases, Contact contact, DeliveryPrefs deliveryPrefs) {
        return initiateInvoice("pay-now", purchases, contact, deliveryPrefs, true);
    }

    public CheckoutInitiation payLater(ProductKey key, int quantity, Contact contact, DeliveryPrefs deliveryPrefs) {
        return payLater(List.of(new Purchase(key, quantity)), contact, deliveryPrefs);
    }

    public CheckoutInitiation payLater(List<Purchase> purchases, Contact contact, DeliveryPrefs deliveryPrefs) {
        return initiateInvoice("pay-later", purchases, contact, deliveryPrefs, false);
    }

    public boolean redeemInvoice(String invoiceNo) {
        Objects.requireNonNull(invoiceNo, "invoiceNo");
        Optional<OrderStore.Invoice> invoiceOpt = orderStore.findInvoice(invoiceNo);
        if (invoiceOpt.isEmpty()) {
            LOGGER.log(Level.WARNING, () -> "Attempted to redeem unknown invoice " + invoiceNo);
            return false;
        }
        OrderStore.Invoice invoice = invoiceOpt.get();
        if (invoice.getStatus() == InvoiceStatus.CANCELLED) {
            LOGGER.log(Level.WARNING, () -> "Invoice " + invoiceNo + " is cancelled");
            return false;
        }
        if (invoice.getStatus() == InvoiceStatus.PAID) {
            return true;
        }

        fulfillInvoice(invoice);
        return invoice.getStatus() == InvoiceStatus.PAID;
    }

    public boolean processGatewayCallback(String invoiceNo) {
        if (invoiceNo == null || invoiceNo.isBlank()) {
            return false;
        }
        String normalized = invoiceNo.trim();
        Optional<OrderStore.Invoice> invoiceOpt = orderStore.findInvoice(normalized);
        if (invoiceOpt.isEmpty()) {
            LOGGER.log(Level.WARNING, () -> "Received callback for unknown invoice " + normalized);
            return false;
        }
        fulfillInvoice(invoiceOpt.get());
        return true;
    }

    private void fulfillInvoice(OrderStore.Invoice invoice) {
        synchronized (invoice) {
            if (invoice.getStatus() == InvoiceStatus.PAID) {
                return;
            }
            Map<ProductKey, List<String>> deliveredCodes = new LinkedHashMap<>();
            List<OrderStore.OrderLine> orderLines = new ArrayList<>();
            for (OrderStore.InvoiceLine line : invoice.getLines()) {
                List<String> codes = resolveCodes(invoice, line);
                deliveredCodes.put(line.getKey(), codes);
                orderLines.add(new OrderStore.OrderLine(
                        line.getKey(),
                        line.getQuantity(),
                        line.getTotalMinor(),
                        line.getCurrency(),
                        codes));
            }
            orderStore.markInvoicePaid(invoice.getInvoiceNo(), deliveredCodes);
            String orderId = orderStore.createOrder(orderLines, invoice.getContact(), invoice.getDeliveryPrefs());
            deliver(orderId, invoice.getContact(), invoice.getDeliveryPrefs(), orderLines);
            LOGGER.log(Level.INFO, () -> String.format("Redeemed invoice %s as order %s", invoice.getInvoiceNo(), orderId));
        }
    }

    private List<String> resolveCodes(OrderStore.Invoice invoice, OrderStore.InvoiceLine line) {
        if (line.getKey().family() == ProductFamily.ENROLLMENT) {
            return fetchEnrollmentPins(invoice, line);
        }
        return new ArrayList<>(pinVault.take(line.getKey(), line.getQuantity()));
    }

    private CheckoutInitiation initiateInvoice(String label,
                                               List<Purchase> purchases,
                                               Contact contact,
                                               DeliveryPrefs deliveryPrefs,
                                               boolean autoFulfill) {
        Objects.requireNonNull(purchases, "purchases");
        Objects.requireNonNull(contact, "contact");
        Objects.requireNonNull(deliveryPrefs, "deliveryPrefs");
        if (purchases.isEmpty()) {
            throw new IllegalArgumentException("purchases must not be empty");
        }
        List<PurchaseDetail> details = new ArrayList<>();
        for (Purchase purchase : purchases) {
            Objects.requireNonNull(purchase, "purchase");
            validateQuantity(purchase.getQuantity());
            Price price = pricingService.get(purchase.getKey());
            details.add(new PurchaseDetail(purchase.getKey(), purchase.getQuantity(), price));
        }

        List<OrderStore.InvoiceLine> invoiceLines = new ArrayList<>();
        for (PurchaseDetail detail : details) {
            long totalMinor = multiply(detail.price().amountMinor(), detail.quantity());
            invoiceLines.add(new OrderStore.InvoiceLine(
                    detail.key(),
                    detail.quantity(),
                    totalMinor,
                    detail.price().currency(),
                    List.of()));
        }
        String storedInvoice = orderStore.createInvoice(invoiceLines, contact, deliveryPrefs, null, null);

        LOGGER.log(Level.INFO, () -> String.format("Created %s invoice %s for %s", label, storedInvoice, contact.email()));
        if (autoFulfill) {
            orderStore.findInvoice(storedInvoice).ifPresent(this::fulfillInvoice);
        }
        return new CheckoutInitiation(storedInvoice, null);
    }

    private List<String> fetchEnrollmentPins(OrderStore.Invoice invoice, OrderStore.InvoiceLine line) {
        EnrollmentSku sku = EnrollmentSku.bySku(line.getKey().sku())
            .orElseThrow(() -> new IllegalArgumentException("Unknown enrollment SKU: " + line.getKey().sku()));

        TransactionRequest request = buildTransactionRequest(invoice, line, sku);
        TransactionRecord transaction = atlasService.createTransaction(request);
        if (transaction == null || transaction.transactionId() == null || transaction.transactionId().isBlank()) {
            throw new IllegalStateException("Atlas transaction could not be created for " + sku.name());
        }

        List<PinRecord> pins = atlasService.confirmTransaction(transaction.transactionId());
        if (pins == null || pins.isEmpty()) {
            throw new IllegalStateException("Atlas returned no PINs for transaction " + transaction.transactionId());
        }
        if (pins.size() < line.getQuantity()) {
            LOGGER.log(Level.WARNING, () -> String.format(
                "Atlas returned %d PIN(s) but %d were requested for %s",
                pins.size(),
                line.getQuantity(),
                sku.name()));
        }

        return pins.stream()
            .map(this::formatPinRecord)
            .collect(Collectors.toList());
    }

    private TransactionRequest buildTransactionRequest(OrderStore.Invoice invoice,
                                                       OrderStore.InvoiceLine line,
                                                       EnrollmentSku sku) {
        TransactionRequest request = new TransactionRequest();
        request.setServiceId(sku.name());
        request.setQuantity(line.getQuantity());
        BigDecimal totalMajor = BigDecimal.valueOf(line.getTotalMinor(), 2);
        request.setAmountPaid(totalMajor.doubleValue());
        request.setExchangeRate(1.0d);
        request.setPaymentMode(DEFAULT_PAYMENT_MODE);
        request.setTellerName(DEFAULT_TELLER);
        request.setPaymentId(invoice.getInvoiceNo());
        request.setDateOfPayment(currentPaymentTimestamp());
        request.setSource(DEFAULT_SOURCE);
        return request;
    }

    private String currentPaymentTimestamp() {
        return LocalDateTime.now(ZoneId.systemDefault()).format(PAYMENT_TIMESTAMP_FORMAT);
    }

    private String formatPinRecord(PinRecord pin) {
        String serial = Optional.ofNullable(pin.serialNo()).orElse("N/A");
        String value = Optional.ofNullable(pin.pin()).orElse("");
        return serial + " - " + value;
    }

    private void deliver(String reference,
                         Contact contact,
                         DeliveryPrefs deliveryPrefs,
                         List<OrderStore.OrderLine> lines) {
        if (lines.isEmpty()) {
            return;
        }
        Map<String, List<String>> codesByProduct = new LinkedHashMap<>();
        for (OrderStore.OrderLine line : lines) {
            if (line.getCodes().isEmpty()) {
                continue;
            }
            String description = VariantDescriptions.describe(line.getKey().family(), line.getKey().sku());
            codesByProduct.computeIfAbsent(description, key -> new ArrayList<>())
                    .addAll(line.getCodes());
        }
        if (codesByProduct.isEmpty()) {
            return;
        }
        if (deliveryPrefs.byEmail() && contact.email() != null && !contact.email().isBlank()) {
            boolean sent = emailService.sendPinsEmail(contact.email(), reference, codesByProduct);
            if (!sent) {
                LOGGER.log(Level.WARNING, () -> "Failed to send PIN email to " + contact.email());
            }
        }
        if (deliveryPrefs.bySms() && contact.msisdn() != null && !contact.msisdn().isBlank()) {
            LOGGER.log(Level.INFO, () -> "SMS delivery pending implementation for " + contact.msisdn());
        }
    }

    private void validateQuantity(int quantity) {
        if (quantity <= 0) {
            throw new IllegalArgumentException("quantity must be positive");
        }
    }

    private long multiply(long amountMinor, int quantity) {
        return Math.multiplyExact(amountMinor, quantity);
    }

    public static final class CheckoutInitiation implements Serializable {

        @Serial
        private static final long serialVersionUID = 1L;

        private final String invoiceNo;
        private final String checkoutUrl;

        public CheckoutInitiation(String invoiceNo, String checkoutUrl) {
            this.invoiceNo = Objects.requireNonNull(invoiceNo, "invoiceNo");
            this.checkoutUrl = checkoutUrl;
        }

        public String getInvoiceNo() {
            return invoiceNo;
        }

        public String getCheckoutUrl() {
            return checkoutUrl;
        }
    }

    public static final class Purchase implements Serializable {

        @Serial
        private static final long serialVersionUID = 1L;

        private final ProductKey key;
        private final int quantity;

        public Purchase(ProductKey key, int quantity) {
            this.key = Objects.requireNonNull(key, "key");
            this.quantity = quantity;
        }

        public ProductKey getKey() {
            return key;
        }

        public int getQuantity() {
            return quantity;
        }
    }

    private static final class PurchaseDetail implements Serializable {

        @Serial
        private static final long serialVersionUID = 1L;

        private final ProductKey key;
        private final int quantity;
        private final Price price;

        PurchaseDetail(ProductKey key, int quantity, Price price) {
            this.key = Objects.requireNonNull(key, "key");
            this.quantity = quantity;
            this.price = Objects.requireNonNull(price, "price");
        }

        public ProductKey key() {
            return key;
        }

        public int quantity() {
            return quantity;
        }

        public Price price() {
            return price;
        }
    }
}
