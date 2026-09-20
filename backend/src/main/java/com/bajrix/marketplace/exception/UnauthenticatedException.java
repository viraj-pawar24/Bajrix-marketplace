package com.bajrix.marketplace.exception;

/** Thrown when a seller-scoped endpoint is hit without a valid seller identity. */
public class UnauthenticatedException extends RuntimeException {
    public UnauthenticatedException(String message) {
        super(message);
    }
}
