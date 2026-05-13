package com.swd301.foodmarket.service;

import com.swd301.foodmarket.dto.request.ShipperLocationRequest;
import com.swd301.foodmarket.dto.request.UpdateOrderStatusRequest;
import com.swd301.foodmarket.dto.response.AvailableOrderResponse;
import com.swd301.foodmarket.dto.response.ShipperLocationResponse;
import com.swd301.foodmarket.dto.response.ShipperOrderResponse;

import java.util.List;

public interface ShipperService {
    // Xem top 6 đơn gần nhất (có GPS shipper + Goong distance)
    List<AvailableOrderResponse> getAvailableOrdersNearby(Double shipperLat, Double shipperLng);

    // Nhận đơn
    ShipperOrderResponse acceptOrder(Integer orderId);

    // Cập nhật trạng thái đơn
    ShipperOrderResponse updateOrderStatus(Integer orderId, UpdateOrderStatusRequest request);

    // Đơn của tôi
    List<ShipperOrderResponse> getMyOrders();

    // WebSocket: cập nhật vị trí GPS
    ShipperLocationResponse updateLocation(ShipperLocationRequest request);

    // REST fallback: lấy vị trí shipper theo orderId
    ShipperLocationResponse getShipperLocationByOrder(Integer orderId);

    ShipperLocationResponse updateLocationByEmail(String email, ShipperLocationRequest request);

}