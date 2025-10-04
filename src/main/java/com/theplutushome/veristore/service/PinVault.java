package com.theplutushome.veristore.service;

import com.theplutushome.veristore.domain.ServiceDefinition;
import com.theplutushome.veristore.domain.ServiceKey;
import com.theplutushome.veristore.util.Masker;

import javax.annotation.PostConstruct;
import javax.enterprise.context.ApplicationScoped;
import javax.inject.Inject;
import java.io.Serializable;
import java.security.SecureRandom;
import java.time.LocalDateTime;
import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.Deque;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.logging.Logger;

@ApplicationScoped
public class PinVault implements Serializable {

    private static final Logger LOGGER = Logger.getLogger(PinVault.class.getName());

    @Inject
    private ServiceCatalog serviceCatalog;

    private final Map<ServiceKey, Deque<String>> cache = new ConcurrentHashMap<>();
    private final SecureRandom random = new SecureRandom();

    @PostConstruct
    void init() {
        for (ServiceDefinition definition : serviceCatalog.getVerificationServices()) {
            cache.putIfAbsent(definition.getKey(), new ArrayDeque<>());
            topUp(definition.getKey(), 10);
        }
        for (ServiceDefinition definition : serviceCatalog.getEnrollmentServices()) {
            cache.putIfAbsent(definition.getKey(), new ArrayDeque<>());
            topUp(definition.getKey(), 10);
        }
    }

    public synchronized List<String> dispense(ServiceKey key, int quantity) {
        if (quantity <= 0) {
            throw new IllegalArgumentException("Quantity must be positive");
        }
        Deque<String> deque = cache.computeIfAbsent(key, k -> new ArrayDeque<>());
        if (deque.size() < quantity) {
            topUp(key, quantity - deque.size() + 5);
        }
        List<String> pins = new ArrayList<>(quantity);
        for (int i = 0; i < quantity; i++) {
            pins.add(deque.removeFirst());
        }
        LOGGER.info(() -> String.format("Dispensed %d PIN(s) for %s at %s", quantity, key.code(), LocalDateTime.now()));
        return pins;
    }

    private void topUp(ServiceKey key, int additional) {
        Deque<String> deque = cache.computeIfAbsent(key, k -> new ArrayDeque<>());
        for (int i = 0; i < additional; i++) {
            deque.addLast(generatePin(key));
        }
        LOGGER.fine(() -> String.format("Vault topped up with %d PIN(s) for %s", additional, key.code()));
    }

    private String generatePin(ServiceKey key) {
        int segment1 = random.nextInt(9000) + 1000;
        int segment2 = random.nextInt(9000) + 1000;
        return String.format("%s-%04d-%04d", key.code(), segment1, segment2);
    }

    public List<String> maskPins(List<String> pins) {
        List<String> masked = new ArrayList<>(pins.size());
        for (String pin : pins) {
            masked.add(Masker.mask(pin));
        }
        return masked;
    }
}
