package com.swd301.foodmarket.service.impl;

import com.swd301.foodmarket.dto.request.GetVoucherForOrder;
import com.swd301.foodmarket.dto.response.VoucherUsageResponse;
import com.swd301.foodmarket.dto.response.VoucherUserResponse;
import com.swd301.foodmarket.entity.*;
import com.swd301.foodmarket.enums.TransactionStatus;
import com.swd301.foodmarket.enums.TransactionType;
import com.swd301.foodmarket.enums.VoucherType;
import com.swd301.foodmarket.exception.AppException;
import com.swd301.foodmarket.exception.ErrorCode;
import com.swd301.foodmarket.mapper.VoucherUsageMapper;
import com.swd301.foodmarket.mapper.VoucherUserMapper;
import com.swd301.foodmarket.repository.UserRepository;
import com.swd301.foodmarket.repository.TransactionRepository;
import com.swd301.foodmarket.repository.VoucherRespository;
import com.swd301.foodmarket.repository.VoucherUsageRepository;
import com.swd301.foodmarket.repository.VoucherUserRepository;
import jakarta.transaction.Transactional;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

@RequiredArgsConstructor
@Service
public class VoucherUserServiceImpl  implements  com.swd301.foodmarket.service.VoucherUserService {

    private final VoucherUserRepository voucherUserRepository;
    private final VoucherRespository voucherRespository;
    private final VoucherUserMapper voucherUserMapper;
    private final VoucherUsageRepository voucherUsageRepository;
    private final VoucherUsageMapper voucherUsageMapper;
    private final UserRepository userRepository;
    private final TransactionRepository transactionRepository;
    @Transactional
    @Override
    public VoucherUserResponse createVoucherUser(String voucherCode) {
        Voucher voucher = voucherRespository.findByVoucherCode(voucherCode);
        if (voucher == null) {
            throw  new AppException(ErrorCode.VOUCHER_NOT_FOUND);
        }
       if(voucher.getClaimLimit() != null && voucher.getClaimedCount() + 1 > voucher.getClaimLimit()) {
           throw new AppException(ErrorCode.VOUCHER_RECEIVE_LIMIT_REACHED);
       }
       if(voucherUserRepository.existsByUserAndVoucher(getCurrentUser(), voucher)) {
           throw new AppException(ErrorCode.VOUCHER_ALREADY_CLAIMED);
       }
       if(voucher.getExpiryDate() != null && voucher.getExpiryDate().isBefore(LocalDateTime.now())) {
           throw new AppException(ErrorCode.VOUCHER_EXPIRED);
       }
       if(!voucher.isActive()){
           throw  new AppException(ErrorCode.VOUCHER_INACTIVE);
       }
       VoucherUser voucherUser = VoucherUser.builder()
               .user(getCurrentUser())
               .voucher(voucher)
               .quantity(voucher.getPerUserLimit() == null ? 1 : voucher.getPerUserLimit())
               .build();
       voucher.setClaimedCount(voucher.getClaimedCount() + 1);
       voucherRespository.save(voucher);
        return voucherUserMapper.toVoucherUserResponse(voucherUserRepository.save(voucherUser));
    }

    @Override
    public Page<VoucherUserResponse> getMyVouchers(int page, int size) {
        Pageable pageable = PageRequest.of(page, size);
        Page<VoucherUser> voucherUsers = voucherUserRepository.getMyVoucherOfBuyerCanUse(
                getCurrentUser(), LocalDateTime.now(),true, pageable);
        return voucherUsers.map(voucherUserMapper::toVoucherUserResponse);
    }

    @Override
    public VoucherUserResponse getVoucherUserById(UUID id) {
            VoucherUser voucherUser = voucherUserRepository.findById(id).orElseThrow(
                    () -> new AppException(ErrorCode.VOUCHER_USER_NOT_FOUND)
            );
            return voucherUserMapper.toVoucherUserResponse(voucherUser);
    }

