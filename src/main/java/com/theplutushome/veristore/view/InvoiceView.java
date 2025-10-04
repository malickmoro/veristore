package com.theplutushome.veristore.view;

import com.theplutushome.veristore.domain.InvoiceRecord;
import com.theplutushome.veristore.service.OrderService;

import javax.faces.application.FacesMessage;
import javax.faces.context.ExternalContext;
import javax.faces.context.FacesContext;
import javax.faces.view.ViewScoped;
import javax.inject.Inject;
import javax.inject.Named;
import java.io.Serializable;
import java.util.Map;

@Named
@ViewScoped
public class InvoiceView implements Serializable {

    @Inject
    private OrderService orderService;

    private InvoiceRecord invoice;
    private boolean initialized;

    public void load() {
        if (initialized) {
            return;
        }
        initialized = true;
        FacesContext context = FacesContext.getCurrentInstance();
        ExternalContext externalContext = context.getExternalContext();
        Map<String, String> params = externalContext.getRequestParameterMap();
        String invoiceNumber = params.get("invoice");
        if (invoiceNumber == null) {
            Object flashValue = externalContext.getFlash().get("invoiceNumber");
            if (flashValue != null) {
                invoiceNumber = flashValue.toString();
            }
        }
        if (invoiceNumber == null || invoiceNumber.isEmpty()) {
            context.addMessage(null, new FacesMessage(FacesMessage.SEVERITY_ERROR, "Invoice not specified", null));
            return;
        }
        orderService.findInvoice(invoiceNumber).ifPresentOrElse(invoiceRecord -> {
            this.invoice = invoiceRecord;
            if (Boolean.TRUE.equals(externalContext.getFlash().get("showMessage"))) {
                context.addMessage(null, new FacesMessage(FacesMessage.SEVERITY_INFO, "Invoice created successfully", null));
            }
        }, () -> context.addMessage(null, new FacesMessage(FacesMessage.SEVERITY_ERROR, "Invoice not found", null)));
    }

    public InvoiceRecord getInvoice() {
        return invoice;
    }
}
