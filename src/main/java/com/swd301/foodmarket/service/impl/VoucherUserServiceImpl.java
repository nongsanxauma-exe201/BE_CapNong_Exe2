package com.swd301.foodmarket.service.impl;

import com.swd301.foodmarket.dto.request.GetVoucherForOrder;
import com.swd301.foodmarket.dto.response.VoucherUsageResponse;
import com.swd301.foodmarket.dto.response.VoucherUserResponse;
import com.swd301.foodmarket.entity.*;
import com.swd301.foodmarket.exception.AppException;
import com.swd301.foodmarket.exception.ErrorCode;
import com.swd301.foodmarket.mapper.VoucherUserMapper;
import com.swd301.foodmarket.repository.UserRepository;
import com.swd301.foodmarket.repository.VoucherRespository;
import com.swd301.foodmarket.repository.VoucherUserRepository;
import jakarta.transaction.Transactional;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

@RequiredArgsConstructor
@Service
public class VoucherUserServiceImpl  implements  com.swd301.foodmarket.service.VoucherUserService {

    private final VoucherUserRepository voucherUserRepository;
    private final VoucherRespository voucherRespository;
    private final VoucherUserMapper voucherUserMapper;
    private final UserRepository userRepository;
    @Transactional
    @Override
    public VoucherUserResponse createVoucherUser(String voucherCode) {
        Voucher voucher = voucherRespository.findByVoucherCode(voucherCode);
        if (voucher == null) {
            throw  new AppException(ErrorCode.VOUCHER_NOT_FOUND);
        }
       if(voucher.getClaimedCount() + 1 > voucher.getClaimLimit()) {
           throw new AppException(ErrorCode.VOUCHER_RECEIVE_LIMIT_REACHED);
       }
       if(voucherUserRepository.existsByUserAndVoucher(getCurrentUser(), voucher)) {
           throw new AppException(ErrorCode.VOUCHER_ALREADY_CLAIMED);
       }
       if(voucher.getExpiryDate().isBefore(LocalDateTime.now())) {
           throw new AppException(ErrorCode.VOUCHER_EXPIRED);
       }
       if(!voucher.isActive()){
           throw  new AppException(ErrorCode.VOUCHER_INACTIVE);
       }
       VoucherUser voucherUser = VoucherUser.builder()
               .user(getCurrentUser())
               .voucher(voucher)
               .quantity(voucher.getPerUserLimit())
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
        if(voucher.getClaimedCount() + 1 > voucher.getClaimLimit()) {
           return  false;
        }
        if(voucherUserRepository.existsByUserAndVoucher(getCurrentUser(), voucher)) {
            return  false;
        }
        if(voucher.getExpiryDate().isBefore(LocalDateTime.now())) {
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
          User user = getCurrentUser();
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
    public VoucherUsageResponse useVoucherForOrder(Order order, List<String> voucherCode) {

        return null;
    }

    private User getCurrentUser() {
        String name = SecurityContextHolder.getContext().getAuthentication().getName();
        User user = userRepository.findByEmail(name).orElseThrow(
                () -> new AppException(ErrorCode.USER_NOT_EXISTED)
        );
        return user;
    }
}
