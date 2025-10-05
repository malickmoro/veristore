package com.theplutushome.veristore.view;

import com.theplutushome.veristore.catalog.ProductFamily;
import com.theplutushome.veristore.catalog.ProductKey;
import com.theplutushome.veristore.catalog.VerificationSku;
import com.theplutushome.veristore.domain.PaymentMode;
import com.theplutushome.veristore.domain.Price;
import com.theplutushome.veristore.service.PricingService;

import jakarta.annotation.PostConstruct;
import jakarta.faces.application.FacesMessage;
import jakarta.faces.context.FacesContext;
import jakarta.faces.view.ViewScoped;
import jakarta.inject.Inject;
import jakarta.inject.Named;

import java.io.Serial;
import java.io.Serializable;
import java.util.List;
import java.util.Locale;
import java.util.Optional;

@Named("pinPurchaseView")
@ViewScoped
public class VerificationPinPurchaseView implements Serializable {

    @Serial
    private static final long serialVersionUID = 1L;

    public enum Channel {
        MOBILE,
        WEB
    }

    @Inject
    private PricingService pricingService;

    @Inject
    private CartView cartView;

    private Channel channel;
    private String merchantCode;
    private MerchantProfile merchant;
    private boolean lookupAttempted;
    private int qty;

    @PostConstruct
    public void init() {
        reset();
    }

    public void prepareMobilePins() {
        channel = Channel.MOBILE;
        reset();
    }

    public void prepareWebPins() {
        channel = Channel.WEB;
        reset();
    }

    private void reset() {
        merchantCode = "";
        merchant = null;
        lookupAttempted = false;
        qty = 1;
    }

    public Channel getChannel() {
        return channel;
    }

    public String getMerchantCode() {
        return merchantCode;
    }

    public void setMerchantCode(String merchantCode) {
        this.merchantCode = merchantCode == null ? "" : merchantCode.trim();
    }

    public int getQty() {
        return qty;
    }

    public void setQty(int qty) {
        if (qty <= 0) {
            this.qty = 1;
        } else {
            this.qty = qty;
        }
    }

    public boolean isLookupAttempted() {
        return lookupAttempted;
    }

    public boolean isMerchantFound() {
        return merchant != null;
    }

    public String getMerchantName() {
        return merchant == null ? "" : merchant.name();
    }

    public String getMerchantLocation() {
        return merchant == null ? "" : merchant.location();
    }

    public void searchMerchant() {
        lookupAttempted = true;
        merchant = null;
        // JSF required validation will handle empty codes
        String normalized = merchantCode.toUpperCase(Locale.ROOT);
        merchantCode = normalized;
        Optional<MerchantProfile> match = MerchantDirectory.lookup(normalized);
        if (match.isEmpty()) {
            // Don't add global message - let the search results panel handle the display
            return;
        }
        merchant = match.get();
        // Don't add global messages - let the search results panel handle the display
    }

    public boolean isEligible() {
        if (merchant == null) {
            return false;
        }
        return switch (activeChannel()) {
            case MOBILE -> merchant.mobileEligible();
            case WEB -> merchant.webEligible();
        };
    }

    public String getEligibilityMessage() {
        if (merchant == null) {
            return "";
        }
        if (isEligible()) {
            return merchant.name() + " can purchase " + productLabel().toLowerCase(Locale.ROOT) + ".";
        }
        return merchant.name() + " is not eligible to purchase " + productLabel().toLowerCase(Locale.ROOT) + ".";
    }

    private Channel activeChannel() {
        return channel == null ? Channel.MOBILE : channel;
    }

    public String productLabel() {
        return switch (activeChannel()) {
            case MOBILE -> "Mobile verification PINs";
            case WEB -> "Web verification PINs";
        };
    }

    public String getStepOneTitle() {
        return "Step 1 · Find merchant";
    }

    public String getStepTwoTitle() {
        return "Step 2 · Choose quantity";
    }

    public String getStepOneDescription() {
        return "Search using the merchant code provided during onboarding.";
    }

    public String getStepTwoDescription() {
        return "Enter how many " + productLabel().toLowerCase(Locale.ROOT) + " the merchant wants to buy. Pricing is per PIN.";
    }

    public Price getUnitPrice() {
        VerificationSku sku = channelSku();
        return sku.price();
    }

    public String getUnitPriceFormatted() {
        return pricingService.format(getUnitPrice());
    }

    public String getTotalFormatted() {
        Price price = getUnitPrice();
        long totalMinor = Math.multiplyExact(price.amountMinor(), qty);
        return pricingService.format(new Price(price.currency(), totalMinor));
    }

    public String getSku() {
        return channelSku().sku;
    }

    public String getSkuDisplayName() {
        return channelSku().displayName;
    }

    public String addToCart() {
        if (!isEligible()) {
            addMessage(null, FacesMessage.SEVERITY_ERROR, "Select an eligible merchant before adding to cart.");
            return null;
        }
        if (qty <= 0) {
            addMessage(componentId("quantity"), FacesMessage.SEVERITY_ERROR, "Quantity must be at least 1.");
            return null;
        }
        Price unitPrice = getUnitPrice();
        cartView.addOrUpdateLine(new ProductKey(ProductFamily.VERIFICATION, getSku()),
            getSkuDisplayName(),
            unitPrice,
            qty,
            PaymentMode.PAY_NOW,
            true,
            false,
            "",
            "");
        String message = qty > 1
            ? String.format("%s (×%d) added to cart.", getSkuDisplayName(), qty)
            : String.format("%s added to cart.", getSkuDisplayName());
        queueMessage(FacesMessage.SEVERITY_INFO, message);
        return "/index?faces-redirect=true";
    }

    private VerificationSku channelSku() {
        return switch (activeChannel()) {
            case MOBILE -> VerificationSku.MOBILE;
            case WEB -> VerificationSku.WEB;
        };
    }

    private String componentId(String id) {
        return "pinForm:" + id;
    }

    private void addMessage(String clientId, FacesMessage.Severity severity, String summary) {
        FacesContext context = FacesContext.getCurrentInstance();
        context.addMessage(clientId, new FacesMessage(severity, summary, ""));
    }

    private void queueMessage(FacesMessage.Severity severity, String summary) {
        FacesContext context = FacesContext.getCurrentInstance();
        context.addMessage(null, new FacesMessage(severity, summary, ""));
        context.getExternalContext().getFlash().setKeepMessages(true);
    }

    private record MerchantProfile(String code, String name, String location, boolean mobileEligible, boolean webEligible) {
    }

    private static final class MerchantDirectory {

        private static final List<MerchantProfile> MERCHANTS = List.of(
            new MerchantProfile("MERCH001", "Accra Digital Hub", "Accra", true, true),
            new MerchantProfile("MERCH145", "Kumasi Retail Group", "Kumasi", true, false),
            new MerchantProfile("MERCH008", "Takoradi Telco", "Takoradi", false, true),
            new MerchantProfile("MERCH377", "Tamale Market", "Tamale", true, true)
        );

        private MerchantDirectory() {
        }

        static Optional<MerchantProfile> lookup(String code) {
            return MERCHANTS.stream()
                .filter(profile -> profile.code().equalsIgnoreCase(code))
                .findFirst();
        }
    }
}
