package com.swd301.foodmarket.service.impl;

import com.swd301.foodmarket.dto.request.CreatePaymentRequest;
import com.swd301.foodmarket.dto.response.PayAllOption;
import com.swd301.foodmarket.dto.response.PayOSPaymentInfoResponse;
import com.swd301.foodmarket.dto.response.PaymentResponse;
import com.swd301.foodmarket.dto.response.ResumePaymentOptionsResponse;
import com.swd301.foodmarket.dto.response.ResumePaymentOrderOption;
import com.swd301.foodmarket.entity.*;
import com.swd301.foodmarket.entity.Notification;
import com.swd301.foodmarket.enums.OrderStatus;
import com.swd301.foodmarket.enums.PaymentStatus;
import com.swd301.foodmarket.enums.CheckoutBatchStatus;
import com.swd301.foodmarket.enums.TransactionStatus;
import com.swd301.foodmarket.enums.TransactionType;
import com.swd301.foodmarket.enums.WalletType;
import com.swd301.foodmarket.exception.AppException;
import com.swd301.foodmarket.exception.ErrorCode;
import com.swd301.foodmarket.mapper.PaymentMapper;
import com.swd301.foodmarket.repository.MysteryBoxRepository;
import com.swd301.foodmarket.repository.OrderRepository;
import com.swd301.foodmarket.repository.CheckoutBatchRepository;
import com.swd301.foodmarket.repository.PaymentRepository;
import com.swd301.foodmarket.repository.ProductRepository;
import com.swd301.foodmarket.repository.TransactionRepository;
import com.swd301.foodmarket.repository.WalletRepository;
import com.swd301.foodmarket.service.CartService;
import com.swd301.foodmarket.service.NotificationService;
import com.swd301.foodmarket.service.OrderRewardService;
import com.swd301.foodmarket.service.PaymentService;
import com.swd301.foodmarket.service.PayosService;
import com.swd301.foodmarket.service.VoucherUserService;
import com.swd301.foodmarket.service.support.OrderSettlementCalculator;
import jakarta.transaction.Transactional;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
@Slf4j
public class PaymentServiceImpl implements PaymentService {

    private final PaymentRepository paymentRepository;
    private final OrderRepository orderRepository;
    private final PaymentMapper paymentMapper;
    private final PayosService payosService;
    private final TransactionRepository transactionRepository;
    private final WalletRepository walletRepository;
    private final VoucherUserService voucherUserService;
    private final CartService cartService;
    private final ProductRepository productRepository;
    private final MysteryBoxRepository mysteryBoxRepository;
    private final OrderRewardService orderRewardService;
    private final NotificationService notificationService;
    private final CheckoutBatchRepository checkoutBatchRepository;
    private final OrderSettlementCalculator orderSettlementCalculator;

    @Override
    public PaymentResponse createPayment(CreatePaymentRequest request) {

        Order order = orderRepository.findById(request.getOrderId())
                .orElseThrow(() -> new AppException(ErrorCode.ORDER_NOT_FOUND));

        String gateway = request.getPaymentGateway();
        if (gateway == null) gateway = request.getMethod();
        if (gateway == null) gateway = order.getPaymentMethod();
        if (gateway == null) gateway = "PAYOS"; // Default

        Payment payment = Payment.builder()
                .order(order)
                .amount(order.getTotalAmount())
                .paymentGateway(gateway)
                .status(PaymentStatus.PENDING)
                .paymentDate(LocalDateTime.now(ZoneId.of("Asia/Ho_Chi_Minh")))
                .createdAt(LocalDateTime.now(ZoneId.of("Asia/Ho_Chi_Minh")))
                .build();

        paymentRepository.save(payment);

        return paymentMapper.toResponse(payment);
    }

    @Override
    public PaymentResponse getById(Integer id) {
        Payment payment = paymentRepository.findById(id)
                .orElseThrow(() -> new AppException(ErrorCode.PAYMENT_NOT_FOUND));
        return paymentMapper.toResponse(payment);
    }

    @Override
    public PaymentResponse getByIdOrOrderCode(String idOrOrderCode) {
        Payment payment;
        try {
            Integer id = Integer.parseInt(idOrOrderCode);
            payment = paymentRepository.findById(id)
                    .orElse(null);
        } catch (NumberFormatException e) {
            payment = null;
        }

        if (payment == null) {
            payment = paymentRepository.findByPayosOrderCode(idOrOrderCode)
                    .orElse(null);
        }

        if (payment != null) {
            return toOrderPaymentResponse(payment);
        }

        CheckoutBatch batch = checkoutBatchRepository.findByPayosOrderCode(idOrOrderCode)
                .orElseThrow(() -> new AppException(ErrorCode.PAYMENT_NOT_FOUND));

        return buildBatchPaymentResponse(batch, null);
    }

    @Override
    public PaymentResponse updateStatus(Integer id, String status) {
        PaymentStatus nextStatus = PaymentStatus.valueOf(status);
        if (nextStatus == PaymentStatus.SUCCESS) {
            // SUCCESS chỉ được set qua webhook PayOS hoặc /confirm đã verify PayOS API
            throw new AppException(ErrorCode.PAYMENT_STATUS_UPDATE_FORBIDDEN);
        }

        Payment payment = paymentRepository.findById(id)
                .orElseThrow(() -> new AppException(ErrorCode.PAYMENT_NOT_FOUND));

        payment.setStatus(nextStatus);
        payment.setUpdatedAt(LocalDateTime.now(ZoneId.of("Asia/Ho_Chi_Minh")));

        paymentRepository.save(payment);

        return paymentMapper.toResponse(payment);
    }

    @Transactional
    public PaymentResponse createPaymentWithPayOS(CreatePaymentRequest request) {

        Order order = orderRepository.findById(request.getOrderId())
                .orElseThrow(() -> new AppException(ErrorCode.ORDER_NOT_FOUND));

        if (order.getStatus() != OrderStatus.PENDING && order.getStatus() != OrderStatus.AWAITING_PAYMENT) {
            throw new AppException(ErrorCode.ORDER_NOT_PAYABLE);
        }

        CheckoutBatch checkoutBatch = order.getCheckoutBatch();
        if (checkoutBatch != null
                && checkoutBatch.getStatus() != CheckoutBatchStatus.CANCELLED
                && checkoutBatch.getBatchVoucherUserId() != null) {
            throw new AppException(ErrorCode.ORDER_IN_BATCH_USE_BATCH_PAYMENT);
        }

        if (paymentRepository.existsByOrder_IdAndStatus(order.getId(), PaymentStatus.PENDING)
                || paymentRepository.existsByOrder_IdAndStatus(order.getId(), PaymentStatus.SUCCESS)) {
            throw new AppException(ErrorCode.ORDER_NOT_PAYABLE);
        }

        BigDecimal amount = order.getTotalAmount();

        String gateway = request.getPaymentGateway();
        if (gateway == null) gateway = request.getMethod();
        if (gateway == null) gateway = "PAYOS";

        // 🔥 TẠO PAYMENT TRƯỚC
        Payment payment = paymentMapper.toEntity(request);
        payment.setOrder(order);
        payment.setAmount(amount);
        payment.setPaymentGateway(gateway);
        payment.setStatus(PaymentStatus.PENDING);
        payment.setCreatedAt(LocalDateTime.now(ZoneId.of("Asia/Ho_Chi_Minh")));
        payment.setUpdatedAt(LocalDateTime.now(ZoneId.of("Asia/Ho_Chi_Minh")));

        paymentRepository.save(payment);

        Integer paymentId = payment.getId();

        // 🔥 tạo orderCode unique để tránh trùng PayOS
        long orderCode = System.currentTimeMillis() + paymentId;

        PayosPayoutResult qr = payosService.createOrderPaymentQr(
                amount,
                orderCode,
                order.getId(),
                order.getBuyer().getFullName()
        );

        if (!qr.isSuccess()) {
            throw new AppException(ErrorCode.PAYOS_CREATE_QR_FAILED);
        }

        // 🔥 update lại payment
        payment.setPayosOrderCode(String.valueOf(orderCode));
        payment.setCheckoutUrl(qr.getCheckoutUrl());
        payment.setQrCodeUrl(qr.getQrCodeUrl());
        payment.setUpdatedAt(LocalDateTime.now(ZoneId.of("Asia/Ho_Chi_Minh")));

        // Đánh dấu order đang trong quá trình thanh toán
        order.setStatus(OrderStatus.AWAITING_PAYMENT);
        orderRepository.save(order);

        paymentRepository.save(payment);

        if (checkoutBatch != null
                && checkoutBatch.getBatchVoucherUserId() == null
                && checkoutBatch.getStatus() != CheckoutBatchStatus.CANCELLED) {
            invalidateBatchPayosLink(checkoutBatch);
        }

        return toOrderPaymentResponse(payment);
    }


