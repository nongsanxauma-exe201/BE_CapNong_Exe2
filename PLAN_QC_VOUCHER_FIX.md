# Kế hoạch sửa Quality Check & Voucher (QC + Damaged Item Voucher + Welcome Voucher)

## Tóm tắt
Có 3 luồng chính cần sửa:
1. **Sửa luồng Quality Check (QC)**: Bỏ kết quả "đạt/không đạt" cuối cùng, thêm field tùy chọn nhập mô tả hàng hư.
2. **Voucher HƯ ĐƠN**: Khi QC phát hiện hàng hư, tự động tạo voucher giảm 20% (max 50k) cho shop gửi tặng buyer khi mà đơn hàng đó giao thành công nha .
3. **Welcome Voucher**: Khi user đăng ký, tặng voucher ship 0đ từ platform + gửi thông báo.

---

## 1. Sửa luồng Quality Check

### 1.1 Hiện trạng
- `QualityCheckRequest` chứa danh sách `qualityChecks` với từng tiêu chí.
- Mỗi tiêu chí có `isPassed` (boolean).
- `QualityResult` có trường cuối (chưa rõ cái này là gì - cần kiểm tra) cái này.
- Hệ thống ghi các tiêu chí pass/fail nhưng không có trường cho mô tả hàng hư.

### 1.2 Vấn đề cần sửa
- [ ] **Bỏ trường "đạt/không đạt" cuối cùng**: Xác định trường nào cần bỏ trong `QualityResult` hoặc `QualityCheckRequest`.
- [ ] **Thêm field tùy chọn hàng hư**: Thêm trường `damageNote` (string) hoặc `damageDescription` vào `QualityCheckRequest`.
- [ ] **Logic**: Nếu staff nhập `damageNote`, hệ thống sẽ:
  - Ghi nhận hàng hư.
  - Tự động tạo "VOUCHER HƯ ĐƠN" cho shop (bước 2).

### 1.3 Files cần sửa
- **Backend**:
  - `QualityCheckRequest` (dto/request): Thêm/sửa field
  - `QualityCheck` (entity): Có thể thêm trường nếu cần
  - `QualityCheckServiceImpl`: Xử lý logic damage
  - `QualityCheckController`: API nhận request mới

### 1.4 API request mẫu (mới)
```json
{
  "orderDetailId": 1,
  "qualityChecks": [
    {
      "qualityCrieriaId": 1,
      "isPassed": true,
      "note": "Ok"
    }
  ],
  "approvedKg": 100,
  "rejectedKg": 0,
  "damageNote": "Hàng bị nước đọng, cần thay",  // ← THÊM MỚI
  "note": "..."
}
```

### 1.5 Kiểm tra các luồng bị ảnh hưởng
- [ ] `OrderServiceImpl` → Xem có logic dựa vào pass/fail chung không?
- [ ] `OrderMapper.allPassQualityChecks()` → Tính logic pass/fail toàn đơn.
- [ ] `OrderStatus.QUALITY_CHECKED` → Khi nào được set khi kiểm tra xong 
- [ ] Notification / webhook khi QC xong.

---

## 2. Voucher HƯ ĐƠN (Damaged Item Voucher)

### 2.1 Yêu cầu
- **Trigger**: Khi QC phát hiện hàng hư (staff nhập `damageNote`).
- **Loại voucher**: "HƯ ĐƠN" (Voucher tặng vì hàng bị hỏng).
- **Giảm**: 20% tổng đơn hàng.
- **Giới hạn**: Tối đa 50k.
- **Hiệu lực**: 15 ngày kể từ ngày nhận được hàng.
- **Chủ sở hữu**: Shop (để shop tặng buyer).
- **Số lượng**: 1 voucher per đơn hàng bị hư. và khi đơn hàng đó hoàn thành thì người đó mới nhận được voucher và hệ thống sẽ thông báo cho người dùng là bạn vừa được tặng voucher từ shop .... thay lời xin lỗi do đơn hàng .. 

### 2.2 Logic tạo
Khi `QualityCheckServiceImpl.checkQuality()` xong:
1. Kiểm tra `damageNote` có giá trị không.
2. Nếu có:
   - Lấy `order.shopOwner` từ order detail.
   - Gọi `VoucherService` để tạo voucher tự động.
   - Ghi discount value = 20%, maxDiscount = 50k.
   - Set `expiryDate = receivedDate + 15 days`.
   - Lưu voucher vào `Voucher` table với `shopOwner = order.shopOwner`.

