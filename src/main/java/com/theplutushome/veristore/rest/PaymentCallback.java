/*
 * Click nbfs://nbhost/SystemFileSystem/Templates/Licenses/license-default.txt to change this license
 * Click nbfs://nbhost/SystemFileSystem/Templates/Classes/Class.java to edit this template
 */
package com.theplutushome.veristore.rest;

import com.theplutushome.veristore.service.payment.PaymentService;
import jakarta.inject.Inject;
import jakarta.ws.rs.Consumes;
import jakarta.ws.rs.POST;
import jakarta.ws.rs.Path;
import jakarta.ws.rs.QueryParam;
import jakarta.ws.rs.core.MediaType;
import jakarta.ws.rs.core.Response;

import java.util.logging.Level;
import java.util.logging.Logger;

/**
 *
 * @author MalickMoro-Samah
 */
@Path("/payment/callback")
public class PaymentCallback {

    private static final Logger LOGGER = Logger.getLogger(PaymentCallback.class.getName());

    @Inject
    private PaymentService paymentService;

    @Consumes(MediaType.APPLICATION_FORM_URLENCODED)
    @POST
    public Response processCallback(@QueryParam(value = "invoice_number") String invoiceNumber) {
        if (invoiceNumber == null || invoiceNumber.isBlank()) {
            LOGGER.warning("Payment callback received without invoice number");
            return Response.status(Response.Status.BAD_REQUEST).entity("invoice_number is required").build();
        }
        String normalized = invoiceNumber.trim();
        LOGGER.log(Level.INFO, () -> "Processing payment callback for invoice " + normalized);
        try {
            boolean fulfilled = paymentService.processGatewayCallback(normalized);
            if (fulfilled) {
                return Response.ok().build();
            }
            return Response.status(Response.Status.ACCEPTED).build();
        } catch (Exception e) {
            LOGGER.log(Level.SEVERE, "Error processing payment callback", e);
            return Response.serverError().build();
        }
    }

}
