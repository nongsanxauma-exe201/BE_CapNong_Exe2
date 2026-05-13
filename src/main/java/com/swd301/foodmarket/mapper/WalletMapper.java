package com.swd301.foodmarket.mapper;

import com.swd301.foodmarket.dto.request.WalletCreationRequest;
import com.swd301.foodmarket.dto.response.WalletResponse;
import com.swd301.foodmarket.entity.Wallet;
import org.mapstruct.*;

@Mapper(componentModel = "spring")
public interface WalletMapper {

    Wallet toEntity(WalletCreationRequest request);

    @Mapping(target = "shopOwnerId", source = "shopOwner.id")
    @Mapping(target = "shipperId", source = "shipper.id")
    WalletResponse toResponse(Wallet wallet);

    @BeanMapping(nullValuePropertyMappingStrategy = NullValuePropertyMappingStrategy.IGNORE)
    void updateWallet(@MappingTarget Wallet wallet, WalletCreationRequest request);
}