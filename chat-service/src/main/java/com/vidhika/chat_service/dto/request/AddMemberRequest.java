package com.vidhika.chat_service.dto.request;

import lombok.*;

import java.util.List;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class AddMemberRequest {

    private String userId;

    private List<String> userIds;
}
