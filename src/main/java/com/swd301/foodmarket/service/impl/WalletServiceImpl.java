package com.swd301.foodmarket.service.impl;

import com.swd301.foodmarket.dto.request.WalletCreationRequest;
import com.swd301.foodmarket.dto.request.WithdrawRequestCreationRequest;
import com.swd301.foodmarket.dto.response.WalletResponse;
import com.swd301.foodmarket.dto.response.WithdrawRequestResponse;
import com.swd301.foodmarket.entity.PayosPayoutResult;
import com.swd301.foodmarket.entity.User;
import com.swd301.foodmarket.entity.Wallet;
import com.swd301.foodmarket.entity.WithdrawRequest;
import com.swd301.foodmarket.enums.PayoutStatus;
import com.swd301.foodmarket.enums.WalletType;
import com.swd301.foodmarket.enums.WithdrawRequestStatus;
import com.swd301.foodmarket.exception.AppException;
import com.swd301.foodmarket.exception.ErrorCode;
import com.swd301.foodmarket.mapper.WalletMapper;
import com.swd301.foodmarket.mapper.WithdrawRequestMapper;
import com.swd301.foodmarket.repository.UserRepository;
import com.swd301.foodmarket.repository.WalletRepository;
import com.swd301.foodmarket.repository.WithdrawRequestRepository;
import com.swd301.foodmarket.service.PayosService;
import com.swd301.foodmarket.service.WalletService;
import jakarta.transaction.Transactional;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDateTime;
import java.util.List;

@Service
@RequiredArgsConstructor
@Transactional
@Slf4j
public class WalletServiceImpl implements WalletService {
    private final WalletRepository walletRepository;
    private final WithdrawRequestRepository withdrawRequestRepository;
    private final WalletMapper walletMapper;
    private final WithdrawRequestMapper withdrawRequestMapper;
    private final UserRepository userRepository;
    private final PayosService payosService;
    private static final BigDecimal WITHDRAW_FEE_PERCENT = new BigDecimal("0.10");// 10%
    @Value("${FRONTEND_URL}")
    private String frontUrl;

    @Override
    public WalletResponse getMyWallet() {
        User currentUser = getCurrentUser();

        Wallet wallet = walletRepository.findByShopOwner_Id(currentUser.getId())
                .orElseThrow(() -> new AppException(ErrorCode.WALLET_NOT_FOUND));

        return walletMapper.toResponse(wallet);
    }

    @Override
    public WalletResponse getMyShipperWallet() {
        User currentUser = getCurrentUser();

        Wallet wallet = walletRepository.findByShipper(currentUser)
                .orElseThrow(() -> new AppException(ErrorCode.WALLET_NOT_FOUND));

        return walletMapper.toResponse(wallet);
    }

    @Override
    public WithdrawRequestResponse createShipperWithdrawRequest(WithdrawRequestCreationRequest request) {
        User shipper = getCurrentUser();

        Wallet wallet = walletRepository.findByShipper(shipper)
                .orElseThrow(() -> new AppException(ErrorCode.WALLET_NOT_FOUND));

        if (request.getAmount().compareTo(BigDecimal.ZERO) <= 0) {
            throw new AppException(ErrorCode.INVALID_AMOUNT);
        }

        if (wallet.getFrozenBalance().compareTo(request.getAmount()) < 0) {
            throw new AppException(ErrorCode.INSUFFICIENT_BALANCE);
        }

        String bankName = request.getBankName() != null && !request.getBankName().isBlank()
                ? request.getBankName() : shipper.getBankName();
        String bankAccountNumber = request.getBankAccountNumber() != null && !request.getBankAccountNumber().isBlank()
                ? request.getBankAccountNumber() : shipper.getBankAccount();
        String bankAccountHolder = request.getBankAccountHolder() != null && !request.getBankAccountHolder().isBlank()
                ? request.getBankAccountHolder() : shipper.getBankAccountHolder();

        if (bankName == null || bankAccountNumber == null || bankAccountHolder == null) {
            throw new AppException(ErrorCode.BANK_ACCOUNT_NOT_FOUND);
        }

        BigDecimal fee = BigDecimal.ZERO;
        BigDecimal receiveAmount = request.getAmount().subtract(fee);

        WithdrawRequest wr = WithdrawRequest.builder()
                .wallet(wallet)
                .amount(request.getAmount())
                .fee(fee)
                .receiveAmount(receiveAmount)
                .reason(request.getReason())
                .status(WithdrawRequestStatus.PENDING)
                .bankName(bankName)
                .bankAccountNumber(bankAccountNumber)
                .bankAccountHolder(bankAccountHolder)
                .payoutProvider("PAYOS")
                .payoutStatus(PayoutStatus.PENDING)
                .build();

        withdrawRequestRepository.save(wr);

        wallet.setFrozenBalance(wallet.getFrozenBalance().subtract(request.getAmount()));
        walletRepository.save(wallet);

        return withdrawRequestMapper.toResponse(wr);
    }

