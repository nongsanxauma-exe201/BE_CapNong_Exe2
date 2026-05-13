package com.swd301.foodmarket.dto.request;

import com.swd301.foodmarket.enums.RoleName;
import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import lombok.*;
import lombok.experimental.FieldDefaults;
import org.springframework.web.multipart.MultipartFile;

@Data
@Builder
@AllArgsConstructor
@NoArgsConstructor
@FieldDefaults(level = AccessLevel.PRIVATE)
public class UserCreationRequest {

    @NotBlank(message = "EMAIL_REQUIRED")
    @Email(message = "EMAIL_INVALID")
    @Size(max = 100, message = "EMAIL_TOO_LONG")
    String email;

    @NotBlank(message = "PASSWORD_REQUIRED")
    @Size(min = 8, message = "INVALID_PASSWORD")
    String password;

    @NotNull(message = "ROLE_REQUIRED")
    RoleName roleName;

    // Common fields
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
    String licenseImageUrl;
    String vehicleDocImageUrl;
}