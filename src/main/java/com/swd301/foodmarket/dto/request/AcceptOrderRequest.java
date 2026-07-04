package com.swd301.foodmarket.dto.request;

import com.swd301.foodmarket.enums.CodPaymentOption;
import lombok.Data;

@Data
public class AcceptOrderRequest {
    private CodPaymentOption codPaymentOption;
}
