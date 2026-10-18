package com.vidhika.post_service.exception;

public class RegistrationNotFoundException extends RuntimeException {
    public RegistrationNotFoundException(String message) {
        super(message);
    }
}
