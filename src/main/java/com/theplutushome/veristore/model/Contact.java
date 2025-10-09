package com.theplutushome.veristore.model;

import java.io.Serializable;
import java.util.Objects;

public record Contact(String email, String msisdn) implements Serializable {

    public Contact {
        Objects.requireNonNull(email, "email");
        Objects.requireNonNull(msisdn, "msisdn");
    }
}
