package com.swd301.foodmarket.config;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Component;

import java.util.List;

@Component
@RequiredArgsConstructor
@Slf4j
public class StartupSchemaMigration implements ApplicationRunner {

    private final JdbcTemplate jdbcTemplate;

    @Override
    public void run(ApplicationArguments args) {
        makeVoucherShopNullable();
        addOrderShippingDiscountAmount();
        ensureVoucherUsageOrderIndex();
        dropVoucherUsageOrderUniqueIndexes();
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
            Integer columnCount = jdbcTemplate.queryForObject("""
                    SELECT COUNT(*)
                    FROM information_schema.COLUMNS
                    WHERE TABLE_SCHEMA = DATABASE()
                      AND TABLE_NAME = 'orders'
                      AND COLUMN_NAME = 'shipping_discount_amount'
                    """, Integer.class);

            if (columnCount != null && columnCount == 0) {
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

    private void dropVoucherUsageOrderUniqueIndexes() {
        try {
            List<String> uniqueIndexes = jdbcTemplate.queryForList("""
                    SELECT INDEX_NAME
                    FROM information_schema.STATISTICS
                    WHERE TABLE_SCHEMA = DATABASE()
                      AND TABLE_NAME = 'voucher_usage'
                      AND COLUMN_NAME = 'order_id'
                      AND NON_UNIQUE = 0
                      AND INDEX_NAME <> 'PRIMARY'
                    """, String.class);

            for (String indexName : uniqueIndexes) {
                jdbcTemplate.execute("ALTER TABLE voucher_usage DROP INDEX `" + indexName + "`");
                log.info("[SCHEMA_MIGRATION] Dropped unique index {} on voucher_usage.order_id", indexName);
            }
        } catch (Exception ex) {
            log.warn("[SCHEMA_MIGRATION] Could not drop voucher_usage.order_id unique index: {}", ex.getMessage());
        }
    }

    private void ensureVoucherUsageOrderIndex() {
        try {
            Integer indexCount = jdbcTemplate.queryForObject("""
                    SELECT COUNT(*)
                    FROM information_schema.STATISTICS
                    WHERE TABLE_SCHEMA = DATABASE()
                      AND TABLE_NAME = 'voucher_usage'
                      AND COLUMN_NAME = 'order_id'
                      AND INDEX_NAME = 'idx_voucher_usage_order_id'
                    """, Integer.class);

            if (indexCount != null && indexCount == 0) {
                jdbcTemplate.execute("CREATE INDEX idx_voucher_usage_order_id ON voucher_usage(order_id)");
                log.info("[SCHEMA_MIGRATION] Added non-unique index idx_voucher_usage_order_id");
            }
        } catch (Exception ex) {
            log.warn("[SCHEMA_MIGRATION] Could not add voucher_usage.order_id index: {}", ex.getMessage());
        }
    }
}