    /**
     * Client return-URL confirm: không tin FE/APP.
     * Chỉ settle khi PayOS API xác nhận link đã PAID và số tiền khớp.
     */
    @Transactional
    public void confirmPaymentByOrderCode(String orderCode) {
        confirmPaymentFromClient(orderCode, null);
    }

    @Override
    @Transactional
    public void confirmPaymentFromClient(String orderCode, String paymentLinkId) {
        // Webhook có thể đã settle trước — không cần gọi PayOS lại
        if (isAlreadySettled(orderCode)) {
            log.info("Payment already settled locally, skip PayOS verify. orderCode={}", orderCode);
            return;
        }
        Long paidAmount = requirePaidAmountFromPayOS(orderCode, paymentLinkId);
        confirmPaymentByOrderCode(orderCode, paidAmount);
    }

    private boolean isAlreadySettled(String orderCode) {
        var batchOpt = checkoutBatchRepository.findByPayosOrderCode(orderCode);
        if (batchOpt.isPresent()) {
            return batchOpt.get().getStatus() == CheckoutBatchStatus.PAID;
        }
        return paymentRepository.findByPayosOrderCode(orderCode)
                .map(p -> p.getStatus() == PaymentStatus.SUCCESS)
                .orElse(false);
    }

    /**
     * Webhook / internal confirm — caller phải đã verify chữ ký PayOS (webhook)
     * hoặc đã lấy amount từ PayOS API (client confirm).
     */
    @Transactional
    public void confirmPaymentByOrderCode(String orderCode, Long verifiedAmount) {

        log.info("===== CONFIRM PAYMENT START =====");
        log.info("OrderCode received: {}, verifiedAmount={}", orderCode, verifiedAmount);

        var batchOpt = checkoutBatchRepository.findByPayosOrderCodeForUpdate(orderCode);
        if (batchOpt.isPresent()) {
            confirmBatchPayment(batchOpt.get(), verifiedAmount);
            return;
        }

        Payment payment = paymentRepository.findByOrderCodeForUpdate(orderCode)
                .orElseThrow(() -> new AppException(ErrorCode.PAYMENT_NOT_FOUND));

        log.info("Payment found: id={}, status={}", payment.getId(), payment.getStatus());

        // tránh chạy 2 lần
        if (payment.getStatus() == PaymentStatus.SUCCESS) {
            log.warn("Payment already SUCCESS → skip processing. OrderCode={}", orderCode);
            return;
        }

        Order order = payment.getOrder();

        if (!"PAYOS".equalsIgnoreCase(order.getPaymentMethod())) {
            throw new AppException(ErrorCode.INVALID_PAYMENT_METHOD);
        }

        if (order.getStatus() != OrderStatus.PENDING && order.getStatus() != OrderStatus.AWAITING_PAYMENT) {
            log.warn("Order is no longer payable. orderId={}, status={}", order.getId(), order.getStatus());
            throw new AppException(ErrorCode.ORDER_NOT_PAYABLE);
        }

        verifyPaidAmount(verifiedAmount, payment.getAmount());

        log.info("Order found: id={}, status={}", order.getId(), order.getStatus());

        payment.setStatus(PaymentStatus.SUCCESS);
        payment.setPaymentDate(LocalDateTime.now(ZoneId.of("Asia/Ho_Chi_Minh")));
        payment.setUpdatedAt(LocalDateTime.now(ZoneId.of("Asia/Ho_Chi_Minh")));

        settlePayosOrder(order, payment.getAmount(), payment);

        paymentRepository.save(payment);

        if (order.getCheckoutBatch() != null) {
            syncBatchStatusAfterPayment(order.getCheckoutBatch());
        }

        log.info("===== CONFIRM PAYMENT COMPLETED =====");
    }

    private void confirmBatchPayment(CheckoutBatch batch, Long verifiedAmount) {
        if (batch.getStatus() == CheckoutBatchStatus.PAID) {
            log.warn("Batch already PAID → skip. batchId={}", batch.getId());
            return;
        }

        verifyPaidAmount(verifiedAmount, batch.getTotalAmount());

        List<Order> orders = orderRepository.findByCheckoutBatchId(batch.getId());
        for (Order order : orders) {
            if (!"PAYOS".equalsIgnoreCase(order.getPaymentMethod())) {
                continue;
            }
            if (order.getStatus() != OrderStatus.PENDING && order.getStatus() != OrderStatus.AWAITING_PAYMENT) {
                log.warn("Batch order not payable. orderId={}, status={}", order.getId(), order.getStatus());
                continue;
            }
            settlePayosOrder(order, order.getTotalAmount(), null);
        }

        syncBatchStatusAfterPayment(batch);
        log.info("===== BATCH CONFIRM PAYMENT COMPLETED batchId={} =====", batch.getId());
    }

