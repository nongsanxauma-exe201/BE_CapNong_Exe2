package com.swd301.foodmarket.service.impl;

import com.swd301.foodmarket.constant.BankBinConstants;
import com.swd301.foodmarket.dto.request.WalletCreationRequest;
import com.swd301.foodmarket.dto.request.WithdrawRequestCreationRequest;
import com.swd301.foodmarket.dto.response.WalletResponse;
import com.swd301.foodmarket.dto.response.WithdrawRequestResponse;
import com.swd301.foodmarket.entity.*;
import com.swd301.foodmarket.enums.PayoutStatus;
import com.swd301.foodmarket.enums.RoleName;
import com.swd301.foodmarket.enums.TransactionStatus;
import com.swd301.foodmarket.enums.TransactionType;
import com.swd301.foodmarket.enums.WalletType;
import com.swd301.foodmarket.enums.WithdrawRequestStatus;
import com.swd301.foodmarket.exception.AppException;
import com.swd301.foodmarket.exception.ErrorCode;
import com.swd301.foodmarket.mapper.WalletMapper;
import com.swd301.foodmarket.mapper.WithdrawRequestMapper;
import com.swd301.foodmarket.repository.ImageRepository;
import com.swd301.foodmarket.repository.TransactionRepository;
import com.swd301.foodmarket.repository.UserRepository;
import com.swd301.foodmarket.repository.WalletRepository;
import com.swd301.foodmarket.repository.WithdrawRequestRepository;
import com.swd301.foodmarket.service.CloudinaryService;
import com.swd301.foodmarket.service.PayosService;
import com.swd301.foodmarket.service.WalletService;
import jakarta.transaction.Transactional;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDateTime;
import java.time.ZoneId;
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
    private final TransactionRepository transactionRepository;
    private final VieQRServiceImpl vieQRService;
    private final CloudinaryService cloudinaryService;
    private static final BigDecimal WITHDRAW_FEE_PERCENT = new BigDecimal("0.10");// 10%
    private final ImageRepository imageRepository;
    @Value("${FRONTEND_URL}")
    private String frontUrl;

    @Override
    public WalletResponse getMyWallet() {
        User currentUser = getCurrentUser();
        Role role = currentUser.getRole();
        Wallet wallet = null;
        if(role.getName().equals(RoleName.SHOP_OWNER)){
             wallet = walletRepository.findByShopOwner_Id(currentUser.getId()).get();
        }
        if(role.getName() == RoleName.BUYER){
             wallet = walletRepository.findByBuyer(currentUser);
        }
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
    public WalletResponse getPlatformWallet() {
        // Authorization is handled at controller level (@PreAuthorize ADMIN)
        Wallet platformWallet = walletRepository.findByType(WalletType.PLATFORM)
                .orElseThrow(() -> new AppException(ErrorCode.PLATFORM_WALLET_NOT_FOUND));
        return walletMapper.toResponse(platformWallet);
    }

    @Override
    public WithdrawRequestResponse createShipperWithdrawRequest(WithdrawRequestCreationRequest request) {
        User shipper = getCurrentUser();

        Wallet wallet = walletRepository.findByShipper(shipper)
                .orElseThrow(() -> new AppException(ErrorCode.WALLET_NOT_FOUND));

        if (request.getAmount().compareTo(BigDecimal.ZERO) <= 0) {
            throw new AppException(ErrorCode.INVALID_AMOUNT);
        }

        if (availableBalance(wallet).compareTo(request.getAmount()) < 0) {
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

        wallet.setFrozenBalance(wallet.getFrozenBalance().add(request.getAmount()));
        walletRepository.save(wallet);

        saveTransaction(TransactionType.WITHDRAW_REQUEST_FREEZE, request.getAmount(),
                wallet.getFrozenBalance(), "WITHDRAW_" + wr.getId(),
                "Khóa tiền shipper khi tạo yêu cầu rút #" + wr.getId());

        return withdrawRequestMapper.toResponse(wr);
    }

    @Override
    public List<WithdrawRequestResponse> getMyShipperWithdrawRequests() {
        User shipper = getCurrentUser();

//        Wallet wallet = walletRepository.findByShipper(shipper)
//                .orElseThrow(() -> new AppException(ErrorCode.WALLET_NOT_FOUND));

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
//
//        // tạo orderCode unique
//        Long orderCode = Long.parseLong(
//                wr.getId() + "" + System.currentTimeMillis()
//        );
//
//        PayosPayoutResult qrResult = payosService.createQr(
//                wr.getReceiveAmount(),
//                orderCode,
//                wr.getId(),
//                wr.getBankName(),
//                wr.getBankAccountNumber(),
//                wr.getBankAccountHolder(),
//                frontUrl+"/admin/wallet?withdrawId=" + wr.getId(),
//                frontUrl+"/admin/wallet?withdrawId=" + wr.getId() + "&cancel=true"
//        );
//
//        if (qrResult == null ||
//                (qrResult.getCheckoutUrl() == null && qrResult.getQrCodeUrl() == null)) {
//            throw new AppException(ErrorCode.PAYOS_CREATE_QR_FAILED);
//        }
       String qrUrl = vieQRService.generateVieQRCode(
               String.valueOf(BankBinConstants.getBankBinIgnoreCase(wr.getBankName())),
                wr.getBankAccountNumber(),
               wr.getReceiveAmount(),
                null,
                wr.getBankAccountHolder()
        );
        wr.setPayoutProvider("VietQR");
        wr.setPayoutStatus(PayoutStatus.CREATED);
//        wr.setPayoutOrderCode(String.valueOf(orderCode));
        wr.setPayoutQrUrl(qrUrl);
//        wr.setPayoutCheckoutUrl(qrResult.getCheckoutUrl());

        withdrawRequestRepository.save(wr);

        WithdrawRequestResponse res = withdrawRequestMapper.toResponse(wr);
        res.setQrCodeUrl(qrUrl);
//        res.setCheckoutUrl(qrResult.getCheckoutUrl());

        return res;
    }
    @Override
    @Transactional
    public WithdrawRequestResponse confirmWithdrawSuccess(Integer requestId, String adminNote, List<MultipartFile> evidents) {

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
        if (wallet.getFrozenBalance().compareTo(amount) < 0) {
            throw new AppException(ErrorCode.WALLET_BALANCE_NOT_ENOUGH);
        }

        wallet.setFrozenBalance(wallet.getFrozenBalance().subtract(amount));
        wallet.setTotalBalance(wallet.getTotalBalance().subtract(amount));
        wallet.setTotalWithdrawn(wallet.getTotalWithdrawn().add(amount));
        walletRepository.save(wallet);

        saveTransaction(TransactionType.WITHDRAW_SUCCESS, amount.negate(),
                wallet.getTotalBalance(), "WITHDRAW_" + wr.getId(),
                "Rút tiền thành công yêu cầu #" + wr.getId());


        // 2. Cập nhật ví hệ thống (Platform) - Luôn phải trừ đi phần tiền hệ thống đã bỏ ra trả
        log.error("Platform balance: {}", platformWallet.getTotalBalance());
        log.error("Receive amount: {}", wr.getReceiveAmount());

        BigDecimal withdrawAmount = wr.getAmount();
        BigDecimal receiveAmount = wr.getReceiveAmount();
        BigDecimal fee = wr.getFee() != null ? wr.getFee() : BigDecimal.ZERO;

        if (platformWallet.getTotalBalance().compareTo(receiveAmount) < 0) {
            throw new AppException(ErrorCode.PLATFORM_BALANCE_NOT_ENOUGH);
        }

        if (platformWallet.getFrozenBalance().compareTo(withdrawAmount) < 0) {
            throw new AppException(ErrorCode.PLATFORM_BALANCE_NOT_ENOUGH);
        }

        platformWallet.setTotalBalance(
                platformWallet.getTotalBalance().subtract(receiveAmount)
        );

        platformWallet.setFrozenBalance(
                platformWallet.getFrozenBalance().subtract(withdrawAmount)
        );

        platformWallet.setTotalWithdrawn(
                platformWallet.getTotalWithdrawn().add(receiveAmount)
        );

        platformWallet.setCommissionWallet(
                platformWallet.getCommissionWallet().add(fee)
        );

        walletRepository.save(platformWallet);

        saveTransaction(TransactionType.PLATFORM_PAYOUT, receiveAmount.negate(),
                platformWallet.getTotalBalance(), "WITHDRAW_" + wr.getId(),
                "Platform chi trả rút tiền yêu cầu #" + wr.getId());

        // 3. Update trạng thái yêu cầu
        wr.setAdmin(admin);
        wr.setStatus(WithdrawRequestStatus.SUCCESS);
        wr.setAdminNote(adminNote);
        wr.setProcessedAt(LocalDateTime.now(ZoneId.of("Asia/Ho_Chi_Minh")));
        wr.setPayoutStatus(PayoutStatus.SUCCESS);
        wr.setPayoutAt(LocalDateTime.now(ZoneId.of("Asia/Ho_Chi_Minh")));

        withdrawRequestRepository.save(wr);
        // insert evident
        saveEvidnetFile(evidents,wr);

        return withdrawRequestMapper.toResponse(wr);
    }
    public  void saveEvidnetFile( List<MultipartFile> evidents, WithdrawRequest wr) {
        if(evidents != null && evidents.size() > 0) {
            for (MultipartFile file : evidents) {
                imageRepository.save(Image.builder()
                        .withdrawRequest(wr)
                        .imageUrl(cloudinaryService.uploadImage(file))
                        .build());
            }
        }
    }
    @Override
    public WithdrawRequestResponse createWithdrawRequest(WithdrawRequestCreationRequest request) {

        User shopOwner = getCurrentUser();

        Wallet wallet = walletRepository.findByShopOwner(shopOwner)
                .orElseThrow(() -> new AppException(ErrorCode.WALLET_NOT_FOUND));

        if (request.getAmount().compareTo(BigDecimal.ZERO) <= 0) {
            throw new AppException(ErrorCode.INVALID_AMOUNT);
        }
log.info("Wallet balance: {}, request amount {}", wallet.getTotalBalance(),request.getAmount());
        if (availableBalance(wallet).compareTo(request.getAmount()) < 0) {
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

        wallet.setFrozenBalance(wallet.getFrozenBalance().add(request.getAmount()));
        walletRepository.save(wallet);

        saveTransaction(TransactionType.WITHDRAW_REQUEST_FREEZE, request.getAmount(),
                wallet.getFrozenBalance(), "WITHDRAW_" + wr.getId(),
                "Khóa tiền shop khi tạo yêu cầu rút #" + wr.getId());

        return withdrawRequestMapper.toResponse(wr);
    }


    @Override
    public WithdrawRequestResponse rejectWithdraw(Integer requestId, String adminNote, List<MultipartFile> files) {
        User admin = getCurrentUser();

        WithdrawRequest wr = withdrawRequestRepository.findById(requestId)
                .orElseThrow(() -> new AppException(ErrorCode.WITHDRAW_REQUEST_NOT_FOUND));

        if (wr.getStatus() != WithdrawRequestStatus.PENDING) {
            throw new AppException(ErrorCode.WITHDRAW_REQUEST_ALREADY_PROCESSED);
        }
        wr.setAdmin(admin);
        wr.setStatus(WithdrawRequestStatus.REJECTED);
        wr.setAdminNote(adminNote);
        wr.setProcessedAt(LocalDateTime.now(ZoneId.of("Asia/Ho_Chi_Minh")));

        Wallet wallet = wr.getWallet();

        if (wallet.getFrozenBalance().compareTo(wr.getAmount()) < 0) {
            throw new AppException(ErrorCode.WALLET_BALANCE_NOT_ENOUGH);
        }

        wallet.setFrozenBalance(
                wallet.getFrozenBalance().subtract(wr.getAmount())
        );

        walletRepository.save(wallet);
        saveTransaction(TransactionType.WITHDRAW_REJECT_RELEASE, wr.getAmount(),
                wallet.getFrozenBalance(), "WITHDRAW_" + wr.getId(),
                "Mở khóa tiền do từ chối yêu cầu rút #" + wr.getId());
        // save image
        saveEvidnetFile(files,wr);

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
        User currentUser = getCurrentUser();
        Role role = currentUser.getRole();
        if(role.getName().equals(RoleName.SHOP_OWNER)){
            return withdrawRequestMapper.toResponses(
                    withdrawRequestRepository.findByWallet_ShopOwner_IdOrderByIdDesc(currentUser.getId())
            );
        }
        if(role.getName() == RoleName.BUYER){
            return withdrawRequestMapper.toResponses(
                    withdrawRequestRepository.findByWallet_Buyer_IdOrderByIdDesc(currentUser.getId())
            );
        }


        return null;
    }

    @Override
    public List<WithdrawRequestResponse> getAllWithdrawRequests() {
        User admin = getCurrentUser();

        // nếu muốn chặt hơn có thể check role ADMIN ở đây
        return withdrawRequestMapper.toResponses(
                withdrawRequestRepository.findAllByOrderByIdDesc()
        );
    }
    private BigDecimal availableBalance(Wallet wallet) {
        BigDecimal total = wallet.getTotalBalance() != null ? wallet.getTotalBalance() : BigDecimal.ZERO;
        BigDecimal frozen = wallet.getFrozenBalance() != null ? wallet.getFrozenBalance() : BigDecimal.ZERO;
        return total.subtract(frozen);
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

}