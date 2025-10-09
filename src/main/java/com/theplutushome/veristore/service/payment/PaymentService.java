package com.theplutushome.veristore.service.payment;

import com.theplutushome.veristore.config.annotations.GOV;
import com.theplutushome.veristore.model.catalog.ProductKey;
import com.theplutushome.veristore.model.Contact;
import com.theplutushome.veristore.model.Currency;
import com.theplutushome.veristore.model.DeliveryPrefs;
import com.theplutushome.veristore.model.InvoiceStatus;
import com.theplutushome.veristore.model.Price;
import com.theplutushome.veristore.service.email.EmailService;
import com.theplutushome.veristore.payload.enums.PaymentCurrency;
import com.theplutushome.veristore.payload.request.CreateInvoiceRequest;
import com.theplutushome.veristore.payload.request.InvoiceCheckRequest;
import com.theplutushome.veristore.payload.response.CreateInvoiceResponse;
import com.theplutushome.veristore.payload.response.PaymentResponse;
import com.theplutushome.veristore.service.payment.impl.GovCheckout;
import com.theplutushome.veristore.service.OrderStore;
import com.theplutushome.veristore.service.PinVault;
import com.theplutushome.veristore.service.PricingService;
import com.theplutushome.veristore.util.VariantDescriptions;

import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;

import java.io.Serial;
import java.io.Serializable;
import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.UUID;
import java.util.logging.Level;
import java.util.logging.Logger;

@ApplicationScoped
public class PaymentService implements Serializable {

    @Serial
    private static final long serialVersionUID = 1L;

    private static final Logger LOGGER = Logger.getLogger(PaymentService.class.getName());

    @Inject
    private PinVault pinVault;

    @Inject
    private PricingService pricingService;

    @Inject
    private OrderStore orderStore;

    @Inject
    @GOV
    private GovCheckout govCheckout;

    @Inject
    private EmailService emailService;

    public CheckoutInitiation payNow(ProductKey key, int quantity, Contact contact, DeliveryPrefs deliveryPrefs) {
        return payNow(List.of(new Purchase(key, quantity)), contact, deliveryPrefs);
    }

    public CheckoutInitiation payNow(List<Purchase> purchases, Contact contact, DeliveryPrefs deliveryPrefs) {
        return initiateInvoice("pay-now", purchases, contact, deliveryPrefs);
    }

    public CheckoutInitiation payLater(ProductKey key, int quantity, Contact contact, DeliveryPrefs deliveryPrefs) {
        return payLater(List.of(new Purchase(key, quantity)), contact, deliveryPrefs);
    }

    public CheckoutInitiation payLater(List<Purchase> purchases, Contact contact, DeliveryPrefs deliveryPrefs) {
        return initiateInvoice("pay-later", purchases, contact, deliveryPrefs);
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

        PaymentResponse response = govCheckout.checkInvoiceStatus(new InvoiceCheckRequest(invoiceNo));
        InvoiceStatus gatewayStatus = mapStatus(response);
        if (gatewayStatus == InvoiceStatus.CANCELLED) {
            orderStore.markInvoiceCancelled(invoiceNo);
            LOGGER.log(Level.INFO, () -> "Marked invoice " + invoiceNo + " as cancelled by gateway");
            return false;
        }
        if (gatewayStatus != InvoiceStatus.PAID) {
            LOGGER.log(Level.INFO, () -> "Invoice " + invoiceNo + " not yet paid. Status: " + gatewayStatus);
            return false;
        }

        fulfillInvoice(invoice);
        return true;
    }

