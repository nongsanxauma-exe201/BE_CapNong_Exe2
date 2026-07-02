package com.swd301.foodmarket.dto.response;

import com.swd301.foodmarket.enums.PaymentStatus;
import lombok.*;
import lombok.experimental.FieldDefaults;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
@FieldDefaults(level = AccessLevel.PRIVATE)
public class PaymentResponse {

    Integer id;
    Integer orderId;
    Integer withdrawRequestId;

    BigDecimal amount;
    String paymentGateway;
    PaymentStatus status;

    LocalDateTime paymentDate;
    LocalDateTime createdAt;
    LocalDateTime updatedAt;

    String payosOrderCode;
    String checkoutUrl;
    String qrCodeUrl;

    Integer batchId;
    List<Integer> orderIds;
    String paymentScope;
}