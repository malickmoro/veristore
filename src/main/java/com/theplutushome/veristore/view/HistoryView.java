package com.theplutushome.veristore.view;

import com.theplutushome.veristore.domain.OrderRecord;
import com.theplutushome.veristore.service.OrderService;

import javax.faces.application.FacesMessage;
import javax.faces.context.FacesContext;
import javax.faces.view.ViewScoped;
import javax.inject.Inject;
import javax.inject.Named;
import java.io.Serializable;
import java.util.Collections;
import java.util.List;

@Named
@ViewScoped
public class HistoryView implements Serializable {

    private String email;
    private String phone;
    private List<OrderRecord> results = Collections.emptyList();
    private boolean searched;

    @Inject
    private OrderService orderService;

    public void search() {
        FacesContext context = FacesContext.getCurrentInstance();
        searched = true;
        if ((email == null || email.trim().isEmpty()) && (phone == null || phone.trim().isEmpty())) {
            context.addMessage(null, new FacesMessage(FacesMessage.SEVERITY_ERROR, "Enter email or phone number", null));
            results = Collections.emptyList();
            return;
        }
        results = orderService.findHistory(email, phone);
        if (results.isEmpty()) {
            context.addMessage(null, new FacesMessage(FacesMessage.SEVERITY_INFO, "No orders found", null));
        }
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

    public List<OrderRecord> getResults() {
        return results;
    }

    public boolean isSearched() {
        return searched;
    }
}
