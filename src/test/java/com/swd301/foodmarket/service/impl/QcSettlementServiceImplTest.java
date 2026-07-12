package com.swd301.foodmarket.service.impl;

import com.swd301.foodmarket.dto.response.OrderResponse;
import com.swd301.foodmarket.entity.*;
import com.swd301.foodmarket.enums.OrderStatus;
import com.swd301.foodmarket.enums.QcDamageTier;
import com.swd301.foodmarket.exception.AppException;
import com.swd301.foodmarket.exception.ErrorCode;
import com.swd301.foodmarket.mapper.OrderMapper;
import com.swd301.foodmarket.repository.MysteryBoxRepository;
import com.swd301.foodmarket.repository.OrderRepository;
import com.swd301.foodmarket.repository.ProductRepository;
import com.swd301.foodmarket.repository.UserRepository;
import com.swd301.foodmarket.service.NotificationService;
import com.swd301.foodmarket.service.PaymentService;
import com.swd301.foodmarket.service.TrackingService;
import com.swd301.foodmarket.service.VoucherService;
import com.swd301.foodmarket.service.VoucherUserService;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.context.SecurityContextHolder;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

/**
 * Automated coverage for QC test matrix (TC-T*, TC-C*, TC-P*, TC-V*, TC-M*, TC-G*).
 * Pure unit tests — no DB / PayOS.
 */
@ExtendWith(MockitoExtension.class)
class QcSettlementServiceImplTest {

    @Mock OrderRepository orderRepository;
    @Mock ProductRepository productRepository;
    @Mock MysteryBoxRepository mysteryBoxRepository;
    @Mock PaymentService paymentService;
    @Mock VoucherService voucherService;
    @Mock VoucherUserService voucherUserService;
    @Mock NotificationService notificationService;
    @Mock TrackingService trackingService;
    @Mock OrderMapper orderMapper;
    @Mock UserRepository userRepository;

    @InjectMocks
    QcSettlementServiceImpl qcSettlement;

    private static final BigDecimal TEN_KG = bd("10");
    private static final BigDecimal PRICE = bd("10000"); // 10k/kg → line 100k

    @BeforeEach
    void stubRefundUpdatesQcRefunded() {
        lenient().when(paymentService.refundQcDamage(any(Order.class), any(BigDecimal.class), anyString()))
                .thenAnswer(inv -> {
                    Order order = inv.getArgument(0);
                    BigDecimal amount = inv.getArgument(1);
                    BigDecimal already = order.getQcRefundedAmount() != null
                            ? order.getQcRefundedAmount() : BigDecimal.ZERO;
                    BigDecimal total = order.getTotalAmount() != null
                            ? order.getTotalAmount() : BigDecimal.ZERO;
                    BigDecimal refundNow = amount.min(total.subtract(already).max(BigDecimal.ZERO));
                    order.setQcRefundedAmount(already.add(refundNow));
                    return refundNow;
                });
        lenient().when(orderRepository.save(any(Order.class))).thenAnswer(inv -> inv.getArgument(0));
        lenient().when(orderMapper.toResponse(any(Order.class))).thenReturn(OrderResponse.builder().build());
    }

    @AfterEach
    void clearSecurity() {
        SecurityContextHolder.clearContext();
    }

    // ─── TC-T*: damage tiers ───────────────────────────────────────────

    @Nested
    @DisplayName("TC-T*: computeDamageTier")
    class TierTests {
        @Test
        @DisplayName("TC-T1 TOLERABLE ≤5%")
        void t1_tolerable() {
            assertEquals(QcDamageTier.TOLERABLE, qcSettlement.computeDamageTier(bd("0.4"), TEN_KG));
            assertEquals(QcDamageTier.TOLERABLE, qcSettlement.computeDamageTier(bd("0.5"), TEN_KG));
        }

        @Test
        @DisplayName("TC-T2 MINOR >5% … ≤20%")
        void t2_minor() {
            assertEquals(QcDamageTier.MINOR, qcSettlement.computeDamageTier(bd("0.6"), TEN_KG));
            assertEquals(QcDamageTier.MINOR, qcSettlement.computeDamageTier(bd("1.5"), TEN_KG));
            assertEquals(QcDamageTier.MINOR, qcSettlement.computeDamageTier(bd("2"), TEN_KG));
        }

