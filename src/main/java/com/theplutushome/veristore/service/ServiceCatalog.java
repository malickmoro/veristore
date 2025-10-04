package com.theplutushome.veristore.service;

import com.theplutushome.veristore.domain.EnrollmentType;
import com.theplutushome.veristore.domain.PinCategory;
import com.theplutushome.veristore.domain.ServiceDefinition;
import com.theplutushome.veristore.domain.ServiceKey;
import com.theplutushome.veristore.domain.VerificationDuration;

import jakarta.annotation.PostConstruct;
import jakarta.enterprise.context.ApplicationScoped;
import java.io.Serializable;
import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.Collections;
import java.util.EnumSet;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;

@ApplicationScoped
public class ServiceCatalog implements Serializable {

    private final Map<ServiceKey, ServiceDefinition> definitions = new LinkedHashMap<>();

    @PostConstruct
    void init() {
        for (VerificationDuration duration : VerificationDuration.values()) {
            ServiceKey key = new ServiceKey(PinCategory.VERIFICATION, duration.name());
            String name = String.format("Verification PIN - %s", duration.getLabel());
            String description = String.format("Digital verification PIN valid for %s.", duration.getLabel().toLowerCase());
            BigDecimal price = new BigDecimal(20 + duration.ordinal() * 5);
            definitions.put(key, new ServiceDefinition(key, name, description, price));
        }

        for (EnrollmentType type : EnumSet.allOf(EnrollmentType.class)) {
            ServiceKey key = new ServiceKey(PinCategory.ENROLLMENT, type.name());
            String groupLabel = type.getGroupLabel();
            String actionLabel = type.getActionLabel();
            String name = String.format("Enrollment PIN - %s %s", groupLabel, actionLabel);
            String description = String.format("Enrollment PIN for %s applicants (%s).", groupLabel.toLowerCase(), actionLabel.toLowerCase());
            BigDecimal base = basePrice(type);
            BigDecimal adjustment = type.isUpdate() ? new BigDecimal("-5") : BigDecimal.ZERO;
            BigDecimal price = base.add(adjustment);
            definitions.put(key, new ServiceDefinition(key, name, description, price));
        }
    }

    public List<ServiceDefinition> getVerificationServices() {
        List<ServiceDefinition> services = new ArrayList<>();
        for (VerificationDuration duration : VerificationDuration.values()) {
            services.add(definitions.get(new ServiceKey(PinCategory.VERIFICATION, duration.name())));
        }
        return Collections.unmodifiableList(services);
    }

    public List<ServiceDefinition> getEnrollmentServices() {
        List<ServiceDefinition> services = new ArrayList<>();
        for (EnrollmentType type : EnrollmentType.values()) {
            services.add(definitions.get(new ServiceKey(PinCategory.ENROLLMENT, type.name())));
        }
        return Collections.unmodifiableList(services);
    }

    public ServiceDefinition getDefinition(ServiceKey key) {
        return Optional.ofNullable(definitions.get(key))
            .orElseThrow(() -> new IllegalArgumentException("Service not found: " + key));
    }

    private BigDecimal basePrice(EnrollmentType type) {
        return switch (type) {
            case CITIZEN_FIRST_ISSUANCE, CITIZEN_UPDATE -> new BigDecimal("40");
            case FOREIGNER_FIRST_ISSUANCE, FOREIGNER_UPDATE -> new BigDecimal("55");
            case REFUGEE_FIRST_ISSUANCE, REFUGEE_UPDATE -> new BigDecimal("30");
        };
    }
}
