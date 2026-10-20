package com.vidhika.user_service.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class GithubRepoItem {
    private String repositoryId;
    private String name;
    private String nameWithOwner;
    private String url;
    private String owner;
}