### 2.3 Files cần sửa
- **Backend**:
  - `VoucherService` / `VoucherServiceImpl`: Thêm method `createAutoVoucherForDamage()`.
  - `QualityCheckServiceImpl`: Gọi method tạo voucher khi phát hiện hư.
  - `Voucher` (entity): Có thể thêm field `autoCreatedReason` = "DAMAGED_ORDER" để tracking.

### 2.4 Code logic mẫu (pseudocode)
```java
// Trong QualityCheckServiceImpl.checkQuality()
if (qualityCheckRequest.getDamageNote() != null && !qualityCheckRequest.getDamageNote().isEmpty()) {
    // Lấy thông tin order và shop
    Order order = orderDetail.getOrder(); // hoặc từ mysteryBox
    User shopOwner = order.getShopOwner();
    LocalDateTime receiveDate = order.getReceivedDate(); // hoặc receivedAt field
    
    // Tạo voucher
    VoucherCreateRequest damageVoucherReq = VoucherCreateRequest.builder()
        .discountValue(new BigDecimal("20"))  // 20%
        .maxDiscount(new BigDecimal("50000"))  // 50k VND
        .minOrderValue(BigDecimal.ZERO)
        .claimLimit(1)
        .usageLimit(1)
        .perUserLimit(1)
        .expiryDate(receiveDate.plusDays(15))
        .voucherType("DAMAGED_ORDER")  // tracking
        .shopOwnerId(shopOwner.getId())
        .build();
    
    // Gọi service tạo voucher với currentUser = shop
    voucherService.createAutoVoucher(damageVoucherReq);
}
```

### 2.5 Thông báo cho shop
- [ ] Gửi notification cho shop khi QC phát hiện hư và tạo voucher tự động.
- [ ] Thông báo có thể nói: "Hàng của đơn #XYZ bị hư, chúng tôi đã tạo voucher giảm 20% (max 50k) trong 15 ngày. Vui lòng gửi tặng voucher cho buyer."

---

## 3. Welcome Voucher (Ship 0đ)

### 3.1 Yêu cầu
- **Trigger**: Khi user đăng ký tài khoản thành công.
- **Loại voucher**: "SHIP 0Đ" (Free shipping).
- **Giảm**: 100% phí ship (hoặc giá trị cố định).
- **Chủ sở hữu**: Platform (admin).
- **Tính năng**: Automatic claim cho buyer hoặc gửi code.
- **Thông báo**: Gửi notification/email cho user khi đăng ký xong.

### 3.2 Logic tạo
Khi `UserServiceImpl.register()` hoặc `OAuth2LoginSuccessHandler` xong:
1. Tạo 1 voucher "SHIP 0Đ" từ platform.
2. Tự động claim cho user (hoặc generate code duy nhất).
3. Gửi notification / email với voucher code.

### 3.3 Files cần sửa
- **Backend**:
  - `UserServiceImpl.register()`: Sau khi tạo user, gọi `VoucherService` tạo welcome voucher.
  - `VoucherService` / `VoucherServiceImpl`: Thêm method `createWelcomeVoucher()`.
  - `VoucherUserService` / `VoucherUserServiceImpl`: Thêm method auto-claim voucher cho user.
  - `NotificationService`: Gửi thông báo welcome voucher.
  - `Voucher` (entity): Có field `voucherType` = "WELCOME" để tracking.

### 3.4 Code logic mẫu (pseudocode)
```java
// Trong UserServiceImpl.register()
User newUser = userRepository.save(user);

// Tạo welcome voucher
if (newUser.getRole().getName() == RoleName.BUYER || 
    newUser.getRole().getName() == RoleName.SHOP_OWNER) {
    
    VoucherCreateRequest welcomeVoucherReq = VoucherCreateRequest.builder()
        .voucherCode("WELCOME" + newUser.getId())  // hoặc random unique
        .discountValue(new BigDecimal("100"))  // 100% giảm
        .maxDiscount(BigDecimal.ZERO)  // Không giới hạn (hoặc set max như 30k)
        .minOrderValue(BigDecimal.ZERO)
        .claimLimit(1)
        .usageLimit(1)
        .perUserLimit(1)
        .expiryDate(LocalDateTime.now().plusDays(30))  // 30 ngày
        .voucherType("WELCOME")
        .build();
    
    // Tạo voucher với admin account (hoặc system)
    Voucher welcomeVoucher = voucherService.createSystemVoucher(welcomeVoucherReq);
    
    // Auto-claim cho user
    VoucherUser voucherUser = voucherUserService.autoClaimVoucher(newUser, welcomeVoucher);
    
    // Gửi notification
    notificationService.sendWelcomeVoucherNotification(newUser, welcomeVoucher);
}
```

