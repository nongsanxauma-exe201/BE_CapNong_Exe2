-- ============================================================
-- FoodMarket_BE - Complete Database Script (MERGED FINAL)
-- Gộp toàn bộ: Base + Products + Orders + Reviews + Cart +
--              Conversations + BuildPlans + Transactions
-- Chạy 1 lần duy nhất trên DB sạch
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
TRUNCATE TABLE orders;
TRUNCATE TABLE withdraw_requests;
TRUNCATE TABLE wallets;
TRUNCATE TABLE product_mystery;
TRUNCATE TABLE mystery_boxes;
TRUNCATE TABLE product_combo;
TRUNCATE TABLE build_combos;
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


-- ============================================================
-- 1. ROLES
-- ============================================================
INSERT INTO roles (name, description) VALUES
                                          ('ADMIN','System administrator'),('BUYER','Regular customer'),
                                          ('SHOP_OWNER','Shop owner / Seller'),('SHIPPER','Shipment personnel')
    ON DUPLICATE KEY UPDATE description=VALUES(description);

SET @admin_role_id=(SELECT id FROM roles WHERE name='ADMIN');
SET @buyer_role_id=(SELECT id FROM roles WHERE name='BUYER');
SET @shop_role_id=(SELECT id FROM roles WHERE name='SHOP_OWNER');
SET @shipper_role_id=(SELECT id FROM roles WHERE name='SHIPPER');


-- ============================================================
-- 2. CATEGORIES
-- ============================================================
INSERT INTO categories (name, description) VALUES
                                               ('Rau lá','Các loại rau xanh tươi sạch: rau muống, cải, xà lách, rau thơm...'),
                                               ('Củ','Các loại củ tươi: cà rốt, khoai tây, khoai lang, củ cải, hành tỏi...'),
                                               ('Quả/Trái cây','Trái cây tươi ngon: chuối, xoài, dưa hấu, cam, bưởi, dâu tây...')
    ON DUPLICATE KEY UPDATE description=VALUES(description);

SET @cat_rau_la=(SELECT category_id FROM categories WHERE name='Rau lá');
SET @cat_cu=(SELECT category_id FROM categories WHERE name='Củ');
SET @cat_trai_cay=(SELECT category_id FROM categories WHERE name='Quả/Trái cây');


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
VALUES ('shop1@gmail.com','$2a$12$lU/EpH6mhkL4ERuMRO2cjeeT1CPHxqAyDHGwnDctIWzojS6k/oG/K',@shop_role_id,'ACTIVE','Trần Văn Kiệt','0933333333','VRF2+9FG, Phường Linh Trung, Thủ Đức, TP.HCM','Nông Sản Xấu Mã','https://res.cloudinary.com/dfp44sbsj/image/upload/v1774252594/brands/srflhqfol1ckwavsaa1n.jpg','Chuyên rau củ quả organic sạch từ Đà Lạt và các vùng nông nghiệp sạch.',4.9,'Vietcombank','0071000123456','TRAN VAN KIET','Hạt Giống Vàng',NOW())
    ON DUPLICATE KEY UPDATE email=email;
SET @shop1_id=(SELECT id FROM users WHERE email='shop1@gmail.com');

INSERT INTO users (email,password,role_id,status,full_name,phone_number,address,shop_name,logo_url,description,rating_average,bank_name,bank_account,bank_account_holder,achievement,create_at)
VALUES ('shop2@gmail.com','$2a$12$lU/EpH6mhkL4ERuMRO2cjeeT1CPHxqAyDHGwnDctIWzojS6k/oG/K',@shop_role_id,'ACTIVE','Lê Thị Ngọc','0944444444','62 Nguyễn Bỉnh Khiêm, Dĩ An, Bình Dương','Vườn Xanh Bình Dương','https://res.cloudinary.com/dfp44sbsj/image/upload/v1774252337/brands/t9muel0yqmqfzfu3ndij.jpg','Chuyên rau củ quả tươi miền Nam, đặc sản miền Tây chất lượng cao.',4.7,'Techcombank','19034567890123','LE THI NGOC','Nông Dân Tinh Hoa',NOW())
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


-- ============================================================
-- 4. WALLETS
-- ============================================================
-- PLATFORM wallet: giữ balance lớn để demo, commission_wallet tích lũy từ các đơn DELIVERED
INSERT INTO wallets (admin_id,status,total_balance,frozen_balance,total_revenue_all_time,total_withdrawn,commission_wallet,type)
VALUES (@admin_id,'ACTIVE',100000000,0,0,0,24210,'PLATFORM');

-- SHOP wallets:
-- shop1: revenue=428,000 | commission(3%)=12,840 | net=415,160
--        withdraw SUCCESS=128,000 → còn 287,160
--        withdraw APPROVED 200,000 đang xử lý → total_balance=87,160, frozen=200,000
-- shop2: revenue=379,000 | commission(3%)=11,370 | net=367,630
--        withdraw PENDING 150,000 → total_balance=217,630, frozen=150,000
INSERT INTO wallets (shop_id,status,total_balance,frozen_balance,total_revenue_all_time,total_withdrawn,commission_wallet,type)
VALUES (@shop1_id,'ACTIVE',87160,200000,428000,128000,12840,'SHOP'),
       (@shop2_id,'ACTIVE',217630,150000,379000,0,11370,'SHOP'),
       (@shop3_id,'ACTIVE',0,0,0,0,0,'SHOP');

-- SHIPPER wallets:
-- shipper1: 4 đơn DELIVERED x 15,000 ship_fee = 60,000đ
INSERT INTO wallets (shipper_id,status,total_balance,frozen_balance,total_revenue_all_time,total_withdrawn,commission_wallet,type)
VALUES (@shipper1_id,'ACTIVE',60000,0,60000,0,0,'SHIPPER'),
       (@shipper2_id,'ACTIVE',0,0,0,0,0,'SHIPPER');

SET @wallet_shop1_id=(SELECT wallet_id FROM wallets WHERE shop_id=@shop1_id);
SET @wallet_shop2_id=(SELECT wallet_id FROM wallets WHERE shop_id=@shop2_id);
SET @wallet_shop3_id=(SELECT wallet_id FROM wallets WHERE shop_id=@shop3_id);


-- ============================================================
-- 5. PRODUCTS (50 sản phẩm)
-- ============================================================
INSERT INTO products (shop_owner_id,category_id,product_name,unit,selling_price,stock_quantity,description,status,image_url) VALUES
-- SHOP 1 RAU LÁ (9)
(@shop1_id,@cat_rau_la,'Rau muống','bó',12000,90,'Rau muống tươi xanh, ngọn non mướt, sạch từ vùng trồng hữu cơ.','AVAILABLE','https://res.cloudinary.com/dfp44sbsj/image/upload/v1773943166/brands/ezrip6hvips9nwmyottf.jpg'),
(@shop1_id,@cat_rau_la,'Cải ngọt','bó',15000,60,'Cải ngọt tươi non, giòn ngọt, thích hợp xào hoặc nấu canh.','AVAILABLE','https://res.cloudinary.com/dfp44sbsj/image/upload/v1773943139/brands/mhfnqgigq3xwxivi3r0u.jpg'),
(@shop1_id,@cat_rau_la,'Xà lách','túi',15000,60,'Xà lách giòn tươi, không thuốc, rửa sạch ăn liền.','AVAILABLE','https://res.cloudinary.com/dfp44sbsj/image/upload/v1773916585/brands/ojx1comtiw3wbpq4rlzy.jpg'),
(@shop1_id,@cat_rau_la,'Cải bó xôi','túi',18000,50,'Cải bó xôi (spinach) giàu sắt và vitamin, tốt cho sức khỏe.','AVAILABLE','https://res.cloudinary.com/dfp44sbsj/image/upload/v1774249130/brands/vleciprlqifxmkwsmlxz.webp'),
(@shop1_id,@cat_rau_la,'Hành lá','bó',8000,100,'Hành lá tươi xanh thơm, dùng làm gia vị hoặc xào nấu.','AVAILABLE','https://res.cloudinary.com/dfp44sbsj/image/upload/v1773943193/brands/fzg3tlvbf7kx09nhznxd.jpg'),
(@shop1_id,@cat_rau_la,'Rau mùi','bó',8000,80,'Rau mùi (ngò rí) thơm đặc trưng, dùng trang trí và tăng hương vị.','AVAILABLE','https://res.cloudinary.com/dfp44sbsj/image/upload/v1774249286/brands/fn5usjst1v3b4lvea7on.webp'),
(@shop1_id,@cat_rau_la,'Bông cải xanh','cái',15000,50,'Bông cải xanh (broccoli) organic Đà Lạt, giàu vitamin C và K.','AVAILABLE','https://res.cloudinary.com/dfp44sbsj/image/upload/v1773916637/brands/ydpaytovqs8e05gix3wh.jpg'),
(@shop1_id,@cat_rau_la,'Cải thảo','cây',20000,45,'Cải thảo tươi to, lá dày, nấu lẩu hoặc muối dưa đều ngon.','AVAILABLE','https://res.cloudinary.com/dfp44sbsj/image/upload/v1774249372/brands/p0cwg1qgivpgp0rmhz8e.jpg'),
(@shop1_id,@cat_rau_la,'Rau cần','bó',12000,55,'Rau cần tươi giòn, thơm nhẹ, dùng xào hoặc ăn lẩu.','AVAILABLE','https://res.cloudinary.com/dfp44sbsj/image/upload/v1774249437/brands/qzokycjqyjypjkdeggzz.webp'),
-- SHOP 1 CỦ (9)
(@shop1_id,@cat_cu,'Cà rốt','kg',25000,100,'Cà rốt Đà Lạt tươi ngon, ngọt tự nhiên, giàu beta-carotene.','AVAILABLE','https://res.cloudinary.com/dfp44sbsj/image/upload/v1773916721/brands/uj1eoinkwvthfjvtpvpq.jpg'),
(@shop1_id,@cat_cu,'Khoai tây','kg',30000,80,'Khoai tây Đà Lạt dẻo thơm, thích hợp chiên, hầm, nấu canh.','AVAILABLE','https://res.cloudinary.com/dfp44sbsj/image/upload/v1773916676/brands/bmgnazybi0rro8bnxhwp.jpg'),
(@shop1_id,@cat_cu,'Khoai lang','kg',28000,70,'Khoai lang tím Đà Lạt ngọt bùi, giàu anthocyanin tốt cho tim mạch.','AVAILABLE','https://res.cloudinary.com/dfp44sbsj/image/upload/v1774249499/brands/pgdvlccom3lid5t24pnk.jpg'),
(@shop1_id,@cat_cu,'Củ cải trắng','kg',18000,70,'Củ cải trắng tươi ngọt thanh, nấu canh hoặc muối dưa đều ngon.','AVAILABLE','https://res.cloudinary.com/dfp44sbsj/image/upload/v1773943219/brands/dysl261o6o2bapfwurmf.jpg'),
(@shop1_id,@cat_cu,'Bí đỏ','kg',22000,60,'Bí đỏ bùi ngọt, giàu vitamin A, nấu canh hoặc hầm đều tuyệt.','AVAILABLE','https://res.cloudinary.com/dfp44sbsj/image/upload/v1773917463/brands/xjz74sedvolg7woxywpo.jpg'),
(@shop1_id,@cat_cu,'Hành tây','kg',25000,80,'Hành tây tươi to tròn, thơm ngọt, dùng xào nấu hay ăn sống.','AVAILABLE','https://res.cloudinary.com/dfp44sbsj/image/upload/v1773943658/brands/qieeuppupbnypfoenpwg.jpg'),
(@shop1_id,@cat_cu,'Cần tây','bó',15000,40,'Cần tây xanh giòn, giàu chất xơ, tốt cho tiêu hóa và giảm cân.','AVAILABLE','https://res.cloudinary.com/dfp44sbsj/image/upload/v1774249561/brands/jtszr297g1cr4g25vqjj.webp'),
(@shop1_id,@cat_cu,'Tỏi','kg',85000,35,'Tỏi ta Lý Sơn đậm vị thơm nồng, gia vị không thể thiếu.','AVAILABLE','https://res.cloudinary.com/dfp44sbsj/image/upload/v1773943632/brands/zhw2ww8qghsqscht29xb.jpg'),
(@shop1_id,@cat_cu,'Gừng','kg',55000,45,'Gừng tươi cay nồng ấm bụng, dùng nấu ăn, pha trà hay làm thuốc.','AVAILABLE','https://res.cloudinary.com/dfp44sbsj/image/upload/v1774249614/brands/ponetjgmfffbv8j8xpto.jpg'),
(@shop1_id,@cat_cu,'Nghệ','kg',40000,30,'Nghệ tươi vàng óng, kháng viêm tự nhiên, dùng nấu ăn và làm đẹp.','AVAILABLE','https://res.cloudinary.com/dfp44sbsj/image/upload/v1774249845/brands/xafrqajebqsjhwhramh3.jpg'),
-- SHOP 1 QUẢ (7)
(@shop1_id,@cat_trai_cay,'Cà chua','kg',35000,50,'Cà chua đỏ tươi ngọt, giàu lycopene, ăn tươi hay nấu đều ngon.','AVAILABLE','https://res.cloudinary.com/dfp44sbsj/image/upload/v1773916530/brands/xjiiaehe5evwybwhmpmu.jpg'),
(@shop1_id,@cat_trai_cay,'Dưa leo','kg',15000,100,'Dưa leo (dưa chuột) sạch giòn mát, thích hợp salad hoặc ăn kèm.','AVAILABLE','https://res.cloudinary.com/dfp44sbsj/image/upload/v1773917431/brands/yif2icgotbydg5qvzvbf.jpg'),
(@shop1_id,@cat_trai_cay,'Ớt chuông','kg',55000,40,'Ớt chuông 3 màu (đỏ/vàng/xanh) giòn ngọt, giàu vitamin C.','AVAILABLE','https://res.cloudinary.com/dfp44sbsj/image/upload/v1774249920/brands/azkaanfoa7ommwnhxbv6.jpg'),
(@shop1_id,@cat_trai_cay,'Dâu tây','kg',180000,20,'Dâu tây Đà Lạt tươi chua ngọt, thu hoạch buổi sáng giao ngay.','AVAILABLE','https://res.cloudinary.com/dfp44sbsj/image/upload/v1773943061/brands/qtspypfmdjcxtsbme7xi.jpg'),
(@shop1_id,@cat_trai_cay,'Chuối','nải',25000,20,'Chuối sứ chín vàng thơm ngọt, giàu kali và năng lượng tự nhiên.','AVAILABLE','https://res.cloudinary.com/dfp44sbsj/image/upload/v1773916917/brands/n8ykdyjv59yavetdoj2l.jpg'),
(@shop1_id,@cat_trai_cay,'Táo','kg',110000,30,'Táo Fuji nhập khẩu giòn ngọt, vỏ đỏ đẹp, bảo quản tươi lâu.','AVAILABLE','https://images.unsplash.com/photo-1560806887-1e4cd0b6cbd6?w=600&h=400&fit=crop'),
(@shop1_id,@cat_trai_cay,'Mướp','kg',18000,60,'Mướp hương tươi ngon, nấu canh hay xào đều thơm ngon.','AVAILABLE','https://res.cloudinary.com/dfp44sbsj/image/upload/v1774249989/brands/o7csibhzy57s2wpmcus9.jpg'),
-- SHOP 2 RAU LÁ (9)
(@shop2_id,@cat_rau_la,'Rau ngót','bó',10000,70,'Rau ngót tươi xanh, vị ngọt thanh, nấu canh thịt heo rất ngon.','AVAILABLE','https://res.cloudinary.com/dfp44sbsj/image/upload/v1774250103/brands/qlsbgtgno3kvconpo0c3.jpg'),
(@shop2_id,@cat_rau_la,'Rau dền','bó',12000,60,'Rau dền đỏ giàu sắt và canxi, xào tỏi hoặc nấu canh đều thơm.','AVAILABLE','https://res.cloudinary.com/dfp44sbsj/image/upload/v1774250148/brands/t27u2nfmhvuykmnepi4i.jpg'),
(@shop2_id,@cat_rau_la,'Cải thìa','bó',14000,80,'Cải thìa xanh mướt, ngọt nhẹ, xào nhanh giữ dinh dưỡng tốt.','AVAILABLE','https://res.cloudinary.com/dfp44sbsj/image/upload/v1773943732/brands/e2cs62np0m1fup69b2vq.jpg'),
(@shop2_id,@cat_rau_la,'Mồng tơi','bó',10000,90,'Mồng tơi tươi non mướt, nấu canh cua hoặc luộc chấm mắm ngon.','AVAILABLE','https://res.cloudinary.com/dfp44sbsj/image/upload/v1774250212/brands/mtbfbygr3tmlctmfdg0x.jpg'),
(@shop2_id,@cat_rau_la,'Rau mầm','hộp',20000,50,'Rau mầm đậu xanh sạch, giàu enzyme sống, dùng salad rất tốt.','AVAILABLE','https://res.cloudinary.com/dfp44sbsj/image/upload/v1774250321/brands/vr6xifyvc3s2f4w0qcr2.jpg'),
(@shop2_id,@cat_rau_la,'Đậu bắp','kg',20000,60,'Đậu bắp tươi xanh mướt, giàu chất nhầy tốt cho dạ dày.','AVAILABLE','https://res.cloudinary.com/dfp44sbsj/image/upload/v1773917403/brands/julxwllwivzxf4r9d0ku.jpg'),
(@shop2_id,@cat_rau_la,'Đậu que','kg',25000,55,'Đậu que tươi giòn, giàu protein thực vật, xào tỏi hoặc luộc.','AVAILABLE','https://res.cloudinary.com/dfp44sbsj/image/upload/v1774250377/brands/xudlsis7nb0l7iylxsfo.jpg'),
(@shop2_id,@cat_rau_la,'Húng quế','bó',8000,70,'Húng quế tươi thơm, dùng pha nước uống, nấu phở hay trang trí.','AVAILABLE','https://res.cloudinary.com/dfp44sbsj/image/upload/v1774250491/brands/fn5r9hvqfjqfn71qd8uc.jpg'),
(@shop2_id,@cat_rau_la,'Tía tô','bó',8000,65,'Tía tô tươi thơm đặc trưng, ăn kèm thịt nướng hay bún riêu.','AVAILABLE','https://res.cloudinary.com/dfp44sbsj/image/upload/v1774250587/brands/dwxt9lm8whaj95t5qkrv.jpg'),
-- SHOP 2 CỦ (9)
(@shop2_id,@cat_cu,'Bí đao','kg',15000,60,'Bí đao tươi to, nấu canh giải nhiệt mùa hè rất mát.','AVAILABLE','https://res.cloudinary.com/dfp44sbsj/image/upload/v1774250920/brands/w0bf8nfrconotcqe8vec.jpg'),
(@shop2_id,@cat_cu,'Bí ngòi','kg',18000,55,'Bí ngòi (zucchini) non tươi, xào nướng hay nấu canh đều ngon.','AVAILABLE','https://res.cloudinary.com/dfp44sbsj/image/upload/v1774250987/brands/z0qmi41ocaxbgaxa7kq1.jpg'),
(@shop2_id,@cat_cu,'Su su','kg',20000,60,'Su su non tươi ngọt thanh, luộc hoặc xào đều giữ vị ngon.','AVAILABLE','https://res.cloudinary.com/dfp44sbsj/image/upload/v1773943711/brands/tjr6dujcccbiyqdpr4qk.jpg'),
(@shop2_id,@cat_cu,'Cà tím','kg',22000,50,'Cà tím tươi bóng mướt, nướng mỡ hành hoặc hấp chấm nước mắm.','AVAILABLE','https://res.cloudinary.com/dfp44sbsj/image/upload/v1773943685/brands/sn8j7wqbpp6uhtitk5nf.jpg'),
(@shop2_id,@cat_cu,'Hành tím','kg',45000,40,'Hành tím thơm nồng, gia vị không thể thiếu trong bếp Việt.','AVAILABLE','https://res.cloudinary.com/dfp44sbsj/image/upload/v1773943658/brands/qieeuppupbnypfoenpwg.jpg'),
(@shop2_id,@cat_cu,'Sả','bó',10000,80,'Sả tươi thơm nồng, dùng xào nướng, nấu lẩu hay đuổi muỗi.','AVAILABLE','https://res.cloudinary.com/dfp44sbsj/image/upload/v1774251166/brands/s5xw72wtr6ezrs1ykpzo.jpg'),
(@shop2_id,@cat_cu,'Khoai môn','kg',32000,40,'Khoai môn dẻo bùi thơm, nấu chè, hầm xương hoặc chiên đều ngon.','AVAILABLE','https://res.cloudinary.com/dfp44sbsj/image/upload/v1774251234/brands/vocydo64vc5ljrztscl8.jpg'),
(@shop2_id,@cat_cu,'Ớt đỏ','kg',50000,35,'Ớt đỏ tươi cay nồng, dùng làm gia vị hay muối chua.','AVAILABLE','https://res.cloudinary.com/dfp44sbsj/image/upload/v1773943106/brands/eburdvo8w0tnequetwte.jpg'),
(@shop2_id,@cat_cu,'Tỏi tây','cây',15000,40,'Tỏi tây (leek) tươi to, vị nhẹ hơn tỏi thường, dùng xào hay nấu súp.','AVAILABLE','https://res.cloudinary.com/dfp44sbsj/image/upload/v1773943632/brands/zhw2ww8qghsqscht29xb.jpg'),
-- SHOP 2 QUẢ (7)
(@shop2_id,@cat_trai_cay,'Dưa hấu','kg',12000,200,'Dưa hấu đỏ mọng nước, ngọt lịm, giải nhiệt mùa hè tuyệt vời.','AVAILABLE','https://res.cloudinary.com/dfp44sbsj/image/upload/v1773917373/brands/h0mivuwgipa6hssaawfi.jpg'),
(@shop2_id,@cat_trai_cay,'Thanh long','kg',35000,80,'Thanh long ruột đỏ Bình Thuận ngọt đậm, giàu chất chống oxy hóa.','AVAILABLE','https://res.cloudinary.com/dfp44sbsj/image/upload/v1773917308/brands/dfm7lmlxne7nztndgk0m.jpg'),
(@shop2_id,@cat_trai_cay,'Xoài','kg',85000,25,'Xoài cát Hòa Lộc thơm lừng vị ngọt đậm đà, đặc sản Tiền Giang.','AVAILABLE','https://images.unsplash.com/photo-1553279768-865429fa0078?w=600&h=400&fit=crop'),
(@shop2_id,@cat_trai_cay,'Cam','kg',35000,150,'Cam sành Tiền Giang mọng nước, ngọt thanh, giàu vitamin C.','AVAILABLE','https://res.cloudinary.com/dfp44sbsj/image/upload/v1773916481/brands/s5ld5232nl4s4fwczawr.jpg'),
(@shop2_id,@cat_trai_cay,'Bưởi','kg',65000,50,'Bưởi da xanh Bến Tre ngọt thanh, múi to không hạt, bổ dưỡng.','AVAILABLE','https://res.cloudinary.com/dfp44sbsj/image/upload/v1773917344/brands/c2i9gkbkl8vx7iwulwsz.png'),
(@shop2_id,@cat_trai_cay,'Ổi','kg',30000,50,'Ổi ruột đỏ thơm giòn ngọt, giàu vitamin C, ăn tươi hoặc ép nước.','AVAILABLE','https://res.cloudinary.com/dfp44sbsj/image/upload/v1773942953/brands/ohl0lsjzhcq6pmqp4qmu.jpg'),
(@shop2_id,@cat_trai_cay,'Nho','kg',120000,25,'Nho đen không hạt, ngọt đậm, giàu resveratrol tốt cho tim mạch.','AVAILABLE','https://res.cloudinary.com/dfp44sbsj/image/upload/v1773916419/brands/zdz1hvl9hcx6yyaq1wq3.jpg');


