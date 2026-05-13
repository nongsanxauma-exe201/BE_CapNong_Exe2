package com.swd301.foodmarket.service.impl;

import com.swd301.foodmarket.entity.Role;
import com.swd301.foodmarket.entity.Wallet;
import com.swd301.foodmarket.enums.RoleName;
import com.swd301.foodmarket.enums.UserStatus;
import com.swd301.foodmarket.enums.WalletStatus;
import com.swd301.foodmarket.enums.WalletType;
import com.swd301.foodmarket.repository.WalletRepository;
import com.swd301.foodmarket.service.CloudinaryService;
import jakarta.mail.internet.MimeMessage;
import lombok.AccessLevel;
import lombok.RequiredArgsConstructor;
import lombok.experimental.FieldDefaults;
import lombok.extern.slf4j.Slf4j;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.mail.javamail.MimeMessageHelper;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import com.swd301.foodmarket.dto.request.UserCreationRequest;
import com.swd301.foodmarket.dto.request.UserUpdateRequest;
import com.swd301.foodmarket.dto.response.PageResponse;
import com.swd301.foodmarket.dto.response.UserResponse;
import com.swd301.foodmarket.entity.User;
import com.swd301.foodmarket.exception.AppException;
import com.swd301.foodmarket.exception.ErrorCode;
import com.swd301.foodmarket.mapper.UserMapper;
import com.swd301.foodmarket.repository.RoleRepository;
import com.swd301.foodmarket.repository.UserRepository;
import com.swd301.foodmarket.service.UserService;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.web.multipart.MultipartFile;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
@FieldDefaults(level = AccessLevel.PRIVATE, makeFinal = true)
@Slf4j
public class UserServiceImpl implements UserService {

    UserRepository userRepository;
    RoleRepository roleRepository;
    UserMapper userMapper;
    PasswordEncoder passwordEncoder;
    CloudinaryService cloudinaryService;
    WalletRepository walletRepository;
    JavaMailSender javaMailSender;

