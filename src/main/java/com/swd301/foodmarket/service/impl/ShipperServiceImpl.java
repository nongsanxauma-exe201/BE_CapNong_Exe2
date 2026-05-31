package com.swd301.foodmarket.service.impl;

import com.swd301.foodmarket.dto.goong.LatLong;
import com.swd301.foodmarket.dto.request.ShipperLocationRequest;
import com.swd301.foodmarket.dto.request.UpdateOrderStatusRequest;
import com.swd301.foodmarket.dto.response.AvailableOrderResponse;
import com.swd301.foodmarket.dto.response.PageResponse;
import com.swd301.foodmarket.dto.response.ShipperLocationResponse;
import com.swd301.foodmarket.dto.response.ShipperOrderResponse;
import com.swd301.foodmarket.entity.*;
import com.swd301.foodmarket.enums.OrderStatus;
import com.swd301.foodmarket.enums.RoleName;
import com.swd301.foodmarket.enums.WalletType;
import com.swd301.foodmarket.exception.AppException;
import com.swd301.foodmarket.exception.ErrorCode;
import com.swd301.foodmarket.mapper.OrderMapper;
import com.swd301.foodmarket.repository.OrderRepository;
import com.swd301.foodmarket.repository.ShipperLocationRepository;
import com.swd301.foodmarket.repository.UserRepository;
import com.swd301.foodmarket.repository.WalletRepository;
import com.swd301.foodmarket.service.GoongMapService;
import com.swd301.foodmarket.service.NotificationService;
import com.swd301.foodmarket.service.ShipperService;
import com.swd301.foodmarket.service.UserService;
import lombok.AccessLevel;
import lombok.RequiredArgsConstructor;
import lombok.experimental.FieldDefaults;
import lombok.extern.slf4j.Slf4j;
import org.aspectj.weaver.ast.Or;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.filter.RequestContextFilter;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.*;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
@FieldDefaults(level = AccessLevel.PRIVATE)
@Slf4j
public class ShipperServiceImpl implements ShipperService {

    final OrderRepository orderRepository;
    final  ShipperLocationRepository shipperLocationRepository;
    final  UserRepository userRepository;
    final  GoongMapService goongMapService;
    final  WalletRepository walletRepository;
    final  UserService userService;
    final NotificationService notificationService;
    @Value("${ADDRESS_WAREHOUSE}")
     String warehouseAddress;
    private final RequestContextFilter requestContextFilter;
    private final OrderMapper orderMapper;
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
//        String shopAddress = order.getShopOwner().getAddress();
        // Lấy địa chỉ kho làm shopAddress vì shipper phải tới kho lấy hàng trước, sau đó mới giao đến buyer
        String shopAddress = warehouseAddress;
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
        User shipper = getCurrentShipper();

        LatLong shipperLocation = new LatLong(shipperLat, shipperLng);

        // Chỉ query READY + chưa có shipper (tự pick)
        List<Order> availableOrders = orderRepository
                .findByShipperIsNullAndStatus(OrderStatus.READY);

        log.info("[SHIPPER] Finding orders near ({}, {}), total READY: {}",
                shipperLat, shipperLng, availableOrders.size());

        return availableOrders.stream()
                .map(order -> {
                    LatLong shopLocation = getOrGeocodeShopLocation(order);
                    LatLong buyerLocation = getOrGeocodeShippingLocation(order);

                    Double shipToShopKm = null;
                    if (shopLocation != null) {
                        double d = goongMapService.getDistanceKm(shipperLocation, shopLocation);
                        shipToShopKm = d == Double.MAX_VALUE ? null : d;
                    }

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
                            .distanceKm(shipToShopKm)
                            .build();
                })
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
        if (order.getStatus() != OrderStatus.READY) throw new AppException(ErrorCode.INVALID_ORDER_STATUS);
        order.setShipper(shipper);
        order.setStatus(OrderStatus.SHIPPING);
        orderRepository.save(order);

        log.info("Shipper {} accepted order {} (self-pick)", shipper.getId(), orderId);
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
        BigDecimal totalAmount = order.getTotalAmount();


        // tiền hàng (chưa trừ phí)

        // ====================================================
        // TRẢ TIỀN SHIPPER
        // ====================================================

