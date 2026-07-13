package com.swd301.foodmarket.service.impl;

import com.swd301.foodmarket.dto.response.OrderResponse;
import com.swd301.foodmarket.entity.*;
import com.swd301.foodmarket.enums.CodPaymentOption;
import com.swd301.foodmarket.enums.CodPaymentStatus;
import com.swd301.foodmarket.enums.OrderStatus;
import com.swd301.foodmarket.enums.TransactionStatus;
import com.swd301.foodmarket.enums.TransactionType;
import com.swd301.foodmarket.enums.WalletType;
import com.swd301.foodmarket.exception.AppException;
import com.swd301.foodmarket.exception.ErrorCode;
import com.swd301.foodmarket.mapper.OrderMapper;
import com.swd301.foodmarket.repository.OrderRepository;
import com.swd301.foodmarket.repository.TransactionRepository;
import com.swd301.foodmarket.repository.WalletRepository;
import com.swd301.foodmarket.service.CodSettlementService;
import com.swd301.foodmarket.service.NotificationService;
import com.swd301.foodmarket.service.support.MoneyRoundingUtil;
import com.swd301.foodmarket.service.support.OrderSettlementCalculator;
import jakarta.transaction.Transactional;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.util.ArrayList;
import java.util.List;

@Service
@RequiredArgsConstructor
@Slf4j
public class CodSettlementServiceImpl implements CodSettlementService {

    private final OrderRepository orderRepository;
    private final WalletRepository walletRepository;
    private final TransactionRepository transactionRepository;
    private final OrderMapper orderMapper;
    private final NotificationService notificationService;
    private final OrderSettlementCalculator orderSettlementCalculator;

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

        OrderSettlementCalculator.SettlementAmounts amounts = orderSettlementCalculator.calculate(order);
        BigDecimal orderTotalAmount = amounts.buyerPaid();
        BigDecimal shippingDiscountAmount = amounts.shippingDiscountAmount();
        BigDecimal platformBatchSubsidy = amounts.platformBatchSubsidy();
        BigDecimal settlementTotal = amounts.settlementTotal();
        BigDecimal shippingFee = amounts.shippingFee();
        BigDecimal shopGross = amounts.shopGross();

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

        BigDecimal platformFeeFromShop = MoneyRoundingUtil.commission(shopChargeable);
        BigDecimal platformFeeFromShip = MoneyRoundingUtil.commission(shippingFee);
        BigDecimal platformFeeTotal = platformFeeFromShop.add(platformFeeFromShip);

        BigDecimal finalShopRevenue = shopChargeable.subtract(platformFeeFromShop);
        BigDecimal shipperNet = MoneyRoundingUtil.netAfterCommission(shippingFee);

