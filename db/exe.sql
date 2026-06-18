-- ============================================================
-- FoodMarket_BE - EXE Database Script
-- Chủ đề: Củ + Quả/Trái cây (không có Rau lá, không có Combo)
-- Chạy lại bất kỳ lúc nào trên DB đã có schema
-- Password: Admin@123 (bcrypt)
-- ============================================================

SET FOREIGN_KEY_CHECKS = 0;

TRUNCATE TABLE user_notifications;
TRUNCATE TABLE notifications;
TRUNCATE TABLE reviews;
TRUNCATE TABLE return_requests;
TRUNCATE TABLE payments;
TRUNCATE TABLE transactions;
TRUNCATE TABLE order_details;
TRUNCATE TABLE order_mystery_boxes;
TRUNCATE TABLE trackings;
TRUNCATE TABLE orders;
TRUNCATE TABLE withdraw_requests;
TRUNCATE TABLE wallets;
TRUNCATE TABLE product_mystery;
TRUNCATE TABLE mystery_boxes;
TRUNCATE TABLE products;
TRUNCATE TABLE categories;
TRUNCATE TABLE vouchers;
TRUNCATE TABLE bot_knowledge;
TRUNCATE TABLE blogs;
TRUNCATE TABLE shipper_locations;
TRUNCATE TABLE cart_item;
TRUNCATE TABLE cart;
TRUNCATE TABLE chat_messages;
TRUNCATE TABLE conversations;
TRUNCATE TABLE meal_products;
TRUNCATE TABLE meal_item;
TRUNCATE TABLE meal;
TRUNCATE TABLE plan_day;
TRUNCATE TABLE build_plans;
TRUNCATE TABLE build_plan_items;
TRUNCATE TABLE users;
TRUNCATE TABLE roles;
TRUNCATE TABLE chat_bot;
TRUNCATE TABLE warehouses;

SET FOREIGN_KEY_CHECKS = 1;

-- ============================================================
-- 1. ROLES
-- ============================================================
ALTER TABLE roles
    MODIFY COLUMN name ENUM(
    'ADMIN',
    'BUYER',
    'SHOP_OWNER',
    'SHIPPER',
    'STAFF'
    );
INSERT INTO roles (name, description) VALUES   ('STAFF','staff personnel'),
                                               ('ADMIN','System administrator'),
                                               ('BUYER','Regular customer'),
                                               ('SHOP_OWNER','Shop owner / Seller'),
                                               ('SHIPPER','Shipment personnel')

    ON DUPLICATE KEY UPDATE description=VALUES(description);

SET @admin_role_id=(SELECT id FROM roles WHERE name='ADMIN');
SET @buyer_role_id=(SELECT id FROM roles WHERE name='BUYER');
SET @shop_role_id=(SELECT id FROM roles WHERE name='SHOP_OWNER');
SET @shipper_role_id=(SELECT id FROM roles WHERE name='SHIPPER');
SET @staff_role_id   = (SELECT id FROM roles WHERE name='STAFF');

-- ============================================================
-- 2. CATEGORIES (4 loại: Củ gia vị, Củ thực phẩm, Trái cây nhiệt đới, Trái cây quanh năm)
-- ============================================================
INSERT INTO categories (name, description) VALUES
                                               ('Củ gia vị','Các loại củ, rau có tính gia vị: tỏi, gừng, nghệ, hành tím, sả, ớt, cần tây, hành tây...'),
                                               ('Củ thực phẩm','Các loại rau củ dùng làm thức ăn chính: cà rốt, khoai tây, khoai lang, bí đỏ, cà tím, khoai môn...'),
                                               ('Trái cây nhiệt đới','Đặc sản trái cây nhiệt đới Việt Nam: xoài, thanh long, dưa hấu, ổi, sầu riêng, chôm chôm...'),
                                               ('Trái cây quanh năm','Trái cây phổ biến quanh năm: cam, bưởi, nho, cà chua, dưa leo, ớt chuông, táo, mướp...')
    ON DUPLICATE KEY UPDATE description=VALUES(description);

SET @cat_cu_gia_vi        =(SELECT category_id FROM categories WHERE name='Củ gia vị');
SET @cat_cu_thuc_pham     =(SELECT category_id FROM categories WHERE name='Củ thực phẩm');
SET @cat_trai_cay_nhiet_doi=(SELECT category_id FROM categories WHERE name='Trái cây nhiệt đới');
SET @cat_trai_cay_quanh_nam=(SELECT category_id FROM categories WHERE name='Trái cây quanh năm');

-- ============================================================
-- 3. USERS
-- ============================================================
INSERT INTO users (email,password,role_id,status,full_name,phone_number,address,description,create_at)
VALUES ('admin@gmail.com','$2a$12$lU/EpH6mhkL4ERuMRO2cjeeT1CPHxqAyDHGwnDctIWzojS6k/oG/K',@admin_role_id,'ACTIVE','Hệ thống Admin','0900000000','TP.HCM','Quản trị viên hệ thống',NOW())
    ON DUPLICATE KEY UPDATE email=email;
SET @admin_id=(SELECT id FROM users WHERE email='admin@gmail.com');

INSERT INTO users (email,password,role_id,status,full_name,phone_number,address,description,rating_average,bank_name,bank_account,bank_account_holder,create_at,logo_url)
VALUES ('buyer1@gmail.com','$2a$12$lU/EpH6mhkL4ERuMRO2cjeeT1CPHxqAyDHGwnDctIWzojS6k/oG/K',@buyer_role_id,'ACTIVE','Ngô Gia Sang','0911111111','Đ. Võ Trường Toản, Linh Trung, Thủ Đức, TP.HCM','Khách hàng thân thiết',5.0,'VietinBank','101888888888','NGO GIA SANG',NOW(),'https://res.cloudinary.com/dfp44sbsj/image/upload/v1774252968/brands/vxm3b2gvixlqzf7xocoy.jpg')
    ON DUPLICATE KEY UPDATE email=email;
SET @buyer1_id=(SELECT id FROM users WHERE email='buyer1@gmail.com');

INSERT INTO users (email,password,role_id,status,full_name,phone_number,address,description,rating_average,license,vehicle_number,license_image_url,vehicle_doc_image_url,create_at,logo_url)
VALUES ('shipper1@gmail.com','$2a$12$lU/EpH6mhkL4ERuMRO2cjeeT1CPHxqAyDHGwnDctIWzojS6k/oG/K',@shipper_role_id,'ACTIVE','Nguyễn Văn Hoàng','0922222222','Đ. Võ Trường Toản, Linh Trung, Thủ Đức, TP.HCM','Shipper nhiệt tình chuyên nghiệp',4.8,'A1-99999999','59-X1 123.45','https://res.cloudinary.com/dfp44sbsj/image/upload/v1774253418/brands/tftlykogelfaisztf7fk.jpg','https://res.cloudinary.com/dfp44sbsj/image/upload/v1774253567/brands/cneiljr2dzws18oibzuq.jpg',NOW(),'https://res.cloudinary.com/dfp44sbsj/image/upload/v1774252921/brands/xu1pv9ty2njacb3bhozh.jpg')
    ON DUPLICATE KEY UPDATE email=email;
SET @shipper1_id=(SELECT id FROM users WHERE email='shipper1@gmail.com');

INSERT INTO users (email,password,role_id,status,full_name,phone_number,address,shop_name,logo_url,description,rating_average,bank_name,bank_account,bank_account_holder,achievement,create_at)
VALUES ('shop1@gmail.com','$2a$12$lU/EpH6mhkL4ERuMRO2cjeeT1CPHxqAyDHGwnDctIWzojS6k/oG/K',@shop_role_id,'ACTIVE','Trần Văn Kiệt','0933333333','VRF2+9FG, Phường Linh Trung, Thủ Đức, TP.HCM','Nông Sản Xấu Mã','https://res.cloudinary.com/dfp44sbsj/image/upload/v1774252594/brands/srflhqfol1ckwavsaa1n.jpg','Chuyên củ quả organic sạch từ Đà Lạt và các vùng nông nghiệp sạch.',4.9,'Vietcombank','0071000123456','TRAN VAN KIET','Hạt Giống Vàng',NOW())
    ON DUPLICATE KEY UPDATE email=email;
SET @shop1_id=(SELECT id FROM users WHERE email='shop1@gmail.com');

INSERT INTO users (email,password,role_id,status,full_name,phone_number,address,shop_name,logo_url,description,rating_average,bank_name,bank_account,bank_account_holder,achievement,create_at)
VALUES ('shop2@gmail.com','$2a$12$lU/EpH6mhkL4ERuMRO2cjeeT1CPHxqAyDHGwnDctIWzojS6k/oG/K',@shop_role_id,'ACTIVE','Lê Thị Ngọc','0944444444','62 Nguyễn Bỉnh Khiêm, Dĩ An, Bình Dương','Vườn Trái Cây Bình Dương','https://res.cloudinary.com/dfp44sbsj/image/upload/v1774252337/brands/t9muel0yqmqfzfu3ndij.jpg','Chuyên củ quả tươi miền Nam, đặc sản miền Tây chất lượng cao.',4.7,'Techcombank','19034567890123','LE THI NGOC','Nông Dân Tinh Hoa',NOW())
    ON DUPLICATE KEY UPDATE email=email;
SET @shop2_id=(SELECT id FROM users WHERE email='shop2@gmail.com');

INSERT INTO users (email,password,role_id,status,full_name,phone_number,address,description,rating_average,bank_name,bank_account,bank_account_holder,create_at,logo_url)
VALUES ('buyer2@gmail.com','$2a$12$lU/EpH6mhkL4ERuMRO2cjeeT1CPHxqAyDHGwnDctIWzojS6k/oG/K',@buyer_role_id,'INACTIVE','Nguyễn Văn Xấu','0911111112','Q.1, TP.HCM','Khách hàng có lịch sử xấu (bị khóa)',0.0,'VietinBank','101888888889','NGUYEN VAN XAU',NOW(),'https://res.cloudinary.com/dfp44sbsj/image/upload/v1774252968/brands/vxm3b2gvixlqzf7xocoy.jpg')
    ON DUPLICATE KEY UPDATE email=email;
SET @buyer2_id=(SELECT id FROM users WHERE email='buyer2@gmail.com');

INSERT INTO users (email,password,role_id,status,full_name,phone_number,address,description,rating_average,license,vehicle_number,license_image_url,vehicle_doc_image_url,create_at,logo_url)
VALUES ('shipper2@gmail.com','$2a$12$lU/EpH6mhkL4ERuMRO2cjeeT1CPHxqAyDHGwnDctIWzojS6k/oG/K',@shipper_role_id,'PENDING','Trần Giao Hàng','0922222223','Thủ Đức, TP.HCM','Shipper mới đăng ký tài khoản chờ duyệt',0.0,'A1-12345678','59-X1 999.99','https://res.cloudinary.com/dfp44sbsj/image/upload/v1774253418/brands/tftlykogelfaisztf7fk.jpg','https://res.cloudinary.com/dfp44sbsj/image/upload/v1774253567/brands/cneiljr2dzws18oibzuq.jpg',NOW(),'https://res.cloudinary.com/dfp44sbsj/image/upload/v1774252921/brands/xu1pv9ty2njacb3bhozh.jpg')
    ON DUPLICATE KEY UPDATE email=email;
SET @shipper2_id=(SELECT id FROM users WHERE email='shipper2@gmail.com');

INSERT INTO users (email,password,role_id,status,full_name,phone_number,address,shop_name,logo_url,description,rating_average,bank_name,bank_account,bank_account_holder,achievement,create_at)
VALUES ('shop3@gmail.com','$2a$12$lU/EpH6mhkL4ERuMRO2cjeeT1CPHxqAyDHGwnDctIWzojS6k/oG/K',@shop_role_id,'PENDING','Phạm Bán Hàng','0944444445','Q.3, TP.HCM','Nông Sản Chờ Duyệt','https://res.cloudinary.com/dfp44sbsj/image/upload/v1774252337/brands/t9muel0yqmqfzfu3ndij.jpg','Shop mới tạo đang chờ Ban quản trị duyệt.',0.0,'Techcombank','19034567890124','PHAM BAN HANG','',NOW())
    ON DUPLICATE KEY UPDATE email=email;
SET @shop3_id=(SELECT id FROM users WHERE email='shop3@gmail.com');
INSERT INTO users (email,password,role_id,status,full_name,phone_number,address,description,create_at)
VALUES ('staff@gmail.com','$2a$12$lU/EpH6mhkL4ERuMRO2cjeeT1CPHxqAyDHGwnDctIWzojS6k/oG/K',@staff_role_id,'ACTIVE','Nhân viên ','0900000009','TP.HCM','Nhân viên kho',NOW())
    ON DUPLICATE KEY UPDATE email=email;
SET @staff_id=(SELECT id FROM users WHERE email='staff@gmail.com');

INSERT INTO users (email, password, role_id, status, full_name, phone_number, address, description, rating_average, license, vehicle_number, license_image_url, vehicle_doc_image_url, create_at, logo_url)
VALUES (
           'shipper3@gmail.com',
           '$2a$12$lU/EpH6mhkL4ERuMRO2cjeeT1CPHxqAyDHGwnDctIWzojS6k/oG/K',
           @shipper_role_id,
           'ACTIVE',
           'Lê Văn Nhanh',
           '0933333334',
           'Thủ Đức, TP.HCM',
           'Shipper chuyên nghiệp khu vực Thủ Đức',
           4.9,
           'B2-11223344',
           '59-X2 456.78',
           'https://res.cloudinary.com/dfp44sbsj/image/upload/v1774253418/brands/tftlykogelfaisztf7fk.jpg',
           'https://res.cloudinary.com/dfp44sbsj/image/upload/v1774253567/brands/cneiljr2dzws18oibzuq.jpg',
           NOW(),
           'https://res.cloudinary.com/dfp44sbsj/image/upload/v1774252921/brands/xu1pv9ty2njacb3bhozh.jpg'
       )
    ON DUPLICATE KEY UPDATE email = email;
SET @shipper3_id = (SELECT id FROM users WHERE email = 'shipper3@gmail.com');

-- ============================================================
-- 4. WALLETS
-- ============================================================
-- platform: 100M base + 225000 (os2 PAYOS confirmed) + 188000 (os4 PAYOS confirmed) = 100413000
-- frozen = 225000 + 188000 = 413000 (khóa tiền 2 đơn SHIPPING PAYOS chờ shipper giao)
INSERT INTO wallets (admin_id,status,total_balance,frozen_balance,total_revenue_all_time,total_withdrawn,commission_wallet,type)
VALUES (@admin_id,'ACTIVE',100413000,413000,0,0,19410,'PLATFORM');

-- shop1 frozen: #17(164700)+#18(189000)=353700 | realized: #3(181800)+#12(153000)+mystery(80100)=414900 | total=414900+353700-128000=640600
-- shop2 frozen: #23(155700)+#25(117000)=272700 | realized: #6(132300)+#19(181800)+#24(157500)+mystery2(134100)=605700 | total=605700+272700=878400
INSERT INTO wallets (shop_id,status,total_balance,frozen_balance,total_revenue_all_time,total_withdrawn,commission_wallet,type)
VALUES (@shop1_id,'ACTIVE',640600,353700,414900,128000,8040,'SHOP'),
       (@shop2_id,'ACTIVE',878400,272700,605700,0,11370,'SHOP'),
       (@shop3_id,'ACTIVE',0,0,0,0,0,'SHOP');

-- shipper1: 12 đơn DELIVERED × 15000 = 180000đ; đã rút 55000; đang chờ rút 50000
-- totalBalance = 180000 - 55000 = 125000; frozenBalance = 50000 (pending)
INSERT INTO wallets (shipper_id,status,total_balance,frozen_balance,total_revenue_all_time,total_withdrawn,commission_wallet,type)
VALUES (@shipper1_id,'ACTIVE',125000,50000,180000,55000,0,'SHIPPER'),
       (@shipper2_id,'ACTIVE',0,0,0,0,0,'SHIPPER');

SET @wallet_shop1_id=(SELECT wallet_id FROM wallets WHERE shop_id=@shop1_id);
SET @wallet_shop2_id=(SELECT wallet_id FROM wallets WHERE shop_id=@shop2_id);
SET @wallet_shop3_id=(SELECT wallet_id FROM wallets WHERE shop_id=@shop3_id);

-- shipper3: 4 đơn DELIVERED × 15000 = 60000đ; đã rút 30000
-- totalBalance = 60000 - 30000 = 30000
INSERT INTO wallets (shipper_id, status, total_balance, frozen_balance, total_revenue_all_time, total_withdrawn, commission_wallet, type)
VALUES (@shipper3_id, 'ACTIVE', 30000, 0, 60000, 30000, 0, 'SHIPPER');

SET @wallet_shipper1_id=(SELECT wallet_id FROM wallets WHERE shipper_id=@shipper1_id);
SET @wallet_shipper3_id=(SELECT wallet_id FROM wallets WHERE shipper_id=@shipper3_id);

