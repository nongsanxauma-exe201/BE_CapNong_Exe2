package com.swd301.foodmarket.scheduler;

import com.swd301.foodmarket.entity.Role;
import com.swd301.foodmarket.entity.User;
import com.swd301.foodmarket.entity.Wallet;
import com.swd301.foodmarket.enums.RoleName;
import com.swd301.foodmarket.enums.WalletStatus;
import com.swd301.foodmarket.enums.WalletType;
import com.swd301.foodmarket.repository.RoleRepository;
import com.swd301.foodmarket.repository.UserRepository;
import com.swd301.foodmarket.repository.WalletRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.boot.CommandLineRunner;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.crypto.password.PasswordEncoder;

import java.math.BigDecimal;

@Configuration
@RequiredArgsConstructor
public class AdminInitializer implements CommandLineRunner {

    private final UserRepository userRepository;
    private final RoleRepository roleRepository;
    private final WalletRepository walletRepository;
    private final PasswordEncoder passwordEncoder;

    @Override
    public void run(String... args) {

        // 1️⃣ Tạo Role nếu chưa tồn tại
        for (RoleName roleName : RoleName.values()) {

            if (!roleRepository.existsByName(roleName)) {

                Role role = Role.builder()
                        .name(roleName)
                        .build();

                roleRepository.save(role);
            }
        }

        // 2️⃣ Kiểm tra admin tồn tại chưa
        boolean adminExists = userRepository.existsByRole_Name(RoleName.ADMIN);

        if (!adminExists) {

            Role roleAdmin = roleRepository.findByName(RoleName.ADMIN)
                    .orElseThrow(() -> new RuntimeException("Role ADMIN not found"));

            User admin = User.builder()
                    .email("admin@gmail.com")
                    .password(passwordEncoder.encode("12345678"))
                    .role(roleAdmin)
                    .fullName("System Admin")
                    .build();

            userRepository.save(admin);

            System.out.println("Admin account created: admin@gmail.com / 12345678");
        }

        // 3️⃣ Lấy admin user
        User admin = userRepository.findFirstByRole_Name(RoleName.ADMIN)
                .orElseThrow(() -> new RuntimeException("Admin not found"));

        // 4️⃣ Kiểm tra admin wallet
        boolean walletExists = walletRepository.existsByAdmin_Id(admin.getId());

        if (walletExists) {
            System.out.println("Admin wallet already exists.");
        } else {

            Wallet wallet = Wallet.builder()
                    .admin(admin)
                    .status(WalletStatus.ACTIVE)
                    .type(WalletType.PLATFORM)
                    .totalBalance(BigDecimal.ZERO)
                    .frozenBalance(BigDecimal.ZERO)
                    .totalRevenueAllTime(BigDecimal.ZERO)
                    .totalWithdrawn(BigDecimal.ZERO)
                    .commissionWallet(BigDecimal.ZERO)
                    .build();

            walletRepository.save(wallet);

            System.out.println("Admin wallet created successfully.");
        }
    }
}