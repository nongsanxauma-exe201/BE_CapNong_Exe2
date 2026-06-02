package com.swd301.foodmarket.dto.response;

import lombok.Builder;
import lombok.Data;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;
@Data
@Builder
public class OrderShopReport {

    private BigDecimal totalRevenue;
    private Long totalOrder;
    private Double averagePrice;
    private Double averageReview;

    private List<ReportOrderDay> reportOrderWeek;
    private List<TopSold> topSold;

    @Data
    @Builder
    public static class ReportOrderDay {
        private String label; // T2, T3, Tuần 1, Th5...
        private LocalDate fromDate;
        private LocalDate toDate;

        private Long completed;
        private Long processing;
        private Long cancelled;
        private Long total;
    }

    @Data
    @Builder
    public static class TopSold {
        private Integer productId;
        private String productName;
        private Long sold;
        private BigDecimal price;
    }

}