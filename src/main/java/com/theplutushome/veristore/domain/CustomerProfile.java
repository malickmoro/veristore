package com.theplutushome.veristore.domain;

import java.io.Serializable;
import java.util.Objects;

public final class CustomerProfile implements Serializable {

    private final String fullName;
    private final Contact contact;

    public CustomerProfile(String fullName, String email, String phone) {
        this(fullName, new Contact(email, phone));
    }

    public CustomerProfile(String fullName, Contact contact) {
        this.fullName = Objects.requireNonNull(fullName, "fullName");
        this.contact = Objects.requireNonNull(contact, "contact");
    }

    public String getFullName() {
        return fullName;
    }

    public Contact getContact() {
        return contact;
    }

    public String getEmail() {
        return contact.email();
    }

    public String getPhone() {
        return contact.msisdn();
    }
}
