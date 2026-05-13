package com.swd301.foodmarket.service.impl;

import com.swd301.foodmarket.dto.goong.LatLong;
import com.swd301.foodmarket.dto.request.ShipperLocationRequest;
import com.swd301.foodmarket.dto.request.UpdateOrderStatusRequest;
import com.swd301.foodmarket.dto.response.AvailableOrderResponse;
import com.swd301.foodmarket.dto.response.ShipperLocationResponse;
import com.swd301.foodmarket.dto.response.ShipperOrderResponse;
import com.swd301.foodmarket.entity.Order;
import com.swd301.foodmarket.entity.ShipperLocation;
import com.swd301.foodmarket.entity.User;
import com.swd301.foodmarket.entity.Wallet;
import com.swd301.foodmarket.enums.OrderStatus;
import com.swd301.foodmarket.enums.RoleName;
import com.swd301.foodmarket.enums.WalletType;
import com.swd301.foodmarket.exception.AppException;
import com.swd301.foodmarket.exception.ErrorCode;
import com.swd301.foodmarket.repository.OrderRepository;
import com.swd301.foodmarket.repository.ShipperLocationRepository;
import com.swd301.foodmarket.repository.UserRepository;
import com.swd301.foodmarket.repository.WalletRepository;
import com.swd301.foodmarket.service.GoongMapService;
import com.swd301.foodmarket.service.ShipperService;
import com.swd301.foodmarket.service.UserService;
import lombok.AccessLevel;
import lombok.RequiredArgsConstructor;
import lombok.experimental.FieldDefaults;
import lombok.extern.slf4j.Slf4j;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.Comparator;
import java.util.List;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
@FieldDefaults(level = AccessLevel.PRIVATE, makeFinal = true)
@Slf4j
public class ShipperServiceImpl implements ShipperService {

    OrderRepository orderRepository;
    ShipperLocationRepository shipperLocationRepository;
    UserRepository userRepository;
    GoongMapService goongMapService;
    WalletRepository walletRepository;
    UserService userService;
    // ===================== HELPERS =====================

    private User getCurrentShipper() {
        String email = SecurityContextHolder.getContext().getAuthentication().getName();
        return getShipperByEmail(email);
    }

    private User getShipperByEmail(String email) {
        User user = userRepository.findByEmail(email)
                .orElseThrow(() -> new AppException(ErrorCode.USER_NOT_EXISTED));
        if (user.getRole().getName() != RoleName.SHIPPER) {
            throw new AppException(ErrorCode.MISS_ROLE);
        }
        return user;
    }

    /**
     * Geocode địa chỉ nhà buyer → cache vào order.shippingLatitude/shippingLongitude
     */
    private LatLong getOrGeocodeShippingLocation(Order order) {
        if (order.getShippingLatitude() != null && order.getShippingLongitude() != null) {
            return new LatLong(order.getShippingLatitude(), order.getShippingLongitude());
        }
        log.info("[Goong] Geocode shippingAddress order {}: '{}'", order.getId(), order.getShippingAddress());
        LatLong latLong = goongMapService.geocodeAddress(order.getShippingAddress());
        if (latLong != null) {
            order.setShippingLatitude(latLong.getLatitude());
            order.setShippingLongitude(latLong.getLongitude());
            orderRepository.save(order);
        }
        return latLong;
    }

    /**
     * ✅ MỚI: Geocode địa chỉ shop → cache vào order.shopLatitude/shopLongitude
     * Đây là điểm shipper cần tới TRƯỚC để lấy hàng
     */
    private LatLong getOrGeocodeShopLocation(Order order) {
        if (order.getShopLatitude() != null && order.getShopLongitude() != null) {
            return new LatLong(order.getShopLatitude(), order.getShopLongitude());
        }
        String shopAddress = order.getShopOwner().getAddress();
        log.info("[Goong] Geocode shopAddress order {}: '{}'", order.getId(), shopAddress);
        LatLong latLong = goongMapService.geocodeAddress(shopAddress);
        if (latLong != null) {
            order.setShopLatitude(latLong.getLatitude());
            order.setShopLongitude(latLong.getLongitude());
            orderRepository.save(order);
        }
        return latLong;
    }

