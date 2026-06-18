package com.swd301.foodmarket.service.impl;

import com.swd301.foodmarket.dto.request.VoucherCreateRequest;
import com.swd301.foodmarket.dto.request.VoucherUpdateRequest;
import com.swd301.foodmarket.dto.response.VoucherResponse;
import com.swd301.foodmarket.entity.User;
import com.swd301.foodmarket.entity.Voucher;
import com.swd301.foodmarket.enums.RoleName;
import com.swd301.foodmarket.exception.AppException;
import com.swd301.foodmarket.exception.ErrorCode;
import com.swd301.foodmarket.mapper.VoucherMapper;
import com.swd301.foodmarket.repository.UserRepository;
import com.swd301.foodmarket.repository.VoucherRespository;
import com.swd301.foodmarket.service.VoucherService;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;

@Service
@RequiredArgsConstructor
public class VoucherServiceImpl implements VoucherService {

    private final VoucherRespository voucherRespository;
    private final VoucherMapper voucherMapper;
    private final UserRepository userRepository;

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
        Voucher voucher = voucherMapper.toVoucher(voucherCreateRequest);
        voucher.setCreateAt(LocalDateTime.now());
        voucher.setClaimedCount(0);
        voucher.setUsedCount(0);
        voucher.setShopOwner(user);
        voucher.setActive(true);
        voucher.setVoucherCode(generateUniqueVoucherCode());
        voucherRespository.save(voucher);
        return voucherMapper.toVoucherResponse(voucher);
    }

    @Override
    public VoucherResponse updateVoucher(String code, VoucherUpdateRequest voucherUpdateRequest) {
            Voucher voucher = voucherRespository.findByVoucherCode(code);
            voucherMapper.updateVoucherFromRequest(voucherUpdateRequest, voucher);
        return voucherMapper.toVoucherResponse(voucherRespository.save(voucher));
    }

    @Override
    public void deleteVoucher(String code) {
        Voucher voucher = voucherRespository.findByVoucherCode(code);

        if(voucher == null) {
            throw new AppException(ErrorCode.VOUCHER_NOT_FOUND);
        }
        voucher.setActive(false);
        voucherRespository.save(voucher);
    }

    @Override
    public Page<VoucherResponse> getMyVouchers(int page, int size, boolean active) {

        Pageable pageable = PageRequest.of(page, size, Sort.by("createAt").descending());
        User user = getCurrentUser();
        Page<Voucher> voucherPage = voucherRespository.findByShopOwnerAndActive(getCurrentUser(), active, pageable);
        return voucherPage.map(voucherMapper::toVoucherResponse);
    }

    @Override
    public VoucherResponse activateVoucher(String code) {
        Voucher voucher = voucherRespository.findByVoucherCode(code);

        if(voucher == null) {
            throw new AppException(ErrorCode.VOUCHER_NOT_FOUND);
        }
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
    private String generateUniqueVoucherCode() {
        String code;
        do {
            code = "VOUCHER-" + java.util.UUID.randomUUID().toString().substring(0, 8).toUpperCase();
        } while (voucherRespository.findByVoucherCode(code) != null);
        return code;
    }
}
