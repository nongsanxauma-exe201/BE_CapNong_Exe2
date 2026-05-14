package com.swd301.foodmarket.dto.response;

import lombok.Builder;
import lombok.Data;

@Data
@Builder
public class StaffOrderReportResponse {

    // Đơn hàng đang được vận chuyển tới kho
    private Integer incomingOrders;

    // Đơn hàng đã reject kiểm duyệt chất lượng
    private Integer quantityCheckedReject;

    // Đơn hàng đang chờ kiểm duyệt chất lượng
    private Integer qualityCheckingOrders;

    // Đơn hàng đã đạt chuẩn và cần đóng gói
    private Integer packagingOrders;

    // Số lượng nông dân cần được duyệt / xử lý
    private Integer pendingFarmerApprovals;

    // Số đơn hàng đã kiểm duyệt thành công hôm nay
    private Integer approvedTodayOrders;
}