package com.theplutushome.veristore.view;

import jakarta.enterprise.context.RequestScoped;
import jakarta.inject.Named;
import java.io.Serializable;

@Named
@RequestScoped
public class RedirectView implements Serializable {

    public String toBuyVerification() {
        return "/buy-verification?faces-redirect=true";
    }
}
