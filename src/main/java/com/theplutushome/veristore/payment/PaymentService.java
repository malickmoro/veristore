package com.theplutushome.veristore.payment;

import com.theplutushome.veristore.domain.Contact;
import com.theplutushome.veristore.domain.DeliveryPrefs;
import com.theplutushome.veristore.domain.InvoiceStatus;
import com.theplutushome.veristore.domain.Price;
import com.theplutushome.veristore.domain.ServiceKey;
import com.theplutushome.veristore.service.DeliveryService;
import com.theplutushome.veristore.service.OrderStore;
import com.theplutushome.veristore.service.PricingService;
import com.theplutushome.veristore.service.PinVault;
import com.theplutushome.veristore.util.Masker;

import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;
import java.io.Serial;
import java.io.Serializable;
import java.util.ArrayList;
import java.util.List;
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

    @Inject
    private PinVault pinVault;

    @Inject
    private PricingService pricingService;

    @Inject
    private OrderStore orderStore;

    @Inject
    private DeliveryService deliveryService;

    @Inject
    private PaymentGateway paymentGateway;

    public String payNow(ServiceKey key, int quantity, Contact contact, DeliveryPrefs deliveryPrefs) {
        Objects.requireNonNull(key, "key");
        Objects.requireNonNull(contact, "contact");
        Objects.requireNonNull(deliveryPrefs, "deliveryPrefs");
        validateQuantity(quantity);
        Price price = pricingService.get(key);
        long totalMinor = multiply(price.amountMinor(), quantity);
        List<String> codes = new ArrayList<>(pinVault.take(key, quantity));
        String orderId = orderStore.createOrder(key, quantity, contact, deliveryPrefs, totalMinor, price.currency(), codes);
        deliver(orderId, contact, deliveryPrefs, codes);
        LOGGER.log(Level.INFO, () -> String.format("Completed pay-now order %s for %s", orderId, contact.email()));
        return orderId;
    }

    public String payLater(ServiceKey key, int quantity, Contact contact, DeliveryPrefs deliveryPrefs) {
        Objects.requireNonNull(key, "key");
        Objects.requireNonNull(contact, "contact");
        Objects.requireNonNull(deliveryPrefs, "deliveryPrefs");
        validateQuantity(quantity);
        Price price = pricingService.get(key);
        long totalMinor = multiply(price.amountMinor(), quantity);
        String invoiceNo = orderStore.createInvoice(key, quantity, contact, deliveryPrefs, totalMinor, price.currency());
        LOGGER.log(Level.INFO, () -> String.format("Created invoice %s for %s", invoiceNo, contact.email()));
        return invoiceNo;
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
        List<String> codes = new ArrayList<>(pinVault.take(invoice.getKey(), invoice.getQty()));
        orderStore.markInvoicePaid(invoiceNo, codes);
        String orderId = orderStore.createOrder(invoice.getKey(), invoice.getQty(), invoice.getContact(), invoice.getDeliveryPrefs(), invoice.getTotalMinor(), invoice.getCurrency(), codes);
        deliver(orderId, invoice.getContact(), invoice.getDeliveryPrefs(), codes);
        LOGGER.log(Level.INFO, () -> String.format("Redeemed invoice %s as order %s", invoiceNo, orderId));
        return true;
    }

    public java.net.URI startCheckout(String invoiceNo) {
        return paymentGateway.beginCheckout(invoiceNo);
    }

    public InvoiceStatus handleGatewayCallback(java.util.Map<String, String> parameters) {
        return paymentGateway.verifyCallback(parameters);
    }

    private void deliver(String reference, Contact contact, DeliveryPrefs deliveryPrefs, List<String> codes) {
        List<String> masked = codes.stream().map(Masker::mask).collect(Collectors.toList());
        if (deliveryPrefs.byEmail()) {
            String subject = "Your Veristore PINs";
            String body = String.format("Reference %s:%n%s", reference, String.join(", ", masked));
            deliveryService.sendEmail(contact.email(), masked, subject, body);
        }
        if (deliveryPrefs.bySms()) {
            String body = String.format("Ref %s PINs: %s", reference, String.join(" ", masked));
            deliveryService.sendSms(contact.msisdn(), masked, body);
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
}