        log.info("[COD_NET_AMOUNT] shopChargeable={}, finalShopRevenue={}, shipperNet={}",
                shopChargeable, finalShopRevenue, shipperNet);
        log.info("[COD_EXPECTED_FLOW] platform.totalBalance + codAmount - shop - shipper, shop.totalBalance + finalShopRevenue={}, shipper.totalBalance + shipperNet={}",
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

        // Tiền thật admin/platform nhận từ shipper sau đối soát COD, trừ phần đã chi cho shop/shipper.
        platformWallet.setTotalBalance(
                platformWallet.getTotalBalance()
                        .add(codAmount)
                        .subtract(platformBatchSubsidy)
                        .subtract(finalShopRevenue)
                        .subtract(shipperNet)
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

        BigDecimal expectedPlatformTotalAfter = platformTotalBefore
                .add(codAmount)
                .subtract(platformBatchSubsidy)
                .subtract(finalShopRevenue)
                .subtract(shipperNet);
        BigDecimal expectedPlatformFrozenAfter = platformFrozenBefore;
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
        order.setCodSettledAt(LocalDateTime.now(ZoneId.of("Asia/Ho_Chi_Minh")));

        walletRepository.save(platformWallet);
        walletRepository.save(shopWallet);
        walletRepository.save(shipperWallet);
        orderRepository.save(order);

        saveTransaction(TransactionType.COD_RECEIVED, codAmount,
                platformWallet.getTotalBalance(), "ORDER_" + order.getId(),
                "Đối soát nhận tiền COD đơn #" + order.getId());
        saveTransaction(TransactionType.PLATFORM_COMMISSION, platformFeeTotal,
                platformWallet.getCommissionWallet(), "ORDER_" + order.getId(),
                "Ghi nhận phí nền tảng COD đơn #" + order.getId());
        saveTransaction(TransactionType.SHOP_RELEASE, finalShopRevenue,
                shopWallet.getTotalBalance(), "ORDER_" + order.getId(),
                "Cộng doanh thu shop COD đơn #" + order.getId());
        saveTransaction(TransactionType.SHIPPER_PAYOUT, shipperNet,
                shipperWallet.getTotalBalance(), "ORDER_" + order.getId(),
                "Cộng phí giao hàng shipper COD đơn #" + order.getId());
        if (shippingDiscountAmount.compareTo(BigDecimal.ZERO) > 0) {
            saveTransaction(TransactionType.SHIPPING_SUBSIDY, shippingDiscountAmount.negate(),
                    platformWallet.getTotalBalance(), "ORDER_" + order.getId(),
                    "Admin/platform bù phí vận chuyển COD cho shipper đơn #" + order.getId());
        }
        if (platformBatchSubsidy.compareTo(BigDecimal.ZERO) > 0) {
            saveTransaction(TransactionType.PLATFORM_VOUCHER_SUBSIDY, platformBatchSubsidy.negate(),
                    platformWallet.getTotalBalance(), "ORDER_" + order.getId(),
                    "Admin/platform bù voucher sàn COD đơn #" + order.getId());
        }

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
    @Transactional
    public void confirmShipperCashPayment(Integer orderId) {
        Order order = orderRepository.findById(orderId)
                .orElseThrow(() -> new AppException(ErrorCode.ORDER_NOT_FOUND));
        if (!"COD".equalsIgnoreCase(order.getPaymentMethod())
                || order.getCodPaymentOption() != CodPaymentOption.CASH
                || order.getCodPaymentStatus() != CodPaymentStatus.PENDING_ADMIN_CONFIRM
                || order.getShipper() == null) {
            throw new AppException(ErrorCode.INVALID_ORDER_STATUS);
        }

        BigDecimal prepaidAmount = resolveCodPrepaidAmount(order);
        Wallet platformWallet = walletRepository.findByTypeForUpdate(WalletType.PLATFORM)
                .orElseThrow(() -> new AppException(ErrorCode.PLATFORM_WALLET_NOT_FOUND));

        platformWallet.setTotalBalance(platformWallet.getTotalBalance().add(prepaidAmount));
        platformWallet.setFrozenBalance(platformWallet.getFrozenBalance().add(prepaidAmount));
        platformWallet.setUpdatedAt(LocalDateTime.now(ZoneId.of("Asia/Ho_Chi_Minh")));

        order.setCodPrepaidAmount(prepaidAmount);
        order.setCodPaymentStatus(CodPaymentStatus.PAID);
        order.setShipperPaidAt(LocalDateTime.now(ZoneId.of("Asia/Ho_Chi_Minh")));
        order.setCodCashConfirmedAt(LocalDateTime.now(ZoneId.of("Asia/Ho_Chi_Minh")));

        walletRepository.save(platformWallet);
        orderRepository.save(order);

        saveTransaction(TransactionType.COD_SHIPPER_CASH_CONFIRMED, prepaidAmount,
                platformWallet.getFrozenBalance(), "ORDER_" + order.getId(),
                "Admin xác nhận đã nhận tiền mặt COD gồm tiền hàng và phí nền tảng từ phí ship từ shipper cho đơn #" + order.getId());
    }

    @Override
    @Transactional
    public void refundCodPrepaymentToShipper(Integer orderId) {
        Order order = orderRepository.findById(orderId)
                .orElseThrow(() -> new AppException(ErrorCode.ORDER_NOT_FOUND));
        if (!"COD".equalsIgnoreCase(order.getPaymentMethod())
                || order.getStatus() != OrderStatus.FAILED
                || order.getCodPaymentStatus() != CodPaymentStatus.REFUND_PENDING
                || order.getShipper() == null) {
            throw new AppException(ErrorCode.INVALID_ORDER_STATUS);
        }

        BigDecimal prepaidAmount = order.getCodPrepaidAmount() != null
                ? order.getCodPrepaidAmount()
                : resolveCodPrepaidAmount(order);
        Wallet platformWallet = walletRepository.findByTypeForUpdate(WalletType.PLATFORM)
                .orElseThrow(() -> new AppException(ErrorCode.PLATFORM_WALLET_NOT_FOUND));
        Wallet shipperWallet = walletRepository.findByShipperIdForUpdate(order.getShipper().getId())
                .orElseThrow(() -> new AppException(ErrorCode.WALLET_NOT_FOUND));

        if (platformWallet.getFrozenBalance().compareTo(prepaidAmount) < 0
                || platformWallet.getTotalBalance().compareTo(prepaidAmount) < 0) {
            throw new AppException(ErrorCode.PLATFORM_BALANCE_NOT_ENOUGH);
        }

        platformWallet.setFrozenBalance(platformWallet.getFrozenBalance().subtract(prepaidAmount));
        platformWallet.setTotalBalance(platformWallet.getTotalBalance().subtract(prepaidAmount));
        platformWallet.setUpdatedAt(LocalDateTime.now(ZoneId.of("Asia/Ho_Chi_Minh")));
        shipperWallet.setTotalBalance(shipperWallet.getTotalBalance().add(prepaidAmount));
        shipperWallet.setUpdatedAt(LocalDateTime.now(ZoneId.of("Asia/Ho_Chi_Minh")));

        order.setCodPaymentStatus(CodPaymentStatus.REFUNDED);
        order.setCodRefundedAt(LocalDateTime.now(ZoneId.of("Asia/Ho_Chi_Minh")));

        walletRepository.save(platformWallet);
        walletRepository.save(shipperWallet);
        orderRepository.save(order);

        saveTransaction(TransactionType.COD_SHIPPER_REFUND, prepaidAmount,
                shipperWallet.getTotalBalance(), "ORDER_" + order.getId(),
                "Admin hoàn tiền COD đã ứng cho shipper vì giao thất bại đơn #" + order.getId());
    }

    @Override
    public List<OrderResponse> getPendingCodSettlementOrders() {
        List<Order> orders = new ArrayList<>();
        orders.addAll(orderRepository.findByPaymentMethodIgnoreCaseAndCodPaymentStatus(
                "COD", CodPaymentStatus.PENDING_ADMIN_CONFIRM));
        orders.addAll(orderRepository.findByPaymentMethodIgnoreCaseAndCodPaymentStatus(
                "COD", CodPaymentStatus.REFUND_PENDING));
        orders.addAll(orderRepository.findByPaymentMethodIgnoreCaseAndStatusAndCodSettled(
                "COD", OrderStatus.DELIVERED, false));

        return orders.stream()
                .distinct()
                .map(orderMapper::toResponse)
                .toList();
    }

    private void saveTransaction(TransactionType type, BigDecimal amount, BigDecimal balanceAfter, String referenceId, String content) {
        transactionRepository.save(Transaction.builder()
                .paymentGateway("COD")
                .transactionType(type)
                .referenceId(referenceId)
                .amount(amount)
                .balanceAfter(balanceAfter)
                .status(TransactionStatus.SUCCESS)
                .content(content)
                .build());
    }

    private BigDecimal resolveCodPrepaidAmount(Order order) {
        OrderSettlementCalculator.SettlementAmounts amounts = orderSettlementCalculator.calculate(order);
        BigDecimal refund = order.getRefundAmount() != null ? order.getRefundAmount() : BigDecimal.ZERO;
        BigDecimal shipping = amounts.shippingFee();
        BigDecimal shopChargeable = amounts.shopGross().subtract(refund);
        if (shopChargeable.compareTo(BigDecimal.ZERO) < 0) {
            shopChargeable = BigDecimal.ZERO;
        }
        BigDecimal platformFeeFromShip = shipping.multiply(BigDecimal.valueOf(0.1));
        BigDecimal amount = shopChargeable.add(platformFeeFromShip);
        return amount.compareTo(BigDecimal.ZERO) < 0 ? BigDecimal.ZERO : amount;
    }
}