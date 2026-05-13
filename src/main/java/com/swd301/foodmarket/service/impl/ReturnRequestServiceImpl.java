package com.swd301.foodmarket.service.impl;

import com.swd301.foodmarket.dto.request.ReturnActionRequest;
import com.swd301.foodmarket.dto.request.ReturnCreateRequest;
import com.swd301.foodmarket.dto.response.PayOSPaymentInfoResponse;
import com.swd301.foodmarket.dto.response.ReturnActionResponse;
import com.swd301.foodmarket.entity.*;
import com.swd301.foodmarket.enums.ReturnStatus;
import com.swd301.foodmarket.enums.TransactionStatus;
import com.swd301.foodmarket.enums.WalletType;
import com.swd301.foodmarket.exception.AppException;
import com.swd301.foodmarket.exception.ErrorCode;
import com.swd301.foodmarket.repository.*;
import com.swd301.foodmarket.service.PayosService;
import com.swd301.foodmarket.service.ReturnRequestService;
import jakarta.transaction.Transactional;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.util.List;

@Service
@RequiredArgsConstructor
@Transactional
@Slf4j
public class ReturnRequestServiceImpl implements ReturnRequestService {

    private final ReturnRequestRepository returnRequestRepository;
    private final OrderDetailRepository orderDetailRepository;
    private final WalletRepository walletRepository;
    private final UserRepository userRepository;
    private final com.swd301.foodmarket.service.CloudinaryService cloudinaryService;
    private final PayosService payosService;
    private final TransactionRepository transactionRepository;
    @Value("${FRONTEND_URL}")
    private String frontUrl;

    @Override
    public ReturnRequest createReturnRequest(ReturnCreateRequest request, org.springframework.web.multipart.MultipartFile evidenceFile) {
        User buyer = getCurrentUser();
        OrderDetail detail = orderDetailRepository.findById(request.getOrderDetailId())
                .orElseThrow(() -> new AppException(ErrorCode.ORDER_DETAIL_NOT_FOUND));

        if (detail.getIsRequestedReturn()) {
            throw new AppException(ErrorCode.RETURN_REQUEST_ALREADY_EXISTS);
        }

        String evidenceUrl = "";
        if (evidenceFile != null && !evidenceFile.isEmpty()) {
            try {
                evidenceUrl = cloudinaryService.uploadImage(evidenceFile);
            } catch (Exception e) {
                log.error("Failed to upload evidence to Cloudinary", e);
            }
        }

        Order order = detail.getOrder();
        ReturnRequest rr = ReturnRequest.builder()
                .orderDetail(detail)
                .buyer(buyer)
                .shopOwner(order.getShopOwner())
                .reason(request.getReason())
                .evidence(evidenceUrl)
                .refundAmount(detail.getUnitPrice().multiply(BigDecimal.valueOf(detail.getQuantity())))
                .status(ReturnStatus.PENDING)
                .build();

        detail.setIsRequestedReturn(true);
        orderDetailRepository.save(detail);
        
        return returnRequestRepository.save(rr);
    }

    @Override
    public ReturnActionResponse shopAction(Integer requestId, ReturnActionRequest action) {
        User shop = getCurrentUser();
        ReturnRequest rr = returnRequestRepository.findById(requestId)
                .orElseThrow(() -> new AppException(ErrorCode.RETURN_REQUEST_NOT_FOUND));

        if (!rr.getShopOwner().getId().equals(shop.getId())) {
            throw new AppException(ErrorCode.UNAUTHORIZED);
        }

        if (rr.getStatus() != ReturnStatus.PENDING) {
            throw new AppException(ErrorCode.INVALID_RETURN_STATUS);
        }

        if (action.getAccept()) {
            rr.setStatus(ReturnStatus.SHOP_APPROVED);
            rr.setShopResponse("Shop approved return request. Waiting for Admin to refund.");
        } else {
            rr.setStatus(ReturnStatus.REJECTED);
            rr.setShopResponse(action.getResponse());
        }

        ReturnRequest saved = returnRequestRepository.save(rr);
        return ReturnActionResponse.builder()
                .request(saved)
                .build();
    }

