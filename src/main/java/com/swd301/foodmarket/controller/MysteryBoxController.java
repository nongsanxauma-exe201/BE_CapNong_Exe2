package com.swd301.foodmarket.controller;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.swd301.foodmarket.dto.request.MysteryBoxCreationRequest;
import com.swd301.foodmarket.dto.request.MysteryBoxUpdateRequest;
import com.swd301.foodmarket.dto.response.ApiResponse;
import com.swd301.foodmarket.dto.response.MysteryBoxResponse;
import com.swd301.foodmarket.service.MysteryBoxService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.MediaType;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.util.List;

@RestController
@RequestMapping("/mystery-boxes")
@RequiredArgsConstructor
public class MysteryBoxController {

    private final MysteryBoxService mysteryBoxService;

    @GetMapping
    public ApiResponse<List<MysteryBoxResponse>> getAll() {
        return ApiResponse.<List<MysteryBoxResponse>>builder()
                .result(mysteryBoxService.getAll())
                .build();
    }

    @PostMapping(consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
//    @PreAuthorize("hasRole('SHOP_OWNER')")
    public ApiResponse<MysteryBoxResponse> create(
            @RequestPart("data") String data,
            @RequestPart(value = "image", required = false) MultipartFile image
    ) throws Exception {

        ObjectMapper objectMapper = new ObjectMapper();
        MysteryBoxCreationRequest request =
                objectMapper.readValue(data, MysteryBoxCreationRequest.class);

        return ApiResponse.<MysteryBoxResponse>builder()
                .result(mysteryBoxService.createMysteryBox(request, image))
                .build();
    }

    @PutMapping(value = "/{id}", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    public ApiResponse<MysteryBoxResponse> updateMysteryBox(
            @PathVariable Integer id,
            @RequestPart("data") String data,
            @RequestPart(value = "image", required = false) MultipartFile image
    ) throws Exception {
        ObjectMapper objectMapper = new ObjectMapper();
        MysteryBoxUpdateRequest request = objectMapper.readValue(data, MysteryBoxUpdateRequest.class);
        return ApiResponse.<MysteryBoxResponse>builder()
                .result(mysteryBoxService.updateMysteryBox(id, request, image))
                .build();
    }

    @DeleteMapping("/{id}")
    @PreAuthorize("hasRole('SHOP_OWNER')")
    public ApiResponse<Void> delete(@PathVariable Integer id) {
        mysteryBoxService.deleteMysteryBox(id);
        return ApiResponse.<Void>builder().build();
    }

    @GetMapping("/{id}")
    public ApiResponse<MysteryBoxResponse> getOne(@PathVariable Integer id) {
        return ApiResponse.<MysteryBoxResponse>builder()
                .result(mysteryBoxService.getById(id))
                .build();
    }

    @GetMapping("/me")
    @PreAuthorize("hasRole('SHOP_OWNER')")
    public ApiResponse<List<MysteryBoxResponse>> getMyBoxes() {
        return ApiResponse.<List<MysteryBoxResponse>>builder()
                .result(mysteryBoxService.getMyMysteryBoxes())
                .build();
    }
}