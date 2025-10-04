package com.theplutushome.veristore.view;

import com.theplutushome.veristore.domain.CustomerProfile;
import com.theplutushome.veristore.domain.OrderRecord;
import com.theplutushome.veristore.domain.ServiceDefinition;
import com.theplutushome.veristore.domain.ServiceKey;
import com.theplutushome.veristore.domain.VerificationDuration;
import com.theplutushome.veristore.service.OrderService;
import com.theplutushome.veristore.service.ServiceCatalog;

import javax.faces.application.FacesMessage;
import javax.faces.context.ExternalContext;
import javax.faces.context.FacesContext;
import javax.faces.view.ViewScoped;
import javax.inject.Inject;
import javax.inject.Named;
import java.io.IOException;
import java.io.Serializable;
import java.math.BigDecimal;
import java.util.EnumSet;
import java.util.List;
import java.util.Map;

@Named
@ViewScoped
public class BuyVerificationView implements Serializable {

    private String fullName;
    private String email;
    private String phone;
    private VerificationDuration selectedDuration = VerificationDuration.Y1;
    private int quantity = 1;
    private PaymentOption paymentOption = PaymentOption.PAY_NOW;

    @Inject
    private OrderService orderService;

    @Inject
    private ServiceCatalog serviceCatalog;

    public List<VerificationDuration> getDurations() {
        return List.copyOf(EnumSet.allOf(VerificationDuration.class));
    }

    public PaymentOption[] getPaymentOptions() {
        return PaymentOption.values();
    }

    public String describe(PaymentOption option) {
        if (option == PaymentOption.PAY_LATER) {
            return "Pay later – receive an invoice to pay at the bank";
        }
        return "Pay now – instant confirmation and delivery";
    }

    public ServiceDefinition getSelectedDefinition() {
        return serviceCatalog.getDefinition(ServiceKey.verification(selectedDuration));
    }

    public BigDecimal getEstimatedTotal() {
        BigDecimal unitPrice = getSelectedDefinition().getUnitPrice();
        int safeQuantity = Math.max(0, quantity);
        return unitPrice.multiply(BigDecimal.valueOf(safeQuantity));
    }

    public void submit() throws IOException {
        if (!validateInputs()) {
            return;
        }
        CustomerProfile customer = new CustomerProfile(fullName.trim(), email.trim(), phone.trim());
        ServiceKey key = ServiceKey.verification(selectedDuration);
        if (paymentOption == PaymentOption.PAY_NOW) {
            OrderRecord order = orderService.completePurchase(key, customer, quantity);
            redirectWithOrder(order);
        } else {
            redirectWithInvoice(orderService.createInvoice(key, customer, quantity).getInvoiceNumber());
        }
    }

    private boolean validateInputs() {
        boolean valid = true;
        FacesContext context = FacesContext.getCurrentInstance();
        if (isBlank(fullName)) {
            context.addMessage(null, new FacesMessage(FacesMessage.SEVERITY_ERROR, "Full name is required", null));
            valid = false;
        }
        if (isBlank(email)) {
            context.addMessage(null, new FacesMessage(FacesMessage.SEVERITY_ERROR, "Email is required", null));
            valid = false;
        }
        if (isBlank(phone)) {
            context.addMessage(null, new FacesMessage(FacesMessage.SEVERITY_ERROR, "Phone number is required", null));
            valid = false;
        }
        if (quantity <= 0) {
            context.addMessage(null, new FacesMessage(FacesMessage.SEVERITY_ERROR, "Quantity must be at least 1", null));
            valid = false;
        }
        return valid;
    }

    private void redirectWithOrder(OrderRecord order) throws IOException {
        ExternalContext externalContext = FacesContext.getCurrentInstance().getExternalContext();
        Map<String, Object> flash = externalContext.getFlash();
        flash.put("orderId", order.getOrderId());
        flash.put("showMessage", Boolean.TRUE);
        externalContext.redirect(externalContext.getRequestContextPath() + "/success.xhtml?order=" + order.getOrderId());
        FacesContext.getCurrentInstance().responseComplete();
    }

    private void redirectWithInvoice(String invoiceNumber) throws IOException {
        ExternalContext externalContext = FacesContext.getCurrentInstance().getExternalContext();
        Map<String, Object> flash = externalContext.getFlash();
        flash.put("invoiceNumber", invoiceNumber);
        flash.put("showMessage", Boolean.TRUE);
        externalContext.redirect(externalContext.getRequestContextPath() + "/invoice.xhtml?invoice=" + invoiceNumber);
        FacesContext.getCurrentInstance().responseComplete();
    }

    private boolean isBlank(String value) {
        return value == null || value.trim().isEmpty();
    }

    public String getFullName() {
        return fullName;
    }

    public void setFullName(String fullName) {
        this.fullName = fullName;
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

    public VerificationDuration getSelectedDuration() {
        return selectedDuration;
    }

    public void setSelectedDuration(VerificationDuration selectedDuration) {
        this.selectedDuration = selectedDuration;
    }

    public int getQuantity() {
        return quantity;
    }

    public void setQuantity(int quantity) {
        this.quantity = quantity;
    }

    public PaymentOption getPaymentOption() {
        return paymentOption;
    }

    public void setPaymentOption(PaymentOption paymentOption) {
        this.paymentOption = paymentOption;
    }
}
