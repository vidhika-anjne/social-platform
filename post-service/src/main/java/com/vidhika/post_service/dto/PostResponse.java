package com.vidhika.post_service.dto;

import com.vidhika.post_service.enums.HackathonTagType;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class PostResponse {

    private UUID id;
    private UUID userId;
    private String content;
    private List<String> mediaUrls;
    private UUID hackathonId;
    private HackathonTagType hackathonTagType;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;
}
