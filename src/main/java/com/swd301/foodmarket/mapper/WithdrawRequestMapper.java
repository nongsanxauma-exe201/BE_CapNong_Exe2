package com.swd301.foodmarket.mapper;

import com.swd301.foodmarket.dto.response.WithdrawRequestResponse;
import com.swd301.foodmarket.entity.WithdrawRequest;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;

import java.util.List;

@Mapper(componentModel = "spring")
public interface WithdrawRequestMapper {

    @Mapping(target = "walletId", source = "wallet.id")
    @Mapping(target = "shopOwnerId", source = "wallet.shopOwner.id")
    @Mapping(target = "shipperId", source = "wallet.shipper.id")
    WithdrawRequestResponse toResponse(WithdrawRequest wr);

    List<WithdrawRequestResponse> toResponses(List<WithdrawRequest> list);
}