package com.bajrix.marketplace.exception;

/** Thrown for domain/business-rule violations that aren't simple field validation. */
public class BusinessValidationException extends RuntimeException {
    public BusinessValidationException(String message) {
        super(message);
    }
}
