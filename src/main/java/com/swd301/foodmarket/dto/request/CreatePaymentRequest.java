package com.swd301.foodmarket.dto.request;

import jakarta.validation.constraints.NotNull;
import lombok.*;
import lombok.experimental.FieldDefaults;

import java.math.BigDecimal;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
@FieldDefaults(level = AccessLevel.PRIVATE)
public class CreatePaymentRequest {

    @NotNull
    Integer orderId;

    BigDecimal amount;

    String paymentGateway; // PAYOS

    String method; // WALLET, PAYOS
}