        if (request.getStatus() == OrderStatus.DELIVERED) {

            log.info("========== [DELIVERED PAYOUT START] Order #{} ==========", order.getId());
            log.info("[ORDER] totalAmount={}, shippingFee={}, paymentMethod={}, refundAmount={}",
                    order.getTotalAmount(),
                    order.getShippingFee(),
                    order.getPaymentMethod(),
                    order.getRefundAmount()
            );

            BigDecimal shippingFee = order.getShippingFee() != null
                    ? order.getShippingFee()
                    : BigDecimal.ZERO;

            log.info("Processing shipping payout for order {}", orderId);
            log.info("Shipping fee: {}", shippingFee);

            if (shippingFee.compareTo(BigDecimal.ZERO) > 0) {

                // shipper nhận 90% tiền ship (trừ 10% phí sàn)
                BigDecimal shipperFee = shippingFee.multiply(BigDecimal.valueOf(0.9));
                log.info("[SHIPPER_PAYOUT_CALC] shippingFee={}, shipperFee=90%={}",
                        shippingFee,
                        shipperFee
                );
                // ======================
                // SHIPPER WALLET
                // ======================

                Wallet shipperWallet = walletRepository.findByShipperIdForUpdate(shipper.getId())
                        .orElseThrow(() -> new AppException(ErrorCode.WALLET_NOT_FOUND));
                log.info("[SHIPPER_WALLET_BEFORE] shipperId={}, totalBalance={}, frozenBalance={}, totalRevenueAllTime={}",
                        shipper.getId(),
                        shipperWallet.getTotalBalance(),
                        shipperWallet.getFrozenBalance(),
                        shipperWallet.getTotalRevenueAllTime()
                );

                shipperWallet.setTotalBalance(
                        shipperWallet.getTotalBalance().add(shipperFee)
                );

                shipperWallet.setTotalRevenueAllTime(
                        shipperWallet.getTotalRevenueAllTime().add(shipperFee)
                );

                shipperWallet.setUpdatedAt(LocalDateTime.now());

                walletRepository.save(shipperWallet);

                log.info("[SHIPPER_WALLET_AFTER] shipperId={}, totalBalance={}, frozenBalance={}, totalRevenueAllTime={}",
                        shipper.getId(),
                        shipperWallet.getTotalBalance(),
                        shipperWallet.getFrozenBalance(),
                        shipperWallet.getTotalRevenueAllTime()
                );
            }
            BigDecimal shopGross = totalAmount.subtract(shippingFee);
            BigDecimal shopRevenue = shopGross.multiply(BigDecimal.valueOf(0.9));
            BigDecimal finalShopRevenue = shopRevenue;

            if (shopRevenue.compareTo(BigDecimal.ZERO) > 0) {

                Wallet shopWallet = walletRepository.findByShopOwner_Id(order.getShopOwner().getId())
                        .orElseThrow(() -> new AppException(ErrorCode.WALLET_NOT_FOUND));
                log.info("[SHOP_WALLET_BEFORE] shopOwnerId={}, totalBalance={}, frozenBalance={}, totalRevenueAllTime={}",
                        order.getShopOwner().getId(),
                        shopWallet.getTotalBalance(),
                        shopWallet.getFrozenBalance(),
                        shopWallet.getTotalRevenueAllTime()
                );
                BigDecimal refundAmount = order.getRefundAmount() != null
                        ? order.getRefundAmount()
                        : BigDecimal.ZERO;

                log.info("[SHOP_PAYOUT_CALC] orderId={}, totalAmount={}, shippingFee={}, shopGross=total-shipping={}, shopRevenue=90%={}, refundAmount={}",
                        orderId,
                        totalAmount,
                        shippingFee,
                        shopGross,
                        shopRevenue,

                        refundAmount
                );
                if (refundAmount.compareTo(BigDecimal.ZERO) > 0) {

                    if (refundAmount.compareTo(shopRevenue) > 0) {
                        log.error("[REFUND_INVALID] orderId={}, refundAmount={}, shopRevenue={}",
                                orderId,
                                refundAmount,
                                shopRevenue
                        );
                        throw new AppException(ErrorCode.INVALID_REFUND_AMOUNT);
                    }

                    finalShopRevenue = shopRevenue.subtract(refundAmount);
                    log.info("[REFUND_CALC] orderId={}, refundAmount={}, finalShopRevenue=shopRevenue-refund={}",
                            orderId,
                            refundAmount,
                            finalShopRevenue
                    );
                    if (refundAmount.compareTo(BigDecimal.ZERO) > 0) {
                        Wallet buyerWallet = walletRepository.findByBuyer(order.getBuyer());

                        if (buyerWallet == null) {
                            throw new AppException(ErrorCode.WALLET_NOT_FOUND);
                        }

                        log.info("[BUYER_WALLET_BEFORE_REFUND] buyerId={}, totalBalance={}, frozenBalance={}, refundAmount={}",
                                order.getBuyer().getId(),
                                buyerWallet.getTotalBalance(),
                                buyerWallet.getFrozenBalance(),
                                refundAmount
                        );


                        buyerWallet.setTotalBalance(
                                buyerWallet.getTotalBalance().add(refundAmount)
                        );

                        walletRepository.save(buyerWallet);

                        log.info("[BUYER_WALLET_AFTER_REFUND] buyerId={}, totalBalance={}, frozenBalance={}",
                                order.getBuyer().getId(),
                                buyerWallet.getTotalBalance(),
                                buyerWallet.getFrozenBalance()
                        );

                        Notification buyerNotification = Notification.builder()
                                .title("Hoàn tiền đơn hàng #" + order.getId())
                                .message("Bạn đã nhận được " + refundAmount + "đ hoàn tiền từ đơn hàng #" + order.getId())
                                .receiverType("USER_" + order.getBuyer().getId())
                                .build();

                        notificationService.createUserNotification(order.getBuyer(), buyerNotification);

                        Notification shopNotification = Notification.builder()
                                .title("Hoàn tiền đơn hàng #" + order.getId())
                                .message("Bạn đã bị trừ " + refundAmount + "đ do hoàn tiền đơn hàng #" + order.getId())
                                .receiverType("USER_" + order.getShopOwner().getId())
                                .build();

                        notificationService.createUserNotification(order.getShopOwner(), shopNotification);
                    }
                }

                boolean isCOD = "COD".equals(order.getPaymentMethod());
                log.info("[SHOP_PAYOUT_METHOD] orderId={}, isCOD={}", orderId, isCOD);

                if (isCOD) {
                    // COD: tiền mặt shipper thu từ khách, chưa từng freeze → cộng trực tiếp vào ví shop
                    log.info("[SHOP_WALLET_OPERATION_COD] orderId={}, totalBalance + finalShopRevenue = {} + {}, revenueAllTime + finalShopRevenue = {} + {}",
                            orderId,
                            shopWallet.getTotalBalance(),
                            finalShopRevenue,
                            shopWallet.getTotalRevenueAllTime(),
                            finalShopRevenue
                    );
                    shopWallet.setTotalBalance(
                            shopWallet.getTotalBalance().add(finalShopRevenue)
                    );
                } else {
                    // PAYOS: tiền đã freeze lúc thanh toán → unfreeze khi giao thành công
                    if (shopWallet.getFrozenBalance().compareTo(shopRevenue) < 0) {
                        log.error("[SHOP_FROZEN_NOT_ENOUGH] orderId={}, frozenBalance={}, needToUnfreeze={}",
                                orderId,
                                shopWallet.getFrozenBalance(),
                                shopRevenue
                        );
                        throw new AppException(ErrorCode.WALLET_BALANCE_NOT_ENOUGH);
                    }

                    log.info("[SHOP_WALLET_OPERATION_PAYOS] orderId={}, frozenBalance - shopRevenue = {} - {}, totalBalance - refundAmount = {} - {}, revenueAllTime + finalShopRevenue = {} + {}",
                            orderId,
                            shopWallet.getFrozenBalance(),
                            shopRevenue,
                            shopWallet.getTotalBalance(),
                            refundAmount,
                            shopWallet.getTotalRevenueAllTime(),
                            finalShopRevenue
                    );
                    shopWallet.setFrozenBalance(
                            shopWallet.getFrozenBalance().subtract(shopRevenue)
                    );

                    if (refundAmount.compareTo(BigDecimal.ZERO) > 0) {
                        shopWallet.setTotalBalance(
                                shopWallet.getTotalBalance().subtract(refundAmount)
                        );
                    }
                }

                shopWallet.setTotalRevenueAllTime(
                        shopWallet.getTotalRevenueAllTime().add(finalShopRevenue)
                );

                shopWallet.setUpdatedAt(LocalDateTime.now());
                log.info("[SHOP_WALLET_AFTER] shopOwnerId={}, totalBalance={}, frozenBalance={}, totalRevenueAllTime={}",
                        order.getShopOwner().getId(),
                        shopWallet.getTotalBalance(),
                        shopWallet.getFrozenBalance(),
                        shopWallet.getTotalRevenueAllTime()
                );
                walletRepository.save(shopWallet);
            }

            log.info("[PAYOUT_SUMMARY] orderId={}, totalAmount={}, shippingFee={}, shopGross={}, shopRevenue={}, refundAmount={}, finalShopRevenue={}",
                    orderId,
                    totalAmount,
                    shippingFee,
                    shopGross,
                    shopRevenue,
                    order.getRefundAmount() != null
                            ? order.getRefundAmount()
                            : BigDecimal.ZERO,
                    finalShopRevenue
            );
            log.info("========== [DELIVERED PAYOUT END] Order #{} ==========", order.getId());
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


//    public Map<String,String> calculateRefundAmount(Order order) {
//        List<OrderDetail> details = order.getOrderDetails();
//        BigDecimal refundAmount = BigDecimal.ZERO;
//        String message = "";
//        for(OrderDetail detail : details) {
//            QualityResult qualityResult = detail.getQualityResult();
//            double percentage = 0;
//            if (qualityResult != null && qualityResult.getRejectedWeight() != null &&   qualityResult.getRejectedWeight() > 0) {
//                percentage = (double) (qualityResult.getRejectedWeight() * 100) / detail.getQuantity();
//            }
//            if (percentage > 5) {
//                message+= "Sản phẩm " + detail.getProduct().getProductName() + " bị hư hỏng " + qualityResult.getRejectedWeight();
//                refundAmount = refundAmount.add(detail.getUnitPrice().multiply(BigDecimal.valueOf(qualityResult.getRejectedWeight())));
//            }
//        }
//        Map<String,String> map = new HashMap<>();
//        map.put("message", message);
//        map.put("refundAmount", refundAmount.toString());
//        return map;
//
//    }
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
    public PageResponse<AvailableOrderResponse> getAllOrderCanShipper(int page, int size) {


        List<Order> availableOrders = orderRepository.findByShipperIsNullAndStatus(OrderStatus.READY);

        List<AvailableOrderResponse> availableOrderResponses = availableOrders.stream()
                .map(order -> {
                    // ✅ Geocode shop (điểm lấy hàng)
                    LatLong shopLocation = getOrGeocodeShopLocation(order);

                    // ✅ Geocode nhà buyer (điểm giao hàng)
                    LatLong buyerLocation = getOrGeocodeShippingLocation(order);

                    // ✅ Khoảng cách shop → nhà buyer (shipper biết tổng quãng đường)
                    Double shopToBuyerKm = null;
                    if (shopLocation != null && buyerLocation != null) {
                        double d = goongMapService.getDistanceKm(shopLocation, buyerLocation);
                        shopToBuyerKm = d == Double.MAX_VALUE ? null : d;
                    }



                    return AvailableOrderResponse.builder()
                            .orderId(order.getId())
                            .shopName("Kho tập kết hàng hóa") // Hiện tại để cố định tên shop vì shipper phải tới kho lấy hàng trước, sau đó mới giao đến buyer
                            .shopAddress(warehouseAddress)
                            .shopPhone(order.getShopOwner().getPhoneNumber())
                            .shippingAddress(order.getShippingAddress())
                            .recipientName(order.getRecipientName())
                            .recipientPhone(order.getRecipientPhone())
                            .shippingFee(order.getShippingFee() != null ? order.getShippingFee().multiply(BigDecimal.valueOf(0.9)) : BigDecimal.ZERO)
                            .totalAmount(order.getTotalAmount())
                            .status(order.getStatus())
                            .createdAt(order.getCreatedAt())
                            .estimatedDeliveryDate(order.getEstimatedDeliveryDate())
                            .shopToBuyerKm(shopToBuyerKm)
                            .distanceKm(shopToBuyerKm) // backward compat
                            .build();
                }).sorted(Comparator.comparing(AvailableOrderResponse::getCreatedAt)).
                toList();



        Long from = (long) page * size;
        Long to = Math.min(from + size, availableOrderResponses.size());
        List<AvailableOrderResponse> result = new ArrayList<>();
        if(from < availableOrderResponses.size())
        {
            result = availableOrderResponses.subList(from.intValue(), to.intValue());
        }
        return PageResponse.<AvailableOrderResponse>builder()
                .content(result)
                .page(page)
                .size(size)
                .totalElements(availableOrderResponses.size())
                .totalPages(((int) Math.ceil((double) availableOrderResponses.size() / size)))
                .last((page + 1) * size >= availableOrderResponses.size())
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