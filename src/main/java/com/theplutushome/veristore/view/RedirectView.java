package com.theplutushome.veristore.view;

import javax.enterprise.context.RequestScoped;
import javax.inject.Named;
import java.io.Serializable;

@Named
@RequestScoped
public class RedirectView implements Serializable {

    public String toBuyVerification() {
        return "/buy-verification?faces-redirect=true";
    }
}
