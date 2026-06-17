package com.swd301.foodmarket.mapper;

import com.swd301.foodmarket.dto.request.ProductCreationRequest;
import com.swd301.foodmarket.dto.request.ProductUpdateRequest;
import com.swd301.foodmarket.dto.response.ProductResponse;
import com.swd301.foodmarket.entity.Product;
import org.mapstruct.*;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDateTime;

@Mapper(componentModel = "spring")
public interface ProductMapper {

    @Mapping(target = "images", ignore = true)
    Product toProduct(ProductCreationRequest request);

    @Mapping(source = "shopOwner.shopName", target = "shopName")
    @Mapping(source = "shopOwner.id", target = "shopId")
    @Mapping(source = "category.id", target = "categoryId")
    @Mapping(target = "sellingPrice", source = "sellingPrice")
    @Mapping(source = "status", target = "status")
    @Mapping(source = "expiryDate", target = "expiryDate")
    @Mapping(target = "categoryName", source = "category.name")
    @Mapping(target = "images", ignore = true)
    @Mapping(target = "salePrice", ignore = true)
    ProductResponse toResponse(Product product);

    @AfterMapping
    default void computeSalePrice(Product product, @MappingTarget ProductResponse response) {
        Integer pct = product.getDiscountPercent();
        BigDecimal price = product.getSellingPrice();
        if (pct != null && pct > 0 && pct < 100 && price != null) {
            LocalDateTime now = LocalDateTime.now();
            LocalDateTime start = product.getDiscountStartDate();
            LocalDateTime end = product.getDiscountEndDate();
            boolean active = (start == null || !now.isBefore(start)) && (end == null || !now.isAfter(end));
            if (active) {
                BigDecimal multiplier = BigDecimal.valueOf(100 - pct).divide(BigDecimal.valueOf(100));
                response.setSalePrice(price.multiply(multiplier).setScale(0, RoundingMode.HALF_UP));
            }
        }
    }

    @BeanMapping(nullValuePropertyMappingStrategy = NullValuePropertyMappingStrategy.IGNORE)
    @Mapping(target = "id", ignore = true)
    @Mapping(target = "category", ignore = true)
    @Mapping(target = "shopOwner", ignore = true)
    @Mapping(target = "images", ignore = true)
    void update(@MappingTarget Product product, ProductUpdateRequest request);
}
