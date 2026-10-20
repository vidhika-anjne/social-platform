package com.vidhika.user_service.github.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.Map;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class GraphQLRequest {
    private String query;
    private Map<String, Object> variables;
}