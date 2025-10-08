/*
 * Click nbfs://nbhost/SystemFileSystem/Templates/Licenses/license-default.txt to change this license
 * Click nbfs://nbhost/SystemFileSystem/Templates/Classes/Class.java to edit this template
 */
package com.theplutushome.veristore.payment.impl;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.theplutushome.veristore.annotations.GOV;
import com.theplutushome.veristore.config.CustomSSLConfig;
import com.theplutushome.veristore.payload.request.CreateInvoiceRequest;
import com.theplutushome.veristore.payload.request.InvoiceCheckRequest;
import com.theplutushome.veristore.payload.response.CreateInvoiceResponse;
import com.theplutushome.veristore.payload.response.PaymentResponse;
import jakarta.enterprise.context.ApplicationScoped;
import kong.unirest.HttpResponse;
import kong.unirest.Unirest;
import kong.unirest.UnirestInstance;

/**
 *
 * @author MalickMoro-Samah
 */
@GOV
@ApplicationScoped
public class GovCheckout {

    private final String BASE_URL = "https://www.govgh.org/api/v1.0/";
    private final String DIRECT_ENPOINT = BASE_URL + "checkout/direct.php";
    private final String INVOICE_ENDPOINT = BASE_URL + "checkout/invoice.php";
    ObjectMapper mapper = new ObjectMapper();

    UnirestInstance instance;

    public GovCheckout() {
        instance = Unirest.spawnInstance();
        instance.config().socketTimeout(15000)
                .sslContext(CustomSSLConfig.getSSLContext())
                .connectTimeout(150000)
                .automaticRetries(true)
                .verifySsl(false); // <-- disable hostname/SAN verification
    }

    public CreateInvoiceResponse initiateCheckout(CreateInvoiceRequest request) {
        try {
            String json = new ObjectMapper().writeValueAsString(request);
            System.out.println("Final JSON sent to API >>> " + json);
            HttpResponse<String> response = instance.post(INVOICE_ENDPOINT)
                    .body(json)
                    .header("Content-Type", "application/json") // 👈 add this
                    .asString();
            System.out.println("The response for checkout >>>> " + response.getBody());
            if (!response.isSuccess()) {
                return null;
            }
            return mapper.readValue(response.getBody(), CreateInvoiceResponse.class);
        } catch (JsonProcessingException ex) {
            ex.printStackTrace();
            return null;
        }
    }

    public PaymentResponse checkInvoiceStatus(InvoiceCheckRequest request) {
        try {
            System.out.println("The request for invoice check>>>> " + request.toString());
            HttpResponse<String> response = instance.get(INVOICE_ENDPOINT)
                    .queryString("request", request.getRequest())
                    .queryString("api_key", request.getApiKey())
                    .queryString("invoice_number", request.getInvoiceNumber())
                    .asString();
            System.out.println("The response for invoice check >>>> " + response.getBody());
            return mapper.readValue(response.getBody(), PaymentResponse.class);
        } catch (JsonProcessingException ex) {
            ex.printStackTrace();
            return null;
        }
    }
}
