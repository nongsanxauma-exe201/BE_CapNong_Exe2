package com.swd301.foodmarket.service;

import com.swd301.foodmarket.dto.request.OrderCreationRequest;
import com.swd301.foodmarket.dto.request.OrderUpdateRequest;
import com.swd301.foodmarket.dto.response.*;
import com.swd301.foodmarket.enums.OrderStatus;
import jakarta.mail.Multipart;
import org.springframework.data.domain.Page;
import org.springframework.web.multipart.MultipartFile;

import java.math.BigDecimal;
import java.util.List;

public interface OrderService {
    OrderResponse createOrder(OrderCreationRequest request);
    List<OrderResponse> getAllOrders();
    PageResponse<OrderResponse> getAllOrdersPaged(int page, int size);
    OrderResponse updateOrder(Integer id, OrderUpdateRequest request, List<MultipartFile> files);
    List<OrderResponse> getOrdersByUserId(Integer userId);
    PageResponse<OrderResponse> getOrdersByUserIdPaged(Integer userId, int page, int size);
    OrderResponse getOrderById(Integer id);
    void deleteOrder(Integer id);

    BigDecimal calculateShippingFee(String shippingAddress, Integer shopId);

    /** Tính phí ship khi đã biết placeId — bỏ qua geocoding, dùng Place Detail trực tiếp. */
    BigDecimal calculateShippingFeeByPlaceId(String placeId, Integer shopId);
    Page<OrderResponse> getOrdersForStaff(int page, int size, OrderStatus status);
    StaffOrderReportResponse generateStaffOrderReport();
    OrderShopReport  orderShopReport(String type);
    OrderResponseReport orderReport();
}