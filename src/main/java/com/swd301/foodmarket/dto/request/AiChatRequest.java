package com.swd301.foodmarket.dto.request;

import lombok.*;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class AiChatRequest {
    private String message;
    /** Dùng để nhớ danh sách sản phẩm đang chờ chọn (guest hoặc logged-in) */
    private String sessionId;
}