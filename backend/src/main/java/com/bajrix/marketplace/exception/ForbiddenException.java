package com.bajrix.marketplace.exception;

/** Thrown when an authenticated seller tries to act on data they do not own. */
public class ForbiddenException extends RuntimeException {
    public ForbiddenException(String message) {
        super(message);
    }
}
