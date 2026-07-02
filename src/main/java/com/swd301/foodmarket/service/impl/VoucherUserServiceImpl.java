package com.swd301.foodmarket.service.impl;

import com.swd301.foodmarket.dto.request.GetVoucherForBatch;
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
import java.util.ArrayList;
import java.util.HashSet;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.UUID;
import java.util.function.Function;

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
    public Page<VoucherUserResponse> getVoucherForBatch(GetVoucherForBatch request, int page, int size) {
        Pageable pageable = PageRequest.of(page, size);
        BigDecimal amount = request.getAmount() == null ? BigDecimal.ZERO : request.getAmount();
        Page<VoucherUser> voucherUsers = voucherUserRepository.getMyBatchVouchers(
                getCurrentUser(), LocalDateTime.now(), true, amount, pageable);
        return voucherUsers.map(voucherUserMapper::toVoucherUserResponse);
    }

    @Override
    public VoucherUsageResponse useVoucherForOrder(Order order, List<String> voucherIdentifiers) {
        return useVoucherForOrder(order, voucherIdentifiers, false);
    }

    @Override
    public VoucherUsageResponse useVoucherForOrder(Order order, List<String> voucherIdentifiers, boolean shopVouchersOnly) {
        if (order == null || voucherIdentifiers == null || voucherIdentifiers.isEmpty()) {
            return null;
        }

        User currentUser = getCurrentUser();
        VoucherUsageResponse lastUsageResponse = null;
        Set<UUID> appliedVoucherUserIds = new HashSet<>();
        for (String voucherIdentifier : normalizeVoucherIdentifiers(voucherIdentifiers)) {
            VoucherUser voucherUser = resolveVoucherUser(currentUser, List.of(voucherIdentifier))
                    .orElseThrow(() -> new AppException(ErrorCode.VOUCHER_USER_NOT_FOUND));
            if (voucherUser.getId() != null && !appliedVoucherUserIds.add(voucherUser.getId())) {
                continue;
            }

            Voucher voucher = voucherUser.getVoucher();
            validateVoucherForOrder(voucherUser, order, shopVouchersOnly);

            BigDecimal orderSubtotal = calculateRemainingProductSubtotal(order);
            BigDecimal remainingShippingFee = calculateRemainingShippingFee(order);

            BigDecimal discountAmount = calculateDiscountAmount(voucher, orderSubtotal, remainingShippingFee);
            if (discountAmount.compareTo(BigDecimal.ZERO) <= 0) {
                continue;
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
            } else if (voucher.getVoucherType() == VoucherType.PLATFORM) {
                BigDecimal currentPlatformDiscount = order.getBatchDiscountAmount() == null
                        ? BigDecimal.ZERO
                        : order.getBatchDiscountAmount();
                order.setBatchDiscountAmount(currentPlatformDiscount.add(discountAmount));
                saveTransaction(TransactionType.PLATFORM_VOUCHER_SUBSIDY, discountAmount.negate(), null,
                        "ORDER_" + order.getId(),
                        "Admin/platform bù voucher sàn cho đơn #" + order.getId());
            }

            VoucherUsage usage = VoucherUsage.builder()
                    .voucher(voucher)
                    .user(currentUser)
                    .order(order)
                    .discountAmount(discountAmount)
                    .usedAt(LocalDateTime.now())
                    .build();

            lastUsageResponse = voucherUsageMapper.toResponse(voucherUsageRepository.save(usage));
        }
        return lastUsageResponse;
    }

    @Transactional
    @Override
    public BigDecimal applyBatchVoucher(CheckoutBatch batch, List<Order> orders, UUID batchVoucherUserId) {
        if (batch == null || batchVoucherUserId == null || orders == null || orders.isEmpty()) {
            return BigDecimal.ZERO;
        }

        User currentUser = getCurrentUser();
        VoucherUser voucherUser = voucherUserRepository.findByIdAndUser(batchVoucherUserId, currentUser)
                .orElseThrow(() -> new AppException(ErrorCode.VOUCHER_USER_NOT_FOUND));
        Voucher voucher = voucherUser.getVoucher();
        if (voucher == null) {
            throw new AppException(ErrorCode.VOUCHER_NOT_FOUND);
        }
        if (voucher.getVoucherType() != VoucherType.PLATFORM && voucher.getVoucherType() != VoucherType.WELCOME) {
            throw new AppException(ErrorCode.INVALID_REQUEST);
        }

        validateBatchVoucher(voucherUser, orders);

        BigDecimal batchSubtotal = orders.stream()
                .map(this::calculateRemainingProductSubtotal)
                .reduce(BigDecimal.ZERO, BigDecimal::add);
        BigDecimal batchShipping = orders.stream()
                .map(this::calculateRemainingShippingFee)
                .reduce(BigDecimal.ZERO, BigDecimal::add);

        BigDecimal totalDiscount;
        Map<Order, BigDecimal> allocations;
        if (voucher.getVoucherType() == VoucherType.WELCOME) {
            totalDiscount = calculateDiscountAmount(voucher, batchSubtotal, batchShipping);
            allocations = allocateProportional(orders, totalDiscount, this::calculateRemainingShippingFee);
        } else {
            totalDiscount = calculateDiscountAmount(voucher, batchSubtotal, BigDecimal.ZERO);
            allocations = allocateProportional(orders, totalDiscount, this::calculateRemainingProductSubtotal);
        }

        if (totalDiscount.compareTo(BigDecimal.ZERO) <= 0) {
            return BigDecimal.ZERO;
        }

        voucherUser.setQuantityUsed((voucherUser.getQuantityUsed() == null ? 0 : voucherUser.getQuantityUsed()) + 1);
        voucherUser.setQuantity(Math.max(0, (voucherUser.getQuantity() == null ? 0 : voucherUser.getQuantity()) - 1));
        voucherUserRepository.save(voucherUser);

        voucher.setUsedCount((voucher.getUsedCount() == null ? 0 : voucher.getUsedCount()) + 1);
        if (voucher.getUsageLimit() != null && voucher.getUsedCount() >= voucher.getUsageLimit()) {
            voucher.setActive(false);
        }
        voucherRespository.save(voucher);

        BigDecimal appliedTotal = BigDecimal.ZERO;
        for (Order order : orders) {
            BigDecimal alloc = allocations.getOrDefault(order, BigDecimal.ZERO);
            if (alloc.compareTo(BigDecimal.ZERO) <= 0) {
                continue;
            }
            BigDecimal orderTotal = order.getTotalAmount() == null ? BigDecimal.ZERO : order.getTotalAmount();
            BigDecimal actualAlloc = alloc.min(orderTotal);
            order.setTotalAmount(orderTotal.subtract(actualAlloc).max(BigDecimal.ZERO));
            order.setBatchDiscountAmount(actualAlloc);
            if (voucher.getVoucherType() == VoucherType.WELCOME) {
                BigDecimal currentShippingDiscount = order.getShippingDiscountAmount() == null
                        ? BigDecimal.ZERO
                        : order.getShippingDiscountAmount();
                order.setShippingDiscountAmount(currentShippingDiscount.add(actualAlloc));
            }
            appliedTotal = appliedTotal.add(actualAlloc);
        }

        if (voucher.getVoucherType() == VoucherType.WELCOME) {
            saveTransaction(TransactionType.SHIPPING_SUBSIDY, appliedTotal.negate(), null,
                    "BATCH_" + batch.getId(),
                    "Admin/platform bù phí vận chuyển bằng welcome voucher cho batch #" + batch.getId());
        } else {
            saveTransaction(TransactionType.PLATFORM_VOUCHER_SUBSIDY, appliedTotal.negate(), null,
                    "BATCH_" + batch.getId(),
                    "Admin/platform bù voucher sàn cho batch #" + batch.getId());
        }

        VoucherUsage usage = VoucherUsage.builder()
                .voucher(voucher)
                .user(currentUser)
                .order(orders.get(0))
                .discountAmount(appliedTotal)
                .usedAt(LocalDateTime.now())
                .build();
        voucherUsageRepository.save(usage);

        batch.setBatchVoucherUserId(batchVoucherUserId);
        batch.setBatchDiscountAmount(appliedTotal);
        return appliedTotal;
    }

    @Transactional
    @Override
    public void restoreBatchVoucher(CheckoutBatch batch, List<Order> orders) {
        if (batch == null || batch.getBatchVoucherUserId() == null || orders == null || orders.isEmpty()) {
            return;
        }

        VoucherType batchVoucherType = null;
        for (Order order : orders) {
            BigDecimal batchDiscount = order.getBatchDiscountAmount();
            if (batchDiscount == null || batchDiscount.compareTo(BigDecimal.ZERO) <= 0) {
                continue;
            }
            if (order.getTotalAmount() != null) {
                order.setTotalAmount(order.getTotalAmount().add(batchDiscount));
            }
            if (order.getShippingDiscountAmount() != null
                    && order.getShippingDiscountAmount().compareTo(batchDiscount) >= 0) {
                order.setShippingDiscountAmount(order.getShippingDiscountAmount().subtract(batchDiscount));
            }
            order.setBatchDiscountAmount(BigDecimal.ZERO);
        }

        for (Order order : orders) {
            List<VoucherUsage> usages = voucherUsageRepository.findAllByOrder_Id(order.getId());
            for (VoucherUsage usage : usages) {
                Voucher voucher = usage.getVoucher();
                if (voucher == null) {
                    continue;
                }
                if (voucher.getVoucherType() != VoucherType.PLATFORM
                        && voucher.getVoucherType() != VoucherType.WELCOME) {
                    continue;
                }
                batchVoucherType = voucher.getVoucherType();
                Optional<VoucherUser> voucherUserOptional = voucherUserRepository.findByUserAndVoucher(usage.getUser(), voucher);
                voucherUserOptional.ifPresent(vu -> {
                    int currentUsed = vu.getQuantityUsed() == null ? 0 : vu.getQuantityUsed();
                    int currentRemaining = vu.getQuantity() == null ? 0 : vu.getQuantity();
                    vu.setQuantityUsed(Math.max(0, currentUsed - 1));
                    vu.setQuantity(currentRemaining + 1);
                    voucherUserRepository.save(vu);
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
                voucherUsageRepository.delete(usage);
                break;
            }
            if (batchVoucherType != null) {
                break;
            }
        }

        batch.setBatchVoucherUserId(null);
        batch.setBatchDiscountAmount(null);
    }

    @Transactional
    @Override
    public void restoreVoucherForOrder(Order order) {
        if (order == null || order.getId() == null) {
            return;
        }

        List<VoucherUsage> usages = voucherUsageRepository.findAllByOrder_Id(order.getId());
        if (usages.isEmpty()) {
            return;
        }

        for (VoucherUsage usage : usages) {
        Voucher voucher = usage.getVoucher();
        if (voucher == null) {
            voucherUsageRepository.delete(usage);
            continue;
        }
        if (voucher.getVoucherType() == VoucherType.PLATFORM
                || voucher.getVoucherType() == VoucherType.WELCOME) {
            CheckoutBatch batch = order.getCheckoutBatch();
            if (batch != null && batch.getBatchVoucherUserId() != null) {
                continue;
            }
            if (voucher.getVoucherType() == VoucherType.WELCOME) {
                continue;
            }
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
            } else if (usage.getVoucher() != null && usage.getVoucher().getVoucherType() == VoucherType.PLATFORM) {
                BigDecimal currentPlatformDiscount = order.getBatchDiscountAmount() == null
                        ? BigDecimal.ZERO
                        : order.getBatchDiscountAmount();
                order.setBatchDiscountAmount(currentPlatformDiscount.subtract(discountAmount).max(BigDecimal.ZERO));
            }
        }

        voucherUsageRepository.delete(usage);
        }
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
        validateVoucherForOrder(voucherUser, order, false);
    }

    private void validateVoucherForOrder(VoucherUser voucherUser, Order order, boolean shopVouchersOnly) {
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
        if (shopVouchersOnly && isPlatformVoucher) {
            throw new AppException(ErrorCode.INVALID_REQUEST);
        }
        boolean isShopVoucherForOrder = voucher.getShopOwner() != null
                && order.getShopOwner() != null
                && voucher.getShopOwner().getId().equals(order.getShopOwner().getId());
        if (!isPlatformVoucher && !isShopVoucherForOrder) {
            throw new AppException(ErrorCode.INVALID_REQUEST);
        }

        BigDecimal subtotal = calculateRemainingProductSubtotal(order);
        if (voucher.getMinOrderValue() != null && subtotal.compareTo(voucher.getMinOrderValue()) < 0) {
            throw new AppException(ErrorCode.INVALID_REQUEST);
        }
    }

    private void validateBatchVoucher(VoucherUser voucherUser, List<Order> orders) {
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

        BigDecimal batchSubtotal = orders.stream()
                .map(this::calculateRemainingProductSubtotal)
                .reduce(BigDecimal.ZERO, BigDecimal::add);
        if (voucher.getMinOrderValue() != null && batchSubtotal.compareTo(voucher.getMinOrderValue()) < 0) {
            throw new AppException(ErrorCode.INVALID_REQUEST);
        }
    }

    private Map<Order, BigDecimal> allocateProportional(
            List<Order> orders,
            BigDecimal totalDiscount,
            Function<Order, BigDecimal> weightFn
    ) {
        Map<Order, BigDecimal> result = new java.util.LinkedHashMap<>();
        if (orders.isEmpty() || totalDiscount.compareTo(BigDecimal.ZERO) <= 0) {
            return result;
        }

        BigDecimal totalWeight = orders.stream()
                .map(weightFn)
                .reduce(BigDecimal.ZERO, BigDecimal::add);
        if (totalWeight.compareTo(BigDecimal.ZERO) <= 0) {
            return result;
        }

        BigDecimal allocated = BigDecimal.ZERO;
        for (int i = 0; i < orders.size(); i++) {
            Order order = orders.get(i);
            BigDecimal weight = weightFn.apply(order);
            BigDecimal share;
            if (i == orders.size() - 1) {
                share = totalDiscount.subtract(allocated);
            } else {
                share = totalDiscount.multiply(weight)
                        .divide(totalWeight, 0, RoundingMode.DOWN);
            }
            if (share.compareTo(BigDecimal.ZERO) > 0) {
                allocated = allocated.add(share);
                result.put(order, share);
            }
        }
        return result;
    }

    private List<String> normalizeVoucherIdentifiers(List<String> voucherIdentifiers) {
        LinkedHashSet<String> normalized = new LinkedHashSet<>();
        for (String rawIdentifier : voucherIdentifiers) {
            if (rawIdentifier != null && !rawIdentifier.isBlank()) {
                normalized.add(rawIdentifier.trim());
            }
        }
        return new ArrayList<>(normalized);
    }

    private BigDecimal calculateRemainingProductSubtotal(Order order) {
        BigDecimal totalAmount = order.getTotalAmount() == null ? BigDecimal.ZERO : order.getTotalAmount();
        BigDecimal remainingShippingFee = calculateRemainingShippingFee(order);
        return totalAmount.subtract(remainingShippingFee).max(BigDecimal.ZERO);
    }

    private BigDecimal calculateRemainingShippingFee(Order order) {
        BigDecimal shippingFee = order.getShippingFee() == null ? BigDecimal.ZERO : order.getShippingFee();
        BigDecimal shippingDiscountAmount = order.getShippingDiscountAmount() == null
                ? BigDecimal.ZERO
                : order.getShippingDiscountAmount();
        return shippingFee.subtract(shippingDiscountAmount).max(BigDecimal.ZERO);
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