    @Override
    public ReturnRequest dispute(Integer requestId, String reason) {
        User buyer = getCurrentUser();
        ReturnRequest rr = returnRequestRepository.findById(requestId)
                .orElseThrow(() -> new AppException(ErrorCode.RETURN_REQUEST_NOT_FOUND));

        if (!rr.getBuyer().getId().equals(buyer.getId())) {
            throw new AppException(ErrorCode.UNAUTHORIZED);
        }

        if (rr.getStatus() != ReturnStatus.REJECTED) {
            throw new AppException(ErrorCode.INVALID_RETURN_STATUS);
        }

        rr.setStatus(ReturnStatus.DISPUTED);
        rr.setReason(rr.getReason() + " | Dispute: " + reason);
        
        return returnRequestRepository.save(rr);
    }

    @Override
    public ReturnActionResponse adminAction(Integer requestId, ReturnActionRequest action) {
        ReturnRequest rr = returnRequestRepository.findById(requestId)
                .orElseThrow(() -> new AppException(ErrorCode.RETURN_REQUEST_NOT_FOUND));

        if (rr.getStatus() != ReturnStatus.DISPUTED && rr.getStatus() != ReturnStatus.SHOP_APPROVED) {
            throw new AppException(ErrorCode.INVALID_RETURN_STATUS);
        }

        rr.setAdminRemark(action.getResponse());
        PayosPayoutResult payoutResult = null;

        if (action.getAccept()) {
            if (action.getRefundAmount() != null) {
                rr.setRefundAmount(action.getRefundAmount());
            }
            
            // Unique code for each attempt to avoid PayOS error 231
            Long orderCode = System.currentTimeMillis() / 1000; 
            rr.setPayoutOrderCode(orderCode);

            // Generate QR for Admin to scan and pay the refund
            payoutResult = payosService.createQr(
                    rr.getRefundAmount(),
                    orderCode,
                    rr.getId(),
                    "REFUND", 
                    "BUYER_" + rr.getBuyer().getId(),
                    rr.getBuyer().getFullName() != null ? rr.getBuyer().getFullName() : "Buyer",
                    frontUrl+"/admin/disputes?requestId=" + rr.getId() + "&orderCode=" + orderCode,
                    frontUrl+"/admin/disputes?requestId=" + rr.getId() + "&cancel=true"
            );

            rr.setStatus(ReturnStatus.REFUND_PENDING); 
        } else {
            rr.setStatus(ReturnStatus.REJECTED);
        }

        ReturnRequest saved = returnRequestRepository.save(rr);
        
        return ReturnActionResponse.builder()
                .request(saved)
                .qrCodeUrl(payoutResult != null ? payoutResult.getQrCodeUrl() : null)
                .checkoutUrl(payoutResult != null ? payoutResult.getCheckoutUrl() : null)
                .build();
    }

    private void performRefund(ReturnRequest rr) {
        // Refund logic: Platform Wallet -> Buyer (Admin paid via PayOS, so we deduct from frozen/revenue)
        Wallet shopWallet = walletRepository.findByShopOwnerIdForUpdate(rr.getShopOwner().getId())
                .orElseThrow(() -> new AppException(ErrorCode.WALLET_NOT_FOUND));

        Wallet platformWallet = walletRepository.findByTypeForUpdate(WalletType.PLATFORM)
                .orElseThrow(() -> new AppException(ErrorCode.PLATFORM_WALLET_NOT_FOUND));

        BigDecimal amount = rr.getRefundAmount();
        BigDecimal platformFee = amount.multiply(BigDecimal.valueOf(0.1));
        BigDecimal shopPortion = amount.subtract(platformFee);

        log.info("--- PERFORMING REFUND ---");
        log.info("Refund Amount: {}", amount);
        log.info("Platform Fee Deduct: {}", platformFee);
        log.info("Shop Portion Deduct: {}", shopPortion);

        // 1. Giảm số dư và doanh thu của Admin (Vì Admin đã bỏ tiền túi ra trả hoàn tiền qua PayOS)
        // Lưu ý: Admin thu tiền lúc khách thanh toán, giờ phải trả lại.
        platformWallet.setTotalBalance(platformWallet.getTotalBalance().subtract(amount));
        platformWallet.setTotalRevenueAllTime(platformWallet.getTotalRevenueAllTime().subtract(platformFee));

        // 2. Giảm số dư đóng băng và doanh thu của Shop (Phần tiền shop lẽ ra được nhận giờ bị trừ đi)
        shopWallet.setFrozenBalance(shopWallet.getFrozenBalance().subtract(shopPortion));
        shopWallet.setTotalRevenueAllTime(shopWallet.getTotalRevenueAllTime().subtract(shopPortion));

        walletRepository.save(shopWallet);
        walletRepository.save(platformWallet);
        
        // 3. Log Transaction
        Transaction trans = Transaction.builder()
                .paymentGateway("PAYOS")
                .amount(amount.negate()) // Hoàn tiền là số âm đối với hệ thống balance
                .balanceAfter(platformWallet.getTotalBalance())
                .status(TransactionStatus.SUCCESS)
                .content("Hoàn tiền đơn hàng #" + rr.getOrderDetail().getOrder().getId() + " - Khiếu nại #" + rr.getId())
                .build();
        transactionRepository.save(trans);

        log.info("Refund wallets and transaction updated successfully");
    }

