package com.theplutushome.veristore.service;

import com.theplutushome.veristore.domain.EnrollmentType;
import com.theplutushome.veristore.domain.PinCategory;
import com.theplutushome.veristore.domain.ServiceKey;

import javax.annotation.PostConstruct;
import javax.enterprise.context.ApplicationScoped;
import java.io.Serializable;
import java.security.SecureRandom;
import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.Deque;
import java.util.EnumSet;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

@ApplicationScoped
public class PinVault implements Serializable {

    private static final int SEED_QUANTITY = 10;
    private static final char[] ALLOWED = "ABCDEFGHJKLMNPQRSTUVWXYZ23456789".toCharArray();

    private final Map<ServiceKey, Deque<String>> stock = new ConcurrentHashMap<>();
    private final SecureRandom random = new SecureRandom();

    @PostConstruct
    void init() {
        stock.putIfAbsent(new ServiceKey(PinCategory.VERIFICATION, "Y1"), new ArrayDeque<>());
        stock.putIfAbsent(new ServiceKey(PinCategory.VERIFICATION, "Y2"), new ArrayDeque<>());
        stock.putIfAbsent(new ServiceKey(PinCategory.VERIFICATION, "Y3"), new ArrayDeque<>());
        for (ServiceKey key : stock.keySet()) {
            ensure(key, SEED_QUANTITY);
        }
        for (EnrollmentType type : EnumSet.allOf(EnrollmentType.class)) {
            ServiceKey key = new ServiceKey(PinCategory.ENROLLMENT, type.name());
            stock.putIfAbsent(key, new ArrayDeque<>());
            ensure(key, SEED_QUANTITY);
        }
    }

    public synchronized List<String> take(ServiceKey key, int quantity) {
        if (quantity <= 0) {
            throw new IllegalArgumentException("quantity must be positive");
        }
        Deque<String> deque = stock.computeIfAbsent(key, k -> new ArrayDeque<>());
        while (deque.size() < quantity) {
            deque.addLast(generateCode());
        }
        List<String> pins = new ArrayList<>(quantity);
        for (int i = 0; i < quantity; i++) {
            pins.add(deque.removeFirst());
        }
        return pins;
    }

    public void ensure(ServiceKey key, int minimum) {
        if (minimum <= 0) {
            return;
        }
        synchronized (this) {
            Deque<String> deque = stock.computeIfAbsent(key, k -> new ArrayDeque<>());
            while (deque.size() < minimum) {
                deque.addLast(generateCode());
            }
        }
    }

    private String generateCode() {
        char[] buffer = new char[14];
        for (int i = 0; i < buffer.length; i++) {
            buffer[i] = ALLOWED[random.nextInt(ALLOWED.length)];
        }
        return new String(buffer);
    }
}
