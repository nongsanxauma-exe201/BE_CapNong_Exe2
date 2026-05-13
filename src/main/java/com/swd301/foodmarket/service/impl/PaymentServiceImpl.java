package com.swd301.foodmarket.service.impl;

import com.swd301.foodmarket.dto.request.CreatePaymentRequest;
import com.swd301.foodmarket.dto.request.PayOSCreateRequest;
import com.swd301.foodmarket.dto.request.PayOSWebhookRequest;
import com.swd301.foodmarket.dto.response.PayOSCreateResponse;
import com.swd301.foodmarket.dto.response.PaymentResponse;
import com.swd301.foodmarket.entity.*;
import com.swd301.foodmarket.enums.OrderStatus;
import com.swd301.foodmarket.enums.PaymentStatus;
import com.swd301.foodmarket.enums.TransactionStatus;
import com.swd301.foodmarket.enums.WalletType;
import com.swd301.foodmarket.exception.AppException;
import com.swd301.foodmarket.exception.ErrorCode;
import com.swd301.foodmarket.mapper.PaymentMapper;
import com.swd301.foodmarket.repository.OrderRepository;
import com.swd301.foodmarket.repository.PaymentRepository;
import com.swd301.foodmarket.repository.TransactionRepository;
import com.swd301.foodmarket.repository.WalletRepository;
import com.swd301.foodmarket.service.CartService;
import com.swd301.foodmarket.service.PaymentService;
import com.swd301.foodmarket.service.PayosService;
import jakarta.transaction.Transactional;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.checkerframework.checker.units.qual.C;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.time.LocalDateTime;

@Service
@RequiredArgsConstructor
@Slf4j
public class PaymentServiceImpl implements PaymentService {

