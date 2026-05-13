package com.swd301.foodmarket.dto.request;

import com.swd301.foodmarket.enums.UserStatus;
import jakarta.validation.constraints.Size;
import lombok.*;
import lombok.experimental.FieldDefaults;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@FieldDefaults(level = AccessLevel.PRIVATE)
public class UserUpdateRequest {

    @Size(min = 8, message = "INVALID_PASSWORD")
    String password;

    String fullName;
    String phoneNumber;
    String address;

    // Shop Owner specific
    String shopName;
    String bankName;
    String bankAccount;
    String bankAccountHolder;
    String description;
    String logoUrl;
    String achievement;

    // Shipper specific
    String license;
    String vehicleNumber;

    UserStatus status;
}