    private void settlePayosOrder(Order order, BigDecimal totalAmount, Payment payment) {
        order.setStatus(OrderStatus.PAID);
        orderRewardService.applyRewardIfEligible(order);
        orderRepository.save(order);

        log.info("Order status updated to PAID: orderId={}", order.getId());

        OrderSettlementCalculator.SettlementAmounts amounts = orderSettlementCalculator.calculate(order);
        BigDecimal shippingFee = amounts.shippingFee();
        BigDecimal shippingDiscountAmount = amounts.shippingDiscountAmount();
        BigDecimal platformBatchSubsidy = amounts.platformBatchSubsidy();
        BigDecimal settlementTotal = amounts.settlementTotal();
        BigDecimal shopGross = amounts.shopGross();
        BigDecimal buyerPaid = amounts.buyerPaid();

        log.info("[PAYOS_PAYMENT_SUCCESS] orderId={}, buyerPaid={}, shippingFee={}, shippingDiscountAmount={}, platformBatchSubsidy={}, settlementTotal={}, shopGross={}",
                order.getId(),
                buyerPaid,
                shippingFee,
                shippingDiscountAmount,
                platformBatchSubsidy,
                settlementTotal,
                shopGross
        );

        log.info("Total amount: {}", buyerPaid);
        log.info("Shipping fee: {}", shippingFee);
        log.info("Shop gross: {}", shopGross);
        
//        // Phí nền tảng 10% cho toàn bộ đơn hàng
//        BigDecimal platformFeeTotal = totalAmount.multiply(BigDecimal.valueOf(0.1));
//
//        // Phí lấy từ phía Shop (10% của shopGross)
//        BigDecimal platformFeeFromShop = shopGross.multiply(BigDecimal.valueOf(0.1));
//
//        // Phí lấy từ phía Ship (10% của shippingFee)
//        BigDecimal platformFeeFromShip = shippingFee.multiply(BigDecimal.valueOf(0.1));
//
//        // Tiền thực nhận
//        BigDecimal shopNet = shopGross.subtract(platformFeeFromShop);
//        BigDecimal shipperNet = shippingFee.subtract(platformFeeFromShip);
//
//        log.info("Platform Fee Total (10%): {}", platformFeeTotal);
//        log.info("Shop Net: {}", shopNet);
//        log.info("Shipper Net: {}", shipperNet);

        // ======================
        // ADMIN WALLET
        // ======================

        Wallet adminWallet = walletRepository.findByTypeForUpdate(WalletType.PLATFORM)
                .orElseThrow(() -> new AppException(ErrorCode.PLATFORM_WALLET_NOT_FOUND));

        adminWallet.setTotalBalance(
                adminWallet.getTotalBalance().add(buyerPaid).subtract(platformBatchSubsidy)
        );

// Khóa toàn bộ tiền PayOS đang nằm trong hệ thống.
// Đến DELIVERED mới tách: shop, shipper, commission, refund.
        adminWallet.setFrozenBalance(
                adminWallet.getFrozenBalance().add(buyerPaid)
        );

        walletRepository.save(adminWallet);

        log.info("[PAYOS_PLATFORM_AFTER_PAYMENT] totalBalance={}, frozenBalance={}, commissionWallet={}, totalRevenueAllTime={}",
                adminWallet.getTotalBalance(),
                adminWallet.getFrozenBalance(),
                adminWallet.getCommissionWallet(),
                adminWallet.getTotalRevenueAllTime()
        );
        // ======================
        // SHOP WALLET
        // ======================

        User shop = order.getShopOwner();

        Wallet shopWallet = walletRepository.findByShopOwnerIdForUpdate(shop.getId())
                .orElseThrow(() -> new AppException(ErrorCode.WALLET_NOT_FOUND));

// Ghi nhận tiền hàng gốc cho shop nhưng khóa lại.
// Đến DELIVERED mới trừ refund + phí sàn.
        shopWallet.setTotalBalance(
                shopWallet.getTotalBalance().add(shopGross)
        );

        shopWallet.setFrozenBalance(
                shopWallet.getFrozenBalance().add(shopGross)
        );

        shopWallet.setUpdatedAt(LocalDateTime.now(ZoneId.of("Asia/Ho_Chi_Minh")));

        walletRepository.save(shopWallet);

        log.info("[PAYOS_SHOP_AFTER_PAYMENT] shopOwnerId={}, totalBalance={}, frozenBalance={}",
                shop.getId(),
                shopWallet.getTotalBalance(),
                shopWallet.getFrozenBalance()
        );
        // ======================
        // SHIPPER WALLET
        // ======================

//        if (order.getShipper() != null) {
//            Wallet shipperWallet = walletRepository.findByShipperIdForUpdate(order.getShipper().getId())
//                    .orElseThrow(() -> new AppException(ErrorCode.WALLET_NOT_FOUND));
//
//            log.info("Shipper wallet BEFORE frozen balance: {}", shipperWallet.getFrozenBalance());
//
//            shipperWallet.setFrozenBalance(
//                    shipperWallet.getFrozenBalance().add(shipperNet)
//            );
//
////            shipperWallet.setTotalRevenueAllTime(
////                    shipperWallet.getTotalRevenueAllTime().add(shipperNet)
////            );
//
//            shipperWallet.setUpdatedAt(LocalDateTime.now(ZoneId.of("Asia/Ho_Chi_Minh")));
//
//            walletRepository.save(shipperWallet);
//
//            log.info("Shipper wallet AFTER frozen balance: {}", shipperWallet.getFrozenBalance());
//        }

        // ======================
        // TRANSACTION LOG
        // ======================

        Transaction transaction = Transaction.builder()
                .payment(payment)
                .paymentGateway(payment != null ? payment.getPaymentGateway() : "PAYOS")
                .transactionType(TransactionType.PAYMENT_RECEIVED)
                .referenceId("ORDER_" + order.getId())
                .amount(buyerPaid)
                .balanceAfter(adminWallet.getTotalBalance())
                .status(TransactionStatus.SUCCESS)
                .content("Thanh toán đơn hàng #" + order.getId())
                .build();

        transactionRepository.save(transaction);

        transactionRepository.save(Transaction.builder()
                .payment(payment)
                .paymentGateway(payment != null ? payment.getPaymentGateway() : "PAYOS")
                .transactionType(TransactionType.PLATFORM_FREEZE)
                .referenceId("ORDER_" + order.getId())
                .amount(buyerPaid)
                .balanceAfter(adminWallet.getFrozenBalance())
                .status(TransactionStatus.SUCCESS)
                .content("Khóa tiền PayOS đơn hàng #" + order.getId() + " trong ví platform")
                .build());

        transactionRepository.save(Transaction.builder()
                .payment(payment)
                .paymentGateway(payment != null ? payment.getPaymentGateway() : "PAYOS")
                .transactionType(TransactionType.SHOP_FREEZE)
                .referenceId("ORDER_" + order.getId())
                .amount(shopGross)
                .balanceAfter(shopWallet.getFrozenBalance())
                .status(TransactionStatus.SUCCESS)
                .content("Ghi nhận và khóa tiền hàng shop cho đơn #" + order.getId())
                .build());

        if (platformBatchSubsidy.compareTo(BigDecimal.ZERO) > 0) {
            transactionRepository.save(Transaction.builder()
                    .payment(payment)
                    .paymentGateway(payment != null ? payment.getPaymentGateway() : "PAYOS")
                    .transactionType(TransactionType.PLATFORM_VOUCHER_SUBSIDY)
                    .referenceId("ORDER_" + order.getId())
                    .amount(platformBatchSubsidy.negate())
                    .balanceAfter(adminWallet.getTotalBalance())
                    .status(TransactionStatus.SUCCESS)
                    .content("Platform bù voucher sàn cho đơn #" + order.getId())
                    .build());
        }

        log.info("Transaction created: id={}, amount={}", transaction.getId(), buyerPaid);
    }

