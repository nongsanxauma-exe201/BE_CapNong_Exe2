package com.swd301.foodmarket.dto.response;
import lombok.*;

import java.math.BigDecimal;
import java.util.List;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class AiCartPreviewResponse {
    private List<AiCartItemResponse> items;
    private BigDecimal totalAmount;
    private String note;
}