package com.swd301.foodmarket.mapper;

import com.swd301.foodmarket.dto.response.VoucherUsageResponse;
import com.swd301.foodmarket.entity.VoucherUsage;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.UUID;
@Mapper(componentModel = "spring")
public interface VoucherUsageMapper  {
    @Mapping(target = "voucherCode", source = "voucher.voucherCode")
    @Mapping(target = "voucherId",source = "voucher.id")
    @Mapping(target = "discountValue",source = "voucher.discountValue")
    @Mapping(target = "maxDiscount",source ="voucher.maxDiscount")
    VoucherUsageResponse toResponse(VoucherUsage voucherUsage);
}
