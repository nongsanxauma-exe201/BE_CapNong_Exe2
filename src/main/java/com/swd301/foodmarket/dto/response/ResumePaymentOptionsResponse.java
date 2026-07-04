package com.swd301.foodmarket.dto.response;

import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonProperty;
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
    @JsonProperty("isBatch")
    boolean isBatch;
    Integer batchId;
    @JsonProperty("hasPlatformVoucher")
    boolean hasPlatformVoucher;
    Integer triggerOrderId;
    List<ResumePaymentOrderOption> unpaidOrders;
    PayAllOption payAllOption;
    PaymentResponse singleOrderPayment;
}
