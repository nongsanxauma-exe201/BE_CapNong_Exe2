package com.swd301.foodmarket.controller;

import com.swd301.foodmarket.dto.request.BuildComboCreationRequest;
import com.swd301.foodmarket.dto.request.BuildComboUpdateRequest;
import com.swd301.foodmarket.dto.response.ApiResponse;
import com.swd301.foodmarket.dto.response.BuildComboResponse;
import com.swd301.foodmarket.dto.response.ShopComboCountResponse;
import com.swd301.foodmarket.service.BuildComboService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.util.List;

@RestController
@RequestMapping("/build-combos")
@RequiredArgsConstructor
public class BuildComboController {

    private final BuildComboService buildComboService;

    @GetMapping("/shops")
    public ApiResponse<List<ShopComboCountResponse>> getShopsByComboCount() {
        return ApiResponse.<List<ShopComboCountResponse>>builder()
                .result(buildComboService.getShopsByComboCount())
                .build();
    }

    @GetMapping
    public ApiResponse<List<BuildComboResponse>> getAll() {
        return ApiResponse.<List<BuildComboResponse>>builder()
                .result(buildComboService.getAll())
                .build();
    }

    @PostMapping(consumes = "multipart/form-data")
    @PreAuthorize("hasRole('SHOP_OWNER')")
    public ApiResponse<BuildComboResponse> create(
            @RequestPart("data") @Valid BuildComboCreationRequest request,
            @RequestPart(value = "image", required = false) MultipartFile image
    ) {
        return ApiResponse.<BuildComboResponse>builder()
                .result(buildComboService.create(request, image))
                .build();
    }

    @PutMapping(value = "/{id}", consumes = "multipart/form-data")
    @PreAuthorize("hasRole('SHOP_OWNER')")
    public ApiResponse<BuildComboResponse> update(
            @PathVariable Integer id,
            @RequestPart("data") @Valid BuildComboUpdateRequest request,
            @RequestPart(value = "image", required = false) MultipartFile image
    ) {
        return ApiResponse.<BuildComboResponse>builder()
                .result(buildComboService.update(id, request, image))
                .build();
    }

    @DeleteMapping("/{id}")
    @PreAuthorize("hasRole('SHOP_OWNER')")
    public ApiResponse<Void> delete(@PathVariable Integer id) {
        buildComboService.delete(id);
        return ApiResponse.<Void>builder().build();
    }

    @GetMapping("/{id}")
    public ApiResponse<BuildComboResponse> getOne(@PathVariable Integer id) {
        return ApiResponse.<BuildComboResponse>builder()
                .result(buildComboService.getById(id))
                .build();
    }

    @GetMapping("/shop/{shopId}")
    public ApiResponse<List<BuildComboResponse>> getByShop(
            @PathVariable Integer shopId
    ) {
        return ApiResponse.<List<BuildComboResponse>>builder()
                .result(buildComboService.getByShopId(shopId))
                .build();
    }

    @GetMapping("/my-combos")
    @PreAuthorize("hasRole('SHOP_OWNER')")
    public ApiResponse<List<BuildComboResponse>> getMyCombos() {
        return ApiResponse.<List<BuildComboResponse>>builder()
                .result(buildComboService.getMyCombos())
                .build();
    }

}
