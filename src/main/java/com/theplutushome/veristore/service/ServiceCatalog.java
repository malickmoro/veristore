package com.theplutushome.veristore.service;

import com.theplutushome.veristore.domain.EnrollmentAction;
import com.theplutushome.veristore.domain.EnrollmentType;
import com.theplutushome.veristore.domain.ServiceDefinition;
import com.theplutushome.veristore.domain.ServiceKey;
import com.theplutushome.veristore.domain.VerificationDuration;

import javax.annotation.PostConstruct;
import javax.enterprise.context.ApplicationScoped;
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
            ServiceKey key = ServiceKey.verification(duration);
            String name = String.format("Verification PIN - %s", duration.getLabel());
            String description = String.format("Digital verification PIN valid for %s.", duration.getLabel().toLowerCase());
            BigDecimal price = new BigDecimal(20 + duration.ordinal() * 5);
            definitions.put(key, new ServiceDefinition(key, name, description, price));
        }

        for (EnrollmentType type : EnrollmentType.values()) {
            for (EnrollmentAction action : EnumSet.allOf(EnrollmentAction.class)) {
                ServiceKey key = ServiceKey.enrollment(type, action);
                String name = String.format("Enrollment PIN - %s %s", type.getLabel(), action.getLabel());
                String description = String.format("Enrollment PIN for %s - %s applicants.", type.getLabel(), action.getLabel().toLowerCase());
                BigDecimal base;
                switch (type) {
                    case CITIZEN:
                        base = new BigDecimal("40");
                        break;
                    case FOREIGNER:
                        base = new BigDecimal("55");
                        break;
                    case REFUGEE:
                    default:
                        base = new BigDecimal("30");
                        break;
                }
                BigDecimal adjustment = action == EnrollmentAction.UPDATE ? new BigDecimal("-5") : BigDecimal.ZERO;
                BigDecimal price = base.add(adjustment);
                definitions.put(key, new ServiceDefinition(key, name, description, price));
            }
        }
    }

    public List<ServiceDefinition> getVerificationServices() {
        List<ServiceDefinition> services = new ArrayList<>();
        for (VerificationDuration duration : VerificationDuration.values()) {
            services.add(definitions.get(ServiceKey.verification(duration)));
        }
        return Collections.unmodifiableList(services);
    }

    public List<ServiceDefinition> getEnrollmentServices() {
        List<ServiceDefinition> services = new ArrayList<>();
        for (EnrollmentType type : EnrollmentType.values()) {
            for (EnrollmentAction action : EnumSet.allOf(EnrollmentAction.class)) {
                services.add(definitions.get(ServiceKey.enrollment(type, action)));
            }
        }
        return Collections.unmodifiableList(services);
    }

    public ServiceDefinition getDefinition(ServiceKey key) {
        return Optional.ofNullable(definitions.get(key))
            .orElseThrow(() -> new IllegalArgumentException("Service not found: " + key));
    }
}
