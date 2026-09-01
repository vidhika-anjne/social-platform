package com.vidhika.user_service.dto;

import lombok.Builder;
import lombok.Data;

@Data
@Builder
public class CountResponse {

    private long count;
}