    @Override
    public boolean canRecieveVoucher(String voucherCode) {
        Voucher voucher = voucherRespository.findByVoucherCode(voucherCode);
        if (voucher == null) {
            throw new AppException(ErrorCode.VOUCHER_NOT_FOUND);
        }
        if(voucher.getClaimLimit() != null && voucher.getClaimedCount() + 1 > voucher.getClaimLimit()) {
           return  false;
        }
        if(voucherUserRepository.existsByUserAndVoucher(getCurrentUser(), voucher)) {
            return  false;
        }
        if(voucher.getExpiryDate() != null && voucher.getExpiryDate().isBefore(LocalDateTime.now())) {
            return  false;
        }
        if(!voucher.isActive()){
            return  false;
        }
        return true;
    }

    @Override
    public Page<VoucherUserResponse> getVoucherForOrder(GetVoucherForOrder request,int page, int size) {
        Pageable pageable = PageRequest.of(page, size);
          User shop = userRepository.findById(request.getShopId()).orElseThrow(
                  () -> new AppException(ErrorCode.USER_NOT_EXISTED)
          );
        Page<VoucherUser> voucherUsers = voucherUserRepository.
                getMyVoucherForOrder(
                        getCurrentUser(), LocalDateTime.now(),true,request.getAmount(),shop ,pageable
                );
        return voucherUsers.map(voucherUserMapper::toVoucherUserResponse);
    }

    @Override
    public VoucherUsageResponse useVoucherForOrder(Order order, List<String> voucherIdentifiers) {
        if (order == null || voucherIdentifiers == null || voucherIdentifiers.isEmpty()) {
            return null;
        }

        User currentUser = getCurrentUser();
        VoucherUser voucherUser = resolveVoucherUser(currentUser, voucherIdentifiers)
                .orElseThrow(() -> new AppException(ErrorCode.VOUCHER_USER_NOT_FOUND));

        Voucher voucher = voucherUser.getVoucher();
        validateVoucherForOrder(voucherUser, order);

        BigDecimal shippingFee = order.getShippingFee() == null ? BigDecimal.ZERO : order.getShippingFee();
        BigDecimal orderSubtotal = order.getTotalAmount().subtract(shippingFee);
        if (orderSubtotal.compareTo(BigDecimal.ZERO) < 0) {
            orderSubtotal = BigDecimal.ZERO;
        }

        BigDecimal discountAmount = calculateDiscountAmount(voucher, orderSubtotal, shippingFee);
        if (discountAmount.compareTo(BigDecimal.ZERO) <= 0) {
            return null;
        }

        voucherUser.setQuantityUsed((voucherUser.getQuantityUsed() == null ? 0 : voucherUser.getQuantityUsed()) + 1);
        voucherUser.setQuantity(Math.max(0, (voucherUser.getQuantity() == null ? 0 : voucherUser.getQuantity()) - 1));
        voucherUserRepository.save(voucherUser);

        voucher.setUsedCount((voucher.getUsedCount() == null ? 0 : voucher.getUsedCount()) + 1);
        if (voucher.getUsageLimit() != null && voucher.getUsedCount() >= voucher.getUsageLimit()) {
            voucher.setActive(false);
        }
        voucherRespository.save(voucher);

        BigDecimal newTotal = order.getTotalAmount().subtract(discountAmount);
        order.setTotalAmount(newTotal.max(BigDecimal.ZERO));
        if (voucher.getVoucherType() == VoucherType.WELCOME) {
            BigDecimal currentShippingDiscount = order.getShippingDiscountAmount() == null
                    ? BigDecimal.ZERO
                    : order.getShippingDiscountAmount();
            order.setShippingDiscountAmount(currentShippingDiscount.add(discountAmount));
            saveTransaction(TransactionType.SHIPPING_SUBSIDY, discountAmount.negate(), null,
                    "ORDER_" + order.getId(),
                    "Admin/platform bù phí vận chuyển bằng welcome voucher cho đơn #" + order.getId());
        }

        VoucherUsage usage = VoucherUsage.builder()
                .voucher(voucher)
                .user(currentUser)
                .order(order)
                .discountAmount(discountAmount)
                .usedAt(LocalDateTime.now())
                .build();

        return voucherUsageMapper.toResponse(voucherUsageRepository.save(usage));
    }

