package com.theplutushome.veristore.domain;

import java.io.Serializable;

public enum EnrollmentType implements Serializable {
    CITIZEN,
    FOREIGNER,
    REFUGEE;

    public String getLabel() {
        switch (this) {
            case CITIZEN:
                return "Citizen";
            case FOREIGNER:
                return "Foreigner";
            case REFUGEE:
            default:
                return "Refugee";
        }
    }
}