        @Test
        @DisplayName("TC-T3 MODERATE >20% … <80%")
        void t3_moderate() {
            assertEquals(QcDamageTier.MODERATE, qcSettlement.computeDamageTier(bd("2.1"), TEN_KG));
            assertEquals(QcDamageTier.MODERATE, qcSettlement.computeDamageTier(bd("3"), TEN_KG));
            assertEquals(QcDamageTier.MODERATE, qcSettlement.computeDamageTier(bd("7.9"), TEN_KG));
        }

        @Test
        @DisplayName("TC-T4 SEVERE ≥80% … <100%")
        void t4_severe() {
            assertEquals(QcDamageTier.SEVERE, qcSettlement.computeDamageTier(bd("8"), TEN_KG));
            assertEquals(QcDamageTier.SEVERE, qcSettlement.computeDamageTier(bd("9"), TEN_KG));
            assertEquals(QcDamageTier.SEVERE, qcSettlement.computeDamageTier(bd("9.9"), TEN_KG));
        }

        @Test
        @DisplayName("TC-T5 TOTAL ≥100%")
        void t5_total() {
            assertEquals(QcDamageTier.TOTAL, qcSettlement.computeDamageTier(bd("10"), TEN_KG));
            assertEquals(QcDamageTier.TOTAL, qcSettlement.computeDamageTier(bd("12"), TEN_KG));
        }
    }

    // ─── applyLine + finalize status / refund timing ───────────────────

    @Test
    @DisplayName("TC-T1 PayOS: TOLERABLE → QUALITY_CHECKED, no refund, no voucher")
    void t1_applyAndFinalize_noMoneyMove() {
        Order order = payosOrder(100_000);
        OrderDetail detail = line(order, TEN_KG, PRICE);
        QualityResult qr = new QualityResult();
        qr.setOrderDetail(detail);

        qcSettlement.applyLineQcResult(order, detail, null, qr, bd("0.4"), TEN_KG);
        detail.setQualityResult(qr);
        qcSettlement.syncOrderAfterQcItem(order);

        assertEquals(QcDamageTier.TOLERABLE, qr.getDamageTier());
        assertEquals(0, qr.getLineRefundAmount().compareTo(BigDecimal.ZERO));
        assertFalse(qr.isLineCancelled());
        assertEquals(OrderStatus.QUALITY_CHECKED, order.getStatus());
        // Order entity có thể default refundAmount=0; kỳ vọng không phát sinh hoàn
        assertTrue(order.getRefundAmount() == null || order.getRefundAmount().signum() == 0);
        verify(paymentService, never()).refundQcDamage(any(), any(), anyString());
        verify(voucherService, never()).createAutoVoucherForDamage(any(), anyString());
    }

    @Test
    @DisplayName("TC-T2 PayOS: MINOR → QUALITY_CHECKED, refundAmount set, NO immediate wallet refund")
    void t2_minor_deferredRefund() {
        Order order = payosOrder(100_000);
        OrderDetail detail = line(order, TEN_KG, PRICE);
        QualityResult qr = new QualityResult();
        qr.setOrderDetail(detail);

        qcSettlement.applyLineQcResult(order, detail, null, qr, bd("1.5"), TEN_KG);
        detail.setQualityResult(qr);
        qcSettlement.syncOrderAfterQcItem(order);

        assertEquals(QcDamageTier.MINOR, qr.getDamageTier());
        assertEquals(0, order.getRefundAmount().compareTo(bd("15000")));
        assertEquals(OrderStatus.QUALITY_CHECKED, order.getStatus());
        verify(paymentService, never()).refundQcDamage(any(), any(), anyString());
        verify(voucherService, never()).createAutoVoucherForDamage(any(), anyString());
    }

