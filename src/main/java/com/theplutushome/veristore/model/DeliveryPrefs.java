package com.theplutushome.veristore.model;

import java.io.Serializable;

public record DeliveryPrefs(boolean byEmail, boolean bySms) implements Serializable {
}
