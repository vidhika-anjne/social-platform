package com.vidhika.user_service.github.dto;

import com.vidhika.user_service.dto.GithubRepoItem;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ContributionRepositoriesPage {
    private List<GithubRepoItem> repositories;
    private String endCursor;
    private boolean hasNextPage;
    private int totalCount;
}
