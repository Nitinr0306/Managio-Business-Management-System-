package com.nitin.saas.common.exception;

public enum ErrorCode {
    BUSINESS_ERROR("BUS_001", "Business error occurred"),
    VALIDATION_ERROR("VAL_001", "Validation error"),
    AUTHENTICATION_ERROR("AUTH_001", "Authentication failed"),
    AUTHORIZATION_ERROR("AUTH_002", "Authorization failed"),
    RESOURCE_NOT_FOUND("RES_001", "Resource not found"),
    DUPLICATE_RESOURCE("RES_002", "Resource already exists"),
    FEATURE_NOT_AVAILABLE("FEAT_001", "Feature not available"),
    USAGE_LIMIT_EXCEEDED("LIM_001", "Usage limit exceeded"),
    PAYMENT_ERROR("PAY_001", "Payment processing error"),
    SUBSCRIPTION_ERROR("SUB_001", "Subscription error"),
    RATE_LIMIT_EXCEEDED("RATE_001", "Rate limit exceeded"),
    INTERNAL_ERROR("INT_001", "Internal server error"),
    EXTERNAL_SERVICE_ERROR("EXT_001", "External service error");

    private final String code;
    private final String description;

    ErrorCode(String code, String description) {
        this.code = code;
        this.description = description;
    }

    public String getCode() {
        return code;
    }

    public String getDescription() {
        return description;
    }
}