    private ShipperOrderResponse toShipperOrderResponse(Order order) {
        return ShipperOrderResponse.builder()
                .orderId(order.getId())
                .buyerName(order.getBuyer().getFullName())
                .buyerPhone(order.getBuyer().getPhoneNumber())
                .shippingAddress(order.getShippingAddress())
                .recipientName(order.getRecipientName())
                .recipientPhone(order.getRecipientPhone())
                .shippingFee(order.getShippingFee() != null ? order.getShippingFee().multiply(BigDecimal.valueOf(0.9)) : BigDecimal.ZERO)
                .status(order.getStatus())
                .createdAt(order.getCreatedAt())
                .estimatedDeliveryDate(order.getEstimatedDeliveryDate())
                .note(order.getNote())
                // ✅ Trả về tọa độ để FakeGPS dùng đúng điểm thật
                .shopLatitude(order.getShopLatitude())
                .shopLongitude(order.getShopLongitude())
                .shippingLatitude(order.getShippingLatitude())
                .shippingLongitude(order.getShippingLongitude())
                .build();
    }

    // ===================== REST =====================

    /**
     * ✅ LOGIC ĐÚNG NGHIỆP VỤ:
     *
     * Trước (sai): sort theo khoảng cách shipper → nhà buyer
     * Sau  (đúng): sort theo khoảng cách shipper → shop
     *              vì shipper phải tới shop lấy hàng TRƯỚC
     *
     * Response trả về 2 khoảng cách:
     *   - shipToShopKm:   shipper → shop   (để shipper quyết định nhận đơn)
     *   - shopToBuyerKm:  shop → nhà buyer (để shipper biết tổng quãng đường giao)
     */
    @Override
    @Transactional
    public List<AvailableOrderResponse> getAvailableOrdersNearby(Double shipperLat, Double shipperLng) {
        getCurrentShipper();

        LatLong shipperLocation = new LatLong(shipperLat, shipperLng);
        List<Order> availableOrders = orderRepository.findByShipperIsNullAndStatus(OrderStatus.CONFIRMED);

        log.info("[SHIPPER] Finding orders near ({}, {}), total CONFIRMED: {}",
                shipperLat, shipperLng, availableOrders.size());

        return availableOrders.stream()
                .map(order -> {
                    // ✅ Geocode shop (điểm lấy hàng)
                    LatLong shopLocation = getOrGeocodeShopLocation(order);

                    // ✅ Geocode nhà buyer (điểm giao hàng)
                    LatLong buyerLocation = getOrGeocodeShippingLocation(order);

                    // ✅ Khoảng cách shipper → shop (dùng để sort, quan trọng nhất)
                    Double shipToShopKm = null;
                    if (shopLocation != null) {
                        double d = goongMapService.getDistanceKm(shipperLocation, shopLocation);
                        shipToShopKm = d == Double.MAX_VALUE ? null : d;
                    }

                    // ✅ Khoảng cách shop → nhà buyer (shipper biết tổng quãng đường)
                    Double shopToBuyerKm = null;
                    if (shopLocation != null && buyerLocation != null) {
                        double d = goongMapService.getDistanceKm(shopLocation, buyerLocation);
                        shopToBuyerKm = d == Double.MAX_VALUE ? null : d;
                    }

                    return AvailableOrderResponse.builder()
                            .orderId(order.getId())
                            .shopName(order.getShopOwner().getShopName())
                            .shopAddress(order.getShopOwner().getAddress())
                            .shopPhone(order.getShopOwner().getPhoneNumber())
                            .shippingAddress(order.getShippingAddress())
                            .recipientName(order.getRecipientName())
                            .recipientPhone(order.getRecipientPhone())
                            .shippingFee(order.getShippingFee() != null ? order.getShippingFee().multiply(BigDecimal.valueOf(0.9)) : BigDecimal.ZERO)
                            .totalAmount(order.getTotalAmount())
                            .status(order.getStatus())
                            .createdAt(order.getCreatedAt())
                            .estimatedDeliveryDate(order.getEstimatedDeliveryDate())
                            .shipToShopKm(shipToShopKm)
                            .shopToBuyerKm(shopToBuyerKm)
                            .distanceKm(shipToShopKm) // backward compat
                            .build();
                })
                // ✅ Sort theo shipper → shop (gần shop nhất trước)
                .sorted(Comparator.comparingDouble(o ->
                        o.getShipToShopKm() != null ? o.getShipToShopKm() : Double.MAX_VALUE))
                .limit(6)
                .collect(Collectors.toList());
    }

