package com.theplutushome.veristore.view;

import com.theplutushome.veristore.catalog.CatalogLabels;
import com.theplutushome.veristore.catalog.EnrollmentSku;
import com.theplutushome.veristore.catalog.ProductFamily;
import com.theplutushome.veristore.catalog.ProductKey;
import com.theplutushome.veristore.catalog.VerificationSku;
import com.theplutushome.veristore.domain.ApplicationType;
import com.theplutushome.veristore.domain.CitizenTier;
import com.theplutushome.veristore.domain.CitizenshipType;
import com.theplutushome.veristore.domain.Contact;
import com.theplutushome.veristore.domain.DeliveryPrefs;
import com.theplutushome.veristore.domain.PaymentMode;
import com.theplutushome.veristore.domain.Price;
import com.theplutushome.veristore.domain.UpdateType;
import com.theplutushome.veristore.payment.PaymentService;
import com.theplutushome.veristore.service.PricingService;

import jakarta.annotation.PostConstruct;
import jakarta.faces.application.FacesMessage;
import jakarta.faces.context.ExternalContext;
import jakarta.faces.context.FacesContext;
import jakarta.faces.view.ViewScoped;
import jakarta.inject.Inject;
import jakarta.inject.Named;
import java.io.IOException;
import java.io.Serial;
import java.io.Serializable;
import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.util.Arrays;
import java.util.List;
import java.util.Objects;
import java.util.regex.Pattern;

@Named
@ViewScoped
public class PurchaseView implements Serializable {

    @Serial
    private static final long serialVersionUID = 1L;

    private static final String FORM_ID = "purchaseForm";
    private static final Pattern EMAIL_PATTERN = Pattern.compile("^[^@\\s]+@[^@\\s]+\\.[^@\\s]+$");
    private static final Pattern PHONE_PATTERN = Pattern.compile("^\\+[0-9]{7,15}$");

    @Inject
    private PaymentService paymentService;

    @Inject
    private PricingService pricingService;

    private ApplicationType appType;
    private CitizenshipType citizenship;
    private CitizenTier citizenTier;
    private UpdateType updateType;
    private String selectedSku;
    private int qty;
    private String email;
    private String msisdn;
    private boolean deliverEmail;
    private boolean deliverSms;
    private PaymentMode mode;

    @PostConstruct
    public void init() {
        qty = 1;
        email = "";
        msisdn = "";
        deliverEmail = true;
        deliverSms = false;
        mode = PaymentMode.PAY_NOW;
    }

    public void prepareFirstIssuance() {
        setAppType(ApplicationType.FIRST_ISSUANCE);
    }

    public void prepareRenewal() {
        setAppType(ApplicationType.RENEWAL);
    }

    public void prepareReplacement() {
        setAppType(ApplicationType.REPLACEMENT);
    }

    public void prepareUpdate() {
        setAppType(ApplicationType.UPDATE);
    }

    public void prepareVerification() {
        setAppType(ApplicationType.VERIFICATION);
    }

    public ApplicationType getAppType() {
        return appType;
    }

    public CitizenshipType getCitizenship() {
        return citizenship;
    }

    public void setCitizenship(CitizenshipType citizenship) {
        if (!Objects.equals(this.citizenship, citizenship)) {
            this.citizenship = citizenship;
            if (citizenship != CitizenshipType.CITIZEN) {
                this.citizenTier = null;
            }
            if (appType != ApplicationType.UPDATE) {
                this.updateType = null;
            }
            this.selectedSku = null;
        }
    }

    public CitizenTier getCitizenTier() {
        return citizenTier;
    }

    public void setCitizenTier(CitizenTier citizenTier) {
        if (!Objects.equals(this.citizenTier, citizenTier)) {
            this.citizenTier = citizenTier;
            this.selectedSku = null;
        }
    }

    public UpdateType getUpdateType() {
        return updateType;
    }

    public void setUpdateType(UpdateType updateType) {
        if (!Objects.equals(this.updateType, updateType)) {
            this.updateType = updateType;
            this.selectedSku = null;
        }
    }

    public String getSelectedSku() {
        return selectedSku;
    }

