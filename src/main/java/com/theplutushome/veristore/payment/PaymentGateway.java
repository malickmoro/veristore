package com.theplutushome.veristore.payment;

import com.theplutushome.veristore.domain.InvoiceStatus;

import java.net.URI;
import java.util.Map;

public interface PaymentGateway {

    URI beginCheckout(String invoiceNo);

    InvoiceStatus verifyCallback(Map<String, String> parameters);
}
