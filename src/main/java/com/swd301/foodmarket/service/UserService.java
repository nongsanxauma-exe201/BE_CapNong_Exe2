package com.swd301.foodmarket.service;

import com.swd301.foodmarket.dto.request.UserCreationRequest;
import com.swd301.foodmarket.dto.request.UserUpdateRequest;
import com.swd301.foodmarket.dto.response.PageResponse;
import com.swd301.foodmarket.dto.response.UserResponse;
import org.springframework.web.multipart.MultipartFile;

import java.util.List;

public interface UserService {
    UserResponse createUser(UserCreationRequest request, MultipartFile logoUrl, MultipartFile achievement, MultipartFile licenseImage, MultipartFile vehicleDocImage);

    UserResponse updateUser(Integer userId, UserUpdateRequest request, MultipartFile logoUrl, MultipartFile achievement);

    UserResponse getUserById(Integer userId);

    UserResponse getMyInfo();

    UserResponse updateMyInfo(UserUpdateRequest request);

    UserResponse updateMyImages(MultipartFile logoUrl, MultipartFile licenseImage, MultipartFile vehicleDocImage);

    List<UserResponse> getAllUsers();
    PageResponse<UserResponse> getAllUsersPaged(int page, int size);

    void deleteUser(Integer userId);

    void activateUser(Integer userId);

    void deactivateUser(Integer userId);

    UserResponse approveShopOwner(Integer userId);

    UserResponse approveShipper(Integer userId);
}