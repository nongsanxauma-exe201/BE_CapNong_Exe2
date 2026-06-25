package com.swd301.foodmarket.service.impl;

import com.swd301.foodmarket.dto.goong.LatLong;
import com.swd301.foodmarket.dto.request.AcceptOrderRequest;
import com.swd301.foodmarket.dto.request.ShipperLocationRequest;
import com.swd301.foodmarket.dto.request.UpdateOrderStatusRequest;
import com.swd301.foodmarket.dto.response.AvailableOrderResponse;
import com.swd301.foodmarket.dto.response.PageResponse;
import com.swd301.foodmarket.dto.response.ShipperLocationResponse;
import com.swd301.foodmarket.dto.response.ShipperOrderResponse;
import com.swd301.foodmarket.entity.*;
import com.swd301.foodmarket.enums.CodPaymentOption;
import com.swd301.foodmarket.enums.CodPaymentStatus;
import com.swd301.foodmarket.enums.OrderStatus;
import com.swd301.foodmarket.enums.RoleName;
import com.swd301.foodmarket.enums.TransactionStatus;
import com.swd301.foodmarket.enums.TransactionType;
import com.swd301.foodmarket.enums.WalletType;
import com.swd301.foodmarket.exception.AppException;
import com.swd301.foodmarket.exception.ErrorCode;
import com.swd301.foodmarket.mapper.OrderMapper;
import com.swd301.foodmarket.repository.OrderRepository;
import com.swd301.foodmarket.repository.ShipperLocationRepository;
import com.swd301.foodmarket.repository.TransactionRepository;
import com.swd301.foodmarket.repository.UserRepository;
import com.swd301.foodmarket.repository.WalletRepository;
import com.swd301.foodmarket.repository.WarehouseRepository;
import com.swd301.foodmarket.service.GoongMapService;
import com.swd301.foodmarket.service.NotificationService;
import com.swd301.foodmarket.service.OrderRewardService;
import com.swd301.foodmarket.service.ShipperService;
import com.swd301.foodmarket.service.UserService;
import com.swd301.foodmarket.service.VoucherService;
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
import java.time.ZoneId;
import java.util.*;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
@FieldDefaults(level = AccessLevel.PRIVATE)
@Slf4j
public class ShipperServiceImpl implements ShipperService {

    final OrderRepository orderRepository;
    final ShipperLocationRepository shipperLocationRepository;
    final UserRepository userRepository;
    final GoongMapService goongMapService;
    final WalletRepository walletRepository;
    final UserService userService;
    final NotificationService notificationService;
    final WarehouseRepository warehouseRepository;
    @Value("${ADDRESS_WAREHOUSE}")
    String warehouseAddress;
    private final RequestContextFilter requestContextFilter;
    private final OrderMapper orderMapper;
    private final OrderRewardService orderRewardService;
    private final TransactionRepository transactionRepository;
    private final VoucherService voucherService;
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

    /**
     * Lấy kho bãi đang active đầu tiên từ DB.
     * Production chỉ có 1 kho; fake data có thể có nhiều nên lấy cái đầu tiên.
     */
    private Warehouse getActiveWarehouse() {
        return warehouseRepository.findAllByActive(true)
                .stream().findFirst().orElse(null);
    }

