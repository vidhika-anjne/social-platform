package com.vidhika.user_service.exception;

public class GitHubAuthenticationException extends RuntimeException {
    public GitHubAuthenticationException(String message) {
        super(message);
    }
}