-- ============================================================
-- 6. BUILD COMBOS (28 combo)
-- ============================================================
INSERT INTO build_combos (shop_owner_id,combo_name,discount_price,description,type,region,meal_type,image_url) VALUES
                                                                                                                   (@shop1_id,'Combo nấu canh chua cá',95000,'Nguyên liệu đầy đủ để nấu nồi canh chua cá thơm ngon: cà chua tươi, đậu bắp xanh, hành lá, rau mùi thơm và các gia vị tươi. Thiếu cá thì ghé chợ thêm là xong nồi canh chua đậm vị miền Nam.','Available','MIEN_NAM','LUNCH','https://res.cloudinary.com/dfp44sbsj/image/upload/v1774253772/brands/sftnvmjw2mvym1plcpql.jpg'),
                                                                                                                   (@shop1_id,'Combo salad giảm cân',65000,'Bộ rau sạch cho bữa salad ăn kiêng: xà lách xanh mướt, dưa leo giòn mát, ớt chuông đỏ ngọt, cần tây thái lát. Ít calo, giàu chất xơ, phù hợp người đang giảm cân hoặc ăn eat clean.','Available','MIEN_NAM','BREAKFAST','https://images.unsplash.com/photo-1512621776951-a57141f2eefd?w=400&h=300&fit=crop'),
                                                                                                                   (@shop1_id,'Combo xào rau thập cẩm',70000,'Hỗn hợp rau củ đa dạng để xào nhanh: cải ngọt non, bông cải xanh, cà rốt thái sợi, hành tây xắt múi, hành lá. Đủ màu sắc, đủ vitamin, xào 15 phút là có bữa trưa ngon lành.','Available','MIEN_NAM','LUNCH','https://images.unsplash.com/photo-1547592180-85f173990554?w=400&h=300&fit=crop'),
                                                                                                                   (@shop1_id,'Combo detox xanh buổi sáng',55000,'Rau củ để ép nước detox buổi sáng: cải bó xôi tươi, dưa leo mát lạnh, cần tây giòn, gừng tươi cay nhẹ. Uống mỗi sáng giúp thanh lọc cơ thể, tăng năng lượng, sáng da.','Available','MIEN_NAM','BREAKFAST','https://images.unsplash.com/photo-1619566636858-adf3ef46400b?w=400&h=300&fit=crop'),
                                                                                                                   (@shop1_id,'Combo hầm rau củ bổ dưỡng',90000,'Rau củ để hầm súp bổ dưỡng cho cả nhà: khoai tây to bùi, cà rốt ngọt, bí đỏ mềm, củ cải trắng thanh, hành tây thơm. Hầm 45 phút với xương heo sẽ ra nồi súp vàng ươm bổ dưỡng.','Available','MIEN_NAM','DINNER','https://images.unsplash.com/photo-1540420773420-3366772f4999?w=400&h=300&fit=crop'),
                                                                                                                   (@shop1_id,'Combo trái cây tráng miệng',120000,'Mix trái cây tươi cho bữa tráng miệng: táo Fuji giòn ngọt, chuối sứ chín thơm, dâu tây Đà Lạt chua ngọt. Thơm ngon ngọt mát, cung cấp vitamin và khoáng chất thiết yếu.','Available','MIEN_NAM','SNACK','https://res.cloudinary.com/dfp44sbsj/image/upload/v1774253772/brands/sftnvmjw2mvym1plcpql.jpg'),
                                                                                                                   (@shop1_id,'Combo rau sống ăn kèm',45000,'Rau sống tươi sạch ăn kèm các món nướng, lẩu, bún, bánh hỏi: xà lách xanh, rau mùi thơm, hành lá, dưa leo thái lát. Rửa sạch là dùng được ngay, không cần sơ chế nhiều.','Available','MIEN_NAM','LUNCH','https://images.unsplash.com/photo-1512621776951-a57141f2eefd?w=400&h=300&fit=crop'),
                                                                                                                   (@shop1_id,'Combo nấu lẩu rau củ',95000,'Rau củ đa dạng để nhúng lẩu: cải ngọt, bông cải xanh, cà rốt, khoai tây, mướp hương, rau cần. Đủ màu sắc hấp dẫn, nhúng lẩu nước dùng xương ăn ngon miệng.','Available','MIEN_NAM','DINNER','https://images.unsplash.com/photo-1547592180-85f173990554?w=400&h=300&fit=crop'),
                                                                                                                   (@shop1_id,'Combo nấu bún bò',85000,'Rau ăn kèm bún bò Huế truyền thống: rau muống tươi, xà lách, rau mùi thơm, hành lá thái nhỏ. Đủ bộ rau sạch cho tô bún bò đúng điệu, ăn kèm chả cua tôm càng thêm ngon.','Available','MIEN_NAM','LUNCH','https://images.unsplash.com/photo-1619566636858-adf3ef46400b?w=400&h=300&fit=crop'),
                                                                                                                   (@shop1_id,'Combo làm gỏi cuốn',60000,'Nguyên liệu gỏi cuốn tươi ngon: xà lách giòn, cà rốt bào sợi, dưa leo thái thanh, rau mùi thơm. Cuốn cùng tôm hoặc thịt luộc, chấm tương hoisin hoặc nước mắm chua ngọt.','Available','MIEN_NAM','LUNCH','https://images.unsplash.com/photo-1540420773420-3366772f4999?w=400&h=300&fit=crop'),
                                                                                                                   (@shop1_id,'Combo sinh tố rau củ',75000,'Nguyên liệu để xay sinh tố rau củ healthy: cà rốt ngọt, cần tây giòn, gừng tươi cay nhẹ, táo xanh. Giàu enzyme sống và vitamin, uống mỗi ngày tốt cho da và tiêu hóa.','Available','MIEN_NAM','BREAKFAST','https://res.cloudinary.com/dfp44sbsj/image/upload/v1774253772/brands/sftnvmjw2mvym1plcpql.jpg'),
                                                                                                                   (@shop1_id,'Combo cơm chiên rau củ',80000,'Rau củ để nấu cơm chiên ngũ sắc: cà rốt hạt lựu, hành tây xắt nhỏ, hành lá thái, mướp hương thái hạt lựu. Cơm chiên màu sắc đẹp, thơm ngon bổ dưỡng cho cả gia đình.','Available','MIEN_NAM','LUNCH','https://images.unsplash.com/photo-1512621776951-a57141f2eefd?w=400&h=300&fit=crop'),
                                                                                                                   (@shop1_id,'Combo nấu canh ngót',70000,'Nguyên liệu nấu canh ngót bổ dưỡng: rau muống tươi non, hành lá xanh, gừng tươi thơm, hành lá. Nấu cùng thịt heo xay hoặc cua đồng ra nồi canh ngọt thanh rất bổ dưỡng.','Available','MIEN_NAM','DINNER','https://images.unsplash.com/photo-1547592180-85f173990554?w=400&h=300&fit=crop'),
                                                                                                                   (@shop1_id,'Combo ăn kiêng eat clean',70000,'Rau củ cho chế độ ăn kiêng eat clean nghiêm túc: cải bó xôi giàu sắt, bông cải xanh nhiều vitamin, cà rốt beta-carotene, cần tây ít calo, dưa leo mát. Luộc hấp đơn giản, không dầu mỡ.','Available','MIEN_NAM','BREAKFAST','https://images.unsplash.com/photo-1619566636858-adf3ef46400b?w=400&h=300&fit=crop'),
                                                                                                                   (@shop2_id,'Combo canh rau ngọt',55000,'Rau nấu canh ngọt thanh đậm vị: rau ngót tươi đặc trưng, mồng tơi non mướt, cải thìa xanh mát. Nấu cùng tôm khô hoặc xương heo ra nồi canh ngọt tự nhiên không cần bột ngọt.','Available','MIEN_NAM','LUNCH','https://images.unsplash.com/photo-1540420773420-3366772f4999?w=400&h=300&fit=crop'),
                                                                                                                   (@shop2_id,'Combo giải nhiệt mùa hè',80000,'Trái cây giải nhiệt ngày nóng: dưa hấu đỏ mọng nước cắt sẵn, bưởi da xanh ngọt thanh, cam sành mọng nước. Ăn tươi hoặc ép nước uống đều mát lạnh, bổ sung điện giải tự nhiên.','Available','MIEN_NAM','SNACK','https://res.cloudinary.com/dfp44sbsj/image/upload/v1774253772/brands/sftnvmjw2mvym1plcpql.jpg'),
                                                                                                                   (@shop2_id,'Combo rau mầm healthy',60000,'Rau sạch cho bữa eat clean hoàn hảo: rau mầm đậu xanh giàu enzyme sống, đậu que tươi giòn, đậu bắp xanh mướt. Luộc nhẹ hoặc ăn sống trực tiếp đều giữ nguyên dinh dưỡng.','Available','MIEN_NAM','BREAKFAST','https://images.unsplash.com/photo-1512621776951-a57141f2eefd?w=400&h=300&fit=crop'),
                                                                                                                   (@shop2_id,'Combo xào cà tím hành tỏi',75000,'Nguyên liệu xào cà tím thơm ngon: cà tím tươi thái khúc vừa, hành tím bóc sẵn, ớt đỏ thái lát cay nhẹ, sả thơm. Xào với nước tương và dầu hào ra món ăn đậm đà hấp dẫn.','Available','MIEN_NAM','LUNCH','https://images.unsplash.com/photo-1547592180-85f173990554?w=400&h=300&fit=crop'),
                                                                                                                   (@shop2_id,'Combo trái cây nhiệt đới',100000,'Đặc sản trái cây miền Nam chính gốc: xoài cát Hòa Lộc thơm lừng, thanh long ruột đỏ mọng nước, ổi ruột đỏ giòn ngọt. Tươi ngon bổ dưỡng, giàu vitamin và khoáng chất.','Available','MIEN_NAM','SNACK','https://images.unsplash.com/photo-1619566636858-adf3ef46400b?w=400&h=300&fit=crop'),
                                                                                                                   (@shop2_id,'Combo súp bí đao bổ mát',65000,'Nguyên liệu nấu súp bí đao giải nhiệt: bí đao to thái miếng vuông, bí ngòi non thái khoanh, su su tươi bổ tư, hành lá thái nhỏ. Nấu cùng tôm khô ra nồi súp mát ngọt tự nhiên.','Available','MIEN_NAM','DINNER','https://images.unsplash.com/photo-1540420773420-3366772f4999?w=400&h=300&fit=crop'),
                                                                                                                   (@shop2_id,'Combo gia vị tươi cần thiết',85000,'Bộ gia vị tươi không thể thiếu trong bếp Việt: hành tím thơm nồng, sả cây thơm đặc trưng, ớt đỏ cay vừa, tỏi tây vị nhẹ. Đủ gia vị để nấu bất kỳ món ăn ngon nào.','Available','MIEN_NAM','LUNCH','https://res.cloudinary.com/dfp44sbsj/image/upload/v1774253772/brands/sftnvmjw2mvym1plcpql.jpg'),
                                                                                                                   (@shop2_id,'Combo nước ép hoa quả',90000,'Trái cây để ép nước uống hàng ngày: cam sành vắt nước mọng, bưởi da xanh ép lạnh thanh mát, ổi ruột đỏ xay sinh tố. Giàu vitamin C, tăng đề kháng tự nhiên mỗi ngày.','Available','MIEN_NAM','BREAKFAST','https://images.unsplash.com/photo-1512621776951-a57141f2eefd?w=400&h=300&fit=crop'),
                                                                                                                   (@shop2_id,'Combo nấu canh chua tôm',110000,'Nguyên liệu canh chua tôm kiểu miền Tây: mồng tơi tươi, đậu bắp xanh, sả thơm, ớt đỏ cay nhẹ. Kết hợp với tôm tươi và me chua sẽ ra nồi canh chua đặc trưng miền sông nước.','Available','MIEN_NAM','LUNCH','https://images.unsplash.com/photo-1547592180-85f173990554?w=400&h=300&fit=crop'),
                                                                                                                   (@shop2_id,'Combo lẩu rau củ chay',85000,'Rau củ nhúng lẩu chay thanh đạm: bí đao, su su, đậu bắp, cải thìa, mồng tơi, đậu que, rau mầm. Nhúng nước lẩu nấm thanh đạm hoặc nước dùng rau củ rất ngon miệng.','Available','MIEN_NAM','DINNER','https://images.unsplash.com/photo-1619566636858-adf3ef46400b?w=400&h=300&fit=crop'),
                                                                                                                   (@shop2_id,'Combo gỏi xoài xanh',75000,'Nguyên liệu làm gỏi xoài chua ngọt: xoài tươi thái lát, rau dền tía giòn, tía tô thơm, húng quế tươi. Trộn nước mắm chua ngọt chanh tỏi ăn kèm bánh phồng tôm rất ngon.','Available','MIEN_NAM','SNACK','https://images.unsplash.com/photo-1540420773420-3366772f4999?w=400&h=300&fit=crop'),
                                                                                                                   (@shop2_id,'Combo rau ăn phở',50000,'Rau ăn kèm phở truyền thống đúng điệu: giá đỗ tươi (rau mầm), húng quế thơm, tía tô thơm đặc trưng. Đủ bộ rau sạch cho tô phở đậm đà, thêm vài lát ớt là hoàn hảo.','Available','MIEN_NAM','BREAKFAST','https://res.cloudinary.com/dfp44sbsj/image/upload/v1774253772/brands/sftnvmjw2mvym1plcpql.jpg'),
                                                                                                                   (@shop2_id,'Combo dưa góp ăn kèm',55000,'Nguyên liệu làm dưa góp giòn ngon: cà tím thái khoanh, hành tím bóc vỏ, ớt đỏ thái lát. Muối chua 30-60 phút là ăn được, giòn ngon ăn kèm thịt kho hay cá chiên.','Available','MIEN_NAM','LUNCH','https://images.unsplash.com/photo-1512621776951-a57141f2eefd?w=400&h=300&fit=crop'),
                                                                                                                   (@shop2_id,'Combo sinh tố trái cây',95000,'Trái cây xay sinh tố bổ dưỡng ngon miệng: xoài chín ngọt lịm, cam vắt nước cốt, ổi ruột đỏ thơm. Xay với sữa chua hoặc sữa tươi ra ly sinh tố vừa ngon vừa bổ dưỡng cho cả nhà.','Available','MIEN_NAM','BREAKFAST','https://images.unsplash.com/photo-1547592180-85f173990554?w=400&h=300&fit=crop');


