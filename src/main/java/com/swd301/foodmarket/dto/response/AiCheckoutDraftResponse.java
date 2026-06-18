package com.swd301.foodmarket.dto.response;
import lombok.*;

import java.math.BigDecimal;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class AiCheckoutDraftResponse {
    private String checkoutUrl;
    private BigDecimal totalAmount;
}