    private final PaymentRepository paymentRepository;
    private final OrderRepository orderRepository;
    private final PaymentMapper paymentMapper;
    private final PayosService payosService;
    private final TransactionRepository transactionRepository;
    private final CartService cartService;
    private final WalletRepository walletRepository;

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
                .paymentDate(LocalDateTime.now())
                .createdAt(LocalDateTime.now())
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
                    .orElseThrow(() -> new AppException(ErrorCode.PAYMENT_NOT_FOUND));
        }

        return paymentMapper.toResponse(payment);
    }

    @Override
    public PaymentResponse updateStatus(Integer id, String status) {
        Payment payment = paymentRepository.findById(id)
                .orElseThrow(() -> new AppException(ErrorCode.PAYMENT_NOT_FOUND));

        payment.setStatus(PaymentStatus.valueOf(status));
        payment.setUpdatedAt(LocalDateTime.now());

        paymentRepository.save(payment);

        return paymentMapper.toResponse(payment);
    }

    @Transactional
    public PaymentResponse createPaymentWithPayOS(CreatePaymentRequest request) {

        Order order = orderRepository.findById(request.getOrderId())
                .orElseThrow(() -> new AppException(ErrorCode.ORDER_NOT_FOUND));

        if (!order.getStatus().name().equals("PENDING")) {
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
        payment.setCreatedAt(LocalDateTime.now());
        payment.setUpdatedAt(LocalDateTime.now());

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
        payment.setUpdatedAt(LocalDateTime.now());

        paymentRepository.save(payment);

        return paymentMapper.toResponse(payment);
    }


    @Transactional
    public void confirmPaymentByOrderCode(String orderCode) {

        log.info("===== CONFIRM PAYMENT START =====");
        log.info("OrderCode received: {}", orderCode);

        Payment payment = paymentRepository.findByOrderCodeForUpdate(orderCode)
                .orElseThrow(() -> new AppException(ErrorCode.PAYMENT_NOT_FOUND));

        log.info("Payment found: id={}, status={}", payment.getId(), payment.getStatus());

        // tránh chạy 2 lần
        if (payment.getStatus() == PaymentStatus.SUCCESS) {
            log.warn("Payment already SUCCESS → skip processing. OrderCode={}", orderCode);
            return;
        }

        Order order = payment.getOrder();

        log.info("Order found: id={}, status={}", order.getId(), order.getStatus());

        payment.setStatus(PaymentStatus.SUCCESS);
        payment.setPaymentDate(LocalDateTime.now());
        payment.setUpdatedAt(LocalDateTime.now());

        order.setStatus(OrderStatus.PAID);
        orderRepository.save(order);

        log.info("Order status updated to PAID");

        // ======================
        // TÍNH TIỀN
        // ======================

        BigDecimal totalAmount = payment.getAmount();

        BigDecimal shippingFee = order.getShippingFee() != null
                ? order.getShippingFee()
                : BigDecimal.ZERO;

        // tiền hàng (chưa trừ phí)
        BigDecimal shopGross = totalAmount.subtract(shippingFee);

        log.info("Total amount: {}", totalAmount);
        log.info("Shipping fee: {}", shippingFee);
        log.info("Shop gross: {}", shopGross);
        
        // Phí nền tảng 10% cho toàn bộ đơn hàng
        BigDecimal platformFeeTotal = totalAmount.multiply(BigDecimal.valueOf(0.1));
        
        // Phí lấy từ phía Shop (10% của shopGross)
        BigDecimal platformFeeFromShop = shopGross.multiply(BigDecimal.valueOf(0.1));
        
        // Phí lấy từ phía Ship (10% của shippingFee)
        BigDecimal platformFeeFromShip = shippingFee.multiply(BigDecimal.valueOf(0.1));

        // Tiền thực nhận
        BigDecimal shopNet = shopGross.subtract(platformFeeFromShop);
        BigDecimal shipperNet = shippingFee.subtract(platformFeeFromShip);

        log.info("Platform Fee Total (10%): {}", platformFeeTotal);
        log.info("Shop Net: {}", shopNet);
        log.info("Shipper Net: {}", shipperNet);

        // ======================
        // ADMIN WALLET
        // ======================

        Wallet adminWallet = walletRepository.findByTypeForUpdate(WalletType.PLATFORM)
                .orElseThrow(() -> new AppException(ErrorCode.PLATFORM_WALLET_NOT_FOUND));

        log.info("Admin wallet BEFORE balance: {}", adminWallet.getTotalBalance());

        // Doanh thu thực của Admin là tiền phí (10%)
        adminWallet.setTotalRevenueAllTime(
                adminWallet.getTotalRevenueAllTime().add(platformFeeTotal)
        );
        
        // Tiền Admin cầm trong ví hoa hồng (là lợi nhuận có thể rút/sử dụng)
        adminWallet.setCommissionWallet(
                adminWallet.getCommissionWallet().add(platformFeeTotal)
        );
        
        // Admin giữ phần tiền của Shop và Shipper (Held Money)
        BigDecimal totalHeld = shopNet.add(shipperNet);
        
        adminWallet.setTotalBalance(
                adminWallet.getTotalBalance().add(totalHeld)
        );
        
        // Cộng luôn vào frozen_balance theo yêu cầu (để theo dõi tiền đang giữ)
        adminWallet.setFrozenBalance(
                adminWallet.getFrozenBalance().add(totalHeld)
        );

        walletRepository.save(adminWallet);

        log.info("Admin wallet AFTER balance: {}", adminWallet.getTotalBalance());

        // ======================
        // SHOP WALLET
        // ======================

        User shop = order.getShopOwner();

        Wallet shopWallet = walletRepository.findByShopOwnerIdForUpdate(shop.getId())
                .orElseThrow(() -> new AppException(ErrorCode.WALLET_NOT_FOUND));

        log.info("Shop wallet BEFORE frozen balance: {}", shopWallet.getFrozenBalance());

        shopWallet.setFrozenBalance(
                shopWallet.getFrozenBalance().add(shopNet)
        );

        shopWallet.setTotalRevenueAllTime(
                shopWallet.getTotalRevenueAllTime().add(shopNet)
        );

        shopWallet.setUpdatedAt(LocalDateTime.now());

        walletRepository.save(shopWallet);

        log.info("Shop wallet AFTER frozen balance: {}", shopWallet.getFrozenBalance());

        // ======================
        // SHIPPER WALLET
        // ======================

        if (order.getShipper() != null) {
            Wallet shipperWallet = walletRepository.findByShipperIdForUpdate(order.getShipper().getId())
                    .orElseThrow(() -> new AppException(ErrorCode.WALLET_NOT_FOUND));

            log.info("Shipper wallet BEFORE frozen balance: {}", shipperWallet.getFrozenBalance());

            shipperWallet.setFrozenBalance(
                    shipperWallet.getFrozenBalance().add(shipperNet)
            );

            shipperWallet.setTotalRevenueAllTime(
                    shipperWallet.getTotalRevenueAllTime().add(shipperNet)
            );

            shipperWallet.setUpdatedAt(LocalDateTime.now());

            walletRepository.save(shipperWallet);

            log.info("Shipper wallet AFTER frozen balance: {}", shipperWallet.getFrozenBalance());
        }

        // ======================
        // TRANSACTION LOG
        // ======================

        Transaction transaction = Transaction.builder()
                .payment(payment)
                .paymentGateway(payment.getPaymentGateway())
                .amount(totalAmount)
                .balanceAfter(adminWallet.getTotalBalance())
                .status(TransactionStatus.SUCCESS)
                .content("Thanh toán đơn hàng #" + order.getId())
                .build();

        transactionRepository.save(transaction);

        log.info("Transaction created: id={}, amount={}", transaction.getId(), totalAmount);

        paymentRepository.save(payment);

        // clear cart
        cartService.clearCart();

        log.info("===== CONFIRM PAYMENT COMPLETED =====");
    }

    @Transactional
    public void cancelPaymentByOrderCode(String orderCode) {

        log.info("===== CANCEL PAYMENT START =====");
        log.info("OrderCode received: {}", orderCode);

        Payment payment = paymentRepository.findByOrderCodeForUpdate(orderCode)
                .orElseThrow(() -> new AppException(ErrorCode.PAYMENT_NOT_FOUND));

        log.info("Payment found: id={}, status={}", payment.getId(), payment.getStatus());

        //  Không cho cancel nếu đã thành công
        if (payment.getStatus() == PaymentStatus.SUCCESS) {
            log.error("Cannot cancel payment SUCCESS. OrderCode={}", orderCode);
            throw new AppException(ErrorCode.PAYMENT_ALREADY_SUCCESS);
        }

        //  Tránh cancel nhiều lần
        if (payment.getStatus() == PaymentStatus.FAILED) {
            log.warn("Payment already FAILED → skip. OrderCode={}", orderCode);
            return;
        }

        Order order = payment.getOrder();

        log.info("Order found: id={}, status={}", order.getId(), order.getStatus());

        // ======================
        // UPDATE STATUS
        // ======================

        payment.setStatus(PaymentStatus.FAILED);
        payment.setUpdatedAt(LocalDateTime.now());

        order.setStatus(OrderStatus.FAILED);
        orderRepository.save(order);

        log.info("Order status updated to FAILED");

        // ======================
        // TRANSACTION LOG (optional)
        // ======================

        Transaction transaction = Transaction.builder()
                .payment(payment)
                .paymentGateway(payment.getPaymentGateway())
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


}
