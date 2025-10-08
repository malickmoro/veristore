/*
 * Click nbfs://nbhost/SystemFileSystem/Templates/Licenses/license-default.txt to change this license
 * Click nbfs://nbhost/SystemFileSystem/Templates/Classes/Class.java to edit this template
 */
package com.theplutushome.veristore.config;

import java.security.SecureRandom;
import java.security.cert.X509Certificate;
import javax.net.ssl.SSLContext;
import javax.net.ssl.TrustManager;
import javax.net.ssl.X509TrustManager;

/**
 *
 * @author awuah
 */
public class CustomSSLConfig {

    public static SSLContext getSSLContext() {
        try {
            SSLContext ssl = SSLContext.getInstance("SSL");
            ssl.init(null, buildTrustCert(), new SecureRandom());
            return ssl;
        } catch (Exception e) {
            e.printStackTrace();
            return null;
        }
    }

    // Create a trust manager that does not validate certificate chains
    private static TrustManager[] buildTrustCert() {
        return new TrustManager[]{
            new X509TrustManager() {
                public java.security.cert.X509Certificate[] getAcceptedIssuers() {
                    return new X509Certificate[0];
                }

                public void checkClientTrusted(
                        java.security.cert.X509Certificate[] certs, String authType) {
                }

                public void checkServerTrusted(
                        java.security.cert.X509Certificate[] certs, String authType) {
                }
            }
        };
    }
}
