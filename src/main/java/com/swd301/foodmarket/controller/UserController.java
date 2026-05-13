package com.swd301.foodmarket.controller;

import com.fasterxml.jackson.databind.ObjectMapper;
import jakarta.validation.Valid;
import lombok.AccessLevel;
import lombok.RequiredArgsConstructor;
import lombok.experimental.FieldDefaults;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.MediaType;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;
import com.swd301.foodmarket.dto.request.UserCreationRequest;
import com.swd301.foodmarket.dto.request.UserUpdateRequest;
import com.swd301.foodmarket.dto.response.ApiResponse;
import com.swd301.foodmarket.dto.response.PageResponse;
import com.swd301.foodmarket.dto.response.UserResponse;
import com.swd301.foodmarket.service.UserService;
import org.springframework.web.multipart.MultipartFile;

import java.util.List;

@RestController
@RequestMapping("/users")
@RequiredArgsConstructor
@FieldDefaults(level = AccessLevel.PRIVATE, makeFinal = true)
@Slf4j
public class UserController {
    UserService userService;

    /**
     * Register new user (Public endpoint)
     */
    @PostMapping(value = "/register", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    public ApiResponse<UserResponse> createUser(
            @RequestPart("data") String data,
            @RequestPart(value = "logoUrl", required = false) MultipartFile logoUrl,
            @RequestPart(value = "achievement", required = false) MultipartFile achievement,
            @RequestPart(value = "licenseImage", required = false) MultipartFile licenseImage,
            @RequestPart(value = "vehicleDocImage", required = false) MultipartFile vehicleDocImage
    ) throws Exception {

        ObjectMapper objectMapper = new ObjectMapper();
        UserCreationRequest request = objectMapper.readValue(data, UserCreationRequest.class);

        return ApiResponse.<UserResponse>builder()
                .result(userService.createUser(request, logoUrl, achievement, licenseImage, vehicleDocImage))
                .build();
    }

    /**
     * Get my profile (Authenticated users)
     */
    @GetMapping("/me")
    public ApiResponse<UserResponse> getMyInfo() {
        log.info("Get my info");
        return ApiResponse.<UserResponse>builder()
                .result(userService.getMyInfo())
                .build();
    }

    /**
     * Update my images (Authenticated users — multipart)
     */
    @PatchMapping(value = "/me/images", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    public ApiResponse<UserResponse> updateMyImages(
            @RequestPart(value = "logoUrl", required = false) MultipartFile logoUrl,
            @RequestPart(value = "licenseImage", required = false) MultipartFile licenseImage,
            @RequestPart(value = "vehicleDocImage", required = false) MultipartFile vehicleDocImage
    ) {
        log.info("Update my images");
        return ApiResponse.<UserResponse>builder()
                .result(userService.updateMyImages(logoUrl, licenseImage, vehicleDocImage))
                .build();
    }

    /**
     * Update my profile (Authenticated users)
     */
    @PutMapping("/me")
    public ApiResponse<UserResponse> updateMyInfo(@RequestBody @Valid UserUpdateRequest request) {
        log.info("Update my info");
        return ApiResponse.<UserResponse>builder()
                .result(userService.updateMyInfo(request))
                .build();
    }

    /**
     * Get user by ID (Admin only)
     */
    @GetMapping("/{userId}")
    @PreAuthorize("hasRole('ADMIN')")
    public ApiResponse<UserResponse> getUserById(@PathVariable Integer userId) {
        log.info("Get user by ID: {}", userId);
        return ApiResponse.<UserResponse>builder()
                .result(userService.getUserById(userId))
                .build();
    }

    /**
     * Get all users (Admin only)
     */
    @GetMapping
    @PreAuthorize("hasRole('ADMIN')")
    public ApiResponse<List<UserResponse>> getAllUsers() {
        log.info("Get all users");
        return ApiResponse.<List<UserResponse>>builder()
                .result(userService.getAllUsers())
                .build();
    }

    /**
     * Get all users paged (Admin only)
     */
    @GetMapping("/paged")
    @PreAuthorize("hasRole('ADMIN')")
    public ApiResponse<PageResponse<UserResponse>> getAllUsersPaged(
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "10") int size
    ) {
        log.info("Get all users paged");
        return ApiResponse.<PageResponse<UserResponse>>builder()
                .result(userService.getAllUsersPaged(page, size))
                .build();
    }

    /**
     * Update user (Admin only)
     */
    /**
     * Update user (Admin only)
     */
    @PutMapping(value = "/{userId}", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    @PreAuthorize("hasRole('ADMIN')")
    public ApiResponse<UserResponse> updateUser(
            @PathVariable Integer userId,
            @RequestPart("data") @Valid UserUpdateRequest request,
            @RequestPart(value = "logoUrl", required = false) MultipartFile logoUrl,
            @RequestPart(value = "achievement", required = false) MultipartFile achievement
    ) {
        log.info("[PUT] /users/{} - Update user", userId);

        UserResponse result = userService.updateUser(userId, request, logoUrl, achievement);

        ApiResponse<UserResponse> response = new ApiResponse<>();
        response.setResult(result);
        return response;
    }
    /**
     * Delete user (Admin only)
     */
    @DeleteMapping("/{userId}")
    @PreAuthorize("hasRole('ADMIN')")
    public ApiResponse<Void> deleteUser(@PathVariable Integer userId) {
        log.info("Delete user: {}", userId);
        userService.deleteUser(userId);
        return ApiResponse.<Void>builder()
                .message("User deleted successfully")
                .build();
    }

    /**
     * Activate user (Admin only)
     */
    @PatchMapping("/{userId}/activate")
    @PreAuthorize("hasRole('ADMIN')")
    public ApiResponse<Void> activateUser(@PathVariable Integer userId) {
        log.info("Activate user: {}", userId);
        userService.activateUser(userId);
        return ApiResponse.<Void>builder()
                .message("User activated successfully")
                .build();
    }

    /**
     * Deactivate user (Admin only)
     */
    @PatchMapping("/{userId}/deactivate")
    @PreAuthorize("hasRole('ADMIN')")
    public ApiResponse<Void> deactivateUser(@PathVariable Integer userId) {
        log.info("Deactivate user: {}", userId);
        userService.deactivateUser(userId);
        return ApiResponse.<Void>builder()
                .message("User deactivated successfully")
                .build();
    }
    // ================= APPROVE ENDPOINTS =================

    /**
     * Approve shop owner (Admin only)
     */
    @PatchMapping("/{userId}/approve-shop-owner")
    @PreAuthorize("hasRole('ADMIN')")
    public ApiResponse<UserResponse> approveShopOwner(@PathVariable Integer userId) {
        log.info("Approve shop owner: {}", userId);
        return ApiResponse.<UserResponse>builder()
                .result(userService.approveShopOwner(userId))
                .message("Shop owner approved successfully")
                .build();
    }

    /**
     * Approve shipper (Admin only)
     */
    @PatchMapping("/{userId}/approve-shipper")
    @PreAuthorize("hasRole('ADMIN')")
    public ApiResponse<UserResponse> approveShipper(@PathVariable Integer userId) {
        log.info("Approve shipper: {}", userId);
        return ApiResponse.<UserResponse>builder()
                .result(userService.approveShipper(userId))
                .message("Shipper approved successfully")
                .build();
    }
}