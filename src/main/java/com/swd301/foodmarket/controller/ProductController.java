package com.swd301.foodmarket.controller;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.swd301.foodmarket.dto.request.ProductCreationRequest;
import com.swd301.foodmarket.dto.request.ProductImageReorderRequest;
import com.swd301.foodmarket.dto.request.ProductUpdateRequest;
import com.swd301.foodmarket.dto.request.SetPrimaryImageRequest;
import com.swd301.foodmarket.dto.response.ApiResponse;
import com.swd301.foodmarket.dto.response.PageResponse;
import com.swd301.foodmarket.dto.response.ProductImageResponse;
import com.swd301.foodmarket.dto.response.ProductResponse;
import com.swd301.foodmarket.service.ProductService;
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
    ObjectMapper objectMapper;

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

    @PostMapping(consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    public ApiResponse<ProductResponse> createProduct(
            @RequestPart("data") String data,
            @RequestPart(value = "images", required = false) List<MultipartFile> images
    ) throws Exception {
        ProductCreationRequest request = objectMapper.readValue(data, ProductCreationRequest.class);
        return ApiResponse.<ProductResponse>builder()
                .result(productService.createProduct(request, images))
                .build();
    }

    @PutMapping(value = "/{id}", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    public ApiResponse<ProductResponse> updateProduct(
            @PathVariable Integer id,
            @RequestPart("data") String data,
            @RequestPart(value = "image", required = false) MultipartFile image
    ) throws Exception {
        ProductUpdateRequest request = objectMapper.readValue(data, ProductUpdateRequest.class);
        return ApiResponse.<ProductResponse>builder()
                .result(productService.updateProduct(id, request, image))
                .build();
    }

    @DeleteMapping("/{id}")
    public ApiResponse<Void> deleteProduct(@PathVariable Integer id) {
        productService.deleteProduct(id);
        return ApiResponse.<Void>builder().message("PRODUCT_DELETED").build();
    }

    @PutMapping("/active/{id}")
    public ApiResponse<Void> activateProduct(@PathVariable Integer id) {
        productService.activeProduct(id);
        return ApiResponse.<Void>builder().message("Active product").build();
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

    @GetMapping("/buyer")
    public ApiResponse<List<ProductResponse>> getByBuyer() {
        return ApiResponse.<List<ProductResponse>>builder()
                .result(productService.getProductForBuyer())
                .build();
    }

    // ─── Gallery endpoints ────────────────────────────────────────────────────

    @GetMapping("/{id}/images")
    public ApiResponse<List<ProductImageResponse>> getImages(@PathVariable Integer id) {
        return ApiResponse.<List<ProductImageResponse>>builder()
                .result(productService.getImages(id))
                .build();
    }

    @PostMapping(value = "/{id}/images", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    public ApiResponse<List<ProductImageResponse>> addImages(
            @PathVariable Integer id,
            @RequestPart("images") List<MultipartFile> images
    ) {
        return ApiResponse.<List<ProductImageResponse>>builder()
                .result(productService.addImages(id, images))
                .build();
    }

    @DeleteMapping("/{id}/images/{imageId}")
    public ApiResponse<Void> deleteImage(
            @PathVariable Integer id,
            @PathVariable Long imageId
    ) {
        productService.deleteImage(id, imageId);
        return ApiResponse.<Void>builder().message("IMAGE_DELETED").build();
    }

    @PatchMapping("/{id}/images/primary")
    public ApiResponse<ProductImageResponse> setPrimaryImage(
            @PathVariable Integer id,
            @RequestBody SetPrimaryImageRequest request
    ) {
        return ApiResponse.<ProductImageResponse>builder()
                .result(productService.setPrimaryImage(id, request))
                .build();
    }

    @PatchMapping("/{id}/images/reorder")
    public ApiResponse<List<ProductImageResponse>> reorderImages(
            @PathVariable Integer id,
            @RequestBody ProductImageReorderRequest request
    ) {
        return ApiResponse.<List<ProductImageResponse>>builder()
                .result(productService.reorderImages(id, request))
                .build();
    }
}
