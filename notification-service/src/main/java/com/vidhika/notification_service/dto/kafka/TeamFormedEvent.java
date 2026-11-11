package com.vidhika.notification_service.dto.kafka;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@JsonIgnoreProperties(ignoreUnknown = true)
public class TeamFormedEvent {

    private String eventType;
    private String teamId;
    private String hackathonId;
    private String hackathonTitle;
    private String teamName;
    private List<String> memberIds;
}

