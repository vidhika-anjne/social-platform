package com.vidhika.post_service.exception;

public class PostNotFoundException extends RuntimeException {

    public PostNotFoundException(String postId) {
        super("Post not found with id: " + postId);
    }
}
