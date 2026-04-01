package com.ticketing.api.exception;

public class NotAdmittedException extends RuntimeException {
    public NotAdmittedException(String message) {
        super(message);
    }
}
