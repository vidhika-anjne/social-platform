package com.vidhika.post_service.exception;

public class UnauthorizedActionException extends RuntimeException {

    public UnauthorizedActionException(String message) {
        super(message);
    }
}
