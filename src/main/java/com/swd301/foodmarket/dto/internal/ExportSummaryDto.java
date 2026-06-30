package com.swd301.foodmarket.dto.internal;

import lombok.Builder;
import lombok.Data;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.Map;

@Data
@Builder
public class ExportSummaryDto {
    private long totalOrders;
    private long distinctBuyers;
    private BigDecimal totalRevenue;
    private BigDecimal totalShippingFee;
    private LocalDate periodFrom;
    private LocalDate periodTo;
    private Map<String, Long> provinceBreakdown;
}
