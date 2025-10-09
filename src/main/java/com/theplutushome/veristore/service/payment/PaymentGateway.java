package com.theplutushome.veristore.service.payment;

import com.theplutushome.veristore.model.InvoiceStatus;

import java.net.URI;
import java.util.Map;

public interface PaymentGateway {

    URI beginCheckout(String invoiceNo);

    InvoiceStatus verifyCallback(Map<String, String> parameters);
}
