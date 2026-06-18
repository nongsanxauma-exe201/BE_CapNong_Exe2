package com.swd301.foodmarket.mapper;

import com.swd301.foodmarket.dto.response.VoucherUserResponse;
import com.swd301.foodmarket.entity.VoucherUser;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;

@Mapper(componentModel ="spring",uses = {VoucherMapper.class})
public interface VoucherUserMapper {
    @Mapping(target = "voucherUserId", source = "id")
    @Mapping(target = "voucherResponse",source = "voucher")
    VoucherUserResponse toVoucherUserResponse(VoucherUser voucherUser);
}
