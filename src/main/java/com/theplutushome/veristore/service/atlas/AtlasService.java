/*
 * Click nbfs://nbhost/SystemFileSystem/Templates/Licenses/license-default.txt to change this license
 * Click nbfs://nbhost/SystemFileSystem/Templates/Classes/Class.java to edit this template
 */
package com.theplutushome.veristore.service.atlas;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.theplutushome.veristore.model.PinRecord;
import com.theplutushome.veristore.model.SubServiceRecord;
import com.theplutushome.veristore.model.TransactionRecord;
import com.theplutushome.veristore.payload.request.TransactionRequest;
import com.theplutushome.veristore.payload.response.ApiResponse;
import jakarta.ejb.Stateless;
import java.io.IOException;
import java.util.Collections;
import java.util.List;
import kong.unirest.HttpResponse;
import kong.unirest.Unirest;
import kong.unirest.UnirestInstance;

/**
 *
 * @author MalickMoro-Samah
 */
@Stateless
public class AtlasService {

    private static final String AUTH_TOKEN = "eyJhbGciOiJIUzUxMiJ9.eyJzdWIiOiIxNzZkMGQyYzNmZjk0YjMzOTU3YWQwZTdjYjk0MmQwOCIsImlhdCI6MTc2MDM3NDQ1MSwiZXhwIjoxODYwMzc0MzE1fQ.gsddoCinuqbfSEKJGVCoAnhSatvp6t8Wt8_1PF_nmW8sESWi5MASF5_VprDkZefF9Wxc07Ukm7Eqaws0biPhgg";
    private static final String BASE_URL = "http://172.16.10.129:1640/atlas/api/v1";
    private static final String GET_SERVICE_INFO_URL = BASE_URL + "/payment/usage/service_types/subcategory/subservice/";
    private static final String CREATE_TRANSACTION_URL = BASE_URL + "/transaction/create";
    private static final String CONFIRM_TRANSACTION_URL = BASE_URL + "/transaction/confirmed/";

    private final ObjectMapper om = new ObjectMapper();
    private final UnirestInstance instance = Unirest.spawnInstance();

    /**
     * Parses a generic API response and validates success.
     */
    private <T> ApiResponse<T> parseResponse(HttpResponse<String> response, Class<T> dataType) throws IOException {
        if (!response.isSuccess()) {
            throw new RuntimeException("HTTP " + response.getStatus() + ": " + response.getBody());
        }

        ApiResponse<T> apiResponse = om.readValue(
                response.getBody(),
                om.getTypeFactory().constructParametricType(ApiResponse.class, dataType)
        );

        if (!apiResponse.isSuccess() || !"00".equals(apiResponse.getCode())) {
            throw new RuntimeException("API returned error: " + apiResponse.getMsg());
        }
        return apiResponse;
    }

    /**
     * Parses a list-based API response for endpoints returning arrays.
     */
    private <T> ApiResponse<List<T>> parseListResponse(HttpResponse<String> response, Class<T> elementType) throws IOException {
        if (!response.isSuccess()) {
            throw new RuntimeException("HTTP " + response.getStatus() + ": " + response.getBody());
        }

        ApiResponse<List<T>> apiResponse = om.readValue(
                response.getBody(),
                om.getTypeFactory().constructParametricType(
                        ApiResponse.class,
                        om.getTypeFactory().constructCollectionType(List.class, elementType)
                )
        );

        if (!apiResponse.isSuccess() || !"00".equals(apiResponse.getCode())) {
            throw new RuntimeException("API returned error: " + apiResponse.getMsg());
        }
        return apiResponse;
    }

    /**
     * Fetch sub-service details from Atlas.
     */
    public SubServiceRecord getServiceInfo(String service) {
        try {
            HttpResponse<String> response = instance.get(GET_SERVICE_INFO_URL + service)
                    .header("Authorization", "Bearer " + AUTH_TOKEN)
                    .asString();

            ApiResponse<List<SubServiceRecord>> apiResponse = parseListResponse(response, SubServiceRecord.class);
            List<SubServiceRecord> records = apiResponse.getData();

            return (records == null || records.isEmpty()) ? null : records.get(0);

        } catch (Exception e) {
            System.err.println("Error fetching service info for: " + service);
            e.printStackTrace();
            return null;
        }
    }

    /**
     * Creates a new transaction record.
     */
    public TransactionRecord createTransaction(TransactionRequest transaction) {
        try {
            HttpResponse<String> response = instance.post(CREATE_TRANSACTION_URL)
                    .header("Authorization", "Bearer " + AUTH_TOKEN)
                    .contentType("application/json")
                    .body(transaction)
                    .asString();

            ApiResponse<TransactionRecord> apiResponse = parseResponse(response, TransactionRecord.class);
            return apiResponse.getData();

        } catch (Exception e) {
            System.err.println("Error creating transaction for: " + transaction.getServiceId());
            e.printStackTrace();
            return null;
        }
    }

    /**
     * Confirms a transaction and retrieves associated PIN records.
     */
    public List<PinRecord> confirmTransaction(String transactionId) {
        try {
            HttpResponse<String> response = instance.get(CONFIRM_TRANSACTION_URL + transactionId)
                    .header("Authorization", "Bearer " + AUTH_TOKEN)
                    .asString();

            ApiResponse<List<PinRecord>> apiResponse = parseListResponse(response, PinRecord.class);
            return apiResponse.getData() == null ? Collections.emptyList() : apiResponse.getData();

        } catch (Exception e) {
            System.err.println("Error confirming transaction for: " + transactionId);
            e.printStackTrace();
            return Collections.emptyList();
        }
    }
}
