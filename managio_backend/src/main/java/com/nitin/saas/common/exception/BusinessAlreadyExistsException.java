package com.nitin.saas.common.exception;

public class BusinessAlreadyExistsException extends RuntimeException {

    public BusinessAlreadyExistsException(String message) {
        super(message);
    }
}
