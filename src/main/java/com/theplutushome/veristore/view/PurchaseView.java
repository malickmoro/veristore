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

    public enum WizardStep {
        CITIZENSHIP,
        TIER,
        UPDATE_TYPE,
        PACKAGE
    }

    @Serial
    private static final long serialVersionUID = 1L;

    private static final String FORM_ID = "purchaseForm";
    private static final Pattern EMAIL_PATTERN = Pattern.compile("^[^@\\s]+@[^@\\s]+\\.[^@\\s]+$");
    private static final Pattern PHONE_PATTERN = Pattern.compile("^\\+[0-9]{7,15}$");

    @Inject
    private PaymentService paymentService;

    @Inject
    private PricingService pricingService;

    @Inject
    private CartView cartView;

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
    private int wizardStep;

    @PostConstruct
    public void init() {
        qty = 1;
        email = "";
        msisdn = "";
        deliverEmail = true;
        deliverSms = false;
        mode = PaymentMode.PAY_NOW;
        wizardStep = 1;
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
            wizardStep = 1;
            normalizeWizardStep();
        }
    }

    public CitizenTier getCitizenTier() {
        return citizenTier;
    }

    public void setCitizenTier(CitizenTier citizenTier) {
        if (!Objects.equals(this.citizenTier, citizenTier)) {
            this.citizenTier = citizenTier;
            this.selectedSku = null;
            normalizeWizardStep();
        }
    }

    public UpdateType getUpdateType() {
        return updateType;
    }

    public void setUpdateType(UpdateType updateType) {
        if (!Objects.equals(this.updateType, updateType)) {
            this.updateType = updateType;
            this.selectedSku = null;
            normalizeWizardStep();
        }
    }

    public String getSelectedSku() {
        return selectedSku;
    }

    public void setSelectedSku(String selectedSku) {
        this.selectedSku = selectedSku;
    }

    public List<WizardStep> getWizardSteps() {
        if (appType == null) {
            return List.of();
        }
        return switch (appType) {
            case FIRST_ISSUANCE -> List.of(WizardStep.CITIZENSHIP, WizardStep.PACKAGE);
            case RENEWAL -> List.of(WizardStep.CITIZENSHIP, WizardStep.PACKAGE);
            case REPLACEMENT -> {
                if (citizenship == null || citizenship == CitizenshipType.CITIZEN) {
                    yield List.of(WizardStep.CITIZENSHIP, WizardStep.TIER, WizardStep.PACKAGE);
                }
                yield List.of(WizardStep.CITIZENSHIP, WizardStep.PACKAGE);
            }
            case UPDATE -> {
                boolean tierNeeded = citizenship == null || citizenship == CitizenshipType.CITIZEN;
                if (tierNeeded) {
                    yield List.of(WizardStep.CITIZENSHIP, WizardStep.TIER, WizardStep.UPDATE_TYPE, WizardStep.PACKAGE);
                }
                yield List.of(WizardStep.CITIZENSHIP, WizardStep.UPDATE_TYPE, WizardStep.PACKAGE);
            }
            case VERIFICATION -> List.of(WizardStep.PACKAGE);
        };
    }

    public WizardStep getCurrentWizardStep() {
        List<WizardStep> steps = getWizardSteps();
        if (steps.isEmpty() || wizardStep < 1) {
            return null;
        }
        int index = Math.min(wizardStep - 1, steps.size() - 1);
        return steps.get(index);
    }

    public WizardStep getCitizenshipStep() {
        return WizardStep.CITIZENSHIP;
    }

    public WizardStep getTierStep() {
        return WizardStep.TIER;
    }

    public WizardStep getUpdateTypeStep() {
        return WizardStep.UPDATE_TYPE;
    }

    public WizardStep getPackageStep() {
        return WizardStep.PACKAGE;
    }

    public boolean isCurrentStep(WizardStep step) {
        if (step == null) {
            return false;
        }
        WizardStep current = getCurrentWizardStep();
        return current != null && current == step;
    }

    public boolean isStepComplete(WizardStep step) {
        List<WizardStep> steps = getWizardSteps();
        int index = steps.indexOf(step);
        if (index < 0) {
            return false;
        }
        return index < wizardStep - 1 && isStepSatisfied(step);
    }

    public boolean isStepSatisfied(WizardStep step) {
        if (step == null) {
            return false;
        }
        return switch (step) {
            case CITIZENSHIP -> citizenship != null;
            case TIER -> citizenTier != null;
            case UPDATE_TYPE -> updateType != null;
            case PACKAGE -> selectedSku != null && !selectedSku.isBlank();
        };
    }

    public String wizardStepTitle(WizardStep step) {
        if (step == null) {
            return "";
        }
        return switch (step) {
            case CITIZENSHIP -> "Applicant category";
            case TIER -> "Service tier";
            case UPDATE_TYPE -> "Update type";
            case PACKAGE -> "Package";
        };
    }

    public String wizardStepCss(WizardStep step) {
        String base = "btn wizard-step w-100 flex-grow-1 text-start text-md-center";
        if (isCurrentStep(step)) {
            return base + " btn-primary";
        }
        if (isStepComplete(step)) {
            return base + " btn-success";
        }
        return base + " btn-outline-secondary";
    }

    public void nextWizardStep() {
        List<WizardStep> steps = getWizardSteps();
        if (steps.isEmpty()) {
            return;
        }
        int index = Math.min(wizardStep - 1, steps.size() - 1);
        WizardStep current = steps.get(index);
        if (!isStepSatisfied(current)) {
            showStepValidationMessage(current);
            return;
        }
        if (index < steps.size() - 1) {
            wizardStep = index + 2;
        }
    }

    public void previousWizardStep() {
        if (wizardStep > 1) {
            wizardStep--;
        }
    }

    public void goToWizardStep(WizardStep step) {
        List<WizardStep> steps = getWizardSteps();
        int targetIndex = steps.indexOf(step);
        if (targetIndex < 0) {
            return;
        }
        if (targetIndex < wizardStep - 1) {
            wizardStep = targetIndex + 1;
            return;
        }
        for (int i = 0; i <= targetIndex; i++) {
            WizardStep candidate = steps.get(i);
            if (!isStepSatisfied(candidate)) {
                wizardStep = i + 1;
                showStepValidationMessage(candidate);
                return;
            }
        }
        wizardStep = targetIndex + 1;
    }

    private void showStepValidationMessage(WizardStep step) {
        if (step == null) {
            return;
        }
        String message;
        String targetComponent;
        switch (step) {
            case CITIZENSHIP -> {
                message = "Choose an applicant category to continue.";
                targetComponent = componentId("citizenship");
            }
            case TIER -> {
                message = "Select a service tier to continue.";
                targetComponent = componentId("tier");
            }
            case UPDATE_TYPE -> {
                message = "Pick an update type to continue.";
                targetComponent = componentId("updateType");
            }
            case PACKAGE -> {
                message = "Choose a package to continue.";
                targetComponent = componentId("variant");
            }
            default -> throw new IllegalStateException("Unexpected step: " + step);
        }
        addMessage(targetComponent, FacesMessage.SEVERITY_ERROR, message);
    }

    private void normalizeWizardStep() {
        List<WizardStep> steps = getWizardSteps();
        if (steps.isEmpty()) {
            wizardStep = 1;
            return;
        }
        if (wizardStep < 1) {
            wizardStep = 1;
        }
        if (wizardStep > steps.size()) {
            wizardStep = steps.size();
        }
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

    public void selectCitizenship(CitizenshipType choice) {
        setCitizenship(choice);
    }

    public boolean isCitizenshipSelected(CitizenshipType type) {
        return type != null && type == citizenship;
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

    public void selectCitizenTier(CitizenTier tier) {
        setCitizenTier(tier);
    }

    public boolean isCitizenTierSelected(CitizenTier tier) {
        return tier != null && tier == citizenTier;
    }

    public List<UpdateType> getUpdateTypes() {
        if (appType == ApplicationType.UPDATE) {
            return Arrays.asList(UpdateType.values());
        }
        return List.of();
    }

    public void selectUpdateType(UpdateType type) {
        setUpdateType(type);
    }

    public boolean isUpdateTypeSelected(UpdateType type) {
        return type != null && type == updateType;
    }

    public List<EnrollmentSku> getVariants() {
        if (appType == null || appType == ApplicationType.VERIFICATION) {
            return List.of();
        }
        if (citizenship == null) {
            return List.of();
        }
        // For renewals and first issuance, tier is not required - show all packages
        // For replacements and updates, tier is required for citizens only
        if (appType != ApplicationType.RENEWAL && appType != ApplicationType.FIRST_ISSUANCE) {
            if (appType == ApplicationType.UPDATE || appType == ApplicationType.REPLACEMENT) {
                // For updates and replacements, only require tier for citizens
                if (citizenship == CitizenshipType.CITIZEN && citizenTier == null) {
                    return List.of();
                }
            } else {
                // For other types, use the standard tier requirement logic
                if (citizenship == CitizenshipType.CITIZEN && isTierRequired() && citizenTier == null) {
                    return List.of();
                }
            }
        }
        List<EnrollmentSku> variants = switch (appType) {
            case FIRST_ISSUANCE ->
                EnrollmentSku.filter(citizenship, ApplicationType.FIRST_ISSUANCE, null, citizenTier);
            case RENEWAL ->
                EnrollmentSku.renewalsFor(citizenship);
            case REPLACEMENT ->
                EnrollmentSku.filter(citizenship, ApplicationType.REPLACEMENT, null, citizenTier);
            case UPDATE -> {
                // Update type is no longer required - show all update packages
                yield EnrollmentSku.filter(citizenship, ApplicationType.UPDATE, null, citizenTier);
            }
            case VERIFICATION ->
                List.of();
        };
        syncSelectedSku(variants.stream().map(sku -> sku.sku).toList());
        return variants;
    }

    public List<VerificationSku> getVerificationVariants() {
        if (appType != ApplicationType.VERIFICATION) {
            return List.of();
        }
        List<VerificationSku> variants = Arrays.stream(VerificationSku.values())
                .filter(variant -> variant.getCategory() == VerificationSku.VerificationSkuCategory.DURATION)
                .toList();
        syncSelectedSku(variants.stream().map(sku -> sku.sku).toList());
        return variants;
    }

    public void selectSku(String sku) {
        if (sku != null && !sku.isBlank()) {
            this.selectedSku = sku;
        }
    }

    public boolean isSkuSelected(String sku) {
        return sku != null && sku.equals(selectedSku);
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

    public String formatPrice(Price price) {
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

    public boolean isFirstIssuanceFlow() {
        return appType == ApplicationType.FIRST_ISSUANCE;
    }

    public String labelForCitizenship(CitizenshipType type) {
        return switch (type) {
            case CITIZEN ->
                "Citizen";
            case NON_CITIZEN ->
                "Non-citizen";
            case REFUGEE ->
                "Refugee";
        };
    }

    public String labelForTier(CitizenTier tier) {
        return CatalogLabels.citizenTierLabel(tier);
    }

    public String labelForUpdate(UpdateType type) {
        return CatalogLabels.updateLabel(type);
    }

    public String describeCitizenshipOption(CitizenshipType type) {
        if (type == null) {
            return "";
        }
        return switch (type) {
            case CITIZEN ->
                "Ghanaian applicants enrolling for their first national ID.";
            case NON_CITIZEN ->
                "Foreign residents completing registration in Ghana.";
            case REFUGEE ->
                "Refugee programme participants issued by NIA.";
        };
    }

    public String describeTierOption(CitizenTier tier) {
        if (tier == null) {
            return "";
        }
        return switch (tier) {
            case STANDARD ->
                "Simple, standard service – affordable.";
            case PREMIUM ->
                "Priority processing – instant issuance.";
        };
    }

    public String describeUpdateOption(UpdateType type) {
        if (type == null) {
            return "";
        }
        return CatalogLabels.updateLabel(type);
    }

    public String describeVariant(EnrollmentSku sku) {
        if (sku == null) {
            return "";
        }
        return switch (sku.appType) {
            case FIRST_ISSUANCE ->
                switch (sku.citizenship) {
                    case CITIZEN ->
                        sku.citizenTier == CitizenTier.PREMIUM
                        ? "Premium citizen package with priority fulfilment."
                        : "Regular citizen package for first-time enrolment.";
                    case NON_CITIZEN ->
                        "First issuance for foreign residents.";
                    case REFUGEE ->
                        "First issuance for registered refugees.";
                };
            case RENEWAL ->
                sku.durationYears > 0
                ? (sku.durationYears == 1 ? "Valid for 1 year." : "Valid for " + sku.durationYears + " years.")
                : "Single renewal issuance.";
            case REPLACEMENT ->
                "Replacement PIN for lost or damaged cards.";
            case UPDATE ->
                sku.updateType == null
                ? "Update existing enrollment details."
                : CatalogLabels.updateLabel(sku.updateType) + " update.";
            case VERIFICATION ->
                "";
        };
    }

    public String subcategoryLabel(EnrollmentSku sku) {
        if (sku == null) {
            return "";
        }
        return switch (sku.appType) {
            case FIRST_ISSUANCE ->
                switch (sku.citizenship) {
                    case CITIZEN ->
                        sku.citizenTier == CitizenTier.PREMIUM
                        ? "Citizen • Premium First Issuance"
                        : "Citizen • Regular First Issuance";
                    case NON_CITIZEN ->
                        "Non-citizen • First Issuance";
                    case REFUGEE ->
                        "Refugee • First Issuance";
                };
            case RENEWAL ->
                (sku.durationYears > 0
                ? labelForCitizenship(sku.citizenship) + " • Renewal (" + sku.durationYears + (sku.durationYears == 1 ? " year)" : " years)")
                : labelForCitizenship(sku.citizenship) + " • Renewal");
            case REPLACEMENT ->
                labelForCitizenship(sku.citizenship) + " • Replacement";
            case UPDATE ->
                labelForCitizenship(sku.citizenship) + " • " + CatalogLabels.updateLabel(sku.updateType);
            case VERIFICATION ->
                "Verification";
        };
    }

    public String describeVerificationVariant(VerificationSku sku) {
        if (sku == null) {
            return "";
        }
        return switch (sku) {
            case Y1 ->
                "12 months of verification access.";
            case Y2 ->
                "24 months of verification access.";
            case Y3 ->
                "36 months of verification access.";
            case MOBILE ->
                "Bundle of mobile verification PINs for on-device checks.";
            case WEB ->
                "Bundle of web verification PINs for browser-based checks.";
        };
    }

    public PaymentMode[] getPaymentModes() {
        return PaymentMode.values();
    }

    public void selectPaymentMode(PaymentMode newMode) {
        setMode(newMode);
    }

    public boolean isPaymentModeSelected(PaymentMode modeOption) {
        return modeOption != null && modeOption == mode;
    }

    public String paymentModeTitle(PaymentMode modeOption) {
        if (modeOption == null) {
            return "";
        }
        return switch (modeOption) {
            case PAY_NOW ->
                "Pay now";
            case PAY_LATER ->
                "Pay later";
        };
    }

    public String describePaymentMode(PaymentMode modeOption) {
        if (modeOption == null) {
            return "";
        }
        return switch (modeOption) {
            case PAY_NOW ->
                "Checkout instantly and receive masked PINs immediately.";
            case PAY_LATER ->
                "Generate an invoice for bank payment and redeem later.";
        };
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

    public String addToCart() {
        boolean added;
        if (appType == ApplicationType.VERIFICATION) {
            if (!validateVerification()) {
                return null;
            }
            added = addSelectionToCart(new ProductKey(ProductFamily.VERIFICATION, selectedSku));
        } else {
            // For add to cart, only validate selections, not common fields (qty, delivery, etc.)
            // Those will be configured during checkout
            if (!finalizeValidation(validateEnrollmentSelections())) {
                return null;
            }
            added = addSelectionToCartWithDefaults(new ProductKey(ProductFamily.ENROLLMENT, selectedSku));
        }
        if (added) {
            String productName = getUnitLabel();
            String message = qty > 1
                    ? String.format("%s (×%d) added to cart.", productName, qty)
                    : String.format("%s added to cart.", productName);
            queueMessage(FacesMessage.SEVERITY_INFO, message);
            return "/index?faces-redirect=true";
        }
        return null;
    }

    private boolean validateEnrollment() {
        boolean valid = validateEnrollmentSelections();
        if (!validateCommon()) {
            valid = false;
        }
        return finalizeValidation(valid);
    }

    private boolean validateEnrollmentSelections() {
        boolean valid = true;
        if (citizenship == null) {
            addMessage(componentId("citizenship"), FacesMessage.SEVERITY_ERROR, "Select a citizenship.");
            valid = false;
        }
        // For renewals and first issuance, tier is not required
        // For replacements and updates, tier is required for citizens only
        if (appType != ApplicationType.RENEWAL && appType != ApplicationType.FIRST_ISSUANCE) {
            if (appType == ApplicationType.UPDATE || appType == ApplicationType.REPLACEMENT) {
                // For updates and replacements, only require tier for citizens
                if (citizenship == CitizenshipType.CITIZEN && citizenTier == null) {
                    addMessage(componentId("tier"), FacesMessage.SEVERITY_ERROR, "Select a citizen tier.");
                    valid = false;
                }
            } else {
                // For other types, use the standard tier requirement logic
                if (citizenship == CitizenshipType.CITIZEN && citizenTier == null) {
                    addMessage(componentId("tier"), FacesMessage.SEVERITY_ERROR, "Select a citizen tier.");
                    valid = false;
                }
            }
        }
        // Update type is no longer required for updates
        // if (appType == ApplicationType.UPDATE && updateType == null) {
        //     addMessage(componentId("updateType"), FacesMessage.SEVERITY_ERROR, "Select an update type.");
        //     valid = false;
        // }
        List<EnrollmentSku> variants = getVariants();
        if (variants.isEmpty()) {
            addMessage(componentId("variant"), FacesMessage.SEVERITY_ERROR, "No variants are available for the chosen options.");
            valid = false;
        } else if (selectedSku == null || variants.stream().noneMatch(sku -> sku.sku.equals(selectedSku))) {
            addMessage(componentId("variant"), FacesMessage.SEVERITY_ERROR, "Choose a variant.");
            valid = false;
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
        return finalizeValidation(valid);
    }

    private boolean finalizeValidation(boolean valid) {
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

    public boolean hasSelectedSku() {
        if (appType != ApplicationType.VERIFICATION && (selectedSku == null || selectedSku.isBlank())) {
            getVariants();
        }
        return selectedSku != null && !selectedSku.isBlank();
    }

    public EnrollmentSku getSelectedEnrollmentSku() {
        if (!isEnrollmentFlow() || !hasSelectedSku()) {
            return null;
        }
        return EnrollmentSku.bySku(selectedSku).orElse(null);
    }

    public String selectedSkuDurationLabel() {
        EnrollmentSku sku = getSelectedEnrollmentSku();
        if (sku == null) {
            return "";
        }
        if (sku.durationYears <= 0) {
            return "Not applicable";
        }
        return sku.durationYears == 1 ? "1 year" : sku.durationYears + " years";
    }

    public String selectedSkuActiveLabel() {
        EnrollmentSku sku = getSelectedEnrollmentSku();
        if (sku == null) {
            return "";
        }
        return sku.active ? "Available" : "Unavailable";
    }

    public String selectedSkuCurrencyCode() {
        EnrollmentSku sku = getSelectedEnrollmentSku();
        if (sku == null) {
            return "";
        }
        return sku.currency.name();
    }

    public String selectedSkuDisplayName() {
        EnrollmentSku sku = getSelectedEnrollmentSku();
        return sku == null ? "" : sku.displayName;
    }

    public String selectedSkuDescription() {
        EnrollmentSku sku = getSelectedEnrollmentSku();
        return sku == null ? "" : describeVariant(sku);
    }

    public String selectedSkuCitizenshipLabel() {
        EnrollmentSku sku = getSelectedEnrollmentSku();
        return sku == null ? "" : labelForCitizenship(sku.citizenship);
    }

    public String selectedSkuSubcategoryLabel() {
        return subcategoryLabel(getSelectedEnrollmentSku());
    }

    public String iconForCitizenship(CitizenshipType type) {
        if (type == null) {
            return "";
        }
        System.out.println("The citizen type is >>>" + type.name());
        return switch (type) {
            case CITIZEN ->
                "🇬🇭";
            case NON_CITIZEN ->
                "🌍";
            case REFUGEE ->
                "🕊️";
        };
    }

    public String tierPriceLabel(CitizenTier tier) {
        if (tier == null || appType != ApplicationType.FIRST_ISSUANCE || citizenship != CitizenshipType.CITIZEN) {
            return "";
        }
        return EnrollmentSku.filter(CitizenshipType.CITIZEN, ApplicationType.FIRST_ISSUANCE, null, tier)
                .stream()
                .findFirst()
                .map(EnrollmentSku::price)
                .map(this::formatPrice)
                .orElse("");
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
        context.addMessage(null, new FacesMessage(severity, message, ""));
        context.getExternalContext().getFlash().setKeepMessages(true);
    }

    private boolean addSelectionToCart(ProductKey key) {
        Price unitPrice = getUnitPrice();
        if (unitPrice == null) {
            addMessage(componentId("variant"), FacesMessage.SEVERITY_ERROR, "Select a package.");
            return false;
        }
        cartView.addOrUpdateLine(key,
                getUnitLabel(),
                unitPrice,
                qty,
                mode,
                deliverEmail,
                deliverSms,
                email,
                msisdn);
        return true;
    }

    private boolean addSelectionToCartWithDefaults(ProductKey key) {
        Price unitPrice = getUnitPrice();
        if (unitPrice == null) {
            addMessage(componentId("variant"), FacesMessage.SEVERITY_ERROR, "Select a package.");
            return false;
        }
        // Use default values for add-to-cart flow - user will configure during checkout
        cartView.addOrUpdateLine(key,
                getUnitLabel(),
                unitPrice,
                1, // Default quantity
                PaymentMode.PAY_NOW, // Default payment mode
                true, // Default to email delivery
                false, // Default to no SMS
                "", // Empty email - will be configured at checkout
                ""); // Empty phone - will be configured at checkout
        return true;
    }

    public boolean isReadyToAddToCart() {
        if (appType == ApplicationType.VERIFICATION) {
            return isVerificationReady();
        } else if (isFirstIssuanceFlow()) {
            return isEnrollmentReady();
        } else {
            return isEnrollmentReady();
        }
    }

    private boolean isEnrollmentReady() {
        // Check if all required selections are made
        if (citizenship == null) {
            return false;
        }
        // For renewals and first issuance, tier is not required
        // For replacements and updates, tier is required for citizens only
        if (appType != ApplicationType.RENEWAL && appType != ApplicationType.FIRST_ISSUANCE) {
            if (appType == ApplicationType.UPDATE || appType == ApplicationType.REPLACEMENT) {
                // For updates and replacements, only require tier for citizens
                if (citizenship == CitizenshipType.CITIZEN && citizenTier == null) {
                    return false;
                }
            } else {
                // For other types, use the standard tier requirement logic
                if (citizenship == CitizenshipType.CITIZEN && citizenTier == null) {
                    return false;
                }
            }
        }
        // Update type is no longer required for updates
        // if (appType == ApplicationType.UPDATE && updateType == null) {
        //     return false;
        // }
        if (selectedSku == null || getVariants().isEmpty()) {
            return false;
        }
        // Check if a valid variant is selected
        List<EnrollmentSku> variants = getVariants();
        return !variants.stream().noneMatch(sku -> sku.sku.equals(selectedSku));
    }

    private boolean isVerificationReady() {
        if (selectedSku == null || getVerificationVariants().isEmpty()) {
            return false;
        }
        // Check if a valid verification variant is selected
        List<VerificationSku> variants = getVerificationVariants();
        return variants.stream().anyMatch(sku -> sku.sku.equals(selectedSku));
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
            wizardStep = 1;
            normalizeWizardStep();
        }
    }
}
