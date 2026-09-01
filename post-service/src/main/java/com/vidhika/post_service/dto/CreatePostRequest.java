package com.vidhika.post_service.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import lombok.Data;

@Data
public class CreatePostRequest {

    @NotBlank(message = "Content cannot be blank")
    @Size(max = 5000, message = "Content must not exceed 5000 characters")
    private String content;

    private String mediaUrl;
}
