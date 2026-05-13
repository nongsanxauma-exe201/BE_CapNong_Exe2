package com.swd301.foodmarket.dto.request;

import com.swd301.foodmarket.enums.OrderStatus;
import jakarta.validation.constraints.NotNull;
import lombok.*;
import lombok.experimental.FieldDefaults;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@FieldDefaults(level = AccessLevel.PRIVATE)
public class UpdateOrderStatusRequest {

    @NotNull(message = "STATUS_REQUIRED")
    OrderStatus status;

    // Ghi chú tuỳ chọn (vd: lý do giao thất bại)
    String note;
}