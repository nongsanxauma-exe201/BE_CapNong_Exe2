package com.swd301.foodmarket.dto.response;

import com.swd301.foodmarket.enums.UserStatus;
import lombok.*;
import lombok.experimental.FieldDefaults;

import java.time.LocalDateTime;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@FieldDefaults(level = AccessLevel.PRIVATE)
public class UserResponse {
    Integer id;
    String email;
    String fullName;
    String phoneNumber;
    String address;
    UserStatus status;
    RoleResponse role;

    // Shop Owner specific
    String shopName;
    String bankName;
    String bankAccount;
    String bankAccountHolder;
    Double ratingAverage;
    String description;
    String logoUrl;
    String achievement;

    // Shipper specific
    String license;
    String vehicleNumber;
    String licenseImageUrl;
    String vehicleDocImageUrl;

    LocalDateTime createAt;
}