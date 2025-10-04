package com.theplutushome.veristore.payment;

import com.theplutushome.veristore.domain.InvoiceStatus;

import jakarta.enterprise.context.ApplicationScoped;
import java.io.Serial;
import java.io.Serializable;
import java.net.URI;
import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.util.Map;
import java.util.Objects;

@ApplicationScoped
public class MockHubtelGateway implements PaymentGateway, Serializable {

    @Serial
    private static final long serialVersionUID = 1L;

    @Override
    public URI beginCheckout(String invoiceNo) {
        Objects.requireNonNull(invoiceNo, "invoiceNo");
        String encoded = URLEncoder.encode(invoiceNo, StandardCharsets.UTF_8);
        String redirect = String.format("/api/mock/hubtel/callback?invoice=%s&paid=true", encoded);
        return URI.create(redirect);
    }

    @Override
    public InvoiceStatus verifyCallback(Map<String, String> parameters) {
        if (parameters == null) {
            return InvoiceStatus.PENDING;
        }
        String paid = parameters.getOrDefault("paid", "false");
        return Boolean.parseBoolean(paid) ? InvoiceStatus.PAID : InvoiceStatus.PENDING;
    }
}
