package com.swd301.foodmarket.service.support;

import com.swd301.foodmarket.entity.CheckoutBatch;
import com.swd301.foodmarket.entity.Order;
import com.swd301.foodmarket.entity.Voucher;
import com.swd301.foodmarket.entity.VoucherUser;
import com.swd301.foodmarket.enums.VoucherType;
import com.swd301.foodmarket.repository.VoucherUserRepository;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.math.BigDecimal;
import java.util.Optional;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class OrderSettlementCalculatorTest {

    @Mock
    private VoucherUserRepository voucherUserRepository;

    @InjectMocks
    private OrderSettlementCalculator calculator;

    @Test
    void includesPlatformBatchSubsidyInShopGross() {
        UUID voucherUserId = UUID.randomUUID();
        CheckoutBatch batch = CheckoutBatch.builder()
                .id(10)
                .batchVoucherUserId(voucherUserId)
                .build();
        Voucher platformVoucher = Voucher.builder().voucherType(VoucherType.PLATFORM).build();
        VoucherUser voucherUser = VoucherUser.builder().voucher(platformVoucher).build();

        Order order = Order.builder()
                .totalAmount(new BigDecimal("350000"))
                .shippingFee(new BigDecimal("30000"))
                .shippingDiscountAmount(BigDecimal.ZERO)
                .batchDiscountAmount(new BigDecimal("50000"))
                .checkoutBatch(batch)
                .build();

        when(voucherUserRepository.findById(voucherUserId)).thenReturn(Optional.of(voucherUser));

        OrderSettlementCalculator.SettlementAmounts amounts = calculator.calculate(order);

        assertEquals(new BigDecimal("50000"), amounts.platformBatchSubsidy());
        assertEquals(new BigDecimal("400000"), amounts.settlementTotal());
        assertEquals(new BigDecimal("370000"), amounts.shopGross());
    }

    @Test
    void doesNotDoubleCountWelcomeBatchSubsidy() {
        UUID voucherUserId = UUID.randomUUID();
        CheckoutBatch batch = CheckoutBatch.builder()
                .id(11)
                .batchVoucherUserId(voucherUserId)
                .build();
        Voucher welcomeVoucher = Voucher.builder().voucherType(VoucherType.WELCOME).build();
        VoucherUser voucherUser = VoucherUser.builder().voucher(welcomeVoucher).build();

        Order order = Order.builder()
                .totalAmount(new BigDecimal("320000"))
                .shippingFee(new BigDecimal("30000"))
                .shippingDiscountAmount(new BigDecimal("30000"))
                .batchDiscountAmount(new BigDecimal("30000"))
                .checkoutBatch(batch)
                .build();

        when(voucherUserRepository.findById(voucherUserId)).thenReturn(Optional.of(voucherUser));

        OrderSettlementCalculator.SettlementAmounts amounts = calculator.calculate(order);

        assertEquals(BigDecimal.ZERO, amounts.platformBatchSubsidy());
        assertEquals(new BigDecimal("350000"), amounts.settlementTotal());
        assertEquals(new BigDecimal("320000"), amounts.shopGross());
    }

    @Test
    void includesPerOrderPlatformSubsidyInShopGross() {
        Order order = Order.builder()
                .totalAmount(new BigDecimal("350000"))
                .shippingFee(new BigDecimal("30000"))
                .shippingDiscountAmount(BigDecimal.ZERO)
                .batchDiscountAmount(new BigDecimal("50000"))
                .build();

        OrderSettlementCalculator.SettlementAmounts amounts = calculator.calculate(order);

        assertEquals(new BigDecimal("50000"), amounts.platformBatchSubsidy());
        assertEquals(new BigDecimal("400000"), amounts.settlementTotal());
        assertEquals(new BigDecimal("370000"), amounts.shopGross());
    }
}
