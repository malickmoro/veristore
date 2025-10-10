/*
 * Click nbfs://nbhost/SystemFileSystem/Templates/Licenses/license-default.txt to change this license
 * Click nbfs://nbhost/SystemFileSystem/Templates/Classes/Class.java to edit this template
 */
package com.theplutushome.veristore.payload.request;

import java.io.Serializable;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
/**
 *
 * @author MalickMoro-Samah
 */
@Getter
@Setter
@AllArgsConstructor
@NoArgsConstructor
public class TransactionRequest implements Serializable {

    private String serviceId;
    private int quantity;
    private String paymentMode;
    private double amountPaid;
    private double exchangeRate;
    private String tellerName;
    private String paymentId;
    private String dateOfPayment;
    private String source;

}