    @Override
    public List<WithdrawRequestResponse> getMyShipperWithdrawRequests() {
        User shipper = getCurrentUser();

        Wallet wallet = walletRepository.findByShipper(shipper)
                .orElseThrow(() -> new AppException(ErrorCode.WALLET_NOT_FOUND));

        return withdrawRequestMapper.toResponses(
                withdrawRequestRepository.findByWallet_Shipper_IdOrderByIdDesc(shipper.getId())
        );
    }

    @Transactional
    public WithdrawRequestResponse createWithdrawQr(Integer id) {

        WithdrawRequest wr = withdrawRequestRepository.findById(id)
                .orElseThrow(() -> new AppException(ErrorCode.WITHDRAW_REQUEST_NOT_FOUND));

        // chỉ cho phép khi chưa tạo QR
        if (wr.getStatus() != WithdrawRequestStatus.PENDING
                || wr.getPayoutStatus() != PayoutStatus.PENDING) {
            throw new AppException(ErrorCode.INVALID_WITHDRAW_STATUS);
        }

        if (wr.getReceiveAmount() == null
                || wr.getReceiveAmount().compareTo(BigDecimal.ZERO) <= 0) {
            throw new AppException(ErrorCode.INVALID_AMOUNT);
        }

        // tạo orderCode unique
        Long orderCode = Long.parseLong(
                wr.getId() + "" + System.currentTimeMillis()
        );

        PayosPayoutResult qrResult = payosService.createQr(
                wr.getReceiveAmount(),
                orderCode,
                wr.getId(),
                wr.getBankName(),
                wr.getBankAccountNumber(),
                wr.getBankAccountHolder(),
                frontUrl+"/admin/wallet?withdrawId=" + wr.getId(),
                frontUrl+"/admin/wallet?withdrawId=" + wr.getId() + "&cancel=true"
        );

        if (qrResult == null ||
                (qrResult.getCheckoutUrl() == null && qrResult.getQrCodeUrl() == null)) {
            throw new AppException(ErrorCode.PAYOS_CREATE_QR_FAILED);
        }

        wr.setPayoutProvider("PAYOS");
        wr.setPayoutStatus(PayoutStatus.CREATED);
        wr.setPayoutOrderCode(String.valueOf(orderCode));
        wr.setPayoutQrUrl(qrResult.getQrCodeUrl());
        wr.setPayoutCheckoutUrl(qrResult.getCheckoutUrl());

        withdrawRequestRepository.save(wr);

        WithdrawRequestResponse res = withdrawRequestMapper.toResponse(wr);
        res.setQrCodeUrl(qrResult.getQrCodeUrl());
        res.setCheckoutUrl(qrResult.getCheckoutUrl());

        return res;
    }
    @Override
    @Transactional
    public WithdrawRequestResponse confirmWithdrawSuccess(Integer requestId, String adminNote) {

        User admin = getCurrentUser();

        WithdrawRequest wr = withdrawRequestRepository.findById(requestId)
                .orElseThrow(() -> new AppException(ErrorCode.WITHDRAW_REQUEST_NOT_FOUND));

        if (wr.getPayoutStatus() != PayoutStatus.CREATED) {
            throw new AppException(ErrorCode.INVALID_WITHDRAW_STATUS);
        }

        Wallet wallet = wr.getWallet();
        Wallet platformWallet = walletRepository.findByType(WalletType.PLATFORM)
                .orElseThrow(() -> new AppException(ErrorCode.PLATFORM_WALLET_NOT_FOUND));

        BigDecimal amount = wr.getAmount();

        // 1. Cập nhật ví chủ sở hữu (Shop/Shipper)
        if (wallet.getType() == WalletType.SHIPPER) {
            // Khi Shipper tạo yêu cầu, tiền đã bị TRỪ khỏi frozenBalance rồi.
            // Ở đây chỉ cần tăng totalWithdrawn để thống kê.
            wallet.setTotalWithdrawn(wallet.getTotalWithdrawn().add(amount));
        } else {
            // Khi Shop tạo yêu cầu, tiền được GIỮ (add) vào frozenBalance.
            // Ở đây cần kiểm tra và TRỪ khỏi frozenBalance.
            if (wallet.getFrozenBalance().compareTo(amount) < 0) {
                throw new AppException(ErrorCode.WALLET_BALANCE_NOT_ENOUGH);
            }
            wallet.setFrozenBalance(wallet.getFrozenBalance().subtract(amount));
            wallet.setTotalWithdrawn(wallet.getTotalWithdrawn().add(amount));
        }
        walletRepository.save(wallet);

        // 2. Cập nhật ví hệ thống (Platform) - Luôn phải trừ đi phần tiền hệ thống đã bỏ ra trả
        log.error("Platform balance: {}", platformWallet.getTotalBalance());
        log.error("Receive amount: {}", wr.getReceiveAmount());

        if (platformWallet.getTotalBalance().compareTo(wr.getReceiveAmount()) < 0) {
            throw new AppException(ErrorCode.PLATFORM_BALANCE_NOT_ENOUGH);
        }

        platformWallet.setTotalWithdrawn(platformWallet.getTotalWithdrawn().add(amount));
        platformWallet.setTotalBalance(
                platformWallet.getTotalBalance().subtract(wr.getReceiveAmount())
        );
        platformWallet.setFrozenBalance(
                platformWallet.getFrozenBalance().subtract(wr.getReceiveAmount())
        );
        platformWallet.setCommissionWallet(
                platformWallet.getCommissionWallet().add(wr.getFee())
        );
        walletRepository.save(platformWallet);

        // 3. Update trạng thái yêu cầu
        wr.setAdmin(admin);
        wr.setStatus(WithdrawRequestStatus.SUCCESS);
        wr.setAdminNote(adminNote);
        wr.setProcessedAt(LocalDateTime.now());
        wr.setPayoutStatus(PayoutStatus.SUCCESS);
        wr.setPayoutAt(LocalDateTime.now());

        withdrawRequestRepository.save(wr);

        return withdrawRequestMapper.toResponse(wr);
    }
    @Override
    public WithdrawRequestResponse createWithdrawRequest(WithdrawRequestCreationRequest request) {

        User shopOwner = getCurrentUser();

        Wallet wallet = walletRepository.findByShopOwner(shopOwner)
                .orElseThrow(() -> new AppException(ErrorCode.WALLET_NOT_FOUND));

        if (request.getAmount().compareTo(BigDecimal.ZERO) <= 0) {
            throw new AppException(ErrorCode.INVALID_AMOUNT);
        }

        if (wallet.getTotalBalance().compareTo(request.getAmount()) < 0) {
            throw new AppException(ErrorCode.INSUFFICIENT_BALANCE);
        }

        String bankName = request.getBankName();
        String bankAccountNumber = request.getBankAccountNumber();
        String bankAccountHolder = request.getBankAccountHolder();

        // Nếu FE không truyền → lấy từ hồ sơ shop owner
        if (bankName == null || bankName.isBlank()) {
            bankName = shopOwner.getBankName();

        }

        if (bankAccountNumber == null || bankAccountNumber.isBlank()) {
            bankAccountNumber = shopOwner.getBankAccount();
        }

        if (bankAccountHolder == null || bankAccountHolder.isBlank()) {
            bankAccountHolder = shopOwner.getBankAccountHolder();
        }

        if (bankName == null || bankAccountNumber == null || bankAccountHolder == null) {
            throw new AppException(ErrorCode.BANK_ACCOUNT_NOT_FOUND);
        }

        BigDecimal fee = request.getAmount().multiply(new BigDecimal("0"));
        BigDecimal receiveAmount = request.getAmount().subtract(fee);

        WithdrawRequest wr = WithdrawRequest.builder()
                .wallet(wallet)
                .amount(request.getAmount())
                .fee(fee)
                .receiveAmount(receiveAmount)
                .reason(request.getReason())
                .status(WithdrawRequestStatus.PENDING)
                .bankName(bankName)
                .bankAccountNumber(bankAccountNumber)
                .bankAccountHolder(bankAccountHolder)
                .payoutProvider("PAYOS")
                .payoutStatus(PayoutStatus.PENDING)
                .build();

        withdrawRequestRepository.save(wr);

        wallet.setTotalBalance(wallet.getTotalBalance().subtract(request.getAmount()));
        wallet.setFrozenBalance(wallet.getFrozenBalance().add(request.getAmount()));
        walletRepository.save(wallet);

        return withdrawRequestMapper.toResponse(wr);
    }


