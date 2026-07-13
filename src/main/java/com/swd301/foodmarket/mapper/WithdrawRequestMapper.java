package com.swd301.foodmarket.mapper;

import com.swd301.foodmarket.dto.response.WithdrawRequestResponse;
import com.swd301.foodmarket.entity.WithdrawRequest;
import com.swd301.foodmarket.entity.Wallet;
import org.mapstruct.AfterMapping;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;
import org.mapstruct.MappingTarget;

import java.util.List;

@Mapper(componentModel = "spring", uses = {ImageMapper.class})
public interface WithdrawRequestMapper {

    @Mapping(target = "walletId", source = "wallet.id")
    @Mapping(target = "shopOwnerId", ignore = true)
    @Mapping(target = "shipperId", ignore = true)
    @Mapping(target = "buyerId", ignore = true)
    @Mapping(target = "walletType", ignore = true)
    WithdrawRequestResponse toResponse(WithdrawRequest wr);

    List<WithdrawRequestResponse> toResponses(List<WithdrawRequest> list);

    @AfterMapping
    default void fillWalletOwnerFields(WithdrawRequest wr, @MappingTarget WithdrawRequestResponse res) {
        Wallet wallet = wr != null ? wr.getWallet() : null;
        if (wallet == null) {
            return;
        }
        if (wallet.getId() != null) {
            res.setWalletId(wallet.getId());
        }
        if (wallet.getType() != null) {
            res.setWalletType(wallet.getType().name());
        }
        if (wallet.getShopOwner() != null) {
            res.setShopOwnerId(wallet.getShopOwner().getId());
        }
        if (wallet.getShipper() != null) {
            res.setShipperId(wallet.getShipper().getId());
        }
        if (wallet.getBuyer() != null) {
            res.setBuyerId(wallet.getBuyer().getId());
        }
    }
}