    @Transactional
    @Override
    public void restoreVoucherForOrder(Order order) {
        if (order == null || order.getId() == null) {
            return;
        }

        Optional<VoucherUsage> usageOptional = voucherUsageRepository.findByOrder_Id(order.getId());
        if (usageOptional.isEmpty()) {
            return;
        }

        VoucherUsage usage = usageOptional.get();
        Voucher voucher = usage.getVoucher();
        if (voucher == null) {
            voucherUsageRepository.delete(usage);
            return;
        }

        Optional<VoucherUser> voucherUserOptional = voucherUserRepository.findByUserAndVoucher(usage.getUser(), voucher);
        voucherUserOptional.ifPresent(voucherUser -> {
            int currentUsed = voucherUser.getQuantityUsed() == null ? 0 : voucherUser.getQuantityUsed();
            int currentRemaining = voucherUser.getQuantity() == null ? 0 : voucherUser.getQuantity();
            voucherUser.setQuantityUsed(Math.max(0, currentUsed - 1));
            voucherUser.setQuantity(currentRemaining + 1);
            voucherUserRepository.save(voucherUser);
        });

        int usedCount = voucher.getUsedCount() == null ? 0 : voucher.getUsedCount();
        voucher.setUsedCount(Math.max(0, usedCount - 1));
        if (voucher.getUsageLimit() != null
                && voucher.getUsedCount() < voucher.getUsageLimit()
                && voucher.getExpiryDate() != null
                && voucher.getExpiryDate().isAfter(LocalDateTime.now())) {
            voucher.setActive(true);
        }
        voucherRespository.save(voucher);

        BigDecimal discountAmount = usage.getDiscountAmount() == null ? BigDecimal.ZERO : usage.getDiscountAmount();
        if (discountAmount.compareTo(BigDecimal.ZERO) > 0 && order.getTotalAmount() != null) {
            order.setTotalAmount(order.getTotalAmount().add(discountAmount));
            if (usage.getVoucher() != null && usage.getVoucher().getVoucherType() == VoucherType.WELCOME) {
                BigDecimal currentShippingDiscount = order.getShippingDiscountAmount() == null
                        ? BigDecimal.ZERO
                        : order.getShippingDiscountAmount();
                order.setShippingDiscountAmount(currentShippingDiscount.subtract(discountAmount).max(BigDecimal.ZERO));
            }
        }

        voucherUsageRepository.delete(usage);
    }

    private Optional<VoucherUser> resolveVoucherUser(User currentUser, List<String> voucherIdentifiers) {
        for (String rawIdentifier : voucherIdentifiers) {
            if (rawIdentifier == null || rawIdentifier.isBlank()) {
                continue;
            }
            String identifier = rawIdentifier.trim();

            try {
                UUID voucherUserId = UUID.fromString(identifier);
                Optional<VoucherUser> byId = voucherUserRepository.findByIdAndUser(voucherUserId, currentUser);
                if (byId.isPresent()) {
                    return byId;
                }
            } catch (IllegalArgumentException ignored) {
                // Not a UUID, continue checking by voucher code.
            }

            Optional<VoucherUser> byCode = voucherUserRepository.findByUserAndVoucher_VoucherCode(currentUser, identifier);
            if (byCode.isPresent()) {
                return byCode;
            }
        }
        return Optional.empty();
    }

