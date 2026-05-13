package com.swd301.foodmarket.mapper;


import com.swd301.foodmarket.dto.request.BuildComboCreationRequest;
import com.swd301.foodmarket.dto.request.BuildComboUpdateRequest;
import com.swd301.foodmarket.dto.response.BuildComboResponse;
import com.swd301.foodmarket.dto.response.ProductComboResponse;
import com.swd301.foodmarket.dto.response.ProductResponse;
import com.swd301.foodmarket.entity.BuildCombo;
import com.swd301.foodmarket.entity.Product;
import com.swd301.foodmarket.entity.ProductCombo;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;
import org.mapstruct.MappingTarget;

import java.util.List;

@Mapper(componentModel = "spring")
public interface BuildComboMapper {
    BuildCombo toEntity(BuildComboCreationRequest request);

    @Mapping(target = "shopOwnerId", source = "shopOwner.id")
    @Mapping(target = "items", source = "items")
    BuildComboResponse toResponse(BuildCombo combo);

    List<ProductComboResponse> toItemResponses(List<ProductCombo> items);

    @Mapping(target = "productId", source = "product.id")
    @Mapping(target = "productName", source = "product.productName")
    @Mapping(target = "price", source = "product.sellingPrice")
    @Mapping(target = "quantity", source = "quantity")
    ProductComboResponse toItemResponse(ProductCombo item);

    @Mapping(target = "id", ignore = true)
    @Mapping(target = "shopOwner", ignore = true)
    @Mapping(target = "items", ignore = true)
    @Mapping(target = "imageUrl", ignore = true)
    void update(@MappingTarget BuildCombo combo, BuildComboUpdateRequest request);
}
