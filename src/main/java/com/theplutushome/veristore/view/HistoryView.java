package com.theplutushome.veristore.view;

import com.theplutushome.veristore.domain.Contact;
import com.theplutushome.veristore.domain.Price;
import com.theplutushome.veristore.service.OrderStore;
import com.theplutushome.veristore.service.PricingService;
import com.theplutushome.veristore.util.Masker;
import com.theplutushome.veristore.util.VariantDescriptions;

import javax.faces.application.FacesMessage;
import javax.faces.context.FacesContext;
import javax.faces.view.ViewScoped;
import javax.inject.Inject;
import javax.inject.Named;
import java.io.Serial;
import java.io.Serializable;
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
        return VariantDescriptions.describe(order.getKey().category(), order.getKey().variant());
    }

    private String normalize(String value) {
        return value == null ? "" : value.trim();
    }
}