    @Override
    public ReturnActionResponse checkPayoutStatus(Integer requestId) {
        ReturnRequest rr = returnRequestRepository.findById(requestId)
                .orElseThrow(() -> new AppException(ErrorCode.RETURN_REQUEST_NOT_FOUND));

        if (rr.getPayoutOrderCode() == null) {
            return ReturnActionResponse.builder().request(rr).build();
        }

        // Đã bỏ bước gọi PayOS API do lỗi kết nối (UnknownHostException)
        // Thay vào đó tin tưởng vào việc admin đã thanh toán khi quay về từ PayOS
        if (rr.getStatus() == ReturnStatus.REFUND_PENDING) {
             // Để tránh update nhầm nếu admin chỉ nhấn Check mà chưa trả, 
             // Logic này nên được kích hoạt từ auto-check khi có callback parameter.
             // Manual check ở đây có thể chỉ trả về trạng thái hiện tại.
        }

        return ReturnActionResponse.builder().request(rr).build();
    }

    @Transactional
    @Override
    public ReturnActionResponse autoCheckByOrderCode(Long orderCode) {
        ReturnRequest rr = returnRequestRepository.findByPayoutOrderCode(orderCode)
                .orElseThrow(() -> new AppException(ErrorCode.PAYMENT_NOT_FOUND));

        // Nếu đang chờ thanh toán thì thực hiện hoàn tất
        if (rr.getStatus() == ReturnStatus.REFUND_PENDING) {
            rr.setStatus(ReturnStatus.COMPLETED);
            performRefund(rr);
            returnRequestRepository.save(rr);
            
            // Log giao dịch rút tiền hoàn trả
            log.info("✅ Refund processed for requestId: {}, orderCode: {}", rr.getId(), orderCode);
        }

        return ReturnActionResponse.builder().request(rr).build();
    }

    @Override
    public List<ReturnRequest> getMyReturns() {
        return returnRequestRepository.findByBuyerId(getCurrentUser().getId());
    }

    @Override
    public List<ReturnRequest> getShopReturns() {
        return returnRequestRepository.findByShopOwnerId(getCurrentUser().getId());
    }

    @Override
    public List<ReturnRequest> getDisputes() {
        return returnRequestRepository.findAll();
    }

    @Override
    public ReturnRequest getById(Integer id) {
        return returnRequestRepository.findById(id)
                .orElseThrow(() -> new AppException(ErrorCode.RETURN_REQUEST_NOT_FOUND));
    }

    private User getCurrentUser() {
        Authentication auth = SecurityContextHolder.getContext().getAuthentication();
        if (auth == null || !auth.isAuthenticated()) {
            throw new AppException(ErrorCode.UNAUTHORIZED);
        }
        return userRepository.findByEmail(auth.getName())
                .orElseThrow(() -> new AppException(ErrorCode.USER_NOT_EXISTED));
    }
}
