package com.theplutushome.veristore.payment;

import java.io.Serial;
import java.io.Serializable;
import java.net.URI;

import jakarta.enterprise.context.ApplicationScoped;

@ApplicationScoped
public class PaymentService implements Serializable {

    @Serial
    private static final long serialVersionUID = 1L;

    public URI createInvoiceAndRedirect(long totalMinor, String currency) {
        return URI.create("#psp-redirect");
    }
}