-- ============================================================
-- 5. PRODUCTS
-- AVAILABLE: sản phẩm bình thường đang bán
-- PENDING:   sản phẩm mới đăng chờ admin duyệt
-- INACTIVE:  sản phẩm bị shop tạm ẩn / ngừng bán
-- OUT_OF_STOCK / EXPIRED: demo thêm nếu cần
-- ============================================================
INSERT INTO products (shop_owner_id,category_id,product_name,unit,selling_price,stock_quantity,description,status,image_url) VALUES
-- SHOP 1 - CỦ THỰC PHẨM (AVAILABLE)
(@shop1_id,@cat_cu_thuc_pham,'Cà rốt','kg',25000,100,'Cà rốt Đà Lạt tươi ngon, ngọt tự nhiên, giàu beta-carotene.','AVAILABLE','https://res.cloudinary.com/dfp44sbsj/image/upload/v1773916721/brands/uj1eoinkwvthfjvtpvpq.jpg'),
(@shop1_id,@cat_cu_thuc_pham,'Khoai tây','kg',30000,80,'Khoai tây Đà Lạt dẻo thơm, thích hợp chiên, hầm, nấu canh.','AVAILABLE','https://res.cloudinary.com/dfp44sbsj/image/upload/v1773916676/brands/bmgnazybi0rro8bnxhwp.jpg'),
(@shop1_id,@cat_cu_thuc_pham,'Khoai lang','kg',28000,70,'Khoai lang tím Đà Lạt ngọt bùi, giàu anthocyanin tốt cho tim mạch.','AVAILABLE','https://res.cloudinary.com/dfp44sbsj/image/upload/v1774249499/brands/pgdvlccom3lid5t24pnk.jpg'),
(@shop1_id,@cat_cu_thuc_pham,'Củ cải trắng','kg',18000,70,'Củ cải trắng tươi ngọt thanh, nấu canh hoặc muối dưa đều ngon.','AVAILABLE','https://res.cloudinary.com/dfp44sbsj/image/upload/v1773943219/brands/dysl261o6o2bapfwurmf.jpg'),
(@shop1_id,@cat_cu_thuc_pham,'Bí đỏ','kg',22000,60,'Bí đỏ bùi ngọt, giàu vitamin A, nấu canh hoặc hầm đều tuyệt.','AVAILABLE','https://res.cloudinary.com/dfp44sbsj/image/upload/v1773917463/brands/xjz74sedvolg7woxywpo.jpg'),
-- SHOP 1 - CỦ GIA VỊ (AVAILABLE)
(@shop1_id,@cat_cu_gia_vi,'Hành tây','kg',25000,80,'Hành tây tươi to tròn, thơm ngọt, dùng xào nấu hay ăn sống.','AVAILABLE','https://res.cloudinary.com/dfp44sbsj/image/upload/v1773943658/brands/qieeuppupbnypfoenpwg.jpg'),
(@shop1_id,@cat_cu_gia_vi,'Tỏi','kg',85000,35,'Tỏi ta Lý Sơn đậm vị thơm nồng, gia vị không thể thiếu.','AVAILABLE','https://res.cloudinary.com/dfp44sbsj/image/upload/v1773943632/brands/zhw2ww8qghsqscht29xb.jpg'),
(@shop1_id,@cat_cu_gia_vi,'Gừng','kg',55000,45,'Gừng tươi cay nồng ấm bụng, dùng nấu ăn, pha trà hay làm thuốc.','AVAILABLE','https://res.cloudinary.com/dfp44sbsj/image/upload/v1774249614/brands/ponetjgmfffbv8j8xpto.jpg'),
(@shop1_id,@cat_cu_gia_vi,'Nghệ','kg',40000,30,'Nghệ tươi vàng óng, kháng viêm tự nhiên, dùng nấu ăn và làm đẹp.','AVAILABLE','https://res.cloudinary.com/dfp44sbsj/image/upload/v1774249845/brands/xafrqajebqsjhwhramh3.jpg'),
(@shop1_id,@cat_cu_gia_vi,'Cần tây','bó',15000,40,'Cần tây xanh giòn, giàu chất xơ, tốt cho tiêu hóa và giảm cân.','AVAILABLE','https://res.cloudinary.com/dfp44sbsj/image/upload/v1774249561/brands/jtszr297g1cr4g25vqjj.webp'),
-- SHOP 1 - TRÁI CÂY QUANH NĂM (AVAILABLE)
(@shop1_id,@cat_trai_cay_quanh_nam,'Cà chua','kg',35000,50,'Cà chua đỏ tươi ngọt, giàu lycopene, ăn tươi hay nấu đều ngon.','AVAILABLE','https://res.cloudinary.com/dfp44sbsj/image/upload/v1773916530/brands/xjiiaehe5evwybwhmpmu.jpg'),
(@shop1_id,@cat_trai_cay_quanh_nam,'Dưa leo','kg',15000,100,'Dưa leo sạch giòn mát, thích hợp salad hoặc ăn kèm.','AVAILABLE','https://res.cloudinary.com/dfp44sbsj/image/upload/v1773917431/brands/yif2icgotbydg5qvzvbf.jpg'),
(@shop1_id,@cat_trai_cay_quanh_nam,'Ớt chuông','kg',55000,40,'Ớt chuông 3 màu giòn ngọt, giàu vitamin C.','AVAILABLE','https://res.cloudinary.com/dfp44sbsj/image/upload/v1774249920/brands/azkaanfoa7ommwnhxbv6.jpg'),
(@shop1_id,@cat_trai_cay_quanh_nam,'Táo','kg',110000,30,'Táo Fuji nhập khẩu giòn ngọt, vỏ đỏ đẹp, bảo quản tươi lâu.','AVAILABLE','https://images.unsplash.com/photo-1560806887-1e4cd0b6cbd6?w=600&h=400&fit=crop'),
(@shop1_id,@cat_trai_cay_quanh_nam,'Mướp','kg',18000,60,'Mướp hương tươi ngon, nấu canh hay xào đều thơm ngon.','AVAILABLE','https://res.cloudinary.com/dfp44sbsj/image/upload/v1774249989/brands/o7csibhzy57s2wpmcus9.jpg'),
-- SHOP 1 - TRÁI CÂY NHIỆT ĐỚI (AVAILABLE)
(@shop1_id,@cat_trai_cay_nhiet_doi,'Dâu tây','kg',180000,20,'Dâu tây Đà Lạt tươi chua ngọt, thu hoạch buổi sáng giao ngay.','AVAILABLE','https://res.cloudinary.com/dfp44sbsj/image/upload/v1773943061/brands/qtspypfmdjcxtsbme7xi.jpg'),
(@shop1_id,@cat_trai_cay_nhiet_doi,'Chuối','nải',25000,20,'Chuối sứ chín vàng thơm ngọt, giàu kali và năng lượng tự nhiên.','AVAILABLE','https://res.cloudinary.com/dfp44sbsj/image/upload/v1773916917/brands/n8ykdyjv59yavetdoj2l.jpg'),
-- SHOP 1 - PENDING (chờ admin duyệt)
(@shop1_id,@cat_cu_thuc_pham,'Khoai mỡ','kg',32000,50,'Khoai mỡ tím dẻo bùi, mới nhập từ vùng trồng Vĩnh Long, chờ kiểm duyệt.','PENDING','https://res.cloudinary.com/dfp44sbsj/image/upload/v1774251234/brands/vocydo64vc5ljrztscl8.jpg'),
(@shop1_id,@cat_trai_cay_quanh_nam,'Lê Hàn Quốc','kg',145000,15,'Lê Hàn Quốc nhập khẩu giòn ngọt, mới đăng bán chờ duyệt.','PENDING','https://images.unsplash.com/photo-1560806887-1e4cd0b6cbd6?w=600&h=400&fit=crop'),
(@shop1_id,@cat_cu_thuc_pham,'Khoai sọ Đà Lạt','kg',38000,25,'Khoai sọ Đà Lạt dẻo bùi, mới đăng chờ admin kiểm duyệt chất lượng.','PENDING','https://res.cloudinary.com/dfp44sbsj/image/upload/v1774249499/brands/pgdvlccom3lid5t24pnk.jpg'),
-- SHOP 1 - OUT_OF_STOCK (hết hàng, stock=0)
(@shop1_id,@cat_cu_thuc_pham,'Củ đậu','kg',20000,0,'Củ đậu giòn mát, hiện đã hết hàng, chờ nhập thêm.','OUT_OF_STOCK','https://res.cloudinary.com/dfp44sbsj/image/upload/v1773943219/brands/dysl261o6o2bapfwurmf.jpg'),
(@shop1_id,@cat_trai_cay_nhiet_doi,'Mận hậu','kg',95000,0,'Mận hậu Sơn La ngọt chua, hiện đã hết hàng theo mùa.','OUT_OF_STOCK','https://images.unsplash.com/photo-1464965911861-746a04b4bca6?w=400&h=300&fit=crop'),
(@shop1_id,@cat_cu_thuc_pham,'Củ năng','kg',22000,0,'Củ năng tươi giòn ngọt, hết hàng chờ mùa vụ mới.','OUT_OF_STOCK','https://res.cloudinary.com/dfp44sbsj/image/upload/v1773943711/brands/tjr6dujcccbiyqdpr4qk.jpg'),
-- SHOP 1 - INACTIVE (bị từ chối khi đăng bán)
(@shop1_id,@cat_cu_thuc_pham,'Củ cải đỏ','kg',35000,0,'Sản phẩm bị từ chối do thiếu thông tin nguồn gốc xuất xứ.','INACTIVE','https://res.cloudinary.com/dfp44sbsj/image/upload/v1773943219/brands/dysl261o6o2bapfwurmf.jpg'),
(@shop1_id,@cat_trai_cay_quanh_nam,'Dưa lưới','kg',75000,0,'Sản phẩm bị từ chối do hình ảnh không đạt tiêu chuẩn đăng bán.','INACTIVE','https://images.unsplash.com/photo-1619566636858-adf3ef46400b?w=400&h=300&fit=crop'),
(@shop1_id,@cat_cu_thuc_pham,'Khoai tây chiên sẵn','gói',45000,0,'Sản phẩm bị từ chối do không thuộc danh mục nông sản tươi.','INACTIVE','https://res.cloudinary.com/dfp44sbsj/image/upload/v1773916676/brands/bmgnazybi0rro8bnxhwp.jpg'),
-- SHOP 2 - CỦ THỰC PHẨM (AVAILABLE)
(@shop2_id,@cat_cu_thuc_pham,'Bí đao','kg',15000,60,'Bí đao tươi to, nấu canh giải nhiệt mùa hè rất mát.','AVAILABLE','https://res.cloudinary.com/dfp44sbsj/image/upload/v1774250920/brands/w0bf8nfrconotcqe8vec.jpg'),
(@shop2_id,@cat_cu_thuc_pham,'Bí ngòi','kg',18000,55,'Bí ngòi non tươi, xào nướng hay nấu canh đều ngon.','AVAILABLE','https://res.cloudinary.com/dfp44sbsj/image/upload/v1774250987/brands/z0qmi41ocaxbgaxa7kq1.jpg'),
(@shop2_id,@cat_cu_thuc_pham,'Su su','kg',20000,60,'Su su non tươi ngọt thanh, luộc hoặc xào đều giữ vị ngon.','AVAILABLE','https://res.cloudinary.com/dfp44sbsj/image/upload/v1773943711/brands/tjr6dujcccbiyqdpr4qk.jpg'),
(@shop2_id,@cat_cu_thuc_pham,'Cà tím','kg',22000,50,'Cà tím tươi bóng mướt, nướng mỡ hành hoặc hấp chấm nước mắm.','AVAILABLE','https://res.cloudinary.com/dfp44sbsj/image/upload/v1773943685/brands/sn8j7wqbpp6uhtitk5nf.jpg'),
(@shop2_id,@cat_cu_thuc_pham,'Khoai môn','kg',32000,40,'Khoai môn dẻo bùi thơm, nấu chè, hầm xương hoặc chiên đều ngon.','AVAILABLE','https://res.cloudinary.com/dfp44sbsj/image/upload/v1774251234/brands/vocydo64vc5ljrztscl8.jpg'),
-- SHOP 2 - CỦ GIA VỊ (AVAILABLE)
(@shop2_id,@cat_cu_gia_vi,'Hành tím','kg',45000,40,'Hành tím thơm nồng, gia vị không thể thiếu trong bếp Việt.','AVAILABLE','https://res.cloudinary.com/dfp44sbsj/image/upload/v1773943658/brands/qieeuppupbnypfoenpwg.jpg'),
(@shop2_id,@cat_cu_gia_vi,'Sả','bó',10000,80,'Sả tươi thơm nồng, dùng xào nướng, nấu lẩu hay đuổi muỗi.','AVAILABLE','https://res.cloudinary.com/dfp44sbsj/image/upload/v1774251166/brands/s5xw72wtr6ezrs1ykpzo.jpg'),
(@shop2_id,@cat_cu_gia_vi,'Ớt đỏ','kg',50000,35,'Ớt đỏ tươi cay nồng, dùng làm gia vị hay muối chua.','AVAILABLE','https://res.cloudinary.com/dfp44sbsj/image/upload/v1773943106/brands/eburdvo8w0tnequetwte.jpg'),
(@shop2_id,@cat_cu_gia_vi,'Tỏi tây','cây',15000,40,'Tỏi tây tươi to, vị nhẹ hơn tỏi thường, dùng xào hay nấu súp.','AVAILABLE','https://res.cloudinary.com/dfp44sbsj/image/upload/v1773943632/brands/zhw2ww8qghsqscht29xb.jpg'),
-- SHOP 2 - TRÁI CÂY NHIỆT ĐỚI (AVAILABLE)
(@shop2_id,@cat_trai_cay_nhiet_doi,'Dưa hấu','kg',12000,200,'Dưa hấu đỏ mọng nước, ngọt lịm, giải nhiệt mùa hè tuyệt vời.','AVAILABLE','https://res.cloudinary.com/dfp44sbsj/image/upload/v1773917373/brands/h0mivuwgipa6hssaawfi.jpg'),
(@shop2_id,@cat_trai_cay_nhiet_doi,'Thanh long','kg',35000,80,'Thanh long ruột đỏ Bình Thuận ngọt đậm, giàu chất chống oxy hóa.','AVAILABLE','https://res.cloudinary.com/dfp44sbsj/image/upload/v1773917308/brands/dfm7lmlxne7nztndgk0m.jpg'),
(@shop2_id,@cat_trai_cay_nhiet_doi,'Xoài','kg',85000,25,'Xoài cát Hòa Lộc thơm lừng vị ngọt đậm đà, đặc sản Tiền Giang.','AVAILABLE','https://images.unsplash.com/photo-1553279768-865429fa0078?w=600&h=400&fit=crop'),
(@shop2_id,@cat_trai_cay_nhiet_doi,'Ổi','kg',30000,50,'Ổi ruột đỏ thơm giòn ngọt, giàu vitamin C, ăn tươi hoặc ép nước.','AVAILABLE','https://res.cloudinary.com/dfp44sbsj/image/upload/v1773942953/brands/ohl0lsjzhcq6pmqp4qmu.jpg'),
-- SHOP 2 - TRÁI CÂY QUANH NĂM (AVAILABLE)
(@shop2_id,@cat_trai_cay_quanh_nam,'Cam','kg',35000,150,'Cam sành Tiền Giang mọng nước, ngọt thanh, giàu vitamin C.','AVAILABLE','https://res.cloudinary.com/dfp44sbsj/image/upload/v1773916481/brands/s5ld5232nl4s4fwczawr.jpg'),
(@shop2_id,@cat_trai_cay_quanh_nam,'Bưởi','kg',65000,50,'Bưởi da xanh Bến Tre ngọt thanh, múi to không hạt, bổ dưỡng.','AVAILABLE','https://res.cloudinary.com/dfp44sbsj/image/upload/v1773917344/brands/c2i9gkbkl8vx7iwulwsz.png'),
(@shop2_id,@cat_trai_cay_quanh_nam,'Nho','kg',120000,25,'Nho đen không hạt, ngọt đậm, giàu resveratrol tốt cho tim mạch.','AVAILABLE','https://res.cloudinary.com/dfp44sbsj/image/upload/v1773916419/brands/zdz1hvl9hcx6yyaq1wq3.jpg'),
-- SHOP 2 - PENDING (chờ admin duyệt)
(@shop2_id,@cat_trai_cay_nhiet_doi,'Sầu riêng Musang King','kg',320000,10,'Sầu riêng Musang King Malaysia nhập khẩu, mới đăng chờ duyệt.','PENDING','https://images.unsplash.com/photo-1553279768-865429fa0078?w=600&h=400&fit=crop'),
(@shop2_id,@cat_cu_thuc_pham,'Củ sen','kg',55000,20,'Củ sen tươi giòn ngọt, mới đăng bán chờ admin kiểm duyệt.','PENDING','https://res.cloudinary.com/dfp44sbsj/image/upload/v1774251234/brands/vocydo64vc5ljrztscl8.jpg'),
(@shop2_id,@cat_trai_cay_nhiet_doi,'Chôm chôm Java','kg',45000,30,'Chôm chôm Java ngọt thơm đặc sản miền Tây, chờ duyệt.','PENDING','https://images.unsplash.com/photo-1619566636858-adf3ef46400b?w=400&h=300&fit=crop'),
-- SHOP 2 - OUT_OF_STOCK (hết hàng, stock=0)
(@shop2_id,@cat_cu_thuc_pham,'Măng tươi','kg',25000,0,'Măng tươi Đà Lạt, hiện đã hết hàng chờ nhập thêm.','OUT_OF_STOCK','https://res.cloudinary.com/dfp44sbsj/image/upload/v1774250920/brands/w0bf8nfrconotcqe8vec.jpg'),
(@shop2_id,@cat_trai_cay_nhiet_doi,'Mít tố nữ','kg',45000,0,'Mít tố nữ miền Tây, hết hàng chưa vào mùa.','OUT_OF_STOCK','https://images.unsplash.com/photo-1619566636858-adf3ef46400b?w=400&h=300&fit=crop'),
(@shop2_id,@cat_trai_cay_nhiet_doi,'Vú sữa Lò Rèn','kg',85000,0,'Vú sữa Lò Rèn Vĩnh Long đặc sản, hết hàng theo mùa.','OUT_OF_STOCK','https://images.unsplash.com/photo-1553279768-865429fa0078?w=600&h=400&fit=crop'),
-- SHOP 2 - INACTIVE (bị từ chối khi đăng bán)
(@shop2_id,@cat_cu_thuc_pham,'Củ cải muối sẵn','hũ',35000,0,'Sản phẩm bị từ chối do là thực phẩm chế biến, không phải nông sản tươi.','INACTIVE','https://res.cloudinary.com/dfp44sbsj/image/upload/v1773943219/brands/dysl261o6o2bapfwurmf.jpg'),
(@shop2_id,@cat_trai_cay_quanh_nam,'Nước ép cam đóng chai','chai',25000,0,'Sản phẩm bị từ chối do không thuộc danh mục nông sản tươi.','INACTIVE','https://res.cloudinary.com/dfp44sbsj/image/upload/v1773916481/brands/s5ld5232nl4s4fwczawr.jpg'),
(@shop2_id,@cat_trai_cay_nhiet_doi,'Trái cây sấy khô','gói',65000,0,'Sản phẩm bị từ chối do thiếu giấy tờ kiểm định an toàn thực phẩm.','INACTIVE','https://images.unsplash.com/photo-1512621776951-a57141f2eefd?w=400&h=300&fit=crop');

