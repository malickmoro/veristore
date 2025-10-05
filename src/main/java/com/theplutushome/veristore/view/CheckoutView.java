package com.theplutushome.veristore.view;

import com.theplutushome.veristore.domain.Currency;
import com.theplutushome.veristore.domain.PaymentMode;
import com.theplutushome.veristore.domain.Price;
import com.theplutushome.veristore.dto.CartLineDTO;
import com.theplutushome.veristore.service.PricingService;

import jakarta.annotation.PostConstruct;
import jakarta.faces.application.FacesMessage;
import jakarta.faces.context.ExternalContext;
import jakarta.faces.context.FacesContext;
import jakarta.faces.view.ViewScoped;
import jakarta.inject.Inject;
import jakarta.inject.Named;

import java.io.Serial;
import java.io.Serializable;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.regex.Pattern;

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

    private CartLineDTO line;
    private boolean missing;
    private boolean initialized;

    private int qty;
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
        qty = 1;
    }

    public void load() {
        if (initialized) {
            return;
        }
        initialized = true;
        FacesContext context = FacesContext.getCurrentInstance();
        if (context == null) {
            missing = true;
            return;
        }
        ExternalContext externalContext = context.getExternalContext();
        Map<String, String> params = externalContext.getRequestParameterMap();
        String sku = params.get("sku");
        if (sku == null || sku.isBlank()) {
            missing = true;
            return;
        }
        CartLineDTO existing = cartView.getLine(sku);
        if (existing == null) {
            missing = true;
            return;
        }
        line = existing;
        qty = Math.max(1, existing.getQty());
        paymentMode = existing.getPaymentMode() == null ? PaymentMode.PAY_NOW : existing.getPaymentMode();
        deliverEmail = existing.isDeliverEmail();
        deliverSms = existing.isDeliverSms();
        email = existing.getEmail() == null ? "" : existing.getEmail();
        msisdn = existing.getMsisdn() == null ? "" : existing.getMsisdn();
    }

    public boolean isMissing() {
        return missing;
    }

    public CartLineDTO getLine() {
        return line;
    }

    public int getQty() {
        return qty;
    }

    public void setQty(int qty) {
        if (qty < 1) {
            this.qty = 1;
        } else if (qty > 10) {
            this.qty = 10;
        } else {
            this.qty = qty;
        }
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

    public PaymentMode[] getPaymentModes() {
        return PaymentMode.values();
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
            case PAY_NOW -> "Checkout instantly and receive masked PINs immediately.";
            case PAY_LATER -> "Generate an invoice for bank payment and redeem later.";
        };
    }

    public String getUnitPriceFormatted() {
        Price unitPrice = unitPrice();
        return unitPrice == null ? "" : pricingService.format(unitPrice);
    }

    public String getTotalPriceFormatted() {
        Price totalPrice = totalPrice();
        return totalPrice == null ? "" : pricingService.format(totalPrice);
    }

    public String getCurrencyCode() {
        return line == null ? "" : line.getCurrency();
    }

    public String getSubmitLabel() {
        if (paymentMode == PaymentMode.PAY_LATER) {
            return "Generate invoice";
        }
        return "Complete checkout";
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

    public String submit() {
        if (line == null) {
            return null;
        }
        if (!validate()) {
            return null;
        }
        boolean updated = cartView.updateLineDetails(line.getSku(), qty, paymentMode, deliverEmail, deliverSms, email, msisdn);
        if (!updated) {
            addMessage(null, FacesMessage.SEVERITY_ERROR, "The cart item could not be found.");
            return null;
        }
        return cartView.checkoutLine(line.getSku());
    }

    public String cancel() {
        return "/index?faces-redirect=true";
    }

    private boolean validate() {
        boolean valid = true;
        if (qty < 1 || qty > 10) {
            addMessage(componentId("quantity"), FacesMessage.SEVERITY_ERROR, "Quantity must be between 1 and 10.");
            valid = false;
        }
        if (!deliverEmail && !deliverSms) {
            addMessage(componentId("deliveryOptions"), FacesMessage.SEVERITY_ERROR, "Choose at least one delivery channel.");
            valid = false;
        }
        if (deliverEmail) {
            if (email == null || email.isBlank()) {
                addMessage(componentId("email"), FacesMessage.SEVERITY_ERROR, "Enter an email address for delivery.");
                valid = false;
            } else if (!EMAIL_PATTERN.matcher(email).matches()) {
                addMessage(componentId("email"), FacesMessage.SEVERITY_ERROR, "Enter a valid email address.");
                valid = false;
            }
        }
        if (deliverSms) {
            if (msisdn == null || msisdn.isBlank()) {
                addMessage(componentId("msisdn"), FacesMessage.SEVERITY_ERROR, "Enter a phone number including country code.");
                valid = false;
            } else if (!PHONE_PATTERN.matcher(msisdn).matches()) {
                addMessage(componentId("msisdn"), FacesMessage.SEVERITY_ERROR, "Use international format starting with + and digits.");
                valid = false;
            }
        }
        if (!valid) {
            FacesContext context = FacesContext.getCurrentInstance();
            if (context != null) {
                context.validationFailed();
            }
        }
        return valid;
    }

    private Price unitPrice() {
        if (line == null || line.getCurrency() == null || line.getCurrency().isBlank()) {
            return null;
        }
        try {
            Currency currency = Currency.valueOf(line.getCurrency());
            return new Price(currency, line.getUnitPriceMinor());
        } catch (IllegalArgumentException ex) {
            return null;
        }
    }

    private Price totalPrice() {
        Price unit = unitPrice();
        if (unit == null) {
            return null;
        }
        return new Price(unit.currency(), Math.multiplyExact(unit.amountMinor(), qty));
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
