package com.vidhika.post_service.exception;

public class CommentNotFoundException extends RuntimeException {
    public CommentNotFoundException(String commentId) {
        super("Comment not found with id: " + commentId);
    }
}