    /**
     * Gọi PayOS GET payment-link: thử orderCode rồi paymentLinkId (id trên return URL).
     */
    private Long requirePaidAmountFromPayOS(String orderCode, String paymentLinkId) {
        PayOSPaymentInfoResponse info = null;
        try {
            info = payosService.getPaymentLinkInformation(Long.parseLong(orderCode));
        } catch (NumberFormatException ignored) {
            info = payosService.getPaymentLinkInformation(orderCode);
        }

        if ((info == null || info.getData() == null)
                && paymentLinkId != null && !paymentLinkId.isBlank()) {
            log.info("Retry PayOS verify with paymentLinkId={}", paymentLinkId);
            info = payosService.getPaymentLinkInformation(paymentLinkId.trim());
        }

        if (info == null) {
            log.error("[PAYOS_VERIFY_FAILED] empty response for orderCode={}, paymentLinkId={}",
                    orderCode, paymentLinkId);
            throw new AppException(ErrorCode.PAYOS_VERIFY_FAILED);
        }

        if (info.getData() == null) {
            log.warn("[PAYOS_VERIFY_FAILED] no data orderCode={}, code={}, desc={}",
                    orderCode, info.getCode(), info.getDesc());
            throw new AppException(ErrorCode.PAYOS_VERIFY_FAILED);
        }

        PayOSPaymentInfoResponse.PaymentDetails data = info.getData();
        String status = data.getStatus();
        boolean paid = "PAID".equalsIgnoreCase(status)
                || ("00".equals(info.getCode()) && data.getAmountPaid() > 0 && data.getAmountRemaining() == 0);

        if (!paid) {
            log.warn("[PAYOS_PAYMENT_NOT_PAID] orderCode={}, status={}, code={}, amountPaid={}",
                    orderCode, status, info.getCode(), data.getAmountPaid());
            throw new AppException(ErrorCode.PAYOS_PAYMENT_NOT_PAID);
        }

        // Đảm bảo orderCode trên PayOS khớp đơn local (chống confirm nhầm link)
        if (data.getOrderCode() > 0) {
            String payosOrderCode = String.valueOf(data.getOrderCode());
            if (!payosOrderCode.equals(orderCode)) {
                log.error("[PAYOS_VERIFY_FAILED] orderCode mismatch url={} payos={}", orderCode, payosOrderCode);
                throw new AppException(ErrorCode.PAYOS_VERIFY_FAILED);
            }
        }

        long amount = data.getAmountPaid() > 0 ? data.getAmountPaid() : data.getAmount();
        if (amount <= 0) {
            log.error("[PAYOS_VERIFY_FAILED] invalid paid amount for orderCode={}", orderCode);
            throw new AppException(ErrorCode.PAYOS_VERIFY_FAILED);
        }
        return amount;
    }

    private void verifyPaidAmount(Long paidAmount, BigDecimal expectedAmount) {
        if (paidAmount == null || expectedAmount == null) {
            log.error("[PAYOS_AMOUNT_MISMATCH] missing amount paid={}, expected={}", paidAmount, expectedAmount);
            throw new AppException(ErrorCode.PAYOS_AMOUNT_MISMATCH);
        }
        if (expectedAmount.longValue() != paidAmount) {
            log.error("[PAYOS_AMOUNT_MISMATCH] expected={}, paid={}", expectedAmount, paidAmount);
            throw new AppException(ErrorCode.PAYOS_AMOUNT_MISMATCH);
        }
    }

    @Transactional
    public void cancelPaymentByOrderCode(String orderCode) {

        log.info("===== CANCEL PAYMENT START =====");
        log.info("OrderCode received: {}", orderCode);

        var batchOpt = checkoutBatchRepository.findByPayosOrderCodeForUpdate(orderCode);
        if (batchOpt.isPresent()) {
            cancelBatchPayment(batchOpt.get());
            return;
        }

        Payment payment = paymentRepository.findByOrderCodeForUpdate(orderCode)
                .orElseThrow(() -> new AppException(ErrorCode.PAYMENT_NOT_FOUND));

        log.info("Payment found: id={}, status={}", payment.getId(), payment.getStatus());

        //  Không cho cancel nếu đã thành công
        if (payment.getStatus() == PaymentStatus.SUCCESS) {
            log.error("Cannot cancel payment SUCCESS. OrderCode={}", orderCode);
            throw new AppException(ErrorCode.PAYMENT_ALREADY_SUCCESS);
        }

        Order order = payment.getOrder();

        log.info("Order found: id={}, status={}", order.getId(), order.getStatus());

        // Nếu payment đã FAILED từ callback/lần cancel trước, vẫn đảm bảo cart được trả lại.
        // restoreOrderItemsToCart idempotent nên gọi lại không nhân đôi quantity.
        if (payment.getStatus() == PaymentStatus.FAILED) {
            log.warn("Payment already FAILED → ensure cart restored. OrderCode={}", orderCode);
            cartService.restoreOrderItemsToCart(order);
            return;
        }

        // ======================
        // UPDATE STATUS
        // ======================

        payment.setStatus(PaymentStatus.FAILED);
        payment.setUpdatedAt(LocalDateTime.now(ZoneId.of("Asia/Ho_Chi_Minh")));

        restoreOrderInventory(order);
        voucherUserService.restoreVoucherForOrder(order, shouldAdjustOrderTotalsOnCancel(order));
        cartService.restoreOrderItemsToCart(order);
        order.setStatus(OrderStatus.CANCELLED);
        orderRepository.save(order);

        if (order.getCheckoutBatch() != null) {
            syncBatchStatusAfterCancel(order.getCheckoutBatch());
        }

        log.info("Order status updated to CANCELLED");

        // ======================
        // TRANSACTION LOG (optional)
        // ======================

        Transaction transaction = Transaction.builder()
                .payment(payment)
                .paymentGateway(payment.getPaymentGateway())
                .transactionType(TransactionType.PAYMENT_FAILED)
                .referenceId("ORDER_" + order.getId())
                .amount(payment.getAmount())
                .balanceAfter(null) // không thay đổi ví
                .status(TransactionStatus.FAILED)
                .content("Huỷ thanh toán đơn hàng #" + order.getId())
                .build();

        transactionRepository.save(transaction);

        log.info("Transaction FAILED created");

        paymentRepository.save(payment);

        log.info("Payment updated to FAILED");

        log.info("===== CANCEL PAYMENT COMPLETED =====");
    }

    private void cancelBatchPayment(CheckoutBatch batch) {
        if (batch.getStatus() == CheckoutBatchStatus.PAID) {
            throw new AppException(ErrorCode.PAYMENT_ALREADY_SUCCESS);
        }

        List<Order> orders = orderRepository.findByCheckoutBatchId(batch.getId());

        for (Order order : orders) {
            if (order.getStatus() != OrderStatus.AWAITING_PAYMENT && order.getStatus() != OrderStatus.PENDING) {
                if (order.getStatus() == OrderStatus.CANCELLED) {
                    cartService.restoreOrderItemsToCart(order);
                }
                continue;
            }
            restoreOrderInventory(order);
            voucherUserService.restoreVoucherForOrder(order, false);
            cartService.restoreOrderItemsToCart(order);
            order.setStatus(OrderStatus.CANCELLED);
            orderRepository.save(order);
        }

        syncBatchStatusAfterCancel(batch);
        log.info("===== BATCH CANCEL PAYMENT COMPLETED batchId={} =====", batch.getId());
    }

    @Override
    @Transactional
    public void notifyPendingPayment(String orderCode) {
        log.info("===== NOTIFY PENDING PAYMENT =====");
        log.info("OrderCode: {}", orderCode);

        var batchOpt = checkoutBatchRepository.findByPayosOrderCodeForUpdate(orderCode);
        if (batchOpt.isPresent()) {
            CheckoutBatch batch = batchOpt.get();
            List<Order> unpaid = getUnpaidPayosOrders(batch);
            if (!unpaid.isEmpty()) {
                User buyer = unpaid.get(0).getBuyer();
                String orderIdsText = unpaid.stream()
                        .map(o -> "#" + o.getId())
                        .collect(Collectors.joining(", "));
                Notification notification = Notification.builder()
                        .title("Bạn chưa thanh toán")
                        .message("Bạn vẫn chưa thanh toán, cần thanh toán nhanh kẻo hết hàng! " +
                                 "Các đơn hàng " + orderIdsText + " đang chờ thanh toán của bạn.")
                        .evidence(String.valueOf(unpaid.get(0).getId()))
                        .receiverType("USER_" + buyer.getId())
                        .build();
                notificationService.sendNotificationWebsocket(buyer, notification);
            }
            log.info("===== NOTIFY PENDING PAYMENT COMPLETED (batch) =====");
            return;
        }

        Payment payment = paymentRepository.findByOrderCodeForUpdate(orderCode)
                .orElseThrow(() -> new AppException(ErrorCode.PAYMENT_NOT_FOUND));

        if (payment.getStatus() == PaymentStatus.SUCCESS) {
            log.info("Payment already SUCCESS, skip notification. OrderCode={}", orderCode);
            return;
        }

        Order order = payment.getOrder();
        User buyer = order.getBuyer();

        if (order.getStatus() == OrderStatus.PENDING || order.getStatus() == OrderStatus.AWAITING_PAYMENT) {
            order.setStatus(OrderStatus.AWAITING_PAYMENT);
            orderRepository.save(order);
            log.info("Order {} status updated to AWAITING_PAYMENT", order.getId());
        }

        Notification notification = Notification.builder()
                .title("Bạn chưa thanh toán")
                .message("Bạn vẫn chưa thanh toán, cần thanh toán nhanh kẻo hết hàng! " +
                         "Đơn hàng #" + order.getId() + " đang chờ thanh toán của bạn.")
                .evidence(String.valueOf(order.getId()))
                .receiverType("USER_" + buyer.getId())
                .build();

        notificationService.sendNotificationWebsocket(buyer, notification);
        log.info("Pending payment notification sent to buyer userId={}", buyer.getId());
        log.info("===== NOTIFY PENDING PAYMENT COMPLETED =====");
    }

