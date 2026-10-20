package com.vidhika.user_service.exception;

public class GitHubNotConnectedException extends RuntimeException {
    public GitHubNotConnectedException(String message) {
        super(message);
    }
}
