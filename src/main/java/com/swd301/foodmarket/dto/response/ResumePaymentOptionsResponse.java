package com.swd301.foodmarket.dto.response;

import com.fasterxml.jackson.annotation.JsonInclude;
import lombok.*;
import lombok.experimental.FieldDefaults;

import java.util.List;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
@FieldDefaults(level = AccessLevel.PRIVATE)
@JsonInclude(JsonInclude.Include.NON_NULL)
public class ResumePaymentOptionsResponse {
    boolean isBatch;
    Integer batchId;
    Integer triggerOrderId;
    List<ResumePaymentOrderOption> unpaidOrders;
    PayAllOption payAllOption;
    PaymentResponse singleOrderPayment;
}