    @Override
    public UserResponse createUser(
            UserCreationRequest request,
            MultipartFile logoUrl,
            MultipartFile achievement,
            MultipartFile licenseImage,
            MultipartFile vehicleDocImage
    ) {

        log.info("Creating user with role: {}", request.getRoleName());

        if (userRepository.findByEmail(request.getEmail()).isPresent()) {
            throw new AppException(ErrorCode.USER_EXISTED);
        }

        User user = userMapper.toUser(request);
        user.setPassword(passwordEncoder.encode(request.getPassword()));

        Role role = roleRepository.findByName(request.getRoleName())
                .orElseThrow(() -> new AppException(ErrorCode.ROLE_NOT_FOUND));

        user.setRole(role);
        user.setCreateAt(LocalDateTime.now());

        // ===== STATUS BASED ON ROLE =====

        if (request.getRoleName() == RoleName.BUYER) {
            user.setStatus(UserStatus.ACTIVE);
        }
        else if (request.getRoleName() == RoleName.SHOP_OWNER
                || request.getRoleName() == RoleName.SHIPPER) {
            user.setStatus(UserStatus.PENDING);
        }

        // ===== SHOP OWNER DATA =====

        if (request.getRoleName().equals(RoleName.SHOP_OWNER)) {

            user.setBankName(request.getBankName());
            user.setBankAccountHolder(request.getBankAccountHolder());
            user.setBankAccount(request.getBankAccount());

            if (logoUrl != null && !logoUrl.isEmpty()) {
                String logoCloudUrl = cloudinaryService.uploadImage(logoUrl);
                user.setLogoUrl(logoCloudUrl);
            }

            if (achievement != null && !achievement.isEmpty()) {
                String achievementCloudUrl = cloudinaryService.uploadImage(achievement);
                user.setAchievement(achievementCloudUrl);
            }

        } else {

            user.setShopName(null);
            user.setBankAccount(request.getBankAccount());
            user.setBankAccountHolder(request.getBankAccountHolder());
            user.setBankName(request.getBankName());
            user.setRatingAverage(null);
            user.setDescription(null);

            if (logoUrl != null && !logoUrl.isEmpty()) {
                String logoCloudUrl = cloudinaryService.uploadImage(logoUrl);
                user.setLogoUrl(logoCloudUrl);
            }

            user.setAchievement(null);
        }

        // ===== SHIPPER DATA =====

        if (!request.getRoleName().equals(RoleName.SHIPPER)) {
            user.setLicense(null);
            user.setVehicleNumber(null);
        } else {
            // Upload ảnh bằng lái xe
            if (licenseImage != null && !licenseImage.isEmpty()) {
                String licenseImageUrl = cloudinaryService.uploadImage(licenseImage);
                user.setLicenseImageUrl(licenseImageUrl);
            }
            // Upload ảnh giấy tờ xe
            if (vehicleDocImage != null && !vehicleDocImage.isEmpty()) {
                String vehicleDocImageUrl = cloudinaryService.uploadImage(vehicleDocImage);
                user.setVehicleDocImageUrl(vehicleDocImageUrl);
            }
        }

        // ===== SAVE USER =====

        User savedUser = userRepository.save(user);

        log.info("User created successfully with ID: {}", savedUser.getId());

        // ===============================
        // AUTO CREATE SHOP WALLET
        // ===============================

        if (savedUser.getRole().getName().equals(RoleName.SHOP_OWNER)) {

            if (!walletRepository.existsByShopOwner(savedUser)) {

                Wallet wallet = Wallet.builder()
                        .shopOwner(savedUser)
                        .status(WalletStatus.ACTIVE)
                        .totalBalance(BigDecimal.ZERO)
                        .frozenBalance(BigDecimal.ZERO)
                        .totalRevenueAllTime(BigDecimal.ZERO)
                        .totalWithdrawn(BigDecimal.ZERO)
                        .commissionWallet(BigDecimal.ZERO)
                        .type(WalletType.SHOP)
                        .build();

                walletRepository.save(wallet);

                log.info("Wallet created for shop owner ID: {}", savedUser.getId());
            }
        }

        // ===============================
        // AUTO CREATE SHIPPER WALLET
        // ===============================

        if (savedUser.getRole().getName().equals(RoleName.SHIPPER)) {

            if (!walletRepository.existsByShipper(savedUser)) {

                Wallet wallet = Wallet.builder()
                        .shipper(savedUser)
                        .status(WalletStatus.ACTIVE)
                        .totalBalance(BigDecimal.ZERO)
                        .frozenBalance(BigDecimal.ZERO)
                        .totalRevenueAllTime(BigDecimal.ZERO)
                        .totalWithdrawn(BigDecimal.ZERO)
                        .commissionWallet(BigDecimal.ZERO)
                        .type(WalletType.SHIPPER)
                        .build();

                walletRepository.save(wallet);

                log.info("Wallet created for shipper ID: {}", savedUser.getId());
            }
        }

        log.info("bankName in entity = {}", savedUser.getBankName());
        log.info("bankHolder in entity = {}", savedUser.getBankAccountHolder());

        return userMapper.toUserResponse(savedUser);
    }


