package com.swd301.foodmarket.service.impl;

import com.swd301.foodmarket.dto.response.AvailableShipperResponse;
import com.swd301.foodmarket.dto.response.OrderResponse;
import com.swd301.foodmarket.entity.Order;
import com.swd301.foodmarket.entity.ShipperLocation;
import com.swd301.foodmarket.entity.User;
import com.swd301.foodmarket.enums.OrderStatus;
import com.swd301.foodmarket.enums.RoleName;
import com.swd301.foodmarket.enums.UserStatus;
import com.swd301.foodmarket.exception.AppException;
import com.swd301.foodmarket.exception.ErrorCode;
import com.swd301.foodmarket.mapper.OrderMapper;
import com.swd301.foodmarket.repository.OrderRepository;
import com.swd301.foodmarket.repository.ShipperLocationRepository;
import com.swd301.foodmarket.repository.UserRepository;
import com.swd301.foodmarket.service.StaffService;
import lombok.AccessLevel;
import lombok.RequiredArgsConstructor;
import lombok.experimental.FieldDefaults;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

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

        User shipper = userRepository.findById(shipperId)
                .orElseThrow(() -> new AppException(ErrorCode.USER_NOT_EXISTED));

        if (shipper.getRole().getName() != RoleName.SHIPPER) {
            throw new AppException(ErrorCode.MISS_ROLE);
        }

        order.setShipper(shipper);
        order.setStatus(OrderStatus.SHIPPING); // gán xong → SHIPPING luôn
        orderRepository.save(order);

        log.info("[STAFF] Assigned shipper {} to order {} → SHIPPING", shipperId, orderId);
        return orderMapper.toResponse(order);
    }
}
