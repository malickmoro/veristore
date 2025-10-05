package com.theplutushome.veristore.service;

import com.theplutushome.veristore.catalog.EnrollmentSku;
import com.theplutushome.veristore.catalog.ProductFamily;
import com.theplutushome.veristore.catalog.ProductKey;
import com.theplutushome.veristore.catalog.VerificationSku;

import jakarta.annotation.PostConstruct;
import jakarta.enterprise.context.ApplicationScoped;

import java.io.Serial;
import java.io.Serializable;
import java.security.SecureRandom;
import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.Deque;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

@ApplicationScoped
public class PinVault implements Serializable {

    @Serial
    private static final long serialVersionUID = 1L;

    private static final int SEED_QUANTITY = 10;
    private static final char[] ALLOWED = "ABCDEFGHJKLMNPQRSTUVWXYZ23456789".toCharArray();

    private final Map<ProductKey, Deque<String>> stock = new ConcurrentHashMap<>();
    private final SecureRandom random = new SecureRandom();

    @PostConstruct
    void init() {
        for (VerificationSku sku : VerificationSku.values()) {
            ProductKey key = new ProductKey(ProductFamily.VERIFICATION, sku.sku);
            stock.putIfAbsent(key, new ArrayDeque<>());
            ensure(key, SEED_QUANTITY);
        }
        for (EnrollmentSku sku : EnrollmentSku.values()) {
            if (!sku.active) {
                continue;
            }
            ProductKey key = new ProductKey(ProductFamily.ENROLLMENT, sku.sku);
            stock.putIfAbsent(key, new ArrayDeque<>());
            ensure(key, SEED_QUANTITY);
        }
    }

    public synchronized List<String> take(ProductKey key, int quantity) {
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

    public void ensure(ProductKey key, int minimum) {
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
