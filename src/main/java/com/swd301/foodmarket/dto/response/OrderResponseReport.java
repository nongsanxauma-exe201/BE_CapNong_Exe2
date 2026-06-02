package com.swd301.foodmarket.dto.response;

import lombok.Builder;
import lombok.Data;

import java.math.BigDecimal;

@Builder
@Data
public class OrderResponseReport {

    BigDecimal totalRevenua;
    Long totalComplete;
    Long totalCreated;
}
