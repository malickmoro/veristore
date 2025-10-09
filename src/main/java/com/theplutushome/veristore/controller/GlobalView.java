package com.theplutushome.veristore.controller;

import java.io.Serial;
import java.io.Serializable;
import java.math.BigDecimal;
import java.math.RoundingMode;
import java.text.NumberFormat;
import java.util.Currency;
import java.util.Locale;
import java.util.Optional;

import jakarta.annotation.PostConstruct;
import jakarta.enterprise.context.SessionScoped;
import jakarta.faces.context.FacesContext;
import jakarta.inject.Named;

@Named
@SessionScoped
public class GlobalView implements Serializable {

    @Serial
    private static final long serialVersionUID = 1L;

    private String localeCode = "en";
    private String currency = "USD";
    private boolean rtl;

    @PostConstruct
    public void init() {
        updateLocaleState();
        applyLocaleToViewRoot();
    }

    public String getLocaleCode() {
        return localeCode;
    }

    public void setLocaleCode(String localeCode) {
        this.localeCode = Optional.ofNullable(localeCode).filter(code -> !code.isBlank()).orElse("en");
        updateLocaleState();
        applyLocaleToViewRoot();
    }

    public String getCurrency() {
        return currency;
    }

    public void setCurrency(String currency) {
        this.currency = normalizeCurrency(currency);
    }

    public boolean isRtl() {
        return rtl;
    }

    public Locale getLocale() {
        return Locale.forLanguageTag(localeCode);
    }

    public void changeLocale() {
        updateLocaleState();
        applyLocaleToViewRoot();
    }

    public void changeCurrency() {
        this.currency = normalizeCurrency(this.currency);
    }

    public String formatMoney(long amountMinor, String currencyCode) {
        Currency resolvedCurrency = resolveCurrency(currencyCode);
        int fractionDigits = Math.max(resolvedCurrency.getDefaultFractionDigits(), 0);
        BigDecimal amount = BigDecimal.valueOf(amountMinor, fractionDigits);
        NumberFormat format = NumberFormat.getCurrencyInstance(getLocale());
        format.setCurrency(resolvedCurrency);
        format.setMinimumFractionDigits(fractionDigits);
        format.setMaximumFractionDigits(fractionDigits);
        return format.format(amount.setScale(fractionDigits, RoundingMode.HALF_UP));
    }

    private void updateLocaleState() {
        rtl = "ar".equalsIgnoreCase(localeCode) || localeCode.toLowerCase(Locale.ROOT).startsWith("ar-");
    }

    private void applyLocaleToViewRoot() {
        FacesContext facesContext = FacesContext.getCurrentInstance();
        if (facesContext != null && facesContext.getViewRoot() != null) {
            facesContext.getViewRoot().setLocale(getLocale());
        }
    }

    private String normalizeCurrency(String currencyCode) {
        try {
            return Currency.getInstance(Optional.ofNullable(currencyCode).filter(code -> !code.isBlank()).orElse(this.currency)).getCurrencyCode();
        } catch (IllegalArgumentException ex) {
            return "USD";
        }
    }

    private Currency resolveCurrency(String currencyCode) {
        try {
            if (currencyCode != null && !currencyCode.isBlank()) {
                return Currency.getInstance(currencyCode);
            }
        } catch (IllegalArgumentException ignored) {
        }
        try {
            return Currency.getInstance(this.currency);
        } catch (IllegalArgumentException ex) {
            return Currency.getInstance("USD");
        }
    }
}