### 3.5 Thông báo cho user
- [ ] Gửi notification: "Chào mừng bạn đến với Nông Sản Xauma! Bạn đã nhận được voucher ship 0đ, hãy sử dụng: WELCOME{userId}"
- [ ] Hoặc tự động apply vào profile buyer để xem voucher đó.

---

## 4. Checklist thực thi

### 4.1 Quality Check
- [ ] Xác định trường "đạt/không đạt" cuối cần bỏ.
- [ ] Thêm field `damageNote` (string, nullable) vào `QualityCheckRequest`.
- [ ] Update `QualityCheckServiceImpl.checkQuality()` xử lý logic damage.
- [ ] Kiểm tra `OrderMapper.allPassQualityChecks()` logic.
- [ ] Kiểm tra các luồng ảnh hưởng: order status, notification, etc.

### 4.2 Damaged Item Voucher
- [ ] Thêm method `createAutoVoucherForDamage()` vào `VoucherService`.
- [ ] Implement trong `VoucherServiceImpl`.
- [ ] Gọi từ `QualityCheckServiceImpl` khi `damageNote` có giá trị.
- [ ] Thêm field tracking `voucherType` hoặc `autoCreatedReason` vào `Voucher` entity.
- [ ] Gửi notification cho shop.

### 4.3 Welcome Voucher
- [ ] Thêm method `createWelcomeVoucher()` và `autoClaimVoucher()`.
- [ ] Update `UserServiceImpl.register()` sau khi tạo user.
- [ ] Thêm logic auto-claim hoặc tạo `VoucherUser` record.
- [ ] Gửi notification/email welcome voucher.
- [ ] Thêm field tracking `voucherType` = "WELCOME" vào `Voucher`.

### 4.4 Test case
- [ ] QC với damage note → tạo voucher tự động.
- [ ] Register user → nhận welcome voucher.
- [ ] Buyer sử dụng welcome voucher trên đơn hàng.
- [ ] Shop nhận thông báo voucher hư.
- [ ] Buyer nhận thông báo welcome voucher.

---

## 5. Database changes

### 5.1 Voucher table (sửa/thêm)
```sql
-- Nếu chưa có
ALTER TABLE vouchers ADD COLUMN voucher_type VARCHAR(50) DEFAULT NULL;  -- WELCOME, DAMAGED_ORDER, SHOP, etc.
ALTER TABLE vouchers ADD COLUMN auto_created_from_order_id INT DEFAULT NULL;  -- Tracking order nào tạo
ALTER TABLE vouchers ADD COLUMN created_by_admin BOOLEAN DEFAULT FALSE;  -- Platform created?
```

### 5.2 QualityCheck table (sửa/thêm)
```sql
-- Nếu cần
ALTER TABLE quality_check ADD COLUMN damage_note VARCHAR(500) DEFAULT NULL;  -- Mô tả hàng hư
ALTER TABLE quality_check ADD COLUMN auto_voucher_created BOOLEAN DEFAULT FALSE;  -- Đã tạo voucher chưa
```

---

## 6. API endpoints (mới hoặc sửa)

### 6.1 QC API (sửa)
- **POST** `/quality-checks` (sửa request body thêm `damageNote`)

### 6.2 System Voucher API (mới - nếu cần)
- **POST** `/admin/vouchers/create-system` (tạo voucher hệ thống, admin only)
- **POST** `/admin/vouchers/auto-welcome/{userId}` (tạo welcome voucher cho user)

---

## 7. Thứ tự thực hiện (recommend)
1. **Sửa QC luồng** (bước 1) - bỏ field, thêm `damageNote`.
2. **Bổ sung Voucher logic tạo tự động** (bước 2, 3) - thêm method vào service.
3. **Integrate QC + Damaged voucher** - gọi từ QC service.
4. **Integrate Register + Welcome voucher** - gọi từ User service.
5. **Test & thông báo** - verify flow hoạt động.

---

## 8. Notes
- Đảm bảo transaction safety khi tạo QC + voucher cùng lúc.
- Kiểm tra role authorization (chỉ staff/admin tạo QC, chỉ admin tạo system voucher).
- Ghi log chi tiết khi tạo voucher tự động để audit.
- Thông báo phải rõ ràng, hướng dẫn shop/buyer cách dùng voucher.

---

*Plan này dùng để gửi cho dev/chat để code.*
