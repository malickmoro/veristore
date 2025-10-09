/*
 * Click nbfs://nbhost/SystemFileSystem/Templates/Licenses/license-default.txt to change this license
 * Click nbfs://nbhost/SystemFileSystem/Templates/Classes/Class.java to edit this template
 */
package com.theplutushome.veristore.service.atlas;

import com.theplutushome.veristore.model.SubServiceRecord;
import com.theplutushome.veristore.model.TransactionRecord;
import com.theplutushome.veristore.payload.request.TransactionRequest;
import com.theplutushome.veristore.payload.response.ApiResponse;
import com.theplutushome.veristore.payload.response.PinResponse;
import com.theplutushome.veristore.payload.response.ServiceInfoResponse;
import jakarta.ejb.Stateless;
import kong.unirest.Unirest;
import kong.unirest.UnirestInstance;

/**
 *
 * @author MalickMoro-Samah
 */
@Stateless
public class AtlasService {

    private final String AUTH_TOKEN = "";
    private final String BASE_URL = "http://localhost:1640/atlas/api/v1";
    private final String GET_SERVICE_INFO_URL = BASE_URL + "/payment/usage/service_types/subcategory/subservice/";
    private final String CREATE_TRANSACTION_URL = BASE_URL + "/transaction/create";
    private final String CONFIRM_TRANSACTION_URL = BASE_URL + "/transaction/confirmed/";

    UnirestInstance instance = Unirest.spawnInstance();

    public SubServiceRecord getServiceInfo(String service) {
        instance.get(GET_SERVICE_INFO_URL + service)
                .header("Authorization", "BEARER " + AUTH_TOKEN);
    }

    public TransactionRecord createTransaction(TransactionRequest transaction) {
        instance.post(CREATE_TRANSACTION_URL).body(transaction)
                .header("Authorization", "BEARER " + AUTH_TOKEN);
    }

    public PinResponse confirmTransaction(String transactionId) {
        instance.get(CONFIRM_TRANSACTION_URL + transactionId)
                .header("Authorization", "BEARER " + AUTH_TOKEN);
    }

}
