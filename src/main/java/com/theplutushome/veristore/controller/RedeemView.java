package com.theplutushome.veristore.controller;

import com.theplutushome.veristore.service.payment.PaymentService;

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

@Named
@ViewScoped
public class RedeemView implements Serializable {

    @Serial
    private static final long serialVersionUID = 1L;

    @Inject
    private PaymentService paymentService;

    private String invoiceNo;

    public String redeem() {
        FacesContext context = FacesContext.getCurrentInstance();
        if (invoiceNo == null || invoiceNo.trim().isEmpty()) {
            context.addMessage(null, new FacesMessage(FacesMessage.SEVERITY_ERROR, "Invoice number is required", null));
            return null;
        }
        String normalized = invoiceNo.trim();
        boolean redeemed = paymentService.redeemInvoice(normalized);
        if (!redeemed) {
            context.addMessage(null, new FacesMessage(FacesMessage.SEVERITY_ERROR, "Unable to redeem invoice", null));
            return null;
        }
        ExternalContext externalContext = context.getExternalContext();
        context.addMessage(null, new FacesMessage(FacesMessage.SEVERITY_INFO, "Invoice " + normalized + " redeemed successfully.", null));
        externalContext.getFlash().setKeepMessages(true);
        String encoded = URLEncoder.encode(normalized, StandardCharsets.UTF_8);
        try {
            externalContext.redirect(externalContext.getRequestContextPath() + "/success.xhtml?invoice=" + encoded);
            context.responseComplete();
        } catch (IOException ex) {
            context.addMessage(null, new FacesMessage(FacesMessage.SEVERITY_ERROR, ex.getMessage(), null));
        }
        return null;
    }

    public String getInvoiceNo() {
        return invoiceNo;
    }

    public void setInvoiceNo(String invoiceNo) {
        this.invoiceNo = invoiceNo;
    }
}
