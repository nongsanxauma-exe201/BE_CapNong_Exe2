package com.swd301.foodmarket.dto.response;

import lombok.Builder;
import lombok.Data;

import java.time.LocalDate;
import java.util.List;

@Builder
@Data
public class AdminUserReport {

    Long totalUsers;
    Long totalShops;
    Long totalShopPending;
    Long totalBuyers;
    Long totalActiveUsers;
    Long totalInactiveUsers;
    Long totalShopSelling;
    Long totalShopNotSelling;
    Long totalUserLock;
    List<UserReport> userReports;

    @Builder
    @Data
    public static class  UserReport {
        Long totalBuyers;
        Long totalUserLock;
        Long totalShops;
        LocalDate fromDate;
        LocalDate toDate;
       String label;
    }
}
