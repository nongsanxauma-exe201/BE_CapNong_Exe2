package com.swd301.foodmarket.dto.response;

import com.swd301.foodmarket.entity.ReturnRequest;
import lombok.*;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ReturnActionResponse {
    ReturnRequest request;
    String qrCodeUrl;
    String checkoutUrl;
}
