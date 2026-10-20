package com.vidhika.user_service.exception;

public class GitHubRateLimitException extends RuntimeException {
    public GitHubRateLimitException(String message) {
        super(message);
    }
}
