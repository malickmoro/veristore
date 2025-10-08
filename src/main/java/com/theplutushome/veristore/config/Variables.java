/*
 * Click nbfs://nbhost/SystemFileSystem/Templates/Licenses/license-default.txt to change this license
 * Click nbfs://nbhost/SystemFileSystem/Templates/Classes/Class.java to edit this template
 */
package com.theplutushome.veristore.config;

import jakarta.ejb.LocalBean;

/**
 *
 * @author MalickMoro-Samah
 */
@LocalBean
public class Variables {

    public final static String API_KEY = "111";
    public final static String MDA_BRANCH = "111";
    public final static String POST_URL = "http://localhost:8080/veristore/rest/api/payment/callback";
    public final static String REDIRECT_URL = "http://localhost:8080/veristore/payment/status.xhtml?tranxCode=";
}