    @Override
    @Transactional
    public ShipperOrderResponse acceptOrder(Integer orderId) {
        User shipper = getCurrentShipper();
        Order order = orderRepository.findById(orderId)
                .orElseThrow(() -> new AppException(ErrorCode.ORDER_NOT_FOUND));
        if (order.getShipper() != null) throw new AppException(ErrorCode.ORDER_ALREADY_TAKEN);
        if (order.getStatus() != OrderStatus.CONFIRMED) throw new AppException(ErrorCode.INVALID_ORDER_STATUS);
        order.setShipper(shipper);
        order.setStatus(OrderStatus.SHIPPING);
        orderRepository.save(order);
        log.info("Shipper {} accepted order {}", shipper.getId(), orderId);
        return toShipperOrderResponse(order);
    }

    @Override
    @Transactional
    public ShipperOrderResponse updateOrderStatus(Integer orderId, UpdateOrderStatusRequest request) {


        User shipper = getCurrentShipper();

        Order order = orderRepository.findById(orderId)
                .orElseThrow(() -> new AppException(ErrorCode.ORDER_NOT_FOUND));

        if (!order.getShipper().getId().equals(shipper.getId())) {
            throw new AppException(ErrorCode.MISS_ROLE);
        }

        validateStatusTransition(order.getStatus(), request.getStatus());

        log.info("[SHIPPER] Update order {} → {}", orderId, request.getStatus());

        order.setStatus(request.getStatus());

        if (request.getNote() != null && !request.getNote().isBlank()) {
            order.setNote(request.getNote());
        }

        // ====================================================
        // TRẢ TIỀN SHIPPER (3% SHIPPING FEE)
        // ====================================================

        if (request.getStatus() == OrderStatus.DELIVERED) {

            BigDecimal shippingFee = order.getShippingFee() != null
                    ? order.getShippingFee()
                    : BigDecimal.ZERO;

            log.info("Processing shipping payout for order {}", orderId);
            log.info("Shipping fee: {}", shippingFee);

            if (shippingFee.compareTo(BigDecimal.ZERO) > 0) {

                // shipper nhận 90% tiền ship (trừ 10% phí sàn)
                BigDecimal shipperFee = shippingFee.multiply(BigDecimal.valueOf(0.9));

                // ======================
                // SHIPPER WALLET
                // ======================

                Wallet shipperWallet = walletRepository.findByShipperIdForUpdate(shipper.getId())
                        .orElseThrow(() -> new AppException(ErrorCode.WALLET_NOT_FOUND));

                log.info("Shipper wallet BEFORE: {}", shipperWallet.getFrozenBalance());

                shipperWallet.setFrozenBalance(
                        shipperWallet.getFrozenBalance().add(shipperFee)
                );

                shipperWallet.setTotalRevenueAllTime(
                        shipperWallet.getTotalRevenueAllTime().add(shipperFee)
                );
                shipperWallet.setUpdatedAt(LocalDateTime.now());

                walletRepository.save(shipperWallet);

                log.info("Shipper wallet AFTER: {}", shipperWallet.getFrozenBalance());

                // ======================
                // TRỪ TIỀN ADMIN (vì đã giữ shipping trước đó)
                // ======================

//                Wallet adminWallet = walletRepository.findByTypeForUpdate(WalletType.PLATFORM)
//                        .orElseThrow(() -> new AppException(ErrorCode.PLATFORM_WALLET_NOT_FOUND));
//
//                log.info("Admin wallet BEFORE: {}", adminWallet.getTotalBalance());
//
//                adminWallet.setTotalBalance(
//                        adminWallet.getTotalBalance().subtract(shipperFee)
//                );
//
//                walletRepository.save(adminWallet);

//                log.info("Admin wallet AFTER: {}", adminWallet.getTotalBalance());
            }
        }

        // ====================================================
        // CLEAR CURRENT ORDER FROM SHIPPER LOCATION
        // ====================================================

        if (request.getStatus() == OrderStatus.DELIVERED
                || request.getStatus() == OrderStatus.FAILED) {

            shipperLocationRepository.findByShipper(shipper).ifPresent(loc -> {
                loc.setCurrentOrder(null);
                shipperLocationRepository.save(loc);
            });
        }

        orderRepository.save(order);
        if (request.getStatus() == OrderStatus.FAILED) {
            Integer buyerId = order.getBuyer().getId();
            long failedCount = orderRepository
                    .countByBuyerIdAndStatus(buyerId, OrderStatus.FAILED) + 1;

            if (failedCount >= 3) {
                userService.deactivateUser(buyerId);
                log.info("[AutoLock] Buyer {} bị khóa do {} đơn FAILED", buyerId, failedCount);
            }
        }

        log.info("Order {} → {} by shipper {}", orderId, request.getStatus(), shipper.getId());

        return toShipperOrderResponse(order);
    }


