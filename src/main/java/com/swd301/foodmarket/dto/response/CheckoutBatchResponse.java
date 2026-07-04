package com.swd301.foodmarket.dto.response;

import com.fasterxml.jackson.annotation.JsonInclude;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.util.List;

@Data
@Builder
@AllArgsConstructor
@NoArgsConstructor
@JsonInclude(JsonInclude.Include.NON_NULL)
public class CheckoutBatchResponse {
    Integer batchId;
    List<OrderResponse> orders;
    List<Integer> orderIds;
    BigDecimal totalAmount;
    String paymentMethod;
    String checkoutUrl;
    String qrCodeUrl;
    String payosOrderCode;
}
