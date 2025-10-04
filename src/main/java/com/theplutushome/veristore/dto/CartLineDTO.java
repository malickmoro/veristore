package com.theplutushome.veristore.dto;

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
}