-- ============================================================
-- 6. MYSTERY BOXES
-- ============================================================
INSERT INTO mystery_boxes (shop_owner_id,box_type,image_url,price,description,note,is_active,total_quantity,sold_quantity) VALUES
                                                                                                                               (@shop1_id,'Hộp Củ Bí Ẩn Đà Lạt','https://res.cloudinary.com/dfp44sbsj/image/upload/v1774253772/brands/sftnvmjw2mvym1plcpql.jpg',89000,'Hộp củ bí ẩn từ vườn organic Đà Lạt. Bên trong là 4-5 loại củ tươi ngon được chọn lọc kỹ càng.','Giao hàng trong ngày, bảo đảm tươi sạch',TRUE,20,5),
                                                                                                                               (@shop1_id,'Hộp Trái Cây Dinh Dưỡng','https://images.unsplash.com/photo-1619566636858-adf3ef46400b?w=400&h=300&fit=crop',129000,'Hộp trái cây bí ẩn gồm 3-4 loại quả tươi ngon, đầy đủ vitamin và khoáng chất.','Giá trị thực tế cao hơn 30% so với giá bán',TRUE,15,3),
                                                                                                                               (@shop2_id,'Hộp Củ Quả Miền Tây','https://images.unsplash.com/photo-1512621776951-a57141f2eefd?w=400&h=300&fit=crop',75000,'Hộp củ quả đặc sản miền Tây gồm 4-5 loại tươi xanh mát, thích hợp nấu canh và xào.','Thu hoạch buổi sáng, giao ngay trong ngày',TRUE,25,8),
                                                                                                                               (@shop2_id,'Hộp Trái Cây Nhiệt Đới','https://images.unsplash.com/photo-1553279768-865429fa0078?w=400&h=300&fit=crop',149000,'Hộp trái cây nhiệt đới đặc sản miền Nam gồm 4-5 loại trái cây ngọt ngon tươi mát.','Phù hợp giải nhiệt mùa hè',TRUE,15,4);

-- Product ID variables - shop1
SET @p_ca_rot    =(SELECT product_id FROM products WHERE product_name='Cà rốt'       AND shop_owner_id=@shop1_id);
SET @p_khoai_tay =(SELECT product_id FROM products WHERE product_name='Khoai tây'    AND shop_owner_id=@shop1_id);
SET @p_khoai_lang=(SELECT product_id FROM products WHERE product_name='Khoai lang'   AND shop_owner_id=@shop1_id);
SET @p_cu_cai    =(SELECT product_id FROM products WHERE product_name='Củ cải trắng' AND shop_owner_id=@shop1_id);
SET @p_bi_do     =(SELECT product_id FROM products WHERE product_name='Bí đỏ'        AND shop_owner_id=@shop1_id);
SET @p_hanh_tay  =(SELECT product_id FROM products WHERE product_name='Hành tây'     AND shop_owner_id=@shop1_id);
SET @p_toi       =(SELECT product_id FROM products WHERE product_name='Tỏi'          AND shop_owner_id=@shop1_id);
SET @p_gung      =(SELECT product_id FROM products WHERE product_name='Gừng'         AND shop_owner_id=@shop1_id);
SET @p_nghe      =(SELECT product_id FROM products WHERE product_name='Nghệ'         AND shop_owner_id=@shop1_id);
SET @p_can_tay   =(SELECT product_id FROM products WHERE product_name='Cần tây'      AND shop_owner_id=@shop1_id);
SET @p_ca_chua   =(SELECT product_id FROM products WHERE product_name='Cà chua'      AND shop_owner_id=@shop1_id);
SET @p_dua_leo   =(SELECT product_id FROM products WHERE product_name='Dưa leo'      AND shop_owner_id=@shop1_id);
SET @p_ot_chuong =(SELECT product_id FROM products WHERE product_name='Ớt chuông'    AND shop_owner_id=@shop1_id);
SET @p_dau_tay   =(SELECT product_id FROM products WHERE product_name='Dâu tây'      AND shop_owner_id=@shop1_id);
SET @p_chuoi     =(SELECT product_id FROM products WHERE product_name='Chuối'        AND shop_owner_id=@shop1_id);
SET @p_tao       =(SELECT product_id FROM products WHERE product_name='Táo'          AND shop_owner_id=@shop1_id);
SET @p_muop      =(SELECT product_id FROM products WHERE product_name='Mướp'         AND shop_owner_id=@shop1_id);
-- Product ID variables - shop2
SET @p2_bi_dao    =(SELECT product_id FROM products WHERE product_name='Bí đao'    AND shop_owner_id=@shop2_id);
SET @p2_bi_ngoi   =(SELECT product_id FROM products WHERE product_name='Bí ngòi'   AND shop_owner_id=@shop2_id);
SET @p2_su_su     =(SELECT product_id FROM products WHERE product_name='Su su'     AND shop_owner_id=@shop2_id);
SET @p2_ca_tim    =(SELECT product_id FROM products WHERE product_name='Cà tím'    AND shop_owner_id=@shop2_id);
SET @p2_hanh_tim  =(SELECT product_id FROM products WHERE product_name='Hành tím'  AND shop_owner_id=@shop2_id);
SET @p2_sa        =(SELECT product_id FROM products WHERE product_name='Sả'        AND shop_owner_id=@shop2_id);
SET @p2_khoai_mon =(SELECT product_id FROM products WHERE product_name='Khoai môn' AND shop_owner_id=@shop2_id);
SET @p2_ot_do     =(SELECT product_id FROM products WHERE product_name='Ớt đỏ'    AND shop_owner_id=@shop2_id);
SET @p2_toi_tay   =(SELECT product_id FROM products WHERE product_name='Tỏi tây'   AND shop_owner_id=@shop2_id);
SET @p2_dua_hau   =(SELECT product_id FROM products WHERE product_name='Dưa hấu'   AND shop_owner_id=@shop2_id);
SET @p2_thanh_long=(SELECT product_id FROM products WHERE product_name='Thanh long' AND shop_owner_id=@shop2_id);
SET @p2_xoai      =(SELECT product_id FROM products WHERE product_name='Xoài'      AND shop_owner_id=@shop2_id);
SET @p2_cam       =(SELECT product_id FROM products WHERE product_name='Cam'       AND shop_owner_id=@shop2_id);
SET @p2_buoi      =(SELECT product_id FROM products WHERE product_name='Bưởi'      AND shop_owner_id=@shop2_id);
SET @p2_oi        =(SELECT product_id FROM products WHERE product_name='Ổi'        AND shop_owner_id=@shop2_id);
SET @p2_nho       =(SELECT product_id FROM products WHERE product_name='Nho'       AND shop_owner_id=@shop2_id);

SET @box1_s1=(SELECT mystery_id FROM mystery_boxes WHERE box_type='Hộp Củ Bí Ẩn Đà Lạt'     AND shop_owner_id=@shop1_id);
SET @box2_s1=(SELECT mystery_id FROM mystery_boxes WHERE box_type='Hộp Trái Cây Dinh Dưỡng'  AND shop_owner_id=@shop1_id);
SET @box1_s2=(SELECT mystery_id FROM mystery_boxes WHERE box_type='Hộp Củ Quả Miền Tây'      AND shop_owner_id=@shop2_id);
SET @box2_s2=(SELECT mystery_id FROM mystery_boxes WHERE box_type='Hộp Trái Cây Nhiệt Đới'   AND shop_owner_id=@shop2_id);

INSERT INTO product_mystery (product_id,mystery_id,quantity) VALUES
                                                                 (@p_ca_rot,   @box1_s1,1),(@p_khoai_tay,@box1_s1,1),(@p_bi_do,@box1_s1,1),(@p_cu_cai,@box1_s1,1),
                                                                 (@p_dau_tay,  @box2_s1,1),(@p_tao,      @box2_s1,1),(@p_chuoi,@box2_s1,1),
                                                                 (@p2_bi_dao,  @box1_s2,1),(@p2_su_su,   @box1_s2,1),(@p2_ca_tim,@box1_s2,1),(@p2_khoai_mon,@box1_s2,1),
                                                                 (@p2_dua_hau, @box2_s2,2),(@p2_thanh_long,@box2_s2,1),(@p2_xoai,@box2_s2,1);

-- ============================================================
-- 6b. PRODUCT DISCOUNTS (demo: ~50% sản phẩm AVAILABLE có giảm giá)
-- Tất cả đang trong thời gian hiệu lực (active khi chạy demo)
-- ============================================================

-- SHOP 1 — có discount
UPDATE products SET discount_percent=15, discount_start_date='2026-06-01 00:00:00', discount_end_date='2026-06-30 23:59:59' WHERE product_id=@p_khoai_tay;
UPDATE products SET discount_percent=10, discount_start_date='2026-06-05 00:00:00', discount_end_date='2026-07-05 23:59:59' WHERE product_id=@p_cu_cai;
UPDATE products SET discount_percent=20, discount_start_date='2026-06-10 00:00:00', discount_end_date=NULL                   WHERE product_id=@p_gung;
UPDATE products SET discount_percent=25, discount_start_date='2026-06-15 00:00:00', discount_end_date='2026-07-15 23:59:59' WHERE product_id=@p_dau_tay;
UPDATE products SET discount_percent=12, discount_start_date='2026-06-01 00:00:00', discount_end_date='2026-07-31 23:59:59' WHERE product_id=@p_ca_chua;
UPDATE products SET discount_percent=18, discount_start_date='2026-06-08 00:00:00', discount_end_date='2026-07-08 23:59:59' WHERE product_id=@p_ot_chuong;
UPDATE products SET discount_percent=8,  discount_start_date='2026-06-12 00:00:00', discount_end_date=NULL                   WHERE product_id=@p_tao;

-- SHOP 1 — không discount (để NULL)
-- Cà rốt, Khoai lang, Bí đỏ, Hành tây, Tỏi, Nghệ, Cần tây, Dưa leo, Mướp, Chuối

-- SHOP 2 — có discount
UPDATE products SET discount_percent=10, discount_start_date='2026-06-03 00:00:00', discount_end_date='2026-06-30 23:59:59' WHERE product_id=@p2_bi_dao;
UPDATE products SET discount_percent=12, discount_start_date='2026-06-07 00:00:00', discount_end_date='2026-07-07 23:59:59' WHERE product_id=@p2_su_su;
UPDATE products SET discount_percent=15, discount_start_date='2026-06-10 00:00:00', discount_end_date=NULL                   WHERE product_id=@p2_hanh_tim;
UPDATE products SET discount_percent=20, discount_start_date='2026-06-01 00:00:00', discount_end_date='2026-07-20 23:59:59' WHERE product_id=@p2_thanh_long;
UPDATE products SET discount_percent=8,  discount_start_date='2026-06-14 00:00:00', discount_end_date='2026-07-14 23:59:59' WHERE product_id=@p2_oi;
UPDATE products SET discount_percent=22, discount_start_date='2026-06-05 00:00:00', discount_end_date=NULL                   WHERE product_id=@p2_nho;
UPDATE products SET discount_percent=5,  discount_start_date='2026-06-16 00:00:00', discount_end_date='2026-08-31 23:59:59' WHERE product_id=@p2_cam;

-- SHOP 2 — không discount
-- Bí ngòi, Cà tím, Khoai môn, Sả, Ớt đỏ, Tỏi tây, Dưa hấu, Xoài, Bưởi

-- ============================================================
-- 7. VOUCHERS
-- ============================================================
INSERT INTO vouchers (shop_id,voucher_code,discount_value,max_discount,min_order_value,usage_limit,expiry_date) VALUES
                                                                                                                    (@shop1_id,'NONGSAN10',10000,20000,50000,100,DATE_ADD(NOW(),INTERVAL 30 DAY)),
                                                                                                                    (@shop1_id,'ORGANIC20',20000,40000,100000,50,DATE_ADD(NOW(),INTERVAL 30 DAY)),
                                                                                                                    (@shop1_id,'DALAT50K',50000,50000,200000,20,DATE_ADD(NOW(),INTERVAL 15 DAY)),
                                                                                                                    (@shop1_id,'FREESHIP1',15000,15000,80000,200,DATE_ADD(NOW(),INTERVAL 60 DAY)),
                                                                                                                    (@shop2_id,'VUONTRAI15',15000,30000,70000,100,DATE_ADD(NOW(),INTERVAL 30 DAY)),
                                                                                                                    (@shop2_id,'MIENTAY25',25000,50000,120000,50,DATE_ADD(NOW(),INTERVAL 30 DAY)),
                                                                                                                    (@shop2_id,'TRAICAY30',30000,30000,150000,30,DATE_ADD(NOW(),INTERVAL 20 DAY)),
                                                                                                                    (@shop2_id,'SHIP0DONG',20000,20000,100000,150,DATE_ADD(NOW(),INTERVAL 45 DAY));

-- ============================================================
-- 8. ORDERS (25 đơn, xen kẽ status cách 30 phút, page 1 có đủ 6 status)
-- Thứ tự DESC: PENDING, FAILED, DELIVERED, CANCELLED, SHIPPING,
--              DELIVERED, PENDING, CANCELLED, CONFIRMED, FAILED,
--              SHIPPING, DELIVERED, CONFIRMED, CANCELLED, FAILED,
--              PENDING, CONFIRMED, SHIPPING, DELIVERED, CANCELLED,
--              PENDING, FAILED, SHIPPING, DELIVERED, CONFIRMED
-- ============================================================
ALTER TABLE orders
    MODIFY COLUMN status ENUM(
    'PENDING',
    'CONFIRMED',
    'SHIPPING',
    'SHIPPING_TO_WAREHOUSE',
    'ARRIVED_AT_WAREHOUSE',
    'QUALITY_CHECKING',
    'QUALITY_CHECKED',
    'QUALITY_APPROVED',
    'QUALITY_REJECTED',
    'READY',
    'DELIVERED',
    'CANCELLED',
    'PAID',
    'FAILED'
    ) NOT NULL;
-- Sync ENUM trackings.status với OrderStatus Java enum
ALTER TABLE trackings
    MODIFY COLUMN status ENUM(
    'PENDING',
    'CONFIRMED',
    'SHIPPING',
    'SHIPPING_TO_WAREHOUSE',
    'ARRIVED_AT_WAREHOUSE',
    'QUALITY_CHECKING',
    'QUALITY_CHECKED',
    'QUALITY_APPROVED',
    'QUALITY_REJECTED',
    'READY',
    'DELIVERED',
    'CANCELLED',
    'PAID',
    'FAILED'
    ) NULL;




