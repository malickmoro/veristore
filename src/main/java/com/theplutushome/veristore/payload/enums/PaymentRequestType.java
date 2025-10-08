/*
 * Click nbfs://nbhost/SystemFileSystem/Templates/Licenses/license-default.txt to change this license
 * Click nbfs://nbhost/SystemFileSystem/Templates/Classes/Enum.java to edit this template
 */
package com.theplutushome.veristore.payload.enums;

import lombok.Getter;

/**
 *
 * @author MalickMoro-Samah
 */
public enum PaymentRequestType {
    pay_with_momo("Mobile Money"),
    pay_with_card("Debit / Credit Card");

    @Getter
    private final String label;

    private PaymentRequestType(String label) {
        this.label = label;
    }
}
