package com.vidhika.post_service.exception;

public class AlreadyLikedException extends RuntimeException {
    public AlreadyLikedException() {
        super("You have already liked this post");
    }
}
