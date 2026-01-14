package com.nitin.saas.business.exception.businessException;

public class BusinessAlreadyExistsException extends RuntimeException{
    public BusinessAlreadyExistsException(String slug){
        super("Business already exists with slug: "+slug);
    }
}
