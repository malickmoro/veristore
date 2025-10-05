package com.theplutushome.veristore.view;

import com.theplutushome.veristore.domain.InvoiceStatus;
import com.theplutushome.veristore.domain.Price;
import com.theplutushome.veristore.service.OrderStore;
import com.theplutushome.veristore.service.PricingService;
import com.theplutushome.veristore.util.Masker;
import com.theplutushome.veristore.util.VariantDescriptions;

import jakarta.faces.application.FacesMessage;
import jakarta.faces.context.ExternalContext;
import jakarta.faces.context.FacesContext;
import jakarta.faces.view.ViewScoped;
import jakarta.inject.Inject;
import jakarta.inject.Named;
import java.io.Serial;
import java.io.Serializable;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.stream.Collectors;

@Named
@ViewScoped
public class SuccessView implements Serializable {

    @Serial
    private static final long serialVersionUID = 1L;

    @Inject
    private OrderStore orderStore;

    @Inject
    private PricingService pricingService;

    private OrderStore.Order order;
    private OrderStore.Invoice invoice;
    private boolean loaded;

    public void load() {
        if (loaded) {
            return;
        }
        loaded = true;
        FacesContext context = FacesContext.getCurrentInstance();
        ExternalContext externalContext = context.getExternalContext();
        Map<String, String> params = externalContext.getRequestParameterMap();
        String orderId = Optional.ofNullable(params.get("orderId")).filter(id -> !id.isBlank()).orElse(null);
        String invoiceNo = Optional.ofNullable(params.get("invoice")).filter(id -> !id.isBlank()).orElse(null);
        if (orderId != null) {
            order = orderStore.findOrder(orderId).orElse(null);
            if (order == null) {
                context.addMessage(null, new FacesMessage(FacesMessage.SEVERITY_ERROR, "Order not found", null));
            }
            if (order != null && context.getMessageList().isEmpty()) {
                context.addMessage(null, new FacesMessage(FacesMessage.SEVERITY_INFO, "Order " + order.getId() + " completed successfully.", null));
            }
            return;
        }
        if (invoiceNo != null) {
            invoice = orderStore.findInvoice(invoiceNo).orElse(null);
            if (invoice == null) {
                context.addMessage(null, new FacesMessage(FacesMessage.SEVERITY_ERROR, "Invoice not found", null));
            } else if (invoice.getStatus() != InvoiceStatus.PAID) {
                context.addMessage(null, new FacesMessage(FacesMessage.SEVERITY_WARN, "Invoice pending payment", null));
            } else if (context.getMessageList().isEmpty()) {
                context.addMessage(null, new FacesMessage(FacesMessage.SEVERITY_INFO, "Invoice " + invoice.getInvoiceNo() + " paid successfully.", null));
            }
            return;
        }
        context.addMessage(null, new FacesMessage(FacesMessage.SEVERITY_ERROR, "Reference not specified", null));
    }

    public boolean hasOrder() {
        return order != null;
    }

    public boolean hasInvoice() {
        return invoice != null;
    }

    public String getReference() {
        if (order != null) {
            return order.getId();
        }
        if (invoice != null) {
            return invoice.getInvoiceNo();
        }
        return "";
    }

    public int getQuantity() {
        if (order != null) {
            return order.getQty();
        }
        if (invoice != null) {
            return invoice.getQty();
        }
        return 0;
    }

    public String getTotal() {
        if (order != null) {
            return pricingService.format(new Price(order.getCurrency(), order.getTotalMinor()));
        }
        if (invoice != null) {
            return pricingService.format(new Price(invoice.getCurrency(), invoice.getTotalMinor()));
        }
        return "";
    }

    public String getServiceLabel() {
        if (order != null) {
            return VariantDescriptions.describe(order.getKey().family(), order.getKey().sku());
        }
        if (invoice != null) {
            return VariantDescriptions.describe(invoice.getKey().family(), invoice.getKey().sku());
        }
        return "";
    }

    public List<String> getMaskedCodes() {
        if (order != null) {
            return order.getCodes().stream().map(Masker::mask).collect(Collectors.toList());
        }
        if (invoice != null && !invoice.getCodesIfDelivered().isEmpty()) {
            return invoice.getCodesIfDelivered().stream().map(Masker::mask).collect(Collectors.toList());
        }
        return Collections.emptyList();
    }
}
