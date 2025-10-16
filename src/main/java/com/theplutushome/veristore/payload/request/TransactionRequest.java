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

    @Override
    public String toString() {
        StringBuilder sb = new StringBuilder();
        sb.append("TransactionRequest{");
        sb.append("serviceId=").append(serviceId);
        sb.append(", quantity=").append(quantity);
        sb.append(", paymentMode=").append(paymentMode);
        sb.append(", amountPaid=").append(amountPaid);
        sb.append(", exchangeRate=").append(exchangeRate);
        sb.append(", tellerName=").append(tellerName);
        sb.append(", paymentId=").append(paymentId);
        sb.append(", dateOfPayment=").append(dateOfPayment);
        sb.append(", source=").append(source);
        sb.append('}');
        return sb.toString();
    }
    
    

}
