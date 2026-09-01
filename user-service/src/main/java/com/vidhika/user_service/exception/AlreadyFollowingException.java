package com.vidhika.user_service.exception;

public class AlreadyFollowingException extends RuntimeException {
    public AlreadyFollowingException() {
        super("You are already following this user");
    }
}
