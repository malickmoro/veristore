/*
 * Click nbfs://nbhost/SystemFileSystem/Templates/Licenses/license-default.txt to change this license
 * Click nbfs://nbhost/SystemFileSystem/Templates/Classes/Class.java to edit this template
 */
package com.theplutushome.veristore.payment;

import jakarta.ws.rs.Consumes;
import jakarta.ws.rs.POST;
import jakarta.ws.rs.Path;
import jakarta.ws.rs.QueryParam;
import jakarta.ws.rs.core.Response;

/**
 *
 * @author MalickMoro-Samah
 */
@Path("/payment/callback")
public class PaymentCallback {

    @Consumes(value = "application/x-www-form-urlencoded")
    @POST
    public Response processCallback(@QueryParam(value = "invoice_number") String invoiceNumber) {
        System.out.println("The invoice number received is >>>>>>> " + invoiceNumber);
        try {          
        } catch (Exception e) {
            System.out.println("ERROR FROM CALLBACK PROCESS >>>>>>>>>>>>> " + e.getMessage());
            return Response.status(400).build();
        }

        return Response.status(400).build();
    }

}
