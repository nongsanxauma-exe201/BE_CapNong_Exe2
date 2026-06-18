package com.swd301.foodmarket.dto.response;

import lombok.*;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class AiNavigationResponse {
    private String url;
    private String label;
}
