package com.theplutushome.veristore.domain;

public enum EnrollmentType {
    CITIZEN_FIRST_ISSUANCE("Citizen", "First Issuance"),
    CITIZEN_UPDATE("Citizen", "Update"),
    FOREIGNER_FIRST_ISSUANCE("Foreigner", "First Issuance"),
    FOREIGNER_UPDATE("Foreigner", "Update"),
    REFUGEE_FIRST_ISSUANCE("Refugee", "First Issuance"),
    REFUGEE_UPDATE("Refugee", "Update");

    private final String groupLabel;
    private final String actionLabel;

    EnrollmentType(String groupLabel, String actionLabel) {
        this.groupLabel = groupLabel;
        this.actionLabel = actionLabel;
    }

    public String getGroupLabel() {
        return groupLabel;
    }

    public String getActionLabel() {
        return actionLabel;
    }

    public String getLabel() {
        return groupLabel + " - " + actionLabel;
    }

    public boolean isUpdate() {
        return "Update".equals(actionLabel);
    }
}