INSERT INTO orders (buyer_id,shop_owner_id,shipper_id,shipping_fee,recipient_name,recipient_phone,shipping_address,shipping_latitude,shipping_longitude,shop_latitude,shop_longitude,note,status,total_amount,payment_method,created_at) VALUES
-- #1  -30m   PENDING
(@buyer1_id,@shop1_id,NULL,15000,'Ngô Gia Sang','0911111111','Đ. Võ Trường Toản, Linh Trung, Thủ Đức, TP.HCM',10.8651,106.7752,10.8697,106.7717,'Giao buổi sáng trước 10h','PENDING',95000,'COD',DATE_SUB(NOW(),INTERVAL 30 MINUTE)),
-- #2  -60m   FAILED
(@buyer1_id,@shop2_id,@shipper1_id,15000,'Ngô Gia Sang','0911111111','A15 Tiền Giang, Đông Hòa, Dĩ An, Bình Dương',10.8978,106.7731,10.9021,106.7698,'Không có nhà','FAILED',95000,'COD',DATE_SUB(NOW(),INTERVAL 60 MINUTE)),
-- #3  -90m   DELIVERED
(@buyer1_id,@shop1_id,@shipper1_id,15000,'Ngô Gia Sang','0911111111','Đ. Võ Trường Toản, Linh Trung, Thủ Đức, TP.HCM',10.8651,106.7752,10.8697,106.7717,'Củ rất tươi, sẽ mua lại','DELIVERED',217000,'PAYOS',DATE_SUB(NOW(),INTERVAL 90 MINUTE)),
-- #4  -120m  CANCELLED
(@buyer1_id,@shop2_id,NULL,15000,'Ngô Gia Sang','0911111111','A15 Tiền Giang, Đông Hòa, Dĩ An, Bình Dương',10.8978,106.7731,10.9021,106.7698,'Đặt nhầm sản phẩm','CANCELLED',75000,'COD',DATE_SUB(NOW(),INTERVAL 120 MINUTE)),
-- #5  -150m  SHIPPING
(@buyer1_id,@shop1_id,@shipper1_id,15000,'Ngô Gia Sang','0911111111','Đ. Võ Trường Toản, Linh Trung, Thủ Đức, TP.HCM',10.8651,106.7752,10.8697,106.7717,'Giao nhanh giúp mình','SHIPPING',135000,'COD',DATE_SUB(NOW(),INTERVAL 150 MINUTE)),
-- #6  -180m  DELIVERED
(@buyer1_id,@shop2_id,@shipper1_id,15000,'Ngô Gia Sang','0911111111','A15 Tiền Giang, Đông Hòa, Dĩ An, Bình Dương',10.8978,106.7731,10.9021,106.7698,NULL,'DELIVERED',162000,'COD',DATE_SUB(NOW(),INTERVAL 180 MINUTE)),
-- #7  -210m  PENDING
(@buyer1_id,@shop2_id,NULL,15000,'Ngô Gia Sang','0911111111','A15 Tiền Giang, Đông Hòa, Dĩ An, Bình Dương',10.8978,106.7731,10.9021,106.7698,'Gọi trước khi giao 30 phút','PENDING',87000,'COD',DATE_SUB(NOW(),INTERVAL 210 MINUTE)),
-- #8  -240m  CANCELLED
(@buyer1_id,@shop1_id,NULL,15000,'Ngô Gia Sang','0911111111','Đ. Võ Trường Toản, Linh Trung, Thủ Đức, TP.HCM',10.8651,106.7752,10.8697,106.7717,NULL,'CANCELLED',115000,'PAYOS',DATE_SUB(NOW(),INTERVAL 240 MINUTE)),
-- #9  -270m  CONFIRMED
(@buyer1_id,@shop1_id,NULL,15000,'Ngô Gia Sang','0911111111','Đ. Võ Trường Toản, Linh Trung, Thủ Đức, TP.HCM',10.8651,106.7752,10.8697,106.7717,'Củ tươi ngon nhé shop','CONFIRMED',112000,'COD',DATE_SUB(NOW(),INTERVAL 270 MINUTE)),
-- #10 -300m  FAILED
(@buyer1_id,@shop1_id,@shipper1_id,15000,'Ngô Gia Sang','0911111111','Đ. Võ Trường Toản, Linh Trung, Thủ Đức, TP.HCM',10.8651,106.7752,10.8697,106.7717,NULL,'FAILED',125000,'COD',DATE_SUB(NOW(),INTERVAL 300 MINUTE)),
-- #11 -330m  SHIPPING
(@buyer1_id,@shop2_id,@shipper1_id,15000,'Ngô Gia Sang','0911111111','A15 Tiền Giang, Đông Hòa, Dĩ An, Bình Dương',10.8978,106.7731,10.9021,106.7698,'Cẩn thận trái cây dễ dập','SHIPPING',172000,'COD',DATE_SUB(NOW(),INTERVAL 330 MINUTE)),
-- #12 -360m  DELIVERED
(@buyer1_id,@shop1_id,@shipper1_id,15000,'Ngô Gia Sang','0911111111','Đ. Võ Trường Toản, Linh Trung, Thủ Đức, TP.HCM',10.8651,106.7752,10.8697,106.7717,'Gừng nghệ tươi thơm lắm!','DELIVERED',185000,'COD',DATE_SUB(NOW(),INTERVAL 360 MINUTE)),
-- #13 -390m  CONFIRMED
(@buyer1_id,@shop2_id,NULL,15000,'Ngô Gia Sang','0911111111','A15 Tiền Giang, Đông Hòa, Dĩ An, Bình Dương',10.8978,106.7731,10.9021,106.7698,'Trái cây chín tự nhiên nhé','CONFIRMED',163000,'COD',DATE_SUB(NOW(),INTERVAL 390 MINUTE)),
-- #14 -420m  CANCELLED
(@buyer1_id,@shop2_id,NULL,15000,'Ngô Gia Sang','0911111111','A15 Tiền Giang, Đông Hòa, Dĩ An, Bình Dương',10.8978,106.7731,10.9021,106.7698,'Thay đổi kế hoạch','CANCELLED',92000,'COD',DATE_SUB(NOW(),INTERVAL 420 MINUTE)),
-- #15 -450m  FAILED
(@buyer1_id,@shop2_id,@shipper1_id,15000,'Ngô Gia Sang','0911111111','A15 Tiền Giang, Đông Hòa, Dĩ An, Bình Dương',10.8978,106.7731,10.9021,106.7698,'Địa chỉ không tìm được','FAILED',78000,'COD',DATE_SUB(NOW(),INTERVAL 450 MINUTE)),
-- #16 -480m  PENDING
(@buyer1_id,@shop1_id,NULL,15000,'Ngô Gia Sang','0911111111','Đ. Võ Trường Toản, Linh Trung, Thủ Đức, TP.HCM',10.8651,106.7752,10.8697,106.7717,'Để trước cửa nếu vắng nhà','PENDING',125000,'PAYOS',DATE_SUB(NOW(),INTERVAL 480 MINUTE)),
-- #17 -510m  CONFIRMED
(@buyer1_id,@shop1_id,NULL,15000,'Ngô Gia Sang','0911111111','Đ. Võ Trường Toản, Linh Trung, Thủ Đức, TP.HCM',10.8651,106.7752,10.8697,106.7717,NULL,'CONFIRMED',198000,'PAYOS',DATE_SUB(NOW(),INTERVAL 510 MINUTE)),
-- #18 -540m  SHIPPING
(@buyer1_id,@shop1_id,@shipper1_id,15000,'Ngô Gia Sang','0911111111','Đ. Võ Trường Toản, Linh Trung, Thủ Đức, TP.HCM',10.8651,106.7752,10.8697,106.7717,NULL,'SHIPPING',225000,'PAYOS',DATE_SUB(NOW(),INTERVAL 540 MINUTE)),
-- #19 -570m  DELIVERED
(@buyer1_id,@shop2_id,@shipper1_id,15000,'Ngô Gia Sang','0911111111','A15 Tiền Giang, Đông Hòa, Dĩ An, Bình Dương',10.8978,106.7731,10.9021,106.7698,NULL,'DELIVERED',217000,'PAYOS',DATE_SUB(NOW(),INTERVAL 570 MINUTE)),
-- #20 -600m  CANCELLED
(@buyer1_id,@shop1_id,NULL,15000,'Ngô Gia Sang','0911111111','Đ. Võ Trường Toản, Linh Trung, Thủ Đức, TP.HCM',10.8651,106.7752,10.8697,106.7717,'Hết tiền rồi hủy thôi','CANCELLED',138000,'COD',DATE_SUB(NOW(),INTERVAL 600 MINUTE)),
-- #21 -630m  PENDING
(@buyer1_id,@shop2_id,NULL,15000,'Ngô Gia Sang','0911111111','A15 Tiền Giang, Đông Hòa, Dĩ An, Bình Dương',10.8978,106.7731,10.9021,106.7698,NULL,'PENDING',155000,'PAYOS',DATE_SUB(NOW(),INTERVAL 630 MINUTE)),
-- #22 -660m  FAILED
(@buyer1_id,@shop1_id,@shipper1_id,15000,'Ngô Gia Sang','0911111111','Đ. Võ Trường Toản, Linh Trung, Thủ Đức, TP.HCM',10.8651,106.7752,10.8697,106.7717,'Khách từ chối nhận hàng','FAILED',145000,'PAYOS',DATE_SUB(NOW(),INTERVAL 660 MINUTE)),
-- #23 -690m  SHIPPING
(@buyer1_id,@shop2_id,@shipper1_id,15000,'Ngô Gia Sang','0911111111','A15 Tiền Giang, Đông Hòa, Dĩ An, Bình Dương',10.8978,106.7731,10.9021,106.7698,NULL,'SHIPPING',188000,'PAYOS',DATE_SUB(NOW(),INTERVAL 690 MINUTE)),
-- #24 -720m  DELIVERED
(@buyer1_id,@shop2_id,@shipper1_id,15000,'Ngô Gia Sang','0911111111','A15 Tiền Giang, Đông Hòa, Dĩ An, Bình Dương',10.8978,106.7731,10.9021,106.7698,'Nho ngọt, thanh long đẹp!','DELIVERED',190000,'PAYOS',DATE_SUB(NOW(),INTERVAL 720 MINUTE)),
-- #25 -750m  CONFIRMED
(@buyer1_id,@shop2_id,NULL,15000,'Ngô Gia Sang','0911111111','A15 Tiền Giang, Đông Hòa, Dĩ An, Bình Dương',10.8978,106.7731,10.9021,106.7698,NULL,'CONFIRMED',145000,'PAYOS',DATE_SUB(NOW(),INTERVAL 750 MINUTE));

-- Lấy ID theo status + shop
-- PENDING: #1(30m s1), #7(210m s2), #16(480m s1), #21(630m s2)
SET @op1=(SELECT order_id FROM orders WHERE status='PENDING' AND shop_owner_id=@shop1_id ORDER BY created_at DESC LIMIT 1);
SET @op2=(SELECT order_id FROM orders WHERE status='PENDING' AND shop_owner_id=@shop1_id ORDER BY created_at ASC  LIMIT 1);
SET @op3=(SELECT order_id FROM orders WHERE status='PENDING' AND shop_owner_id=@shop2_id ORDER BY created_at DESC LIMIT 1);
SET @op4=(SELECT order_id FROM orders WHERE status='PENDING' AND shop_owner_id=@shop2_id ORDER BY created_at ASC  LIMIT 1);
-- CONFIRMED: #9(270m s1), #13(390m s2), #17(510m s1), #25(750m s2)
SET @oc1=(SELECT order_id FROM orders WHERE status='CONFIRMED' AND shop_owner_id=@shop1_id ORDER BY created_at DESC LIMIT 1 OFFSET 1);
SET @oc2=(SELECT order_id FROM orders WHERE status='CONFIRMED' AND shop_owner_id=@shop1_id ORDER BY created_at DESC LIMIT 1);
SET @oc3=(SELECT order_id FROM orders WHERE status='CONFIRMED' AND shop_owner_id=@shop2_id ORDER BY created_at DESC LIMIT 1 OFFSET 1);
SET @oc4=(SELECT order_id FROM orders WHERE status='CONFIRMED' AND shop_owner_id=@shop2_id ORDER BY created_at DESC LIMIT 1);
-- SHIPPING: #5(150m s1), #11(330m s2), #18(540m s1), #23(690m s2)
SET @os1=(SELECT order_id FROM orders WHERE status='SHIPPING' AND shop_owner_id=@shop1_id ORDER BY created_at DESC LIMIT 1 OFFSET 1);
SET @os2=(SELECT order_id FROM orders WHERE status='SHIPPING' AND shop_owner_id=@shop1_id ORDER BY created_at DESC LIMIT 1);
SET @os3=(SELECT order_id FROM orders WHERE status='SHIPPING' AND shop_owner_id=@shop2_id ORDER BY created_at DESC LIMIT 1 OFFSET 1);
SET @os4=(SELECT order_id FROM orders WHERE status='SHIPPING' AND shop_owner_id=@shop2_id ORDER BY created_at DESC LIMIT 1);
-- DELIVERED: #3(90m s1), #6(180m s2), #12(360m s1), #19(570m s2), #24(720m s2)
SET @od1=(SELECT order_id FROM orders WHERE status='DELIVERED' AND shop_owner_id=@shop1_id ORDER BY created_at DESC LIMIT 1 OFFSET 1);
SET @od2=(SELECT order_id FROM orders WHERE status='DELIVERED' AND shop_owner_id=@shop1_id ORDER BY created_at DESC LIMIT 1);
SET @od3=(SELECT order_id FROM orders WHERE status='DELIVERED' AND shop_owner_id=@shop2_id ORDER BY created_at DESC LIMIT 1 OFFSET 2);
SET @od4=(SELECT order_id FROM orders WHERE status='DELIVERED' AND shop_owner_id=@shop2_id ORDER BY created_at DESC LIMIT 1 OFFSET 1);
SET @od5=(SELECT order_id FROM orders WHERE status='DELIVERED' AND shop_owner_id=@shop2_id ORDER BY created_at DESC LIMIT 1);
-- CANCELLED: #4(120m s2), #8(240m s1), #14(420m s2), #20(600m s1)
SET @oca1=(SELECT order_id FROM orders WHERE status='CANCELLED' AND shop_owner_id=@shop2_id ORDER BY created_at DESC LIMIT 1 OFFSET 1);
SET @oca2=(SELECT order_id FROM orders WHERE status='CANCELLED' AND shop_owner_id=@shop1_id ORDER BY created_at DESC LIMIT 1 OFFSET 1);
SET @oca3=(SELECT order_id FROM orders WHERE status='CANCELLED' AND shop_owner_id=@shop2_id ORDER BY created_at DESC LIMIT 1);
SET @oca4=(SELECT order_id FROM orders WHERE status='CANCELLED' AND shop_owner_id=@shop1_id ORDER BY created_at DESC LIMIT 1);
-- FAILED: #2(60m s2), #10(300m s1), #15(450m s2), #22(660m s1)
SET @of1=(SELECT order_id FROM orders WHERE status='FAILED' AND shop_owner_id=@shop2_id ORDER BY created_at DESC LIMIT 1 OFFSET 1);
SET @of2=(SELECT order_id FROM orders WHERE status='FAILED' AND shop_owner_id=@shop1_id ORDER BY created_at DESC LIMIT 1 OFFSET 1);
SET @of3=(SELECT order_id FROM orders WHERE status='FAILED' AND shop_owner_id=@shop2_id ORDER BY created_at DESC LIMIT 1);
SET @of4=(SELECT order_id FROM orders WHERE status='FAILED' AND shop_owner_id=@shop1_id ORDER BY created_at DESC LIMIT 1);

INSERT INTO order_details (order_id,product_id,quantity,unit_price,is_requested_return) VALUES
-- PENDING
(@op1,@p_ca_rot,3,25000,0),(@op1,@p_khoai_tay,2,30000,0),
(@op2,@p_bi_do,2,22000,0),(@op2,@p_hanh_tay,2,25000,0),(@op2,@p_toi,1,85000,0),
(@op3,@p2_bi_dao,3,15000,0),(@op3,@p2_su_su,2,20000,0),
(@op4,@p2_dua_hau,3,12000,0),(@op4,@p2_thanh_long,1,35000,0),(@op4,@p2_xoai,1,85000,0),
-- CONFIRMED
(@oc1,@p_khoai_lang,2,28000,0),(@oc1,@p_cu_cai,2,18000,0),(@oc1,@p_gung,1,55000,0),
(@oc2,@p_dau_tay,1,180000,0),(@oc2,@p_tao,1,110000,0),
(@oc3,@p2_cam,2,35000,0),(@oc3,@p2_buoi,1,65000,0),(@oc3,@p2_dua_hau,1,12000,0),
(@oc4,@p2_ca_tim,2,22000,0),(@oc4,@p2_hanh_tim,1,45000,0),
-- SHIPPING
(@os1,@p_ca_rot,3,25000,0),(@os1,@p_khoai_tay,2,30000,0),(@os1,@p_bi_do,1,22000,0),
(@os2,@p_ca_rot,2,25000,0),(@os2,@p_khoai_tay,2,30000,0),(@os2,@p_bi_do,2,22000,0),
(@os3,@p2_xoai,2,85000,0),(@os3,@p2_thanh_long,1,35000,0),
(@os4,@p2_cam,3,35000,0),(@os4,@p2_buoi,2,65000,0),
-- DELIVERED
(@od1,@p_ca_rot,2,25000,0),(@od1,@p_khoai_tay,1,30000,0),
(@od2,@p_dau_tay,1,180000,0),(@od2,@p_tao,1,110000,0),(@od2,@p_chuoi,1,25000,0),
(@od3,@p2_cam,2,35000,0),(@od3,@p2_dua_hau,3,12000,0),(@od3,@p2_thanh_long,1,35000,0),
(@od4,@p2_xoai,2,85000,0),(@od4,@p2_buoi,1,65000,0),(@od4,@p2_cam,1,35000,0),
(@od5,@p2_nho,1,120000,0),(@od5,@p2_thanh_long,2,35000,0),(@od5,@p2_oi,1,30000,0),
-- CANCELLED
(@oca1,@p2_cam,2,35000,0),(@oca1,@p2_dua_hau,1,12000,0),
(@oca2,@p_ca_rot,3,25000,0),(@oca2,@p_khoai_tay,2,30000,0),
(@oca3,@p2_xoai,1,85000,0),(@oca3,@p2_thanh_long,1,35000,0),
(@oca4,@p_bi_do,3,22000,0),(@oca4,@p_ca_rot,2,25000,0),
-- FAILED
(@of1,@p2_cam,1,35000,0),(@of1,@p2_thanh_long,1,35000,0),
(@of2,@p_ca_rot,4,25000,0),(@of2,@p_khoai_tay,2,30000,0),
(@of3,@p2_xoai,1,85000,0),(@of3,@p2_buoi,1,65000,0),
(@of4,@p_bi_do,3,22000,0),(@of4,@p_hanh_tay,2,25000,0);

INSERT IGNORE INTO shipper_locations (shipper_id,order_id,latitude,longitude,updated_at)
VALUES (@shipper1_id,@os1,10.8670,106.7730,NOW());

INSERT IGNORE INTO shipper_locations (shipper_id, order_id, latitude, longitude, updated_at)
VALUES (@shipper3_id, NULL, 10.8651, 106.7752, NOW());
-- ============================================================
-- 9. REVIEWS  (14 reviews, trải đều 2–5 sao để demo bộ lọc)
-- ============================================================
-- order_details đã DELIVERED – mỗi dòng 1 biến
SET @ood1 =(SELECT order_detail_id FROM order_details WHERE order_id=@od1 ORDER BY order_detail_id LIMIT 1);           -- cà rốt   (od1/shop1)
SET @ood2 =(SELECT order_detail_id FROM order_details WHERE order_id=@od1 ORDER BY order_detail_id LIMIT 1 OFFSET 1);  -- khoai tây (od1/shop1)
SET @ood3 =(SELECT order_detail_id FROM order_details WHERE order_id=@od2 ORDER BY order_detail_id LIMIT 1);           -- dâu tây   (od2/shop1)
SET @ood4 =(SELECT order_detail_id FROM order_details WHERE order_id=@od2 ORDER BY order_detail_id LIMIT 1 OFFSET 1);  -- táo       (od2/shop1)
SET @ood5 =(SELECT order_detail_id FROM order_details WHERE order_id=@od2 ORDER BY order_detail_id LIMIT 1 OFFSET 2);  -- chuối     (od2/shop1)
SET @ood6 =(SELECT order_detail_id FROM order_details WHERE order_id=@od3 ORDER BY order_detail_id LIMIT 1);           -- cam       (od3/shop2)
SET @ood7 =(SELECT order_detail_id FROM order_details WHERE order_id=@od3 ORDER BY order_detail_id LIMIT 1 OFFSET 1);  -- dưa hấu   (od3/shop2)
SET @ood8 =(SELECT order_detail_id FROM order_details WHERE order_id=@od3 ORDER BY order_detail_id LIMIT 1 OFFSET 2);  -- thanh long(od3/shop2)
SET @ood9 =(SELECT order_detail_id FROM order_details WHERE order_id=@od4 ORDER BY order_detail_id LIMIT 1);           -- xoài      (od4/shop2)
SET @ood10=(SELECT order_detail_id FROM order_details WHERE order_id=@od4 ORDER BY order_detail_id LIMIT 1 OFFSET 1);  -- bưởi      (od4/shop2)
SET @ood11=(SELECT order_detail_id FROM order_details WHERE order_id=@od4 ORDER BY order_detail_id LIMIT 1 OFFSET 2);  -- cam       (od4/shop2)
SET @ood12=(SELECT order_detail_id FROM order_details WHERE order_id=@od5 ORDER BY order_detail_id LIMIT 1);           -- nho       (od5/shop2)
SET @ood13=(SELECT order_detail_id FROM order_details WHERE order_id=@od5 ORDER BY order_detail_id LIMIT 1 OFFSET 1);  -- thanh long(od5/shop2)
SET @ood14=(SELECT order_detail_id FROM order_details WHERE order_id=@od5 ORDER BY order_detail_id LIMIT 1 OFFSET 2);  -- ổi        (od5/shop2)

