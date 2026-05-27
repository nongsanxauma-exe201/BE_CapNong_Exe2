package com.swd301.foodmarket.dto.response;

import lombok.Builder;
import lombok.Data;

@Data
@Builder
public class StaffOrderReportResponse {

    // Đơn hàng đang được vận chuyển tới kho
    private Integer incomingOrders;

    // Đơn hàng đang kiểm tra (QUALITY_CHECKING)
    private Integer quantityChecking;

    // Đơn hàng đang chờ kiểm duyệt chất lượng (ARRIVED_AT_WAREHOUSE)
    private Integer qualityCheckingOrders;

    // Đơn hàng đã đạt chuẩn và cần đóng gói (QUALITY_CHECKED)
    private Integer packagingOrders;

    // Đơn hàng đã đóng gói xong, chờ shipper lấy (READY)
    private Integer readyOrders;

    // Số lượng nông dân cần được duyệt / xử lý
    private Integer pendingFarmerApprovals;

    // Số đơn hàng đã kiểm duyệt thành công hôm nay
    private Integer approvedTodayOrders;
}
