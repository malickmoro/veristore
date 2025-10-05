package com.theplutushome.veristore.view;

import com.theplutushome.veristore.catalog.ProductFamily;
import com.theplutushome.veristore.catalog.ProductKey;
import com.theplutushome.veristore.domain.Contact;
import com.theplutushome.veristore.domain.Currency;
import com.theplutushome.veristore.domain.DeliveryPrefs;
import com.theplutushome.veristore.domain.PaymentMode;
import com.theplutushome.veristore.domain.Price;
import com.theplutushome.veristore.dto.CartLineDTO;
import com.theplutushome.veristore.payment.PaymentService;
import com.theplutushome.veristore.service.PricingService;

import jakarta.enterprise.context.SessionScoped;
import jakarta.faces.application.FacesMessage;
import jakarta.faces.context.FacesContext;
import jakarta.inject.Inject;
import jakarta.inject.Named;

import java.io.Serial;
import java.io.Serializable;
import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.UUID;

@Named
@SessionScoped
public class CartView implements Serializable {

    @Serial
    private static final long serialVersionUID = 1L;

    @Inject
    private PricingService pricingService;

    @Inject
    private PaymentService paymentService;

    private final List<CartLineDTO> lines = new ArrayList<>();
    private final Map<String, CheckoutResult> checkoutResults = new HashMap<>();

    public List<CartLineDTO> getLines() {
        return lines;
    }

    public List<CartLineDTO> getLineCopies() {
        List<CartLineDTO> copies = new ArrayList<>();
        for (CartLineDTO line : lines) {
            copies.add(new CartLineDTO(line));
        }
        return copies;
    }

    public CartLineDTO getLine(String sku) {
        return findLine(sku).map(CartLineDTO::new).orElse(null);
    }

    public boolean getHasNoItems() {
        return lines.isEmpty();
    }

    public int getItemCount() {
        return lines.stream().mapToInt(CartLineDTO::getQty).sum();
    }

    public Map<String, String> getTotalsByCurrency() {
        Map<String, Long> totals = new LinkedHashMap<>();
        for (CartLineDTO line : lines) {
            if (line.getCurrency() == null || line.getCurrency().isBlank()) {
                continue;
            }
            totals.merge(line.getCurrency(), line.getTotalMinor(), Long::sum);
        }
        Map<String, String> formatted = new LinkedHashMap<>();
        for (Map.Entry<String, Long> entry : totals.entrySet()) {
            String currencyCode = entry.getKey();
            try {
                Currency currency = Currency.valueOf(currencyCode);
                Price price = new Price(currency, entry.getValue());
                formatted.put(currencyCode, pricingService.format(price));
            } catch (IllegalArgumentException ex) {
                // Skip unknown currency codes rather than failing the entire cart view
            }
        }
        return formatted;
    }

    public void addOrUpdateLine(ProductKey key,
                                String name,
                                Price unitPrice,
                                int qty,
                                PaymentMode mode,
                                boolean deliverEmail,
                                boolean deliverSms,
                                String email,
                                String msisdn) {
        Objects.requireNonNull(key, "key");
        Objects.requireNonNull(unitPrice, "unitPrice");
        Objects.requireNonNull(mode, "mode");
        Optional<CartLineDTO> existingLine = findLine(key.sku());
        CartLineDTO line;
        boolean isNewItem = existingLine.isEmpty();
        
        if (isNewItem) {
            line = new CartLineDTO();
            line.setSku(key.sku());
            line.setFamily(key.family());
            lines.add(line);
        } else {
            line = existingLine.get();
        }
        
        line.setName(name);
        // If item already exists, add to existing quantity; otherwise set the quantity
        if (isNewItem) {
            line.setQty(qty);
        } else {
            line.setQty(line.getQty() + qty);
        }
        line.setUnitPriceMinor(unitPrice.amountMinor());
        line.setTotalMinor(Math.multiplyExact(unitPrice.amountMinor(), line.getQty()));
        line.setPriceFormatted(pricingService.format(unitPrice));
        line.setTotalFormatted(pricingService.format(new Price(unitPrice.currency(), line.getTotalMinor())));
        line.setCurrency(unitPrice.currency().name());
        line.setPaymentMode(mode);
        line.setDeliverEmail(deliverEmail);
        line.setDeliverSms(deliverSms);
        line.setEmail(email);
        line.setMsisdn(msisdn);
    }

    public void removeLine(String sku) {
        lines.removeIf(line -> Objects.equals(line.getSku(), sku));
        FacesContext context = FacesContext.getCurrentInstance();
        if (context != null) {
            context.addMessage(null, new FacesMessage(FacesMessage.SEVERITY_INFO, "Removed item from cart.", null));
        }
    }

    public void updateLineQuantity(String sku, int qty) {
        Optional<CartLineDTO> lineOpt = findLine(sku);
        if (lineOpt.isEmpty()) {
            FacesContext context = FacesContext.getCurrentInstance();
            if (context != null) {
                context.addMessage(null, new FacesMessage(FacesMessage.SEVERITY_ERROR, "Cart item could not be found.", null));
            }
            return;
        }
        CartLineDTO line = lineOpt.get();
        int normalizedQty = Math.max(1, Math.min(qty, 10));
        updateLineDetails(sku,
                normalizedQty,
                line.getPaymentMode(),
                line.isDeliverEmail(),
                line.isDeliverSms(),
                line.getEmail(),
                line.getMsisdn());
        FacesContext context = FacesContext.getCurrentInstance();
        if (context != null) {
            context.addMessage(null, new FacesMessage(FacesMessage.SEVERITY_INFO,
                    "Updated quantity to " + normalizedQty + ".",
                    null));
        }
    }

