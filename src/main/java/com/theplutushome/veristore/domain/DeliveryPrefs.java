package com.theplutushome.veristore.domain;

import java.io.Serializable;

public record DeliveryPrefs(boolean byEmail, boolean bySms) implements Serializable {
}
