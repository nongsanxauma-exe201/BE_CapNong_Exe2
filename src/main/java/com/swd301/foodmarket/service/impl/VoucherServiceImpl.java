package com.swd301.foodmarket.service.impl;

import com.swd301.foodmarket.dto.request.VoucherCreateRequest;
import com.swd301.foodmarket.dto.request.VoucherUpdateRequest;
import com.swd301.foodmarket.dto.response.VoucherResponse;
import com.swd301.foodmarket.entity.Notification;
import com.swd301.foodmarket.entity.Order;
import com.swd301.foodmarket.entity.User;
import com.swd301.foodmarket.entity.Voucher;
import com.swd301.foodmarket.entity.VoucherUser;
import com.swd301.foodmarket.enums.RoleName;
import com.swd301.foodmarket.enums.VoucherType;
import com.swd301.foodmarket.exception.AppException;
import com.swd301.foodmarket.exception.ErrorCode;
import com.swd301.foodmarket.mapper.VoucherMapper;
import com.swd301.foodmarket.repository.UserRepository;
import com.swd301.foodmarket.repository.VoucherRespository;
import com.swd301.foodmarket.repository.VoucherUserRepository;
import com.swd301.foodmarket.service.NotificationService;
import com.swd301.foodmarket.service.VoucherService;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.time.LocalDateTime;

@Service
@RequiredArgsConstructor
public class VoucherServiceImpl implements VoucherService {

    private final VoucherRespository voucherRespository;
    private final VoucherMapper voucherMapper;
    private final UserRepository userRepository;
    private final VoucherUserRepository voucherUserRepository;
    private final NotificationService notificationService;

    @Override
    public VoucherResponse getVoucherByCode(String code) {
        Voucher voucher = voucherRespository.findByVoucherCode(code);

        if(voucher == null) {
            throw new AppException(ErrorCode.VOUCHER_NOT_FOUND);
        }
        return voucherMapper.toVoucherResponse(voucher);
    }

    @Override
    public VoucherResponse createVoucher(VoucherCreateRequest voucherCreateRequest) {
        User user =  getCurrentUser();
        if (user.getRole().getName() != RoleName.SHOP_OWNER) {
            throw new AppException(ErrorCode.UNAUTHORIZED);
        }
        Voucher voucher = voucherMapper.toVoucher(voucherCreateRequest);
        voucher.setCreateAt(LocalDateTime.now());
        voucher.setClaimedCount(0);
        voucher.setUsedCount(0);
        voucher.setShopOwner(user);
        voucher.setVoucherType(VoucherType.SHOP);
        voucher.setActive(true);
        voucher.setVoucherCode(generateUniqueVoucherCode());
        voucherRespository.save(voucher);
        return voucherMapper.toVoucherResponse(voucher);
    }

    @Override
    public VoucherResponse createPlatformVoucher(VoucherCreateRequest voucherCreateRequest) {
        User user = getCurrentUser();
        if (user.getRole().getName() != RoleName.ADMIN) {
            throw new AppException(ErrorCode.UNAUTHORIZED);
        }
        Voucher voucher = voucherMapper.toVoucher(voucherCreateRequest);
        voucher.setCreateAt(LocalDateTime.now());
        voucher.setClaimedCount(0);
        voucher.setUsedCount(0);
        voucher.setShopOwner(null);
        voucher.setVoucherType(VoucherType.PLATFORM);
        voucher.setActive(true);
        voucher.setVoucherCode(generateUniqueVoucherCode());
        return voucherMapper.toVoucherResponse(voucherRespository.save(voucher));
    }