    @Override
    public UserResponse updateUser(
            Integer userId,
            UserUpdateRequest request,
            MultipartFile logoUrl,
            MultipartFile achievement
    ) {
        log.info("Updating user with ID: {}", userId);

        User user = userRepository.findById(userId)
                .orElseThrow(() -> new AppException(ErrorCode.USER_NOT_EXISTED));

        // Update password nếu có
        if (request.getPassword() != null && !request.getPassword().isEmpty()) {
            user.setPassword(passwordEncoder.encode(request.getPassword()));
        }

        // Update các field khác
        userMapper.updateUser(user, request);

        // ===== Update logo nếu có =====
        if (logoUrl != null && !logoUrl.isEmpty()) {
            String logoCloudUrl = cloudinaryService.uploadImage(logoUrl);
            user.setLogoUrl(logoCloudUrl);
        }

        // ===== Update achievement nếu có =====
        if (achievement != null && !achievement.isEmpty()) {
            String achievementCloudUrl = cloudinaryService.uploadImage(achievement);
            user.setAchievement(achievementCloudUrl);
        }

        User updatedUser = userRepository.save(user);
        log.info("User updated successfully: {}", userId);

        return userMapper.toUserResponse(updatedUser);
    }

    @Override
    public UserResponse getUserById(Integer userId) {
        log.info("Getting user with ID: {}", userId);

        User user = userRepository.findById(userId)
                .orElseThrow(() -> new AppException(ErrorCode.USER_NOT_EXISTED));

        return userMapper.toUserResponse(user);
    }

    @Override
    public UserResponse getMyInfo() {
        var context = SecurityContextHolder.getContext();
        String email = context.getAuthentication().getName();

        User user = userRepository.findByEmail(email)
                .orElseThrow(() -> new AppException(ErrorCode.USER_NOT_EXISTED));

        return userMapper.toUserResponse(user);
    }

    @Override
    public UserResponse updateMyInfo(UserUpdateRequest request) {
        var context = SecurityContextHolder.getContext();
        String email = context.getAuthentication().getName();

        User user = userRepository.findByEmail(email)
                .orElseThrow(() -> new AppException(ErrorCode.USER_NOT_EXISTED));

        // Update password if provided
        if (request.getPassword() != null && !request.getPassword().isEmpty()) {
            user.setPassword(passwordEncoder.encode(request.getPassword()));
        }

        // Update other fields using mapper
        userMapper.updateUser(user, request);

        User updatedUser = userRepository.save(user);
        log.info("My info updated successfully");

        return userMapper.toUserResponse(updatedUser);
    }

    @Override
    public UserResponse updateMyImages(MultipartFile logoUrl, MultipartFile licenseImage, MultipartFile vehicleDocImage) {
        var context = SecurityContextHolder.getContext();
        String email = context.getAuthentication().getName();

        User user = userRepository.findByEmail(email)
                .orElseThrow(() -> new AppException(ErrorCode.USER_NOT_EXISTED));

        if (logoUrl != null && !logoUrl.isEmpty()) {
            user.setLogoUrl(cloudinaryService.uploadImage(logoUrl));
        }
        if (licenseImage != null && !licenseImage.isEmpty()) {
            user.setLicenseImageUrl(cloudinaryService.uploadImage(licenseImage));
        }
        if (vehicleDocImage != null && !vehicleDocImage.isEmpty()) {
            user.setVehicleDocImageUrl(cloudinaryService.uploadImage(vehicleDocImage));
        }

        User updated = userRepository.save(user);
        log.info("My images updated successfully");
        return userMapper.toUserResponse(updated);
    }

    @Override
    public List<UserResponse> getAllUsers() {
        log.info("Getting all users");

        return userRepository.findAll()
                .stream()
                .map(userMapper::toUserResponse)
                .collect(Collectors.toList());
    }

    @Override
    public PageResponse<UserResponse> getAllUsersPaged(int page, int size) {
        log.info("Getting all users paged: page={}, size={}", page, size);
        Pageable pageable = PageRequest.of(page, size, Sort.by("id").descending());
        Page<User> pageResult = userRepository.findAll(pageable);
        return PageResponse.<UserResponse>builder()
                .content(pageResult.getContent().stream().map(userMapper::toUserResponse).toList())
                .page(pageResult.getNumber())
                .size(pageResult.getSize())
                .totalElements(pageResult.getTotalElements())
                .totalPages(pageResult.getTotalPages())
                .first(pageResult.isFirst())
                .last(pageResult.isLast())
                .build();
    }

