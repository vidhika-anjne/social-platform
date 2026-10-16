package com.vidhika.post_service.exception;

public class FailedUploadException extends RuntimeException {
    public FailedUploadException(String message) {
        super(message);
    }
}
