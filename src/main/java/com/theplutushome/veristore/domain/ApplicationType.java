package com.theplutushome.veristore.domain;

import java.io.Serializable;

public enum ApplicationType implements Serializable {
    FIRST_ISSUANCE,
    RENEWAL,
    REPLACEMENT,
    UPDATE,
    VERIFICATION
}
