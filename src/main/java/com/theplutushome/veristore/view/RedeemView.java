package com.theplutushome.veristore.view;

import com.theplutushome.veristore.domain.InvoiceRecord;
import com.theplutushome.veristore.domain.InvoiceStatus;
import com.theplutushome.veristore.domain.OrderRecord;
import com.theplutushome.veristore.service.OrderService;

import javax.faces.application.FacesMessage;
import javax.faces.context.FacesContext;
import javax.faces.view.ViewScoped;
import javax.inject.Inject;
import javax.inject.Named;
import java.io.Serializable;
import java.util.Optional;

@Named
@ViewScoped
public class RedeemView implements Serializable {

    private String invoiceNumber;
    private OrderRecord redeemedOrder;
    private boolean alreadyRedeemed;

    @Inject
    private OrderService orderService;

    public void redeem() {
        FacesContext context = FacesContext.getCurrentInstance();
        redeemedOrder = null;
        alreadyRedeemed = false;
        if (invoiceNumber == null || invoiceNumber.trim().isEmpty()) {
            context.addMessage(null, new FacesMessage(FacesMessage.SEVERITY_ERROR, "Invoice number is required", null));
            return;
        }
        String trimmedNumber = invoiceNumber.trim();
        Optional<InvoiceRecord> invoiceOpt = orderService.findInvoice(trimmedNumber);
        if (!invoiceOpt.isPresent()) {
            context.addMessage(null, new FacesMessage(FacesMessage.SEVERITY_ERROR, "Invoice not found", null));
            return;
        }
        InvoiceRecord invoice = invoiceOpt.get();
        if (invoice.getStatus() == InvoiceStatus.PAID) {
            alreadyRedeemed = true;
            redeemedOrder = orderService.findOrder(invoice.getOrderId()).orElse(null);
            context.addMessage(null, new FacesMessage(FacesMessage.SEVERITY_INFO, "Invoice already marked as paid", null));
            return;
        }
        Optional<OrderRecord> order = orderService.redeemInvoice(trimmedNumber);
        if (order.isPresent()) {
            redeemedOrder = order.get();
            context.addMessage(null, new FacesMessage(FacesMessage.SEVERITY_INFO, "Payment verified and PINs delivered", null));
        } else {
            context.addMessage(null, new FacesMessage(FacesMessage.SEVERITY_ERROR, "Unable to redeem invoice", null));
        }
    }

    public String getInvoiceNumber() {
        return invoiceNumber;
    }

    public void setInvoiceNumber(String invoiceNumber) {
        this.invoiceNumber = invoiceNumber;
    }

    public OrderRecord getRedeemedOrder() {
        return redeemedOrder;
    }

    public boolean isAlreadyRedeemed() {
        return alreadyRedeemed;
    }
}
