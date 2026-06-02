package com.swd301.foodmarket.service.impl;

import com.swd301.foodmarket.dto.response.OrderResponse;
import com.swd301.foodmarket.entity.*;
import com.swd301.foodmarket.enums.OrderStatus;
import com.swd301.foodmarket.enums.WalletType;
import com.swd301.foodmarket.exception.AppException;
import com.swd301.foodmarket.exception.ErrorCode;
import com.swd301.foodmarket.mapper.OrderMapper;
import com.swd301.foodmarket.repository.OrderRepository;
import com.swd301.foodmarket.repository.WalletRepository;
import com.swd301.foodmarket.service.CodSettlementService;
import com.swd301.foodmarket.service.NotificationService;
import jakarta.transaction.Transactional;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;

@Service
@RequiredArgsConstructor
@Slf4j
public class CodSettlementServiceImpl implements CodSettlementService {

    private final OrderRepository orderRepository;
    private final WalletRepository walletRepository;
    private final OrderMapper orderMapper;
    private final NotificationService notificationService;

    @Override
    @Transactional
    public void settleCodOrder(Integer orderId) {
        Order order = orderRepository.findById(orderId)
                .orElseThrow(() -> new AppException(ErrorCode.ORDER_NOT_FOUND));

        log.info("========== [COD SETTLEMENT START] Order #{} ==========", order.getId());

        if (!"COD".equalsIgnoreCase(order.getPaymentMethod())) {
            log.error("[COD_SETTLEMENT_INVALID_METHOD] orderId={}, paymentMethod={}",
                    order.getId(),
                    order.getPaymentMethod()
            );
            throw new AppException(ErrorCode.INVALID_PAYMENT_METHOD);
        }

        if (order.getStatus() != OrderStatus.DELIVERED) {
            log.error("[COD_SETTLEMENT_INVALID_STATUS] orderId={}, status={}",
                    order.getId(),
                    order.getStatus()
            );
            throw new AppException(ErrorCode.INVALID_ORDER_STATUS);
        }

        if (Boolean.TRUE.equals(order.getCodSettled())) {
            log.error("[COD_SETTLEMENT_ALREADY_SETTLED] orderId={}, codSettled={}, codSettledAt={}",
                    order.getId(),
                    order.getCodSettled(),
                    order.getCodSettledAt()
            );
            throw new AppException(ErrorCode.COD_ALREADY_SETTLED);
        }

        if (order.getShipper() == null) {
            log.error("[COD_SETTLEMENT_NO_SHIPPER] orderId={}", order.getId());
            throw new AppException(ErrorCode.SHIPPER_NOT_FOUND);
        }

        BigDecimal orderTotalAmount = order.getTotalAmount() != null
                ? order.getTotalAmount()
                : BigDecimal.ZERO;

        BigDecimal shippingFee = order.getShippingFee() != null
                ? order.getShippingFee()
                : BigDecimal.ZERO;

        BigDecimal refundAmount = order.getRefundAmount() != null
                ? order.getRefundAmount()
                : BigDecimal.ZERO;

        BigDecimal codAmount = order.getCodAmount() != null
                ? order.getCodAmount()
                : orderTotalAmount.subtract(refundAmount);

        if (codAmount.compareTo(BigDecimal.ZERO) < 0) {
            log.error("[COD_SETTLEMENT_INVALID_COD_AMOUNT] orderId={}, orderTotalAmount={}, refundAmount={}, codAmount={}",
                    order.getId(),
                    orderTotalAmount,
                    refundAmount,
                    codAmount
            );
            throw new AppException(ErrorCode.INVALID_AMOUNT);
        }

        BigDecimal shopGross = orderTotalAmount.subtract(shippingFee);

        if (refundAmount.compareTo(shopGross) > 0) {
            log.error("[COD_SETTLEMENT_INVALID_REFUND] orderId={}, refundAmount={}, shopGross={}",
                    order.getId(),
                    refundAmount,
                    shopGross
            );
            throw new AppException(ErrorCode.INVALID_REFUND_AMOUNT);
        }

// Phí sàn shop tính sau refund
        BigDecimal shopChargeable = shopGross.subtract(refundAmount);

        BigDecimal platformFeeFromShop = shopChargeable.multiply(BigDecimal.valueOf(0.1));
        BigDecimal platformFeeFromShip = shippingFee.multiply(BigDecimal.valueOf(0.1));
        BigDecimal platformFeeTotal = platformFeeFromShop.add(platformFeeFromShip);

        BigDecimal finalShopRevenue = shopChargeable.subtract(platformFeeFromShop);
        BigDecimal shipperNet = shippingFee.subtract(platformFeeFromShip);

        BigDecimal totalHeld = finalShopRevenue.add(shipperNet);
        log.info("[COD_ORDER_INFO] orderId={}, paymentMethod={}, status={}, codSettled={}, shipperId={}, shopOwnerId={}, buyerId={}",
                order.getId(),
                order.getPaymentMethod(),
                order.getStatus(),
                order.getCodSettled(),
                order.getShipper() != null ? order.getShipper().getId() : null,
                order.getShopOwner() != null ? order.getShopOwner().getId() : null,
                order.getBuyer() != null ? order.getBuyer().getId() : null
        );

        log.info("[COD_AMOUNT_INPUT] orderTotalAmount={}, shippingFee={}, refundAmount={}, codAmount={}",
                orderTotalAmount,
                shippingFee,
                refundAmount,
                codAmount
        );

        log.info("[COD_FORMULA] shopGross=orderTotalAmount-shippingFee => {} - {} = {}",
                orderTotalAmount,
                shippingFee,
                shopGross
        );

        log.info("[COD_PLATFORM_FEE] platformFeeFromShop=shopChargeable*10%={}, platformFeeFromShip=shippingFee*10%={}, platformFeeTotal={}",
                platformFeeFromShop,
                platformFeeFromShip,
                platformFeeTotal
        );

        log.info("[COD_NET_AMOUNT] shopChargeable=shopGross-refundAmount => {} - {} = {}, finalShopRevenue=shopChargeable-platformFeeFromShop => {} - {} = {}, shipperNet={}, totalHeld={}",
                shopGross,
                refundAmount,
                shopChargeable,
                shopChargeable,
                platformFeeFromShop,
                finalShopRevenue,
                shipperNet,
                totalHeld
        );
        log.info("[COD_EXPECTED_FLOW] platform.totalBalance + codAmount={}, platform.frozenBalance + totalHeld={}, shop.totalBalance + finalShopRevenue={}, shipper.totalBalance + shipperNet={}",
                codAmount,
                totalHeld,
                finalShopRevenue,
                shipperNet
        );

        Wallet platformWallet = walletRepository.findByTypeForUpdate(WalletType.PLATFORM)
                .orElseThrow(() -> new AppException(ErrorCode.PLATFORM_WALLET_NOT_FOUND));

        Wallet shopWallet = walletRepository.findByShopOwnerIdForUpdate(order.getShopOwner().getId())
                .orElseThrow(() -> new AppException(ErrorCode.WALLET_NOT_FOUND));

        Wallet shipperWallet = walletRepository.findByShipperIdForUpdate(order.getShipper().getId())
                .orElseThrow(() -> new AppException(ErrorCode.WALLET_NOT_FOUND));

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
        BigDecimal platformAvailableBefore = platformTotalBefore.subtract(platformFrozenBefore);
        BigDecimal shopAvailableBefore = shopTotalBefore.subtract(shopFrozenBefore);
        BigDecimal shipperAvailableBefore = shipperTotalBefore.subtract(shipperFrozenBefore);

        log.info("[COD_AVAILABLE_BEFORE] platformAvailable={}, shopAvailable={}, shipperAvailable={}",
                platformAvailableBefore,
                shopAvailableBefore,
                shipperAvailableBefore
        );
        log.info("[PLATFORM_WALLET_BEFORE] totalBalance={}, frozenBalance={}, commissionWallet={}, totalRevenueAllTime={}",
                platformTotalBefore,
                platformFrozenBefore,
                platformCommissionBefore,
                platformRevenueBefore
        );

        log.info("[SHOP_WALLET_BEFORE] shopOwnerId={}, totalBalance={}, frozenBalance={}, totalRevenueAllTime={}",
                order.getShopOwner().getId(),
                shopTotalBefore,
                shopFrozenBefore,
                shopRevenueBefore
        );

        log.info("[SHIPPER_WALLET_BEFORE] shipperId={}, totalBalance={}, frozenBalance={}, totalRevenueAllTime={}",
                order.getShipper().getId(),
                shipperTotalBefore,
                shipperFrozenBefore,
                shipperRevenueBefore
        );

        // Tiền thật admin/platform nhận từ shipper sau đối soát COD.
        platformWallet.setTotalBalance(
                platformWallet.getTotalBalance().add(codAmount)
        );

        // Platform đang giữ hộ shop + shipper.
        platformWallet.setFrozenBalance(
                platformWallet.getFrozenBalance().add(totalHeld)
        );

        platformWallet.setCommissionWallet(
                platformWallet.getCommissionWallet().add(platformFeeTotal)
        );

        platformWallet.setTotalRevenueAllTime(
                platformWallet.getTotalRevenueAllTime().add(platformFeeTotal)
        );

        // COD đã delivered + settled, tiền shop khả dụng luôn.
        shopWallet.setTotalBalance(
                shopWallet.getTotalBalance().add(finalShopRevenue)
        );

        shopWallet.setTotalRevenueAllTime(
                shopWallet.getTotalRevenueAllTime().add(finalShopRevenue)
        );

        // Shipper nhận phí ship khả dụng luôn.
        shipperWallet.setTotalBalance(
                shipperWallet.getTotalBalance().add(shipperNet)
        );

        shipperWallet.setTotalRevenueAllTime(
                shipperWallet.getTotalRevenueAllTime().add(shipperNet)
        );

        log.info("[PLATFORM_WALLET_AFTER] totalBalance={} -> {}, frozenBalance={} -> {}, commissionWallet={} -> {}, totalRevenueAllTime={} -> {}",
                platformTotalBefore,
                platformWallet.getTotalBalance(),
                platformFrozenBefore,
                platformWallet.getFrozenBalance(),
                platformCommissionBefore,
                platformWallet.getCommissionWallet(),
                platformRevenueBefore,
                platformWallet.getTotalRevenueAllTime()
        );

        log.info("[SHOP_WALLET_AFTER] shopOwnerId={}, totalBalance={} -> {}, frozenBalance={} -> {}, totalRevenueAllTime={} -> {}",
                order.getShopOwner().getId(),
                shopTotalBefore,
                shopWallet.getTotalBalance(),
                shopFrozenBefore,
                shopWallet.getFrozenBalance(),
                shopRevenueBefore,
                shopWallet.getTotalRevenueAllTime()
        );

        log.info("[SHIPPER_WALLET_AFTER] shipperId={}, totalBalance={} -> {}, frozenBalance={} -> {}, totalRevenueAllTime={} -> {}",
                order.getShipper().getId(),
                shipperTotalBefore,
                shipperWallet.getTotalBalance(),
                shipperFrozenBefore,
                shipperWallet.getFrozenBalance(),
                shipperRevenueBefore,
                shipperWallet.getTotalRevenueAllTime()
        );
        BigDecimal platformAvailableAfter = platformWallet.getTotalBalance()
                .subtract(platformWallet.getFrozenBalance());

        BigDecimal shopAvailableAfter = shopWallet.getTotalBalance()
                .subtract(shopWallet.getFrozenBalance());

        BigDecimal shipperAvailableAfter = shipperWallet.getTotalBalance()
                .subtract(shipperWallet.getFrozenBalance());

        log.info("[COD_AVAILABLE_AFTER] platformAvailable={} -> {}, shopAvailable={} -> {}, shipperAvailable={} -> {}",
                platformAvailableBefore,
                platformAvailableAfter,
                shopAvailableBefore,
                shopAvailableAfter,
                shipperAvailableBefore,
                shipperAvailableAfter
        );

        BigDecimal expectedPlatformTotalAfter = platformTotalBefore.add(codAmount);
        BigDecimal expectedPlatformFrozenAfter = platformFrozenBefore.add(totalHeld);
        BigDecimal expectedPlatformCommissionAfter = platformCommissionBefore.add(platformFeeTotal);
        BigDecimal expectedPlatformRevenueAfter = platformRevenueBefore.add(platformFeeTotal);
        BigDecimal expectedShopTotalAfter = shopTotalBefore.add(finalShopRevenue);
        BigDecimal expectedShopRevenueAfter = shopRevenueBefore.add(finalShopRevenue);
        BigDecimal expectedShipperTotalAfter = shipperTotalBefore.add(shipperNet);
        BigDecimal expectedShipperRevenueAfter = shipperRevenueBefore.add(shipperNet);

        log.info("[COD_VERIFY] expectedPlatformTotalAfter={}, actualPlatformTotalAfter={}, match={}",
                expectedPlatformTotalAfter,
                platformWallet.getTotalBalance(),
                expectedPlatformTotalAfter.compareTo(platformWallet.getTotalBalance()) == 0
        );

        log.info("[COD_VERIFY] expectedPlatformFrozenAfter={}, actualPlatformFrozenAfter={}, match={}",
                expectedPlatformFrozenAfter,
                platformWallet.getFrozenBalance(),
                expectedPlatformFrozenAfter.compareTo(platformWallet.getFrozenBalance()) == 0
        );

        log.info("[COD_VERIFY] expectedPlatformCommissionAfter={}, actualPlatformCommissionAfter={}, match={}",
                expectedPlatformCommissionAfter,
                platformWallet.getCommissionWallet(),
                expectedPlatformCommissionAfter.compareTo(platformWallet.getCommissionWallet()) == 0
        );

        log.info("[COD_VERIFY] expectedPlatformRevenueAfter={}, actualPlatformRevenueAfter={}, match={}",
                expectedPlatformRevenueAfter,
                platformWallet.getTotalRevenueAllTime(),
                expectedPlatformRevenueAfter.compareTo(platformWallet.getTotalRevenueAllTime()) == 0
        );

        log.info("[COD_VERIFY] expectedShopTotalAfter={}, actualShopTotalAfter={}, match={}",
                expectedShopTotalAfter,
                shopWallet.getTotalBalance(),
                expectedShopTotalAfter.compareTo(shopWallet.getTotalBalance()) == 0
        );

        log.info("[COD_VERIFY] expectedShopRevenueAfter={}, actualShopRevenueAfter={}, match={}",
                expectedShopRevenueAfter,
                shopWallet.getTotalRevenueAllTime(),
                expectedShopRevenueAfter.compareTo(shopWallet.getTotalRevenueAllTime()) == 0
        );

        log.info("[COD_VERIFY] expectedShipperTotalAfter={}, actualShipperTotalAfter={}, match={}",
                expectedShipperTotalAfter,
                shipperWallet.getTotalBalance(),
                expectedShipperTotalAfter.compareTo(shipperWallet.getTotalBalance()) == 0
        );

        log.info("[COD_VERIFY] expectedShipperRevenueAfter={}, actualShipperRevenueAfter={}, match={}",
                expectedShipperRevenueAfter,
                shipperWallet.getTotalRevenueAllTime(),
                expectedShipperRevenueAfter.compareTo(shipperWallet.getTotalRevenueAllTime()) == 0
        );

        order.setCodAmount(codAmount);
        order.setCodSettled(true);
        order.setCodSettledAt(LocalDateTime.now());

        walletRepository.save(platformWallet);
        walletRepository.save(shopWallet);
        walletRepository.save(shipperWallet);
        orderRepository.save(order);

        log.info("[COD_SETTLEMENT_SAVED] orderId={}, codAmount={}, codSettled={}, codSettledAt={}",
                order.getId(),
                order.getCodAmount(),
                order.getCodSettled(),
                order.getCodSettledAt()
        );

        Notification shopNotification = Notification.builder()
                .title("Đối soát COD thành công #" + order.getId())
                .message("Đơn hàng COD #" + order.getId()
                        + " đã được đối soát. Bạn nhận được "
                        + finalShopRevenue + "đ.")
                .receiverType("USER_" + order.getShopOwner().getId())
                .build();

        notificationService.createUserNotification(order.getShopOwner(), shopNotification);

        Notification shipperNotification = Notification.builder()
                .title("Đối soát COD thành công #" + order.getId())
                .message("Đơn hàng COD #" + order.getId()
                        + " đã được đối soát. Bạn nhận được "
                        + shipperNet + "đ phí giao hàng.")
                .receiverType("USER_" + order.getShipper().getId())
                .build();

        notificationService.createUserNotification(order.getShipper(), shipperNotification);

        log.info("========== [COD SETTLEMENT END] Order #{} ==========", order.getId());
    }

    @Override
    public List<OrderResponse> getPendingCodSettlementOrders() {
        return orderRepository
                .findByPaymentMethodIgnoreCaseAndStatusAndCodSettled(
                        "COD",
                        OrderStatus.DELIVERED,
                        false
                )
                .stream()
                .map(orderMapper::toResponse)
                .toList();
    }
}