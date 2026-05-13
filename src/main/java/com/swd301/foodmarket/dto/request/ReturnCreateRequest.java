package com.swd301.foodmarket.dto.request;

import lombok.*;
import lombok.experimental.FieldDefaults;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
@FieldDefaults(level = AccessLevel.PRIVATE)
public class ReturnCreateRequest {
    Integer orderDetailId;
    String reason;
    String evidence; // Semicolon separated URLs
}
