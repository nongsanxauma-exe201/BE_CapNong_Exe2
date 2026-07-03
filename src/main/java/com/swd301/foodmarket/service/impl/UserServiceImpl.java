package com.swd301.foodmarket.service.impl;

import com.swd301.foodmarket.dto.response.AdminUserReport;
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
import com.swd301.foodmarket.service.VoucherService;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.web.multipart.MultipartFile;

import org.springframework.core.io.ClassPathResource;

import java.io.InputStream;
import java.math.BigDecimal;
import java.nio.charset.StandardCharsets;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.time.temporal.ChronoUnit;
import java.util.ArrayList;
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
    VoucherService voucherService;

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
        user.setCreateAt(LocalDateTime.now(ZoneId.of("Asia/Ho_Chi_Minh")));

        // ===== STATUS BASED ON ROLE =====

        if (request.getRoleName() == RoleName.BUYER) {
            user.setStatus(UserStatus.ACTIVE);
        } else if (request.getRoleName() == RoleName.SHOP_OWNER
                || request.getRoleName() == RoleName.SHIPPER) {
            user.setStatus(UserStatus.PENDING);
        }

        // ===== SHOP OWNER DATA =====

        if (request.getRoleName().equals(RoleName.SHOP_OWNER)) {

            user.setBankName(request.getBankName());
            user.setBankAccountHolder(request.getBankAccountHolder());
            user.setBankAccount(request.getBankAccount());
            user.setShopActive(true);
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
        if (request.getRoleName().equals(RoleName.BUYER)) {
            if (!walletRepository.existsByBuyer(savedUser)) {

                Wallet wallet = Wallet.builder()
                        .buyer(savedUser)
                        .status(WalletStatus.ACTIVE)
                        .totalBalance(BigDecimal.ZERO)
                        .frozenBalance(BigDecimal.ZERO)
                        .totalRevenueAllTime(BigDecimal.ZERO)
                        .totalWithdrawn(BigDecimal.ZERO)
                        .commissionWallet(BigDecimal.ZERO)
                        .type(WalletType.MEMBER)
                        .build();

                walletRepository.save(wallet);

                log.info("Wallet created for buyer ID: {}", savedUser.getId());
            }
        }

        voucherService.createWelcomeVoucherForUser(savedUser);

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

        // Update other fields using mapper
        userMapper.updateUser(user, request);

        // Encode password AFTER mapper (mapper overwrites with plain text if done before)
        if (request.getPassword() != null && !request.getPassword().isEmpty()) {
            user.setPassword(passwordEncoder.encode(request.getPassword()));
        }

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
    public PageResponse<UserResponse> getUsersByRolePaged(List<String> roles, String status, int page, int size) {
        log.info("Getting users by roles={} status={} paged: page={}, size={}", roles, status, page, size);
        Pageable pageable = PageRequest.of(page, size, Sort.by("id").descending());
        List<RoleName> roleNames = roles.stream().map(RoleName::valueOf).toList();
        Page<User> pageResult;
        if (status != null && !status.isBlank()) {
            UserStatus userStatus = UserStatus.valueOf(status);
            pageResult = userRepository.findByRole_NameInAndStatus(roleNames, userStatus, pageable);
        } else {
            pageResult = userRepository.findByRole_NameIn(roleNames, pageable);
        }
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
        user.setLockedAt(LocalDateTime.now(ZoneId.of("Asia/Ho_Chi_Minh")));
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
        sendShopOwnerApprovedEmail(approvedUser);

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
        sendShipperApprovedEmail(approvedUser);

        return userMapper.toUserResponse(approvedUser);
    }

    @Override
    public UserResponse updateStatusShop(Integer userId, boolean open) {

        User user = userRepository.findById(userId).orElseThrow(
                () -> new AppException(ErrorCode.USER_NOT_EXISTED)
        );
        user.setShopActive(open);

        return userMapper.toUserResponse(userRepository.save(user));
    }

    @Override
    public AdminUserReport generateAdminUserReport(String type, LocalDate from, LocalDate to) {

        LocalDate now = LocalDate.now();
        long step;

        if (type != null && !type.isBlank()) {
            if (type.equalsIgnoreCase("week")) {
                from = now.minusDays(6);
                to = now;
                step = 1;
            } else if (type.equalsIgnoreCase("month")) {
                from = now.minusDays(29);
                to = now;
                step = 7;
            } else if (type.equalsIgnoreCase("year")) {
                from = now.minusMonths(11).withDayOfMonth(1);
                to = now;
                step = 30;
            } else if (type.equalsIgnoreCase("all")) {
                from = LocalDate.of(now.getYear() - 5, 1, 1);
                to = now;
                step = 365;
            } else {
                from = now.minusDays(6);
                to = now;
                step = 1;
            }
        } else {
            if (from == null || to == null) {
                from = now.minusDays(6);
                to = now;
            }

            if (from.isAfter(to)) {
                throw new AppException(ErrorCode.INVALID_DATE);
            }

            long days = ChronoUnit.DAYS.between(from, to) + 1;

            if (days <= 14) {
                step = 1;
            } else if (days <= 90) {
                step = 7;
            } else if (days <= 366) {
                step = 30;
            } else {
                step = 365;
            }
        }

        AdminUserReport report = AdminUserReport.builder()
                .totalUsers(userRepository.count())
                .totalBuyers(userRepository.countByRole_Name(RoleName.BUYER))
                .totalShops(userRepository.countByRole_Name(RoleName.SHOP_OWNER))
                .totalActiveUsers(userRepository.countByStatus(UserStatus.ACTIVE))
                .totalInactiveUsers(userRepository.countByStatus(UserStatus.INACTIVE))
                .totalShopPending(userRepository.countByRole_NameAndStatus(RoleName.SHOP_OWNER, UserStatus.PENDING))
                .totalShopNotSelling(userRepository.countByRole_NameAndShopActive(RoleName.SHOP_OWNER, false))
                .totalShopSelling(userRepository.countByRole_NameAndShopActive(RoleName.SHOP_OWNER, true))
                .totalUserLock(userRepository.countUserLocked())
                .totalShipper(userRepository.countByRole_Name(RoleName.SHIPPER))
                .build();

        report.setUserReports(generateReport(from, to, step,type));

        return report;
    }

    public List<AdminUserReport.UserReport> generateReport(LocalDate from, LocalDate to, long step,String type) {
        List<AdminUserReport.UserReport> userReports = new ArrayList<>();

        LocalDate cursor = from;

        while (!cursor.isAfter(to)) {

            LocalDate toDate = cursor.plusDays(step - 1);
            if(step == 30)
            {
                toDate = cursor.plusMonths(1).minusDays(1);
            }else if(step == 365){
                toDate = cursor.plusYears(1).minusDays(1);
            }

            if (toDate.isAfter(to)) {
                toDate = to;
            }

            LocalDateTime startDateTime = cursor.atStartOfDay();
            LocalDateTime endDateTime = toDate.plusDays(1).atStartOfDay();

            String label;

            if (step == 1 && type != null && !type.isBlank()) {
                label = formatDayLabel(cursor);
            }
            else if(step ==1 && type == null )
            {
                label = cursor.getDayOfMonth() + "/" + cursor.getMonthValue();
            }
            else if (step == 7 ) {
                label = cursor.getDayOfMonth() + "/" + cursor.getMonthValue()
                        + " - "
                        + toDate.getDayOfMonth() + "/" + toDate.getMonthValue();
            } else if (step == 30) {
                label = "Tháng " + cursor.getMonthValue();
            } else {
                label = "Năm " + cursor.getYear();
            }

            AdminUserReport.UserReport userReport = AdminUserReport.UserReport.builder()
                    .fromDate(cursor)
                    .toDate(toDate)
                    .label(label)
                    .totalBuyers(
                            userRepository.countByRole_NameAndCreateAtGreaterThanEqualAndCreateAtLessThan(
                                    RoleName.BUYER,
                                    startDateTime,
                                    endDateTime
                            )
                    )
                    .totalShops(
                            userRepository.countByRole_NameAndCreateAtGreaterThanEqualAndCreateAtLessThan(
                                    RoleName.SHOP_OWNER,
                                    startDateTime,
                                    endDateTime
                            )
                    )
                    .totalUserLock(
                            userRepository.countByLockedAtGreaterThanEqualAndLockedAtLessThan(
                                    startDateTime,
                                    endDateTime
                            )
                    )
                    .totalShipper(userRepository.countByRole_NameAndCreateAtGreaterThanEqualAndCreateAtLessThan(
                            RoleName.SHIPPER,
                            startDateTime,
                            endDateTime
                    ))
                    .build();

            userReports.add(userReport);

            cursor = toDate.plusDays(1);
        }

        return userReports;
    }
    private void sendShopOwnerApprovedEmail(User user) {
        try {
            ClassPathResource resource = new ClassPathResource("templates/shop_owner_approved_template.html");
            InputStream inputStream = resource.getInputStream();
            String html = new String(inputStream.readAllBytes(), StandardCharsets.UTF_8)
                    .replace("${fullName}", user.getFullName());

            MimeMessage message = javaMailSender.createMimeMessage();
            MimeMessageHelper helper = new MimeMessageHelper(message, true, "UTF-8");
            helper.setTo(user.getEmail());
            helper.setFrom("nongsanxauma@gmail.com");
            helper.setSubject("Tài khoản người bán của bạn đã được duyệt - Nông sản xấu mã");
            helper.setText(html, true);
            javaMailSender.send(message);
        } catch (Exception e) {
            log.error("Failed to send approval email to shop owner {}", user.getId());
        }
    }

    private void sendShipperApprovedEmail(User user) {
        try {
            ClassPathResource resource = new ClassPathResource("templates/shipper_approved_template.html");
            InputStream inputStream = resource.getInputStream();
            String html = new String(inputStream.readAllBytes(), StandardCharsets.UTF_8)
                    .replace("${fullName}", user.getFullName());

            MimeMessage message = javaMailSender.createMimeMessage();
            MimeMessageHelper helper = new MimeMessageHelper(message, true, "UTF-8");
            helper.setTo(user.getEmail());
            helper.setFrom("nongsanxauma@gmail.com");
            helper.setSubject("Hồ sơ tài xế của bạn đã được duyệt - Nông sản xấu mã");
            helper.setText(html, true);
            javaMailSender.send(message);
        } catch (Exception e) {
            log.error("Failed to send approval email to shipper {}", user.getId());
        }
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
    private String formatDayLabel(LocalDate date) {
        return switch (date.getDayOfWeek()) {
            case MONDAY -> "T2";
            case TUESDAY -> "T3";
            case WEDNESDAY -> "T4";
            case THURSDAY -> "T5";
            case FRIDAY -> "T6";
            case SATURDAY -> "T7";
            case SUNDAY -> "CN";
        };
    }
}