    public void clear() {
        lines.clear();
    }

    public String goToCheckout() {
        if (getHasNoItems()) {
            FacesContext context = FacesContext.getCurrentInstance();
            if (context != null) {
                context.addMessage(null, new FacesMessage(FacesMessage.SEVERITY_WARN,
                        "Add an item before checking out.", null));
            }
            return null;
        }
        return "/checkout?faces-redirect=true";
    }

    public boolean updateLineDetails(String sku,
                                     int qty,
                                     PaymentMode mode,
                                     boolean deliverEmail,
                                     boolean deliverSms,
                                     String email,
                                     String msisdn) {
        Optional<CartLineDTO> lineOpt = findLine(sku);
        if (lineOpt.isEmpty()) {
            return false;
        }
        CartLineDTO line = lineOpt.get();
        line.setQty(qty);
        line.setPaymentMode(mode);
        line.setDeliverEmail(deliverEmail);
        line.setDeliverSms(deliverSms);
        line.setEmail(email);
        line.setMsisdn(msisdn);
        line.setTotalMinor(Math.multiplyExact(line.getUnitPriceMinor(), qty));
        if (line.getCurrency() != null && !line.getCurrency().isBlank()) {
            try {
                Currency currency = Currency.valueOf(line.getCurrency());
                Price unitPrice = new Price(currency, line.getUnitPriceMinor());
                line.setPriceFormatted(pricingService.format(unitPrice));
                Price totalPrice = new Price(currency, line.getTotalMinor());
                line.setTotalFormatted(pricingService.format(totalPrice));
            } catch (IllegalArgumentException ex) {
                // Leave formatted fields unchanged for unknown currencies.
            }
        }
        return true;
    }

    public String checkoutAll(PaymentMode mode,
                               boolean deliverEmail,
                               boolean deliverSms,
                               String email,
                               String msisdn) {
        if (lines.isEmpty()) {
            FacesContext context = FacesContext.getCurrentInstance();
            if (context != null) {
                context.addMessage(null, new FacesMessage(FacesMessage.SEVERITY_WARN, "Your cart is empty.", null));
            }
            return null;
        }
        Objects.requireNonNull(mode, "mode");
        String normalizedEmail = email == null ? "" : email;
        String normalizedMsisdn = msisdn == null ? "" : msisdn;
        Contact contact = new Contact(normalizedEmail, normalizedMsisdn);
        DeliveryPrefs prefs = new DeliveryPrefs(deliverEmail, deliverSms);
        List<CheckoutReference> references = new ArrayList<>();
        try {
            for (CartLineDTO line : new ArrayList<>(lines)) {
                line.setPaymentMode(mode);
                line.setDeliverEmail(deliverEmail);
                line.setDeliverSms(deliverSms);
                line.setEmail(normalizedEmail);
                line.setMsisdn(normalizedMsisdn);
                ProductKey key = new ProductKey(Optional.ofNullable(line.getFamily()).orElse(ProductFamily.ENROLLMENT), line.getSku());
                if (mode == PaymentMode.PAY_NOW) {
                    String orderId = paymentService.payNow(key, line.getQty(), contact, prefs);
                    references.add(new CheckoutReference(CheckoutReference.Type.ORDER, orderId));
                } else {
                    String invoiceNo = paymentService.payLater(key, line.getQty(), contact, prefs);
                    references.add(new CheckoutReference(CheckoutReference.Type.INVOICE, invoiceNo));
                }
            }
        } catch (Exception ex) {
            FacesContext context = FacesContext.getCurrentInstance();
            if (context != null) {
                context.addMessage(null, new FacesMessage(FacesMessage.SEVERITY_ERROR, ex.getMessage(), null));
            }
            return null;
        }
        lines.clear();
        String token = storeCheckoutResult(new CheckoutResult(mode, references));
        return "/checkout-result?faces-redirect=true&token=" + URLEncoder.encode(token, StandardCharsets.UTF_8);
    }

    private Optional<CartLineDTO> findLine(String sku) {
        return lines.stream().filter(line -> Objects.equals(line.getSku(), sku)).findFirst();
    }

    public CheckoutResult consumeCheckoutResult(String token) {
        if (token == null || token.isBlank()) {
            return null;
        }
        return checkoutResults.remove(token);
    }

    private String storeCheckoutResult(CheckoutResult result) {
        String token = UUID.randomUUID().toString();
        checkoutResults.put(token, result);
        return token;
    }

    public static final class CheckoutResult implements Serializable {

        @Serial
        private static final long serialVersionUID = 1L;

        private final PaymentMode paymentMode;
        private final List<CheckoutReference> references;

        public CheckoutResult(PaymentMode paymentMode, List<CheckoutReference> references) {
            this.paymentMode = Objects.requireNonNull(paymentMode, "paymentMode");
            this.references = List.copyOf(Objects.requireNonNull(references, "references"));
        }

        public PaymentMode getPaymentMode() {
            return paymentMode;
        }

        public List<CheckoutReference> getReferences() {
            return references;
        }
    }

    public static final class CheckoutReference implements Serializable {

        @Serial
        private static final long serialVersionUID = 1L;

        public enum Type {
            ORDER,
            INVOICE
        }

        private final Type type;
        private final String reference;

        public CheckoutReference(Type type, String reference) {
            this.type = Objects.requireNonNull(type, "type");
            this.reference = Objects.requireNonNull(reference, "reference");
        }

        public Type getType() {
            return type;
        }

        public String getReference() {
            return reference;
        }
    }

}
