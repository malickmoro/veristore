package com.theplutushome.veristore.view;

import com.theplutushome.veristore.domain.Contact;
import com.theplutushome.veristore.domain.DeliveryPrefs;
import com.theplutushome.veristore.domain.EnrollmentType;
import com.theplutushome.veristore.domain.PaymentMode;
import com.theplutushome.veristore.domain.PinCategory;
import com.theplutushome.veristore.domain.Price;
import com.theplutushome.veristore.domain.ServiceKey;
import com.theplutushome.veristore.payment.PaymentService;
import com.theplutushome.veristore.service.PricingService;
import com.theplutushome.veristore.util.VariantDescriptions;

import javax.annotation.PostConstruct;
import javax.faces.application.FacesMessage;
import javax.faces.context.ExternalContext;
import javax.faces.context.FacesContext;
import javax.faces.model.SelectItem;
import javax.faces.view.ViewScoped;
import javax.inject.Inject;
import javax.inject.Named;
import java.io.IOException;
import java.io.Serial;
import java.io.Serializable;
import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;

@Named
@ViewScoped
public class PurchaseView implements Serializable {

    @Serial
    private static final long serialVersionUID = 1L;

    private static final String[][] VERIFICATION_VARIANTS = new String[][]{
        {"Y1", "Verification PIN (1 year)"},
        {"Y2", "Verification PIN (2 years)"},
        {"Y3", "Verification PIN (3 years)"}
    };

    @Inject
    private PaymentService paymentService;

    @Inject
    private PricingService pricingService;

    private PinCategory category;
    private String variant;
    private int qty = 1;
    private Contact contact;
    private DeliveryPrefs deliveryPrefs;
    private PaymentMode paymentMode = PaymentMode.PAY_NOW;

    @PostConstruct
    public void init() {
        contact = new Contact("", "");
        deliveryPrefs = new DeliveryPrefs(true, false);
    }

    public void prepareVerification() {
        setCategory(PinCategory.VERIFICATION);
    }

    public void prepareEnrollment() {
        setCategory(PinCategory.ENROLLMENT);
    }

    public PinCategory getCategory() {
        return category;
    }

    public void setCategory(PinCategory category) {
        if (!Objects.equals(this.category, category)) {
            this.category = category;
            ensureVariant();
        }
    }

    public String getVariant() {
        ensureVariant();
        return variant;
    }

    public void setVariant(String variant) {
        this.variant = variant;
    }

    public int getQty() {
        return qty;
    }

    public void setQty(int qty) {
        this.qty = Math.max(1, qty);
    }

    public String getEmail() {
        return contact.email();
    }

    public void setEmail(String email) {
        String normalized = email == null ? "" : email.trim();
        contact = new Contact(normalized, contact.msisdn());
    }

    public String getMsisdn() {
        return contact.msisdn();
    }

    public void setMsisdn(String msisdn) {
        String normalized = msisdn == null ? "" : msisdn.trim();
        contact = new Contact(contact.email(), normalized);
    }

    public boolean isDeliverByEmail() {
        return deliveryPrefs.byEmail();
    }

    public void setDeliverByEmail(boolean deliverByEmail) {
        deliveryPrefs = new DeliveryPrefs(deliverByEmail, deliveryPrefs.bySms());
    }

    public boolean isDeliverBySms() {
        return deliveryPrefs.bySms();
    }

    public void setDeliverBySms(boolean deliverBySms) {
        deliveryPrefs = new DeliveryPrefs(deliveryPrefs.byEmail(), deliverBySms);
    }

    public PaymentMode getPaymentMode() {
        return paymentMode;
    }

    public void setPaymentMode(PaymentMode paymentMode) {
        if (paymentMode != null) {
            this.paymentMode = paymentMode;
        }
    }

