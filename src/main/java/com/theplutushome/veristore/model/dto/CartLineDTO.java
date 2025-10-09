package com.theplutushome.veristore.model.dto;

import com.theplutushome.veristore.model.PaymentMode;
import com.theplutushome.veristore.model.catalog.ProductFamily;

import java.io.Serial;
import java.io.Serializable;

public class CartLineDTO implements Serializable {

    @Serial
    private static final long serialVersionUID = 1L;

    private String sku;
    private String name;
    private int qty;
    private long unitPriceMinor;
    private long totalMinor;
    private String priceFormatted;
    private String totalFormatted;
    private String currency;
    private ProductFamily family;
    private PaymentMode paymentMode;
    private boolean deliverEmail;
    private boolean deliverSms;
    private String email;
    private String msisdn;

    public CartLineDTO() {
    }

    public CartLineDTO(CartLineDTO other) {
        this.sku = other.sku;
        this.name = other.name;
        this.qty = other.qty;
        this.unitPriceMinor = other.unitPriceMinor;
        this.totalMinor = other.totalMinor;
        this.priceFormatted = other.priceFormatted;
        this.totalFormatted = other.totalFormatted;
        this.currency = other.currency;
        this.family = other.family;
        this.paymentMode = other.paymentMode;
        this.deliverEmail = other.deliverEmail;
        this.deliverSms = other.deliverSms;
        this.email = other.email;
        this.msisdn = other.msisdn;
    }

    public String getSku() {
        return sku;
    }

    public void setSku(String sku) {
        this.sku = sku;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public int getQty() {
        return qty;
    }

    public void setQty(int qty) {
        this.qty = qty;
    }

    public long getUnitPriceMinor() {
        return unitPriceMinor;
    }

    public void setUnitPriceMinor(long unitPriceMinor) {
        this.unitPriceMinor = unitPriceMinor;
    }

    public long getTotalMinor() {
        return totalMinor;
    }

    public void setTotalMinor(long totalMinor) {
        this.totalMinor = totalMinor;
    }

    public String getPriceFormatted() {
        return priceFormatted;
    }

    public void setPriceFormatted(String priceFormatted) {
        this.priceFormatted = priceFormatted;
    }

    public String getTotalFormatted() {
        return totalFormatted;
    }

    public void setTotalFormatted(String totalFormatted) {
        this.totalFormatted = totalFormatted;
    }

    public String getCurrency() {
        return currency;
    }

    public void setCurrency(String currency) {
        this.currency = currency;
    }

    public ProductFamily getFamily() {
        return family;
    }

    public void setFamily(ProductFamily family) {
        this.family = family;
    }

    public PaymentMode getPaymentMode() {
        return paymentMode;
    }

    public void setPaymentMode(PaymentMode paymentMode) {
        this.paymentMode = paymentMode;
    }

    public boolean isDeliverEmail() {
        return deliverEmail;
    }

    public void setDeliverEmail(boolean deliverEmail) {
        this.deliverEmail = deliverEmail;
    }

    public boolean isDeliverSms() {
        return deliverSms;
    }

    public void setDeliverSms(boolean deliverSms) {
        this.deliverSms = deliverSms;
    }

    public String getEmail() {
        return email;
    }

    public void setEmail(String email) {
        this.email = email;
    }

    public String getMsisdn() {
        return msisdn;
    }

    public void setMsisdn(String msisdn) {
        this.msisdn = msisdn;
    }
}
