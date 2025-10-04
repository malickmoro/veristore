package com.theplutushome.veristore.domain;

import java.io.Serializable;
import java.util.Objects;

public final class ServiceKey implements Serializable {

    private final ServiceCategory category;
    private final VerificationDuration verificationDuration;
    private final EnrollmentType enrollmentType;
    private final EnrollmentAction enrollmentAction;
    private final String code;

    private ServiceKey(ServiceCategory category,
                       VerificationDuration verificationDuration,
                       EnrollmentType enrollmentType,
                       EnrollmentAction enrollmentAction) {
        this.category = Objects.requireNonNull(category, "category");
        this.verificationDuration = verificationDuration;
        this.enrollmentType = enrollmentType;
        this.enrollmentAction = enrollmentAction;
        this.code = buildCode();
    }

    public static ServiceKey verification(VerificationDuration duration) {
        return new ServiceKey(ServiceCategory.VERIFICATION, Objects.requireNonNull(duration, "duration"), null, null);
    }

    public static ServiceKey enrollment(EnrollmentType type, EnrollmentAction action) {
        return new ServiceKey(ServiceCategory.ENROLLMENT, null, Objects.requireNonNull(type, "type"), Objects.requireNonNull(action, "action"));
    }

    private String buildCode() {
        switch (category) {
            case VERIFICATION:
                return "VER-" + verificationDuration.name();
            case ENROLLMENT:
            default:
                return String.format("ENR-%s-%s", enrollmentType.name(), enrollmentAction.name());
        }
    }

    public ServiceCategory getCategory() {
        return category;
    }

    public VerificationDuration getVerificationDuration() {
        return verificationDuration;
    }

    public EnrollmentType getEnrollmentType() {
        return enrollmentType;
    }

    public EnrollmentAction getEnrollmentAction() {
        return enrollmentAction;
    }

    public String getCode() {
        return code;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) {
            return true;
        }
        if (!(o instanceof ServiceKey)) {
            return false;
        }
        ServiceKey that = (ServiceKey) o;
        return category == that.category
            && verificationDuration == that.verificationDuration
            && enrollmentType == that.enrollmentType
            && enrollmentAction == that.enrollmentAction;
    }

    @Override
    public int hashCode() {
        return Objects.hash(category, verificationDuration, enrollmentType, enrollmentAction);
    }

    @Override
    public String toString() {
        return code;
    }
}
