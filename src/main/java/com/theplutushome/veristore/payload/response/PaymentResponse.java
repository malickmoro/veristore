/*
 * Click nbfs://nbhost/SystemFileSystem/Templates/Licenses/license-default.txt to change this license
 * Click nbfs://nbhost/SystemFileSystem/Templates/Classes/Class.java to edit this template
 */
package com.theplutushome.veristore.payload.response;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.theplutushome.veristore.payload.enums.PaymentCurrency;
import java.math.BigDecimal;
import lombok.Getter;
import lombok.Setter;

/**
 *
 * @author MalickMoro-Samah
 */
@Getter
@Setter
@JsonIgnoreProperties(ignoreUnknown = true)
public class PaymentResponse {

    private int status;
    private String message;
    private InvoiceDetails output;

    @Getter @Setter @JsonIgnoreProperties(ignoreUnknown = true)
    public static class InvoiceDetails {

        @JsonProperty(value = "payment_status_code")
        private int paymentStatusCode;
        
        @JsonProperty(value = "payment_status_text")
        private String paymentStatusText;
        
        private BigDecimal amount;

        private PaymentCurrency currency;
        
        @JsonProperty(value = "invoice_number")
        private String invoiceNumber;
        
        @JsonProperty(value = "date_processed")
        private String dateProcessed;
        
        @JsonProperty(value = "payment_reference")
        private String paymentReference;
    }
}