    public List<SelectItem> getVariantOptions() {
        List<SelectItem> items = new ArrayList<>();
        if (category == PinCategory.VERIFICATION) {
            for (String[] variant : VERIFICATION_VARIANTS) {
                items.add(new SelectItem(variant[0], variant[1]));
            }
        } else if (category == PinCategory.ENROLLMENT) {
            for (EnrollmentType type : EnrollmentType.values()) {
                items.add(new SelectItem(type.name(), VariantDescriptions.describe(type)));
            }
        }
        return items;
    }

    public String getVariantLabel() {
        if (category == PinCategory.VERIFICATION) {
            for (String[] variant : VERIFICATION_VARIANTS) {
                if (Objects.equals(variant[0], getVariant())) {
                    return variant[1];
                }
            }
            return getVariant();
        }
        if (category == PinCategory.ENROLLMENT) {
            return VariantDescriptions.describe(category, getVariant());
        }
        return "";
    }

    public String getUnitPrice() {
        ServiceKey key = currentKey();
        if (key == null) {
            return "";
        }
        Price price = pricingService.get(key);
        return pricingService.format(price);
    }

    public String getTotal() {
        ServiceKey key = currentKey();
        if (key == null) {
            return "";
        }
        Price price = pricingService.get(key);
        long totalMinor = Math.multiplyExact(price.amountMinor(), getQty());
        return pricingService.format(new Price(price.currency(), totalMinor));
    }

    public String submit() {
        ServiceKey key = currentKey();
        if (key == null) {
            addMessage(FacesMessage.SEVERITY_ERROR, "Please select a service variant.");
            return null;
        }
        try {
            if (paymentMode == PaymentMode.PAY_NOW) {
                String orderId = paymentService.payNow(key, getQty(), contact, deliveryPrefs);
                queueMessage(FacesMessage.SEVERITY_INFO, "Order " + orderId + " completed successfully.");
                return redirectTo("success", "orderId", orderId);
            }
            String invoiceNo = paymentService.payLater(key, getQty(), contact, deliveryPrefs);
            queueMessage(FacesMessage.SEVERITY_INFO, "Invoice " + invoiceNo + " generated.");
            return redirectTo("invoice", "no", invoiceNo);
        } catch (Exception ex) {
            addMessage(FacesMessage.SEVERITY_ERROR, ex.getMessage());
            return null;
        }
    }

    public String submitPayNow() {
        paymentMode = PaymentMode.PAY_NOW;
        return submit();
    }

    public String submitPayLater() {
        paymentMode = PaymentMode.PAY_LATER;
        return submit();
    }

    private String redirectTo(String view, String paramName, String value) throws IOException {
        FacesContext context = FacesContext.getCurrentInstance();
        ExternalContext externalContext = context.getExternalContext();
        String encoded = URLEncoder.encode(value, StandardCharsets.UTF_8);
        String url = externalContext.getRequestContextPath() + "/" + view + ".xhtml?" + paramName + "=" + encoded;
        externalContext.getFlash().setKeepMessages(true);
        externalContext.redirect(url);
        context.responseComplete();
        return null;
    }

    private void ensureVariant() {
        if (category == null) {
            variant = null;
            return;
        }
        List<SelectItem> options = getVariantOptions();
        if (options.isEmpty()) {
            variant = null;
            return;
        }
        boolean match = options.stream().anyMatch(item -> Objects.equals(item.getValue(), variant));
        if (!match) {
            Object first = options.get(0).getValue();
            variant = first == null ? null : first.toString();
        }
    }

    private ServiceKey currentKey() {
        if (category == null || variant == null || variant.isBlank()) {
            return null;
        }
        return new ServiceKey(category, variant);
    }

    private void addMessage(FacesMessage.Severity severity, String message) {
        FacesContext context = FacesContext.getCurrentInstance();
        context.addMessage(null, new FacesMessage(severity, message, null));
    }

    private void queueMessage(FacesMessage.Severity severity, String message) {
        FacesContext context = FacesContext.getCurrentInstance();
        context.addMessage(null, new FacesMessage(severity, message, null));
        context.getExternalContext().getFlash().setKeepMessages(true);
    }
}
