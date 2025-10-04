package com.theplutushome.veristore.domain;

import java.io.Serializable;
import java.util.Objects;

public final class CustomerProfile implements Serializable {

    private final String fullName;
    private final String email;
    private final String phone;

    public CustomerProfile(String fullName, String email, String phone) {
        this.fullName = Objects.requireNonNull(fullName, "fullName");
        this.email = Objects.requireNonNull(email, "email");
        this.phone = Objects.requireNonNull(phone, "phone");
    }

    public String getFullName() {
        return fullName;
    }

    public String getEmail() {
        return email;
    }

    public String getPhone() {
        return phone;
    }
}
