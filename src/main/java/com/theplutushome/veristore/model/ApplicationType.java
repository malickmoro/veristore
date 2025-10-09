package com.theplutushome.veristore.model;

import java.io.Serializable;

public enum ApplicationType implements Serializable {
    FIRST_ISSUANCE,
    RENEWAL,
    REPLACEMENT,
    UPDATE,
    VERIFICATION
}
