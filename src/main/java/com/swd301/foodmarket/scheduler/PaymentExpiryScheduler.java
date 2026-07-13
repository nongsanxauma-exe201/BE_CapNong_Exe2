package com.swd301.foodmarket.scheduler;

import com.swd301.foodmarket.entity.CheckoutBatch;
import com.swd301.foodmarket.entity.Payment;
import com.swd301.foodmarket.enums.CheckoutBatchStatus;
import com.swd301.foodmarket.enums.PaymentStatus;
import com.swd301.foodmarket.repository.CheckoutBatchRepository;
import com.swd301.foodmarket.repository.PaymentRepository;
import com.swd301.foodmarket.service.PaymentService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

import java.time.LocalDateTime;
import java.time.ZoneId;
import java.util.List;

@Component
@RequiredArgsConstructor
@Slf4j
public class PaymentExpiryScheduler {

    private static final int PAYMENT_EXPIRY_HOURS = 24;

    private final PaymentRepository paymentRepository;
    private final CheckoutBatchRepository checkoutBatchRepository;
    private final PaymentService paymentService;

    @Scheduled(cron = "0 */30 * * * ?")
    public void expireStalePendingPayments() {
        LocalDateTime cutoff = LocalDateTime.now(ZoneId.of("Asia/Ho_Chi_Minh")).minusHours(PAYMENT_EXPIRY_HOURS);
        List<Payment> expiredPayments = paymentRepository.findByStatusAndCreatedAtBefore(PaymentStatus.PENDING, cutoff);

        for (Payment payment : expiredPayments) {
            if (payment.getPayosOrderCode() == null || payment.getPayosOrderCode().isBlank()) {
                continue;
            }
            try {
                paymentService.cancelPaymentByOrderCode(payment.getPayosOrderCode());
                log.info("[PAYMENT_EXPIRY] Cancelled stale payment id={}, orderCode={}",
                        payment.getId(), payment.getPayosOrderCode());
            } catch (Exception ex) {
                log.warn("[PAYMENT_EXPIRY] Failed to cancel payment id={}: {}",
                        payment.getId(), ex.getMessage());
            }
        }

        List<CheckoutBatch> staleBatches = checkoutBatchRepository
                .findByStatusAndCreatedAtBefore(CheckoutBatchStatus.PENDING, cutoff);
        for (CheckoutBatch batch : staleBatches) {
            if (batch.getPayosOrderCode() == null || batch.getPayosOrderCode().isBlank()) {
                continue;
            }
            try {
                paymentService.cancelPaymentByOrderCode(batch.getPayosOrderCode());
                log.info("[PAYMENT_EXPIRY] Cancelled stale batch id={}, orderCode={}",
                        batch.getId(), batch.getPayosOrderCode());
            } catch (Exception ex) {
                log.warn("[PAYMENT_EXPIRY] Failed to cancel batch id={}: {}",
                        batch.getId(), ex.getMessage());
            }
        }
    }
}
