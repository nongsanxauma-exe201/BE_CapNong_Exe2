package com.swd301.foodmarket.mapper;

import com.swd301.foodmarket.dto.request.VoucherCreateRequest;
import com.swd301.foodmarket.dto.request.VoucherUpdateRequest;
import com.swd301.foodmarket.dto.response.VoucherResponse;
import com.swd301.foodmarket.dto.response.VoucherUserResponse;
import com.swd301.foodmarket.entity.Voucher;
import com.swd301.foodmarket.entity.VoucherUser;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;
import org.mapstruct.MappingTarget;

@Mapper(componentModel = "spring")
public interface VoucherMapper {

    @Mapping(target = "voucherId", source = "id")
    @Mapping(target = "shopId", source = "shopOwner.id")
    @Mapping(target = "shopName", source = "shopOwner.shopName")
    VoucherResponse toVoucherResponse(com.swd301.foodmarket.entity.Voucher voucher);
    Voucher toVoucher(VoucherCreateRequest VoucherCreateRequest);
    void updateVoucherFromRequest(
            VoucherUpdateRequest request,
            @MappingTarget Voucher voucher
    );
    VoucherUserResponse.VoucheResponserUser toVoucherUserResponse(Voucher voucher);
}
