/*
 * Click nbfs://nbhost/SystemFileSystem/Templates/Licenses/license-default.txt to change this license
 * Click nbfs://nbhost/SystemFileSystem/Templates/Classes/Class.java to edit this template
 */
package com.theplutushome.veristore.payload.request;

import com.fasterxml.jackson.annotation.JsonProperty;
import com.theplutushome.veristore.config.Variables;
import com.theplutushome.veristore.payload.enums.InvoiceRequestType;
import lombok.Getter;
import lombok.Setter;

/**
 *
 * @author MalickMoro-Samah
 */
@Getter @Setter
public class InvoiceCheckRequest {

    private InvoiceRequestType request;

    @JsonProperty(value = "api_key")
    private String apiKey;

    @JsonProperty(value = "invoice_number")
    private String invoiceNumber;

    public InvoiceCheckRequest(String invoiceNumber) {
        this.request = InvoiceRequestType.get_invoice_status;
        this.apiKey = Variables.API_KEY;
        this.invoiceNumber = invoiceNumber;
    }

    @Override
    public String toString() {
        StringBuilder sb = new StringBuilder();
        sb.append("InvoiceCheckRequest{");
        sb.append("request=").append(request);
        sb.append(", apiKey=").append(apiKey);
        sb.append(", invoiceNumber=").append(invoiceNumber);
        sb.append('}');
        return sb.toString();
    }

}
