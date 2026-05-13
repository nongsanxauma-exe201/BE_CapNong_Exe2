package com.swd301.foodmarket.dto.request;

import com.swd301.foodmarket.enums.RoleName;
import lombok.Data;

@Data
public class CompleteProfileRequest {
    private RoleName roleName;
}