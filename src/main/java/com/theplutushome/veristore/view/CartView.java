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
import jakarta.faces.context.ExternalContext;
import jakarta.faces.context.FacesContext;
import jakarta.inject.Inject;
import jakarta.inject.Named;

import java.io.IOException;
import java.io.Serial;
import java.io.Serializable;
import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;

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

    public List<CartLineDTO> getLines() {
        return lines;
    }

    public boolean isEmpty() {
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
        CartLineDTO line = findLine(key.sku()).orElseGet(() -> {
            CartLineDTO created = new CartLineDTO();
            created.setSku(key.sku());
            created.setFamily(key.family());
            lines.add(created);
            return created;
        });
        line.setName(name);
        line.setQty(qty);
        line.setUnitPriceMinor(unitPrice.amountMinor());
        line.setTotalMinor(Math.multiplyExact(unitPrice.amountMinor(), qty));
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

    public void clear() {
        lines.clear();
    }

    public String checkoutLine(String sku) {
        Optional<CartLineDTO> lineOpt = findLine(sku);
        if (lineOpt.isEmpty()) {
            return null;
        }
        CartLineDTO line = lineOpt.get();
        ProductKey key = new ProductKey(Optional.ofNullable(line.getFamily()).orElse(ProductFamily.ENROLLMENT), line.getSku());
        Contact contact = new Contact(line.getEmail(), line.getMsisdn());
        DeliveryPrefs prefs = new DeliveryPrefs(line.isDeliverEmail(), line.isDeliverSms());
        try {
            String destination;
            if (line.getPaymentMode() == PaymentMode.PAY_NOW) {
                String orderId = paymentService.payNow(key, line.getQty(), contact, prefs);
                destination = redirectUrl("success", "orderId", orderId);
            } else {
                String invoiceNo = paymentService.payLater(key, line.getQty(), contact, prefs);
                destination = redirectUrl("invoice", "no", invoiceNo);
            }
            lines.remove(line);
            return destination;
        } catch (Exception ex) {
            FacesContext context = FacesContext.getCurrentInstance();
            if (context != null) {
                context.addMessage(null, new FacesMessage(FacesMessage.SEVERITY_ERROR, ex.getMessage(), null));
            }
            return null;
        }
    }

    private Optional<CartLineDTO> findLine(String sku) {
        return lines.stream().filter(line -> Objects.equals(line.getSku(), sku)).findFirst();
    }

    public String paymentModeLabel(PaymentMode mode) {
        if (mode == null) {
            return "";
        }
        return switch (mode) {
            case PAY_NOW -> "Pay now";
            case PAY_LATER -> "Pay later";
        };
    }

    public String checkoutButtonLabel(PaymentMode mode) {
        if (mode == null) {
            return "Checkout";
        }
        return switch (mode) {
            case PAY_NOW -> "Checkout now";
            case PAY_LATER -> "Generate invoice";
        };
    }

    private String redirectUrl(String view, String paramName, String value) throws IOException {
        FacesContext context = FacesContext.getCurrentInstance();
        if (context == null) {
            return null;
        }
        ExternalContext externalContext = context.getExternalContext();
        String encoded = URLEncoder.encode(value, StandardCharsets.UTF_8);
        String url = externalContext.getRequestContextPath() + "/" + view + ".xhtml?" + paramName + "=" + encoded + "&faces-redirect=true";
        externalContext.redirect(url);
        context.responseComplete();
        return null;
    }
}
