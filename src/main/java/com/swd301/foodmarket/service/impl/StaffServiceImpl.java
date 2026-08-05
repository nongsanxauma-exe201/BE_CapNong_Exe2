package com.swd301.foodmarket.service.impl;

import com.swd301.foodmarket.dto.response.AvailableShipperResponse;
import com.swd301.foodmarket.dto.response.OrderResponse;
import com.swd301.foodmarket.entity.Order;
import com.swd301.foodmarket.entity.ShipperLocation;
import com.swd301.foodmarket.entity.User;
import com.swd301.foodmarket.enums.CodPaymentOption;
import com.swd301.foodmarket.enums.CodPaymentStatus;
import com.swd301.foodmarket.enums.DeliveryType;
import com.swd301.foodmarket.enums.OrderStatus;
import com.swd301.foodmarket.enums.RoleName;
import com.swd301.foodmarket.enums.UserStatus;
import com.swd301.foodmarket.exception.AppException;
import com.swd301.foodmarket.exception.ErrorCode;
import com.swd301.foodmarket.mapper.OrderMapper;
import com.swd301.foodmarket.repository.OrderRepository;
import com.swd301.foodmarket.repository.ShipperLocationRepository;
import com.swd301.foodmarket.repository.UserRepository;
import com.swd301.foodmarket.service.OrderRewardService;
import com.swd301.foodmarket.service.ShipperService;
import com.swd301.foodmarket.service.StaffService;
import com.swd301.foodmarket.service.TrackingService;
import com.swd301.foodmarket.service.VoucherService;
import lombok.AccessLevel;
import lombok.RequiredArgsConstructor;
import lombok.experimental.FieldDefaults;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.util.List;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
@FieldDefaults(level = AccessLevel.PRIVATE, makeFinal = true)
@Slf4j
public class StaffServiceImpl implements StaffService {

    ShipperLocationRepository shipperLocationRepository;
    OrderRepository orderRepository;
    UserRepository userRepository;
    OrderMapper orderMapper;
    ShipperService shipperService;
    OrderRewardService orderRewardService;
    VoucherService voucherService;
    TrackingService trackingService;

    /**
     * Lấy danh sách shipper đang online.
     * "Online" = có bản ghi trong bảng shipper_locations (đã gửi GPS lên hệ thống).
     */
    @Override
    public List<AvailableShipperResponse> getAvailableShippers() {
        // Chỉ lấy shipper có status ACTIVE
        List<ShipperLocation> locations = shipperLocationRepository.findByShipper_Status(UserStatus.ACTIVE);

        return locations.stream()
                .map(loc -> {
                    User shipper = loc.getShipper();

                    // Lấy đơn đang SHIPPING thực tế từ DB thay vì dùng loc.getCurrentOrder()
                    Order activeOrder = orderRepository
                            .findFirstByShipperIdAndStatus(shipper.getId(), OrderStatus.SHIPPING)
                            .orElse(null);

                    return AvailableShipperResponse.builder()
                            .id(shipper.getId())
                            .fullName(shipper.getFullName())
                            .phoneNumber(shipper.getPhoneNumber())
                            .vehicleNumber(shipper.getVehicleNumber())
                            .logoUrl(shipper.getLogoUrl())
                            .ratingAverage(shipper.getRatingAverage())
                            .latitude(loc.getLatitude())
                            .longitude(loc.getLongitude())
                            .currentOrderId(activeOrder != null ? activeOrder.getId() : null)
                            .build();
                })
                .collect(Collectors.toList());
    }

