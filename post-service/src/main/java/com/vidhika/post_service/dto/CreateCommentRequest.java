package com.vidhika.post_service.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import lombok.Data;

@Data
public class CreateCommentRequest {

    @NotBlank(message = "Comment content cannot be blank")
    @Size(max = 2000, message = "Comment must not exceed 2000 characters")
    private String content;
}