    @Override
    public VoucherResponse createAutoVoucherForDamage(Order order, String reason) {
        if (order == null || order.getId() == null || order.getShopOwner() == null || isBlank(reason)) {
            throw new AppException(ErrorCode.INVALID_REQUEST);
        }

        return voucherMapper.toVoucherResponse(
                voucherRespository.findByVoucherTypeAndAutoCreatedFromOrderId(VoucherType.DAMAGED_ORDER, order.getId())
                        .orElseGet(() -> {
                            Voucher voucher = Voucher.builder()
                                    .shopOwner(order.getShopOwner())
                                    .voucherType(VoucherType.DAMAGED_ORDER)
                                    .voucherCode(generateUniqueVoucherCode("HU-DON-"))
                                    .discountValue(new BigDecimal("20"))
                                    .maxDiscount(new BigDecimal("50000"))
                                    .minOrderValue(BigDecimal.ZERO)
                                    .claimLimit(1)
                                    .claimedCount(0)
                                    .usageLimit(1)
                                    .usedCount(0)
                                    .perUserLimit(1)
                                    .active(false)
                                    .autoCreatedReason("DAMAGED_ORDER")
                                    .autoCreatedFromOrderId(order.getId())
                                    .createAt(LocalDateTime.now())
                                    .build();
                            Voucher saved = voucherRespository.save(voucher);
                            notifyUser(order.getShopOwner(),
                                    "Đã tạo voucher xin lỗi cho đơn #" + order.getId(),
                                    reason
                                            + ". Hệ thống đã tạo voucher giảm 20% tối đa 50.000đ và sẽ gửi cho khách khi đơn giao thành công.");
                            return saved;
                        })
        );
    }

    @Override
    public void grantDamageVoucherForDeliveredOrder(Order order) {
        if (order == null || order.getId() == null || order.getBuyer() == null) {
            return;
        }

        voucherRespository.findByVoucherTypeAndAutoCreatedFromOrderId(VoucherType.DAMAGED_ORDER, order.getId())
                .ifPresent(voucher -> {
                    if (voucherUserRepository.existsByUserAndVoucher(order.getBuyer(), voucher)) {
                        return;
                    }
                    voucher.setActive(true);
                    voucher.setExpiryDate(LocalDateTime.now().plusDays(15));
                    voucher.setClaimedCount((voucher.getClaimedCount() == null ? 0 : voucher.getClaimedCount()) + 1);
                    voucherRespository.save(voucher);

                    voucherUserRepository.save(VoucherUser.builder()
                            .user(order.getBuyer())
                            .voucher(voucher)
                            .quantity(1)
                            .quantityUsed(0)
                            .claimAt(LocalDateTime.now())
                            .build());

                    String shopName = order.getShopOwner() != null && order.getShopOwner().getShopName() != null
                            ? order.getShopOwner().getShopName()
                            : "shop";
                    notifyUser(order.getBuyer(),
                            "Bạn vừa nhận voucher xin lỗi từ " + shopName,
                            "Do đơn #" + order.getId() + " có khối lượng không đạt chuẩn trên 20%, bạn được tặng voucher giảm 20% tối đa 50.000đ. Voucher có hiệu lực 15 ngày.");
                });
    }

    @Override
    public void createWelcomeVoucherForUser(User user) {
        if (user == null || user.getId() == null || user.getRole() == null
                || user.getRole().getName() != RoleName.BUYER) {
            return;
        }

        Voucher voucher = Voucher.builder()
                .shopOwner(null)
                .voucherType(VoucherType.WELCOME)
                .voucherCode(generateUniqueVoucherCode("WELCOME-"))
                .discountValue(new BigDecimal("100"))
                .maxDiscount(null)
                .minOrderValue(BigDecimal.ZERO)
                .claimLimit(1)
                .claimedCount(1)
                .usageLimit(1)
                .usedCount(0)
                .perUserLimit(1)
                .active(true)
                .autoCreatedReason("WELCOME")
                .autoCreatedFromOrderId(null)
                .expiryDate(LocalDateTime.now().plusDays(30))
                .createAt(LocalDateTime.now())
                .build();
        Voucher saved = voucherRespository.save(voucher);

        voucherUserRepository.save(VoucherUser.builder()
                .user(user)
                .voucher(saved)
                .quantity(1)
                .quantityUsed(0)
                .claimAt(LocalDateTime.now())
                .build());

        notifyUser(user,
                "Chào mừng bạn đến với Nông Sản Xấu Mã",
                "Bạn đã nhận voucher ship 0đ mã " + saved.getVoucherCode() + ", hiệu lực 30 ngày.");
    }

