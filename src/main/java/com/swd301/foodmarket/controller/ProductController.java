package com.swd301.foodmarket.controller;


import com.fasterxml.jackson.databind.ObjectMapper;
import com.swd301.foodmarket.dto.request.ProductCreationRequest;
import com.swd301.foodmarket.dto.request.ProductUpdateRequest;
import com.swd301.foodmarket.dto.response.ApiResponse;
import com.swd301.foodmarket.dto.response.PageResponse;
import com.swd301.foodmarket.dto.response.ProductResponse;
import com.swd301.foodmarket.entity.Product;
import com.swd301.foodmarket.service.ProductService;
import jakarta.validation.Valid;
import lombok.AccessLevel;
import lombok.RequiredArgsConstructor;
import lombok.experimental.FieldDefaults;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.MediaType;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.util.List;

@RestController
@RequestMapping("/products")
@RequiredArgsConstructor
@FieldDefaults(level = AccessLevel.PRIVATE, makeFinal = true)
@Slf4j
public class ProductController {
    ProductService productService;


    @GetMapping
    public ApiResponse<List<ProductResponse>> getAll() {
        return ApiResponse.<List<ProductResponse>>builder()
                .result(productService.getAll())
                .build();
    }

    @GetMapping("/paged")
    public ApiResponse<PageResponse<ProductResponse>> getAllPaged(
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "10") int size
    ) {
        return ApiResponse.<PageResponse<ProductResponse>>builder()
                .result(productService.getAllPaged(page, size))
                .build();
    }


    @GetMapping("/shop/{shopId}/paged")
    public ApiResponse<PageResponse<ProductResponse>> getByShopIdPaged(
            @PathVariable Integer shopId,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "10") int size
    ) {
        return ApiResponse.<PageResponse<ProductResponse>>builder()
                .result(productService.getByShopIdPaged(shopId, page, size))
                .build();
    }

    @PostMapping
    public ApiResponse<ProductResponse> createProduct(
            @RequestPart("data") String data,
            @RequestPart(value = "image", required = false) MultipartFile image
    ) throws Exception {

        ObjectMapper objectMapper = new ObjectMapper();
        ProductCreationRequest request =
                objectMapper.readValue(data, ProductCreationRequest.class);

        return ApiResponse.<ProductResponse>builder()
                .result(productService.createProduct(request, image))
                .build();
    }

    @PutMapping(value = "/{id}", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    public ApiResponse<ProductResponse> updateProduct(
            @PathVariable Integer id,
            @RequestPart("data") String data,
            @RequestPart(value = "image", required = false) MultipartFile image
    ) throws Exception {

        ObjectMapper objectMapper = new ObjectMapper();
        ProductUpdateRequest request =
                objectMapper.readValue(data, ProductUpdateRequest.class);

        return ApiResponse.<ProductResponse>builder()
                .result(productService.updateProduct(id, request, image))
                .build();
    }

    @DeleteMapping("/{id}")
    public ApiResponse<Void> deleteProduct(
            @PathVariable Integer id
    ) {
        log.info("Delete product id={}", id);

        productService.deleteProduct(id);

        return ApiResponse.<Void>builder()
                .message("PRODUCT_DELETED")
                .build();
    }

    @GetMapping("/{id}")
    public ApiResponse<ProductResponse> getById(@PathVariable Integer id) {
        return ApiResponse.<ProductResponse>builder()
                .result(productService.getById(id))
                .build();
    }

    @GetMapping("/shop/{shopId}")
    public ApiResponse<List<ProductResponse>> getByShopId(@PathVariable Integer shopId) {
        return ApiResponse.<List<ProductResponse>>builder()
                .result(productService.getByShopId(shopId))
                .build();
    }

    @GetMapping("/pending")
    @PreAuthorize("hasRole('ADMIN')")
    public ApiResponse<List<ProductResponse>> getPendingProducts() {
        return ApiResponse.<List<ProductResponse>>builder()
                .result(productService.getPendingProducts())
                .build();
    }

    @PatchMapping("/{id}/approve")
    @PreAuthorize("hasRole('ADMIN')")
    public ApiResponse<ProductResponse> approveProduct(@PathVariable Integer id) {
        return ApiResponse.<ProductResponse>builder()
                .result(productService.approveProduct(id))
                .build();
    }

    @PatchMapping("/{id}/reject")
    @PreAuthorize("hasRole('ADMIN')")
    public ApiResponse<ProductResponse> rejectProduct(
            @PathVariable Integer id,
            @RequestParam(required = false) String reason
    ) {
        return ApiResponse.<ProductResponse>builder()
                .result(productService.rejectProduct(id, reason))
                .build();
    }
}
