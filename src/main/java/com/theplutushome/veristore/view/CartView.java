package com.theplutushome.veristore.view;

import com.theplutushome.veristore.catalog.EnrollmentSku;
import com.theplutushome.veristore.catalog.VerificationSku;
import com.theplutushome.veristore.domain.Contact;
import com.theplutushome.veristore.domain.DeliveryPrefs;
import com.theplutushome.veristore.domain.PaymentMode;
import com.theplutushome.veristore.domain.Price;
import com.theplutushome.veristore.payment.PaymentService;
import com.theplutushome.veristore.catalog.ProductFamily;
import com.theplutushome.veristore.catalog.ProductKey;
import com.theplutushome.veristore.service.PricingService;

import jakarta.annotation.PostConstruct;
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
import java.util.Objects;

@Named
@SessionScoped
public class CartView implements Serializable {

    @Serial
    private static final long serialVersionUID = 1L;

    private CartItem currentItem;

    @Inject
    private PaymentService paymentService;

    @Inject
    private PricingService pricingService;

    @PostConstruct
    public void init() {
        currentItem = null;
    }

    public void addEnrollmentItem(EnrollmentSku sku,
                                  int quantity,
                                  PaymentMode mode,
                                  Contact contact,
                                  DeliveryPrefs delivery,
                                  String description) {
        Objects.requireNonNull(sku, "sku");
        currentItem = new CartItem(
            new ProductKey(ProductFamily.ENROLLMENT, sku.sku),
            sku.displayName,
            description,
            sku.price(),
            quantity,
            mode,
            contact,
            delivery
        );
        queueInfo("Added " + sku.displayName + " to cart.");
    }

    public void addVerificationItem(VerificationSku sku,
                                    int quantity,
                                    PaymentMode mode,
                                    Contact contact,
                                    DeliveryPrefs delivery,
                                    String description) {
        Objects.requireNonNull(sku, "sku");
        currentItem = new CartItem(
            new ProductKey(ProductFamily.VERIFICATION, sku.sku),
            sku.displayName,
            description,
            sku.price(),
            quantity,
            mode,
            contact,
            delivery
        );
        queueInfo("Added " + sku.displayName + " to cart.");
    }

    public boolean hasItem() {
        return currentItem != null;
    }

    public int getItemCount() {
        return hasItem() ? 1 : 0;
    }

    public CartItem getCurrentItem() {
        return currentItem;
    }

    public String getUnitPriceFormatted() {
        if (currentItem == null) {
            return "";
        }
        return pricingService.format(currentItem.unitPrice());
    }

    public String getTotalFormatted() {
        if (currentItem == null) {
            return "";
        }
        long totalMinor = Math.multiplyExact(currentItem.unitPrice().amountMinor(), currentItem.quantity());
        Price total = new Price(currentItem.unitPrice().currency(), totalMinor);
        return pricingService.format(total);
    }

    public String removeItem() {
        currentItem = null;
        queueInfo("Cart cleared.");
        return null;
    }

    public String checkout() {
        if (currentItem == null) {
            queueError("Add a package before checking out.");
            return null;
        }
        try {
            String target;
            if (currentItem.mode() == PaymentMode.PAY_NOW) {
                String orderId = paymentService.payNow(
                    currentItem.key(),
                    currentItem.quantity(),
                    currentItem.contact(),
                    currentItem.delivery()
                );
                queueInfo("Order " + orderId + " completed successfully.");
                target = redirectTo("success", "orderId", orderId);
            } else {
                String invoiceNo = paymentService.payLater(
                    currentItem.key(),
                    currentItem.quantity(),
                    currentItem.contact(),
                    currentItem.delivery()
                );
                queueInfo("Invoice " + invoiceNo + " generated.");
                target = redirectTo("invoice", "no", invoiceNo);
            }
            currentItem = null;
            return target;
        } catch (Exception ex) {
            queueError(ex.getMessage());
            return null;
        }
    }

    private void queueInfo(String message) {
        FacesContext context = FacesContext.getCurrentInstance();
        context.addMessage(null, new FacesMessage(FacesMessage.SEVERITY_INFO, message, null));
        context.getExternalContext().getFlash().setKeepMessages(true);
    }

    private void queueError(String message) {
        FacesContext context = FacesContext.getCurrentInstance();
        context.addMessage(null, new FacesMessage(FacesMessage.SEVERITY_ERROR, message, null));
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

    public record CartItem(ProductKey key,
                           String displayName,
                           String description,
                           Price unitPrice,
                           int quantity,
                           PaymentMode mode,
                           Contact contact,
                           DeliveryPrefs delivery) implements Serializable {
        @Serial
        private static final long serialVersionUID = 1L;
    }
}

