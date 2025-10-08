package com.theplutushome.veristore.payment;

import com.theplutushome.veristore.annotations.GOV;
import com.theplutushome.veristore.catalog.ProductKey;
import com.theplutushome.veristore.domain.Contact;
import com.theplutushome.veristore.domain.Currency;
import com.theplutushome.veristore.domain.DeliveryPrefs;
import com.theplutushome.veristore.domain.InvoiceStatus;
import com.theplutushome.veristore.domain.Price;
import com.theplutushome.veristore.email.EmailService;
import com.theplutushome.veristore.payload.enums.PaymentCurrency;
import com.theplutushome.veristore.payload.request.CreateInvoiceRequest;
import com.theplutushome.veristore.payload.request.InvoiceCheckRequest;
import com.theplutushome.veristore.payload.response.CreateInvoiceResponse;
import com.theplutushome.veristore.payload.response.PaymentResponse;
import com.theplutushome.veristore.payment.impl.GovCheckout;
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
import java.util.List;
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

    public String payNow(ProductKey key, int quantity, Contact contact, DeliveryPrefs deliveryPrefs) {
        Objects.requireNonNull(key, "key");
        Objects.requireNonNull(contact, "contact");
        Objects.requireNonNull(deliveryPrefs, "deliveryPrefs");
        validateQuantity(quantity);

        Price price = pricingService.get(key);
        long totalMinor = multiply(price.amountMinor(), quantity);
        List<String> codes = new ArrayList<>(pinVault.take(key, quantity));
        String orderId = orderStore.createOrder(key,
                quantity,
                contact,
                deliveryPrefs,
                totalMinor,
                price.currency(),
                codes);
        String productDescription = VariantDescriptions.describe(key.family(), key.sku());
        deliver(orderId, contact, deliveryPrefs, codes, productDescription);
        LOGGER.log(Level.INFO, () -> String.format("Completed pay-now order %s for %s", orderId, contact.email()));
        return orderId;
    }

    public String payLater(ProductKey key, int quantity, Contact contact, DeliveryPrefs deliveryPrefs) {
        Objects.requireNonNull(key, "key");
        Objects.requireNonNull(contact, "contact");
        Objects.requireNonNull(deliveryPrefs, "deliveryPrefs");
        validateQuantity(quantity);

        Price price = pricingService.get(key);
        long totalMinor = multiply(price.amountMinor(), quantity);

        CreateInvoiceRequest request = buildInvoiceRequest(key, quantity, contact, price);
        CreateInvoiceResponse response = govCheckout.initiateCheckout(request);
        if (response == null || response.getInvoiceNumber() == null || response.getInvoiceNumber().isBlank()) {
            throw new IllegalStateException("Unable to create invoice at payment gateway.");
        }

        String invoiceNo = response.getInvoiceNumber().trim();
        String checkoutUrl = response.getCheckoutUrl();
        String storedInvoice = orderStore.createInvoice(key,
                quantity,
                contact,
                deliveryPrefs,
                totalMinor,
                price.currency(),
                invoiceNo,
                checkoutUrl);

        LOGGER.log(Level.INFO, () -> String.format("Created GOV invoice %s for %s", storedInvoice, contact.email()));
        if (checkoutUrl != null && !checkoutUrl.isBlank()) {
            LOGGER.log(Level.INFO, () -> "Checkout URL: " + checkoutUrl);
        }
        return storedInvoice;
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
            List<String> codes = new ArrayList<>(pinVault.take(invoice.getKey(), invoice.getQty()));
            orderStore.markInvoicePaid(invoice.getInvoiceNo(), codes);
            String orderId = orderStore.createOrder(invoice.getKey(),
                    invoice.getQty(),
                    invoice.getContact(),
                    invoice.getDeliveryPrefs(),
                    invoice.getTotalMinor(),
                    invoice.getCurrency(),
                    codes);
            String productDescription = VariantDescriptions.describe(invoice.getKey().family(), invoice.getKey().sku());
            deliver(orderId, invoice.getContact(), invoice.getDeliveryPrefs(), codes, productDescription);
            LOGGER.log(Level.INFO, () -> String.format("Redeemed invoice %s as order %s", invoice.getInvoiceNo(), orderId));
        }
    }

    private CreateInvoiceRequest buildInvoiceRequest(ProductKey key,
                                                     int quantity,
                                                     Contact contact,
                                                     Price price) {
        CreateInvoiceRequest request = new CreateInvoiceRequest();
        request.setFirstname(extractNamePart(contact.email(), true));
        request.setLastname(extractNamePart(contact.email(), false));
        request.setPhonenumber(contact.msisdn());
        request.setEmail(contact.email());
        request.setApplicationId(UUID.randomUUID().toString());
        request.setDescription(VariantDescriptions.describe(key.family(), key.sku()));
        request.setExtraDetails(String.format("sku=%s;quantity=%d", key.sku(), quantity));

        CreateInvoiceRequest.InvoiceItem item = new CreateInvoiceRequest.InvoiceItem();
        item.setServiceCode(key.sku());
        item.setAmount(BigDecimal.valueOf(price.amountMinor(), 2).multiply(BigDecimal.valueOf(quantity)));
        item.setCurrency(mapCurrency(price.currency()));
        item.setMemo(String.format("%s x%d", key.sku(), quantity));
        request.addInvoice(item);

        return request;
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

    private void deliver(String reference, Contact contact, DeliveryPrefs deliveryPrefs, List<String> codes, String productDescription) {
        if (codes.isEmpty()) {
            return;
        }
        if (deliveryPrefs.byEmail() && contact.email() != null && !contact.email().isBlank()) {
            boolean sent = emailService.sendPinsEmail(contact.email(), reference, codes, productDescription);
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
}