-- ============================================================
-- 7. PRODUCT COMBO MAPPINGS
-- ============================================================
SET @p_rau_muong=(SELECT product_id FROM products WHERE product_name='Rau muống' AND shop_owner_id=@shop1_id);
SET @p_cai_ngot=(SELECT product_id FROM products WHERE product_name='Cải ngọt' AND shop_owner_id=@shop1_id);
SET @p_xa_lach=(SELECT product_id FROM products WHERE product_name='Xà lách' AND shop_owner_id=@shop1_id);
SET @p_cai_bo_xoi=(SELECT product_id FROM products WHERE product_name='Cải bó xôi' AND shop_owner_id=@shop1_id);
SET @p_hanh_la=(SELECT product_id FROM products WHERE product_name='Hành lá' AND shop_owner_id=@shop1_id);
SET @p_rau_mui=(SELECT product_id FROM products WHERE product_name='Rau mùi' AND shop_owner_id=@shop1_id);
SET @p_bong_cai=(SELECT product_id FROM products WHERE product_name='Bông cải xanh' AND shop_owner_id=@shop1_id);
SET @p_cai_thao=(SELECT product_id FROM products WHERE product_name='Cải thảo' AND shop_owner_id=@shop1_id);
SET @p_rau_can=(SELECT product_id FROM products WHERE product_name='Rau cần' AND shop_owner_id=@shop1_id);
SET @p_ca_rot=(SELECT product_id FROM products WHERE product_name='Cà rốt' AND shop_owner_id=@shop1_id);
SET @p_khoai_tay=(SELECT product_id FROM products WHERE product_name='Khoai tây' AND shop_owner_id=@shop1_id);
SET @p_khoai_lang=(SELECT product_id FROM products WHERE product_name='Khoai lang' AND shop_owner_id=@shop1_id);
SET @p_cu_cai=(SELECT product_id FROM products WHERE product_name='Củ cải trắng' AND shop_owner_id=@shop1_id);
SET @p_bi_do=(SELECT product_id FROM products WHERE product_name='Bí đỏ' AND shop_owner_id=@shop1_id);
SET @p_hanh_tay=(SELECT product_id FROM products WHERE product_name='Hành tây' AND shop_owner_id=@shop1_id);
SET @p_can_tay=(SELECT product_id FROM products WHERE product_name='Cần tây' AND shop_owner_id=@shop1_id);
SET @p_toi=(SELECT product_id FROM products WHERE product_name='Tỏi' AND shop_owner_id=@shop1_id);
SET @p_gung=(SELECT product_id FROM products WHERE product_name='Gừng' AND shop_owner_id=@shop1_id);
SET @p_nghe=(SELECT product_id FROM products WHERE product_name='Nghệ' AND shop_owner_id=@shop1_id);
SET @p_ca_chua=(SELECT product_id FROM products WHERE product_name='Cà chua' AND shop_owner_id=@shop1_id);
SET @p_dua_leo=(SELECT product_id FROM products WHERE product_name='Dưa leo' AND shop_owner_id=@shop1_id);
SET @p_ot_chuong=(SELECT product_id FROM products WHERE product_name='Ớt chuông' AND shop_owner_id=@shop1_id);
SET @p_dau_tay=(SELECT product_id FROM products WHERE product_name='Dâu tây' AND shop_owner_id=@shop1_id);
SET @p_chuoi=(SELECT product_id FROM products WHERE product_name='Chuối' AND shop_owner_id=@shop1_id);
SET @p_tao=(SELECT product_id FROM products WHERE product_name='Táo' AND shop_owner_id=@shop1_id);
SET @p_muop=(SELECT product_id FROM products WHERE product_name='Mướp' AND shop_owner_id=@shop1_id);

SET @p2_rau_ngot=(SELECT product_id FROM products WHERE product_name='Rau ngót' AND shop_owner_id=@shop2_id);
SET @p2_rau_den=(SELECT product_id FROM products WHERE product_name='Rau dền' AND shop_owner_id=@shop2_id);
SET @p2_cai_thia=(SELECT product_id FROM products WHERE product_name='Cải thìa' AND shop_owner_id=@shop2_id);
SET @p2_mong_toi=(SELECT product_id FROM products WHERE product_name='Mồng tơi' AND shop_owner_id=@shop2_id);
SET @p2_rau_mam=(SELECT product_id FROM products WHERE product_name='Rau mầm' AND shop_owner_id=@shop2_id);
SET @p2_dau_bap=(SELECT product_id FROM products WHERE product_name='Đậu bắp' AND shop_owner_id=@shop2_id);
SET @p2_dau_que=(SELECT product_id FROM products WHERE product_name='Đậu que' AND shop_owner_id=@shop2_id);
SET @p2_hung_que=(SELECT product_id FROM products WHERE product_name='Húng quế' AND shop_owner_id=@shop2_id);
SET @p2_tia_to=(SELECT product_id FROM products WHERE product_name='Tía tô' AND shop_owner_id=@shop2_id);
SET @p2_bi_dao=(SELECT product_id FROM products WHERE product_name='Bí đao' AND shop_owner_id=@shop2_id);
SET @p2_bi_ngoi=(SELECT product_id FROM products WHERE product_name='Bí ngòi' AND shop_owner_id=@shop2_id);
SET @p2_su_su=(SELECT product_id FROM products WHERE product_name='Su su' AND shop_owner_id=@shop2_id);
SET @p2_ca_tim=(SELECT product_id FROM products WHERE product_name='Cà tím' AND shop_owner_id=@shop2_id);
SET @p2_hanh_tim=(SELECT product_id FROM products WHERE product_name='Hành tím' AND shop_owner_id=@shop2_id);
SET @p2_sa=(SELECT product_id FROM products WHERE product_name='Sả' AND shop_owner_id=@shop2_id);
SET @p2_khoai_mon=(SELECT product_id FROM products WHERE product_name='Khoai môn' AND shop_owner_id=@shop2_id);
SET @p2_ot_do=(SELECT product_id FROM products WHERE product_name='Ớt đỏ' AND shop_owner_id=@shop2_id);
SET @p2_toi_tay=(SELECT product_id FROM products WHERE product_name='Tỏi tây' AND shop_owner_id=@shop2_id);
SET @p2_dua_hau=(SELECT product_id FROM products WHERE product_name='Dưa hấu' AND shop_owner_id=@shop2_id);
SET @p2_thanh_long=(SELECT product_id FROM products WHERE product_name='Thanh long' AND shop_owner_id=@shop2_id);
SET @p2_xoai=(SELECT product_id FROM products WHERE product_name='Xoài' AND shop_owner_id=@shop2_id);
SET @p2_cam=(SELECT product_id FROM products WHERE product_name='Cam' AND shop_owner_id=@shop2_id);
SET @p2_buoi=(SELECT product_id FROM products WHERE product_name='Bưởi' AND shop_owner_id=@shop2_id);
SET @p2_oi=(SELECT product_id FROM products WHERE product_name='Ổi' AND shop_owner_id=@shop2_id);
SET @p2_nho=(SELECT product_id FROM products WHERE product_name='Nho' AND shop_owner_id=@shop2_id);

SET @c1_s1=(SELECT combo_id FROM build_combos WHERE combo_name='Combo nấu canh chua cá' AND shop_owner_id=@shop1_id);
SET @c2_s1=(SELECT combo_id FROM build_combos WHERE combo_name='Combo salad giảm cân' AND shop_owner_id=@shop1_id);
SET @c3_s1=(SELECT combo_id FROM build_combos WHERE combo_name='Combo xào rau thập cẩm' AND shop_owner_id=@shop1_id);
SET @c4_s1=(SELECT combo_id FROM build_combos WHERE combo_name='Combo detox xanh buổi sáng' AND shop_owner_id=@shop1_id);
SET @c5_s1=(SELECT combo_id FROM build_combos WHERE combo_name='Combo hầm rau củ bổ dưỡng' AND shop_owner_id=@shop1_id);
SET @c6_s1=(SELECT combo_id FROM build_combos WHERE combo_name='Combo trái cây tráng miệng' AND shop_owner_id=@shop1_id);
SET @c7_s1=(SELECT combo_id FROM build_combos WHERE combo_name='Combo rau sống ăn kèm' AND shop_owner_id=@shop1_id);
SET @c8_s1=(SELECT combo_id FROM build_combos WHERE combo_name='Combo nấu lẩu rau củ' AND shop_owner_id=@shop1_id);
SET @c9_s1=(SELECT combo_id FROM build_combos WHERE combo_name='Combo nấu bún bò' AND shop_owner_id=@shop1_id);
SET @c10_s1=(SELECT combo_id FROM build_combos WHERE combo_name='Combo làm gỏi cuốn' AND shop_owner_id=@shop1_id);
SET @c11_s1=(SELECT combo_id FROM build_combos WHERE combo_name='Combo sinh tố rau củ' AND shop_owner_id=@shop1_id);
SET @c12_s1=(SELECT combo_id FROM build_combos WHERE combo_name='Combo cơm chiên rau củ' AND shop_owner_id=@shop1_id);
SET @c13_s1=(SELECT combo_id FROM build_combos WHERE combo_name='Combo nấu canh ngót' AND shop_owner_id=@shop1_id);
SET @c14_s1=(SELECT combo_id FROM build_combos WHERE combo_name='Combo ăn kiêng eat clean' AND shop_owner_id=@shop1_id);
SET @c1_s2=(SELECT combo_id FROM build_combos WHERE combo_name='Combo canh rau ngọt' AND shop_owner_id=@shop2_id);
SET @c2_s2=(SELECT combo_id FROM build_combos WHERE combo_name='Combo giải nhiệt mùa hè' AND shop_owner_id=@shop2_id);
SET @c3_s2=(SELECT combo_id FROM build_combos WHERE combo_name='Combo rau mầm healthy' AND shop_owner_id=@shop2_id);
SET @c4_s2=(SELECT combo_id FROM build_combos WHERE combo_name='Combo xào cà tím hành tỏi' AND shop_owner_id=@shop2_id);
SET @c5_s2=(SELECT combo_id FROM build_combos WHERE combo_name='Combo trái cây nhiệt đới' AND shop_owner_id=@shop2_id);
SET @c6_s2=(SELECT combo_id FROM build_combos WHERE combo_name='Combo súp bí đao bổ mát' AND shop_owner_id=@shop2_id);
SET @c7_s2=(SELECT combo_id FROM build_combos WHERE combo_name='Combo gia vị tươi cần thiết' AND shop_owner_id=@shop2_id);
SET @c8_s2=(SELECT combo_id FROM build_combos WHERE combo_name='Combo nước ép hoa quả' AND shop_owner_id=@shop2_id);
SET @c9_s2=(SELECT combo_id FROM build_combos WHERE combo_name='Combo nấu canh chua tôm' AND shop_owner_id=@shop2_id);
SET @c10_s2=(SELECT combo_id FROM build_combos WHERE combo_name='Combo lẩu rau củ chay' AND shop_owner_id=@shop2_id);
SET @c11_s2=(SELECT combo_id FROM build_combos WHERE combo_name='Combo gỏi xoài xanh' AND shop_owner_id=@shop2_id);
SET @c12_s2=(SELECT combo_id FROM build_combos WHERE combo_name='Combo rau ăn phở' AND shop_owner_id=@shop2_id);
SET @c13_s2=(SELECT combo_id FROM build_combos WHERE combo_name='Combo dưa góp ăn kèm' AND shop_owner_id=@shop2_id);
SET @c14_s2=(SELECT combo_id FROM build_combos WHERE combo_name='Combo sinh tố trái cây' AND shop_owner_id=@shop2_id);

