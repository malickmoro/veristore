package com.theplutushome.veristore.service;

import com.theplutushome.veristore.domain.EnrollmentType;
import com.theplutushome.veristore.domain.PinCategory;
import com.theplutushome.veristore.domain.Price;
import com.theplutushome.veristore.domain.ServiceKey;

import javax.enterprise.context.ApplicationScoped;
import java.io.Serializable;
import java.math.BigDecimal;
import java.text.NumberFormat;
import java.util.Currency;
import java.util.Locale;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

@ApplicationScoped
public class PricingService implements Serializable {

    private static final Locale USD_LOCALE = Locale.US;

    private final Map<ServiceKey, Price> prices = new ConcurrentHashMap<>();

    public PricingService() {
        prices.put(new ServiceKey(PinCategory.VERIFICATION, "Y1"), new Price("USD", 50_000));
        prices.put(new ServiceKey(PinCategory.VERIFICATION, "Y2"), new Price("USD", 90_000));
        prices.put(new ServiceKey(PinCategory.VERIFICATION, "Y3"), new Price("USD", 120_000));

        prices.put(new ServiceKey(PinCategory.ENROLLMENT, EnrollmentType.CITIZEN_FIRST_ISSUANCE.name()), new Price("USD", 100_000));
        prices.put(new ServiceKey(PinCategory.ENROLLMENT, EnrollmentType.CITIZEN_UPDATE.name()), new Price("USD", 60_000));
        prices.put(new ServiceKey(PinCategory.ENROLLMENT, EnrollmentType.FOREIGNER_FIRST_ISSUANCE.name()), new Price("USD", 150_000));
        prices.put(new ServiceKey(PinCategory.ENROLLMENT, EnrollmentType.FOREIGNER_UPDATE.name()), new Price("USD", 90_000));
        prices.put(new ServiceKey(PinCategory.ENROLLMENT, EnrollmentType.REFUGEE_FIRST_ISSUANCE.name()), new Price("USD", 30_000));
        prices.put(new ServiceKey(PinCategory.ENROLLMENT, EnrollmentType.REFUGEE_UPDATE.name()), new Price("USD", 20_000));
    }

    public Price get(ServiceKey key) {
        Price price = prices.get(key);
        if (price == null) {
            throw new IllegalArgumentException("Unknown service key: " + key);
        }
        return price;
    }

    public String format(Price price) {
        NumberFormat formatter = NumberFormat.getCurrencyInstance(USD_LOCALE);
        formatter.setCurrency(Currency.getInstance(price.currency()));
        BigDecimal major = BigDecimal.valueOf(price.amountMinor(), 2);
        return formatter.format(major);
    }
}
