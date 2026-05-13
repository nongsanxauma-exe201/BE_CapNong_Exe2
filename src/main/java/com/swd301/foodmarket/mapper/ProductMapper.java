package com.swd301.foodmarket.mapper;

import com.swd301.foodmarket.dto.request.ProductCreationRequest;
import com.swd301.foodmarket.dto.request.ProductUpdateRequest;
import com.swd301.foodmarket.dto.response.ProductResponse;
import com.swd301.foodmarket.entity.Product;
import org.mapstruct.*;

@Mapper(componentModel = "spring")
public interface ProductMapper {
    Product toProduct(ProductCreationRequest request);
    @Mapping(source = "shopOwner.shopName", target = "shopName")
    @Mapping(source = "shopOwner.id", target = "shopId")
    @Mapping(source = "category.id", target = "categoryId")
    @Mapping(target = "sellingPrice", source = "sellingPrice")
    @Mapping(source = "status", target = "status")
    @Mapping(source = "expiryDate", target = "expiryDate")
    ProductResponse toResponse(Product product);

    @BeanMapping(nullValuePropertyMappingStrategy = NullValuePropertyMappingStrategy.IGNORE)
    @Mapping(target = "id", ignore = true)
    @Mapping(target = "category", ignore = true)
    @Mapping(target = "shopOwner", ignore = true)
    void update(@MappingTarget Product product, ProductUpdateRequest request);
}