INSERT INTO product_combo (combo_id,product_id,quantity,price) VALUES
                                                                   (@c1_s1,@p_ca_chua,2,35000),(@c1_s1,@p_rau_muong,2,12000),(@c1_s1,@p_hanh_la,1,8000),
                                                                   (@c2_s1,@p_xa_lach,1,15000),(@c2_s1,@p_dua_leo,1,15000),(@c2_s1,@p_ot_chuong,1,55000),
                                                                   (@c3_s1,@p_cai_ngot,2,15000),(@c3_s1,@p_bong_cai,1,15000),(@c3_s1,@p_ca_rot,1,25000),(@c3_s1,@p_hanh_tay,1,25000),
                                                                   (@c4_s1,@p_cai_bo_xoi,1,18000),(@c4_s1,@p_dua_leo,1,15000),(@c4_s1,@p_can_tay,1,15000),(@c4_s1,@p_gung,1,55000),
                                                                   (@c5_s1,@p_khoai_tay,1,30000),(@c5_s1,@p_ca_rot,1,25000),(@c5_s1,@p_bi_do,1,22000),(@c5_s1,@p_cu_cai,1,18000),
                                                                   (@c6_s1,@p_tao,1,110000),(@c6_s1,@p_chuoi,1,25000),(@c6_s1,@p_dau_tay,1,180000),
                                                                   (@c7_s1,@p_xa_lach,1,15000),(@c7_s1,@p_rau_mui,1,8000),(@c7_s1,@p_hanh_la,1,8000),(@c7_s1,@p_dua_leo,1,15000),
                                                                   (@c8_s1,@p_cai_ngot,2,15000),(@c8_s1,@p_bong_cai,1,15000),(@c8_s1,@p_ca_rot,1,25000),(@c8_s1,@p_muop,1,18000),
                                                                   (@c9_s1,@p_rau_muong,2,12000),(@c9_s1,@p_xa_lach,1,15000),(@c9_s1,@p_rau_mui,1,8000),
                                                                   (@c10_s1,@p_xa_lach,1,15000),(@c10_s1,@p_ca_rot,1,25000),(@c10_s1,@p_dua_leo,1,15000),
                                                                   (@c11_s1,@p_ca_rot,1,25000),(@c11_s1,@p_can_tay,1,15000),(@c11_s1,@p_gung,1,55000),
                                                                   (@c12_s1,@p_ca_rot,1,25000),(@c12_s1,@p_hanh_tay,1,25000),(@c12_s1,@p_hanh_la,1,8000),
                                                                   (@c13_s1,@p_rau_muong,2,12000),(@c13_s1,@p_hanh_la,1,8000),(@c13_s1,@p_gung,1,55000),
                                                                   (@c14_s1,@p_cai_bo_xoi,1,18000),(@c14_s1,@p_bong_cai,1,15000),(@c14_s1,@p_ca_rot,1,25000),(@c14_s1,@p_dua_leo,1,15000),
                                                                   (@c1_s2,@p2_rau_ngot,2,10000),(@c1_s2,@p2_mong_toi,2,10000),(@c1_s2,@p2_cai_thia,1,14000),
                                                                   (@c2_s2,@p2_dua_hau,2,12000),(@c2_s2,@p2_buoi,1,65000),(@c2_s2,@p2_cam,1,35000),
                                                                   (@c3_s2,@p2_rau_mam,1,20000),(@c3_s2,@p2_dau_que,1,25000),(@c3_s2,@p2_dau_bap,1,20000),
                                                                   (@c4_s2,@p2_ca_tim,1,22000),(@c4_s2,@p2_hanh_tim,1,45000),(@c4_s2,@p2_ot_do,1,50000),
                                                                   (@c5_s2,@p2_xoai,1,85000),(@c5_s2,@p2_thanh_long,1,35000),(@c5_s2,@p2_oi,1,30000),
                                                                   (@c6_s2,@p2_bi_dao,1,15000),(@c6_s2,@p2_bi_ngoi,1,18000),(@c6_s2,@p2_su_su,1,20000),
                                                                   (@c7_s2,@p2_hanh_tim,1,45000),(@c7_s2,@p2_sa,1,10000),(@c7_s2,@p2_ot_do,1,50000),
                                                                   (@c8_s2,@p2_cam,2,35000),(@c8_s2,@p2_buoi,1,65000),(@c8_s2,@p2_oi,1,30000),
                                                                   (@c9_s2,@p2_mong_toi,2,10000),(@c9_s2,@p2_dau_bap,1,20000),(@c9_s2,@p2_sa,1,10000),
                                                                   (@c10_s2,@p2_bi_dao,1,15000),(@c10_s2,@p2_su_su,1,20000),(@c10_s2,@p2_dau_bap,1,20000),(@c10_s2,@p2_cai_thia,1,14000),
                                                                   (@c11_s2,@p2_xoai,1,85000),(@c11_s2,@p2_rau_den,1,12000),(@c11_s2,@p2_tia_to,1,8000),
                                                                   (@c12_s2,@p2_rau_mam,1,20000),(@c12_s2,@p2_hung_que,1,8000),(@c12_s2,@p2_tia_to,1,8000),
                                                                   (@c13_s2,@p2_ca_tim,1,22000),(@c13_s2,@p2_hanh_tim,1,45000),(@c13_s2,@p2_ot_do,1,50000),
                                                                   (@c14_s2,@p2_xoai,1,85000),(@c14_s2,@p2_cam,1,35000),(@c14_s2,@p2_oi,1,30000);


-- ============================================================
-- 8. MYSTERY BOXES
-- ============================================================
INSERT INTO mystery_boxes (shop_owner_id,box_type,image_url,price,description,note,is_active,total_quantity,sold_quantity) VALUES
                                                                                                                               (@shop1_id,'Hộp Rau Xanh Bí Ẩn','https://images.unsplash.com/photo-1540420773420-3366772f4999?w=400&h=300&fit=crop',89000,'Hộp rau xanh tươi bí ẩn từ vườn organic Đà Lạt. Bên trong là 4-5 loại rau lá tươi ngon được chọn lọc kỹ càng.','Giao hàng trong ngày, bảo đảm tươi sạch',TRUE,20,5),
                                                                                                                               (@shop1_id,'Hộp Củ Quả Dinh Dưỡng','https://res.cloudinary.com/dfp44sbsj/image/upload/v1774253772/brands/sftnvmjw2mvym1plcpql.jpg',129000,'Hộp củ quả bí ẩn gồm 3-4 loại củ và 2-3 loại quả tươi ngon, đầy đủ vitamin và khoáng chất.','Giá trị thực tế cao hơn 30% so với giá bán',TRUE,15,3),
                                                                                                                               (@shop2_id,'Hộp Rau Miền Tây Tươi','https://images.unsplash.com/photo-1512621776951-a57141f2eefd?w=400&h=300&fit=crop',75000,'Hộp rau đặc sản miền Tây gồm 4-5 loại rau tươi xanh mát, thích hợp nấu canh và xào.','Thu hoạch buổi sáng, giao ngay trong ngày',TRUE,25,8),
                                                                                                                               (@shop2_id,'Hộp Trái Cây Nhiệt Đới','https://images.unsplash.com/photo-1619566636858-adf3ef46400b?w=400&h=300&fit=crop',149000,'Hộp trái cây nhiệt đới đặc sản miền Nam gồm 4-5 loại trái cây ngọt ngon tươi mát.','Phù hợp giải nhiệt mùa hè',TRUE,15,4);

SET @box1_s1=(SELECT mystery_id FROM mystery_boxes WHERE box_type='Hộp Rau Xanh Bí Ẩn' AND shop_owner_id=@shop1_id);
SET @box2_s1=(SELECT mystery_id FROM mystery_boxes WHERE box_type='Hộp Củ Quả Dinh Dưỡng' AND shop_owner_id=@shop1_id);
SET @box1_s2=(SELECT mystery_id FROM mystery_boxes WHERE box_type='Hộp Rau Miền Tây Tươi' AND shop_owner_id=@shop2_id);
SET @box2_s2=(SELECT mystery_id FROM mystery_boxes WHERE box_type='Hộp Trái Cây Nhiệt Đới' AND shop_owner_id=@shop2_id);

INSERT INTO product_mystery (product_id,mystery_id,quantity) VALUES
                                                                 (@p_rau_muong,@box1_s1,2),(@p_cai_ngot,@box1_s1,1),(@p_xa_lach,@box1_s1,1),(@p_cai_bo_xoi,@box1_s1,1),
                                                                 (@p_ca_rot,@box2_s1,1),(@p_khoai_tay,@box2_s1,1),(@p_bi_do,@box2_s1,1),
                                                                 (@p2_rau_ngot,@box1_s2,2),(@p2_cai_thia,@box1_s2,1),(@p2_mong_toi,@box1_s2,1),(@p2_dau_bap,@box1_s2,1),
                                                                 (@p2_dua_hau,@box2_s2,2),(@p2_thanh_long,@box2_s2,1),(@p2_xoai,@box2_s2,1);


-- ============================================================
-- 9. VOUCHERS
-- ============================================================
INSERT INTO vouchers (shop_id,voucher_code,discount_value,max_discount,min_order_value,usage_limit,expiry_date) VALUES
                                                                                                                    (@shop1_id,'RAUSACH10',10000,20000,50000,100,DATE_ADD(NOW(),INTERVAL 30 DAY)),
                                                                                                                    (@shop1_id,'ORGANIC20',20000,40000,100000,50,DATE_ADD(NOW(),INTERVAL 30 DAY)),
                                                                                                                    (@shop1_id,'DALAT50K',50000,50000,200000,20,DATE_ADD(NOW(),INTERVAL 15 DAY)),
                                                                                                                    (@shop1_id,'FREESHIP1',15000,15000,80000,200,DATE_ADD(NOW(),INTERVAL 60 DAY)),
                                                                                                                    (@shop2_id,'VUONXANH15',15000,30000,70000,100,DATE_ADD(NOW(),INTERVAL 30 DAY)),
                                                                                                                    (@shop2_id,'MIENTAY25',25000,50000,120000,50,DATE_ADD(NOW(),INTERVAL 30 DAY)),
                                                                                                                    (@shop2_id,'TRAICAY30',30000,30000,150000,30,DATE_ADD(NOW(),INTERVAL 20 DAY)),
                                                                                                                    (@shop2_id,'SHIP0DONG',20000,20000,100000,150,DATE_ADD(NOW(),INTERVAL 45 DAY));


-- ============================================================
-- 10. ORDERS
-- ============================================================
INSERT INTO orders (buyer_id,shop_owner_id,shipper_id,shipping_fee,recipient_name,recipient_phone,shipping_address,shipping_latitude,shipping_longitude,shop_latitude,shop_longitude,note,status,total_amount,payment_method,created_at) VALUES
                                                                                                                                                                                                                                             (@buyer1_id,@shop1_id,NULL,15000,'Ngô Gia Sang','0911111111','Đ. Võ Trường Toản, Linh Trung, Thủ Đức, TP.HCM',10.8651,106.7752,10.8697,106.7717,'Giao buổi sáng trước 10h','PENDING',95000,'COD',DATE_SUB(NOW(),INTERVAL 2 HOUR)),
                                                                                                                                                                                                                                             (@buyer1_id,@shop1_id,NULL,15000,'Ngô Gia Sang','0911111111','Đ. Võ Trường Toản, Linh Trung, Thủ Đức, TP.HCM',10.8651,106.7752,10.8697,106.7717,'Để trước cửa nếu vắng nhà','PENDING',125000,'PAYOS',DATE_SUB(NOW(),INTERVAL 1 HOUR)),
                                                                                                                                                                                                                                             (@buyer1_id,@shop2_id,NULL,15000,'Ngô Gia Sang','0911111111','A15 Tiền Giang, Đông Hòa, Dĩ An, Bình Dương',10.8978,106.7731,10.9021,106.7698,'Gọi trước khi giao 30 phút','PENDING',87000,'COD',DATE_SUB(NOW(),INTERVAL 30 MINUTE)),
                                                                                                                                                                                                                                             (@buyer1_id,@shop2_id,NULL,15000,'Ngô Gia Sang','0911111111','A15 Tiền Giang, Đông Hòa, Dĩ An, Bình Dương',10.8978,106.7731,10.9021,106.7698,NULL,'PENDING',155000,'PAYOS',DATE_SUB(NOW(),INTERVAL 15 MINUTE));

SET @op1=(SELECT order_id FROM orders WHERE status='PENDING' AND shop_owner_id=@shop1_id ORDER BY created_at ASC LIMIT 1);
SET @op2=(SELECT order_id FROM orders WHERE status='PENDING' AND shop_owner_id=@shop1_id ORDER BY created_at DESC LIMIT 1);
SET @op3=(SELECT order_id FROM orders WHERE status='PENDING' AND shop_owner_id=@shop2_id ORDER BY created_at ASC LIMIT 1);
SET @op4=(SELECT order_id FROM orders WHERE status='PENDING' AND shop_owner_id=@shop2_id ORDER BY created_at DESC LIMIT 1);

INSERT INTO order_details (order_id,product_id,quantity,unit_price,is_requested_return) VALUES
                                                                                            (@op1,@p_rau_muong,3,12000,0),(@op1,@p_cai_ngot,2,15000,0),(@op1,@p_ca_rot,1,25000,0),
                                                                                            (@op2,@p_bong_cai,2,15000,0),(@op2,@p_xa_lach,2,15000,0),(@op2,@p_dua_leo,2,15000,0),
                                                                                            (@op3,@p2_rau_ngot,3,10000,0),(@op3,@p2_cai_thia,2,14000,0),(@op3,@p2_dau_bap,1,20000,0),
                                                                                            (@op4,@p2_dua_hau,3,12000,0),(@op4,@p2_thanh_long,1,35000,0),(@op4,@p2_xoai,1,85000,0);

INSERT INTO orders (buyer_id,shop_owner_id,shipper_id,shipping_fee,recipient_name,recipient_phone,shipping_address,shipping_latitude,shipping_longitude,shop_latitude,shop_longitude,note,status,total_amount,payment_method,created_at) VALUES
                                                                                                                                                                                                                                             (@buyer1_id,@shop1_id,NULL,15000,'Ngô Gia Sang','0911111111','Đ. Võ Trường Toản, Linh Trung, Thủ Đức, TP.HCM',10.8651,106.7752,10.8697,106.7717,'Rau tươi ngon nhé shop','CONFIRMED',112000,'COD',DATE_SUB(NOW(),INTERVAL 3 HOUR)),
                                                                                                                                                                                                                                             (@buyer1_id,@shop1_id,NULL,15000,'Ngô Gia Sang','0911111111','Đ. Võ Trường Toản, Linh Trung, Thủ Đức, TP.HCM',10.8651,106.7752,10.8697,106.7717,NULL,'CONFIRMED',198000,'PAYOS',DATE_SUB(NOW(),INTERVAL 4 HOUR)),
                                                                                                                                                                                                                                             (@buyer1_id,@shop2_id,NULL,15000,'Ngô Gia Sang','0911111111','A15 Tiền Giang, Đông Hòa, Dĩ An, Bình Dương',10.8978,106.7731,10.9021,106.7698,'Trái cây chín tự nhiên nhé','CONFIRMED',163000,'COD',DATE_SUB(NOW(),INTERVAL 5 HOUR)),
                                                                                                                                                                                                                                             (@buyer1_id,@shop2_id,NULL,15000,'Ngô Gia Sang','0911111111','A15 Tiền Giang, Đông Hòa, Dĩ An, Bình Dương',10.8978,106.7731,10.9021,106.7698,NULL,'CONFIRMED',145000,'PAYOS',DATE_SUB(NOW(),INTERVAL 6 HOUR));

SET @oc1=(SELECT order_id FROM orders WHERE status='CONFIRMED' AND shop_owner_id=@shop1_id ORDER BY created_at DESC LIMIT 1 OFFSET 1);
SET @oc2=(SELECT order_id FROM orders WHERE status='CONFIRMED' AND shop_owner_id=@shop1_id ORDER BY created_at DESC LIMIT 1);
SET @oc3=(SELECT order_id FROM orders WHERE status='CONFIRMED' AND shop_owner_id=@shop2_id ORDER BY created_at DESC LIMIT 1 OFFSET 1);
SET @oc4=(SELECT order_id FROM orders WHERE status='CONFIRMED' AND shop_owner_id=@shop2_id ORDER BY created_at DESC LIMIT 1);

INSERT INTO order_details (order_id,product_id,quantity,unit_price,is_requested_return) VALUES
                                                                                            (@oc1,@p_cai_bo_xoi,2,18000,0),(@oc1,@p_hanh_tay,2,25000,0),(@oc1,@p_can_tay,2,15000,0),
                                                                                            (@oc2,@p_dau_tay,1,180000,0),(@oc2,@p_tao,1,110000,0),
                                                                                            (@oc3,@p2_cam,2,35000,0),(@oc3,@p2_buoi,1,65000,0),(@oc3,@p2_dua_hau,1,12000,0),
                                                                                            (@oc4,@p2_ca_tim,2,22000,0),(@oc4,@p2_hanh_tim,1,45000,0);

