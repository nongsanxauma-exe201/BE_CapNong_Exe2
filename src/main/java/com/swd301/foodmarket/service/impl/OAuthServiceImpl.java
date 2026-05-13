package com.swd301.foodmarket.service.impl;

import com.swd301.foodmarket.entity.Role;
import com.swd301.foodmarket.entity.User;
import com.swd301.foodmarket.enums.RoleName;
import com.swd301.foodmarket.enums.UserStatus;
import com.swd301.foodmarket.repository.RoleRepository;
import com.swd301.foodmarket.repository.UserRepository;
import com.swd301.foodmarket.service.OAuthService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
public class OAuthServiceImpl implements OAuthService {

    private final UserRepository userRepository;
    private final RoleRepository roleRepository;

    @Override
    public User processOAuthPostLogin(String email, String fullName, String avatar) {

        return userRepository.findByEmail(email)
                .orElseGet(() -> {

                    Role defaultRole = roleRepository.findByName(RoleName.BUYER)
                            .orElseThrow(() -> new RuntimeException("Role not found"));

                    User newUser = new User();
                    newUser.setEmail(email);
                    newUser.setFullName(fullName);
                    newUser.setLogoUrl(avatar);
                    newUser.setPassword(null);
                    newUser.setRole(defaultRole);
                    newUser.setStatus(UserStatus.ACTIVE);

                    return userRepository.save(newUser);
                });
    }

    @Override
    public void completeProfile(String email, RoleName roleName) {

        User user = userRepository.findByEmail(email)
                .orElseThrow(() -> new RuntimeException("User not found"));

        // Không cho đổi nếu đã nâng cấp rồi
        if (!user.getRole().getName().equals(RoleName.BUYER)) {
            throw new RuntimeException("Role already updated");
        }

        Role role = roleRepository.findByName(roleName)
                .orElseThrow(() -> new RuntimeException("Role not found"));

        user.setRole(role);

        if (roleName == RoleName.SHOP_OWNER || roleName == RoleName.SHIPPER) {
            user.setStatus(UserStatus.PENDING);
        }

        userRepository.save(user);
    }
}