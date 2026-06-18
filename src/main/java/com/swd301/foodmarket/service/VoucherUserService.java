package com.swd301.foodmarket.service;

import com.swd301.foodmarket.dto.request.GetVoucherForOrder;
import com.swd301.foodmarket.dto.response.VoucherUsageResponse;
import com.swd301.foodmarket.dto.response.VoucherUserResponse;
import com.swd301.foodmarket.entity.Order;
import org.springframework.data.domain.Page;

import java.util.List;
import java.util.UUID;

public interface VoucherUserService {


    VoucherUserResponse createVoucherUser(String voucherCode);
     Page<VoucherUserResponse> getMyVouchers(int page, int size);
    VoucherUserResponse getVoucherUserById(UUID id);
    boolean canRecieveVoucher(String voucherCode);
    Page<VoucherUserResponse> getVoucherForOrder(GetVoucherForOrder request, int page, int size);
    VoucherUsageResponse useVoucherForOrder(Order order, List<String> voucherCode);
    void restoreVoucherForOrder(Order order);
}