INSERT INTO orders (buyer_id,shop_owner_id,shipper_id,shipping_fee,recipient_name,recipient_phone,shipping_address,shipping_latitude,shipping_longitude,shop_latitude,shop_longitude,note,status,total_amount,payment_method,created_at) VALUES
                                                                                                                                                                                                                                             (@buyer1_id,@shop1_id,@shipper1_id,15000,'Ngô Gia Sang','0911111111','Đ. Võ Trường Toản, Linh Trung, Thủ Đức, TP.HCM',10.8651,106.7752,10.8697,106.7717,'Giao nhanh giúp mình','SHIPPING',135000,'COD',DATE_SUB(NOW(),INTERVAL 2 HOUR)),
                                                                                                                                                                                                                                             (@buyer1_id,@shop1_id,@shipper1_id,15000,'Ngô Gia Sang','0911111111','Đ. Võ Trường Toản, Linh Trung, Thủ Đức, TP.HCM',10.8651,106.7752,10.8697,106.7717,NULL,'SHIPPING',225000,'PAYOS',DATE_SUB(NOW(),INTERVAL 3 HOUR)),
                                                                                                                                                                                                                                             (@buyer1_id,@shop2_id,@shipper1_id,15000,'Ngô Gia Sang','0911111111','A15 Tiền Giang, Đông Hòa, Dĩ An, Bình Dương',10.8978,106.7731,10.9021,106.7698,'Cẩn thận trái cây dễ dập','SHIPPING',172000,'COD',DATE_SUB(NOW(),INTERVAL 1 HOUR)),
                                                                                                                                                                                                                                             (@buyer1_id,@shop2_id,@shipper1_id,15000,'Ngô Gia Sang','0911111111','A15 Tiền Giang, Đông Hòa, Dĩ An, Bình Dương',10.8978,106.7731,10.9021,106.7698,NULL,'SHIPPING',188000,'PAYOS',DATE_SUB(NOW(),INTERVAL 90 MINUTE));

SET @os1=(SELECT order_id FROM orders WHERE status='SHIPPING' AND shop_owner_id=@shop1_id ORDER BY created_at DESC LIMIT 1 OFFSET 1);
SET @os2=(SELECT order_id FROM orders WHERE status='SHIPPING' AND shop_owner_id=@shop1_id ORDER BY created_at DESC LIMIT 1);
SET @os3=(SELECT order_id FROM orders WHERE status='SHIPPING' AND shop_owner_id=@shop2_id ORDER BY created_at DESC LIMIT 1 OFFSET 1);
SET @os4=(SELECT order_id FROM orders WHERE status='SHIPPING' AND shop_owner_id=@shop2_id ORDER BY created_at DESC LIMIT 1);

INSERT INTO order_details (order_id,product_id,quantity,unit_price,is_requested_return) VALUES
                                                                                            (@os1,@p_rau_muong,4,12000,0),(@os1,@p_cai_ngot,3,15000,0),(@os1,@p_bong_cai,2,15000,0),
                                                                                            (@os2,@p_ca_rot,2,25000,0),(@os2,@p_khoai_tay,2,30000,0),(@os2,@p_bi_do,2,22000,0),
                                                                                            (@os3,@p2_xoai,2,85000,0),(@os3,@p2_thanh_long,1,35000,0),
                                                                                            (@os4,@p2_cam,3,35000,0),(@os4,@p2_buoi,2,65000,0);

INSERT IGNORE INTO shipper_locations (shipper_id,order_id,latitude,longitude,updated_at)
VALUES (@shipper1_id,@os1,10.8670,106.7730,NOW());

INSERT INTO orders (buyer_id,shop_owner_id,shipper_id,shipping_fee,recipient_name,recipient_phone,shipping_address,shipping_latitude,shipping_longitude,shop_latitude,shop_longitude,note,status,total_amount,payment_method,created_at) VALUES
                                                                                                                                                                                                                                             (@buyer1_id,@shop1_id,@shipper1_id,15000,'Ngô Gia Sang','0911111111','Đ. Võ Trường Toản, Linh Trung, Thủ Đức, TP.HCM',10.8651,106.7752,10.8697,106.7717,NULL,'DELIVERED',183000,'COD',DATE_SUB(NOW(),INTERVAL 1 DAY)),
                                                                                                                                                                                                                                             (@buyer1_id,@shop1_id,@shipper1_id,15000,'Ngô Gia Sang','0911111111','Đ. Võ Trường Toản, Linh Trung, Thủ Đức, TP.HCM',10.8651,106.7752,10.8697,106.7717,'Rau rất tươi, sẽ mua lại','DELIVERED',245000,'PAYOS',DATE_SUB(NOW(),INTERVAL 2 DAY)),
                                                                                                                                                                                                                                             (@buyer1_id,@shop2_id,@shipper1_id,15000,'Ngô Gia Sang','0911111111','A15 Tiền Giang, Đông Hòa, Dĩ An, Bình Dương',10.8978,106.7731,10.9021,106.7698,NULL,'DELIVERED',162000,'COD',DATE_SUB(NOW(),INTERVAL 3 DAY)),
                                                                                                                                                                                                                                             (@buyer1_id,@shop2_id,@shipper1_id,15000,'Ngô Gia Sang','0911111111','A15 Tiền Giang, Đông Hòa, Dĩ An, Bình Dương',10.8978,106.7731,10.9021,106.7698,NULL,'DELIVERED',217000,'PAYOS',DATE_SUB(NOW(),INTERVAL 4 DAY));

SET @od1=(SELECT order_id FROM orders WHERE status='DELIVERED' AND shop_owner_id=@shop1_id ORDER BY created_at DESC LIMIT 1 OFFSET 1);
SET @od2=(SELECT order_id FROM orders WHERE status='DELIVERED' AND shop_owner_id=@shop1_id ORDER BY created_at DESC LIMIT 1);
SET @od3=(SELECT order_id FROM orders WHERE status='DELIVERED' AND shop_owner_id=@shop2_id ORDER BY created_at DESC LIMIT 1 OFFSET 1);
SET @od4=(SELECT order_id FROM orders WHERE status='DELIVERED' AND shop_owner_id=@shop2_id ORDER BY created_at DESC LIMIT 1);

INSERT INTO order_details (order_id,product_id,quantity,unit_price,is_requested_return) VALUES
                                                                                            (@od1,@p_rau_muong,5,12000,0),(@od1,@p_cai_ngot,3,15000,0),(@od1,@p_ca_rot,2,25000,0),
                                                                                            (@od2,@p_dau_tay,1,180000,0),(@od2,@p_tao,1,110000,0),(@od2,@p_chuoi,1,25000,0),
                                                                                            (@od3,@p2_cam,2,35000,0),(@od3,@p2_dua_hau,3,12000,0),(@od3,@p2_thanh_long,1,35000,0),
                                                                                            (@od4,@p2_xoai,2,85000,0),(@od4,@p2_buoi,1,65000,0),(@od4,@p2_cam,1,35000,0);

INSERT INTO orders (buyer_id,shop_owner_id,shipper_id,shipping_fee,recipient_name,recipient_phone,shipping_address,shipping_latitude,shipping_longitude,shop_latitude,shop_longitude,note,status,total_amount,payment_method,created_at) VALUES
                                                                                                                                                                                                                                             (@buyer1_id,@shop1_id,NULL,15000,'Ngô Gia Sang','0911111111','Đ. Võ Trường Toản, Linh Trung, Thủ Đức, TP.HCM',10.8651,106.7752,10.8697,106.7717,'Đặt nhầm sản phẩm','CANCELLED',75000,'COD',DATE_SUB(NOW(),INTERVAL 5 DAY)),
                                                                                                                                                                                                                                             (@buyer1_id,@shop2_id,NULL,15000,'Ngô Gia Sang','0911111111','A15 Tiền Giang, Đông Hòa, Dĩ An, Bình Dương',10.8978,106.7731,10.9021,106.7698,'Thay đổi kế hoạch','CANCELLED',92000,'COD',DATE_SUB(NOW(),INTERVAL 6 DAY)),
                                                                                                                                                                                                                                             (@buyer1_id,@shop1_id,NULL,15000,'Ngô Gia Sang','0911111111','Đ. Võ Trường Toản, Linh Trung, Thủ Đức, TP.HCM',10.8651,106.7752,10.8697,106.7717,NULL,'CANCELLED',115000,'PAYOS',DATE_SUB(NOW(),INTERVAL 7 DAY)),
                                                                                                                                                                                                                                             (@buyer1_id,@shop1_id,@shipper1_id,15000,'Ngô Gia Sang','0911111111','Đ. Võ Trường Toản, Linh Trung, Thủ Đức, TP.HCM',10.8651,106.7752,10.8697,106.7717,'Không có nhà','FAILED',95000,'COD',DATE_SUB(NOW(),INTERVAL 8 DAY)),
                                                                                                                                                                                                                                             (@buyer1_id,@shop2_id,@shipper1_id,15000,'Ngô Gia Sang','0911111111','A15 Tiền Giang, Đông Hòa, Dĩ An, Bình Dương',10.8978,106.7731,10.9021,106.7698,'Địa chỉ không tìm được','FAILED',78000,'COD',DATE_SUB(NOW(),INTERVAL 9 DAY)),
                                                                                                                                                                                                                                             (@buyer1_id,@shop1_id,@shipper1_id,15000,'Ngô Gia Sang','0911111111','Đ. Võ Trường Toản, Linh Trung, Thủ Đức, TP.HCM',10.8651,106.7752,10.8697,106.7717,NULL,'FAILED',125000,'COD',DATE_SUB(NOW(),INTERVAL 10 DAY));

SET @oca1=(SELECT order_id FROM orders WHERE status='CANCELLED' AND shop_owner_id=@shop1_id ORDER BY created_at DESC LIMIT 1 OFFSET 1);
SET @oca2=(SELECT order_id FROM orders WHERE status='CANCELLED' AND shop_owner_id=@shop2_id LIMIT 1);
SET @oca3=(SELECT order_id FROM orders WHERE status='CANCELLED' AND shop_owner_id=@shop1_id ORDER BY created_at DESC LIMIT 1);
SET @of1=(SELECT order_id FROM orders WHERE status='FAILED' AND shop_owner_id=@shop1_id ORDER BY created_at DESC LIMIT 1 OFFSET 1);
SET @of2=(SELECT order_id FROM orders WHERE status='FAILED' AND shop_owner_id=@shop2_id LIMIT 1);
SET @of3=(SELECT order_id FROM orders WHERE status='FAILED' AND shop_owner_id=@shop1_id ORDER BY created_at DESC LIMIT 1);

INSERT INTO order_details (order_id,product_id,quantity,unit_price,is_requested_return) VALUES
                                                                                            (@oca1,@p_rau_muong,3,12000,0),(@oca1,@p_xa_lach,2,15000,0),
                                                                                            (@oca2,@p2_cam,2,35000,0),(@oca2,@p2_dua_hau,1,12000,0),
                                                                                            (@oca3,@p_bong_cai,3,15000,0),(@oca3,@p_ca_rot,2,25000,0),
                                                                                            (@of1,@p_rau_muong,4,12000,0),(@of1,@p_cai_ngot,2,15000,0),
                                                                                            (@of2,@p2_cam,1,35000,0),(@of2,@p2_thanh_long,1,35000,0),
                                                                                            (@of3,@p_bong_cai,3,15000,0),(@of3,@p_hanh_tay,2,25000,0);


-- ============================================================
-- 11. REVIEWS
-- ============================================================
SET @ood1=(SELECT order_detail_id FROM order_details WHERE order_id=@od1 LIMIT 1);
SET @ood2=(SELECT order_detail_id FROM order_details WHERE order_id=@od1 LIMIT 1 OFFSET 1);
SET @ood3=(SELECT order_detail_id FROM order_details WHERE order_id=@od2 LIMIT 1);
SET @ood4=(SELECT order_detail_id FROM order_details WHERE order_id=@od3 LIMIT 1);
SET @ood5=(SELECT order_detail_id FROM order_details WHERE order_id=@od4 LIMIT 1);

INSERT INTO reviews (buyer_id,shop_owner_id,order_detail_id,rating_star,comment,evidence,reply_from_shop) VALUES
                                                                                                              (@buyer1_id,@shop1_id,@ood1,5.0,'Rau muống rất tươi, xanh mướt! Đóng gói cẩn thận, giao nhanh. Sẽ ủng hộ thường xuyên!','https://images.unsplash.com/photo-1540420773420-3366772f4999?w=400&h=300&fit=crop','Cảm ơn bạn đã tin tưởng Nông Sản Xấu Mã! 🌿'),
                                                                                                              (@buyer1_id,@shop1_id,@ood2,4.0,'Cải ngọt tươi non, cà rốt to đều, ngọt tự nhiên. Hài lòng!',NULL,'Cảm ơn bạn đã đánh giá! Chúng tôi sẽ cố gắng hoàn thiện hơn!'),
                                                                                                              (@buyer1_id,@shop1_id,@ood3,5.0,'Dâu tây Đà Lạt tuyệt vời! Táo giòn ngọt. Đáng tiền lắm!','https://images.unsplash.com/photo-1464965911861-746a04b4bca6?w=400&h=300&fit=crop','Dâu tây nhập trực tiếp từ Đà Lạt mỗi sáng 🍓'),
                                                                                                              (@buyer1_id,@shop2_id,@ood4,4.5,'Cam ngọt lịm, dưa hấu đỏ au. Đóng gói cẩn thận không bị dập.',NULL,'Cảm ơn bạn đã ủng hộ Vườn Xanh Bình Dương! 🍊'),
                                                                                                              (@buyer1_id,@shop2_id,@ood5,5.0,'Xoài thơm lừng, bưởi không hạt, cam ngon xuất sắc!','https://images.unsplash.com/photo-1553279768-865429fa0078?w=400&h=300&fit=crop','Đây đều là đặc sản miền Tây chính gốc! 🌟');

UPDATE users SET rating_average=4.8 WHERE id=@shop1_id;
UPDATE users SET rating_average=4.7 WHERE id=@shop2_id;


-- ============================================================
-- 12. PAYMENTS
-- ============================================================
INSERT INTO payments (order_id,payment_date,amount,payment_gateway,status,created_at,updated_at,payos_order_code) VALUES
                                                                                                                      (@od2,DATE_SUB(NOW(),INTERVAL 2 DAY),245000,'PAYOS','SUCCESS',DATE_SUB(NOW(),INTERVAL 2 DAY),DATE_SUB(NOW(),INTERVAL 2 DAY),'FOOD20240001'),
                                                                                                                      (@od4,DATE_SUB(NOW(),INTERVAL 4 DAY),217000,'PAYOS','SUCCESS',DATE_SUB(NOW(),INTERVAL 4 DAY),DATE_SUB(NOW(),INTERVAL 4 DAY),'FOOD20240002'),
                                                                                                                      (@os2,NULL,225000,'PAYOS','PENDING',DATE_SUB(NOW(),INTERVAL 3 HOUR),NOW(),'FOOD20240003'),
                                                                                                                      (@os4,NULL,188000,'PAYOS','PENDING',DATE_SUB(NOW(),INTERVAL 90 MINUTE),NOW(),'FOOD20240004'),
                                                                                                                      (@op2,NULL,125000,'PAYOS','PENDING',DATE_SUB(NOW(),INTERVAL 1 HOUR),NOW(),'FOOD20240005');

SET @payment_success1=(SELECT payment_id FROM payments WHERE payos_order_code='FOOD20240001' LIMIT 1);
SET @payment_success2=(SELECT payment_id FROM payments WHERE payos_order_code='FOOD20240002' LIMIT 1);
SET @payment_pending3=(SELECT payment_id FROM payments WHERE payos_order_code='FOOD20240003' LIMIT 1);
SET @payment_pending4=(SELECT payment_id FROM payments WHERE payos_order_code='FOOD20240004' LIMIT 1);
SET @payment_pending5=(SELECT payment_id FROM payments WHERE payos_order_code='FOOD20240005' LIMIT 1);


-- ============================================================
-- 13. WITHDRAW REQUESTS
-- ============================================================
INSERT INTO withdraw_requests (admin_id,wallet_id,reason,admin_note,created_at,processed_at,status,amount,fee,receive_amount,bank_name,bank_account_number,bank_account_holder,payout_provider,payout_status) VALUES
                                                                                                                                                                                                                  (@admin_id,@wallet_shop1_id,'Rút tiền doanh thu tháng này','Đã xác nhận, đang xử lý',DATE_SUB(NOW(),INTERVAL 1 DAY),DATE_SUB(NOW(),INTERVAL 12 HOUR),'APPROVED',200000,5000,195000,'Vietcombank','0071000123456','TRAN VAN KIET','PAYOS','PENDING'),
                                                                                                                                                                                                                  (@admin_id,@wallet_shop2_id,'Rút tiền tuần',NULL,DATE_SUB(NOW(),INTERVAL 2 HOUR),NULL,'PENDING',150000,3750,146250,'Techcombank','19034567890123','LE THI NGOC','PAYOS','CREATED'),
                                                                                                                                                                                                                  (@admin_id,@wallet_shop1_id,'Rút tiền tuần trước','Đã chuyển khoản thành công',DATE_SUB(NOW(),INTERVAL 7 DAY),DATE_SUB(NOW(),INTERVAL 6 DAY),'SUCCESS',128000,3200,124800,'Vietcombank','0071000123456','TRAN VAN KIET','PAYOS','SUCCESS');


