package com.theplutushome.veristore.domain;

import java.io.Serializable;

public enum EnrollmentAction implements Serializable {
    FIRST_ISSUANCE,
    UPDATE;

    public String getLabel() {
        switch (this) {
            case FIRST_ISSUANCE:
                return "First Issuance";
            case UPDATE:
            default:
                return "Update";
        }
    }
}
