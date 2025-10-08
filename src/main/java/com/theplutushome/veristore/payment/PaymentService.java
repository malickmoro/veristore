/*
 * Click nbfs://nbhost/SystemFileSystem/Templates/Licenses/license-default.txt to change this license
 * Click nbfs://nbhost/SystemFileSystem/Templates/Classes/Class.java to edit this template
 */
package com.theplutushome.veristore.payment;

import com.theplutushome.veristore.payload.request.CreateInvoiceRequest;
import com.theplutushome.veristore.payload.request.InvoiceCheckRequest;
import com.theplutushome.veristore.payload.response.CreateInvoiceResponse;
import com.theplutushome.veristore.payload.response.PaymentResponse;

/**
 *
 * @author MalickMoro-Samah
 */
public interface PaymentService {

    public CreateInvoiceResponse initiateCheckout(CreateInvoiceRequest request);
    
    public PaymentResponse checkInvoiceStatus(InvoiceCheckRequest request);

}
