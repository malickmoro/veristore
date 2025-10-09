package com.theplutushome.veristore.controller;

import com.theplutushome.veristore.model.Currency;
import com.theplutushome.veristore.model.PaymentMode;
import com.theplutushome.veristore.model.Price;
import com.theplutushome.veristore.model.dto.CartLineDTO;
import com.theplutushome.veristore.service.PricingService;

import jakarta.annotation.PostConstruct;
import jakarta.faces.application.FacesMessage;
import jakarta.faces.context.FacesContext;
import jakarta.faces.view.ViewScoped;
import jakarta.inject.Inject;
import jakarta.inject.Named;

import java.io.Serial;
import java.io.Serializable;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.regex.Pattern;

import static com.theplutushome.veristore.model.PaymentMode.PAY_LATER;
import static com.theplutushome.veristore.model.PaymentMode.PAY_NOW;

@Named
@ViewScoped
public class CheckoutView implements Serializable {

    @Serial
    private static final long serialVersionUID = 1L;

    private static final Pattern EMAIL_PATTERN = Pattern.compile("^[^@\\s]+@[^@\\s]+\\.[^@\\s]+$");
    private static final Pattern PHONE_PATTERN = Pattern.compile("^\\+[0-9]{7,15}$");

    @Inject
    private CartView cartView;

    @Inject
    private PricingService pricingService;

    private final List<CartLineDTO> lines = new ArrayList<>();
    private boolean missing;
    private boolean initialized;

    private PaymentMode paymentMode;
    private boolean deliverEmail;
    private boolean deliverSms;
    private String email;
    private String msisdn;

    @PostConstruct
    public void init() {
        email = "";
        msisdn = "";
        paymentMode = PaymentMode.PAY_NOW;
        deliverEmail = true;
        deliverSms = true;
    }

    public void load() {
        if (initialized) {
            return;
        }
        initialized = true;
        lines.clear();
        lines.addAll(cartView.getLineCopies());
        if (lines.isEmpty()) {
            missing = true;
            return;
        }
        CartLineDTO first = lines.get(0);
        paymentMode = Optional.ofNullable(first.getPaymentMode()).orElse(PaymentMode.PAY_NOW);
        deliverEmail = true;
        deliverSms = true;
        email = Optional.ofNullable(first.getEmail()).orElse("");
        msisdn = Optional.ofNullable(first.getMsisdn()).orElse("");
    }

    public boolean isMissing() {
        return missing;
    }

    public List<CartLineDTO> getLines() {
        return lines;
    }

    public PaymentMode[] getPaymentModes() {
        return PaymentMode.values();
    }

    public PaymentMode getPaymentMode() {
        return paymentMode;
    }

    public void setPaymentMode(PaymentMode paymentMode) {
        this.paymentMode = paymentMode;
    }

    public String selectPaymentMode(PaymentMode mode) {
        setPaymentMode(mode);
        return null;
    }

    public boolean isDeliverEmail() {
        return deliverEmail;
    }

    public void setDeliverEmail(boolean deliverEmail) {
        this.deliverEmail = deliverEmail;
    }

    public boolean isDeliverSms() {
        return deliverSms;
    }

    public void setDeliverSms(boolean deliverSms) {
        this.deliverSms = deliverSms;
    }

    public String getEmail() {
        return email;
    }

    public void setEmail(String email) {
        this.email = email;
    }

    public String getMsisdn() {
        return msisdn;
    }

    public void setMsisdn(String msisdn) {
        this.msisdn = msisdn;
    }

    public boolean isPaymentModeSelected(PaymentMode mode) {
        return mode != null && mode == paymentMode;
    }

    public String paymentModeTitle(PaymentMode mode) {
        if (mode == null) {
            return "";
        }
        return switch (mode) {
            case PAY_NOW -> "Pay now";
            case PAY_LATER -> "Pay later";
        };
    }

    public String describePaymentMode(PaymentMode mode) {
        if (mode == null) {
            return "";
        }
        return switch (mode) {
            case PAY_NOW -> "Complete payment immediately and receive PINs right away.";
            case PAY_LATER -> "Generate an invoice to pay via bank and redeem later.";
        };
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
            try {
                Currency currency = Currency.valueOf(entry.getKey());
                Price price = new Price(currency, entry.getValue());
                formatted.put(entry.getKey(), pricingService.format(price));
            } catch (IllegalArgumentException ex) {
                // Skip unknown currencies
            }
        }
        return formatted;
    }

    public String getDeliverySummary() {
        List<String> channels = new ArrayList<>();
        if (deliverEmail) {
            channels.add("Email");
        }
        if (deliverSms) {
            channels.add("SMS");
        }
        if (channels.isEmpty()) {
            return "—";
        }
        return String.join(" & ", channels);
    }

    public String getSubmitLabel() {
        if (paymentMode == PaymentMode.PAY_LATER) {
            return "Generate invoice";
        }
        return "Pay now";
    }

    public String submit() {
        if (lines.isEmpty()) {
            return null;
        }
        if (!validate()) {
            return null;
        }
        return cartView.checkoutAll(paymentMode, deliverEmail, deliverSms, email, msisdn);
    }

    public String cancel() {
        return "/index?faces-redirect=true";
    }

    private boolean validate() {
        boolean valid = true;
        if (lines.isEmpty()) {
            addMessage(null, FacesMessage.SEVERITY_ERROR, "Your cart is empty.");
            valid = false;
        }
        if (email == null || email.isBlank()) {
            addMessage(componentId("email"), FacesMessage.SEVERITY_ERROR, "Enter an email address for delivery.");
            valid = false;
        } else if (!EMAIL_PATTERN.matcher(email).matches()) {
            addMessage(componentId("email"), FacesMessage.SEVERITY_ERROR, "Enter a valid email address.");
            valid = false;
        }
        if (msisdn == null || msisdn.isBlank()) {
            addMessage(componentId("msisdn"), FacesMessage.SEVERITY_ERROR, "Enter a phone number including country code.");
            valid = false;
        } else if (!PHONE_PATTERN.matcher(msisdn).matches()) {
            addMessage(componentId("msisdn"), FacesMessage.SEVERITY_ERROR, "Use international format starting with + and digits.");
            valid = false;
        }
        if (!valid) {
            FacesContext context = FacesContext.getCurrentInstance();
            if (context != null) {
                context.validationFailed();
            }
        }
        return valid;
    }

    private void addMessage(String clientId, FacesMessage.Severity severity, String summary) {
        FacesContext context = FacesContext.getCurrentInstance();
        if (context != null) {
            context.addMessage(clientId, new FacesMessage(severity, summary, null));
        }
    }

    private String componentId(String id) {
        return "checkoutForm:" + id;
    }
}