    @Override
    public PaymentResponse getLatestPendingByOrderId(Integer orderId) {
        return paymentRepository
                .findTopByOrderIdAndStatusOrderByCreatedAtDesc(orderId, PaymentStatus.PENDING)
                .map(this::toOrderPaymentResponse)
                .orElse(null);
    }

    @Override
    public ResumePaymentOptionsResponse getResumePaymentOptions(Integer orderId) {
        Order order = orderRepository.findById(orderId)
                .orElseThrow(() -> new AppException(ErrorCode.ORDER_NOT_FOUND));

        // Terminal state check applies regardless of batch membership
        if (order.getStatus() == OrderStatus.PAID
                || order.getStatus() == OrderStatus.DELIVERED
                || order.getStatus() == OrderStatus.CONFIRMED
                || order.getStatus() == OrderStatus.READY_FOR_PICKUP
                || order.getStatus() == OrderStatus.SHIPPING
                || order.getStatus() == OrderStatus.QUALITY_CHECKED
                || order.getStatus() == OrderStatus.READY) {
            throw new AppException(ErrorCode.ORDER_ALREADY_PAID);
        }
        if (order.getStatus() == OrderStatus.CANCELLED || order.getStatus() == OrderStatus.FAILED) {
            throw new AppException(ErrorCode.ORDER_ALREADY_CANCELLED);
        }

        CheckoutBatch batch = order.getCheckoutBatch();
        if (batch == null || batch.getStatus() == CheckoutBatchStatus.CANCELLED) {
            PaymentResponse singleOrderPayment = null;
            try {
                singleOrderPayment = getLatestPendingByOrderId(orderId);
            } catch (AppException ignored) {
                // no pending payment yet — will attempt to create one
            }
            return ResumePaymentOptionsResponse.builder()
                    .isBatch(false)
                    .triggerOrderId(orderId)
                    .singleOrderPayment(singleOrderPayment)
                    .build();
        }

        if (batch.getStatus() == CheckoutBatchStatus.PAID) {
            throw new AppException(ErrorCode.ORDER_ALREADY_PAID);
        }

        List<Order> unpaid = getUnpaidPayosOrders(batch);
        if (unpaid.isEmpty()) {
            throw new AppException(ErrorCode.ORDER_NOT_PAYABLE);
        }

        BigDecimal payAllAmount = resolveBatchPayableAmount(batch, unpaid);
        boolean canReuse = !hasPaidOrdersInBatch(batch)
                && batch.getCheckoutUrl() != null
                && batch.getPayosOrderCode() != null
                && batch.getTotalAmount() != null
                && batch.getTotalAmount().compareTo(payAllAmount) == 0
                // Không reuse link cũ khi có voucher sàn — tránh PayOS còn số gross trước giảm giá
                && (batch.getBatchDiscountAmount() == null
                    || batch.getBatchDiscountAmount().compareTo(BigDecimal.ZERO) <= 0);

        List<ResumePaymentOrderOption> unpaidOptions = unpaid.stream()
                .map(o -> ResumePaymentOrderOption.builder()
                        .orderId(o.getId())
                        .shopName(resolveShopDisplayName(o.getShopOwner()))
                        .amount(o.getTotalAmount())
                        .status(o.getStatus() != null ? o.getStatus().name() : null)
                        .build())
                .toList();

        PayAllOption payAllOption = PayAllOption.builder()
                .amount(payAllAmount)
                .checkoutUrl(canReuse ? batch.getCheckoutUrl() : null)
                .payosOrderCode(canReuse ? batch.getPayosOrderCode() : null)
                .canReuseExistingLink(canReuse)
                .build();

        return ResumePaymentOptionsResponse.builder()
                .isBatch(true)
                .batchId(batch.getId())
                .hasPlatformVoucher(batch.getBatchVoucherUserId() != null)
                .triggerOrderId(orderId)
                .unpaidOrders(unpaidOptions)
                .payAllOption(payAllOption)
                .build();
    }

    @Override
    @Transactional
    public PaymentResponse createBatchPayOS(Integer batchId) {
        CheckoutBatch batch = checkoutBatchRepository.findById(batchId)
                .orElseThrow(() -> new AppException(ErrorCode.ORDER_NOT_FOUND));

        if (batch.getStatus() == CheckoutBatchStatus.PAID) {
            throw new AppException(ErrorCode.ORDER_NOT_PAYABLE);
        }

        List<Order> unpaid = getUnpaidPayosOrders(batch);
        if (unpaid.isEmpty()) {
            throw new AppException(ErrorCode.ORDER_NOT_PAYABLE);
        }

        BigDecimal remainingTotal = resolveBatchPayableAmount(batch, unpaid);
        User buyer = unpaid.get(0).getBuyer();
        long orderCode = System.currentTimeMillis() + batch.getId();

        PayosPayoutResult qr = payosService.createOrderPaymentQr(
                remainingTotal,
                orderCode,
                batch.getId(),
                buyer.getFullName()
        );

        if (qr == null || !qr.isSuccess()) {
            throw new AppException(ErrorCode.PAYOS_CREATE_QR_FAILED);
        }

        batch.setPayosOrderCode(String.valueOf(orderCode));
        batch.setCheckoutUrl(qr.getCheckoutUrl());
        batch.setQrCodeUrl(qr.getQrCodeUrl());
        batch.setTotalAmount(remainingTotal);
        checkoutBatchRepository.save(batch);

        return buildBatchPaymentResponse(batch, unpaid.get(0).getId());
    }

    private PaymentResponse resolvePendingBatchPaymentByOrderId(Integer orderId) {
        Order order = orderRepository.findById(orderId)
                .orElseThrow(() -> new AppException(ErrorCode.ORDER_NOT_FOUND));

        CheckoutBatch batch = order.getCheckoutBatch();
        if (batch == null) {
            throw new AppException(ErrorCode.PAYMENT_NOT_FOUND);
        }

        if (batch.getStatus() != CheckoutBatchStatus.PENDING
                && batch.getStatus() != CheckoutBatchStatus.PARTIALLY_PAID) {
            throw new AppException(ErrorCode.PAYMENT_NOT_FOUND);
        }

        List<Order> unpaid = getUnpaidPayosOrders(batch);
        if (unpaid.isEmpty() || batch.getCheckoutUrl() == null || batch.getPayosOrderCode() == null) {
            throw new AppException(ErrorCode.PAYMENT_NOT_FOUND);
        }

        boolean orderStillUnpaid = unpaid.stream().anyMatch(o -> o.getId().equals(orderId));
        if (!orderStillUnpaid) {
            throw new AppException(ErrorCode.PAYMENT_NOT_FOUND);
        }

        return buildBatchPaymentResponse(batch, orderId);
    }