    @Override
    public void deleteUser(Integer userId) {
        log.info("Deleting user with ID: {}", userId);

        if (!userRepository.existsById(userId)) {
            throw new AppException(ErrorCode.USER_NOT_EXISTED);
        }

        userRepository.deleteById(userId);
        log.info("User deleted successfully: {}", userId);
    }

    @Override
    public void activateUser(Integer userId) {
        log.info("Activating user with ID: {}", userId);

        User user = userRepository.findById(userId)
                .orElseThrow(() -> new AppException(ErrorCode.USER_NOT_EXISTED));

        user.setStatus(UserStatus.ACTIVE);
        user.setLockedAt(null);
        userRepository.save(user);

        sendAccountLockedEmail(user);

        log.info("User activated successfully: {}", userId);
    }

    @Override
    public void deactivateUser(Integer userId) {
        log.info("Deactivating user with ID: {}", userId);

        User user = userRepository.findById(userId)
                .orElseThrow(() -> new AppException(ErrorCode.USER_NOT_EXISTED));

        user.setStatus(UserStatus.INACTIVE);
        user.setLockedAt(LocalDateTime.now());
        userRepository.save(user);

        log.info("User deactivated successfully: {}", userId);
    }

    @Override
    public UserResponse approveShopOwner(Integer userId) {
        log.info("Approving shop owner with ID: {}", userId);

        User user = userRepository.findById(userId)
                .orElseThrow(() -> new AppException(ErrorCode.USER_NOT_EXISTED));

        // Validate role
        if (user.getRole().getName() != RoleName.SHOP_OWNER) {
            throw new AppException(ErrorCode.MISS_ROLE);
        }

        // Validate current status
        if (user.getStatus() != UserStatus.PENDING) {
            throw new AppException(ErrorCode.INVALID_KEY);
        }

        user.setStatus(UserStatus.ACTIVE);
        User approvedUser = userRepository.save(user);

        log.info("Shop owner approved successfully: {}", userId);

        return userMapper.toUserResponse(approvedUser);
    }

    @Override
    public UserResponse approveShipper(Integer userId) {
        log.info("Approving shipper with ID: {}", userId);

        User user = userRepository.findById(userId)
                .orElseThrow(() -> new AppException(ErrorCode.USER_NOT_EXISTED));

        // Validate role
        if (user.getRole().getName() != RoleName.SHIPPER) {
            throw new AppException(ErrorCode.MISS_ROLE);
        }

        // Validate current status
        if (user.getStatus() != UserStatus.PENDING) {
            throw new AppException(ErrorCode.INVALID_KEY);
        }

        user.setStatus(UserStatus.ACTIVE);
        User approvedUser = userRepository.save(user);

        log.info("Shipper approved successfully: {}", userId);

        return userMapper.toUserResponse(approvedUser);
    }


    private void sendAccountLockedEmail(User user) {

        try {
            MimeMessage message = javaMailSender.createMimeMessage();
            MimeMessageHelper helper = new MimeMessageHelper(message, true, "UTF-8");

            helper.setTo(user.getEmail());
            helper.setFrom("your_email@gmail.com");
            helper.setSubject("Tài khoản của bạn đã bị khóa");

            String html = """
                <h2>Tài khoản của bạn đã bị khóa</h2>
                <p>Xin chào %s,</p>
                <p>Tài khoản của bạn đã bị khóa do có <b>3 đơn hàng thất bại</b>.</p>
                <p>Tài khoản sẽ được mở khóa sau <b>7 ngày</b>.</p>
                <p>Nếu có thắc mắc vui lòng liên hệ hệ thống.</p>
                """.formatted(user.getFullName());

            helper.setText(html, true);

            javaMailSender.send(message);

        } catch (Exception e) {
            log.error("Failed to send lock email to user {}", user.getId());
        }
    }
}