    /**
     * Staff assign shipper cho đơn hàng đã đóng gói xong (READY).
     * Sau khi assign:
     *   - order.shipper = shipper được chọn
     *   - order.status  = SHIPPING (để shipper thấy trong đơn đang giao)
     *
     * Nếu đơn chưa ở trạng thái READY → báo lỗi.
     */
    @Override
    @Transactional
    public OrderResponse assignShipperToOrder(Integer orderId, Integer shipperId) {
        Order order = orderRepository.findById(orderId)
                .orElseThrow(() -> new AppException(ErrorCode.ORDER_NOT_FOUND));

        if (order.getStatus() != OrderStatus.READY) {
            throw new AppException(ErrorCode.INVALID_ORDER_STATUS);
        }
        if (order.getDeliveryType() == DeliveryType.PICKUP_AT_WAREHOUSE) {
            throw new AppException(ErrorCode.INVALID_ORDER_STATUS);
        }

        User shipper = userRepository.findById(shipperId)
                .orElseThrow(() -> new AppException(ErrorCode.USER_NOT_EXISTED));

        if (shipper.getRole().getName() != RoleName.SHIPPER) {
            throw new AppException(ErrorCode.MISS_ROLE);
        }

        order.setShipper(shipper);
        order.setStatus(OrderStatus.SHIPPING);

        if ("COD".equalsIgnoreCase(order.getPaymentMethod())) {
            order.setCodPaymentStatus(CodPaymentStatus.PAID);
            order.setCodPaymentOption(CodPaymentOption.CASH);
            order.setCodPrepaidAmount(BigDecimal.ZERO);
        }

        orderRepository.save(order);

        log.info("[STAFF] Assigned shipper {} to order {} → SHIPPING", shipperId, orderId);
        return orderMapper.toResponse(order);
    }

    /**
     * Staff xác nhận buyer đã đến kho nhận hàng (đơn deliveryType=PICKUP_AT_WAREHOUSE).
     * Không có shipper trong luồng này — staff đóng vai trò tương đương shipper bấm "Đã giao".
     * READY → DELIVERED, tái sử dụng đúng các hiệu ứng phụ (payout, thưởng điểm, voucher...)
     * mà luồng shipper DELIVERED đang có, để mọi báo cáo/thống kê dựa trên DELIVERED hoạt động đúng.
     */
    @Override
    @Transactional
    public OrderResponse confirmWarehousePickupDelivered(Integer orderId) {
        Order order = orderRepository.findById(orderId)
                .orElseThrow(() -> new AppException(ErrorCode.ORDER_NOT_FOUND));

        if (order.getDeliveryType() != DeliveryType.PICKUP_AT_WAREHOUSE) {
            throw new AppException(ErrorCode.INVALID_ORDER_STATUS);
        }
        if (order.getStatus() != OrderStatus.READY) {
            throw new AppException(ErrorCode.INVALID_ORDER_STATUS);
        }

        if ("COD".equalsIgnoreCase(order.getPaymentMethod())) {
            order.setCodPaymentStatus(CodPaymentStatus.PAID);
            order.setCodPaymentOption(CodPaymentOption.CASH);
            order.setCodPrepaidAmount(BigDecimal.ZERO);
        }

        order.setStatus(OrderStatus.DELIVERED);
        orderRepository.save(order);

        if ("PAYOS".equalsIgnoreCase(order.getPaymentMethod())) {
            shipperService.settlePayosPickupPayout(order);
        }
        if ("COD".equalsIgnoreCase(order.getPaymentMethod())) {
            shipperService.settleCodPickupDelivered(order);
        }
        orderRewardService.applyRewardIfEligible(order);
        voucherService.grantDamageVoucherForDeliveredOrder(order);
        trackingService.trackingOrder(order, List.of());

        log.info("[STAFF] Order {} (nhận tại kho) → DELIVERED", orderId);
        return orderMapper.toResponse(order);
    }

    /**
     * Staff đánh dấu buyer không đến lấy hàng tại kho (đơn deliveryType=PICKUP_AT_WAREHOUSE).
     * READY → FAILED, tái sử dụng hiệu ứng phụ giống luồng shipper FAILED
     * (hoàn tiền PayOS nếu đã thanh toán, hoàn voucher, khóa tài khoản sau 3 lần thất bại...).
     */
    @Override
    @Transactional
    public OrderResponse markWarehousePickupFailed(Integer orderId) {
        Order order = orderRepository.findById(orderId)
                .orElseThrow(() -> new AppException(ErrorCode.ORDER_NOT_FOUND));

        if (order.getDeliveryType() != DeliveryType.PICKUP_AT_WAREHOUSE) {
            throw new AppException(ErrorCode.INVALID_ORDER_STATUS);
        }
        if (order.getStatus() != OrderStatus.READY) {
            throw new AppException(ErrorCode.INVALID_ORDER_STATUS);
        }

        OrderStatus previousStatus = order.getStatus();
        order.setStatus(OrderStatus.FAILED);
        orderRepository.save(order);
        trackingService.trackingOrder(order, List.of());

        shipperService.settlePickupOrderFailed(order, previousStatus);

        log.info("[STAFF] Order {} (nhận tại kho) → FAILED (buyer không đến lấy)", orderId);
        return orderMapper.toResponse(order);
    }
}
