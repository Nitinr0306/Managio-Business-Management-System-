package com.nitin.saas.common.exception;

public class UsageLimitExceededException extends RuntimeException {
    private final String resource;
    private final Long currentUsage;
    private final Long limit;

    public UsageLimitExceededException(String message) {
        super(message);
        this.resource = null;
        this.currentUsage = null;
        this.limit = null;
    }

    public UsageLimitExceededException(String resource, Long currentUsage, Long limit) {
        super(String.format("Usage limit exceeded for %s. Current: %d, Limit: %d", resource, currentUsage, limit));
        this.resource = resource;
        this.currentUsage = currentUsage;
        this.limit = limit;
    }

    public String getResource() {
        return resource;
    }

    public Long getCurrentUsage() {
        return currentUsage;
    }

    public Long getLimit() {
        return limit;
    }
}