    private PaymentResponse buildBatchPaymentResponse(CheckoutBatch batch, Integer triggerOrderId) {
        List<Order> unpaid = getUnpaidPayosOrders(batch);
        List<Integer> orderIds = unpaid.stream().map(Order::getId).toList();
        Integer resolvedOrderId = triggerOrderId != null
                ? triggerOrderId
                : (orderIds.isEmpty() ? null : orderIds.get(0));

        PaymentStatus status = batch.getStatus() == CheckoutBatchStatus.CANCELLED
                ? PaymentStatus.FAILED
                : (batch.getStatus() == CheckoutBatchStatus.PAID
                ? PaymentStatus.SUCCESS
                : PaymentStatus.PENDING);

        return PaymentResponse.builder()
                .orderId(resolvedOrderId)
                .amount(resolveBatchPayableAmount(batch, unpaid))
                .paymentGateway("PAYOS")
                .status(status)
                .payosOrderCode(batch.getPayosOrderCode())
                .checkoutUrl(batch.getCheckoutUrl())
                .qrCodeUrl(batch.getQrCodeUrl())
                .batchId(batch.getId())
                .orderIds(orderIds)
                .paymentScope("BATCH")
                .build();
    }

    private PaymentResponse toOrderPaymentResponse(Payment payment) {
        PaymentResponse response = paymentMapper.toResponse(payment);
        response.setPaymentScope("ORDER");
        Order order = payment.getOrder();
        if (order != null) {
            if (order.getCheckoutBatch() != null) {
                response.setBatchId(order.getCheckoutBatch().getId());
            }
            response.setOrderIds(List.of(order.getId()));
        }
        return response;
    }

    private List<Order> getUnpaidPayosOrders(CheckoutBatch batch) {
        return orderRepository.findByCheckoutBatchId(batch.getId()).stream()
                .filter(o -> "PAYOS".equalsIgnoreCase(o.getPaymentMethod()))
                .filter(o -> o.getStatus() == OrderStatus.PENDING
                        || o.getStatus() == OrderStatus.AWAITING_PAYMENT)
                .toList();
    }

    private BigDecimal sumUnpaidAmount(List<Order> orders) {
        return orders.stream()
                .map(Order::getTotalAmount)
                .filter(amount -> amount != null)
                .reduce(BigDecimal.ZERO, BigDecimal::add);
    }

    /**
     * Số tiền buyer phải trả cho các đơn unpaid trong batch.
     * Nếu voucher sàn đã trừ vào từng order.totalAmount → dùng tổng totalAmount.
     * Nếu chỉ ghi discount trên batch mà totalAmount vẫn gross → trừ batchDiscountAmount.
     */
    private BigDecimal resolveBatchPayableAmount(CheckoutBatch batch, List<Order> unpaid) {
        BigDecimal sum = sumUnpaidAmount(unpaid);
        if (batch == null || unpaid == null || unpaid.isEmpty()) {
            return sum;
        }
        BigDecimal batchDisc = batch.getBatchDiscountAmount() != null
                ? batch.getBatchDiscountAmount()
                : BigDecimal.ZERO;
        if (batchDisc.compareTo(BigDecimal.ZERO) <= 0) {
            return sum;
        }
        BigDecimal orderDisc = unpaid.stream()
                .map(o -> o.getBatchDiscountAmount() != null ? o.getBatchDiscountAmount() : BigDecimal.ZERO)
                .reduce(BigDecimal.ZERO, BigDecimal::add);
        if (orderDisc.compareTo(BigDecimal.ZERO) > 0) {
            return sum;
        }
        return sum.subtract(batchDisc).max(BigDecimal.ZERO);
    }

    private boolean hasPaidOrdersInBatch(CheckoutBatch batch) {
        return orderRepository.findByCheckoutBatchId(batch.getId()).stream()
                .anyMatch(o -> o.getStatus() == OrderStatus.PAID);
    }

    private void invalidateBatchPayosLink(CheckoutBatch batch) {
        batch.setPayosOrderCode(null);
        batch.setCheckoutUrl(null);
        batch.setQrCodeUrl(null);
        checkoutBatchRepository.save(batch);
    }

    private void syncBatchStatusAfterPayment(CheckoutBatch batch) {
        List<Order> all = orderRepository.findByCheckoutBatchId(batch.getId());
        long paidCount = all.stream().filter(o -> o.getStatus() == OrderStatus.PAID).count();
        List<Order> unpaid = getUnpaidPayosOrders(batch);

        if (unpaid.isEmpty() && paidCount > 0) {
            batch.setStatus(CheckoutBatchStatus.PAID);
        } else if (paidCount > 0) {
            batch.setStatus(CheckoutBatchStatus.PARTIALLY_PAID);
            invalidateBatchPayosLink(batch);
        }
        checkoutBatchRepository.save(batch);
    }

    private void syncBatchStatusAfterCancel(CheckoutBatch batch) {
        List<Order> all = orderRepository.findByCheckoutBatchId(batch.getId());
        boolean hasUnpaid = !getUnpaidPayosOrders(batch).isEmpty();
        boolean hasSettled = all.stream().anyMatch(o ->
                o.getStatus() != OrderStatus.CANCELLED
                        && o.getStatus() != OrderStatus.PENDING
                        && o.getStatus() != OrderStatus.AWAITING_PAYMENT);
        boolean allCancelled = !all.isEmpty()
                && all.stream().allMatch(o -> o.getStatus() == OrderStatus.CANCELLED);

        if (allCancelled || (!hasSettled && !hasUnpaid)) {
            voucherUserService.restoreBatchVoucher(batch, all, false);
            batch.setStatus(CheckoutBatchStatus.CANCELLED);
            invalidateBatchPayosLink(batch);
        } else if (hasSettled && hasUnpaid) {
            batch.setStatus(CheckoutBatchStatus.PARTIALLY_PAID);
            invalidateBatchPayosLink(batch);
        } else if (hasSettled) {
            batch.setStatus(CheckoutBatchStatus.PAID);
            invalidateBatchPayosLink(batch);
        } else if (hasUnpaid) {
            batch.setStatus(CheckoutBatchStatus.PENDING);
            List<Order> unpaid = getUnpaidPayosOrders(batch);
            BigDecimal remaining = resolveBatchPayableAmount(batch, unpaid);
            if (batch.getTotalAmount() == null || batch.getTotalAmount().compareTo(remaining) != 0) {
                batch.setTotalAmount(remaining);
                invalidateBatchPayosLink(batch);
            }
        }
        checkoutBatchRepository.save(batch);
    }

    /** Giữ totalAmount đơn như lúc checkout PayOS — chỉ hoàn voucher, không cộng lại giảm giá lên đơn. */
    private boolean shouldAdjustOrderTotalsOnCancel(Order order) {
        if (order == null || order.getStatus() == null) {
            return true;
        }
        return order.getStatus() != OrderStatus.AWAITING_PAYMENT
                && order.getStatus() != OrderStatus.PENDING;
    }