INSERT INTO reviews (buyer_id,shop_owner_id,order_detail_id,rating_star,comment,evidence,reply_from_shop) VALUES
-- ★★★★★ 5 sao
(@buyer1_id,@shop1_id,@ood1,5.0,'Cà rốt rất tươi, to đều, ngọt tự nhiên! Đóng gói cẩn thận, giao nhanh. Sẽ ủng hộ thường xuyên!','https://images.unsplash.com/photo-1540420773420-3366772f4999?w=400&h=300&fit=crop','Cảm ơn bạn đã tin tưởng Nông Sản Xấu Mã! 🥕'),
(@buyer1_id,@shop1_id,@ood3,5.0,'Dâu tây Đà Lạt chua ngọt hoàn hảo, hái buổi sáng giao trưa vẫn còn tươi nguyên! Sẽ đặt lại.','https://images.unsplash.com/photo-1464965911861-746a04b4bca6?w=400&h=300&fit=crop','Dâu tây nhập trực tiếp từ Đà Lạt mỗi sáng sớm 🍓'),
(@buyer1_id,@shop2_id,@ood12,5.0,'Nho đen không hạt ngọt đậm, bưởi múi to mọng nước, xoài thơm lừng. Combo hoàn hảo!','https://images.unsplash.com/photo-1553279768-865429fa0078?w=400&h=300&fit=crop','Đây đều là đặc sản miền Tây chính gốc, cảm ơn bạn rất nhiều! 🌟'),
-- ★★★★ 4 sao
(@buyer1_id,@shop1_id,@ood2,4.0,'Khoai tây Đà Lạt dẻo thơm, bí đỏ ngọt bùi. Hài lòng với chất lượng!',NULL,'Cảm ơn bạn! Chúng tôi sẽ cố gắng hoàn thiện hơn mỗi ngày 🌿'),
(@buyer1_id,@shop1_id,@ood4,4.0,'Táo Fuji giòn ngọt, vỏ đỏ đẹp. Tuy nhiên một vài quả hơi nhỏ hơn hình. Nhìn chung ổn.',NULL,'Cảm ơn bạn đã phản hồi! Lần sau sẽ chọn quả to đều hơn cho bạn 🍎'),
(@buyer1_id,@shop2_id,@ood9,4.0,'Xoài cát Hòa Lộc thơm, ngọt vừa. Giao hàng cẩn thận không bị dập. Sẽ đặt lại.',NULL,'Xoài cát Hòa Lộc là đặc sản Tiền Giang, cảm ơn bạn ủng hộ! 🥭'),
-- ★★★ 3 sao
(@buyer1_id,@shop2_id,@ood7,3.0,'Dưa hấu tươi nhưng vị hơi nhạt, không đỏ ruột như hình mô tả. Chấp nhận được với mức giá này.',NULL,'Xin lỗi vì trải nghiệm chưa tốt! Dưa hấu phụ thuộc mùa vụ, chúng tôi sẽ cải thiện 🍉'),
(@buyer1_id,@shop2_id,@ood10,3.0,'Bưởi da xanh múi to nhưng hơi đắng ở phần gần vỏ. Ăn được nhưng chưa ngọt như kỳ vọng.',NULL,'Cảm ơn bạn đã phản hồi chân thực! Bưởi tươi gần thu hoạch nên vị hay đổi 🍋'),
(@buyer1_id,@shop2_id,@ood11,3.0,'Cam sành tươi, nước nhiều, nhưng độ ngọt không đồng đều, một số quả khá chua.',NULL,'Xin lỗi bạn! Cam sành vụ này ít nắng nên vị không đồng đều, sẽ cải thiện 🍊'),
(@buyer1_id,@shop2_id,@ood14,3.0,'Ổi ruột đỏ còn khá cứng, chưa chín đều. Phải để thêm 1-2 ngày mới ăn được.',NULL,'Xin lỗi bạn! Chúng tôi sẽ chú ý độ chín hơn khi đóng gói 🍐'),
-- ★★ 2 sao
(@buyer1_id,@shop1_id,@ood5,2.0,'Chuối giao đến bị dập nhiều, một nửa đã thâm đen. Không được như kỳ vọng với mức giá này.','https://images.unsplash.com/photo-1571771894821-ce9b6c11b08e?w=400&h=300&fit=crop','Rất xin lỗi bạn! Đây là sự cố đóng gói, chúng tôi sẽ hoàn tiền và cải thiện ngay 🙏'),
(@buyer1_id,@shop2_id,@ood6,2.0,'Cam sành mua về chua lét, không ngọt gì cả. Mô tả "ngọt thanh" nhưng thực tế không đúng. Thất vọng.','https://images.unsplash.com/photo-1547036967-23d11aacaee0?w=400&h=300&fit=crop','Thành thật xin lỗi bạn! Lô cam này có vấn đề, chúng tôi sẽ liên hệ bù hàng cho bạn 🙏'),
(@buyer1_id,@shop2_id,@ood8,2.0,'Thanh long ruột đỏ nhưng gần nửa số quả đã bị nhũn khi nhận. Đóng gói quá kém, lãng phí tiền.',NULL,'Rất xin lỗi! Đây là sự cố vận chuyển, chúng tôi cam kết cải thiện khâu đóng gói 🙏'),
(@buyer1_id,@shop2_id,@ood13,2.0,'Thanh long nhận về 5 quả thì 3 quả đã nhũn, không ăn được. Shipper giao quá lâu hay do đóng gói kém?',NULL,'Xin lỗi bạn rất nhiều! Chúng tôi sẽ hoàn tiền 3 quả bị hỏng và cải thiện khâu đóng gói ngay 🙏');

-- Cập nhật rating shop theo trung bình thực tế
-- Shop1: (5+5+5+4+4+4+2) / 7 ≈ 4.1
-- Shop2: (4+4+3+3+3+3+2+2+2+2) / 10 = 2.8 → 2.8 (thể hiện sự khác biệt rõ cho demo)
UPDATE users SET rating_average=4.1 WHERE id=@shop1_id;
UPDATE users SET rating_average=2.8 WHERE id=@shop2_id;

-- ============================================================
-- 10. PAYMENTS
-- ============================================================
INSERT INTO payments (order_id,payment_date,amount,payment_gateway,status,created_at,updated_at,payos_order_code) VALUES
(@od2,DATE_SUB(NOW(),INTERVAL 2 DAY),217000,'PAYOS','SUCCESS',DATE_SUB(NOW(),INTERVAL 2 DAY),DATE_SUB(NOW(),INTERVAL 2 DAY),'FOOD20240001'),
(@od4,DATE_SUB(NOW(),INTERVAL 4 DAY),217000,'PAYOS','SUCCESS',DATE_SUB(NOW(),INTERVAL 4 DAY),DATE_SUB(NOW(),INTERVAL 4 DAY),'FOOD20240002'),
(@os2,DATE_SUB(NOW(),INTERVAL 3 HOUR),225000,'PAYOS','SUCCESS',DATE_SUB(NOW(),INTERVAL 4 HOUR),DATE_SUB(NOW(),INTERVAL 3 HOUR),'FOOD20240003'),
(@os4,DATE_SUB(NOW(),INTERVAL 90 MINUTE),188000,'PAYOS','SUCCESS',DATE_SUB(NOW(),INTERVAL 2 HOUR),DATE_SUB(NOW(),INTERVAL 90 MINUTE),'FOOD20240004'),
(@op2,NULL,125000,'PAYOS','PENDING',DATE_SUB(NOW(),INTERVAL 1 HOUR),NOW(),'FOOD20240005');

SET @payment_success1=(SELECT payment_id FROM payments WHERE payos_order_code='FOOD20240001' LIMIT 1);
SET @payment_success2=(SELECT payment_id FROM payments WHERE payos_order_code='FOOD20240002' LIMIT 1);
SET @payment_success3=(SELECT payment_id FROM payments WHERE payos_order_code='FOOD20240003' LIMIT 1);
SET @payment_success4=(SELECT payment_id FROM payments WHERE payos_order_code='FOOD20240004' LIMIT 1);

-- ============================================================
-- 11. WITHDRAW REQUESTS
-- ============================================================
INSERT INTO withdraw_requests (admin_id,wallet_id,reason,admin_note,created_at,processed_at,status,amount,fee,receive_amount,bank_name,bank_account_number,bank_account_holder,payout_provider,payout_status) VALUES
                                                                                                                                                                                                                  (@admin_id,@wallet_shop1_id,'Rút tiền doanh thu tháng này','Đã xác nhận, đang xử lý',DATE_SUB(NOW(),INTERVAL 1 DAY),DATE_SUB(NOW(),INTERVAL 12 HOUR),'APPROVED',100000,2500,97500,'Vietcombank','0071000123456','TRAN VAN KIET','PAYOS','PENDING'),
                                                                                                                                                                                                                  (@admin_id,@wallet_shop2_id,'Rút tiền tuần',NULL,DATE_SUB(NOW(),INTERVAL 2 HOUR),NULL,'PENDING',150000,3750,146250,'Techcombank','19034567890123','LE THI NGOC','PAYOS','CREATED'),
                                                                                                                                                                                                                  (@admin_id,@wallet_shop1_id,'Rút tiền tuần trước','Đã chuyển khoản thành công',DATE_SUB(NOW(),INTERVAL 7 DAY),DATE_SUB(NOW(),INTERVAL 6 DAY),'SUCCESS',128000,3200,124800,'Vietcombank','0071000123456','TRAN VAN KIET','PAYOS','SUCCESS');

-- Shipper rút tiền
INSERT INTO withdraw_requests (admin_id,wallet_id,reason,admin_note,created_at,processed_at,status,amount,fee,receive_amount,bank_name,bank_account_number,bank_account_holder,payout_provider,payout_status) VALUES
                                                                                                                                                                                                                  (@admin_id,@wallet_shipper1_id,'Rút lương tuần 1','Đã chuyển khoản thành công',DATE_SUB(NOW(),INTERVAL 10 DAY),DATE_SUB(NOW(),INTERVAL 9 DAY),'SUCCESS',55000,0,55000,'Vietcombank','0071000999888','NGUYEN VAN HOANG','PAYOS','SUCCESS'),
                                                                                                                                                                                                                  (@admin_id,@wallet_shipper1_id,'Rút tiền cuối tháng',NULL,DATE_SUB(NOW(),INTERVAL 1 DAY),NULL,'PENDING',50000,0,50000,'Vietcombank','0071000999888','NGUYEN VAN HOANG','PAYOS','PENDING'),
                                                                                                                                                                                                                  (@admin_id,@wallet_shipper3_id,'Rút lương tháng trước','Đã chuyển khoản thành công',DATE_SUB(NOW(),INTERVAL 15 DAY),DATE_SUB(NOW(),INTERVAL 14 DAY),'SUCCESS',30000,0,30000,'MB Bank','0987654321098','PHAM THI BICH THUY','PAYOS','SUCCESS');

-- ============================================================
-- 12. NOTIFICATIONS
-- ============================================================
INSERT INTO notifications (admin_id,title,message,evidence,create_at,receiver_type) VALUES
                                                                                        (@admin_id,'🌿 Chào mừng đến Food Market!','Chào mừng bạn đến với Food Market - nền tảng mua sắm củ quả sạch hàng đầu! Khám phá hàng trăm sản phẩm tươi ngon từ các vườn organic uy tín.',NULL,NOW(),'BUYER'),
                                                                                        (@admin_id,'🎉 Khuyến mãi tháng này - Giảm 20% củ quả','Tháng này Food Market có chương trình khuyến mãi đặc biệt! Giảm 20% tất cả sản phẩm củ quả khi đặt đơn từ 100.000đ. Đừng bỏ lỡ!','https://images.unsplash.com/photo-1540420773420-3366772f4999?w=800&h=400&fit=crop',DATE_SUB(NOW(),INTERVAL 1 DAY),'BUYER'),
                                                                                        (@admin_id,'📦 Cập nhật chính sách giao hàng','Food Market vừa cập nhật: Giao hàng trong 2-4 giờ tại TP.HCM và Bình Dương.',NULL,DATE_SUB(NOW(),INTERVAL 2 DAY),'BUYER,SHOP_OWNER'),
                                                                                        (@admin_id,'🏪 Thông báo Shop: Cập nhật quy định bán hàng','Kính gửi các Shop, vui lòng cập nhật thông tin sản phẩm đầy đủ để tăng tỷ lệ chuyển đổi đơn hàng.',NULL,DATE_SUB(NOW(),INTERVAL 3 DAY),'SHOP_OWNER'),
                                                                                        (@admin_id,'🚚 Thông báo Shipper: Khu vực giao hàng mới','Food Market mở rộng sang Bình Dương và Đồng Nai. Shipper đăng ký khu vực mới được phụ cấp thêm 5.000đ/đơn.',NULL,DATE_SUB(NOW(),INTERVAL 4 DAY),'SHIPPER'),
                                                                                        (@admin_id,'⭐ Hệ thống đánh giá mới','Food Market vừa ra mắt hệ thống đánh giá mới! Shop có rating cao sẽ được hiển thị ưu tiên trên trang chủ.',NULL,DATE_SUB(NOW(),INTERVAL 5 DAY),'BUYER,SHOP_OWNER');

SET @n1=(SELECT not_id FROM notifications WHERE title LIKE '%Chào mừng%' LIMIT 1);
SET @n2=(SELECT not_id FROM notifications WHERE title LIKE '%Khuyến mãi%' LIMIT 1);
SET @n3=(SELECT not_id FROM notifications WHERE title LIKE '%giao hàng%' LIMIT 1);
SET @n4=(SELECT not_id FROM notifications WHERE title LIKE '%quy định%' LIMIT 1);
SET @n5=(SELECT not_id FROM notifications WHERE title LIKE '%Shipper%' LIMIT 1);
SET @n6=(SELECT not_id FROM notifications WHERE title LIKE '%đánh giá%' LIMIT 1);

INSERT INTO user_notifications (user_id,notification_id,is_read,is_deleted) VALUES
                                                                                (@buyer1_id,@n1,TRUE,FALSE),(@buyer1_id,@n2,FALSE,FALSE),(@buyer1_id,@n3,FALSE,FALSE),(@buyer1_id,@n6,FALSE,FALSE),
                                                                                (@shop1_id,@n3,TRUE,FALSE),(@shop1_id,@n4,FALSE,FALSE),(@shop1_id,@n6,FALSE,FALSE),
                                                                                (@shop2_id,@n3,TRUE,FALSE),(@shop2_id,@n4,FALSE,FALSE),
                                                                                (@shipper1_id,@n5,FALSE,FALSE);

-- ============================================================
-- 13. BLOGS
-- category phải khớp với APP filter (toUpperCase):
--   XU HƯỚNG  → XU_HUONG  | NHÀ NÔNG → NHA_NONG
--   SỨC KHỎE  → SUC_KHOE  | CẨM NANG → CAM_NANG
-- ============================================================
INSERT INTO blogs (admin_id,title,content,picture_url,status,category,create_at) VALUES

-- ── XU HƯỚNG ─────────────────────────────────────────────
(@admin_id,
 'Trái cây miền Nam theo mùa - Nên mua gì?',
 'Mùa hè là thời điểm vàng của trái cây Nam Bộ:\n- Xoài cát Hòa Lộc: ngọt đậm thơm lừng\n- Thanh long ruột đỏ: mọng nước bổ dưỡng\n- Bưởi da xanh: thanh ngọt không hạt\n- Ổi ruột đỏ: giòn ngọt vitamin C cao',
 'https://res.cloudinary.com/dfp44sbsj/image/upload/v1773921072/brands/zhzaxj3de5vhhuw2ankp.jpg',
 'PUBLISHED','XU_HUONG',DATE_SUB(NOW(),INTERVAL 1 DAY)),

(@admin_id,
 'Xu hướng mua nông sản online năm 2026',
 'Thị trường thương mại điện tử nông sản Việt Nam đang bùng nổ:\n\n1. Nông sản hữu cơ (organic) tăng 45% so với năm trước.\n2. Người tiêu dùng ưu tiên sản phẩm có tem VietGAP, GlobalGAP.\n3. Giao hàng trong ngày (same-day delivery) trở thành tiêu chuẩn.\n4. Review video ngắn trên mạng xã hội thúc đẩy quyết định mua.\n5. Combo rau củ theo tuần ngày càng được yêu thích.',
 'https://images.unsplash.com/photo-1488459716781-31db52582fe9?w=800&h=400&fit=crop',
 'PUBLISHED','XU_HUONG',DATE_SUB(NOW(),INTERVAL 3 DAY)),

-- ── NHÀ NÔNG ─────────────────────────────────────────────
(@admin_id,
 'Bí quyết trồng rau sạch tại nhà dễ dàng',
 'Trồng rau tại nhà không cần nhiều diện tích:\n\n1. Chọn chậu sâu ít nhất 20cm để rễ phát triển tốt.\n2. Dùng đất trộn xơ dừa + phân hữu cơ tỷ lệ 2:1.\n3. Tưới nước buổi sáng sớm, tránh tưới buổi trưa nắng.\n4. Bón phân hữu cơ 2 tuần/lần thay vì phân hóa học.\n5. Rau muống, cải xanh, rau húng dễ trồng nhất cho người mới.\n\nKhởi đầu với 3-4 loại rau là đủ, đừng trồng quá nhiều cùng lúc!',
 'https://images.unsplash.com/photo-1416879595882-3373a0480b5b?w=800&h=400&fit=crop',
 'PUBLISHED','NHA_NONG',DATE_SUB(NOW(),INTERVAL 2 DAY)),

(@admin_id,
 'Nông dân Đà Lạt chia sẻ bí kíp trồng rau',
 'Anh Nguyễn Văn Minh, nông dân Đà Lạt 20 năm kinh nghiệm chia sẻ:\n\n"Bí quyết lớn nhất là hiểu đất và nghe cây nói. Khi lá vàng nhạt là thiếu đạm, lá xoăn là thiếu nước, lá cháy viền là bón phân quá nhiều."\n\nKinh nghiệm thực tế:\n- Luân canh 3 vụ/năm để đất không bị bạc màu\n- Tủ rơm giữ ẩm giảm tưới 30-40%\n- Trồng hoa cúc vạn thọ xung quanh để xua đuổi sâu bọ tự nhiên\n- Bẫy côn trùng bằng bẫy dính vàng thay thuốc trừ sâu',
 'https://images.unsplash.com/photo-1560493676-04071c5f467b?w=800&h=400&fit=crop',
 'PUBLISHED','NHA_NONG',DATE_SUB(NOW(),INTERVAL 5 DAY)),

-- ── SỨC KHỎE ─────────────────────────────────────────────
(@admin_id,
 'Thực đơn detox củ quả 7 ngày cho người bận rộn',
 'Detox bằng củ quả không cần phức tạp:\n- Sáng: Nước ép cà rốt + táo + gừng\n- Trưa: Salad dưa leo + cà chua + ớt chuông\n- Tối: Canh bí đỏ hầm khoai tây\n\nLợi ích sau 7 ngày detox:\n1. Da sáng hơn, bớt mụn\n2. Tiêu hóa cải thiện rõ rệt\n3. Cơ thể nhẹ nhàng hơn 1-2kg\n4. Giấc ngủ sâu hơn\n\nLưu ý: Uống đủ 2 lít nước mỗi ngày trong suốt quá trình detox.',
 'https://res.cloudinary.com/dfp44sbsj/image/upload/v1773920858/brands/a23bsuby6hhu2ty2r3bb.jpg',
 'PUBLISHED','SUC_KHOE',NOW()),

