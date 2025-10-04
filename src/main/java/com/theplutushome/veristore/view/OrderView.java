package com.theplutushome.veristore.view;

import java.io.Serial;
import java.io.Serializable;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

import com.theplutushome.veristore.entity.Order;
import com.theplutushome.veristore.entity.SecretCode;
import com.theplutushome.veristore.repo.OrderRepo;
import com.theplutushome.veristore.repo.SecretCodeRepo;
import com.theplutushome.veristore.util.Masker;

import jakarta.annotation.PostConstruct;
import jakarta.enterprise.context.RequestScoped;
import jakarta.faces.application.FacesMessage;
import jakarta.faces.context.FacesContext;
import jakarta.inject.Inject;
import jakarta.inject.Named;

@Named
@RequestScoped
public class OrderView implements Serializable {

    @Serial
    private static final long serialVersionUID = 1L;

    private Order order;
    private List<SecretCode> codes = Collections.emptyList();

    @Inject
    OrderRepo orderRepo;

    @Inject
    SecretCodeRepo secretCodeRepo;

    @PostConstruct
    public void init() {
        FacesContext facesContext = FacesContext.getCurrentInstance();
        if (facesContext == null) {
            return;
        }
        Map<String, String> params = facesContext.getExternalContext().getRequestParameterMap();
        String publicId = params.getOrDefault("order", params.get("publicId"));
        if (publicId == null || publicId.isBlank()) {
            return;
        }
        orderRepo.findByPublicId(publicId).ifPresent(found -> {
            order = found;
            codes = secretCodeRepo.findByOrderId(found.getId());
        });
    }

    public List<String> maskedCodes() {
        return codes.stream()
                .map(SecretCode::getCode)
                .filter(code -> code != null && !code.isBlank())
                .map(Masker::mask)
                .collect(Collectors.toList());
    }

    public void resendEmail() {
        addMessage(FacesMessage.SEVERITY_INFO, "Email scheduled", "We will resend your codes shortly.");
    }

    public void resendSms() {
        addMessage(FacesMessage.SEVERITY_INFO, "SMS scheduled", "Text delivery will retry within a few minutes.");
    }

    public Order getOrder() {
        return order;
    }

    public boolean isLoaded() {
        return order != null;
    }

    private void addMessage(FacesMessage.Severity severity, String summary, String detail) {
        FacesContext context = FacesContext.getCurrentInstance();
        if (context != null) {
            context.addMessage(null, new FacesMessage(severity, summary, detail));
        }
    }
}
