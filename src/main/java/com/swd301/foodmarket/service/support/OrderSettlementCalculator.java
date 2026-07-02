package com.swd301.foodmarket.service.support;

import com.swd301.foodmarket.entity.CheckoutBatch;
import com.swd301.foodmarket.entity.Order;
import com.swd301.foodmarket.entity.VoucherUser;
import com.swd301.foodmarket.enums.VoucherType;
import com.swd301.foodmarket.repository.VoucherUserRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

import java.math.BigDecimal;

/**
 * Tính settlement cho đơn hàng.
 * Voucher PLATFORM (batch hoặc đơn lẻ) giảm tiền buyer trả nhưng platform phải bù shop — cộng {@code batchDiscountAmount}
 * vào settlementTotal (WELCOME batch đã được tính qua shippingDiscountAmount).
 */
@Component
@RequiredArgsConstructor
public class OrderSettlementCalculator {

    private final VoucherUserRepository voucherUserRepository;

    public SettlementAmounts calculate(Order order) {
        BigDecimal buyerPaid = zeroIfNull(order.getTotalAmount());
        BigDecimal shippingFee = zeroIfNull(order.getShippingFee());
        BigDecimal shippingDiscountAmount = zeroIfNull(order.getShippingDiscountAmount());
        BigDecimal platformBatchSubsidy = resolvePlatformBatchProductSubsidy(order);
        BigDecimal settlementTotal = buyerPaid.add(shippingDiscountAmount).add(platformBatchSubsidy);
        BigDecimal shopGross = settlementTotal.subtract(shippingFee);
        return new SettlementAmounts(
                buyerPaid,
                shippingFee,
                shippingDiscountAmount,
                platformBatchSubsidy,
                settlementTotal,
                shopGross
        );
    }

    public BigDecimal resolvePlatformBatchProductSubsidy(Order order) {
        BigDecimal batchDiscount = zeroIfNull(order.getBatchDiscountAmount());
        if (batchDiscount.compareTo(BigDecimal.ZERO) <= 0) {
            return BigDecimal.ZERO;
        }

        CheckoutBatch batch = order.getCheckoutBatch();
        if (batch != null && batch.getBatchVoucherUserId() != null) {
            return voucherUserRepository.findById(batch.getBatchVoucherUserId())
                    .map(VoucherUser::getVoucher)
                    .filter(voucher -> voucher.getVoucherType() == VoucherType.PLATFORM)
                    .map(voucher -> batchDiscount)
                    .orElse(BigDecimal.ZERO);
        }

        // Voucher PLATFORM áp theo đơn lẻ: lưu trong batchDiscountAmount, platform bù shop.
        return batchDiscount;
    }

    private static BigDecimal zeroIfNull(BigDecimal value) {
        return value == null ? BigDecimal.ZERO : value;
    }

    public record SettlementAmounts(
            BigDecimal buyerPaid,
            BigDecimal shippingFee,
            BigDecimal shippingDiscountAmount,
            BigDecimal platformBatchSubsidy,
            BigDecimal settlementTotal,
            BigDecimal shopGross
    ) {
    }
}