    @Test
    @DisplayName("TC-T3 PayOS: MODERATE → deferred refund + create damage voucher")
    void t3_moderate_voucherCreated() {
        Order order = payosOrder(100_000);
        OrderDetail detail = line(order, TEN_KG, PRICE);
        QualityResult qr = new QualityResult();
        qr.setOrderDetail(detail);

        qcSettlement.applyLineQcResult(order, detail, null, qr, bd("3"), TEN_KG);
        detail.setQualityResult(qr);
        qcSettlement.syncOrderAfterQcItem(order);

        assertEquals(QcDamageTier.MODERATE, qr.getDamageTier());
        assertEquals(0, order.getRefundAmount().compareTo(bd("30000")));
        assertTrue(qr.isAutoVoucherCreated());
        assertEquals(OrderStatus.QUALITY_CHECKED, order.getStatus());
        verify(paymentService, never()).refundQcDamage(any(), any(), anyString());
        verify(voucherService).createAutoVoucherForDamage(eq(order), anyString());
    }

    @Test
    @DisplayName("TC-T4 PayOS: SEVERE → QC_PENDING_BUYER + immediate refund + deadline 24h")
    void t4_severe_pendingBuyer() {
        Order order = payosOrder(100_000);
        OrderDetail detail = line(order, TEN_KG, PRICE);
        QualityResult qr = new QualityResult();
        qr.setOrderDetail(detail);
        qr.setApprovedKg(bd("1"));
        qr.setRejectedKg(bd("9"));

        qcSettlement.applyLineQcResult(order, detail, null, qr, bd("9"), TEN_KG);
        detail.setQualityResult(qr);
        qcSettlement.syncOrderAfterQcItem(order);

        assertEquals(QcDamageTier.SEVERE, qr.getDamageTier());
        assertEquals(OrderStatus.QC_PENDING_BUYER, order.getStatus());
        assertNotNull(order.getQcBuyerDecisionDeadline());
        assertNull(order.getQcBuyerAcceptedRemaining());
        verify(paymentService, atLeastOnce()).refundQcDamage(eq(order), any(BigDecimal.class), anyString());
        assertTrue(order.getQcRefundedAmount().signum() > 0);
        verify(voucherService).createAutoVoucherForDamage(eq(order), anyString());
    }

    @Test
    @DisplayName("TC-T5 PayOS: TOTAL single line → CANCELLED + restore voucher + grant damage voucher")
    void t5_total_cancelOrder() {
        Order order = payosOrder(100_000);
        OrderDetail detail = line(order, TEN_KG, PRICE);
        QualityResult qr = new QualityResult();
        qr.setOrderDetail(detail);

        qcSettlement.applyLineQcResult(order, detail, null, qr, bd("10"), TEN_KG);
        detail.setQualityResult(qr);

        // stock restore needs product stock
        detail.getProduct().setStockKg(BigDecimal.ZERO);
        detail.getProduct().setStockQuantity(0);

        qcSettlement.syncOrderAfterQcItem(order);

        assertTrue(qr.isLineCancelled());
        assertEquals(OrderStatus.CANCELLED, order.getStatus());
        verify(voucherUserService).restoreVoucherForOrder(order);
        verify(voucherService).grantDamageVoucherForDeliveredOrder(order);
        verify(paymentService, atLeastOnce()).refundQcDamage(eq(order), any(BigDecimal.class), anyString());
    }

    // ─── TC-C*: COD ────────────────────────────────────────────────────

    @Test
    @DisplayName("TC-C1 COD MINOR: codAmount = total - refund; refundQcDamage still called only if immediate (not for MINOR)")
    void c1_codMinor_reducesCodAmount() {
        Order order = codOrder(100_000);
        OrderDetail detail = line(order, TEN_KG, PRICE);
        QualityResult qr = new QualityResult();
        qr.setOrderDetail(detail);

        qcSettlement.applyLineQcResult(order, detail, null, qr, bd("1.5"), TEN_KG);
        detail.setQualityResult(qr);
        qcSettlement.syncOrderAfterQcItem(order);

        assertEquals(0, order.getCodAmount().compareTo(bd("85000")));
        assertEquals(OrderStatus.QUALITY_CHECKED, order.getStatus());
        verify(paymentService, never()).refundQcDamage(any(), any(), anyString());
    }

