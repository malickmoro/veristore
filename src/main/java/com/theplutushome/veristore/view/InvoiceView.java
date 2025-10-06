package com.theplutushome.veristore.view;

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
import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.stream.Collectors;

@Named
@ViewScoped
public class InvoiceView implements Serializable {

    @Serial
    private static final long serialVersionUID = 1L;

    @Inject
    private OrderStore orderStore;

    @Inject
    private PricingService pricingService;

    private String invoiceNo;
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
        invoiceNo = Optional.ofNullable(params.get("no"))
            .map(String::trim)
            .filter(value -> !value.isEmpty())
            .orElse(null);
        if (invoiceNo == null) {
            context.addMessage(null, new FacesMessage(FacesMessage.SEVERITY_ERROR, "Invoice number is required", null));
            return;
        }
        invoice = orderStore.findInvoice(invoiceNo).orElse(null);
        if (invoice == null) {
            context.addMessage(null, new FacesMessage(FacesMessage.SEVERITY_ERROR, "Invoice not found", null));
        }
    }

    public boolean isFound() {
        return invoice != null;
    }

    public OrderStore.Invoice getInvoice() {
        return invoice;
    }

    public String getInvoiceNo() {
        return invoiceNo;
    }

    public String getServiceLabel() {
        if (invoice == null) {
            return "";
        }
        return VariantDescriptions.describe(invoice.getKey().family(), invoice.getKey().sku());
    }

    public String getUnitPrice() {
        if (invoice == null) {
            return "";
        }
        Price unit = pricingService.get(invoice.getKey());
        return pricingService.format(unit);
    }

    public String getTotal() {
        if (invoice == null) {
            return "";
        }
        return pricingService.format(new Price(invoice.getCurrency(), invoice.getTotalMinor()));
    }

    public List<String> getMaskedCodes() {
        if (invoice == null || invoice.getCodesIfDelivered().isEmpty()) {
            return List.of();
        }
        return invoice.getCodesIfDelivered().stream().map(Masker::mask).collect(Collectors.toList());
    }

    public LocalDateTime getCreatedLocalDateTime() {
        if (invoice == null) {
            return null;
        }
        return invoice.getCreated().atZone(ZoneId.systemDefault()).toLocalDateTime();
    }

    public String getSimulatePaymentUrl() {
        if (invoice == null) {
            return "";
        }
        String encoded = URLEncoder.encode(invoice.getInvoiceNo(), StandardCharsets.UTF_8);
        ExternalContext externalContext = FacesContext.getCurrentInstance().getExternalContext();
        return externalContext.getRequestContextPath() + "/api/mock/hubtel/callback?invoice=" + encoded + "&paid=true";
    }
}
