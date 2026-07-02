package com.swd301.foodmarket.dto.response;

import lombok.*;
import lombok.experimental.FieldDefaults;

import java.math.BigDecimal;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
@FieldDefaults(level = AccessLevel.PRIVATE)
public class PayAllOption {
    BigDecimal amount;
    String checkoutUrl;
    String payosOrderCode;
    boolean canReuseExistingLink;
}
