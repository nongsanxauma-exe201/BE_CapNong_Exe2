package com.swd301.foodmarket.mapper;

import com.swd301.foodmarket.dto.request.MysteryBoxCreationRequest;
import com.swd301.foodmarket.dto.request.MysteryBoxUpdateRequest;
import com.swd301.foodmarket.dto.response.MysteryBoxResponse;
import com.swd301.foodmarket.dto.response.ProductMysteryResponse;
import com.swd301.foodmarket.entity.MysteryBox;
import com.swd301.foodmarket.entity.ProductMystery;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;
import org.mapstruct.MappingTarget;

import java.util.List;


@Mapper(componentModel = "spring")
public interface MysteryBoxMapper {

    MysteryBox toEntity(MysteryBoxCreationRequest request);

    @Mapping(target = "shopOwnerId", source = "shopOwner.id")
    @Mapping(target = "products", source = "productMysteries")
    @Mapping(target = "isActive", source = "isActive")
    MysteryBoxResponse toResponse(MysteryBox box);

    @Mapping(target = "id", ignore = true)
    @Mapping(target = "shopOwner", ignore = true)
    @Mapping(target = "isActive", source = "active")
    void update(@MappingTarget MysteryBox box, MysteryBoxUpdateRequest request);

    @Mapping(target = "productId", source = "product.id")
    @Mapping(target = "productName", source = "product.productName")
    @Mapping(target = "sellingPrice", source = "product.sellingPrice")
    ProductMysteryResponse toProductMysteryResponse(ProductMystery pm);


    List<ProductMysteryResponse> toProductMysteryResponseList(List<ProductMystery> pmList);
}