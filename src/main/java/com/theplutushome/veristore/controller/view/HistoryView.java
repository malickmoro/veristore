package com.theplutushome.veristore.controller.view;

import com.theplutushome.veristore.model.Contact;
import com.theplutushome.veristore.model.Price;
import com.theplutushome.veristore.service.OrderStore;
import com.theplutushome.veristore.service.PricingService;
import com.theplutushome.veristore.util.Masker;
import com.theplutushome.veristore.util.VariantDescriptions;

import jakarta.faces.application.FacesMessage;
import jakarta.faces.context.FacesContext;
import jakarta.faces.view.ViewScoped;
import jakarta.inject.Inject;
import jakarta.inject.Named;
import java.io.Serial;
import java.io.Serializable;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.util.Collections;
import java.util.List;
import java.util.stream.Collectors;

@Named
@ViewScoped
public class HistoryView implements Serializable {

    @Serial
    private static final long serialVersionUID = 1L;

    private String email;
    private String msisdn;
    private List<OrderStore.Order> results = Collections.emptyList();
    private boolean searched;

    @Inject
    private OrderStore orderStore;

    @Inject
    private PricingService pricingService;

    public void search() {
        FacesContext context = FacesContext.getCurrentInstance();
        searched = true;
        String normalizedEmail = normalize(email);
        String normalizedMsisdn = normalize(msisdn);
        if (normalizedEmail.isEmpty() && normalizedMsisdn.isEmpty()) {
            context.addMessage(null, new FacesMessage(FacesMessage.SEVERITY_ERROR, "Enter an email or phone number", null));
            results = Collections.emptyList();
            return;
        }
        Contact query = new Contact(normalizedEmail, normalizedMsisdn);
        results = orderStore.findOrdersByContact(query);
        if (results.isEmpty()) {
            context.addMessage(null, new FacesMessage(FacesMessage.SEVERITY_INFO, "No matching orders found.", null));
        } else {
            context.addMessage(null, new FacesMessage(FacesMessage.SEVERITY_INFO,
                results.size() == 1 ? "Found 1 matching order." : "Found " + results.size() + " matching orders.", null));
        }
    }

    public String getEmail() {
        return email;
    }

    public void setEmail(String email) {
        this.email = email;
    }

    public String getMsisdn() {
        return msisdn;
    }

    public void setMsisdn(String msisdn) {
        this.msisdn = msisdn;
    }

    public List<OrderStore.Order> getResults() {
        return results;
    }

    public boolean isSearched() {
        return searched;
    }

    public String formatTotal(OrderStore.Order order) {
        Price price = new Price(order.getCurrency(), order.getTotalMinor());
        return pricingService.format(price);
    }

    public List<String> maskedCodes(OrderStore.Order order) {
        return order.getCodes().stream().map(Masker::mask).collect(Collectors.toList());
    }

    public String describeService(OrderStore.Order order) {
        if (order.getLines().isEmpty()) {
            return "";
        }
        return order.getLines().stream()
                .map(line -> VariantDescriptions.describe(line.getKey().family(), line.getKey().sku())
                        + " x" + line.getQuantity())
                .collect(Collectors.joining(", "));
    }

    public LocalDateTime getCreatedLocalDateTime(OrderStore.Order order) {
        return order.getCreated().atZone(ZoneId.systemDefault()).toLocalDateTime();
    }

    private String normalize(String value) {
        return value == null ? "" : value.trim();
    }
}
