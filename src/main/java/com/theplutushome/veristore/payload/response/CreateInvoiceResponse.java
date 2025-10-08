/*
 * Click nbfs://nbhost/SystemFileSystem/Templates/Licenses/license-default.txt to change this license
 * Click nbfs://nbhost/SystemFileSystem/Templates/Classes/Class.java to edit this template
 */
package com.theplutushome.veristore.payload.response;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonProperty;
import java.util.List;
import lombok.Getter;
import lombok.Setter;

/**
 *
 * @author MalickMoro-Samah
 */
@Getter @Setter
@JsonIgnoreProperties(ignoreUnknown = true)
public class CreateInvoiceResponse {

    private int status;

    private String message;

    @JsonProperty("invoice_number")
    private String invoiceNumber;

    @JsonProperty("narration")
    private String narration;

    @JsonProperty(value = "invoice_expires")
    private String invoiceExpires;

    @JsonProperty(value = "invoice_total_amounts")
    private List<TotalAmount> invoiceTotalAmounts;

    @JsonProperty(value = "invoice_items")
    private List<InvoiceItem> invoiceItems;

    @JsonProperty("invoice_currencies")
    private Object[] invoiceCurrencies;

    @JsonProperty("payment_qr_code")
    private String paymentQrCode;

    @JsonProperty(value = "checkout_url")
    private String checkoutUrl;

    @Getter @Setter
    public static class InvoiceItem {

        private String name;

        @JsonProperty(value = "service_code")
        private String serviceCode;

        private List<Amount> amounts;

        @Getter @Setter
        public static class Amount {

            private String amount;
            private String currency;

            @JsonProperty(value = "fx_rate")
            private String fxRate;
        }
    }

    @Getter @Setter
    public static class TotalAmount {

        @JsonProperty(value = "total_amount")
        private String totalAmount;

        @JsonProperty(value = "total_amount_ccy")
        private String totalAmountCcy;

        @JsonProperty(value = "primary_ccy")
        private boolean primaryCcy;

        @JsonProperty(value = "open_service")
        private boolean openService;
    }

}