(@admin_id,
 'Ăn rau củ đúng cách giúp tăng sức đề kháng',
 'Rau củ là "vũ khí" tự nhiên tăng miễn dịch:\n\nVitamin C (ổi, ớt chuông, bưởi): tăng sản sinh bạch cầu, bảo vệ tế bào.\nBeta-carotene (cà rốt, bí đỏ, khoai lang): chống oxy hóa mạnh mẽ.\nZinc (tỏi, hành tây): kháng khuẩn, kháng virus hiệu quả.\n\nMẹo ăn đúng cách:\n- Nấu chín vừa tới, không nhừ quá - giảm vitamin\n- Ăn đa dạng màu sắc mỗi ngày (xanh, đỏ, vàng, tím)\n- Kết hợp rau sống và rau chín để hấp thụ tối đa dinh dưỡng\n- Ăn tỏi sống sau khi để 10 phút cắt/đập cho allicin phát huy tác dụng',
 'https://images.unsplash.com/photo-1512621776951-a57141f2eefd?w=800&h=400&fit=crop',
 'PUBLISHED','SUC_KHOE',DATE_SUB(NOW(),INTERVAL 4 DAY)),

-- ── CẨM NANG ─────────────────────────────────────────────
(@admin_id,
 'Mẹo chọn củ quả tươi ngon không hóa chất',
 'Việc lựa chọn củ quả sạch là ưu tiên hàng đầu.\n\n1. Màu sắc tự nhiên, không bóng bẩy quá mức.\n2. Củ chắc tay, không bị mềm hay có mùi lạ.\n3. Vết xước nhỏ là dấu hiệu ít thuốc BVTV.\n4. Ngửi thấy mùi tự nhiên đặc trưng.\n\nDấu hiệu nhận biết hàng kém chất lượng:\n- Màu sắc đồng đều bất thường, quá đẹp\n- Để lâu không héo - có thể bảo quản hóa chất\n- Mùi hăng nhẹ, không phải mùi đất tự nhiên\n- Bề mặt trơn bóng, không có "lông" tự nhiên',
 'https://images.unsplash.com/photo-1542838132-92c53300491e?w=800&h=400&fit=crop',
 'PUBLISHED','CAM_NANG',DATE_SUB(NOW(),INTERVAL 6 DAY)),

(@admin_id,
 'Cách bảo quản rau củ tươi lâu trong tủ lạnh',
 'Bảo quản đúng cách giúp rau củ tươi lâu gấp đôi:\n\nRau lá (cải, rau muống, xà lách):\n- Bọc giấy báo ẩm, cho vào túi zipper, ngăn mát 3-5 ngày\n\nCủ (cà rốt, khoai tây, cải củ):\n- Không rửa trước khi cất, bọc túi lưới ngăn mát 1-2 tuần\n- Khoai tây: để nơi tối, thoáng, không cần tủ lạnh\n\nTrái cây:\n- Chưa chín: để nhiệt độ phòng\n- Đã chín (chuối, xoài, ổi): ngăn mát 3-5 ngày\n- Trái cây chua (cam, bưởi, thanh long): ngăn mát 7-10 ngày\n\nMẹo: Tách riêng chuối ra khỏi trái cây khác vì chuối thải ethylene làm trái chín nhanh hơn.',
 'https://images.unsplash.com/photo-1490818387583-1baba5e638af?w=800&h=400&fit=crop',
 'PUBLISHED','CAM_NANG',DATE_SUB(NOW(),INTERVAL 8 DAY));

-- ============================================================
-- 14. BOT KNOWLEDGE
-- ============================================================
INSERT INTO bot_knowledge (type,title,content,keywords,active) VALUES
                                                                   ('POLICY','Chính sách giao hàng',
                                                                    'Food Market hỗ trợ giao hàng toàn quốc.\n- Nội thành TP.HCM và Bình Dương: giao trong 2-4 giờ.\n- Tỉnh thành khác: 1-3 ngày làm việc.\n- Phí giao hàng: 15.000đ - 30.000đ tùy khu vực.',
                                                                    'giao hàng, ship, phí ship, vận chuyển, bao lâu',TRUE),
                                                                   ('POLICY','Chính sách đổi trả và hoàn tiền',
                                                                    'Food Market cam kết chất lượng sản phẩm:\n- Đổi trả miễn phí trong 24 giờ nếu không tươi, dập nát hoặc không đúng mô tả.\n- Hoàn tiền 100% nếu sản phẩm bị lỗi do shop hoặc shipper.\n- Liên hệ hotline hoặc chat trực tiếp để được hỗ trợ.',
                                                                    'đổi trả, hoàn tiền, lỗi, không tươi, dập, chất lượng, khiếu nại',TRUE),
                                                                   ('POLICY','Phương thức thanh toán',
                                                                    'Food Market hỗ trợ nhiều hình thức thanh toán:\n- COD: Thanh toán tiền mặt khi nhận hàng.\n- Chuyển khoản ngân hàng.\n- PayOS: Quét mã QR thanh toán nhanh.',
                                                                    'thanh toán, COD, chuyển khoản, PayOS, QR',TRUE),
                                                                   ('POLICY','Chính sách chất lượng sản phẩm',
                                                                    'Food Market đảm bảo chất lượng nghiêm ngặt:\n- Tất cả sản phẩm có nguồn gốc xuất xứ rõ ràng.\n- Ưu tiên sản phẩm đạt chuẩn VietGAP, GlobalGAP hoặc Organic.\n- Kiểm tra chất lượng trước khi đưa lên sàn.',
                                                                    'chất lượng, organic, VietGAP, an toàn, nguồn gốc, xuất xứ',TRUE),
                                                                   ('FAQ','Làm thế nào để đặt hàng?',
                                                                    'Đặt hàng trên Food Market rất đơn giản:\n1. Tìm kiếm sản phẩm hoặc duyệt theo danh mục Củ / Quả.\n2. Chọn sản phẩm và thêm vào giỏ hàng.\n3. Điền địa chỉ giao hàng và chọn thanh toán.\n4. Xác nhận đơn hàng và chờ shop xác nhận.\n5. Shipper sẽ giao hàng trong 2-4 giờ.',
                                                                    'đặt hàng, mua hàng, giỏ hàng, checkout, order',TRUE),
                                                                   ('FAQ','Làm thế nào để theo dõi đơn hàng?',
                                                                    'Bạn có thể theo dõi đơn hàng:\n- Vào mục "Đơn hàng của tôi" trong tài khoản.\n- Xem trạng thái: Chờ xác nhận → Đã xác nhận → Đang giao → Đã giao.\n- Khi shipper đang giao, có thể xem GPS thời gian thực.',
                                                                    'theo dõi đơn, trạng thái đơn hàng, GPS, shipper đang giao',TRUE),
                                                                   ('FAQ','Làm thế nào để bảo quản củ quả tươi lâu?',
                                                                    'Mẹo bảo quản củ quả:\n- Củ (cà rốt, khoai tây, bí đỏ, bí đao, su su, cà tím): Nơi thoáng mát, tránh ánh nắng, dùng 1-2 tuần.\n- Gia vị củ (tỏi, gừng, nghệ, sả, hành tím): Nơi khô thoáng 2-4 tuần.\n- Trái cây chín (chuối, xoài, ổi, dưa hấu): Nhiệt độ phòng 3-5 ngày.\n- Trái cây chua (cam, bưởi, dâu tây, nho, thanh long): Ngăn mát 5-7 ngày.',
                                                                    'bảo quản, tươi lâu, tủ lạnh, để được bao lâu, hư, úa',TRUE),
                                                                   ('FAQ','Làm thế nào để liên hệ hỗ trợ?',
                                                                    'Liên hệ Food Market qua nhiều kênh:\n- Chat với trợ lý AI ngay trên app.\n- Nhắn tin trực tiếp với shop qua tính năng chat.\n- Email: support@foodmarket.vn\n- Hotline: 1900-xxxx (8h-22h mỗi ngày)',
                                                                    'liên hệ, hỗ trợ, hotline, chat, email, tư vấn',TRUE);

-- ============================================================
-- 15. RETURN REQUESTS
-- ============================================================
SET @od1_det=(SELECT order_detail_id FROM order_details WHERE order_id=@od1 LIMIT 1);
SET @od2_det=(SELECT order_detail_id FROM order_details WHERE order_id=@od2 LIMIT 1);
SET @od4_det=(SELECT order_detail_id FROM order_details WHERE order_id=@od4 LIMIT 1);

INSERT INTO return_requests (order_detail_id,shop_owner_id,buyer_id,reason,evidence,status,refund_amount,created_at,updated_at) VALUES
                                                                                                                                    (@od1_det,@shop1_id,@buyer1_id,'Cà rốt không tươi như quảng cáo, bị héo nhiều khi nhận hàng','https://res.cloudinary.com/dfp44sbsj/image/upload/v1774253772/brands/sftnvmjw2mvym1plcpql.jpg','PENDING',25000,DATE_SUB(NOW(),INTERVAL 1 HOUR),DATE_SUB(NOW(),INTERVAL 1 HOUR)),
                                                                                                                                    (@od2_det,@shop1_id,@buyer1_id,'Giao nhầm loại táo, mình đặt táo Fuji nhưng giao táo thường','https://res.cloudinary.com/dfp44sbsj/image/upload/v1774253772/brands/sftnvmjw2mvym1plcpql.jpg','SHOP_APPROVED',110000,DATE_SUB(NOW(),INTERVAL 2 DAY),DATE_SUB(NOW(),INTERVAL 1 DAY)),
                                                                                                                                    (@od4_det,@shop2_id,@buyer1_id,'Cam bị chua, không ngọt như mô tả','https://res.cloudinary.com/dfp44sbsj/image/upload/v1774253772/brands/sftnvmjw2mvym1plcpql.jpg','COMPLETED',35000,DATE_SUB(NOW(),INTERVAL 3 DAY),DATE_SUB(NOW(),INTERVAL 2 DAY));

UPDATE order_details SET is_requested_return=1 WHERE order_detail_id IN (@od1_det,@od2_det,@od4_det);

-- ============================================================
-- 16. CONVERSATIONS & CHAT
-- ============================================================
INSERT INTO conversations (room_key,user1_id,user2_id,created_at,last_message_at) VALUES
    (CONCAT(LEAST(@buyer1_id,@shop1_id),'_',GREATEST(@buyer1_id,@shop1_id)),
     LEAST(@buyer1_id,@shop1_id),GREATEST(@buyer1_id,@shop1_id),DATE_SUB(NOW(),INTERVAL 2 DAY),DATE_SUB(NOW(),INTERVAL 3 HOUR));
SET @conv1=(SELECT id FROM conversations WHERE room_key=CONCAT(LEAST(@buyer1_id,@shop1_id),'_',GREATEST(@buyer1_id,@shop1_id)));

INSERT INTO chat_messages (conversation_id,sender_id,content,sent_at,is_read) VALUES
                                                                                  (@conv1,@buyer1_id,'Chào shop, cà rốt của shop có tươi không ạ?',DATE_SUB(NOW(),INTERVAL 2 DAY),TRUE),
                                                                                  (@conv1,@shop1_id,'Chào bạn! Cà rốt Đà Lạt tươi mỗi ngày, đảm bảo chất lượng nhé! 🥕',DATE_SUB(NOW(),INTERVAL 47 HOUR),TRUE),
                                                                                  (@conv1,@buyer1_id,'Vậy 1kg khoảng bao nhiêu củ vậy shop?',DATE_SUB(NOW(),INTERVAL 46 HOUR),TRUE),
                                                                                  (@conv1,@shop1_id,'Thường 5-7 củ/kg bạn nhé, mình chọn củ to đều cho bạn 😊',DATE_SUB(NOW(),INTERVAL 45 HOUR),TRUE),
                                                                                  (@conv1,@buyer1_id,'Ok shop, mình đặt 2kg cà rốt với 1kg khoai tây nhé!',DATE_SUB(NOW(),INTERVAL 44 HOUR),TRUE),
                                                                                  (@conv1,@shop1_id,'Được bạn! Đặt qua app nhé, mình xác nhận ngay. Giao trong 2-3 tiếng 🚚',DATE_SUB(NOW(),INTERVAL 43 HOUR),TRUE),
                                                                                  (@conv1,@buyer1_id,'Cảm ơn shop nhiều! Lần sau mình sẽ tiếp tục ủng hộ 😄',DATE_SUB(NOW(),INTERVAL 5 HOUR),TRUE),
                                                                                  (@conv1,@shop1_id,'Cảm ơn bạn đã tin tưởng! Hàng tươi ngon đảm bảo nha 🌱',DATE_SUB(NOW(),INTERVAL 3 HOUR),FALSE);

INSERT INTO conversations (room_key,user1_id,user2_id,created_at,last_message_at) VALUES
    (CONCAT(LEAST(@buyer1_id,@shop2_id),'_',GREATEST(@buyer1_id,@shop2_id)),
     LEAST(@buyer1_id,@shop2_id),GREATEST(@buyer1_id,@shop2_id),DATE_SUB(NOW(),INTERVAL 1 DAY),DATE_SUB(NOW(),INTERVAL 1 HOUR));
SET @conv2=(SELECT id FROM conversations WHERE room_key=CONCAT(LEAST(@buyer1_id,@shop2_id),'_',GREATEST(@buyer1_id,@shop2_id)));

INSERT INTO chat_messages (conversation_id,sender_id,content,sent_at,is_read) VALUES
                                                                                  (@conv2,@buyer1_id,'Shop ơi, xoài cát Hòa Lộc còn hàng không?',DATE_SUB(NOW(),INTERVAL 1 DAY),TRUE),
                                                                                  (@conv2,@shop2_id,'Còn hàng bạn ơi! Xoài đang vào mùa, ngọt lắm. Còn khoảng 25kg 🥭',DATE_SUB(NOW(),INTERVAL 23 HOUR),TRUE),
                                                                                  (@conv2,@buyer1_id,'Xoài có chín cây không hay chín ép vậy shop?',DATE_SUB(NOW(),INTERVAL 23 HOUR),TRUE),
                                                                                  (@conv2,@shop2_id,'Toàn chín cây bạn nhé! Lấy thẳng từ vườn Tiền Giang, cam kết không ép 🌿',DATE_SUB(NOW(),INTERVAL 22 HOUR),TRUE),
                                                                                  (@conv2,@buyer1_id,'Mình muốn đặt 2kg xoài và 3kg cam sành nhé shop!',DATE_SUB(NOW(),INTERVAL 2 HOUR),TRUE),
                                                                                  (@conv2,@shop2_id,'Được bạn! Đặt qua app nhé, shop xác nhận ngay và giao trong 2-3 tiếng 🚀',DATE_SUB(NOW(),INTERVAL 90 MINUTE),TRUE),
                                                                                  (@conv2,@buyer1_id,'Cảm ơn shop! Mình đặt rồi nha 😊',DATE_SUB(NOW(),INTERVAL 1 HOUR),FALSE),
                                                                                  (@conv2,@shop2_id,'Shop nhận đơn rồi! Đang chuẩn bị hàng cho bạn 🍊',DATE_SUB(NOW(),INTERVAL 1 HOUR),FALSE);

INSERT INTO conversations (room_key,user1_id,user2_id,created_at,last_message_at) VALUES
    (CONCAT(LEAST(@buyer1_id,@admin_id),'_',GREATEST(@buyer1_id,@admin_id)),
     LEAST(@buyer1_id,@admin_id),GREATEST(@buyer1_id,@admin_id),DATE_SUB(NOW(),INTERVAL 3 DAY),DATE_SUB(NOW(),INTERVAL 2 DAY));
SET @conv3=(SELECT id FROM conversations WHERE room_key=CONCAT(LEAST(@buyer1_id,@admin_id),'_',GREATEST(@buyer1_id,@admin_id)));

INSERT INTO chat_messages (conversation_id,sender_id,content,sent_at,is_read) VALUES
                                                                                  (@conv3,@buyer1_id,'Xin chào Admin, mình có đơn hàng bị giao nhầm địa chỉ, phải làm sao ạ?',DATE_SUB(NOW(),INTERVAL 3 DAY),TRUE),
                                                                                  (@conv3,@admin_id,'Chào bạn! Bạn cung cấp mã đơn hàng cho mình kiểm tra nhé.',DATE_SUB(NOW(),INTERVAL 3 DAY),TRUE),
                                                                                  (@conv3,@buyer1_id,'Mã đơn là FM-001234, shipper giao nhầm số nhà ạ.',DATE_SUB(NOW(),INTERVAL 3 DAY),TRUE),
                                                                                  (@conv3,@admin_id,'Mình đã xem đơn rồi. Mình sẽ liên hệ shipper ngay để xử lý. Bạn chờ khoảng 15 phút nhé.',DATE_SUB(NOW(),INTERVAL 3 DAY),TRUE),
                                                                                  (@conv3,@admin_id,'Đã liên hệ xong rồi bạn! Shipper sẽ quay lại giao đúng địa chỉ trong 20 phút.',DATE_SUB(NOW(),INTERVAL 3 DAY),TRUE),
                                                                                  (@conv3,@buyer1_id,'Cảm ơn Admin đã hỗ trợ nhanh! 🙏',DATE_SUB(NOW(),INTERVAL 2 DAY),TRUE);

INSERT INTO conversations (room_key,user1_id,user2_id,created_at,last_message_at) VALUES
    (CONCAT(LEAST(@shop1_id,@admin_id),'_',GREATEST(@shop1_id,@admin_id)),
     LEAST(@shop1_id,@admin_id),GREATEST(@shop1_id,@admin_id),DATE_SUB(NOW(),INTERVAL 5 DAY),DATE_SUB(NOW(),INTERVAL 4 DAY));
SET @conv4=(SELECT id FROM conversations WHERE room_key=CONCAT(LEAST(@shop1_id,@admin_id),'_',GREATEST(@shop1_id,@admin_id)));