    private void restoreOrderInventory(Order order) {
        if (order == null) {
            return;
        }

        Map<Integer, Product> productsToUpdate = new HashMap<>();

        if (order.getOrderDetails() != null && !order.getOrderDetails().isEmpty()) {
            for (OrderDetail detail : order.getOrderDetails()) {
                if (detail == null
                        || detail.getProduct() == null
                        || detail.getQuantity() == null
                        || detail.getQuantity() <= 0) {
                    continue;
                }

                Product product = detail.getProduct();
                BigDecimal currentStockKg = product.getStockKg() != null
                        ? product.getStockKg()
                        : BigDecimal.valueOf(product.getStockQuantity() == null ? 0 : product.getStockQuantity());
                BigDecimal quantityKg = detail.getQuantityKg() != null
                        ? detail.getQuantityKg()
                        : BigDecimal.valueOf(detail.getQuantity());
                product.setStockKg(currentStockKg.add(quantityKg));
                product.setStockQuantity(product.getStockKg().setScale(0, RoundingMode.DOWN).intValue());
                productsToUpdate.put(product.getId(), product);
            }
        }

        if (!productsToUpdate.isEmpty()) {
            productRepository.saveAll(productsToUpdate.values());
        }

        if (order.getMysteryBoxes() != null && !order.getMysteryBoxes().isEmpty()) {
            for (OrderMysteryBox orderBox : order.getMysteryBoxes()) {
                if (orderBox == null
                        || orderBox.getMysteryBox() == null
                        || orderBox.getQuantity() == null
                        || orderBox.getQuantity() <= 0) {
                    continue;
                }

                MysteryBox box = orderBox.getMysteryBox();
                int currentSold = box.getSoldQuantity() == null ? 0 : box.getSoldQuantity();
                int totalQty = box.getTotalQuantity() == null ? 0 : box.getTotalQuantity();
                box.setSoldQuantity(Math.max(0, currentSold - orderBox.getQuantity()));
                if (!Boolean.TRUE.equals(box.getIsActive()) && box.getSoldQuantity() < totalQty) {
                    box.setIsActive(true);
                }
                mysteryBoxRepository.save(box);
            }
        }
    }

    @Override
    @Transactional
    public void refundPayosOrderOnCancel(Order order) {
        refundPayosOrderToBuyer(order, "Hủy đơn sau thanh toán PayOS");
    }

    @Override
    @Transactional
    public void refundPayosOrderOnFailed(Order order) {
        refundPayosOrderToBuyer(order, "Hoàn tiền do giao hàng thất bại đơn #" + order.getId());
    }

    private void refundPayosOrderToBuyer(Order order, String reasonPrefix) {
        if (order == null
                || !"PAYOS".equalsIgnoreCase(order.getPaymentMethod())
                || order.getStatus() == OrderStatus.CANCELLED
                || order.getStatus() == OrderStatus.DELIVERED) {
            return;
        }

        OrderSettlementCalculator.SettlementAmounts amounts = orderSettlementCalculator.calculate(order);
        BigDecimal buyerPaid = amounts.buyerPaid();
        if (buyerPaid.compareTo(BigDecimal.ZERO) <= 0) {
            log.warn("[PAYOS_REFUND] Order {} has invalid buyerPaid, skip", order.getId());
            return;
        }

        Payment payment = paymentRepository
                .findTopByOrderIdAndStatusOrderByCreatedAtDesc(order.getId(), PaymentStatus.SUCCESS)
                .orElse(null);

        CheckoutBatch batch = order.getCheckoutBatch();
        boolean fromPaidBatch = batch != null
                && (batch.getStatus() == CheckoutBatchStatus.PAID
                || batch.getStatus() == CheckoutBatchStatus.PARTIALLY_PAID);

        if (payment == null && !fromPaidBatch) {
            log.warn("[PAYOS_REFUND] Order {} has no SUCCESS payment and batch not paid, skip", order.getId());
            return;
        }

        if (payment != null && payment.getAmount() != null && payment.getAmount().compareTo(BigDecimal.ZERO) > 0) {
            buyerPaid = payment.getAmount();
        }

        BigDecimal qcRefunded = order.getQcRefundedAmount() != null
                ? order.getQcRefundedAmount()
                : BigDecimal.ZERO;
        BigDecimal remainingRefund = buyerPaid.subtract(qcRefunded);
        if (remainingRefund.compareTo(BigDecimal.ZERO) <= 0) {
            log.warn("[PAYOS_REFUND] Order {} already fully refunded via QC (qcRefunded={}), skip",
                    order.getId(), qcRefunded);
            return;
        }

        BigDecimal platformBatchSubsidy = amounts.platformBatchSubsidy();
        BigDecimal settlementTotal = amounts.settlementTotal();
        BigDecimal shopGross = amounts.shopGross();

        BigDecimal qcShopDebit = settlementTotal.signum() > 0
                ? shopGross.multiply(qcRefunded).divide(settlementTotal, 2, RoundingMode.HALF_UP)
                : BigDecimal.ZERO;
        BigDecimal shopRemainingReversal = shopGross.subtract(qcShopDebit).max(BigDecimal.ZERO);

        log.info("[PAYOS_REFUND] orderId={}, buyerPaid={}, qcRefunded={}, remainingRefund={}, reason={}",
                order.getId(), buyerPaid, qcRefunded, remainingRefund, reasonPrefix);

        Wallet platformWallet = walletRepository.findByTypeForUpdate(WalletType.PLATFORM)
                .orElseThrow(() -> new AppException(ErrorCode.PLATFORM_WALLET_NOT_FOUND));
        Wallet shopWallet = walletRepository.findByShopOwnerIdForUpdate(order.getShopOwner().getId())
                .orElseThrow(() -> new AppException(ErrorCode.WALLET_NOT_FOUND));
        Wallet buyerWallet = walletRepository.findByBuyerForUpdate(order.getBuyer());
        if (buyerWallet == null) {
            throw new AppException(ErrorCode.WALLET_NOT_FOUND);
        }

        platformWallet.setTotalBalance(
                platformWallet.getTotalBalance().subtract(remainingRefund).add(platformBatchSubsidy)
        );
        platformWallet.setFrozenBalance(platformWallet.getFrozenBalance().subtract(remainingRefund));
        walletRepository.save(platformWallet);

        if (shopRemainingReversal.signum() > 0) {
            shopWallet.setTotalBalance(shopWallet.getTotalBalance().subtract(shopRemainingReversal));
            shopWallet.setFrozenBalance(shopWallet.getFrozenBalance().subtract(shopRemainingReversal));
            shopWallet.setUpdatedAt(LocalDateTime.now(ZoneId.of("Asia/Ho_Chi_Minh")));
            walletRepository.save(shopWallet);
        }

        buyerWallet.setTotalBalance(buyerWallet.getTotalBalance().add(remainingRefund));
        walletRepository.save(buyerWallet);

        String referenceId = "ORDER_" + order.getId();
        String gateway = payment != null && payment.getPaymentGateway() != null
                ? payment.getPaymentGateway()
                : "PAYOS";

        transactionRepository.save(Transaction.builder()
                .payment(payment)
                .paymentGateway(gateway)
                .transactionType(TransactionType.PLATFORM_FREEZE)
                .referenceId(referenceId)
                .amount(remainingRefund.negate())
                .balanceAfter(platformWallet.getFrozenBalance())
                .status(TransactionStatus.SUCCESS)
                .content("Hoàn khóa tiền PayOS: " + reasonPrefix + " #" + order.getId())
                .build());

        if (shopRemainingReversal.signum() > 0) {
            transactionRepository.save(Transaction.builder()
                    .payment(payment)
                    .paymentGateway(gateway)
                    .transactionType(TransactionType.SHOP_REFUND_DEBIT)
                    .referenceId(referenceId)
                    .amount(shopRemainingReversal.negate())
                    .balanceAfter(shopWallet.getTotalBalance())
                    .status(TransactionStatus.SUCCESS)
                    .content("Hoàn tiền shop do hoàn PayOS #" + order.getId())
                    .build());
        }

        transactionRepository.save(Transaction.builder()
                .payment(payment)
                .paymentGateway(gateway)
                .transactionType(TransactionType.REFUND)
                .referenceId(referenceId)
                .amount(remainingRefund)
                .balanceAfter(buyerWallet.getTotalBalance())
                .status(TransactionStatus.SUCCESS)
                .content("Hoàn tiền vào ví buyer: " + reasonPrefix)
                .build());

        order.setRefundAmount(buyerPaid);
        order.setRefundReason(qcRefunded.signum() > 0
                ? reasonPrefix + " (đã hoàn QC trước đó " + qcRefunded + "đ)"
                : reasonPrefix);

        String formattedAmount = String.format("%,.0f", remainingRefund) + "đ";
        Notification notification = Notification.builder()
                .title("Hoàn tiền đơn hàng #" + order.getId())
                .message("Đơn hàng #" + order.getId() + ". "
                        + formattedAmount + " đã được hoàn vào ví của bạn.")
                .evidence(String.valueOf(order.getId()))
                .receiverType("USER_" + order.getBuyer().getId())
                .build();
        notificationService.sendNotificationWebsocket(order.getBuyer(), notification);

        log.info("[PAYOS_REFUND] Completed for orderId={}, remainingRefunded={}", order.getId(), remainingRefund);
    }

