package com.vidhika.post_service.dto;

import com.vidhika.post_service.enums.HackathonTagType;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import lombok.Data;
import org.springframework.web.multipart.MultipartFile;

import java.util.UUID;

@Data
public class CreatePostRequest {

    @NotBlank(message = "Content cannot be blank")
    @Size(max = 5000, message = "Content must not exceed 5000 characters")
    private String content;
    private MultipartFile[] files;
    private UUID hackathonId;
    private HackathonTagType hackathonTagType;
}