INSERT INTO chat_messages (conversation_id,sender_id,content,sent_at,is_read) VALUES
                                                                                  (@conv4,@shop1_id,'Admin ơi, shop mình muốn thêm sản phẩm mới thì cần thủ tục gì không?',DATE_SUB(NOW(),INTERVAL 5 DAY),TRUE),
                                                                                  (@conv4,@admin_id,'Chào shop! Shop chỉ cần đăng nhập vào dashboard và thêm sản phẩm là được ngay nhé.',DATE_SUB(NOW(),INTERVAL 5 DAY),TRUE),
                                                                                  (@conv4,@shop1_id,'Vậy có cần upload giấy tờ VietGAP không Admin?',DATE_SUB(NOW(),INTERVAL 5 DAY),TRUE),
                                                                                  (@conv4,@admin_id,'Không bắt buộc, nhưng nếu có thì tỉ lệ mua hàng cao hơn ~30% bạn nhé. Nên upload!',DATE_SUB(NOW(),INTERVAL 5 DAY),TRUE),
                                                                                  (@conv4,@shop1_id,'Còn về phí hoa hồng thì bao nhiêu % vậy Admin?',DATE_SUB(NOW(),INTERVAL 4 DAY),TRUE),
                                                                                  (@conv4,@admin_id,'Hiện tại Food Market thu 3% trên mỗi đơn thành công. Không có phí cố định nhé 😊',DATE_SUB(NOW(),INTERVAL 4 DAY),TRUE),
                                                                                  (@conv4,@shop1_id,'Oke, hợp lý! Cảm ơn Admin đã giải đáp.',DATE_SUB(NOW(),INTERVAL 4 DAY),TRUE);

INSERT INTO conversations (room_key,user1_id,user2_id,created_at,last_message_at) VALUES
    (CONCAT(LEAST(@shipper1_id,@admin_id),'_',GREATEST(@shipper1_id,@admin_id)),
     LEAST(@shipper1_id,@admin_id),GREATEST(@shipper1_id,@admin_id),DATE_SUB(NOW(),INTERVAL 4 DAY),DATE_SUB(NOW(),INTERVAL 3 DAY));
SET @conv5=(SELECT id FROM conversations WHERE room_key=CONCAT(LEAST(@shipper1_id,@admin_id),'_',GREATEST(@shipper1_id,@admin_id)));

INSERT INTO chat_messages (conversation_id,sender_id,content,sent_at,is_read) VALUES
                                                                                  (@conv5,@shipper1_id,'Admin ơi, mình muốn đăng ký nhận đơn khu vực Bình Dương được không?',DATE_SUB(NOW(),INTERVAL 4 DAY),TRUE),
                                                                                  (@conv5,@admin_id,'Được bạn! Bạn cập nhật khu vực hoạt động trong profile là được ngay nhé.',DATE_SUB(NOW(),INTERVAL 4 DAY),TRUE),
                                                                                  (@conv5,@shipper1_id,'Khi nhận đơn Bình Dương có phụ cấp thêm không Admin?',DATE_SUB(NOW(),INTERVAL 4 DAY),TRUE),
                                                                                  (@conv5,@admin_id,'Có! Thêm 5.000đ/đơn cho khu vực ngoại thành nhé. Đang áp dụng từ tháng này.',DATE_SUB(NOW(),INTERVAL 3 DAY),TRUE),
                                                                                  (@conv5,@shipper1_id,'Tuyệt! Mình đăng ký luôn. Cảm ơn Admin.',DATE_SUB(NOW(),INTERVAL 3 DAY),TRUE);

-- ============================================================
-- 17. BUILD PLANS (kế hoạch ăn uống - không dùng combo)
-- ============================================================
INSERT INTO build_plans (buyer_id,plan_name,number_of_people,number_of_days,meal_type,target_budget)
VALUES (@buyer1_id,'Kế hoạch giảm cân 3 ngày',2,3,'DIET',600000.00);
SET @plan1_id=(SELECT plan_id FROM build_plans WHERE buyer_id=@buyer1_id AND plan_name='Kế hoạch giảm cân 3 ngày' LIMIT 1);

INSERT INTO build_plans (buyer_id,plan_name,number_of_people,number_of_days,meal_type,target_budget)
VALUES (@buyer1_id,'Thực đơn gia đình 5 ngày',4,5,'FAMILY',1500000.00);
SET @plan2_id=(SELECT plan_id FROM build_plans WHERE buyer_id=@buyer1_id AND plan_name='Thực đơn gia đình 5 ngày' LIMIT 1);

INSERT INTO build_plans (buyer_id,plan_name,number_of_people,number_of_days,meal_type,target_budget)
VALUES (@buyer1_id,'Thực đơn ăn chay cuối tuần',2,2,'VEGAN',350000.00);
SET @plan3_id=(SELECT plan_id FROM build_plans WHERE buyer_id=@buyer1_id AND plan_name='Thực đơn ăn chay cuối tuần' LIMIT 1);

-- ============================================================
-- 18. BUILD PLAN ITEMS
-- ============================================================
INSERT INTO build_plan_items (plan_id,meal_name,servings,meal_date,completed) VALUES
                                                                                  (@plan1_id,'Bữa sáng ngày 1 - Nước ép cà rốt gừng',       2,DATE_ADD(CURDATE(),INTERVAL 1 DAY),FALSE),
                                                                                  (@plan1_id,'Bữa trưa ngày 1 - Salad dưa leo cà chua',      2,DATE_ADD(CURDATE(),INTERVAL 1 DAY),FALSE),
                                                                                  (@plan1_id,'Bữa tối ngày 1 - Canh bí đỏ hầm khoai tây',   2,DATE_ADD(CURDATE(),INTERVAL 1 DAY),FALSE),
                                                                                  (@plan1_id,'Bữa sáng ngày 2 - Nước ép cà rốt táo',         2,DATE_ADD(CURDATE(),INTERVAL 2 DAY),FALSE),
                                                                                  (@plan1_id,'Bữa trưa ngày 2 - Xào củ thập cẩm',            2,DATE_ADD(CURDATE(),INTERVAL 2 DAY),FALSE),
                                                                                  (@plan1_id,'Bữa tối ngày 2 - Súp bí đao tôm khô',          2,DATE_ADD(CURDATE(),INTERVAL 2 DAY),FALSE),
                                                                                  (@plan1_id,'Bữa sáng ngày 3 - Sinh tố dưa hấu cam',        2,DATE_ADD(CURDATE(),INTERVAL 3 DAY),FALSE),
                                                                                  (@plan1_id,'Bữa trưa ngày 3 - Salad ớt chuông dưa leo',    2,DATE_ADD(CURDATE(),INTERVAL 3 DAY),FALSE),
                                                                                  (@plan1_id,'Bữa tối ngày 3 - Hầm củ bổ dưỡng',            2,DATE_ADD(CURDATE(),INTERVAL 3 DAY),FALSE),
                                                                                  (@plan2_id,'Bữa sáng ngày 1 - Nước ép cà rốt gừng',        4,DATE_ADD(CURDATE(),INTERVAL 1 DAY),FALSE),
                                                                                  (@plan2_id,'Bữa trưa ngày 1 - Xào củ thập cẩm',            4,DATE_ADD(CURDATE(),INTERVAL 1 DAY),FALSE),
                                                                                  (@plan2_id,'Bữa tối ngày 1 - Lẩu củ quả gia đình',         4,DATE_ADD(CURDATE(),INTERVAL 1 DAY),FALSE),
                                                                                  (@plan2_id,'Bữa phụ ngày 1 - Trái cây tráng miệng',        4,DATE_ADD(CURDATE(),INTERVAL 1 DAY),FALSE),
                                                                                  (@plan3_id,'Bữa sáng ngày 1 - Sinh tố cam bưởi',           2,DATE_ADD(CURDATE(),INTERVAL 1 DAY),FALSE),
                                                                                  (@plan3_id,'Bữa trưa ngày 1 - Xào cà tím hành tỏi',        2,DATE_ADD(CURDATE(),INTERVAL 1 DAY),FALSE),
                                                                                  (@plan3_id,'Bữa tối ngày 1 - Canh bí đao su su chay',      2,DATE_ADD(CURDATE(),INTERVAL 1 DAY),FALSE),
                                                                                  (@plan3_id,'Bữa sáng ngày 2 - Nước ép cà rốt táo gừng',   2,DATE_ADD(CURDATE(),INTERVAL 2 DAY),FALSE),
                                                                                  (@plan3_id,'Bữa trưa ngày 2 - Salad dưa leo ớt chuông',    2,DATE_ADD(CURDATE(),INTERVAL 2 DAY),FALSE),
                                                                                  (@plan3_id,'Bữa tối ngày 2 - Hầm khoai môn khoai lang',    2,DATE_ADD(CURDATE(),INTERVAL 2 DAY),FALSE);

SET @bpi1=(SELECT id FROM build_plan_items WHERE plan_id=@plan1_id AND meal_name LIKE '%cà rốt gừng%' AND meal_date=DATE_ADD(CURDATE(),INTERVAL 1 DAY) LIMIT 1);
SET @bpi2=(SELECT id FROM build_plan_items WHERE plan_id=@plan1_id AND meal_name LIKE '%dưa leo%' AND meal_date=DATE_ADD(CURDATE(),INTERVAL 1 DAY) LIMIT 1);

INSERT INTO meal_products (meal_id,product_id) VALUES
                                                   (@bpi1,@p_ca_rot),(@bpi1,@p_gung),
                                                   (@bpi2,@p_dua_leo),(@bpi2,@p_ca_chua),(@bpi2,@p_ot_chuong);

-- ============================================================
-- 19. CART + CART ITEMS (chỉ sản phẩm từ shop1)
-- ============================================================
INSERT INTO cart (user_id) VALUES (@buyer1_id);
SET @cart_buyer1=(SELECT id FROM cart WHERE user_id=@buyer1_id LIMIT 1);

INSERT INTO cart_item (cart_id,product_product_id,mystery_box_id,build_combo_id,quantity) VALUES
                                                                                              (@cart_buyer1,@p_ca_rot,   NULL,NULL,2),
                                                                                              (@cart_buyer1,@p_khoai_tay,NULL,NULL,1),
                                                                                              (@cart_buyer1,@p_dau_tay,  NULL,NULL,1),
                                                                                              (@cart_buyer1,NULL,@box1_s1,NULL,1);

-- ============================================================
-- 20. ORDER MYSTERY BOXES
-- ============================================================
INSERT INTO orders (buyer_id,shop_owner_id,shipper_id,shipping_fee,recipient_name,recipient_phone,shipping_address,shipping_latitude,shipping_longitude,shop_latitude,shop_longitude,note,status,total_amount,payment_method,created_at)
VALUES (@buyer1_id,@shop1_id,@shipper1_id,15000,'Ngô Gia Sang','0911111111','Đ. Võ Trường Toản, Linh Trung, Thủ Đức, TP.HCM',10.8651,106.7752,10.8697,106.7717,'Hộp bí ẩn thú vị lắm!','DELIVERED',104000,'PAYOS',DATE_SUB(NOW(),INTERVAL 5 DAY));
SET @od_box=(SELECT order_id FROM orders WHERE status='DELIVERED' AND shop_owner_id=@shop1_id AND note='Hộp bí ẩn thú vị lắm!' LIMIT 1);
INSERT INTO order_mystery_boxes (order_id,mystery_box_id,quantity) VALUES (@od_box,@box1_s1,1);

INSERT INTO orders (buyer_id,shop_owner_id,shipper_id,shipping_fee,recipient_name,recipient_phone,shipping_address,shipping_latitude,shipping_longitude,shop_latitude,shop_longitude,note,status,total_amount,payment_method,created_at)
VALUES (@buyer1_id,@shop2_id,@shipper1_id,15000,'Ngô Gia Sang','0911111111','A15 Tiền Giang, Đông Hòa, Dĩ An, Bình Dương',10.8978,106.7731,10.9021,106.7698,'Trái cây nhiệt đới bí ẩn!','DELIVERED',164000,'COD',DATE_SUB(NOW(),INTERVAL 6 DAY));
SET @od_box2=(SELECT order_id FROM orders WHERE status='DELIVERED' AND shop_owner_id=@shop2_id AND note='Trái cây nhiệt đới bí ẩn!' LIMIT 1);
INSERT INTO order_mystery_boxes (order_id,mystery_box_id,quantity) VALUES (@od_box2,@box2_s2,1);

-- ============================================================
-- 20b. THÊM ĐƠN DELIVERED LỊCH SỬ CHO SHIPPER (dùng cho màn Thu nhập)
-- ============================================================
-- 5 đơn thêm cho shipper1 (tổng cộng 12 đơn × 15000 = 180000đ)
INSERT INTO orders (buyer_id,shop_owner_id,shipper_id,shipping_fee,recipient_name,recipient_phone,shipping_address,shipping_latitude,shipping_longitude,shop_latitude,shop_longitude,note,status,total_amount,payment_method,created_at)
VALUES
    (@buyer1_id,@shop1_id,@shipper1_id,15000,'Ngô Gia Sang','0911111111','Đ. Võ Trường Toản, Linh Trung, Thủ Đức, TP.HCM',10.8651,106.7752,10.8697,106.7717,'Cà rốt hữu cơ, rau cải sạch','DELIVERED',98000,'COD',DATE_SUB(NOW(),INTERVAL 1 DAY)),
    (@buyer1_id,@shop2_id,@shipper1_id,15000,'Ngô Gia Sang','0911111111','A15 Tiền Giang, Đông Hòa, Dĩ An, Bình Dương',10.8978,106.7731,10.9021,106.7698,'Xoài cát Hòa Lộc ngon tuyệt','DELIVERED',145000,'PAYOS',DATE_SUB(NOW(),INTERVAL 2 DAY)),
    (@buyer1_id,@shop1_id,@shipper1_id,15000,'Ngô Gia Sang','0911111111','Đ. Võ Trường Toản, Linh Trung, Thủ Đức, TP.HCM',10.8651,106.7752,10.8697,106.7717,'Khoai lang mật siêu ngọt','DELIVERED',112000,'COD',DATE_SUB(NOW(),INTERVAL 3 DAY)),
    (@buyer1_id,@shop2_id,@shipper1_id,15000,'Ngô Gia Sang','0911111111','A15 Tiền Giang, Đông Hòa, Dĩ An, Bình Dương',10.8978,106.7731,10.9021,106.7698,'Bưởi da xanh, cam sành tươi','DELIVERED',178000,'PAYOS',DATE_SUB(NOW(),INTERVAL 4 DAY)),
    (@buyer1_id,@shop1_id,@shipper1_id,15000,'Ngô Gia Sang','0911111111','Đ. Võ Trường Toản, Linh Trung, Thủ Đức, TP.HCM',10.8651,106.7752,10.8697,106.7717,'Giao đúng giờ, rất hài lòng','DELIVERED',135000,'COD',DATE_SUB(NOW(),INTERVAL 7 DAY));

-- 4 đơn cho shipper3 (tổng 4 đơn × 15000 = 60000đ)
INSERT INTO orders (buyer_id,shop_owner_id,shipper_id,shipping_fee,recipient_name,recipient_phone,shipping_address,shipping_latitude,shipping_longitude,shop_latitude,shop_longitude,note,status,total_amount,payment_method,created_at)
VALUES
    (@buyer1_id,@shop1_id,@shipper3_id,15000,'Ngô Gia Sang','0911111111','Đ. Võ Trường Toản, Linh Trung, Thủ Đức, TP.HCM',10.8651,106.7752,10.8697,106.7717,'Rau củ tươi ngon','DELIVERED',89000,'COD',DATE_SUB(NOW(),INTERVAL 8 DAY)),
    (@buyer1_id,@shop2_id,@shipper3_id,15000,'Ngô Gia Sang','0911111111','A15 Tiền Giang, Đông Hòa, Dĩ An, Bình Dương',10.8978,106.7731,10.9021,106.7698,'Trái cây nhiệt đới ngon','DELIVERED',125000,'PAYOS',DATE_SUB(NOW(),INTERVAL 10 DAY)),
    (@buyer1_id,@shop1_id,@shipper3_id,15000,'Ngô Gia Sang','0911111111','Đ. Võ Trường Toản, Linh Trung, Thủ Đức, TP.HCM',10.8651,106.7752,10.8697,106.7717,'Hàng tươi sạch như mô tả','DELIVERED',97000,'COD',DATE_SUB(NOW(),INTERVAL 12 DAY)),
    (@buyer1_id,@shop2_id,@shipper3_id,15000,'Ngô Gia Sang','0911111111','A15 Tiền Giang, Đông Hòa, Dĩ An, Bình Dương',10.8978,106.7731,10.9021,106.7698,'Dứa mật siêu thơm','DELIVERED',110000,'PAYOS',DATE_SUB(NOW(),INTERVAL 14 DAY));

-- ============================================================
-- 21. TRANSACTIONS
-- ============================================================
SET @payment_pending5=(SELECT payment_id FROM payments WHERE payos_order_code='FOOD20240005' LIMIT 1);

INSERT INTO transactions (payment_id,payment_gateway,amount,balance_after,status,content) VALUES
(@payment_success1,'PAYOS',217000.00,217000.00,'SUCCESS',
 'Thanh toán đơn hàng FOOD20240001 - Dâu tây Đà Lạt, Táo Fuji, Chuối - 217.000đ'),
(@payment_success2,'PAYOS',217000.00,367630.00,'SUCCESS',
 'Thanh toán đơn hàng FOOD20240002 - Xoài cát, Bưởi da xanh, Cam sành - 217.000đ'),
(@payment_success3,'PAYOS',225000.00,100413000.00,'SUCCESS',
 'Thanh toán đơn hàng FOOD20240003 - Cà rốt, Khoai tây, Bí đỏ - 225.000đ'),
(@payment_success4,'PAYOS',188000.00,100601000.00,'SUCCESS',
 'Thanh toán đơn hàng FOOD20240004 - Cam sành, Bưởi da xanh - 188.000đ'),
(@payment_pending5,'PAYOS',125000.00,0.00,'PENDING',
 'Khởi tạo thanh toán đơn hàng FOOD20240005 - Bí đỏ, Hành tây, Tỏi - 125.000đ'),
(NULL,'PAYOS',128000.00,131960.00,'SUCCESS',
 'Rút tiền doanh thu về tài khoản Vietcombank 0071000123456 - Trần Văn Kiệt'),
(NULL,'PAYOS',150000.00,217630.00,'PENDING',
 'Đang xử lý rút tiền về tài khoản Techcombank 19034567890123 - Lê Thị Ngọc');

