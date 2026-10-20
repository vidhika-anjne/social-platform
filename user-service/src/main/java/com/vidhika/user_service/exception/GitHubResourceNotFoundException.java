package com.vidhika.user_service.exception;

public class GitHubResourceNotFoundException extends RuntimeException {
    public GitHubResourceNotFoundException(String message) {
        super(message);
    }
}