    @Override
    public WithdrawRequestResponse rejectWithdraw(Integer requestId, String adminNote) {
        User admin = getCurrentUser();

        WithdrawRequest wr = withdrawRequestRepository.findById(requestId)
                .orElseThrow(() -> new AppException(ErrorCode.WITHDRAW_REQUEST_NOT_FOUND));

        if (wr.getStatus() != WithdrawRequestStatus.PENDING) {
            throw new AppException(ErrorCode.WITHDRAW_REQUEST_ALREADY_PROCESSED);
        }
        wr.setAdmin(admin);
        wr.setStatus(WithdrawRequestStatus.REJECTED);
        wr.setAdminNote(adminNote);
        wr.setProcessedAt(LocalDateTime.now());

        Wallet wallet = wr.getWallet();
        if (wallet.getType() == WalletType.SHIPPER) {
            wallet.setFrozenBalance(wallet.getFrozenBalance().add(wr.getAmount()));
        } else {
            wallet.setFrozenBalance(wallet.getFrozenBalance().subtract(wr.getAmount()));
            wallet.setTotalBalance(wallet.getTotalBalance().add(wr.getAmount())); // trả tiền lại
        }
        walletRepository.save(wallet);

        return withdrawRequestMapper.toResponse(wr);
    }

    @Override
    public List<WithdrawRequestResponse> getPendingWithdrawRequests() {
        return withdrawRequestMapper.toResponses(
                withdrawRequestRepository.findByStatus(WithdrawRequestStatus.PENDING)
        );
    }

    private User getCurrentUser() {
        Authentication auth = SecurityContextHolder.getContext().getAuthentication();

        if (auth == null || !auth.isAuthenticated()) {
            throw new AppException(ErrorCode.UNAUTHORIZED);
        }

        String email = auth.getName();

        return userRepository.findByEmail(email)
                .orElseThrow(() -> new AppException(ErrorCode.USER_NOT_EXISTED));
    }

    @Override
    public List<WithdrawRequestResponse> getMyWithdrawRequests() {
        User shopOwner = getCurrentUser();

        Wallet wallet = walletRepository.findByShopOwner(shopOwner)
                .orElseThrow(() -> new AppException(ErrorCode.WALLET_NOT_FOUND));

        return withdrawRequestMapper.toResponses(
                withdrawRequestRepository.findByWallet_ShopOwner_IdOrderByIdDesc(shopOwner.getId())
        );
    }

    @Override
    public List<WithdrawRequestResponse> getAllWithdrawRequests() {
        User admin = getCurrentUser();

        // nếu muốn chặt hơn có thể check role ADMIN ở đây
        return withdrawRequestMapper.toResponses(
                withdrawRequestRepository.findAllByOrderByIdDesc()
        );
    }
}