-- ============================================================
-- 22. TEST DATA CHO 3 TÍNH NĂNG STAFF
--     Tính năng 1: Đóng gói  → status QUALITY_APPROVED → READY
--     Tính năng 2: Gọi Shipper → status READY (chưa có shipper)
--     Tính năng 3: Hồ sơ nhân viên → user staff@gmail.com
-- ============================================================

-- ----------------------------------------------------------------
-- TÍNH NĂNG 1: ĐÓNG GÓI — 5 đơn QUALITY_APPROVED
-- Staff tick checklist → bấm "Hoàn tất đóng gói" → READY
-- ----------------------------------------------------------------
INSERT INTO orders (buyer_id,shop_owner_id,shipper_id,shipping_fee,recipient_name,recipient_phone,
                    shipping_address,shipping_latitude,shipping_longitude,
                    shop_latitude,shop_longitude,
                    note,status,total_amount,payment_method,created_at)
VALUES
    (@buyer1_id,@shop1_id,NULL,15000,'Ngô Gia Sang','0911111111',
     'Đ. Võ Trường Toản, Linh Trung, Thủ Đức, TP.HCM',10.8651,106.7752,10.8697,106.7717,
     'Đóng gói cẩn thận giúp mình nhé','QUALITY_CHECKED',130000,'COD',
     DATE_SUB(NOW(),INTERVAL 20 MINUTE)),

    (@buyer1_id,@shop1_id,NULL,15000,'Ngô Gia Sang','0911111111',
     'Đ. Võ Trường Toản, Linh Trung, Thủ Đức, TP.HCM',10.8651,106.7752,10.8697,106.7717,
     'Trái cây dễ dập, bọc kỹ giúp mình','QUALITY_CHECKED',320000,'PAYOS',
     DATE_SUB(NOW(),INTERVAL 35 MINUTE)),

    (@buyer1_id,@shop2_id,NULL,15000,'Ngô Gia Sang','0911111111',
     'A15 Tiền Giang, Đông Hòa, Dĩ An, Bình Dương',10.8978,106.7731,10.9021,106.7698,
     NULL,'QUALITY_CHECKED',220000,'COD',
     DATE_SUB(NOW(),INTERVAL 50 MINUTE)),

    (@buyer1_id,@shop2_id,NULL,15000,'Ngô Gia Sang','0911111111',
     'A15 Tiền Giang, Đông Hòa, Dĩ An, Bình Dương',10.8978,106.7731,10.9021,106.7698,
     NULL,'QUALITY_CHECKED',119000,'COD',
     DATE_SUB(NOW(),INTERVAL 65 MINUTE)),

    (@buyer1_id,@shop1_id,NULL,15000,'Ngô Gia Sang','0911111111',
     'Đ. Võ Trường Toản, Linh Trung, Thủ Đức, TP.HCM',10.8651,106.7752,10.8697,106.7717,
     NULL,'QUALITY_CHECKED',194000,'PAYOS',
     DATE_SUB(NOW(),INTERVAL 80 MINUTE));

SET @qa1=(SELECT order_id FROM orders WHERE status='QUALITY_CHECKED' AND shop_owner_id=@shop1_id ORDER BY created_at DESC LIMIT 1 OFFSET 0);
SET @qa2=(SELECT order_id FROM orders WHERE status='QUALITY_CHECKED' AND shop_owner_id=@shop1_id ORDER BY created_at DESC LIMIT 1 OFFSET 1);
SET @qa3=(SELECT order_id FROM orders WHERE status='QUALITY_CHECKED' AND shop_owner_id=@shop2_id ORDER BY created_at DESC LIMIT 1 OFFSET 0);
SET @qa4=(SELECT order_id FROM orders WHERE status='QUALITY_CHECKED' AND shop_owner_id=@shop2_id ORDER BY created_at DESC LIMIT 1 OFFSET 1);
SET @qa5=(SELECT order_id FROM orders WHERE status='QUALITY_CHECKED' AND shop_owner_id=@shop1_id ORDER BY created_at ASC LIMIT 1);

INSERT INTO order_details (order_id,product_id,quantity,unit_price,is_requested_return) VALUES
                                                                                            (@qa1,@p_ca_rot,3,25000,0), (@qa1,@p_khoai_tay,2,30000,0),
                                                                                            (@qa2,@p_dau_tay,1,180000,0), (@qa2,@p_tao,1,110000,0),
                                                                                            (@qa3,@p2_xoai,2,85000,0), (@qa3,@p2_cam,1,35000,0),
                                                                                            (@qa4,@p2_dua_hau,3,12000,0), (@qa4,@p2_thanh_long,2,35000,0),
                                                                                            (@qa5,@p_bi_do,3,22000,0), (@qa5,@p_hanh_tay,2,25000,0), (@qa5,@p_toi,1,85000,0);

-- ----------------------------------------------------------------
-- TÍNH NĂNG 2: GỌI SHIPPER — 4 đơn READY chưa có shipper
-- Staff chọn shipper → bấm "Phân công" → BE set shipperId + SHIPPING
-- Hoặc shipper tự vào app thấy đơn READY → bấm nhận → SHIPPING
-- ----------------------------------------------------------------
INSERT INTO orders (buyer_id,shop_owner_id,shipper_id,shipping_fee,recipient_name,recipient_phone,
                    shipping_address,shipping_latitude,shipping_longitude,
                    shop_latitude,shop_longitude,
                    note,status,total_amount,payment_method,created_at)
VALUES
    (@buyer1_id,@shop1_id,NULL,15000,'Ngô Gia Sang','0911111111',
     'Đ. Võ Trường Toản, Linh Trung, Thủ Đức, TP.HCM',10.8651,106.7752,10.8697,106.7717,
     'Giao trước 12h trưa','READY',80000,'COD',
     DATE_SUB(NOW(),INTERVAL 95 MINUTE)),

    (@buyer1_id,@shop2_id,NULL,15000,'Ngô Gia Sang','0911111111',
     'A15 Tiền Giang, Đông Hòa, Dĩ An, Bình Dương',10.8978,106.7731,10.9021,106.7698,
     NULL,'READY',110000,'COD',
     DATE_SUB(NOW(),INTERVAL 110 MINUTE)),

    (@buyer1_id,@shop1_id,NULL,15000,'Ngô Gia Sang','0911111111',
     'Đ. Võ Trường Toản, Linh Trung, Thủ Đức, TP.HCM',10.8651,106.7752,10.8697,106.7717,
     NULL,'READY',165000,'PAYOS',
     DATE_SUB(NOW(),INTERVAL 125 MINUTE)),

    (@buyer1_id,@shop2_id,NULL,15000,'Ngô Gia Sang','0911111111',
     'A15 Tiền Giang, Đông Hòa, Dĩ An, Bình Dương',10.8978,106.7731,10.9021,106.7698,
     NULL,'READY',95000,'COD',
     DATE_SUB(NOW(),INTERVAL 140 MINUTE));

SET @rd1=(SELECT order_id FROM orders WHERE status='READY' AND shop_owner_id=@shop1_id ORDER BY created_at DESC LIMIT 1 OFFSET 0);
SET @rd2=(SELECT order_id FROM orders WHERE status='READY' AND shop_owner_id=@shop2_id ORDER BY created_at DESC LIMIT 1 OFFSET 0);
SET @rd3=(SELECT order_id FROM orders WHERE status='READY' AND shop_owner_id=@shop1_id ORDER BY created_at ASC LIMIT 1);
SET @rd4=(SELECT order_id FROM orders WHERE status='READY' AND shop_owner_id=@shop2_id ORDER BY created_at ASC LIMIT 1);

INSERT INTO order_details (order_id,product_id,quantity,unit_price,is_requested_return) VALUES
                                                                                            (@rd1,@p_ca_chua,2,35000,0), (@rd1,@p_dua_leo,1,15000,0),
                                                                                            (@rd2,@p2_oi,2,30000,0), (@rd2,@p2_cam,1,35000,0),
                                                                                            (@rd3,@p_gung,1,55000,0), (@rd3,@p_nghe,1,40000,0), (@rd3,@p2_sa,3,10000,0),
                                                                                            (@rd4,@p2_dua_hau,5,12000,0);

-- ----------------------------------------------------------------
-- SHIPPER LOCATIONS
-- Shipper1 (ACTIVE): rảnh
-- Shipper2 (PENDING): có trong bảng nhưng BE filter ACTIVE nên không hiện
-- Shipper3 (ACTIVE): rảnh
-- ----------------------------------------------------------------
INSERT INTO shipper_locations (shipper_id, order_id, latitude, longitude, updated_at)
VALUES (@shipper1_id, NULL, 10.8670, 106.7730, NOW())
    ON DUPLICATE KEY UPDATE order_id=NULL, latitude=10.8670, longitude=106.7730, updated_at=NOW();

INSERT INTO shipper_locations (shipper_id, order_id, latitude, longitude, updated_at)
VALUES (@shipper2_id, @os1, 10.8720, 106.7680, NOW())
    ON DUPLICATE KEY UPDATE order_id=@os1, latitude=10.8720, longitude=106.7680, updated_at=NOW();

INSERT INTO shipper_locations (shipper_id, order_id, latitude, longitude, updated_at)
VALUES (@shipper3_id, NULL, 10.8651, 106.7752, NOW())
    ON DUPLICATE KEY UPDATE order_id=NULL, latitude=10.8651, longitude=106.7752, updated_at=NOW();

-- ----------------------------------------------------------------
-- TÍNH NĂNG 3: PIPELINE STAFF — đơn ở các trạng thái kho
-- ----------------------------------------------------------------
INSERT INTO orders (buyer_id,shop_owner_id,shipper_id,shipping_fee,recipient_name,recipient_phone,
                    shipping_address,shipping_latitude,shipping_longitude,
                    shop_latitude,shop_longitude,
                    note,status,total_amount,payment_method,created_at)
VALUES
    (@buyer1_id,@shop1_id,NULL,15000,'Ngô Gia Sang','0911111111',
     'Đ. Võ Trường Toản, Linh Trung, Thủ Đức, TP.HCM',10.8651,106.7752,10.8697,106.7717,
     NULL,'SHIPPING_TO_WAREHOUSE',145000,'COD',
     DATE_SUB(NOW(),INTERVAL 155 MINUTE)),

    (@buyer1_id,@shop2_id,NULL,15000,'Ngô Gia Sang','0911111111',
     'A15 Tiền Giang, Đông Hòa, Dĩ An, Bình Dương',10.8978,106.7731,10.9021,106.7698,
     NULL,'SHIPPING_TO_WAREHOUSE',175000,'PAYOS',
     DATE_SUB(NOW(),INTERVAL 170 MINUTE)),

    (@buyer1_id,@shop1_id,NULL,15000,'Ngô Gia Sang','0911111111',
     'Đ. Võ Trường Toản, Linh Trung, Thủ Đức, TP.HCM',10.8651,106.7752,10.8697,106.7717,
     NULL,'SHIPPING_TO_WAREHOUSE',98000,'COD',
     DATE_SUB(NOW(),INTERVAL 185 MINUTE)),

    (@buyer1_id,@shop1_id,NULL,15000,'Ngô Gia Sang','0911111111',
     'Đ. Võ Trường Toản, Linh Trung, Thủ Đức, TP.HCM',10.8651,106.7752,10.8697,106.7717,
     NULL,'ARRIVED_AT_WAREHOUSE',112000,'COD',
     DATE_SUB(NOW(),INTERVAL 200 MINUTE)),

    (@buyer1_id,@shop2_id,NULL,15000,'Ngô Gia Sang','0911111111',
     'A15 Tiền Giang, Đông Hòa, Dĩ An, Bình Dương',10.8978,106.7731,10.9021,106.7698,
     NULL,'ARRIVED_AT_WAREHOUSE',155000,'PAYOS',
     DATE_SUB(NOW(),INTERVAL 215 MINUTE));

SET @stw1=(SELECT order_id FROM orders WHERE status='SHIPPING_TO_WAREHOUSE' AND shop_owner_id=@shop1_id ORDER BY created_at DESC LIMIT 1 OFFSET 0);
SET @stw2=(SELECT order_id FROM orders WHERE status='SHIPPING_TO_WAREHOUSE' AND shop_owner_id=@shop2_id ORDER BY created_at DESC LIMIT 1);
SET @stw3=(SELECT order_id FROM orders WHERE status='SHIPPING_TO_WAREHOUSE' AND shop_owner_id=@shop1_id ORDER BY created_at ASC LIMIT 1);
SET @atw1=(SELECT order_id FROM orders WHERE status='ARRIVED_AT_WAREHOUSE' AND shop_owner_id=@shop1_id ORDER BY created_at DESC LIMIT 1);
SET @atw2=(SELECT order_id FROM orders WHERE status='ARRIVED_AT_WAREHOUSE' AND shop_owner_id=@shop2_id ORDER BY created_at DESC LIMIT 1);

INSERT INTO order_details (order_id,product_id,quantity,unit_price,is_requested_return) VALUES
                                                                                            (@stw1,@p_ca_rot,4,25000,0), (@stw1,@p_khoai_tay,1,30000,0),
                                                                                            (@stw2,@p2_xoai,1,85000,0), (@stw2,@p2_cam,2,35000,0), (@stw2,@p2_oi,1,30000,0),
                                                                                            (@stw3,@p_bi_do,2,22000,0), (@stw3,@p_hanh_tay,2,25000,0),
                                                                                            (@atw1,@p_khoai_lang,2,28000,0), (@atw1,@p_gung,1,55000,0),
                                                                                            (@atw2,@p2_thanh_long,2,35000,0), (@atw2,@p2_buoi,1,65000,0);
SET FOREIGN_KEY_CHECKS = 1;


INSERT INTO quality_crieria (quality_name, description)
VALUES
    ('Màu sắc', 'Kiểm tra màu sắc sản phẩm có tự nhiên, đồng đều hay không'),

    ('Mùi hương', 'Kiểm tra sản phẩm không có mùi lạ, mùi hư hỏng'),

    ('Độ tươi', 'Kiểm tra sản phẩm còn tươi, không héo úa hoặc dập nát'),

    ('Đóng gói', 'Kiểm tra bao bì còn nguyên vẹn, không rách hoặc móp méo'),

    ('Trọng lượng', 'Kiểm tra trọng lượng thực tế có khớp với khai báo'),

    ('Đánh giá chuyên môn', 'Nhân viên kiểm định đánh giá tổng quan chất lượng sản phẩm');

-- ============================================================
-- WAREHOUSES
-- ============================================================
INSERT INTO warehouses (name, address, latitude, longitude, active) VALUES
    ('Kho Bãi', 'Hẻm C21, đường 449, Lê Văn Việt, Phường Tăng Nhơn Phú A, TP HCM', 10.8490, 106.7720, TRUE);

-- ============================================================
-- TỔNG KẾT DỮ LIỆU
-- Roles:            4 (ADMIN, BUYER, SHOP_OWNER, SHIPPER)
-- Categories:       2 (Củ, Quả/Trái cây)
-- Users:            8 (1 admin, 2 buyer, 2 shipper, 3 shop)
-- Wallets:          6 (1 platform, 3 shop, 2 shipper)
-- Products:         shop1: 17 AVAILABLE + 3 PENDING + 3 OUT_OF_STOCK + 3 INACTIVE = 26
--                   shop2: 16 AVAILABLE + 3 PENDING + 3 OUT_OF_STOCK + 3 INACTIVE = 25
-- Mystery Boxes:    4 (2 shop1, 2 shop2)
-- Vouchers:         8 (4 shop1, 4 shop2)
-- Orders:           30 (PENDING×4, CONFIRMED×4, SHIPPING×4,
--                       DELIVERED×6+2 mystery box, CANCELLED×5, FAILED×5)
-- Order Mystery Boxes: 2 records
-- Reviews:          5 records
-- Payments:         5 records
-- Withdraw Requests:3 records
-- Notifications:    6 broadcasts + 10 user_notifications
-- Blogs:            3 articles
-- Bot Knowledge:    8 entries
-- Return Requests:  3 records
-- Cart:             1 cart, 4 items (shop1 only)
-- Conversations:    5 (buyer-shop1, buyer-shop2, buyer-admin, shop1-admin, shipper1-admin)
-- Build Plans:      3 (giảm cân 3 ngày, gia đình 5 ngày, ăn chay 2 ngày)
-- Transactions:     8 (SUCCESS×3, PENDING×3, FAILED×1 + 1 withdraw)
-- ============================================================
SELECT 'exe.sql executed successfully!' AS result;

-- ============================================================
-- CHECKLIST KIEM TRA 3 TINH NANG STAFF
-- Dang nhap: staff@gmail.com / Admin@123
-- ============================================================
-- [1] TRANG PACKING (/staff/pack):
--     - Section "Cho dong goi": 5 don QUALITY_CHECKED
--       * Ca rot + Khoai tay (shop1)
--       * Dau tay + Tao (shop1)
--       * Xoai + Cam (shop2)
--       * Dua hau + Thanh long (shop2)
--       * Bi do + Hanh tay + Toi (shop1)
--     - Tick du 5 checklist -> nut "Bat dau dong goi" sang len
--     - Bam "Bat dau dong goi" -> don chuyen sang PACK
--     - Section "Dang dong goi": 2 don PACK
--       * Khoai lang + Cu cai (shop1)
--       * Nho + Buoi (shop2)
--     - Bam "Hoan tat dong goi" -> don chuyen sang READY
--
-- [2] TRANG CALL SHIPPER (/staff/shipper):
--     - Cot trai "Don san sang giao": 4 don READY
--       * Ca chua + Dua leo (shop1)
--       * Oi + Cam (shop2)
--       * Gung + Nghe + Sa (shop1)
--       * Dua hau (shop2)
--     - Cot phai "Shipper khu vuc": 2 shipper online
--       * Nguyen Van Hoang (shipper1): badge "Ranh"
--       * Tran Giao Hang (shipper2): badge "Dang giao"
--     - Chon shipper ranh -> bam "Phan cong Shipper"
--     - Don bien mat khoi danh sach READY
--
-- [3] TRANG HO SO NHAN VIEN (/staff/profile):
--     - Thong tin: Nhan vien, staff@gmail.com, 0900000009, TP.HCM
--     - Bao cao tong quan:
--       * Don dang van chuyen toi kho: 3
--       * Don dang kiem duyet: 2
--       * Don cho/dang dong goi: 7 (5 QUALITY_APPROVED + 2 PACK)
--       * Don san sang giao: 4 (READY)
--       * Nong dan cho duyet: 2
-- ============================================================






