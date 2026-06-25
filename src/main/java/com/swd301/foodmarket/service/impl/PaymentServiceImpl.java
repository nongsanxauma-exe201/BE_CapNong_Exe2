package com.swd301.foodmarket.service.impl;

import com.swd301.foodmarket.dto.request.CreatePaymentRequest;
import com.swd301.foodmarket.dto.response.PaymentResponse;
import com.swd301.foodmarket.entity.*;
import com.swd301.foodmarket.entity.Notification;
import com.swd301.foodmarket.enums.OrderStatus;
import com.swd301.foodmarket.enums.PaymentStatus;
import com.swd301.foodmarket.enums.TransactionStatus;
import com.swd301.foodmarket.enums.WalletType;
import com.swd301.foodmarket.exception.AppException;
import com.swd301.foodmarket.exception.ErrorCode;
import com.swd301.foodmarket.mapper.PaymentMapper;
import com.swd301.foodmarket.repository.MysteryBoxRepository;
import com.swd301.foodmarket.repository.OrderRepository;
import com.swd301.foodmarket.repository.PaymentRepository;
import com.swd301.foodmarket.repository.ProductRepository;
import com.swd301.foodmarket.repository.TransactionRepository;
import com.swd301.foodmarket.repository.WalletRepository;
import com.swd301.foodmarket.service.CartService;
import com.swd301.foodmarket.service.NotificationService;
import com.swd301.foodmarket.service.PaymentService;
import com.swd301.foodmarket.service.PayosService;
import com.swd301.foodmarket.service.VoucherUserService;
import jakarta.transaction.Transactional;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.util.HashMap;
import java.util.Map;

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
    private final NotificationService notificationService;

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
                    .orElseThrow(() -> new AppException(ErrorCode.PAYMENT_NOT_FOUND));
        }

        return paymentMapper.toResponse(payment);
    }

    @Override
    public PaymentResponse updateStatus(Integer id, String status) {
        Payment payment = paymentRepository.findById(id)
                .orElseThrow(() -> new AppException(ErrorCode.PAYMENT_NOT_FOUND));

        payment.setStatus(PaymentStatus.valueOf(status));
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

        if (!"PAYOS".equalsIgnoreCase(order.getPaymentMethod())) {
            throw new AppException(ErrorCode.INVALID_PAYMENT_METHOD);
        }


        log.info("Order found: id={}, status={}", order.getId(), order.getStatus());

        payment.setStatus(PaymentStatus.SUCCESS);
        payment.setPaymentDate(LocalDateTime.now(ZoneId.of("Asia/Ho_Chi_Minh")));
        payment.setUpdatedAt(LocalDateTime.now(ZoneId.of("Asia/Ho_Chi_Minh")));

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

        BigDecimal shopGross = totalAmount.subtract(shippingFee);

        log.info("[PAYOS_PAYMENT_SUCCESS] orderId={}, totalAmount={}, shippingFee={}, shopGross={}",
                order.getId(),
                totalAmount,
                shippingFee,
                shopGross
        );

        log.info("Total amount: {}", totalAmount);
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
                adminWallet.getTotalBalance().add(totalAmount)
        );

// Khóa toàn bộ tiền PayOS đang nằm trong hệ thống.
// Đến DELIVERED mới tách: shop, shipper, commission, refund.
        adminWallet.setFrozenBalance(
                adminWallet.getFrozenBalance().add(totalAmount)
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
                .paymentGateway(payment.getPaymentGateway())
                .amount(totalAmount)
                .balanceAfter(adminWallet.getTotalBalance())
                .status(TransactionStatus.SUCCESS)
                .content("Thanh toán đơn hàng #" + order.getId())
                .build();

        transactionRepository.save(transaction);

        log.info("Transaction created: id={}, amount={}", transaction.getId(), totalAmount);

        paymentRepository.save(payment);

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
        voucherUserService.restoreVoucherForOrder(order);
        cartService.restoreOrderItemsToCart(order);
        order.setStatus(OrderStatus.CANCELLED);
        orderRepository.save(order);

        log.info("Order status updated to CANCELLED");

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

    @Override
    @Transactional
    public void notifyPendingPayment(String orderCode) {
        log.info("===== NOTIFY PENDING PAYMENT =====");
        log.info("OrderCode: {}", orderCode);

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
        Payment payment = paymentRepository
                .findTopByOrderIdAndStatusOrderByCreatedAtDesc(orderId, PaymentStatus.PENDING)
                .orElseThrow(() -> new AppException(ErrorCode.PAYMENT_NOT_FOUND));
        return paymentMapper.toResponse(payment);
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
                Integer currentStock = product.getStockQuantity() == null ? 0 : product.getStockQuantity();
                product.setStockQuantity(currentStock + detail.getQuantity());
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


}
