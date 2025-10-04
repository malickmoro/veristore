package com.theplutushome.veristore.view;

import com.theplutushome.veristore.domain.OrderRecord;
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
public class SuccessView implements Serializable {

    @Inject
    private OrderService orderService;

    private OrderRecord order;
    private boolean initialized;

    public void load() {
        if (initialized) {
            return;
        }
        initialized = true;
        FacesContext context = FacesContext.getCurrentInstance();
        ExternalContext externalContext = context.getExternalContext();
        Map<String, String> params = externalContext.getRequestParameterMap();
        String orderId = params.get("order");
        if (orderId == null) {
            Object flashValue = externalContext.getFlash().get("orderId");
            if (flashValue != null) {
                orderId = flashValue.toString();
            }
        }
        if (orderId == null || orderId.isEmpty()) {
            context.addMessage(null, new FacesMessage(FacesMessage.SEVERITY_ERROR, "Order not specified", null));
            return;
        }
        orderService.findOrder(orderId).ifPresentOrElse(orderRecord -> {
            this.order = orderRecord;
            if (Boolean.TRUE.equals(externalContext.getFlash().get("showMessage"))) {
                context.addMessage(null, new FacesMessage(FacesMessage.SEVERITY_INFO, "Payment confirmed and PINs delivered", null));
            }
        }, () -> context.addMessage(null, new FacesMessage(FacesMessage.SEVERITY_ERROR, "Order not found", null)));
    }

    public OrderRecord getOrder() {
        return order;
    }
}
