package com.swd301.foodmarket.service;

import com.swd301.foodmarket.dto.request.VoucherCreateRequest;
import com.swd301.foodmarket.dto.request.VoucherUpdateRequest;
import com.swd301.foodmarket.dto.response.VoucherResponse;
import com.swd301.foodmarket.entity.Order;
import com.swd301.foodmarket.entity.User;
import org.springframework.data.domain.Page;

public interface VoucherService {

    VoucherResponse getVoucherByCode(String code);
    VoucherResponse createVoucher(VoucherCreateRequest voucherCreateRequest);
    VoucherResponse createPlatformVoucher(VoucherCreateRequest voucherCreateRequest);
    VoucherResponse createAutoVoucherForDamage(Order order, String damageNote);
    void grantDamageVoucherForDeliveredOrder(Order order);
    void createWelcomeVoucherForUser(User user);
     VoucherResponse updateVoucher(String code, VoucherUpdateRequest updateRequest);
     void deleteVoucher(String code);
     Page<VoucherResponse> getMyVouchers(int page, int size, boolean active);
     Page<VoucherResponse> getPlatformVouchers(int page, int size, boolean active);
     VoucherResponse activateVoucher(String code);
    Page<VoucherResponse> getVoucherForBuyer(int page, int size, Integer shopId);


}
