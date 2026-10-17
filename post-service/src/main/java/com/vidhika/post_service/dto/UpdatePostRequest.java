package com.vidhika.post_service.dto;

import jakarta.validation.constraints.Size;
import lombok.Data;
import org.springframework.web.multipart.MultipartFile;

import java.util.List;

@Data
public class UpdatePostRequest {

    @Size(max = 5000, message = "Content must not exceed 5000 characters")
    private String content;
    private List<String> mediaUrls;
}