    @Override
    public List<ShipperOrderResponse> getMyOrders() {
        User shipper = getCurrentShipper();
        return orderRepository.findByShipper(shipper).stream()
                .map(this::toShipperOrderResponse)
                .collect(Collectors.toList());
    }

    // ===================== WEBSOCKET =====================

    @Override
    @Transactional
    public ShipperLocationResponse updateLocationByEmail(String email, ShipperLocationRequest request) {
        User shipper = getShipperByEmail(email);
        Order order = orderRepository.findById(request.getOrderId())
                .orElseThrow(() -> new AppException(ErrorCode.ORDER_NOT_FOUND));

        ShipperLocation location = shipperLocationRepository.findByShipper(shipper)
                .orElse(ShipperLocation.builder().shipper(shipper).build());

        location.setLatitude(request.getLatitude());
        location.setLongitude(request.getLongitude());
        location.setCurrentOrder(order);
        shipperLocationRepository.save(location);

        return ShipperLocationResponse.builder()
                .shipperId(shipper.getId())
                .shipperName(shipper.getFullName())
                .orderId(order.getId())
                .latitude(request.getLatitude())
                .longitude(request.getLongitude())
                .updatedAt(location.getUpdatedAt())
                .shopLatitude(order.getShopLatitude())
                .shopLongitude(order.getShopLongitude())
                .destLatitude(order.getShippingLatitude())
                .destLongitude(order.getShippingLongitude())
                // ✅ Pass-through isFake → FE Tracking dùng để trimRoute
                .isFake(Boolean.TRUE.equals(request.getIsFake()))
                .build();
    }

    @Override
    @Transactional
    public ShipperLocationResponse updateLocation(ShipperLocationRequest request) {
        User shipper = getCurrentShipper();
        return updateLocationByEmail(shipper.getEmail(), request);
    }

    @Override
    public ShipperLocationResponse getShipperLocationByOrder(Integer orderId) {
        ShipperLocation location = shipperLocationRepository.findByCurrentOrder_Id(orderId)
                .orElseThrow(() -> new AppException(ErrorCode.LOCATION_NOT_FOUND));
        Order order = location.getCurrentOrder();
        return ShipperLocationResponse.builder()
                .shipperId(location.getShipper().getId())
                .shipperName(location.getShipper().getFullName())
                .orderId(orderId)
                .latitude(location.getLatitude())
                .longitude(location.getLongitude())
                .updatedAt(location.getUpdatedAt())
                .shopLatitude(order != null ? order.getShopLatitude() : null)
                .shopLongitude(order != null ? order.getShopLongitude() : null)
                .destLatitude(order != null ? order.getShippingLatitude() : null)
                .destLongitude(order != null ? order.getShippingLongitude() : null)
                .build();
    }

    // ===================== PRIVATE =====================

    private void validateStatusTransition(OrderStatus current, OrderStatus next) {
        boolean valid = switch (current) {
            case SHIPPING -> next == OrderStatus.DELIVERED || next == OrderStatus.FAILED;
            default -> false;
        };
        if (!valid) throw new AppException(ErrorCode.INVALID_ORDER_STATUS);
    }
}