    private ShipperOrderResponse toShipperOrderResponse(Order order) {
        // Lấy kho từ DB — đây là điểm shipper phải đến để lấy hàng
        Warehouse warehouse = getActiveWarehouse();

        // Lazy-geocode tọa độ giao hàng nếu chưa có trong DB (cache lại luôn)
        if (order.getShippingLatitude() == null || order.getShippingLongitude() == null) {
            getOrGeocodeShippingLocation(order);
        }

        // Lazy-geocode tọa độ kho nếu cả warehouse lẫn cache order đều null
        if ((warehouse == null || warehouse.getLatitude() == null)
                && (order.getShopLatitude() == null || order.getShopLongitude() == null)) {
            getOrGeocodeShopLocation(order);
        }

        return ShipperOrderResponse.builder()
                .orderId(order.getId())
                .buyerName(order.getBuyer().getFullName())
                .buyerPhone(order.getBuyer().getPhoneNumber())
                .shippingAddress(order.getShippingAddress())
                .recipientName(order.getRecipientName())
                .recipientPhone(order.getRecipientPhone())
                .shippingFee(order.getShippingFee() != null ? order.getShippingFee().multiply(BigDecimal.valueOf(0.9)) : BigDecimal.ZERO)
                .totalAmount(order.getTotalAmount())
                .paymentMethod(order.getPaymentMethod())
                .codAmount(order.getCodAmount())
                .codPrepaidAmount(order.getCodPrepaidAmount())
                .codPaymentOption(order.getCodPaymentOption())
                .codPaymentStatus(order.getCodPaymentStatus())
                .refundAmount(order.getRefundAmount())
                .refundReason(order.getRefundReason())
                .status(order.getStatus())
                .createdAt(order.getCreatedAt())
                .estimatedDeliveryDate(order.getEstimatedDeliveryDate())
                .note(order.getNote())
                // ── Kho bãi từ bảng warehouses DB ──────────────────────────────
                // Nếu warehouse DB chưa có tọa độ (fake data) → fallback về
                // order.shopLatitude/shopLongitude (đã geocode từ ADDRESS_WAREHOUSE)
                .warehouseId(warehouse != null ? warehouse.getId() : null)
                .warehouseName(warehouse != null ? warehouse.getName() : "Kho bãi")
                .warehouseAddress(warehouse != null ? warehouse.getAddress() : warehouseAddress)
                .warehouseLat(warehouse != null && warehouse.getLatitude() != null
                        ? warehouse.getLatitude() : order.getShopLatitude())
                .warehouseLng(warehouse != null && warehouse.getLongitude() != null
                        ? warehouse.getLongitude() : order.getShopLongitude())
                // ── Điểm giao hàng (nhà buyer) ─────────────────────────────────
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

        // Lấy kho bãi một lần, dùng chung cho toàn bộ stream
        Warehouse warehouse = getActiveWarehouse();

        return availableOrders.stream()
                .map(order -> {
                    LatLong warehouseLocation = getOrGeocodeShopLocation(order);
                    LatLong buyerLocation     = getOrGeocodeShippingLocation(order);

                    Double shipToShopKm = null;
                    if (warehouseLocation != null) {
                        double d = goongMapService.getDistanceKm(shipperLocation, warehouseLocation);
                        shipToShopKm = d == Double.MAX_VALUE ? null : d;
                    }

                    Double shopToBuyerKm = null;
                    if (warehouseLocation != null && buyerLocation != null) {
                        double d = goongMapService.getDistanceKm(warehouseLocation, buyerLocation);
                        shopToBuyerKm = d == Double.MAX_VALUE ? null : d;
                    }

                    return AvailableOrderResponse.builder()
                            .orderId(order.getId())
                            // ── Thông tin kho bãi từ DB (không phải shop owner) ──────────
                            .shopName(warehouse != null ? warehouse.getName() : "Kho bãi")
                            .shopAddress(warehouse != null ? warehouse.getAddress() : warehouseAddress)
                            .shopPhone(null) // kho bãi không có SĐT riêng
                            .shippingAddress(order.getShippingAddress())
                            .recipientName(order.getRecipientName())
                            .recipientPhone(order.getRecipientPhone())
                            .shippingFee(order.getShippingFee() != null ? order.getShippingFee().multiply(BigDecimal.valueOf(0.9)) : BigDecimal.ZERO)
                            .totalAmount(order.getTotalAmount())
                            .paymentMethod(order.getPaymentMethod())
                            .codPrepaidAmount(resolveCodPrepaidAmount(order))
                            .codPaymentOption(order.getCodPaymentOption())
                            .codPaymentStatus(order.getCodPaymentStatus())
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
    public ShipperOrderResponse acceptOrder(Integer orderId, AcceptOrderRequest request) {
        User shipper = getCurrentShipper();
        Order order = orderRepository.findById(orderId)
                .orElseThrow(() -> new AppException(ErrorCode.ORDER_NOT_FOUND));
        if (order.getShipper() != null) throw new AppException(ErrorCode.ORDER_ALREADY_TAKEN);
        if (order.getStatus() != OrderStatus.READY) throw new AppException(ErrorCode.INVALID_ORDER_STATUS);

        if ("COD".equalsIgnoreCase(order.getPaymentMethod())) {
            CodPaymentOption option = request == null ? null : request.getCodPaymentOption();
            if (option == null) {
                throw new AppException(ErrorCode.INVALID_REQUEST);
            }
            processCodPrepaymentOnAccept(order, shipper, option);
        } else {
            order.setCodPaymentStatus(CodPaymentStatus.NOT_REQUIRED);
        }

        order.setShipper(shipper);
        order.setStatus(OrderStatus.SHIPPING);
        orderRepository.save(order);

        log.info("Shipper {} accepted order {} (self-pick)", shipper.getId(), orderId);
        return toShipperOrderResponse(order);
    }

    private void processCodPrepaymentOnAccept(Order order, User shipper, CodPaymentOption option) {
        BigDecimal prepaidAmount = resolveCodPrepaidAmount(order);
        order.setCodPaymentOption(option);
        order.setCodPrepaidAmount(prepaidAmount);

        if (option == CodPaymentOption.CASH) {
            order.setCodPaymentStatus(CodPaymentStatus.PENDING_ADMIN_CONFIRM);
            saveTransaction(TransactionType.COD_SHIPPER_CASH_PENDING, prepaidAmount, null,
                    "ORDER_" + order.getId(),
                    "Shipper chọn trả tiền mặt COD, chờ admin xác nhận đơn #" + order.getId());
            return;
        }

        Wallet shipperWallet = walletRepository.findByShipperIdForUpdate(shipper.getId())
                .orElseThrow(() -> new AppException(ErrorCode.WALLET_NOT_FOUND));
        if (availableBalance(shipperWallet).compareTo(prepaidAmount) < 0) {
            throw new AppException(ErrorCode.INSUFFICIENT_BALANCE);
        }

        Wallet platformWallet = walletRepository.findByTypeForUpdate(WalletType.PLATFORM)
                .orElseThrow(() -> new AppException(ErrorCode.PLATFORM_WALLET_NOT_FOUND));

        shipperWallet.setTotalBalance(shipperWallet.getTotalBalance().subtract(prepaidAmount));
        shipperWallet.setUpdatedAt(LocalDateTime.now(ZoneId.of("Asia/Ho_Chi_Minh")));
        platformWallet.setTotalBalance(platformWallet.getTotalBalance().add(prepaidAmount));
        platformWallet.setFrozenBalance(platformWallet.getFrozenBalance().add(prepaidAmount));
        platformWallet.setUpdatedAt(LocalDateTime.now(ZoneId.of("Asia/Ho_Chi_Minh")));

        walletRepository.save(shipperWallet);
        walletRepository.save(platformWallet);

        order.setCodPaymentStatus(CodPaymentStatus.PAID);
        order.setShipperPaidAt(LocalDateTime.now(ZoneId.of("Asia/Ho_Chi_Minh")));

        saveTransaction(TransactionType.COD_SHIPPER_WALLET_DEBIT, prepaidAmount.negate(),
                shipperWallet.getTotalBalance(), "ORDER_" + order.getId(),
                "Trừ ví shipper để ứng tiền hàng COD đơn #" + order.getId());
        saveTransaction(TransactionType.PLATFORM_FREEZE, prepaidAmount,
                platformWallet.getFrozenBalance(), "ORDER_" + order.getId(),
                "Platform giữ tiền hàng COD shipper ứng qua ví đơn #" + order.getId());
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

        if ("COD".equalsIgnoreCase(order.getPaymentMethod())
                && (request.getStatus() == OrderStatus.DELIVERED || request.getStatus() == OrderStatus.FAILED)
                && order.getCodPaymentStatus() != CodPaymentStatus.PAID) {
            throw new AppException(ErrorCode.INVALID_ORDER_STATUS);
        }

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

            if ("PAYOS".equalsIgnoreCase(order.getPaymentMethod())) {
                processPayosDeliveredPayout(order, shipper);
            }

            if ("COD".equalsIgnoreCase(order.getPaymentMethod())) {
                processCodDelivered(order);
            }

            orderRewardService.applyRewardIfEligible(order);
            voucherService.grantDamageVoucherForDeliveredOrder(order);
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
            if ("COD".equalsIgnoreCase(order.getPaymentMethod())
                    && order.getCodPaymentStatus() == CodPaymentStatus.PAID) {
                order.setCodPaymentStatus(CodPaymentStatus.REFUND_PENDING);
            }
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
    private void processPayosDeliveredPayout(Order order, User shipper) {
        BigDecimal totalAmount = order.getTotalAmount() != null
                ? order.getTotalAmount()
                : BigDecimal.ZERO;

        BigDecimal shippingFee = order.getShippingFee() != null
                ? order.getShippingFee()
                : BigDecimal.ZERO;

        BigDecimal shippingDiscountAmount = order.getShippingDiscountAmount() != null
                ? order.getShippingDiscountAmount()
                : BigDecimal.ZERO;

        BigDecimal refundAmount = order.getRefundAmount() != null
                ? order.getRefundAmount()
                : BigDecimal.ZERO;

        BigDecimal settlementTotal = totalAmount.add(shippingDiscountAmount);
        BigDecimal shopGross = settlementTotal.subtract(shippingFee);

        if (refundAmount.compareTo(shopGross) > 0) {
            throw new AppException(ErrorCode.INVALID_REFUND_AMOUNT);
        }

        // Phí sàn shop tính sau refund
        BigDecimal shopChargeable = shopGross.subtract(refundAmount);

        BigDecimal platformFeeFromShop = shopChargeable.multiply(BigDecimal.valueOf(0.1));
        BigDecimal platformFeeFromShip = shippingFee.multiply(BigDecimal.valueOf(0.1));
        BigDecimal platformFeeTotal = platformFeeFromShop.add(platformFeeFromShip);

        BigDecimal finalShopRevenue = shopChargeable.subtract(platformFeeFromShop);
        BigDecimal shipperNet = shippingFee.subtract(platformFeeFromShip);

        log.info("========== [PAYOS DELIVERED PAYOUT START] Order #{} ==========", order.getId());

        log.info("[PAYOS_AMOUNT_INPUT] buyerPaid={}, shippingFee={}, shippingDiscountAmount={}, settlementTotal={}, shopGross={}, refundAmount={}",
                totalAmount,
                shippingFee,
                shippingDiscountAmount,
                settlementTotal,
                shopGross,
                refundAmount
        );

        log.info("[PAYOS_AFTER_REFUND] shopChargeable=shopGross-refundAmount => {} - {} = {}",
                shopGross,
                refundAmount,
                shopChargeable
        );

        log.info("[PAYOS_FEE] platformFeeFromShop={}, platformFeeFromShip={}, platformFeeTotal={}",
                platformFeeFromShop,
                platformFeeFromShip,
                platformFeeTotal
        );

        log.info("[PAYOS_NET] finalShopRevenue={}, shipperNet={}, refundAmount={}",
                finalShopRevenue,
                shipperNet,
                refundAmount
        );

        Wallet platformWallet = walletRepository.findByTypeForUpdate(WalletType.PLATFORM)
                .orElseThrow(() -> new AppException(ErrorCode.PLATFORM_WALLET_NOT_FOUND));

        Wallet shopWallet = walletRepository.findByShopOwner_Id(order.getShopOwner().getId())
                .orElseThrow(() -> new AppException(ErrorCode.WALLET_NOT_FOUND));

        Wallet shipperWallet = walletRepository.findByShipperIdForUpdate(shipper.getId())
                .orElseThrow(() -> new AppException(ErrorCode.WALLET_NOT_FOUND));
///  Log to check
        BigDecimal platformTotalBefore = platformWallet.getTotalBalance();
        BigDecimal platformFrozenBefore = platformWallet.getFrozenBalance();
        BigDecimal platformCommissionBefore = platformWallet.getCommissionWallet();
        BigDecimal platformRevenueBefore = platformWallet.getTotalRevenueAllTime();

        BigDecimal shopTotalBefore = shopWallet.getTotalBalance();
        BigDecimal shopFrozenBefore = shopWallet.getFrozenBalance();
        BigDecimal shopRevenueBefore = shopWallet.getTotalRevenueAllTime();

        BigDecimal shipperTotalBefore = shipperWallet.getTotalBalance();
        BigDecimal shipperFrozenBefore = shipperWallet.getFrozenBalance();
        BigDecimal shipperRevenueBefore = shipperWallet.getTotalRevenueAllTime();

        log.info("[PAYOS_PLATFORM_BEFORE] totalBalance={}, frozenBalance={}, commissionWallet={}, totalRevenueAllTime={}",
                platformTotalBefore,
                platformFrozenBefore,
                platformCommissionBefore,
                platformRevenueBefore
        );

        log.info("[PAYOS_SHOP_BEFORE] shopOwnerId={}, totalBalance={}, frozenBalance={}, totalRevenueAllTime={}",
                order.getShopOwner().getId(),
                shopTotalBefore,
                shopFrozenBefore,
                shopRevenueBefore
        );

        log.info("[PAYOS_SHIPPER_BEFORE] shipperId={}, totalBalance={}, frozenBalance={}, totalRevenueAllTime={}",
                shipper.getId(),
                shipperTotalBefore,
                shipperFrozenBefore,
                shipperRevenueBefore
        );


        // Payment success đã add platform.frozenBalance += totalAmount.
        // Khi delivered, commission trở thành tiền lời admin nên không còn là tiền giữ hộ.
        if (platformWallet.getFrozenBalance().compareTo(platformFeeTotal) < 0) {
            throw new AppException(ErrorCode.PLATFORM_BALANCE_NOT_ENOUGH);
        }

        platformWallet.setFrozenBalance(
                platformWallet.getFrozenBalance().subtract(platformFeeTotal)
        );

        platformWallet.setCommissionWallet(
                platformWallet.getCommissionWallet().add(platformFeeTotal)
        );

        platformWallet.setTotalRevenueAllTime(
                platformWallet.getTotalRevenueAllTime().add(platformFeeTotal)
        );

        // Payment success đã add shop.totalBalance += shopGross và shop.frozenBalance += shopGross.
        // Delivered thì mở khóa shopGross, sau đó trừ refund + phí sàn shop khỏi totalBalance.
        if (shopWallet.getFrozenBalance().compareTo(shopGross) < 0) {
            throw new AppException(ErrorCode.WALLET_BALANCE_NOT_ENOUGH);
        }

        shopWallet.setFrozenBalance(
                shopWallet.getFrozenBalance().subtract(shopGross)
        );

        BigDecimal shopDeduction = refundAmount.add(platformFeeFromShop);

        shopWallet.setTotalBalance(
                shopWallet.getTotalBalance().subtract(shopDeduction)
        );

        shopWallet.setTotalRevenueAllTime(
                shopWallet.getTotalRevenueAllTime().add(finalShopRevenue)
        );

        shopWallet.setUpdatedAt(LocalDateTime.now(ZoneId.of("Asia/Ho_Chi_Minh")));

        // Shipper nhận 90% phí ship khi delivered
        shipperWallet.setTotalBalance(
                shipperWallet.getTotalBalance().add(shipperNet)
        );

        shipperWallet.setTotalRevenueAllTime(
                shipperWallet.getTotalRevenueAllTime().add(shipperNet)
        );

        shipperWallet.setUpdatedAt(LocalDateTime.now(ZoneId.of("Asia/Ho_Chi_Minh")));

        // PayOS refund về ví buyer
        if (refundAmount.compareTo(BigDecimal.ZERO) > 0) {
            Wallet buyerWallet = walletRepository.findByBuyer(order.getBuyer());

            if (buyerWallet == null) {
                throw new AppException(ErrorCode.WALLET_NOT_FOUND);
            }
            BigDecimal buyerTotalBefore = buyerWallet.getTotalBalance();
            BigDecimal buyerFrozenBefore = buyerWallet.getFrozenBalance();
            BigDecimal buyerRevenueBefore = buyerWallet.getTotalRevenueAllTime();

            log.info("[PAYOS_BUYER_BEFORE] buyerId={}, totalBalance={}, frozenBalance={}, totalRevenueAllTime={}",
                    order.getBuyer().getId(),
                    buyerTotalBefore,
                    buyerFrozenBefore,
                    buyerRevenueBefore
            );

            buyerWallet.setTotalBalance(
                    buyerWallet.getTotalBalance().add(refundAmount)
            );

            walletRepository.save(buyerWallet);
            log.info("[PAYOS_BUYER_AFTER] buyerId={}, totalBalance={} -> {}, frozenBalance={} -> {}, totalRevenueAllTime={} -> {}",
                    order.getBuyer().getId(),
                    buyerTotalBefore,
                    buyerWallet.getTotalBalance(),
                    buyerFrozenBefore,
                    buyerWallet.getFrozenBalance(),
                    buyerRevenueBefore,
                    buyerWallet.getTotalRevenueAllTime()
            );
            BigDecimal expectedBuyerTotalAfter = buyerTotalBefore.add(refundAmount);

            log.info("[PAYOS_VERIFY] expectedBuyerTotalAfter={}, actualBuyerTotalAfter={}, match={}",
                    expectedBuyerTotalAfter,
                    buyerWallet.getTotalBalance(),
                    expectedBuyerTotalAfter.compareTo(buyerWallet.getTotalBalance()) == 0
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

        walletRepository.save(platformWallet);
        walletRepository.save(shopWallet);
        walletRepository.save(shipperWallet);

        saveTransaction(TransactionType.PLATFORM_COMMISSION, platformFeeTotal,
                platformWallet.getCommissionWallet(), "ORDER_" + order.getId(),
                "Ghi nhận phí nền tảng PayOS đơn #" + order.getId());
        saveTransaction(TransactionType.SHOP_RELEASE, finalShopRevenue,
                shopWallet.getTotalBalance(), "ORDER_" + order.getId(),
                "Mở khóa tiền shop sau giao thành công đơn #" + order.getId());
        saveTransaction(TransactionType.SHIPPER_PAYOUT, shipperNet,
                shipperWallet.getTotalBalance(), "ORDER_" + order.getId(),
                "Cộng phí giao hàng cho shipper đơn #" + order.getId());
        if (shippingDiscountAmount.compareTo(BigDecimal.ZERO) > 0) {
            saveTransaction(TransactionType.SHIPPING_SUBSIDY, shippingDiscountAmount.negate(),
                    platformWallet.getTotalBalance(), "ORDER_" + order.getId(),
                    "Admin/platform bù phí vận chuyển cho shipper đơn #" + order.getId());
        }
        if (refundAmount.compareTo(BigDecimal.ZERO) > 0) {
            saveTransaction(TransactionType.REFUND, refundAmount.negate(),
                    walletRepository.findByBuyer(order.getBuyer()).getTotalBalance(), "ORDER_" + order.getId(),
                    "Hoàn tiền vào ví buyer cho đơn #" + order.getId());
        }

        log.info("[PAYOS_PLATFORM_AFTER] totalBalance={} -> {}, frozenBalance={} -> {}, commissionWallet={} -> {}, totalRevenueAllTime={} -> {}",
                platformTotalBefore,
                platformWallet.getTotalBalance(),
                platformFrozenBefore,
                platformWallet.getFrozenBalance(),
                platformCommissionBefore,
                platformWallet.getCommissionWallet(),
                platformRevenueBefore,
                platformWallet.getTotalRevenueAllTime()
        );

        log.info("[PAYOS_SHOP_AFTER] shopOwnerId={}, totalBalance={} -> {}, frozenBalance={} -> {}, totalRevenueAllTime={} -> {}",
                order.getShopOwner().getId(),
                shopTotalBefore,
                shopWallet.getTotalBalance(),
                shopFrozenBefore,
                shopWallet.getFrozenBalance(),
                shopRevenueBefore,
                shopWallet.getTotalRevenueAllTime()
        );

        log.info("[PAYOS_SHIPPER_AFTER] shipperId={}, totalBalance={} -> {}, frozenBalance={} -> {}, totalRevenueAllTime={} -> {}",
                shipper.getId(),
                shipperTotalBefore,
                shipperWallet.getTotalBalance(),
                shipperFrozenBefore,
                shipperWallet.getFrozenBalance(),
                shipperRevenueBefore,
                shipperWallet.getTotalRevenueAllTime()
        );
        BigDecimal expectedPlatformFrozenAfter = platformFrozenBefore.subtract(platformFeeTotal);
        BigDecimal expectedPlatformCommissionAfter = platformCommissionBefore.add(platformFeeTotal);
        BigDecimal expectedPlatformRevenueAfter = platformRevenueBefore.add(platformFeeTotal);

        BigDecimal expectedShopTotalAfter = shopTotalBefore.subtract(refundAmount.add(platformFeeFromShop));
        BigDecimal expectedShopFrozenAfter = shopFrozenBefore.subtract(shopGross);
        BigDecimal expectedShopRevenueAfter = shopRevenueBefore.add(finalShopRevenue);

        BigDecimal expectedShipperTotalAfter = shipperTotalBefore.add(shipperNet);
        BigDecimal expectedShipperRevenueAfter = shipperRevenueBefore.add(shipperNet);

        log.info("[PAYOS_VERIFY] expectedPlatformFrozenAfter={}, actualPlatformFrozenAfter={}, match={}",
                expectedPlatformFrozenAfter,
                platformWallet.getFrozenBalance(),
                expectedPlatformFrozenAfter.compareTo(platformWallet.getFrozenBalance()) == 0
        );

        log.info("[PAYOS_VERIFY] expectedPlatformCommissionAfter={}, actualPlatformCommissionAfter={}, match={}",
                expectedPlatformCommissionAfter,
                platformWallet.getCommissionWallet(),
                expectedPlatformCommissionAfter.compareTo(platformWallet.getCommissionWallet()) == 0
        );

        log.info("[PAYOS_VERIFY] expectedPlatformRevenueAfter={}, actualPlatformRevenueAfter={}, match={}",
                expectedPlatformRevenueAfter,
                platformWallet.getTotalRevenueAllTime(),
                expectedPlatformRevenueAfter.compareTo(platformWallet.getTotalRevenueAllTime()) == 0
        );

        log.info("[PAYOS_VERIFY] expectedShopTotalAfter={}, actualShopTotalAfter={}, match={}",
                expectedShopTotalAfter,
                shopWallet.getTotalBalance(),
                expectedShopTotalAfter.compareTo(shopWallet.getTotalBalance()) == 0
        );

        log.info("[PAYOS_VERIFY] expectedShopFrozenAfter={}, actualShopFrozenAfter={}, match={}",
                expectedShopFrozenAfter,
                shopWallet.getFrozenBalance(),
                expectedShopFrozenAfter.compareTo(shopWallet.getFrozenBalance()) == 0
        );

        log.info("[PAYOS_VERIFY] expectedShopRevenueAfter={}, actualShopRevenueAfter={}, match={}",
                expectedShopRevenueAfter,
                shopWallet.getTotalRevenueAllTime(),
                expectedShopRevenueAfter.compareTo(shopWallet.getTotalRevenueAllTime()) == 0
        );

        log.info("[PAYOS_VERIFY] expectedShipperTotalAfter={}, actualShipperTotalAfter={}, match={}",
                expectedShipperTotalAfter,
                shipperWallet.getTotalBalance(),
                expectedShipperTotalAfter.compareTo(shipperWallet.getTotalBalance()) == 0
        );

        log.info("[PAYOS_VERIFY] expectedShipperRevenueAfter={}, actualShipperRevenueAfter={}, match={}",
                expectedShipperRevenueAfter,
                shipperWallet.getTotalRevenueAllTime(),
                expectedShipperRevenueAfter.compareTo(shipperWallet.getTotalRevenueAllTime()) == 0
        );


        log.info("========== [PAYOS DELIVERED PAYOUT END] Order #{} ==========", order.getId());
    }

    private void saveTransaction(TransactionType type, BigDecimal amount, BigDecimal balanceAfter, String referenceId, String content) {
        transactionRepository.save(Transaction.builder()
                .paymentGateway("INTERNAL")
                .transactionType(type)
                .referenceId(referenceId)
                .amount(amount)
                .balanceAfter(balanceAfter)
                .status(TransactionStatus.SUCCESS)
                .content(content)
                .build());
    }

    private BigDecimal resolveCodPrepaidAmount(Order order) {
        BigDecimal total = order.getTotalAmount() != null ? order.getTotalAmount() : BigDecimal.ZERO;
        BigDecimal shipping = order.getShippingFee() != null ? order.getShippingFee() : BigDecimal.ZERO;
        BigDecimal shippingDiscount = order.getShippingDiscountAmount() != null ? order.getShippingDiscountAmount() : BigDecimal.ZERO;
        BigDecimal refund = order.getRefundAmount() != null ? order.getRefundAmount() : BigDecimal.ZERO;
        BigDecimal amount = total.add(shippingDiscount).subtract(shipping).subtract(refund);
        if (amount.compareTo(BigDecimal.ZERO) < 0) {
            return BigDecimal.ZERO;
        }
        return amount;
    }

    private BigDecimal availableBalance(Wallet wallet) {
        BigDecimal total = wallet.getTotalBalance() != null ? wallet.getTotalBalance() : BigDecimal.ZERO;
        BigDecimal frozen = wallet.getFrozenBalance() != null ? wallet.getFrozenBalance() : BigDecimal.ZERO;
        return total.subtract(frozen);
    }

    private void processCodDelivered(Order order) {
        if (order.getCodPaymentStatus() == CodPaymentStatus.PAID) {
            settlePrepaidCodDelivered(order);
            return;
        }

        if (order.getCodAmount() == null || order.getCodAmount().compareTo(BigDecimal.ZERO) <= 0) {
            order.setCodAmount(order.getTotalAmount());
        }

        if (order.getCodSettled() == null) {
            order.setCodSettled(false);
        }

        // Không cộng ví ở đây.
        // Shipper đã giao và đã thu tiền mặt từ buyer.
        // Admin đối soát COD xong mới cộng ví ở CodSettlementService.
    }

    private void settlePrepaidCodDelivered(Order order) {
        if (Boolean.TRUE.equals(order.getCodSettled())) {
            return;
        }
        BigDecimal prepaidAmount = resolveCodPrepaidAmount(order);
        BigDecimal platformFeeFromShop = prepaidAmount.multiply(BigDecimal.valueOf(0.1));
        BigDecimal finalShopRevenue = prepaidAmount.subtract(platformFeeFromShop);

        Wallet platformWallet = walletRepository.findByTypeForUpdate(WalletType.PLATFORM)
                .orElseThrow(() -> new AppException(ErrorCode.PLATFORM_WALLET_NOT_FOUND));
        Wallet shopWallet = walletRepository.findByShopOwnerIdForUpdate(order.getShopOwner().getId())
                .orElseThrow(() -> new AppException(ErrorCode.WALLET_NOT_FOUND));

        if (platformWallet.getFrozenBalance().compareTo(prepaidAmount) < 0) {
            throw new AppException(ErrorCode.PLATFORM_BALANCE_NOT_ENOUGH);
        }

        platformWallet.setFrozenBalance(platformWallet.getFrozenBalance().subtract(prepaidAmount));
        platformWallet.setCommissionWallet(platformWallet.getCommissionWallet().add(platformFeeFromShop));
        platformWallet.setTotalRevenueAllTime(platformWallet.getTotalRevenueAllTime().add(platformFeeFromShop));
        platformWallet.setUpdatedAt(LocalDateTime.now(ZoneId.of("Asia/Ho_Chi_Minh")));

        shopWallet.setTotalBalance(shopWallet.getTotalBalance().add(finalShopRevenue));
        shopWallet.setTotalRevenueAllTime(shopWallet.getTotalRevenueAllTime().add(finalShopRevenue));
        shopWallet.setUpdatedAt(LocalDateTime.now(ZoneId.of("Asia/Ho_Chi_Minh")));

        order.setCodSettled(true);
        order.setCodSettledAt(LocalDateTime.now(ZoneId.of("Asia/Ho_Chi_Minh")));
        order.setCodAmount(prepaidAmount);

        walletRepository.save(platformWallet);
        walletRepository.save(shopWallet);

        saveTransaction(TransactionType.PLATFORM_COMMISSION, platformFeeFromShop,
                platformWallet.getCommissionWallet(), "ORDER_" + order.getId(),
                "Ghi nhận phí nền tảng COD ứng trước đơn #" + order.getId());
        saveTransaction(TransactionType.SHOP_RELEASE, finalShopRevenue,
                shopWallet.getTotalBalance(), "ORDER_" + order.getId(),
                "Cộng tiền hàng COD cho shop sau giao thành công đơn #" + order.getId());
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
    @Transactional
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

        // Lấy kho bãi một lần, dùng chung cho toàn bộ stream
        Warehouse warehousePaged = getActiveWarehouse();

        List<AvailableOrderResponse> availableOrderResponses = availableOrders.stream()
                .map(order -> {
                    LatLong warehouseLocation = getOrGeocodeShopLocation(order);
                    LatLong buyerLocation     = getOrGeocodeShippingLocation(order);

                    Double shopToBuyerKm = null;
                    if (warehouseLocation != null && buyerLocation != null) {
                        double d = goongMapService.getDistanceKm(warehouseLocation, buyerLocation);
                        shopToBuyerKm = d == Double.MAX_VALUE ? null : d;
                    }

                    return AvailableOrderResponse.builder()
                            .orderId(order.getId())
                            // ── Thông tin kho bãi từ DB ──────────────────────────────────
                            .shopName(warehousePaged != null ? warehousePaged.getName() : "Kho bãi")
                            .shopAddress(warehousePaged != null ? warehousePaged.getAddress() : warehouseAddress)
                            .shopPhone(null)
                            .shippingAddress(order.getShippingAddress())
                            .recipientName(order.getRecipientName())
                            .recipientPhone(order.getRecipientPhone())
                            .shippingFee(order.getShippingFee() != null ? order.getShippingFee().multiply(BigDecimal.valueOf(0.9)) : BigDecimal.ZERO)
                            .totalAmount(order.getTotalAmount())
                            .paymentMethod(order.getPaymentMethod())
                            .codPrepaidAmount(resolveCodPrepaidAmount(order))
                            .codPaymentOption(order.getCodPaymentOption())
                            .codPaymentStatus(order.getCodPaymentStatus())
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