-- ============================================================
-- 14. NOTIFICATIONS
-- ============================================================
INSERT INTO notifications (admin_id,title,message,evidence,create_at,receiver_type) VALUES
                                                                                        (@admin_id,'🌿 Chào mừng đến Food Market!','Chào mừng bạn đến với Food Market - nền tảng mua sắm rau củ quả sạch hàng đầu! Khám phá hàng trăm sản phẩm tươi ngon từ các vườn rau organic uy tín.',NULL,NOW(),'BUYER'),
                                                                                        (@admin_id,'🎉 Khuyến mãi tháng này - Giảm 20% rau củ','Tháng này Food Market có chương trình khuyến mãi đặc biệt! Giảm 20% tất cả sản phẩm rau củ khi đặt đơn từ 100.000đ. Đừng bỏ lỡ!','https://images.unsplash.com/photo-1540420773420-3366772f4999?w=800&h=400&fit=crop',DATE_SUB(NOW(),INTERVAL 1 DAY),'BUYER'),
                                                                                        (@admin_id,'📦 Cập nhật chính sách giao hàng','Food Market vừa cập nhật: Miễn phí ship cho đơn từ 200.000đ. Giao hàng trong 2-4 giờ tại TP.HCM và Bình Dương.',NULL,DATE_SUB(NOW(),INTERVAL 2 DAY),'BUYER,SHOP_OWNER'),
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
-- 15. BLOGS
-- ============================================================
INSERT INTO blogs (admin_id,title,content,picture_url,status,category,create_at) VALUES
                                                                                     (@admin_id,'Mẹo chọn rau củ quả tươi ngon không hóa chất','Việc lựa chọn rau củ quả sạch là ưu tiên hàng đầu.\n\n1. Màu sắc tự nhiên, không bóng bẩy.\n2. Rau lá tươi giòn, không vàng úa.\n3. Có thể có lỗ sâu nhỏ - ít thuốc BVTV.\n4. Ngửi thấy mùi tự nhiên đặc trưng.','https://images.unsplash.com/photo-1542838132-92c53300491e?w=800&h=400&fit=crop','PUBLISHED','Kiến thức',NOW()),
                                                                                     (@admin_id,'Thực đơn detox rau củ 7 ngày cho người bận rộn','Detox bằng rau củ quả không cần phức tạp:\n- Sáng: Nước ép cần tây + cà rốt + táo\n- Trưa: Salad xà lách + cà chua + dưa leo\n- Tối: Canh rau ngọt với bông cải xanh','https://res.cloudinary.com/dfp44sbsj/image/upload/v1773920858/brands/a23bsuby6hhu2ty2r3bb.jpg','PUBLISHED','Sức khỏe',NOW()),
                                                                                     (@admin_id,'Trái cây miền Nam theo mùa - Nên mua gì?','Mùa hè là thời điểm vàng của trái cây Nam Bộ:\n- Xoài cát Hòa Lộc: ngọt đậm thơm lừng\n- Thanh long ruột đỏ: mọng nước bổ dưỡng\n- Bưởi da xanh: thanh ngọt không hạt\n- Ổi ruột đỏ: giòn ngọt vitamin C cao','https://res.cloudinary.com/dfp44sbsj/image/upload/v1773921072/brands/zhzaxj3de5vhhuw2ankp.jpg','PUBLISHED','Tin tức',NOW());


-- ============================================================
-- 16. BOT KNOWLEDGE
-- ============================================================
INSERT INTO bot_knowledge (type,title,content,keywords,active) VALUES

                                                                   ('POLICY','Chính sách giao hàng',
                                                                    'Food Market hỗ trợ giao hàng toàn quốc.\n- Nội thành TP.HCM và Bình Dương: giao trong 2-4 giờ.\n- Tỉnh thành khác: 1-3 ngày làm việc.\n- Phí giao hàng: 15.000đ - 30.000đ tùy khu vực.\n- Miễn phí giao hàng cho đơn từ 200.000đ trở lên.\n- Rau củ quả tươi được đóng gói lạnh giữ độ tươi.',
                                                                    'giao hàng, ship, phí ship, vận chuyển, bao lâu, miễn phí ship',TRUE),

                                                                   ('POLICY','Chính sách đổi trả và hoàn tiền',
                                                                    'Food Market cam kết chất lượng sản phẩm:\n- Đổi trả miễn phí trong 24 giờ nếu không tươi, dập nát hoặc không đúng mô tả.\n- Hoàn tiền 100% nếu sản phẩm bị lỗi do shop hoặc shipper.\n- Liên hệ hotline hoặc chat trực tiếp để được hỗ trợ.\n- Chụp ảnh sản phẩm lỗi gửi kèm để xử lý nhanh hơn.',
                                                                    'đổi trả, hoàn tiền, lỗi, không tươi, dập, chất lượng, khiếu nại',TRUE),

                                                                   ('POLICY','Phương thức thanh toán',
                                                                    'Food Market hỗ trợ nhiều hình thức thanh toán:\n- COD: Thanh toán tiền mặt khi nhận hàng.\n- Chuyển khoản ngân hàng.\n- PayOS: Quét mã QR thanh toán nhanh.',
                                                                    'thanh toán, COD, chuyển khoản, PayOS, QR',TRUE),

                                                                   ('POLICY','Chính sách chất lượng sản phẩm',
                                                                    'Food Market đảm bảo chất lượng nghiêm ngặt:\n- Tất cả sản phẩm có nguồn gốc xuất xứ rõ ràng.\n- Ưu tiên sản phẩm đạt chuẩn VietGAP, GlobalGAP hoặc Organic.\n- Kiểm tra chất lượng trước khi đưa lên sàn.\n- Shop vi phạm chất lượng sẽ bị xử lý theo quy định.',
                                                                    'chất lượng, organic, VietGAP, an toàn, nguồn gốc, xuất xứ',TRUE),

                                                                   ('FAQ','Làm thế nào để đặt hàng?',
                                                                    'Đặt hàng trên Food Market rất đơn giản:\n1. Tìm kiếm sản phẩm hoặc duyệt theo danh mục.\n2. Chọn sản phẩm và thêm vào giỏ hàng.\n3. Điền địa chỉ giao hàng và chọn thanh toán.\n4. Xác nhận đơn hàng và chờ shop xác nhận.\n5. Shipper sẽ giao hàng trong 2-4 giờ.',
                                                                    'đặt hàng, mua hàng, giỏ hàng, checkout, order',TRUE),

                                                                   ('FAQ','Làm thế nào để theo dõi đơn hàng?',
                                                                    'Bạn có thể theo dõi đơn hàng:\n- Vào mục "Đơn hàng của tôi" trong tài khoản.\n- Xem trạng thái: Chờ xác nhận → Đã xác nhận → Đang giao → Đã giao.\n- Khi shipper đang giao, có thể xem GPS thời gian thực.\n- Nhận thông báo cập nhật trạng thái qua app.',
                                                                    'theo dõi đơn, trạng thái đơn hàng, GPS, shipper đang giao',TRUE),

                                                                   ('FAQ','Làm thế nào để bảo quản rau củ quả tươi lâu?',
                                                                    'Mẹo bảo quản rau củ quả:\n- Rau lá (rau muống, cải ngọt, xà lách, cải bó xôi, mồng tơi, rau ngót): Bọc khăn ẩm, ngăn mát tủ lạnh, dùng trong 3-5 ngày.\n- Củ (cà rốt, khoai tây, khoai lang, bí đỏ, bí đao, su su, cà tím): Nơi thoáng mát, tránh ánh nắng, dùng 1-2 tuần.\n- Trái cây chín (chuối, xoài, ổi, dưa hấu): Nhiệt độ phòng 3-5 ngày.\n- Trái cây chua (cam, bưởi, dâu tây, nho, thanh long): Ngăn mát 5-7 ngày.\n- Gia vị (tỏi, gừng, nghệ, sả, hành tím): Nơi khô thoáng 2-4 tuần.',
                                                                    'bảo quản, tươi lâu, tủ lạnh, để được bao lâu, hư, úa',TRUE),

                                                                   ('FAQ','Tôi muốn lên kế hoạch ăn uống thì làm thế nào?',
                                                                    'Food Market có tính năng Build Plan - Lên kế hoạch ăn uống:\n- Vào mục "Thực đơn" trên menu.\n- Chọn số người ăn, số ngày và phong cách (gia đình, giảm cân, ăn chay, bình dân).\n- Hệ thống tự gợi ý thực đơn theo ngày với combo rau củ phù hợp.\n- Nhấn "Mua tất cả" để thêm nguyên liệu vào giỏ hàng.\n- Bạn cũng có thể hỏi chatbot để được tư vấn thực đơn chi tiết.',
                                                                    'kế hoạch ăn uống, build plan, thực đơn, meal plan, gợi ý món ăn',TRUE),

                                                                   ('FAQ','Combo là gì và có tiết kiệm không?',
                                                                    'Combo là bộ rau củ quả chọn sẵn cho một món ăn cụ thể:\n- Ví dụ: Combo nấu canh chua cá, Combo salad giảm cân, Combo xào rau thập cẩm...\n- Giá combo thường tiết kiệm 10-15% so với mua lẻ từng món.\n- Mỗi combo có mô tả chi tiết nguyên liệu và gợi ý nấu ăn.\n- Chọn combo theo bữa: Sáng, Trưa, Tối hoặc Bữa phụ.\n- Vào mục Combo hoặc Build Plan để xem tất cả.',
                                                                    'combo, mua combo, nguyên liệu, combo nấu ăn, tiết kiệm, set',TRUE),

                                                                   ('FAQ','Làm thế nào để liên hệ hỗ trợ?',
                                                                    'Liên hệ Food Market qua nhiều kênh:\n- Chat với trợ lý AI ngay trên app (biểu tượng chat góc phải).\n- Nhắn tin trực tiếp với shop qua tính năng chat.\n- Email: support@foodmarket.vn\n- Hotline: 1900-xxxx (8h-22h mỗi ngày)\nThời gian phản hồi thường trong vòng 30 phút.',
                                                                    'liên hệ, hỗ trợ, hotline, chat, email, tư vấn',TRUE);


-- ============================================================
-- 17. RETURN REQUESTS
-- ============================================================
SET @od1_det=(SELECT order_detail_id FROM order_details WHERE order_id=@od1 LIMIT 1);
SET @od2_det=(SELECT order_detail_id FROM order_details WHERE order_id=@od2 LIMIT 1);
SET @od3_det=(SELECT order_detail_id FROM order_details WHERE order_id=@od3 LIMIT 1);

INSERT INTO return_requests (order_detail_id,shop_owner_id,buyer_id,reason,evidence,status,refund_amount,created_at,updated_at) VALUES
                                                                                                                                    (@od1_det,@shop1_id,@buyer1_id,'Sản phẩm không tươi như quảng cáo, bị héo nhiều khi nhận hàng','https://res.cloudinary.com/dfp44sbsj/image/upload/v1774253772/brands/sftnvmjw2mvym1plcpql.jpg','PENDING',35000,DATE_SUB(NOW(),INTERVAL 1 HOUR),DATE_SUB(NOW(),INTERVAL 1 HOUR)),
                                                                                                                                    (@od2_det,@shop1_id,@buyer1_id,'Giao nhầm loại táo, mình đặt táo Fuji nhưng giao táo thường','https://res.cloudinary.com/dfp44sbsj/image/upload/v1774253772/brands/sftnvmjw2mvym1plcpql.jpg','SHOP_APPROVED',110000,DATE_SUB(NOW(),INTERVAL 1 DAY),DATE_SUB(NOW(),INTERVAL 1 DAY)),
                                                                                                                                    (@od3_det,@shop2_id,@buyer1_id,'Cam bị dập nát do quá trình vận chuyển không cẩn thận','https://res.cloudinary.com/dfp44sbsj/image/upload/v1774253772/brands/sftnvmjw2mvym1plcpql.jpg','COMPLETED',70000,DATE_SUB(NOW(),INTERVAL 2 DAY),DATE_SUB(NOW(),INTERVAL 2 DAY));

UPDATE order_details SET is_requested_return=1 WHERE order_detail_id IN (@od1_det,@od2_det,@od3_det);


-- ============================================================
-- 18. CART + CART ITEMS
-- ============================================================
INSERT INTO cart (user_id) VALUES (@buyer1_id);
SET @cart_buyer1=(SELECT id FROM cart WHERE user_id=@buyer1_id LIMIT 1);

-- Combo aliases cần cho cart và build plans
SET @combo_salad   =(SELECT combo_id FROM build_combos WHERE combo_name='Combo salad giảm cân'       AND shop_owner_id=@shop1_id);
SET @combo_detox   =(SELECT combo_id FROM build_combos WHERE combo_name='Combo detox xanh buổi sáng' AND shop_owner_id=@shop1_id);
SET @combo_ham     =(SELECT combo_id FROM build_combos WHERE combo_name='Combo hầm rau củ bổ dưỡng'  AND shop_owner_id=@shop1_id);
SET @combo_lau     =(SELECT combo_id FROM build_combos WHERE combo_name='Combo nấu lẩu rau củ'       AND shop_owner_id=@shop1_id);
SET @combo_trai_cay=(SELECT combo_id FROM build_combos WHERE combo_name='Combo trái cây tráng miệng' AND shop_owner_id=@shop1_id);
SET @combo_xao     =(SELECT combo_id FROM build_combos WHERE combo_name='Combo xào rau thập cẩm'     AND shop_owner_id=@shop1_id);
SET @combo_eatclean=(SELECT combo_id FROM build_combos WHERE combo_name='Combo ăn kiêng eat clean'   AND shop_owner_id=@shop1_id);
SET @combo2_giam_nhiet=(SELECT combo_id FROM build_combos WHERE combo_name='Combo giải nhiệt mùa hè'  AND shop_owner_id=@shop2_id);
SET @combo2_nhiet_doi =(SELECT combo_id FROM build_combos WHERE combo_name='Combo trái cây nhiệt đới' AND shop_owner_id=@shop2_id);
SET @combo2_rau_mam   =(SELECT combo_id FROM build_combos WHERE combo_name='Combo rau mầm healthy'    AND shop_owner_id=@shop2_id);
SET @combo2_sup_bi    =(SELECT combo_id FROM build_combos WHERE combo_name='Combo súp bí đao bổ mát'  AND shop_owner_id=@shop2_id);

INSERT INTO cart_item (cart_id,product_product_id,mystery_box_id,build_combo_id,quantity) VALUES
                                                                                              (@cart_buyer1,@p_rau_muong,  NULL,NULL,3),
                                                                                              (@cart_buyer1,@p_ca_rot,     NULL,NULL,2),
                                                                                              (@cart_buyer1,@p_bong_cai,   NULL,NULL,1),
                                                                                              (@cart_buyer1,@p2_dua_hau,   NULL,NULL,2),
                                                                                              (@cart_buyer1,@p2_xoai,      NULL,NULL,1),
                                                                                              (@cart_buyer1,NULL,NULL,@combo_salad,   1),
                                                                                              (@cart_buyer1,NULL,NULL,@combo2_rau_mam,1);


-- ============================================================
-- 19. CONVERSATIONS + CHAT MESSAGES
-- ============================================================

-- Conversation 1: Buyer ↔ Shop 1
INSERT INTO conversations (room_key,user1_id,user2_id,created_at,last_message_at)
VALUES (
           CONCAT(LEAST(@buyer1_id,@shop1_id),'_',GREATEST(@buyer1_id,@shop1_id)),
           LEAST(@buyer1_id,@shop1_id),
           GREATEST(@buyer1_id,@shop1_id),
           DATE_SUB(NOW(),INTERVAL 2 DAY),
           DATE_SUB(NOW(),INTERVAL 3 HOUR)
       );
SET @conv1_id=(SELECT id FROM conversations WHERE room_key=CONCAT(LEAST(@buyer1_id,@shop1_id),'_',GREATEST(@buyer1_id,@shop1_id)) LIMIT 1);

