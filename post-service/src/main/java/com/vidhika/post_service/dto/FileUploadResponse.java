package com.vidhika.post_service.dto;

import lombok.*;

import java.util.List;
@Getter
@Setter
@AllArgsConstructor
@NoArgsConstructor
@Builder
public class FileUploadResponse
{
    private String message;
    private List<String> mediaUrls;
    private Integer count;
}
