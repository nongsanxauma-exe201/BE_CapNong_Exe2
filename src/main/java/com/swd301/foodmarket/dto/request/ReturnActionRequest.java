package com.swd301.foodmarket.dto.request;

import lombok.*;
import lombok.experimental.FieldDefaults;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
@FieldDefaults(level = AccessLevel.PRIVATE)
public class ReturnActionRequest {
    String response; // Reason for approval or rejection
    Boolean accept; // true for approve, false for reject
    java.math.BigDecimal refundAmount; // Optional override by admin
}
