/*
 * Click nbfs://nbhost/SystemFileSystem/Templates/Licenses/license-default.txt to change this license
 * Click nbfs://nbhost/SystemFileSystem/Templates/Classes/Class.java to edit this template
 */
package com.theplutushome.veristore.model;

import com.theplutushome.veristore.payload.request.TransactionRequest;

/**
 *
 * @author MalickMoro-Samah
 */
public record TransactionRecord(String transactionId, String comfirmURL, String cancelURL, TransactionRequest request) {

}
