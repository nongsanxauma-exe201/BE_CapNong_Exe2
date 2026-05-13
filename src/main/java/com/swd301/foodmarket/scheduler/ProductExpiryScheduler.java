package com.swd301.foodmarket.scheduler;

import com.swd301.foodmarket.entity.Product;
import com.swd301.foodmarket.enums.ProductStatus;
import com.swd301.foodmarket.repository.ProductRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

import java.time.LocalDate;
import java.util.List;

@Component
@RequiredArgsConstructor
@Slf4j
public class ProductExpiryScheduler {

    private final ProductRepository productRepository;

    @Scheduled(cron = "0 0 0 * * *") // chạy mỗi ngày lúc 00:00
    public void checkExpiredProducts() {
        List<Product> expiredProducts = productRepository
                .findByExpiryDateBeforeAndStatusNot(LocalDate.now(), ProductStatus.EXPIRED);

        expiredProducts.forEach(p -> p.setStatus(ProductStatus.EXPIRED));
        productRepository.saveAll(expiredProducts);

        log.info("Updated {} expired products", expiredProducts.size());
    }
}
