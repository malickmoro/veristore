package com.theplutushome.veristore.domain;

import java.io.Serializable;

public enum VerificationDuration implements Serializable {
    Y1,
    Y2,
    Y3;

    public String getLabel() {
        switch (this) {
            case Y1:
                return "1 Year";
            case Y2:
                return "2 Years";
            case Y3:
            default:
                return "3 Years";
        }
    }
}