    @Test
    @DisplayName("TC-C2 COD SEVERE: QC_PENDING_BUYER + COD reduced; accept → QUALITY_CHECKED")
    void c2_codSevere_acceptRemaining() {
        User buyer = buyer(1, "buyer@test.com");
        Order order = codOrder(100_000);
        order.setBuyer(buyer);
        order.setId(42);
        OrderDetail detail = line(order, TEN_KG, PRICE);
        QualityResult qr = baseSevereResult(detail, bd("1"), bd("9"));
        detail.setQualityResult(qr);

        qcSettlement.applyLineQcResult(order, detail, null, qr, bd("9"), TEN_KG);
        // re-apply approved after applyLine (apply doesn't set approved)
        qr.setApprovedKg(bd("1"));
        qcSettlement.syncOrderAfterQcItem(order);

        assertEquals(OrderStatus.QC_PENDING_BUYER, order.getStatus());
        assertTrue(order.getCodAmount().compareTo(bd("100000")) < 0);

        authAs(buyer);
        when(orderRepository.findById(42)).thenReturn(Optional.of(order));

        qcSettlement.processQcBuyerDecision(42, true);

        assertEquals(OrderStatus.QUALITY_CHECKED, order.getStatus());
        assertEquals(Boolean.TRUE, order.getQcBuyerAcceptedRemaining());
    }

    @Test
    @DisplayName("TC-C3/P2 COD SEVERE reject remaining → CANCELLED + restore voucher")
    void c3_codSevere_rejectRemaining_cancels() {
        User buyer = buyer(1, "buyer@test.com");
        Order order = codOrder(100_000);
        order.setBuyer(buyer);
        order.setId(43);
        OrderDetail detail = line(order, TEN_KG, PRICE);
        QualityResult qr = baseSevereResult(detail, bd("1"), bd("9"));
        detail.setQualityResult(qr);
        detail.getProduct().setStockKg(BigDecimal.ZERO);
        detail.getProduct().setStockQuantity(0);

        order.setStatus(OrderStatus.QC_PENDING_BUYER);
        order.setRefundAmount(bd("90000"));
        order.setQcRefundedAmount(bd("90000"));
        order.setCodAmount(bd("10000"));

        authAs(buyer);
        when(orderRepository.findById(43)).thenReturn(Optional.of(order));

        qcSettlement.processQcBuyerDecision(43, false);

        assertEquals(OrderStatus.CANCELLED, order.getStatus());
        assertTrue(qr.isLineCancelled());
        verify(voucherUserService).restoreVoucherForOrder(order);
        verify(paymentService).refundQcDamage(eq(order), any(BigDecimal.class), contains("từ chối"));
    }

    // ─── TC-P*: PayOS buyer decision guards ────────────────────────────

    @Test
    @DisplayName("TC-P1 PayOS SEVERE accept → QUALITY_CHECKED")
    void p1_payosAccept() {
        User buyer = buyer(2, "p@test.com");
        Order order = payosOrder(100_000);
        order.setBuyer(buyer);
        order.setId(50);
        OrderDetail detail = line(order, TEN_KG, PRICE);
        QualityResult qr = baseSevereResult(detail, bd("1"), bd("9"));
        detail.setQualityResult(qr);
        order.setStatus(OrderStatus.QC_PENDING_BUYER);
        order.setQcRefundedAmount(bd("90000"));

        authAs(buyer);
        when(orderRepository.findById(50)).thenReturn(Optional.of(order));

        qcSettlement.processQcBuyerDecision(50, true);

        assertEquals(OrderStatus.QUALITY_CHECKED, order.getStatus());
        assertEquals(Boolean.TRUE, order.getQcBuyerAcceptedRemaining());
    }

    @Test
    @DisplayName("TC-P3 decision when not QC_PENDING_BUYER → QC_BUYER_DECISION_INVALID")
    void p3_invalidStatus() {
        User buyer = buyer(1, "buyer@test.com");
        Order order = payosOrder(100_000);
        order.setBuyer(buyer);
        order.setId(51);
        order.setStatus(OrderStatus.QUALITY_CHECKED);

        authAs(buyer);
        when(orderRepository.findById(51)).thenReturn(Optional.of(order));

        AppException ex = assertThrows(AppException.class,
                () -> qcSettlement.processQcBuyerDecision(51, true));
        assertEquals(ErrorCode.QC_BUYER_DECISION_INVALID, ex.getErrorCode());
    }

