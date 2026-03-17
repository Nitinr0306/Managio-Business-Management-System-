package com.nitin.saas.common.exception;

public class FeatureNotAvailableException extends RuntimeException {
    private final String feature;
    private final String requiredPlan;

    public FeatureNotAvailableException(String message) {
        super(message);
        this.feature = null;
        this.requiredPlan = null;
    }

    public FeatureNotAvailableException(String feature, String requiredPlan) {
        super(String.format("Feature '%s' is not available. Upgrade to %s plan required.", feature, requiredPlan));
        this.feature = feature;
        this.requiredPlan = requiredPlan;
    }

    public String getFeature() {
        return feature;
    }

    public String getRequiredPlan() {
        return requiredPlan;
    }
}