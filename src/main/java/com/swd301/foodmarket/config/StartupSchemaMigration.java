package com.swd301.foodmarket.config;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
@Slf4j
public class StartupSchemaMigration implements ApplicationRunner {

    private final JdbcTemplate jdbcTemplate;

    @Override
    public void run(ApplicationArguments args) {
        makeVoucherShopNullable();
        addOrderShippingDiscountAmount();
    }

    private void makeVoucherShopNullable() {
        try {
            Integer notNullable = jdbcTemplate.queryForObject("""
                    SELECT COUNT(*)
                    FROM information_schema.COLUMNS
                    WHERE TABLE_SCHEMA = DATABASE()
                      AND TABLE_NAME = 'vouchers'
                      AND COLUMN_NAME = 'shop_id'
                      AND IS_NULLABLE = 'NO'
                    """, Integer.class);

            if (notNullable != null && notNullable > 0) {
                jdbcTemplate.execute("ALTER TABLE vouchers MODIFY COLUMN shop_id INT NULL");
                log.info("[SCHEMA_MIGRATION] Updated vouchers.shop_id to nullable for platform vouchers");
            }
        } catch (Exception ex) {
            log.warn("[SCHEMA_MIGRATION] Could not update vouchers.shop_id nullable constraint: {}", ex.getMessage());
        }
    }

    private void addOrderShippingDiscountAmount() {
        try {
            Integer missingColumn = jdbcTemplate.queryForObject("""
                    SELECT COUNT(*)
                    FROM information_schema.COLUMNS
                    WHERE TABLE_SCHEMA = DATABASE()
                      AND TABLE_NAME = 'orders'
                      AND COLUMN_NAME = 'shipping_discount_amount'
                    """, Integer.class);

            if (missingColumn != null && missingColumn == 0) {
                jdbcTemplate.execute("""
                        ALTER TABLE orders
                        ADD COLUMN shipping_discount_amount DECIMAL(18,2) DEFAULT 0
                        """);
                log.info("[SCHEMA_MIGRATION] Added orders.shipping_discount_amount for free shipping vouchers");
            }
        } catch (Exception ex) {
            log.warn("[SCHEMA_MIGRATION] Could not add orders.shipping_discount_amount: {}", ex.getMessage());
        }
    }
}
