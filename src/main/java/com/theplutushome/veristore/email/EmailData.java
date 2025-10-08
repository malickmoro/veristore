/*
 * Click nbfs://nbhost/SystemFileSystem/Templates/Licenses/license-default.txt to change this license
 * Click nbfs://nbhost/SystemFileSystem/Templates/Classes/Class.java to edit this template
 */
package com.theplutushome.veristore.email;

import lombok.Builder;
import lombok.Getter;
import lombok.Setter;

/**
 *
 * @author MalickMoro-Samah
 */
@Builder
@Getter
@Setter 
public class EmailData {
    private String recipient;
    @Builder.Default
    private String name = "Applicant";
    private String applicationId;
    private String userAgent;
    private String date;
    private String location;
    private String otpCode;
    private String timeSlot;
    private String temporaryPassword;
}
