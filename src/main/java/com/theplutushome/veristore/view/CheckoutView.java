package com.theplutushome.veristore.view;

import java.io.Serial;
import java.io.Serializable;
import java.net.URI;
import java.util.ArrayList;
import java.util.List;

import com.theplutushome.veristore.dto.CartLineDTO;
import com.theplutushome.veristore.payment.PaymentService;

import jakarta.annotation.PostConstruct;
import jakarta.faces.application.FacesMessage;
import jakarta.faces.context.FacesContext;
import jakarta.faces.view.ViewScoped;
import jakarta.inject.Inject;
import jakarta.inject.Named;

@Named
@ViewScoped
public class CheckoutView implements Serializable {

    @Serial
    private static final long serialVersionUID = 1L;

    private String email;
    private String phone;
    private boolean deliverByEmail = true;
    private boolean deliverBySms;
    private List<CartLineDTO> lines = new ArrayList<>();
    private long subtotalMinor;
    private long feesMinor;
    private long totalMinor;
    private String currency;
    private URI paymentRedirect;
    private boolean showStatusPanel;
    private String paymentStatus;

    @Inject
    PaymentService paymentService;

    @Inject
    CartView cartView;

    @Inject
    GlobalView globalView;

    @PostConstruct
    public void init() {
        snapshotCart();
    }

    public void pay() {
        snapshotCart();
        if (lines.isEmpty()) {
            addMessage(FacesMessage.SEVERITY_WARN, "Your cart is empty", "Add items before paying.");
            return;
        }
        paymentRedirect = paymentService.createInvoiceAndRedirect(totalMinor, currency);
        paymentStatus = "Payment initiated. Follow the redirect to complete checkout.";
        showStatusPanel = true;
    }

    public void handlePaymentReturn(String status) {
        paymentStatus = status;
        showStatusPanel = true;
        if (status != null && status.toLowerCase().contains("success")) {
            addMessage(FacesMessage.SEVERITY_INFO, "Payment confirmed", null);
            cartView.empty();
            snapshotCart();
        }
    }

    public void cancel() {
        cartView.empty();
        snapshotCart();
        paymentStatus = null;
        showStatusPanel = false;
    }

    public List<CartLineDTO> getLines() {
        return lines;
    }

    public String getEmail() {
        return email;
    }

    public void setEmail(String email) {
        this.email = email;
    }

    public String getPhone() {
        return phone;
    }

    public void setPhone(String phone) {
        this.phone = phone;
    }

    public boolean isDeliverByEmail() {
        return deliverByEmail;
    }

    public void setDeliverByEmail(boolean deliverByEmail) {
        this.deliverByEmail = deliverByEmail;
    }

    public boolean isDeliverBySms() {
        return deliverBySms;
    }

    public void setDeliverBySms(boolean deliverBySms) {
        this.deliverBySms = deliverBySms;
    }

    public long getTotalMinor() {
        return totalMinor;
    }

    public String getCurrency() {
        return currency;
    }

    public URI getPaymentRedirect() {
        return paymentRedirect;
    }

    public boolean isShowStatusPanel() {
        return showStatusPanel;
    }

    public String getPaymentStatus() {
        return paymentStatus;
    }

    public String getSubtotalFormatted() {
        return globalView.formatMoney(subtotalMinor, currency);
    }

    public String getFeesFormatted() {
        return globalView.formatMoney(feesMinor, currency);
    }

    public String getTotalFormatted() {
        return globalView.formatMoney(totalMinor, currency);
    }

    private void snapshotCart() {
        lines = cartView.getLines().stream().map(CartLineDTO::new).toList();
        subtotalMinor = lines.stream().mapToLong(CartLineDTO::getTotalMinor).sum();
        feesMinor = cartView.fees();
        totalMinor = subtotalMinor + feesMinor;
        currency = cartView.getCurrencyCode();
    }

    private void addMessage(FacesMessage.Severity severity, String summary, String detail) {
        FacesContext context = FacesContext.getCurrentInstance();
        if (context != null) {
            context.addMessage(null, new FacesMessage(severity, summary, detail));
        }
    }
}