INSERT INTO chat_messages (conversation_id,sender_id,content,sent_at,is_read) VALUES
                                                                                  (@conv1_id,@buyer1_id,'Chào shop! Bông cải xanh của shop có chứng nhận organic không vậy ạ?',           DATE_SUB(NOW(),INTERVAL 2 DAY),    TRUE),
                                                                                  (@conv1_id,@shop1_id, 'Chào bạn! Bông cải xanh của mình đạt chuẩn VietGAP, trồng tại Đà Lạt nhé bạn 🌿',DATE_SUB(NOW(),INTERVAL 2 DAY),    TRUE),
                                                                                  (@conv1_id,@buyer1_id,'Tuyệt vời! Vậy 1kg khoảng bao nhiêu cái vậy shop?',                              DATE_SUB(NOW(),INTERVAL 47 HOUR),  TRUE),
                                                                                  (@conv1_id,@shop1_id, 'Thường khoảng 3-4 cái/kg bạn nhé, mình chọn cái vừa to vừa tươi cho bạn 😊',   DATE_SUB(NOW(),INTERVAL 46 HOUR),  TRUE),
                                                                                  (@conv1_id,@buyer1_id,'Ok shop, cho mình đặt 2 cái được không? Với thêm 3 bó rau muống nữa.',           DATE_SUB(NOW(),INTERVAL 45 HOUR),  TRUE),
                                                                                  (@conv1_id,@shop1_id, 'Được bạn! Bạn đặt qua app nhé, mình xác nhận ngay. Giao trong 2-3 tiếng 🚚',   DATE_SUB(NOW(),INTERVAL 44 HOUR),  TRUE),
                                                                                  (@conv1_id,@buyer1_id,'Mình đặt rồi shop ơi, kiểm tra giúp mình nhé!',                                  DATE_SUB(NOW(),INTERVAL 43 HOUR),  TRUE),
                                                                                  (@conv1_id,@shop1_id, 'Mình thấy đơn rồi bạn! Đang chuẩn bị hàng, khoảng 30 phút nữa shipper lấy nhé.',DATE_SUB(NOW(),INTERVAL 42 HOUR),  TRUE),
                                                                                  (@conv1_id,@buyer1_id,'Cảm ơn shop nhiều! Lần sau mình sẽ tiếp tục ủng hộ 😄',                         DATE_SUB(NOW(),INTERVAL 5 HOUR),   TRUE),
                                                                                  (@conv1_id,@shop1_id, 'Cảm ơn bạn đã tin tưởng! Hàng tươi ngon đảm bảo nha 🌱',                       DATE_SUB(NOW(),INTERVAL 3 HOUR),   FALSE);

-- Conversation 2: Buyer ↔ Shop 2
INSERT INTO conversations (room_key,user1_id,user2_id,created_at,last_message_at)
VALUES (
           CONCAT(LEAST(@buyer1_id,@shop2_id),'_',GREATEST(@buyer1_id,@shop2_id)),
           LEAST(@buyer1_id,@shop2_id),
           GREATEST(@buyer1_id,@shop2_id),
           DATE_SUB(NOW(),INTERVAL 1 DAY),
           DATE_SUB(NOW(),INTERVAL 1 HOUR)
       );
SET @conv2_id=(SELECT id FROM conversations WHERE room_key=CONCAT(LEAST(@buyer1_id,@shop2_id),'_',GREATEST(@buyer1_id,@shop2_id)) LIMIT 1);

INSERT INTO chat_messages (conversation_id,sender_id,content,sent_at,is_read) VALUES
                                                                                  (@conv2_id,@buyer1_id,'Shop ơi, xoài cát Hòa Lộc hiện tại còn hàng không ạ?',                             DATE_SUB(NOW(),INTERVAL 1 DAY),   TRUE),
                                                                                  (@conv2_id,@shop2_id, 'Còn hàng bạn ơi! Xoài đang vào mùa, ngọt lắm. Hiện còn khoảng 25kg 🥭',           DATE_SUB(NOW(),INTERVAL 23 HOUR), TRUE),
                                                                                  (@conv2_id,@buyer1_id,'Ngon quá! Shop cho mình hỏi là xoài có chín cây không hay chín ép vậy?',            DATE_SUB(NOW(),INTERVAL 23 HOUR), TRUE),
                                                                                  (@conv2_id,@shop2_id, 'Toàn chín cây bạn nhé! Shop mình lấy thẳng từ vườn Tiền Giang, cam kết không ép 🌿',DATE_SUB(NOW(),INTERVAL 22 HOUR), TRUE),
                                                                                  (@conv2_id,@buyer1_id,'Oke shop, mình đặt 2kg xoài với 1kg bưởi da xanh nhé!',                            DATE_SUB(NOW(),INTERVAL 22 HOUR), TRUE),
                                                                                  (@conv2_id,@shop2_id, 'Đặt ngay trên app bạn nhé! Bưởi của mình cũng đang tươi ngon lắm 🍊',             DATE_SUB(NOW(),INTERVAL 21 HOUR), TRUE),
                                                                                  (@conv2_id,@buyer1_id,'Shop có combo trái cây nào phù hợp cho gia đình 4 người không?',                    DATE_SUB(NOW(),INTERVAL 2 HOUR),  TRUE),
                                                                                  (@conv2_id,@shop2_id, 'Có Combo Trái Cây Nhiệt Đới bạn ơi, đủ 3 loại rất hợp gia đình. Giá 100k thôi!', DATE_SUB(NOW(),INTERVAL 90 MINUTE),TRUE),
                                                                                  (@conv2_id,@buyer1_id,'Nghe hay đó! Mình thử đặt xem sao 😊',                                              DATE_SUB(NOW(),INTERVAL 1 HOUR),  FALSE),
                                                                                  (@conv2_id,@shop2_id, 'Bạn cứ thử đi! Shop đảm bảo không thất vọng. Giao nhanh lắm nha 🚀',              DATE_SUB(NOW(),INTERVAL 1 HOUR),  FALSE);

-- Conversation 3: Buyer ↔ Admin
INSERT INTO conversations (room_key,user1_id,user2_id,created_at,last_message_at)
VALUES (
           CONCAT(LEAST(@buyer1_id,@admin_id),'_',GREATEST(@buyer1_id,@admin_id)),
           LEAST(@buyer1_id,@admin_id),
           GREATEST(@buyer1_id,@admin_id),
           DATE_SUB(NOW(),INTERVAL 3 DAY),
           DATE_SUB(NOW(),INTERVAL 2 DAY)
       );
SET @conv3_id=(SELECT id FROM conversations WHERE room_key=CONCAT(LEAST(@buyer1_id,@admin_id),'_',GREATEST(@buyer1_id,@admin_id)) LIMIT 1);

INSERT INTO chat_messages (conversation_id,sender_id,content,sent_at,is_read) VALUES
                                                                                  (@conv3_id,@buyer1_id,'Xin chào Admin, mình có đơn hàng bị giao nhầm địa chỉ, phải làm sao ạ?',              DATE_SUB(NOW(),INTERVAL 3 DAY),  TRUE),
                                                                                  (@conv3_id,@admin_id, 'Chào bạn! Bạn cung cấp mã đơn hàng cho mình kiểm tra nhé.',                           DATE_SUB(NOW(),INTERVAL 3 DAY),  TRUE),
                                                                                  (@conv3_id,@buyer1_id,'Mã đơn là FM-001234, shipper giao nhầm số nhà ạ.',                                     DATE_SUB(NOW(),INTERVAL 3 DAY),  TRUE),
                                                                                  (@conv3_id,@admin_id, 'Mình đã xem đơn rồi. Mình sẽ liên hệ shipper ngay để xử lý. Bạn chờ khoảng 15 phút nhé.',DATE_SUB(NOW(),INTERVAL 3 DAY),TRUE),
                                                                                  (@conv3_id,@admin_id, 'Đã liên hệ xong rồi bạn! Shipper sẽ quay lại giao đúng địa chỉ trong 20 phút.',       DATE_SUB(NOW(),INTERVAL 3 DAY),  TRUE),
                                                                                  (@conv3_id,@buyer1_id,'Cảm ơn Admin đã hỗ trợ nhanh! 🙏',                                                    DATE_SUB(NOW(),INTERVAL 3 DAY),  TRUE),
                                                                                  (@conv3_id,@admin_id, 'Không có gì bạn! Nếu còn vấn đề gì cứ nhắn mình nhé 😊',                             DATE_SUB(NOW(),INTERVAL 2 DAY),  TRUE);

-- Conversation 4: Shop 1 ↔ Admin
INSERT INTO conversations (room_key,user1_id,user2_id,created_at,last_message_at)
VALUES (
           CONCAT(LEAST(@shop1_id,@admin_id),'_',GREATEST(@shop1_id,@admin_id)),
           LEAST(@shop1_id,@admin_id),
           GREATEST(@shop1_id,@admin_id),
           DATE_SUB(NOW(),INTERVAL 5 DAY),
           DATE_SUB(NOW(),INTERVAL 4 DAY)
       );
SET @conv4_id=(SELECT id FROM conversations WHERE room_key=CONCAT(LEAST(@shop1_id,@admin_id),'_',GREATEST(@shop1_id,@admin_id)) LIMIT 1);

INSERT INTO chat_messages (conversation_id,sender_id,content,sent_at,is_read) VALUES
                                                                                  (@conv4_id,@shop1_id, 'Admin ơi, shop mình muốn thêm sản phẩm mới thì cần thủ tục gì không?',                  DATE_SUB(NOW(),INTERVAL 5 DAY), TRUE),
                                                                                  (@conv4_id,@admin_id, 'Chào shop! Shop chỉ cần đăng nhập vào dashboard và thêm sản phẩm là được ngay nhé.',    DATE_SUB(NOW(),INTERVAL 5 DAY), TRUE),
                                                                                  (@conv4_id,@shop1_id, 'Vậy có cần upload giấy tờ VietGAP không Admin?',                                         DATE_SUB(NOW(),INTERVAL 5 DAY), TRUE),
                                                                                  (@conv4_id,@admin_id, 'Không bắt buộc, nhưng nếu có thì tỉ lệ mua hàng cao hơn ~30% bạn nhé. Nên upload!',   DATE_SUB(NOW(),INTERVAL 5 DAY), TRUE),
                                                                                  (@conv4_id,@shop1_id, 'Hiểu rồi! Shop mình sẽ chuẩn bị. Còn về phí hoa hồng thì bao nhiêu % vậy Admin?',     DATE_SUB(NOW(),INTERVAL 4 DAY), TRUE),
                                                                                  (@conv4_id,@admin_id, 'Hiện tại Food Market thu 3% trên mỗi đơn thành công. Không có phí cố định nhé 😊',     DATE_SUB(NOW(),INTERVAL 4 DAY), TRUE),
                                                                                  (@conv4_id,@shop1_id, 'Oke, hợp lý! Cảm ơn Admin đã giải đáp.',                                               DATE_SUB(NOW(),INTERVAL 4 DAY), TRUE);

-- Conversation 5: Shipper 1 ↔ Admin
INSERT INTO conversations (room_key,user1_id,user2_id,created_at,last_message_at)
VALUES (
           CONCAT(LEAST(@shipper1_id,@admin_id),'_',GREATEST(@shipper1_id,@admin_id)),
           LEAST(@shipper1_id,@admin_id),
           GREATEST(@shipper1_id,@admin_id),
           DATE_SUB(NOW(),INTERVAL 4 DAY),
           DATE_SUB(NOW(),INTERVAL 3 DAY)
       );
SET @conv5_id=(SELECT id FROM conversations WHERE room_key=CONCAT(LEAST(@shipper1_id,@admin_id),'_',GREATEST(@shipper1_id,@admin_id)) LIMIT 1);

INSERT INTO chat_messages (conversation_id,sender_id,content,sent_at,is_read) VALUES
                                                                                  (@conv5_id,@shipper1_id,'Admin ơi, mình muốn đăng ký nhận đơn khu vực Bình Dương được không?',                 DATE_SUB(NOW(),INTERVAL 4 DAY), TRUE),
                                                                                  (@conv5_id,@admin_id,   'Được bạn! Bạn cập nhật khu vực hoạt động trong profile là được ngay nhé.',            DATE_SUB(NOW(),INTERVAL 4 DAY), TRUE),
                                                                                  (@conv5_id,@shipper1_id,'Khi nhận đơn Bình Dương có phụ cấp thêm không Admin?',                                DATE_SUB(NOW(),INTERVAL 4 DAY), TRUE),
                                                                                  (@conv5_id,@admin_id,   'Có! Thêm 5.000đ/đơn cho khu vực ngoại thành nhé. Đang áp dụng từ tháng này.',        DATE_SUB(NOW(),INTERVAL 3 DAY), TRUE),
                                                                                  (@conv5_id,@shipper1_id,'Tuyệt! Mình đăng ký luôn. Cảm ơn Admin.',                                            DATE_SUB(NOW(),INTERVAL 3 DAY), TRUE);


-- ============================================================
-- 20. BUILD PLANS
-- ============================================================

-- ---- PLAN 1: Giảm cân 3 ngày ----
INSERT INTO build_plans (buyer_id,plan_name,number_of_people,number_of_days,meal_type,target_budget)
VALUES (@buyer1_id,'Kế hoạch giảm cân 3 ngày',2,3,'DIET',600000.00);
SET @plan1_id=(SELECT plan_id FROM build_plans WHERE buyer_id=@buyer1_id AND plan_name='Kế hoạch giảm cân 3 ngày' LIMIT 1);

-- Plan 1 - Day 1
INSERT INTO plan_day (day_index,plan_plan_id) VALUES (1,@plan1_id);
SET @plan1_day1=(SELECT id FROM plan_day WHERE plan_plan_id=@plan1_id AND day_index=1 LIMIT 1);
INSERT INTO meal (meal_type,day_id) VALUES (0,@plan1_day1),(1,@plan1_day1),(2,@plan1_day1);
SET @p1d1_breakfast=(SELECT id FROM meal WHERE day_id=@plan1_day1 AND meal_type=0 LIMIT 1);
SET @p1d1_lunch    =(SELECT id FROM meal WHERE day_id=@plan1_day1 AND meal_type=1 LIMIT 1);
SET @p1d1_dinner   =(SELECT id FROM meal WHERE day_id=@plan1_day1 AND meal_type=2 LIMIT 1);
INSERT INTO meal_item (meal_id,combo_combo_id,quantity) VALUES
                                                            (@p1d1_breakfast,@combo_detox,   1),
                                                            (@p1d1_lunch,    @combo_salad,   1),
                                                            (@p1d1_dinner,   @combo_eatclean,1);

-- Plan 1 - Day 2
INSERT INTO plan_day (day_index,plan_plan_id) VALUES (2,@plan1_id);
SET @plan1_day2=(SELECT id FROM plan_day WHERE plan_plan_id=@plan1_id AND day_index=2 LIMIT 1);
INSERT INTO meal (meal_type,day_id) VALUES (0,@plan1_day2),(1,@plan1_day2),(2,@plan1_day2);
SET @p1d2_breakfast=(SELECT id FROM meal WHERE day_id=@plan1_day2 AND meal_type=0 LIMIT 1);
SET @p1d2_lunch    =(SELECT id FROM meal WHERE day_id=@plan1_day2 AND meal_type=1 LIMIT 1);
SET @p1d2_dinner   =(SELECT id FROM meal WHERE day_id=@plan1_day2 AND meal_type=2 LIMIT 1);
INSERT INTO meal_item (meal_id,combo_combo_id,quantity) VALUES
                                                            (@p1d2_breakfast,@combo_detox,    1),
                                                            (@p1d2_lunch,    @combo_xao,      1),
                                                            (@p1d2_dinner,   @combo2_sup_bi,  1);

-- Plan 1 - Day 3
INSERT INTO plan_day (day_index,plan_plan_id) VALUES (3,@plan1_id);
SET @plan1_day3=(SELECT id FROM plan_day WHERE plan_plan_id=@plan1_id AND day_index=3 LIMIT 1);
INSERT INTO meal (meal_type,day_id) VALUES (0,@plan1_day3),(1,@plan1_day3),(2,@plan1_day3);
SET @p1d3_breakfast=(SELECT id FROM meal WHERE day_id=@plan1_day3 AND meal_type=0 LIMIT 1);
SET @p1d3_lunch    =(SELECT id FROM meal WHERE day_id=@plan1_day3 AND meal_type=1 LIMIT 1);
SET @p1d3_dinner   =(SELECT id FROM meal WHERE day_id=@plan1_day3 AND meal_type=2 LIMIT 1);
INSERT INTO meal_item (meal_id,combo_combo_id,quantity) VALUES
                                                            (@p1d3_breakfast,@combo2_rau_mam,1),
                                                            (@p1d3_lunch,    @combo_salad,   1),
                                                            (@p1d3_dinner,   @combo_ham,     1);

-- ---- PLAN 2: Gia đình 5 ngày ----
INSERT INTO build_plans (buyer_id,plan_name,number_of_people,number_of_days,meal_type,target_budget)
VALUES (@buyer1_id,'Thực đơn gia đình 5 ngày',4,5,'FAMILY',1500000.00);
SET @plan2_id=(SELECT plan_id FROM build_plans WHERE buyer_id=@buyer1_id AND plan_name='Thực đơn gia đình 5 ngày' LIMIT 1);