    @Test
    @DisplayName("TC-P4 other user → UNAUTHORIZED")
    void p4_unauthorized() {
        User owner = buyer(1, "owner@test.com");
        User other = buyer(2, "other@test.com");
        Order order = payosOrder(100_000);
        order.setBuyer(owner);
        order.setId(52);
        order.setStatus(OrderStatus.QC_PENDING_BUYER);

        authAs(other);
        when(orderRepository.findById(52)).thenReturn(Optional.of(order));

        AppException ex = assertThrows(AppException.class,
                () -> qcSettlement.processQcBuyerDecision(52, true));
        assertEquals(ErrorCode.UNAUTHORIZED, ex.getErrorCode());
    }

    // ─── TC-V*: vouchers ───────────────────────────────────────────────

    @Test
    @DisplayName("TC-V1 shop voucher + MINOR: no restore voucher")
    void v1_minor_noRestore() {
        Order order = payosOrder(90_000);
        OrderDetail detail = line(order, TEN_KG, PRICE);
        QualityResult qr = new QualityResult();
        qr.setOrderDetail(detail);

        qcSettlement.applyLineQcResult(order, detail, null, qr, bd("1.5"), TEN_KG);
        detail.setQualityResult(qr);
        qcSettlement.syncOrderAfterQcItem(order);

        verify(voucherUserService, never()).restoreVoucherForOrder(any());
        verify(voucherUserService, never()).restoreBatchVoucher(any(), any());
        verify(voucherUserService, never()).restoreBatchVoucher(any(), any(), anyBoolean());
    }

    @Test
    @DisplayName("TC-V2/V4 TOTAL cancel restores order voucher + batch voucher when present")
    void v2_total_restoresBatchVoucher() {
        Order order = payosOrder(100_000);
        CheckoutBatch batch = new CheckoutBatch();
        batch.setId(9);
        batch.setBatchVoucherUserId(java.util.UUID.randomUUID());
        order.setCheckoutBatch(batch);

        OrderDetail detail = line(order, TEN_KG, PRICE);
        detail.getProduct().setStockKg(BigDecimal.ZERO);
        detail.getProduct().setStockQuantity(0);
        QualityResult qr = new QualityResult();
        qr.setOrderDetail(detail);

        qcSettlement.applyLineQcResult(order, detail, null, qr, bd("10"), TEN_KG);
        detail.setQualityResult(qr);
        when(orderRepository.findByCheckoutBatchId(9)).thenReturn(List.of(order));

        qcSettlement.syncOrderAfterQcItem(order);

        assertEquals(OrderStatus.CANCELLED, order.getStatus());
        verify(voucherUserService).restoreVoucherForOrder(order);
        verify(voucherUserService).restoreBatchVoucher(eq(batch), anyList());
    }

    // ─── TC-M*: multi-line ─────────────────────────────────────────────

    @Test
    @DisplayName("TC-M1 first line QC only → QUALITY_CHECKING")
    void m1_partialCheck() {
        Order order = payosOrder(200_000);
        order.setStatus(OrderStatus.ARRIVED_AT_WAREHOUSE);
        OrderDetail d1 = line(order, TEN_KG, PRICE);
        OrderDetail d2 = line(order, TEN_KG, PRICE);
        order.setOrderDetails(new ArrayList<>(List.of(d1, d2)));

        QualityResult qr = new QualityResult();
        qr.setOrderDetail(d1);
        qcSettlement.applyLineQcResult(order, d1, null, qr, bd("0.4"), TEN_KG);
        d1.setQualityResult(qr);
        // d2 still unchecked
        qcSettlement.syncOrderAfterQcItem(order);

        assertEquals(OrderStatus.QUALITY_CHECKING, order.getStatus());
        verify(paymentService, never()).refundQcDamage(any(), any(), anyString());
    }