    private void validateVoucherForOrder(VoucherUser voucherUser, Order order) {
        Voucher voucher = voucherUser.getVoucher();
        if (voucher == null) {
            throw new AppException(ErrorCode.VOUCHER_NOT_FOUND);
        }
        if (!voucher.isActive()) {
            throw new AppException(ErrorCode.VOUCHER_INACTIVE);
        }
        if (voucher.getExpiryDate() != null && voucher.getExpiryDate().isBefore(LocalDateTime.now())) {
            throw new AppException(ErrorCode.VOUCHER_EXPIRED);
        }

        int remainingQuantity = voucherUser.getQuantity() == null ? 0 : voucherUser.getQuantity();
        if (remainingQuantity <= 0) {
            throw new AppException(ErrorCode.INVALID_REQUEST);
        }

        int usedCount = voucher.getUsedCount() == null ? 0 : voucher.getUsedCount();
        if (voucher.getUsageLimit() != null && usedCount >= voucher.getUsageLimit()) {
            throw new AppException(ErrorCode.VOUCHER_INACTIVE);
        }

        boolean isPlatformVoucher = voucher.getVoucherType() == VoucherType.PLATFORM
                || voucher.getVoucherType() == VoucherType.WELCOME;
        boolean isShopVoucherForOrder = voucher.getShopOwner() != null
                && order.getShopOwner() != null
                && voucher.getShopOwner().getId().equals(order.getShopOwner().getId());
        if (!isPlatformVoucher && !isShopVoucherForOrder) {
            throw new AppException(ErrorCode.INVALID_REQUEST);
        }

        BigDecimal shippingFee = order.getShippingFee() == null ? BigDecimal.ZERO : order.getShippingFee();
        BigDecimal subtotal = order.getTotalAmount().subtract(shippingFee);
        if (subtotal.compareTo(BigDecimal.ZERO) < 0) {
            subtotal = BigDecimal.ZERO;
        }
        if (voucher.getMinOrderValue() != null && subtotal.compareTo(voucher.getMinOrderValue()) < 0) {
            throw new AppException(ErrorCode.INVALID_REQUEST);
        }
    }

    private BigDecimal calculateDiscountAmount(Voucher voucher, BigDecimal baseAmount, BigDecimal shippingFee) {
        if (voucher.getVoucherType() == VoucherType.WELCOME) {
            BigDecimal shippingBase = shippingFee == null ? BigDecimal.ZERO : shippingFee;
            if (shippingBase.compareTo(BigDecimal.ZERO) <= 0) {
                return BigDecimal.ZERO;
            }
            BigDecimal discount = shippingBase;
            if (voucher.getMaxDiscount() != null && voucher.getMaxDiscount().compareTo(BigDecimal.ZERO) > 0) {
                discount = discount.min(voucher.getMaxDiscount());
            }
            return discount.min(shippingBase);
        }

        if (voucher.getDiscountValue() == null || baseAmount.compareTo(BigDecimal.ZERO) <= 0) {
            return BigDecimal.ZERO;
        }

        BigDecimal discount = baseAmount
                .multiply(voucher.getDiscountValue())
                .divide(BigDecimal.valueOf(100), 2, RoundingMode.HALF_UP);

        if (voucher.getMaxDiscount() != null && voucher.getMaxDiscount().compareTo(BigDecimal.ZERO) > 0) {
            discount = discount.min(voucher.getMaxDiscount());
        }

        return discount.min(baseAmount);
    }

    private void saveTransaction(TransactionType type, BigDecimal amount, BigDecimal balanceAfter, String referenceId, String content) {
        transactionRepository.save(Transaction.builder()
                .paymentGateway("VOUCHER")
                .transactionType(type)
                .referenceId(referenceId)
                .amount(amount)
                .balanceAfter(balanceAfter)
                .status(TransactionStatus.SUCCESS)
                .content(content)
                .build());
    }

    private User getCurrentUser() {
        String name = SecurityContextHolder.getContext().getAuthentication().getName();
        User user = userRepository.findByEmail(name).orElseThrow(
                () -> new AppException(ErrorCode.USER_NOT_EXISTED)
        );
        return user;
    }
}
