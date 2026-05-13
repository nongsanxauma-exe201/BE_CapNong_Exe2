package com.swd301.foodmarket.service;

import com.swd301.foodmarket.entity.User;
import com.swd301.foodmarket.enums.RoleName;

public interface OAuthService {

    User processOAuthPostLogin(String email, String fullName, String avatar);

    void completeProfile(String email, RoleName roleName);
}