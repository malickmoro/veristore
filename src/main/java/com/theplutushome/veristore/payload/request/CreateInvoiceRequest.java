/*
 * Click nbfs://nbhost/SystemFileSystem/Templates/Licenses/license-default.txt to change this license
 * Click nbfs://nbhost/SystemFileSystem/Templates/Classes/Class.java to edit this template
 */
package com.theplutushome.veristore.payload.request;

import com.fasterxml.jackson.annotation.JsonProperty;
import com.theplutushome.veristore.config.Variables;
import com.theplutushome.veristore.payload.enums.InvoiceRequestType;
import com.theplutushome.veristore.payload.enums.PaymentCurrency;
import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.List;
import lombok.Getter;
import lombok.Setter;

/**
 *
 * @author MalickMoro-Samah
 */
@Getter @Setter
public class CreateInvoiceRequest {

    private InvoiceRequestType request;

    @JsonProperty(value = "api_key")
    private String apiKey;

    @JsonProperty(value = "mda_branch_code")
    private String mdaBranchCode;

    private String firstname;

    private String lastname;

    private String phonenumber;

    private String email;

    @JsonProperty(value = "application_id")
    private String applicationId;

    private String description;

    @JsonProperty(value = "invoice_items")
    private List<InvoiceItem> invoiceItems = new ArrayList<>();

    @JsonProperty("redirect_url")
    private String redirectUrl;

    @JsonProperty("post_url")
    private String postUrl;

    @JsonProperty("extra_details")
    private String extraDetails;

    @Getter @Setter
    public static class InvoiceItem {

        @JsonProperty(value = "service_code")
        private String serviceCode;

        private BigDecimal amount;

        private PaymentCurrency currency;

        private String memo;

        @JsonProperty(value = "account_number")
        private String accountNumber;

        @Override
        public String toString() {
            return "InvoiceItem{" + "serviceCode=" + serviceCode + ", amount=" + amount + ", currency=" + currency + ", memo=" + memo + ", accountNumber=" + accountNumber + '}';
        }
    }

    public CreateInvoiceRequest() {
        this.request = InvoiceRequestType.create;
        this.apiKey = Variables.API_KEY;
        this.mdaBranchCode = Variables.MDA_BRANCH;
        this.redirectUrl = Variables.REDIRECT_URL;
        this.postUrl = Variables.POST_URL;
    }

    public void addInvoice(InvoiceItem item) {
        invoiceItems.add(item);
    }

    @Override
    public String toString() {
        return "CreateInvoiceRequest{" + "request=" + request + ", apiKey=" + apiKey + ", mdaBranchCode=" + mdaBranchCode + ", firstname=" + firstname + ", lastname=" + lastname + ", phonenumber=" + phonenumber + ", email=" + email + ", applicationId=" + applicationId + ", description=" + description + ", invoiceItems=" + invoiceItems.toString() + ", redirectUrl=" + redirectUrl + ", postUrl=" + postUrl + ", extraDetails=" + extraDetails + '}';
    }

}