-- Plan 2 - Day 1
INSERT INTO plan_day (day_index,plan_plan_id) VALUES (1,@plan2_id);
SET @plan2_day1=(SELECT id FROM plan_day WHERE plan_plan_id=@plan2_id AND day_index=1 LIMIT 1);
INSERT INTO meal (meal_type,day_id) VALUES (0,@plan2_day1),(1,@plan2_day1),(2,@plan2_day1),(3,@plan2_day1);
SET @p2d1_b=(SELECT id FROM meal WHERE day_id=@plan2_day1 AND meal_type=0 LIMIT 1);
SET @p2d1_l=(SELECT id FROM meal WHERE day_id=@plan2_day1 AND meal_type=1 LIMIT 1);
SET @p2d1_d=(SELECT id FROM meal WHERE day_id=@plan2_day1 AND meal_type=2 LIMIT 1);
SET @p2d1_s=(SELECT id FROM meal WHERE day_id=@plan2_day1 AND meal_type=3 LIMIT 1);
INSERT INTO meal_item (meal_id,combo_combo_id,quantity) VALUES
                                                            (@p2d1_b,@combo_detox,    1),
                                                            (@p2d1_l,@combo_xao,      2),
                                                            (@p2d1_d,@combo_lau,      2),
                                                            (@p2d1_s,@combo_trai_cay, 1);

-- Plan 2 - Day 2
INSERT INTO plan_day (day_index,plan_plan_id) VALUES (2,@plan2_id);
SET @plan2_day2=(SELECT id FROM plan_day WHERE plan_plan_id=@plan2_id AND day_index=2 LIMIT 1);
INSERT INTO meal (meal_type,day_id) VALUES (0,@plan2_day2),(1,@plan2_day2),(2,@plan2_day2),(3,@plan2_day2);
SET @p2d2_b=(SELECT id FROM meal WHERE day_id=@plan2_day2 AND meal_type=0 LIMIT 1);
SET @p2d2_l=(SELECT id FROM meal WHERE day_id=@plan2_day2 AND meal_type=1 LIMIT 1);
SET @p2d2_d=(SELECT id FROM meal WHERE day_id=@plan2_day2 AND meal_type=2 LIMIT 1);
SET @p2d2_s=(SELECT id FROM meal WHERE day_id=@plan2_day2 AND meal_type=3 LIMIT 1);
INSERT INTO meal_item (meal_id,combo_combo_id,quantity) VALUES
                                                            (@p2d2_b,@combo2_rau_mam,   1),
                                                            (@p2d2_l,@combo_ham,        2),
                                                            (@p2d2_d,@combo_lau,        2),
                                                            (@p2d2_s,@combo2_giam_nhiet,1);

-- Plan 2 - Day 3
INSERT INTO plan_day (day_index,plan_plan_id) VALUES (3,@plan2_id);
SET @plan2_day3=(SELECT id FROM plan_day WHERE plan_plan_id=@plan2_id AND day_index=3 LIMIT 1);
INSERT INTO meal (meal_type,day_id) VALUES (0,@plan2_day3),(1,@plan2_day3),(2,@plan2_day3),(3,@plan2_day3);
SET @p2d3_b=(SELECT id FROM meal WHERE day_id=@plan2_day3 AND meal_type=0 LIMIT 1);
SET @p2d3_l=(SELECT id FROM meal WHERE day_id=@plan2_day3 AND meal_type=1 LIMIT 1);
SET @p2d3_d=(SELECT id FROM meal WHERE day_id=@plan2_day3 AND meal_type=2 LIMIT 1);
SET @p2d3_s=(SELECT id FROM meal WHERE day_id=@plan2_day3 AND meal_type=3 LIMIT 1);
INSERT INTO meal_item (meal_id,combo_combo_id,quantity) VALUES
                                                            (@p2d3_b,@combo_detox,     1),
                                                            (@p2d3_l,@combo_xao,       2),
                                                            (@p2d3_d,@combo2_sup_bi,   2),
                                                            (@p2d3_s,@combo2_nhiet_doi,1);

-- Plan 2 - Day 4
INSERT INTO plan_day (day_index,plan_plan_id) VALUES (4,@plan2_id);
SET @plan2_day4=(SELECT id FROM plan_day WHERE plan_plan_id=@plan2_id AND day_index=4 LIMIT 1);
INSERT INTO meal (meal_type,day_id) VALUES (0,@plan2_day4),(1,@plan2_day4),(2,@plan2_day4),(3,@plan2_day4);
SET @p2d4_b=(SELECT id FROM meal WHERE day_id=@plan2_day4 AND meal_type=0 LIMIT 1);
SET @p2d4_l=(SELECT id FROM meal WHERE day_id=@plan2_day4 AND meal_type=1 LIMIT 1);
SET @p2d4_d=(SELECT id FROM meal WHERE day_id=@plan2_day4 AND meal_type=2 LIMIT 1);
SET @p2d4_s=(SELECT id FROM meal WHERE day_id=@plan2_day4 AND meal_type=3 LIMIT 1);
INSERT INTO meal_item (meal_id,combo_combo_id,quantity) VALUES
                                                            (@p2d4_b,@combo2_rau_mam, 1),
                                                            (@p2d4_l,@combo_salad,    2),
                                                            (@p2d4_d,@combo_lau,      2),
                                                            (@p2d4_s,@combo_trai_cay, 1);

-- Plan 2 - Day 5
INSERT INTO plan_day (day_index,plan_plan_id) VALUES (5,@plan2_id);
SET @plan2_day5=(SELECT id FROM plan_day WHERE plan_plan_id=@plan2_id AND day_index=5 LIMIT 1);
INSERT INTO meal (meal_type,day_id) VALUES (0,@plan2_day5),(1,@plan2_day5),(2,@plan2_day5),(3,@plan2_day5);
SET @p2d5_b=(SELECT id FROM meal WHERE day_id=@plan2_day5 AND meal_type=0 LIMIT 1);
SET @p2d5_l=(SELECT id FROM meal WHERE day_id=@plan2_day5 AND meal_type=1 LIMIT 1);
SET @p2d5_d=(SELECT id FROM meal WHERE day_id=@plan2_day5 AND meal_type=2 LIMIT 1);
SET @p2d5_s=(SELECT id FROM meal WHERE day_id=@plan2_day5 AND meal_type=3 LIMIT 1);
INSERT INTO meal_item (meal_id,combo_combo_id,quantity) VALUES
                                                            (@p2d5_b,@combo_detox,       1),
                                                            (@p2d5_l,@combo_ham,         2),
                                                            (@p2d5_d,@combo2_sup_bi,     2),
                                                            (@p2d5_s,@combo2_giam_nhiet, 1);

-- ---- PLAN 3: Ăn chay 2 ngày ----
INSERT INTO build_plans (buyer_id,plan_name,number_of_people,number_of_days,meal_type,target_budget)
VALUES (@buyer1_id,'Thực đơn ăn chay cuối tuần',2,2,'VEGAN',350000.00);
SET @plan3_id=(SELECT plan_id FROM build_plans WHERE buyer_id=@buyer1_id AND plan_name='Thực đơn ăn chay cuối tuần' LIMIT 1);

-- Plan 3 - Day 1
INSERT INTO plan_day (day_index,plan_plan_id) VALUES (1,@plan3_id);
SET @plan3_day1=(SELECT id FROM plan_day WHERE plan_plan_id=@plan3_id AND day_index=1 LIMIT 1);
INSERT INTO meal (meal_type,day_id) VALUES (0,@plan3_day1),(1,@plan3_day1),(2,@plan3_day1),(3,@plan3_day1);
SET @p3d1_b=(SELECT id FROM meal WHERE day_id=@plan3_day1 AND meal_type=0 LIMIT 1);
SET @p3d1_l=(SELECT id FROM meal WHERE day_id=@plan3_day1 AND meal_type=1 LIMIT 1);
SET @p3d1_d=(SELECT id FROM meal WHERE day_id=@plan3_day1 AND meal_type=2 LIMIT 1);
SET @p3d1_s=(SELECT id FROM meal WHERE day_id=@plan3_day1 AND meal_type=3 LIMIT 1);
INSERT INTO meal_item (meal_id,combo_combo_id,quantity) VALUES
                                                            (@p3d1_b,@combo2_rau_mam,   1),
                                                            (@p3d1_l,@combo_xao,        1),
                                                            (@p3d1_d,@combo2_sup_bi,    1),
                                                            (@p3d1_s,@combo2_nhiet_doi, 1);

-- Plan 3 - Day 2
INSERT INTO plan_day (day_index,plan_plan_id) VALUES (2,@plan3_id);
SET @plan3_day2=(SELECT id FROM plan_day WHERE plan_plan_id=@plan3_id AND day_index=2 LIMIT 1);
INSERT INTO meal (meal_type,day_id) VALUES (0,@plan3_day2),(1,@plan3_day2),(2,@plan3_day2),(3,@plan3_day2);
SET @p3d2_b=(SELECT id FROM meal WHERE day_id=@plan3_day2 AND meal_type=0 LIMIT 1);
SET @p3d2_l=(SELECT id FROM meal WHERE day_id=@plan3_day2 AND meal_type=1 LIMIT 1);
SET @p3d2_d=(SELECT id FROM meal WHERE day_id=@plan3_day2 AND meal_type=2 LIMIT 1);
SET @p3d2_s=(SELECT id FROM meal WHERE day_id=@plan3_day2 AND meal_type=3 LIMIT 1);
INSERT INTO meal_item (meal_id,combo_combo_id,quantity) VALUES
                                                            (@p3d2_b,@combo_detox,        1),
                                                            (@p3d2_l,@combo_salad,        1),
                                                            (@p3d2_d,@combo_ham,          1),
                                                            (@p3d2_s,@combo2_giam_nhiet,  1);


-- ============================================================
-- 21. BUILD PLAN ITEMS
-- ============================================================
INSERT INTO build_plan_items (plan_id,meal_name,servings,meal_date,completed) VALUES
                                                                                  (@plan1_id,'Bữa sáng ngày 1 - Detox xanh',       2,DATE_ADD(CURDATE(),INTERVAL 1 DAY),FALSE),
                                                                                  (@plan1_id,'Bữa trưa ngày 1 - Salad giảm cân',   2,DATE_ADD(CURDATE(),INTERVAL 1 DAY),FALSE),
                                                                                  (@plan1_id,'Bữa tối ngày 1 - Eat clean',          2,DATE_ADD(CURDATE(),INTERVAL 1 DAY),FALSE),
                                                                                  (@plan1_id,'Bữa sáng ngày 2 - Detox xanh',       2,DATE_ADD(CURDATE(),INTERVAL 2 DAY),FALSE),
                                                                                  (@plan1_id,'Bữa trưa ngày 2 - Xào rau thập cẩm', 2,DATE_ADD(CURDATE(),INTERVAL 2 DAY),FALSE),
                                                                                  (@plan1_id,'Bữa tối ngày 2 - Súp bí đao',        2,DATE_ADD(CURDATE(),INTERVAL 2 DAY),FALSE),
                                                                                  (@plan1_id,'Bữa sáng ngày 3 - Rau mầm',          2,DATE_ADD(CURDATE(),INTERVAL 3 DAY),FALSE),
                                                                                  (@plan1_id,'Bữa trưa ngày 3 - Salad giảm cân',   2,DATE_ADD(CURDATE(),INTERVAL 3 DAY),FALSE),
                                                                                  (@plan1_id,'Bữa tối ngày 3 - Hầm rau củ',        2,DATE_ADD(CURDATE(),INTERVAL 3 DAY),FALSE),
                                                                                  (@plan2_id,'Bữa sáng ngày 1 - Detox xanh',       4,DATE_ADD(CURDATE(),INTERVAL 1 DAY),FALSE),
                                                                                  (@plan2_id,'Bữa trưa ngày 1 - Xào rau thập cẩm', 4,DATE_ADD(CURDATE(),INTERVAL 1 DAY),FALSE),
                                                                                  (@plan2_id,'Bữa tối ngày 1 - Lẩu rau củ',        4,DATE_ADD(CURDATE(),INTERVAL 1 DAY),FALSE),
                                                                                  (@plan2_id,'Bữa phụ ngày 1 - Trái cây tráng miệng',4,DATE_ADD(CURDATE(),INTERVAL 1 DAY),FALSE);

SET @bpi1=(SELECT id FROM build_plan_items WHERE plan_id=@plan1_id AND meal_name LIKE '%Detox%' AND meal_date=DATE_ADD(CURDATE(),INTERVAL 1 DAY) LIMIT 1);
SET @bpi2=(SELECT id FROM build_plan_items WHERE plan_id=@plan1_id AND meal_name LIKE '%Salad%' AND meal_date=DATE_ADD(CURDATE(),INTERVAL 1 DAY) LIMIT 1);

INSERT INTO meal_products (meal_id,product_id) VALUES
                                                   (@bpi1,@p_cai_bo_xoi),(@bpi1,@p_dua_leo),(@bpi1,@p_can_tay),
                                                   (@bpi2,@p_xa_lach),   (@bpi2,@p_dua_leo);


-- ============================================================
-- 22. TRANSACTIONS
-- ============================================================
INSERT INTO transactions (payment_id,payment_gateway,amount,balance_after,status,content) VALUES
                                                                                              (@payment_success1,'PAYOS',245000.00,287160.00,'SUCCESS',
                                                                                               'Thanh toán đơn hàng FOOD20240001 - Dâu tây Đà Lạt, Táo Fuji, Chuối - 245.000đ'),
                                                                                              (@payment_success2,'PAYOS',217000.00,367630.00,'SUCCESS',
                                                                                               'Thanh toán đơn hàng FOOD20240002 - Xoài cát, Bưởi da xanh, Cam sành - 217.000đ'),
                                                                                              (@payment_pending3,'PAYOS',225000.00,0.00,'PENDING',
                                                                                               'Đang xử lý thanh toán đơn hàng FOOD20240003 - Cà rốt, Khoai tây, Bí đỏ - 225.000đ'),
                                                                                              (@payment_pending4,'PAYOS',188000.00,0.00,'PENDING',
                                                                                               'Đang xử lý thanh toán đơn hàng FOOD20240004 - Cam sành, Bưởi da xanh - 188.000đ'),
                                                                                              (@payment_pending5,'PAYOS',125000.00,0.00,'PENDING',
                                                                                               'Khởi tạo thanh toán đơn hàng FOOD20240005 - Bông cải xanh, Xà lách - 125.000đ'),
                                                                                              (@payment_pending3,'PAYOS',225000.00,0.00,'FAILED',
                                                                                               'Thanh toán thất bại - hết hạn QR code - đơn FOOD20240003 lần 1'),
                                                                                              (NULL,'PAYOS',128000.00,87160.00,'SUCCESS',
                                                                                               'Rút tiền doanh thu về tài khoản Vietcombank 0071000123456 - Trần Văn Kiệt'),
                                                                                              (NULL,'PAYOS',150000.00,217630.00,'PENDING',
                                                                                               'Đang xử lý rút tiền về tài khoản Techcombank 19034567890123 - Lệ Thị Ngọc');


SET FOREIGN_KEY_CHECKS = 1;

-- ============================================================
-- TỔNG KẾT DỮ LIỆU
-- ============================================================
-- Roles:            4 (ADMIN, BUYER, SHOP_OWNER, SHIPPER)
-- Categories:       3 (Rau lá, Củ, Quả/Trái cây)
-- Users:            8 (1 admin, 2 buyer, 2 shipper, 3 shop)
-- Wallets:          6 (1 platform, 3 shop, 2 shipper)
-- Products:         50 (25 shop1, 25 shop2)
-- Build Combos:     28 (14 shop1, 14 shop2)
-- Product Combos:   76 mapping records
-- Mystery Boxes:    4 (2 shop1, 2 shop2)
-- Product Mystery:  14 mapping records
-- Vouchers:         8 (4 shop1, 4 shop2)
-- Orders:           22 (PENDING×4, CONFIRMED×4, SHIPPING×4,
--                       DELIVERED×4, CANCELLED×3, FAILED×3)
-- Order Details:    48 records
-- Reviews:          5 records
-- Payments:         5 records
-- Withdraw Requests:3 records
-- Notifications:    6 broadcasts + 10 user_notifications
-- Blogs:            3 articles
-- Bot Knowledge:    10 entries (4 POLICY + 6 FAQ)
-- Return Requests:  3 records
-- Cart:             1 cart, 7 cart items
-- Conversations:    5 (buyer-shop1, buyer-shop2, buyer-admin,
--                      shop1-admin, shipper1-admin)
-- Chat Messages:    42 messages
-- Build Plans:      3 (giảm cân 3 ngày, gia đình 5 ngày,
--                      ăn chay 2 ngày)
-- Plan Days:        10 days total
-- Meals:            36 meals (3-4 per day)
-- Meal Items:       36 items
-- Build Plan Items: 13 items
-- Transactions:     8 (SUCCESS×3, PENDING×3, FAILED×1 + 1 withdraw)
-- ============================================================