    public void setSelectedSku(String selectedSku) {
        this.selectedSku = selectedSku;
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

    public String getEmail() {
        return email;
    }

    public void setEmail(String email) {
        this.email = email == null ? "" : email.trim();
    }

    public String getMsisdn() {
        return msisdn;
    }

    public void setMsisdn(String msisdn) {
        this.msisdn = msisdn == null ? "" : msisdn.trim();
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

    public PaymentMode getMode() {
        return mode;
    }

    public void setMode(PaymentMode mode) {
        if (mode != null) {
            this.mode = mode;
        }
    }

    public List<CitizenshipType> getCitizenshipsForApp() {
        return List.of(CitizenshipType.CITIZEN, CitizenshipType.NON_CITIZEN, CitizenshipType.REFUGEE);
    }

    public List<CitizenTier> getTiersForCitizen() {
        if (appType == null || appType == ApplicationType.VERIFICATION) {
            return List.of();
        }
        if (citizenship == CitizenshipType.CITIZEN) {
            return Arrays.asList(CitizenTier.values());
        }
        return List.of();
    }

    public List<UpdateType> getUpdateTypes() {
        if (appType == ApplicationType.UPDATE) {
            return Arrays.asList(UpdateType.values());
        }
        return List.of();
    }

    public List<EnrollmentSku> getVariants() {
        if (appType == null || appType == ApplicationType.VERIFICATION) {
            return List.of();
        }
        if (citizenship == null) {
            return List.of();
        }
        if (citizenship == CitizenshipType.CITIZEN && isTierRequired() && citizenTier == null) {
            return List.of();
        }
        List<EnrollmentSku> variants = switch (appType) {
            case FIRST_ISSUANCE -> EnrollmentSku.filter(citizenship, ApplicationType.FIRST_ISSUANCE, null, citizenTier);
            case RENEWAL -> EnrollmentSku.renewalsFor(citizenship);
            case REPLACEMENT -> EnrollmentSku.filter(citizenship, ApplicationType.REPLACEMENT, null, citizenTier);
            case UPDATE -> {
                if (updateType == null) {
                    yield List.of();
                }
                yield EnrollmentSku.filter(citizenship, ApplicationType.UPDATE, updateType, citizenTier);
            }
            case VERIFICATION -> List.of();
        };
        syncSelectedSku(variants.stream().map(sku -> sku.sku).toList());
        return variants;
    }

    public List<VerificationSku> getVerificationVariants() {
        if (appType != ApplicationType.VERIFICATION) {
            return List.of();
        }
        List<VerificationSku> variants = Arrays.asList(VerificationSku.values());
        syncSelectedSku(variants.stream().map(sku -> sku.sku).toList());
        return variants;
    }

    public Price getUnitPrice() {
        if (selectedSku == null || selectedSku.isBlank()) {
            return null;
        }
        if (appType == ApplicationType.VERIFICATION) {
            return VerificationSku.bySku(selectedSku)
                .map(VerificationSku::price)
                .orElse(null);
        }
        if (appType == null) {
            return null;
        }
        return EnrollmentSku.bySku(selectedSku)
            .map(EnrollmentSku::price)
            .orElse(null);
    }

    public String getUnitPriceFormatted() {
        Price price = getUnitPrice();
        return price == null ? "" : pricingService.format(price);
    }

    public String getUnitLabel() {
        if (selectedSku == null || selectedSku.isBlank()) {
            return "";
        }
        if (appType == ApplicationType.VERIFICATION) {
            return VerificationSku.bySku(selectedSku)
                .map(sku -> sku.displayName)
                .orElse(selectedSku);
        }
        if (appType == null) {
            return "";
        }
        return EnrollmentSku.bySku(selectedSku)
            .map(sku -> sku.displayName)
            .orElse(selectedSku);
    }

    public String getTotalFormatted() {
        Price price = getUnitPrice();
        if (price == null) {
            return "";
        }
        long totalMinor = Math.multiplyExact(price.amountMinor(), qty);
        return pricingService.format(new Price(price.currency(), totalMinor));
    }

    public boolean isEnrollmentFlow() {
        return appType != null && appType != ApplicationType.VERIFICATION;
    }

    public boolean isVerificationFlow() {
        return appType == ApplicationType.VERIFICATION;
    }

    public boolean isTierRequired() {
        return isEnrollmentFlow() && citizenship == CitizenshipType.CITIZEN;
    }

    public boolean isUpdateTypeRequired() {
        return appType == ApplicationType.UPDATE;
    }

    public String labelForCitizenship(CitizenshipType type) {
        return switch (type) {
            case CITIZEN -> "Citizen";
            case NON_CITIZEN -> "Non-citizen";
            case REFUGEE -> "Refugee";
        };
    }

    public String labelForTier(CitizenTier tier) {
        return CatalogLabels.citizenTierLabel(tier);
    }

    public String labelForUpdate(UpdateType type) {
        return CatalogLabels.updateLabel(type);
    }

    public String submitEnrollment() {
        if (!validateEnrollment()) {
            return null;
        }
        return completeSubmission(new ProductKey(ProductFamily.ENROLLMENT, selectedSku));
    }

    public String submitVerification() {
        if (!validateVerification()) {
            return null;
        }
        return completeSubmission(new ProductKey(ProductFamily.VERIFICATION, selectedSku));
    }

    private boolean validateEnrollment() {
        boolean valid = true;
        if (citizenship == null) {
            addMessage(componentId("citizenship"), FacesMessage.SEVERITY_ERROR, "Select a citizenship.");
            valid = false;
        }
        if (citizenship == CitizenshipType.CITIZEN && citizenTier == null) {
            addMessage(componentId("tier"), FacesMessage.SEVERITY_ERROR, "Select a citizen tier.");
            valid = false;
        }
        if (appType == ApplicationType.UPDATE && updateType == null) {
            addMessage(componentId("updateType"), FacesMessage.SEVERITY_ERROR, "Select an update type.");
            valid = false;
        }
        List<EnrollmentSku> variants = getVariants();
        if (variants.isEmpty()) {
            addMessage(componentId("variant"), FacesMessage.SEVERITY_ERROR, "No variants are available for the chosen options.");
            valid = false;
        } else if (selectedSku == null || variants.stream().noneMatch(sku -> sku.sku.equals(selectedSku))) {
            addMessage(componentId("variant"), FacesMessage.SEVERITY_ERROR, "Choose a variant.");
            valid = false;
        }
        if (!validateCommon()) {
            valid = false;
        }
        if (!valid) {
            FacesContext.getCurrentInstance().validationFailed();
        }
        return valid;
    }

    private boolean validateVerification() {
        boolean valid = true;
        List<VerificationSku> variants = getVerificationVariants();
        if (variants.isEmpty()) {
            addMessage(componentId("verificationVariant"), FacesMessage.SEVERITY_ERROR, "No verification variants are available.");
            valid = false;
        } else if (selectedSku == null || variants.stream().noneMatch(sku -> sku.sku.equals(selectedSku))) {
            addMessage(componentId("verificationVariant"), FacesMessage.SEVERITY_ERROR, "Choose a verification duration.");
            valid = false;
        }
        if (!validateCommon()) {
            valid = false;
        }
        if (!valid) {
            FacesContext.getCurrentInstance().validationFailed();
        }
        return valid;
    }

    private boolean validateCommon() {
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
        return valid;
    }

    private String completeSubmission(ProductKey key) {
        try {
            Contact contact = new Contact(email, msisdn);
            DeliveryPrefs deliveryPrefs = new DeliveryPrefs(deliverEmail, deliverSms);
            if (mode == PaymentMode.PAY_NOW) {
                String orderId = paymentService.payNow(key, qty, contact, deliveryPrefs);
                queueMessage(FacesMessage.SEVERITY_INFO, "Order " + orderId + " completed successfully.");
                return redirectTo("success", "orderId", orderId);
            }
            String invoiceNo = paymentService.payLater(key, qty, contact, deliveryPrefs);
            queueMessage(FacesMessage.SEVERITY_INFO, "Invoice " + invoiceNo + " generated.");
            return redirectTo("invoice", "no", invoiceNo);
        } catch (Exception ex) {
            addMessage(null, FacesMessage.SEVERITY_ERROR, ex.getMessage());
            return null;
        }
    }

    private void syncSelectedSku(List<String> availableSkus) {
        if (availableSkus.isEmpty()) {
            selectedSku = null;
            return;
        }
        if (selectedSku == null || availableSkus.stream().noneMatch(value -> value.equals(selectedSku))) {
            selectedSku = availableSkus.get(0);
        }
    }

    private String componentId(String id) {
        return FORM_ID + ":" + id;
    }

    private void addMessage(String clientId, FacesMessage.Severity severity, String message) {
        FacesContext context = FacesContext.getCurrentInstance();
        context.addMessage(clientId, new FacesMessage(severity, message, null));
    }

    private void queueMessage(FacesMessage.Severity severity, String message) {
        FacesContext context = FacesContext.getCurrentInstance();
        context.addMessage(null, new FacesMessage(severity, message, null));
        context.getExternalContext().getFlash().setKeepMessages(true);
    }

    private String redirectTo(String view, String paramName, String value) throws IOException {
        FacesContext context = FacesContext.getCurrentInstance();
        ExternalContext externalContext = context.getExternalContext();
        String encoded = URLEncoder.encode(value, StandardCharsets.UTF_8);
        String url = externalContext.getRequestContextPath() + "/" + view + ".xhtml?" + paramName + "=" + encoded;
        externalContext.redirect(url);
        context.responseComplete();
        return null;
    }

    private void setAppType(ApplicationType newType) {
        if (newType == null) {
            return;
        }
        if (this.appType != newType) {
            this.appType = newType;
            if (newType == ApplicationType.VERIFICATION) {
                this.citizenship = null;
                this.citizenTier = null;
                this.updateType = null;
            } else {
                if (citizenship != CitizenshipType.CITIZEN) {
                    this.citizenTier = null;
                }
                if (newType != ApplicationType.UPDATE) {
                    this.updateType = null;
                }
            }
            this.selectedSku = null;
        }
    }
}