    @Override
    public VoucherResponse updateVoucher(String code, VoucherUpdateRequest voucherUpdateRequest) {
            Voucher voucher = voucherRespository.findByVoucherCode(code);
            if (voucher == null) {
                throw new AppException(ErrorCode.VOUCHER_NOT_FOUND);
            }
            assertCanManageVoucher(voucher);
            voucherMapper.updateVoucherFromRequest(voucherUpdateRequest, voucher);
        return voucherMapper.toVoucherResponse(voucherRespository.save(voucher));
    }

    @Override
    public void deleteVoucher(String code) {
        Voucher voucher = voucherRespository.findByVoucherCode(code);

        if(voucher == null) {
            throw new AppException(ErrorCode.VOUCHER_NOT_FOUND);
        }
        assertCanManageVoucher(voucher);
        voucher.setActive(false);
        voucherRespository.save(voucher);
    }

    @Override
    public Page<VoucherResponse> getMyVouchers(int page, int size, boolean active) {

        Pageable pageable = PageRequest.of(page, size, Sort.by("createAt").descending());
        Page<Voucher> voucherPage = voucherRespository.findByShopOwnerAndActive(getCurrentUser(), active, pageable);
        return voucherPage.map(voucherMapper::toVoucherResponse);
    }

    @Override
    public Page<VoucherResponse> getPlatformVouchers(int page, int size, boolean active) {
        User user = getCurrentUser();
        if (user.getRole().getName() != RoleName.ADMIN) {
            throw new AppException(ErrorCode.UNAUTHORIZED);
        }
        Pageable pageable = PageRequest.of(page, size, Sort.by("createAt").descending());
        return voucherRespository.findByVoucherTypeAndActive(VoucherType.PLATFORM, active, pageable)
                .map(voucherMapper::toVoucherResponse);
    }

    @Override
    public VoucherResponse activateVoucher(String code) {
        Voucher voucher = voucherRespository.findByVoucherCode(code);

        if(voucher == null) {
            throw new AppException(ErrorCode.VOUCHER_NOT_FOUND);
        }
        assertCanManageVoucher(voucher);
        voucher.setActive(true);
        voucherRespository.save(voucher);
        return voucherMapper.toVoucherResponse(voucher);
    }

    @Override
    public Page<VoucherResponse> getVoucherForBuyer(int page, int size, Integer shopId) {

        Pageable pageable = PageRequest.of(page, size, Sort.by("createAt").descending());
        User user = userRepository.findById(shopId).orElseThrow(
                () -> new AppException(ErrorCode.USER_NOT_EXISTED)
        );
        if(user.getRole().getName() != RoleName.SHOP_OWNER)
        {
            throw new AppException(ErrorCode.USER_NOT_EXISTED);
        }
        Page<Voucher> voucherPage = voucherRespository.getVoucherForBuyer(user, LocalDateTime.now(), pageable);
        return voucherPage.map(voucherMapper::toVoucherResponse);
    }

    private User getCurrentUser() {
        String name = SecurityContextHolder.getContext().getAuthentication().getName();
        User user = userRepository.findByEmail(name).orElseThrow(
                () -> new AppException(ErrorCode.USER_NOT_EXISTED)
        );
        return user;
    }

    private void assertCanManageVoucher(Voucher voucher) {
        User user = getCurrentUser();
        if (voucher.getVoucherType() == VoucherType.PLATFORM || voucher.getVoucherType() == VoucherType.WELCOME) {
            if (user.getRole().getName() != RoleName.ADMIN) {
                throw new AppException(ErrorCode.UNAUTHORIZED);
            }
            return;
        }

        if (voucher.getShopOwner() == null
                || !voucher.getShopOwner().getId().equals(user.getId())) {
            throw new AppException(ErrorCode.UNAUTHORIZED);
        }
    }

    private String generateUniqueVoucherCode() {
        return generateUniqueVoucherCode("VOUCHER-");
    }

    private String generateUniqueVoucherCode(String prefix) {
        String code;
        do {
            code = prefix + java.util.UUID.randomUUID().toString().substring(0, 8).toUpperCase();
        } while (voucherRespository.findByVoucherCode(code) != null);
        return code;
    }

    private boolean isBlank(String value) {
        return value == null || value.trim().isEmpty();
    }

    private void notifyUser(User user, String title, String message) {
        notificationService.createUserNotification(user, Notification.builder()
                .title(title)
                .message(message)
                .receiverType("USER_" + user.getId())
                .build());
    }
}
