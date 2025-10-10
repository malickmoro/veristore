package com.theplutushome.veristore.rest.mock;

import com.theplutushome.veristore.service.payment.PaymentService;

import jakarta.inject.Inject;
import jakarta.ws.rs.DefaultValue;
import jakarta.ws.rs.GET;
import jakarta.ws.rs.Path;
import jakarta.ws.rs.Produces;
import jakarta.ws.rs.QueryParam;
import jakarta.ws.rs.core.MediaType;
import jakarta.ws.rs.core.Response;

import java.util.logging.Level;
import java.util.logging.Logger;

@Path("/mock/hubtel")
@Produces(MediaType.TEXT_PLAIN)
public class MockHubtelCallbackResource {

    private static final Logger LOGGER = Logger.getLogger(MockHubtelCallbackResource.class.getName());

    @Inject
    private PaymentService paymentService;

    @GET
    @Path("/callback")
    public Response simulatePayment(@QueryParam("invoice") String invoiceNo,
                                    @QueryParam("paid") @DefaultValue("false") boolean paid) {
        if (invoiceNo == null || invoiceNo.isBlank()) {
            return Response.status(Response.Status.BAD_REQUEST)
                    .entity("invoice parameter is required")
                    .build();
        }
        String normalized = invoiceNo.trim();
        if (!paid) {
            LOGGER.log(Level.INFO, () -> "Simulated payment for invoice " + normalized + " not marked as paid");
            return Response.status(Response.Status.ACCEPTED)
                    .entity("Payment not completed; invoice remains pending.")
                    .build();
        }
        try {
            boolean fulfilled = paymentService.processGatewayCallback(normalized);
            if (fulfilled) {
                return Response.ok("Payment simulated successfully for invoice " + normalized + ".").build();
            }
            return Response.status(Response.Status.ACCEPTED)
                    .entity("Invoice " + normalized + " could not be fulfilled.")
                    .build();
        } catch (Exception ex) {
            LOGGER.log(Level.SEVERE, "Error handling simulated payment for invoice " + normalized, ex);
            return Response.serverError()
                    .entity("An error occurred while processing the simulated payment.")
                    .build();
        }
    }
}