    @Override
    @Transactional
    public BigDecimal refundQcDamage(Order order, BigDecimal amount, String reason) {
        if (order == null || amount == null || amount.compareTo(BigDecimal.ZERO) <= 0) {
            return BigDecimal.ZERO;
        }

        BigDecimal alreadyQcRefunded = order.getQcRefundedAmount() != null
                ? order.getQcRefundedAmount()
                : BigDecimal.ZERO;
        BigDecimal totalAmount = order.getTotalAmount() != null ? order.getTotalAmount() : BigDecimal.ZERO;
        BigDecimal maxRefundable = totalAmount.subtract(alreadyQcRefunded);
        if (maxRefundable.compareTo(BigDecimal.ZERO) <= 0) {
            log.warn("[QC_REFUND] Order {} already fully QC-refunded (requested={}, cap=0)",
                    order.getId(), amount);
            return BigDecimal.ZERO;
        }
        BigDecimal refundNow = amount.min(maxRefundable);

        if ("PAYOS".equalsIgnoreCase(order.getPaymentMethod())) {
            refundPayosPartial(order, refundNow, reason);
        }

        order.setQcRefundedAmount(alreadyQcRefunded.add(refundNow));
        orderRepository.save(order);

        String formattedAmount = String.format("%,.0f", refundNow) + "đ";
        Notification notification = Notification.builder()
                .title("Hoàn tiền QC đơn #" + order.getId())
                .message(buildQcRefundNotificationMessage(order, formattedAmount, reason))
                .evidence(String.valueOf(order.getId()))
                .receiverType("USER_" + order.getBuyer().getId())
                .build();
        notificationService.sendNotificationWebsocket(order.getBuyer(), notification);

        log.info("[QC_REFUND] orderId={}, refunded={}, totalQcRefunded={}",
                order.getId(), refundNow, order.getQcRefundedAmount());
        return refundNow;
    }

    private void refundPayosPartial(Order order, BigDecimal refundAmount, String reason) {
        OrderSettlementCalculator.SettlementAmounts amounts = orderSettlementCalculator.calculate(order);
        BigDecimal totalAmount = amounts.buyerPaid();
        BigDecimal settlementTotal = amounts.settlementTotal();
        BigDecimal shopGross = amounts.shopGross();

        BigDecimal shopDebit = settlementTotal.signum() > 0
                ? shopGross.multiply(refundAmount).divide(settlementTotal, 2, RoundingMode.HALF_UP)
                : BigDecimal.ZERO;

        Payment payment = paymentRepository
                .findTopByOrderIdAndStatusOrderByCreatedAtDesc(order.getId(), PaymentStatus.SUCCESS)
                .orElse(null);

        Wallet platformWallet = walletRepository.findByTypeForUpdate(WalletType.PLATFORM)
                .orElseThrow(() -> new AppException(ErrorCode.PLATFORM_WALLET_NOT_FOUND));
        Wallet shopWallet = walletRepository.findByShopOwnerIdForUpdate(order.getShopOwner().getId())
                .orElseThrow(() -> new AppException(ErrorCode.WALLET_NOT_FOUND));
        Wallet buyerWallet = walletRepository.findByBuyerForUpdate(order.getBuyer());
        if (buyerWallet == null) {
            throw new AppException(ErrorCode.WALLET_NOT_FOUND);
        }

        platformWallet.setTotalBalance(platformWallet.getTotalBalance().subtract(refundAmount));
        platformWallet.setFrozenBalance(platformWallet.getFrozenBalance().subtract(refundAmount));
        walletRepository.save(platformWallet);

        if (shopDebit.signum() > 0) {
            shopWallet.setTotalBalance(shopWallet.getTotalBalance().subtract(shopDebit));
            shopWallet.setFrozenBalance(shopWallet.getFrozenBalance().subtract(shopDebit));
            shopWallet.setUpdatedAt(LocalDateTime.now(ZoneId.of("Asia/Ho_Chi_Minh")));
            walletRepository.save(shopWallet);
        }

        buyerWallet.setTotalBalance(buyerWallet.getTotalBalance().add(refundAmount));
        walletRepository.save(buyerWallet);

        String referenceId = "ORDER_" + order.getId();
        String gateway = payment != null && payment.getPaymentGateway() != null
                ? payment.getPaymentGateway()
                : "PAYOS";

        transactionRepository.save(Transaction.builder()
                .payment(payment)
                .paymentGateway(gateway)
                .transactionType(TransactionType.REFUND)
                .referenceId(referenceId)
                .amount(refundAmount)
                .balanceAfter(buyerWallet.getTotalBalance())
                .status(TransactionStatus.SUCCESS)
                .content(reason != null ? reason : "Hoàn tiền QC đơn #" + order.getId())
                .build());

        if (shopDebit.signum() > 0) {
            transactionRepository.save(Transaction.builder()
                    .payment(payment)
                    .paymentGateway(gateway)
                    .transactionType(TransactionType.SHOP_REFUND_DEBIT)
                    .referenceId(referenceId)
                    .amount(shopDebit.negate())
                    .balanceAfter(shopWallet.getTotalBalance())
                    .status(TransactionStatus.SUCCESS)
                    .content("Trừ tiền shop do hoàn QC đơn #" + order.getId())
                    .build());
        }
    }

    private String buildQcRefundNotificationMessage(Order order, String formattedAmount, String reason) {
        String prefix = reason != null ? reason + ". " : "";
        if ("COD".equalsIgnoreCase(order.getPaymentMethod())) {
            return prefix + formattedAmount + " đã được trừ khỏi số tiền COD khi nhận hàng.";
        }
        return prefix + formattedAmount + " đã được hoàn vào ví của bạn.";
    }

    private String resolveShopDisplayName(User shopOwner) {
        if (shopOwner == null) {
            return null;
        }
        if (shopOwner.getShopName() != null && !shopOwner.getShopName().isBlank()) {
            return shopOwner.getShopName();
        }
        return shopOwner.getFullName();
    }

}
