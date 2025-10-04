package com.theplutushome.veristore.catalog;

import com.theplutushome.veristore.domain.ApplicationType;
import com.theplutushome.veristore.domain.CitizenTier;
import com.theplutushome.veristore.domain.CitizenshipType;
import com.theplutushome.veristore.domain.Currency;
import com.theplutushome.veristore.domain.Price;
import com.theplutushome.veristore.domain.UpdateType;
import jakarta.annotation.Nullable;

import java.io.Serializable;
import java.util.Arrays;
import java.util.Comparator;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.stream.Collectors;

public enum EnrollmentSku implements Serializable {
    REGULAR_PERSONAL_INFORMATION_UPDATE("CU", "Regular Personal Information Update", Currency.GHS, 60, CitizenshipType.CITIZEN, CitizenTier.STANDARD, ApplicationType.UPDATE, UpdateType.PERSONAL_INFORMATION, 0, true),
    REGULAR_DOB_UPDATE("CB", "Regular DOB Update", Currency.GHS, 60, CitizenshipType.CITIZEN, CitizenTier.STANDARD, ApplicationType.UPDATE, UpdateType.DOB, 0, true),
    REGULAR_PICTURE_UPDATE("CP", "Regular Picture Update", Currency.GHS, 60, CitizenshipType.CITIZEN, CitizenTier.STANDARD, ApplicationType.UPDATE, UpdateType.PICTURE, 0, true),
    REGULAR_NATIONALITY_UPDATE("CD", "Regular Nationality Update", Currency.GHS, 70, CitizenshipType.CITIZEN, CitizenTier.STANDARD, ApplicationType.UPDATE, UpdateType.NATIONALITY, 0, true),
    REGULAR_REPLACEMENT("CR", "Regular Replacement", Currency.GHS, 125, CitizenshipType.CITIZEN, CitizenTier.STANDARD, ApplicationType.REPLACEMENT, null, 0, true),
    REGULAR_RENEWAL("CN", "Regular Renewal", Currency.GHS, 60, CitizenshipType.CITIZEN, CitizenTier.STANDARD, ApplicationType.RENEWAL, null, 0, true),

    PREMIUM_SECONDARY_DATA_UPDATE("RU", "Premium Secondary Data Update", Currency.GHS, 310, CitizenshipType.CITIZEN, CitizenTier.PREMIUM, ApplicationType.UPDATE, UpdateType.SECONDARY_DATA, 0, true),
    PREMIUM_PERSONAL_INFORMATION_UPDATE("PU", "Premium Personal Information Update", Currency.GHS, 355, CitizenshipType.CITIZEN, CitizenTier.PREMIUM, ApplicationType.UPDATE, UpdateType.PERSONAL_INFORMATION, 0, true),
    PREMIUM_DOB_UPDATE("PB", "Premium DOB Update", Currency.GHS, 355, CitizenshipType.CITIZEN, CitizenTier.PREMIUM, ApplicationType.UPDATE, UpdateType.DOB, 0, true),
    PREMIUM_PICTURE_UPDATE("PP", "Premium Picture Update", Currency.GHS, 355, CitizenshipType.CITIZEN, CitizenTier.PREMIUM, ApplicationType.UPDATE, UpdateType.PICTURE, 0, true),
    PREMIUM_NATIONALITY_UPDATE("PD", "Premium Nationality Update", Currency.GHS, 365, CitizenshipType.CITIZEN, CitizenTier.PREMIUM, ApplicationType.UPDATE, UpdateType.NATIONALITY, 0, true),
    PREMIUM_REPLACEMENT("PR", "Premium Replacement", Currency.GHS, 420, CitizenshipType.CITIZEN, CitizenTier.PREMIUM, ApplicationType.REPLACEMENT, null, 0, true),
    PREMIUM_RENEWAL("PN", "Premium Renewal", Currency.GHS, 355, CitizenshipType.CITIZEN, CitizenTier.PREMIUM, ApplicationType.RENEWAL, null, 0, true),

    NON_CITIZEN_PERSONAL_INFORMATION_UPDATE("FU", "Non-citizen Personal Information Update", Currency.USD, 60, CitizenshipType.NON_CITIZEN, null, ApplicationType.UPDATE, UpdateType.PERSONAL_INFORMATION, 0, true),
    NON_CITIZEN_DOB_UPDATE("FB", "Non-citizen DOB Update", Currency.USD, 60, CitizenshipType.NON_CITIZEN, null, ApplicationType.UPDATE, UpdateType.DOB, 0, true),
    NON_CITIZEN_NATIONALITY_UPDATE("CF", "Non-citizen Nationality Update", Currency.USD, 120, CitizenshipType.NON_CITIZEN, null, ApplicationType.UPDATE, UpdateType.NATIONALITY, 0, true),
    NON_CITIZEN_REPLACEMENT("FR", "Non-citizen Replacement", Currency.USD, 60, CitizenshipType.NON_CITIZEN, null, ApplicationType.REPLACEMENT, null, 0, true),
    NON_CITIZEN_1_YEAR_RENEWAL("RO", "Non-citizen 1-Year Renewal", Currency.USD, 60, CitizenshipType.NON_CITIZEN, null, ApplicationType.RENEWAL, null, 1, true),
    NON_CITIZEN_2_YEAR_RENEWAL("RW", "Non-citizen 2-Year Renewal", Currency.USD, 120, CitizenshipType.NON_CITIZEN, null, ApplicationType.RENEWAL, null, 2, true),
    NON_CITIZEN_3_YEAR_RENEWAL("RH", "Non-citizen 3-Year Renewal", Currency.USD, 180, CitizenshipType.NON_CITIZEN, null, ApplicationType.RENEWAL, null, 3, true),
    NON_CITIZEN_5_YEAR_RENEWAL("RF", "Non-citizen 5-Year Renewal", Currency.USD, 300, CitizenshipType.NON_CITIZEN, null, ApplicationType.RENEWAL, null, 5, true),