    @Test
    @DisplayName("TC-M2 one TOTAL + one OK → QUALITY_CHECKED (not cancel whole order)")
    void m2_partialTotal() {
        Order order = payosOrder(200_000);
        OrderDetail bad = line(order, TEN_KG, PRICE);
        OrderDetail ok = line(order, TEN_KG, PRICE);
        bad.getProduct().setStockKg(BigDecimal.ZERO);
        bad.getProduct().setStockQuantity(0);

        QualityResult qrBad = new QualityResult();
        qrBad.setOrderDetail(bad);
        qcSettlement.applyLineQcResult(order, bad, null, qrBad, bd("10"), TEN_KG);
        bad.setQualityResult(qrBad);

        QualityResult qrOk = new QualityResult();
        qrOk.setOrderDetail(ok);
        qrOk.setApprovedKg(TEN_KG);
        qcSettlement.applyLineQcResult(order, ok, null, qrOk, bd("0.2"), TEN_KG);
        ok.setQualityResult(qrOk);

        order.setOrderDetails(new ArrayList<>(List.of(bad, ok)));
        qcSettlement.syncOrderAfterQcItem(order);

        assertTrue(qrBad.isLineCancelled());
        assertFalse(qrOk.isLineCancelled());
        assertEquals(OrderStatus.QUALITY_CHECKED, order.getStatus());
        verify(voucherUserService, never()).restoreVoucherForOrder(any());
    }

    @Test
    @DisplayName("TC-M3 two SEVERE lines → still single QC_PENDING_BUYER")
    void m3_twoSevere() {
        Order order = payosOrder(200_000);
        OrderDetail d1 = line(order, TEN_KG, PRICE);
        OrderDetail d2 = line(order, TEN_KG, PRICE);

        QualityResult q1 = new QualityResult();
        q1.setOrderDetail(d1);
        q1.setApprovedKg(bd("1"));
        qcSettlement.applyLineQcResult(order, d1, null, q1, bd("9"), TEN_KG);
        d1.setQualityResult(q1);

        QualityResult q2 = new QualityResult();
        q2.setOrderDetail(d2);
        q2.setApprovedKg(bd("1"));
        qcSettlement.applyLineQcResult(order, d2, null, q2, bd("9"), TEN_KG);
        d2.setQualityResult(q2);

        order.setOrderDetails(new ArrayList<>(List.of(d1, d2)));
        qcSettlement.syncOrderAfterQcItem(order);

        assertEquals(OrderStatus.QC_PENDING_BUYER, order.getStatus());
        assertEquals(QcDamageTier.SEVERE, q1.getDamageTier());
        assertEquals(QcDamageTier.SEVERE, q2.getDamageTier());
    }

    // ─── helpers ───────────────────────────────────────────────────────

    private static BigDecimal bd(String v) {
        return new BigDecimal(v);
    }

    private Order payosOrder(long total) {
        Order order = new Order();
        order.setPaymentMethod("PAYOS");
        order.setTotalAmount(BigDecimal.valueOf(total));
        order.setStatus(OrderStatus.ARRIVED_AT_WAREHOUSE);
        order.setOrderDetails(new ArrayList<>());
        order.setBuyer(buyer(1, "buyer@test.com"));
        return order;
    }

    private Order codOrder(long total) {
        Order order = payosOrder(total);
        order.setPaymentMethod("COD");
        order.setCodAmount(BigDecimal.valueOf(total));
        return order;
    }

    private OrderDetail line(Order order, BigDecimal kg, BigDecimal pricePerKg) {
        Product product = new Product();
        product.setProductName("Rau test");
        product.setStockKg(BigDecimal.valueOf(100));
        product.setStockQuantity(100);

        OrderDetail detail = new OrderDetail();
        detail.setOrder(order);
        detail.setProduct(product);
        detail.setQuantityKg(kg);
        detail.setPricePerKg(pricePerKg);
        detail.setUnitPrice(pricePerKg);
        order.getOrderDetails().add(detail);
        return detail;
    }

    private QualityResult baseSevereResult(OrderDetail detail, BigDecimal approved, BigDecimal rejected) {
        QualityResult qr = new QualityResult();
        qr.setOrderDetail(detail);
        qr.setDamageTier(QcDamageTier.SEVERE);
        qr.setApprovedKg(approved);
        qr.setRejectedKg(rejected);
        qr.setRequestedKg(TEN_KG);
        qr.setLineRefundAmount(rejected.multiply(PRICE));
        qr.setLineCancelled(false);
        return qr;
    }

    private User buyer(int id, String email) {
        User u = new User();
        u.setId(id);
        u.setEmail(email);
        return u;
    }

    private void authAs(User user) {
        SecurityContextHolder.getContext().setAuthentication(
                new UsernamePasswordAuthenticationToken(user.getEmail(), "n/a"));
        when(userRepository.findByEmail(user.getEmail())).thenReturn(Optional.of(user));
    }
}
