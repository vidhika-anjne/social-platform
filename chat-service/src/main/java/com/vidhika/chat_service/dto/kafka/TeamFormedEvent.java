package com.vidhika.chat_service.dto.kafka;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import lombok.*;

import java.util.List;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
@JsonIgnoreProperties(ignoreUnknown = true)
public class TeamFormedEvent {

    private String eventType;
    private String teamId;
    private String hackathonId;
    private List<String> memberIds;
    private String teamName;
}