    REFUGEE_PERSONAL_INFORMATION_UPDATE("ZU", "Refugee Personal Information Update", Currency.USD, 15, CitizenshipType.REFUGEE, null, ApplicationType.UPDATE, UpdateType.PERSONAL_INFORMATION, 0, true),
    REFUGEE_DOB_UPDATE("ZB", "Refugee DOB Update", Currency.USD, 15, CitizenshipType.REFUGEE, null, ApplicationType.UPDATE, UpdateType.DOB, 0, true),
    REFUGEE_NATIONALITY_UPDATE("ZD", "Refugee Nationality Update", Currency.USD, 15, CitizenshipType.REFUGEE, null, ApplicationType.UPDATE, UpdateType.NATIONALITY, 0, true),
    REFUGEE_REPLACEMENT("ZP", "Refugee Replacement", Currency.USD, 15, CitizenshipType.REFUGEE, null, ApplicationType.REPLACEMENT, null, 0, true),
    REFUGEE_5_YEAR_RENEWAL("ZR", "Refugee 5-Year Renewal", Currency.USD, 15, CitizenshipType.REFUGEE, null, ApplicationType.RENEWAL, null, 5, true),

    CITIZEN_FIRST_ISSUANCE_STANDARD("CFS", "Citizen First Issuance — Standard (Free)", Currency.GHS, 0, CitizenshipType.CITIZEN, CitizenTier.STANDARD, ApplicationType.FIRST_ISSUANCE, null, 0, true),
    CITIZEN_FIRST_ISSUANCE_PREMIUM("CFP", "Citizen First Issuance — Premium", Currency.GHS, 0, CitizenshipType.CITIZEN, CitizenTier.PREMIUM, ApplicationType.FIRST_ISSUANCE, null, 0, true),

    NON_CITIZEN_FIRST_ISSUANCE("NFI", "Non-citizen First Issuance", Currency.USD, 0, CitizenshipType.NON_CITIZEN, null, ApplicationType.FIRST_ISSUANCE, null, 0, false),
    REFUGEE_FIRST_ISSUANCE("RFI", "Refugee First Issuance", Currency.USD, 0, CitizenshipType.REFUGEE, null, ApplicationType.FIRST_ISSUANCE, null, 0, false);

    public final String sku;
    public final String displayName;
    public final Currency currency;
    public final int priceMajor;
    public final CitizenshipType citizenship;
    public final CitizenTier citizenTier;
    public final ApplicationType appType;
    public final UpdateType updateType;
    public final int durationYears;
    public final boolean active;

    EnrollmentSku(String sku,
                  String displayName,
                  Currency currency,
                  int priceMajor,
                  CitizenshipType citizenship,
                  @Nullable CitizenTier citizenTier,
                  ApplicationType appType,
                  @Nullable UpdateType updateType,
                  int durationYears,
                  boolean active) {
        this.sku = Objects.requireNonNull(sku, "sku");
        this.displayName = Objects.requireNonNull(displayName, "displayName");
        this.currency = Objects.requireNonNull(currency, "currency");
        this.priceMajor = priceMajor;
        this.citizenship = Objects.requireNonNull(citizenship, "citizenship");
        this.citizenTier = citizenTier;
        this.appType = Objects.requireNonNull(appType, "appType");
        this.updateType = updateType;
        this.durationYears = durationYears;
        this.active = active;
    }

    public Price price() {
        return Price.ofMajor(currency, priceMajor);
    }

    public Optional<CitizenTier> citizenTier() {
        return Optional.ofNullable(citizenTier);
    }

    public Optional<UpdateType> updateType() {
        return Optional.ofNullable(updateType);
    }

    private static final Map<String, EnrollmentSku> BY_SKU = Arrays.stream(values())
        .collect(Collectors.toUnmodifiableMap(e -> e.sku, e -> e, (a, b) -> a));

    public static Optional<EnrollmentSku> bySku(String sku) {
        return Optional.ofNullable(BY_SKU.get(sku));
    }

    public static List<EnrollmentSku> filter(CitizenshipType citizenshipType,
                                             ApplicationType applicationType,
                                             @Nullable UpdateType updateType,
                                             @Nullable CitizenTier citizenTier) {
        return Arrays.stream(values())
            .filter(v -> v.active)
            .filter(v -> v.citizenship == citizenshipType && v.appType == applicationType)
            .filter(v -> updateType == null || v.updateType == updateType)
            .filter(v -> citizenTier == null || v.citizenTier == citizenTier)
            .sorted(Comparator.comparing(v -> v.displayName))
            .toList();
    }

    public static List<EnrollmentSku> renewalsFor(CitizenshipType citizenshipType) {
        return Arrays.stream(values())
            .filter(v -> v.active && v.citizenship == citizenshipType && v.appType == ApplicationType.RENEWAL)
            .sorted(Comparator.comparingInt(v -> v.durationYears))
            .toList();
    }
}
