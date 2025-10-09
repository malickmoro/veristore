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

    public final static String API_KEY = "d24b17b0ba4500e4b1db5abb91b361f963d1c15d71a10c815008b4bf3c8d379170ef808d950bb1af7c9beda862061c4ba0756b270b8791ae30b51f9b1a47dc2407964ba17b6b698dd2754af3a55be3de7024b2e9ee8c84bdbda816475ac92fbfbcf215f93005b62635fe17b40d39c4db";
    public final static String MDA_BRANCH = "NIA_HEAD_OFFICE";
    public final static String POST_URL = "http://localhost:8080/veristore/rest/api/payment/callback";
    public final static String REDIRECT_URL = "http://localhost:8080/veristore/payment/status.xhtml?tranxCode=";
}