    public boolean processGatewayCallback(String invoiceNo) {
        if (invoiceNo == null || invoiceNo.isBlank()) {
            return false;
        }
        String normalized = invoiceNo.trim();
        PaymentResponse response = govCheckout.checkInvoiceStatus(new InvoiceCheckRequest(normalized));
        InvoiceStatus status = mapStatus(response);
        LOGGER.log(Level.INFO, () -> String.format("Gateway callback for %s with status %s", normalized, status));

        if (status == InvoiceStatus.CANCELLED) {
            orderStore.markInvoiceCancelled(normalized);
            return false;
        }
        if (status != InvoiceStatus.PAID) {
            return false;
        }

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
                List<String> codes = new ArrayList<>(pinVault.take(line.getKey(), line.getQuantity()));
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

    private CheckoutInitiation initiateInvoice(String label,
                                               List<Purchase> purchases,
                                               Contact contact,
                                               DeliveryPrefs deliveryPrefs) {
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

        CreateInvoiceRequest request = buildInvoiceRequest(contact, details);
        CreateInvoiceResponse response = govCheckout.initiateCheckout(request);
        if (response == null || response.getInvoiceNumber() == null || response.getInvoiceNumber().isBlank()) {
            throw new IllegalStateException("Unable to create invoice at payment gateway.");
        }

        String invoiceNo = response.getInvoiceNumber().trim();
        String checkoutUrl = response.getCheckoutUrl();
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
        String storedInvoice = orderStore.createInvoice(invoiceLines, contact, deliveryPrefs, invoiceNo, checkoutUrl);

        LOGGER.log(Level.INFO, () -> String.format("Created %s invoice %s for %s", label, storedInvoice, contact.email()));
        if (checkoutUrl != null && !checkoutUrl.isBlank()) {
            LOGGER.log(Level.INFO, () -> "Checkout URL: " + checkoutUrl);
        }
        return new CheckoutInitiation(storedInvoice, checkoutUrl);
    }

    private CreateInvoiceRequest buildInvoiceRequest(Contact contact, List<PurchaseDetail> details) {
        CreateInvoiceRequest request = new CreateInvoiceRequest();
        request.setFirstname(extractNamePart(contact.email(), true));
        request.setLastname(extractNamePart(contact.email(), false));
        request.setPhonenumber(contact.msisdn());
        request.setEmail(contact.email());
        request.setApplicationId(UUID.randomUUID().toString());
        request.setDescription(buildDescription(details));
        request.setExtraDetails(buildExtraDetails(details));

        for (PurchaseDetail detail : details) {
            CreateInvoiceRequest.InvoiceItem item = new CreateInvoiceRequest.InvoiceItem();
            item.setServiceCode(detail.key().sku());
            item.setAmount(BigDecimal.valueOf(detail.price().amountMinor(), 2)
                    .multiply(BigDecimal.valueOf(detail.quantity())));
            item.setCurrency(mapCurrency(detail.price().currency()));
            item.setMemo(String.format("%s x%d", detail.key().sku(), detail.quantity()));
            request.addInvoice(item);
        }

        return request;
    }

    private String buildDescription(List<PurchaseDetail> details) {
        if (details.isEmpty()) {
            return "Veristore purchase";
        }
        if (details.size() == 1) {
            PurchaseDetail detail = details.get(0);
            return VariantDescriptions.describe(detail.key().family(), detail.key().sku());
        }
        return String.format("Veristore purchase (%d items)", details.size());
    }

    private String buildExtraDetails(List<PurchaseDetail> details) {
        StringBuilder builder = new StringBuilder();
        for (int i = 0; i < details.size(); i++) {
            PurchaseDetail detail = details.get(i);
            if (i > 0) {
                builder.append('|');
            }
            builder.append("sku=")
                    .append(detail.key().sku())
                    .append(";quantity=")
                    .append(detail.quantity());
        }
        return builder.toString();
    }

    private PaymentCurrency mapCurrency(Currency currency) {
        return switch (currency) {
            case GHS -> PaymentCurrency.GHS;
            case USD -> PaymentCurrency.USD;
        };
    }

    private InvoiceStatus mapStatus(PaymentResponse response) {
        if (response == null) {
            return InvoiceStatus.PENDING;
        }
        PaymentResponse.InvoiceDetails details = response.getOutput();
        if (details == null) {
            return InvoiceStatus.PENDING;
        }
        int code = details.getPaymentStatusCode();
        String text = Optional.ofNullable(details.getPaymentStatusText()).orElse("");
        if (code == 1 || text.equalsIgnoreCase("paid")) {
            return InvoiceStatus.PAID;
        }
        if (code == 3 || text.equalsIgnoreCase("cancelled") || text.equalsIgnoreCase("canceled")) {
            return InvoiceStatus.CANCELLED;
        }
        return InvoiceStatus.PENDING;
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

    private String extractNamePart(String email, boolean first) {
        if (email == null || email.isBlank()) {
            return first ? "Customer" : "";
        }
        String localPart = email.split("@")[0];
        if (localPart.isBlank()) {
            return first ? "Customer" : "";
        }
        String[] tokens = localPart.replace('.', ' ').replace('_', ' ').split(" ");
        if (tokens.length == 0) {
            return first ? capitalize(localPart) : "";
        }
        if (first) {
            return capitalize(tokens[0]);
        }
        if (tokens.length > 1) {
            return capitalize(tokens[tokens.length - 1]);
        }
        return "";
    }

    private String capitalize(String value) {
        if (value == null || value.isBlank()) {
            return "";
        }
        String trimmed = value.trim();
        if (trimmed.length() == 1) {
            return trimmed.toUpperCase();
        }
        return trimmed.substring(0, 1).toUpperCase() + trimmed.substring(1);
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
