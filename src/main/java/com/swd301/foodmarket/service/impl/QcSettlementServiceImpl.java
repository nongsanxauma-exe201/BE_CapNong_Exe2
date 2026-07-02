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
import com.swd301.foodmarket.service.QcSettlementService;
import com.swd301.foodmarket.service.TrackingService;
import com.swd301.foodmarket.service.VoucherService;
import jakarta.transaction.Transactional;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.util.ArrayList;
import java.util.List;

@Slf4j
@Service
@RequiredArgsConstructor
public class QcSettlementServiceImpl implements QcSettlementService {

    private static final BigDecimal TOLERABLE_MAX_PERCENT = BigDecimal.valueOf(5);
    private static final BigDecimal MINOR_MAX_PERCENT = BigDecimal.valueOf(20);
    private static final BigDecimal SEVERE_MIN_PERCENT = BigDecimal.valueOf(80);
    private static final int BUYER_DECISION_HOURS = 24;

    private final OrderRepository orderRepository;
    private final ProductRepository productRepository;
    private final MysteryBoxRepository mysteryBoxRepository;
    private final PaymentService paymentService;
    private final VoucherService voucherService;
    private final NotificationService notificationService;
    private final TrackingService trackingService;
    private final OrderMapper orderMapper;
    private final UserRepository userRepository;

    @Override
    public QcDamageTier computeDamageTier(BigDecimal rejectedKg, BigDecimal requestedKg) {
        if (rejectedKg == null || rejectedKg.signum() <= 0
                || requestedKg == null || requestedKg.signum() <= 0) {
            return QcDamageTier.TOLERABLE;
        }

        BigDecimal percent = rejectedKg
                .multiply(BigDecimal.valueOf(100))
                .divide(requestedKg, 2, RoundingMode.HALF_UP);

        if (percent.compareTo(BigDecimal.valueOf(100)) >= 0) {
            return QcDamageTier.TOTAL;
        }
        if (percent.compareTo(SEVERE_MIN_PERCENT) >= 0) {
            return QcDamageTier.SEVERE;
        }
        if (percent.compareTo(MINOR_MAX_PERCENT) > 0) {
            return QcDamageTier.MODERATE;
        }
        if (percent.compareTo(TOLERABLE_MAX_PERCENT) > 0) {
            return QcDamageTier.MINOR;
        }
        return QcDamageTier.TOLERABLE;
    }

    @Override
    @Transactional
    public void applyLineQcResult(
            Order order,
            OrderDetail orderDetail,
            OrderMysteryBox mysteryBox,
            QualityResult qualityResult,
            BigDecimal rejectedKg,
            BigDecimal requestedKg
    ) {
        QcDamageTier tier = computeDamageTier(rejectedKg, requestedKg);
        BigDecimal lineRefund = calculateLineRefund(tier, orderDetail, mysteryBox, rejectedKg, requestedKg);
        String itemName = resolveItemName(orderDetail, mysteryBox);

        qualityResult.setDamageTier(tier);
        qualityResult.setLineRefundAmount(lineRefund);
        qualityResult.setLineCancelled(tier == QcDamageTier.TOTAL);
        qualityResult.setRefundedKg(rejectedKg != null ? rejectedKg : BigDecimal.ZERO);

        if (lineRefund.signum() > 0) {
            String message = buildRefundMessage(itemName, tier, rejectedKg, lineRefund);
            BigDecimal currentRefund = order.getRefundAmount() != null ? order.getRefundAmount() : BigDecimal.ZERO;
            order.setRefundAmount(currentRefund.add(lineRefund));
            order.setRefundReason(order.getRefundReason() == null ? message : order.getRefundReason() + "\n" + message);
        }

        if (tier == QcDamageTier.MODERATE || tier == QcDamageTier.SEVERE || tier == QcDamageTier.TOTAL) {
            if (isRejectedOverDamageVoucherThreshold(rejectedKg, requestedKg)) {
                voucherService.createAutoVoucherForDamage(order, buildDamageVoucherReason(rejectedKg, requestedKg));
                qualityResult.setAutoVoucherCreated(true);
            }
        }

        updateCodAmount(order);
        orderRepository.save(order);
    }

    @Override
    @Transactional
    public void syncOrderAfterQcItem(Order order) {
        if (!isAllItemsChecked(order)) {
            if (order.getStatus() == OrderStatus.ARRIVED_AT_WAREHOUSE) {
                order.setStatus(OrderStatus.QUALITY_CHECKING);
                orderRepository.save(order);
            }
            return;
        }

        finalizeOrderAfterAllQc(order);
    }

    @Override
    @Transactional
    public OrderResponse processQcBuyerDecision(Integer orderId, boolean acceptRemaining) {
        Order order = orderRepository.findById(orderId)
                .orElseThrow(() -> new AppException(ErrorCode.ORDER_NOT_FOUND));

        User currentUser = getCurrentUser();
        if (!order.getBuyer().getId().equals(currentUser.getId())) {
            throw new AppException(ErrorCode.UNAUTHORIZED);
        }
        if (order.getStatus() != OrderStatus.QC_PENDING_BUYER) {
            throw new AppException(ErrorCode.QC_BUYER_DECISION_INVALID);
        }

        order.setQcBuyerAcceptedRemaining(acceptRemaining);
        BigDecimal additionalRefund = BigDecimal.ZERO;

        if (!acceptRemaining) {
            additionalRefund = calculateSevereRemainingRefund(order);
            if (additionalRefund.signum() > 0) {
                BigDecimal actualRefunded = paymentService.refundQcDamage(order, additionalRefund,
                        "Hoàn phần còn lại do buyer từ chối nhận đơn #" + order.getId());
                if (actualRefunded.signum() > 0) {
                    order.setRefundReason((order.getRefundReason() == null ? "" : order.getRefundReason() + "\n")
                            + "Buyer từ chối nhận phần còn lại, hoàn thêm " + actualRefunded + "đ");
                }
                additionalRefund = actualRefunded;
            }
        }

        syncRefundAmountFromQcRefunded(order);
        updateCodAmount(order);
        applyPostQcStatus(order);
        orderRepository.save(order);
        trackingService.trackingOrder(order, null);

        notifyBuyerQcDecisionRecorded(order, acceptRemaining, additionalRefund);
        return orderMapper.toResponse(order);
    }

    private void finalizeOrderAfterAllQc(Order order) {
        List<QualityResult> results = collectQualityResults(order);
        int totalLines = results.size();
        long totalCancelled = results.stream().filter(QualityResult::isLineCancelled).count();
        boolean hasSevere = results.stream().anyMatch(r -> r.getDamageTier() == QcDamageTier.SEVERE);

        BigDecimal immediateRefund = BigDecimal.ZERO;
        for (QualityResult result : results) {
            if (result.isLineCancelled()) {
                restoreStockForCancelledLine(result);
            }
            if (result.getLineRefundAmount() != null && result.getLineRefundAmount().signum() > 0
                    && shouldRefundImmediately(result)) {
                immediateRefund = immediateRefund.add(result.getLineRefundAmount());
            }
        }

        if (immediateRefund.signum() > 0) {
            if (hasSevere) {
                BigDecimal reservedForBuyerDecision = calculateSevereApprovedRemainingValue(order);
                BigDecimal buyerPaid = order.getTotalAmount() != null ? order.getTotalAmount() : BigDecimal.ZERO;
                BigDecimal alreadyQcRefunded = order.getQcRefundedAmount() != null
                        ? order.getQcRefundedAmount()
                        : BigDecimal.ZERO;
                BigDecimal maxImmediate = buyerPaid.subtract(alreadyQcRefunded).subtract(reservedForBuyerDecision);
                if (maxImmediate.compareTo(BigDecimal.ZERO) < 0) {
                    maxImmediate = BigDecimal.ZERO;
                }
                if (immediateRefund.compareTo(maxImmediate) > 0) {
                    log.info("[QC_IMMEDIATE_REFUND_CAP] orderId={}, requested={}, cappedTo={}, reservedForBuyer={}",
                            order.getId(), immediateRefund, maxImmediate, reservedForBuyerDecision);
                    immediateRefund = maxImmediate;
                }
            }
        }

        if (immediateRefund.signum() > 0) {
            paymentService.refundQcDamage(order, immediateRefund,
                    "Hoàn tiền QC đơn hàng #" + order.getId());
            for (QualityResult result : results) {
                if (shouldRefundImmediately(result) && result.getLineRefundAmount() != null) {
                    result.setRefunded(true);
                }
            }
        }

        if (totalLines > 0 && totalCancelled == totalLines) {
            cancelEntireOrder(order);
            return;
        }

        if (totalLines == 1 && results.get(0).isLineCancelled()) {
            cancelEntireOrder(order);
            return;
        }

        if (hasSevere) {
            order.setStatus(OrderStatus.QC_PENDING_BUYER);
            order.setQcBuyerDecisionDeadline(LocalDateTime.now(ZoneId.of("Asia/Ho_Chi_Minh")).plusHours(BUYER_DECISION_HOURS));
            order.setQcBuyerAcceptedRemaining(null);
            orderRepository.save(order);
            notifyBuyerQcPendingDecision(order);
            return;
        }

        order.setStatus(OrderStatus.QUALITY_CHECKED);
        orderRepository.save(order);
        notifyBuyerQcCompleted(order, results);
    }

    private void cancelEntireOrder(Order order) {
        BigDecimal totalAmount = order.getTotalAmount() != null ? order.getTotalAmount() : BigDecimal.ZERO;
        BigDecimal alreadyRefunded = order.getQcRefundedAmount() != null ? order.getQcRefundedAmount() : BigDecimal.ZERO;
        BigDecimal remaining = totalAmount.subtract(alreadyRefunded);

        if (remaining.signum() > 0) {
            order.setRefundAmount(totalAmount);
            paymentService.refundQcDamage(order, remaining,
                    "Hủy đơn do QC 100% hư hỏng #" + order.getId());
        }

        order.setStatus(OrderStatus.CANCELLED);
        order.setRefundReason((order.getRefundReason() == null ? "" : order.getRefundReason() + "\n")
                + "Đơn hàng bị hủy do toàn bộ hàng không đạt chất lượng");
        orderRepository.save(order);

        grantDamageVoucherForQcCancelledOrder(order);

        Notification notification = Notification.builder()
                .title("Đơn hàng #" + order.getId() + " đã bị hủy")
                .message(buildQcCancelRefundMessage(order))
                .evidence(String.valueOf(order.getId()))
                .receiverType("USER_" + order.getBuyer().getId())
                .build();
        notificationService.sendNotificationWebsocket(order.getBuyer(), notification);
    }

    private void grantDamageVoucherForQcCancelledOrder(Order order) {
        if (order == null || order.getBuyer() == null) {
            return;
        }
        try {
            voucherService.createAutoVoucherForDamage(
                    order,
                    "Đơn #" + order.getId() + " bị hủy do toàn bộ hàng không đạt chất lượng (QC 100%)"
            );
        } catch (Exception e) {
            log.warn("[QC_CANCEL_VOUCHER] Không thể tạo voucher hư đơn cho đơn #{}: {}",
                    order.getId(), e.getMessage());
        }
        voucherService.grantDamageVoucherForDeliveredOrder(order);
    }

    private void applyPostQcStatus(Order order) {
        if (!hasShippableLines(order)) {
            BigDecimal totalAmount = order.getTotalAmount() != null ? order.getTotalAmount() : BigDecimal.ZERO;
            BigDecimal alreadyRefunded = order.getQcRefundedAmount() != null ? order.getQcRefundedAmount() : BigDecimal.ZERO;
            BigDecimal remaining = totalAmount.subtract(alreadyRefunded);
            if (remaining.signum() > 0) {
                paymentService.refundQcDamage(order, remaining,
                        "Hủy đơn sau QC buyer từ chối nhận #" + order.getId());
            }
            order.setStatus(OrderStatus.CANCELLED);
            return;
        }
        order.setStatus(OrderStatus.QUALITY_CHECKED);
    }

    private boolean hasShippableLines(Order order) {
        for (QualityResult result : collectQualityResults(order)) {
            if (result.isLineCancelled()) {
                continue;
            }
            if (result.getDamageTier() == QcDamageTier.SEVERE) {
                if (Boolean.TRUE.equals(order.getQcBuyerAcceptedRemaining())
                        && result.getApprovedKg() != null
                        && result.getApprovedKg().signum() > 0) {
                    return true;
                }
                continue;
            }
            if (result.getApprovedKg() == null || result.getApprovedKg().signum() > 0) {
                return true;
            }
        }
        return false;
    }

    private BigDecimal calculateSevereApprovedRemainingValue(Order order) {
        BigDecimal total = BigDecimal.ZERO;
        for (QualityResult result : collectQualityResults(order)) {
            if (result.getDamageTier() != QcDamageTier.SEVERE || result.isLineCancelled()) {
                continue;
            }
            BigDecimal approvedKg = result.getApprovedKg() != null ? result.getApprovedKg() : BigDecimal.ZERO;
            if (approvedKg.signum() <= 0) {
                continue;
            }
            BigDecimal unitPrice = resolveUnitPrice(result);
            total = total.add(unitPrice.multiply(approvedKg).setScale(0, RoundingMode.HALF_UP));
        }
        return total;
    }

    private BigDecimal calculateSevereRemainingRefund(Order order) {
        BigDecimal total = BigDecimal.ZERO;
        for (QualityResult result : collectQualityResults(order)) {
            if (result.getDamageTier() != QcDamageTier.SEVERE || result.isLineCancelled()) {
                continue;
            }
            BigDecimal approvedKg = result.getApprovedKg() != null ? result.getApprovedKg() : BigDecimal.ZERO;
            if (approvedKg.signum() <= 0) {
                continue;
            }
            BigDecimal unitPrice = resolveUnitPrice(result);
            total = total.add(unitPrice.multiply(approvedKg).setScale(0, RoundingMode.HALF_UP));
            result.setLineCancelled(true);
            restoreStockForCancelledLine(result);
        }
        return total;
    }

    private BigDecimal calculateLineRefund(
            QcDamageTier tier,
            OrderDetail orderDetail,
            OrderMysteryBox mysteryBox,
            BigDecimal rejectedKg,
            BigDecimal requestedKg
    ) {
        if (tier == QcDamageTier.TOLERABLE) {
            return BigDecimal.ZERO;
        }
        BigDecimal unitPrice = orderDetail != null
                ? (orderDetail.getPricePerKg() != null ? orderDetail.getPricePerKg() : orderDetail.getUnitPrice())
                : mysteryBox.getMysteryBox().getPrice();

        if (tier == QcDamageTier.TOTAL) {
            if (orderDetail != null) {
                BigDecimal qtyKg = orderDetail.getQuantityKg() != null
                        ? orderDetail.getQuantityKg()
                        : BigDecimal.valueOf(orderDetail.getQuantity() == null ? 0 : orderDetail.getQuantity());
                return unitPrice.multiply(qtyKg).setScale(0, RoundingMode.HALF_UP);
            }
            BigDecimal qtyKg = mysteryBox.getRescuedWeightKg() != null
                    ? mysteryBox.getRescuedWeightKg()
                    : BigDecimal.valueOf(mysteryBox.getQuantity() == null ? 0 : mysteryBox.getQuantity());
            return unitPrice.multiply(qtyKg).setScale(0, RoundingMode.HALF_UP);
        }

        if (rejectedKg == null || rejectedKg.signum() <= 0) {
            return BigDecimal.ZERO;
        }
        return unitPrice.multiply(rejectedKg).setScale(0, RoundingMode.HALF_UP);
    }

    private boolean shouldRefundImmediately(QualityResult result) {
        if (result.isRefunded()) {
            return false;
        }
        QcDamageTier tier = result.getDamageTier();
        return tier == QcDamageTier.TOTAL || tier == QcDamageTier.SEVERE;
    }

    private void updateCodAmount(Order order) {
        if (!"COD".equalsIgnoreCase(order.getPaymentMethod())) {
            return;
        }
        BigDecimal totalAmount = order.getTotalAmount() != null ? order.getTotalAmount() : BigDecimal.ZERO;
        BigDecimal refund = order.getRefundAmount() != null ? order.getRefundAmount() : BigDecimal.ZERO;
        if (refund.compareTo(totalAmount) > 0) {
            log.warn("[COD_REFUND_CAP] orderId={} refundAmount {} exceeds total {}, capping",
                    order.getId(), refund, totalAmount);
            refund = totalAmount;
            order.setRefundAmount(refund);
        }
        BigDecimal codAmount = totalAmount.subtract(refund);
        if (codAmount.compareTo(BigDecimal.ZERO) < 0) {
            codAmount = BigDecimal.ZERO;
        }
        order.setCodAmount(codAmount);
    }

    /** Đồng bộ refundAmount theo qcRefundedAmount thực tế, tránh vượt tổng đơn. */
    private void syncRefundAmountFromQcRefunded(Order order) {
        BigDecimal totalAmount = order.getTotalAmount() != null ? order.getTotalAmount() : BigDecimal.ZERO;
        BigDecimal qcRefunded = order.getQcRefundedAmount() != null ? order.getQcRefundedAmount() : BigDecimal.ZERO;
        if (qcRefunded.signum() > 0) {
            order.setRefundAmount(qcRefunded.min(totalAmount));
        }
    }

    private boolean isAllItemsChecked(Order order) {
        int uncheckedProduct = order.getOrderDetails() == null ? 0
                : (int) order.getOrderDetails().stream().filter(d -> d.getQualityResult() == null).count();
        int uncheckedMystery = order.getMysteryBoxes() == null ? 0
                : (int) order.getMysteryBoxes().stream().filter(b -> b.getQualityResult() == null).count();
        return uncheckedProduct + uncheckedMystery == 0;
    }

    private List<QualityResult> collectQualityResults(Order order) {
        List<QualityResult> results = new ArrayList<>();
        if (order.getOrderDetails() != null) {
            for (OrderDetail detail : order.getOrderDetails()) {
                if (detail.getQualityResult() != null) {
                    results.add(detail.getQualityResult());
                }
            }
        }
        if (order.getMysteryBoxes() != null) {
            for (OrderMysteryBox box : order.getMysteryBoxes()) {
                if (box.getQualityResult() != null) {
                    results.add(box.getQualityResult());
                }
            }
        }
        return results;
    }

    private void restoreStockForCancelledLine(QualityResult result) {
        if (result.getOrderDetail() != null) {
            OrderDetail detail = result.getOrderDetail();
            Product product = detail.getProduct();
            BigDecimal kg = detail.getQuantityKg() != null
                    ? detail.getQuantityKg()
                    : BigDecimal.valueOf(detail.getQuantity() == null ? 0 : detail.getQuantity());
            BigDecimal currentStock = product.getStockKg() != null ? product.getStockKg() : BigDecimal.ZERO;
            product.setStockKg(currentStock.add(kg));
            product.setStockQuantity(product.getStockKg().setScale(0, RoundingMode.DOWN).intValue());
            productRepository.save(product);
        } else if (result.getOrderMysteryBox() != null) {
            OrderMysteryBox orderBox = result.getOrderMysteryBox();
            MysteryBox box = orderBox.getMysteryBox();
            int qty = orderBox.getQuantity() == null ? 0 : orderBox.getQuantity();
            box.setSoldQuantity(Math.max(0, box.getSoldQuantity() - qty));
            if (!Boolean.TRUE.equals(box.getIsActive()) && box.getSoldQuantity() < box.getTotalQuantity()) {
                box.setIsActive(true);
            }
            mysteryBoxRepository.save(box);
        }
    }

    private BigDecimal resolveUnitPrice(QualityResult result) {
        if (result.getOrderDetail() != null) {
            OrderDetail detail = result.getOrderDetail();
            return detail.getPricePerKg() != null ? detail.getPricePerKg() : detail.getUnitPrice();
        }
        return result.getOrderMysteryBox().getMysteryBox().getPrice();
    }

    private String resolveItemName(OrderDetail orderDetail, OrderMysteryBox mysteryBox) {
        if (orderDetail != null) {
            return orderDetail.getProduct().getProductName();
        }
        return mysteryBox.getMysteryBox().getDescription();
    }

    private String buildRefundMessage(String itemName, QcDamageTier tier, BigDecimal rejectedKg, BigDecimal refund) {
        if (tier == QcDamageTier.TOTAL) {
            return "Sản phẩm " + itemName + " hư 100%, hủy dòng, hoàn " + refund + "đ";
        }
        return "Sản phẩm " + itemName + " bị hư hỏng "
                + (rejectedKg != null ? rejectedKg.stripTrailingZeros().toPlainString() : "0") + " kg"
                + " (" + tier.name() + "), hoàn " + refund + "đ";
    }

    private boolean isRejectedOverDamageVoucherThreshold(BigDecimal rejectedKg, BigDecimal requestedKg) {
        if (rejectedKg == null || requestedKg == null || requestedKg.signum() <= 0) {
            return false;
        }
        BigDecimal rejectedPercent = rejectedKg
                .multiply(BigDecimal.valueOf(100))
                .divide(requestedKg, 2, RoundingMode.HALF_UP);
        return rejectedPercent.compareTo(MINOR_MAX_PERCENT) > 0;
    }

    private String buildDamageVoucherReason(BigDecimal rejectedKg, BigDecimal requestedKg) {
        BigDecimal rejectedPercent = rejectedKg
                .multiply(BigDecimal.valueOf(100))
                .divide(requestedKg, 2, RoundingMode.HALF_UP);
        return "QC ghi nhận khối lượng không đạt chuẩn "
                + rejectedKg.stripTrailingZeros().toPlainString() + " kg / "
                + requestedKg.stripTrailingZeros().toPlainString() + " kg ("
                + rejectedPercent.stripTrailingZeros().toPlainString() + "%), vượt ngưỡng 20%";
    }

    private void notifyBuyerQcPendingDecision(Order order) {
        Notification notification = Notification.builder()
                .title("Cần xác nhận đơn #" + order.getId())
                .message("Hàng có khối lượng hư nặng (≥80%). Phần kg hư đã được hoàn. "
                        + "Bạn có muốn nhận phần còn lại không? Vui lòng chọn trong Đơn hàng của tôi.")
                .evidence(String.valueOf(order.getId()))
                .receiverType("USER_" + order.getBuyer().getId())
                .build();
        notificationService.sendNotificationWebsocket(order.getBuyer(), notification);
    }

    private void notifyBuyerQcCompleted(Order order, List<QualityResult> results) {
        BigDecimal refund = order.getRefundAmount() != null ? order.getRefundAmount() : BigDecimal.ZERO;
        if (refund.signum() <= 0) {
            return;
        }
        Notification notification = Notification.builder()
                .title("Kết quả kiểm định đơn #" + order.getId())
                .message("Đơn hàng đã qua kiểm định. Tổng hoàn tiền: "
                        + String.format("%,.0f", refund) + "đ đã được ghi nhận.")
                .evidence(String.valueOf(order.getId()))
                .receiverType("USER_" + order.getBuyer().getId())
                .build();
        notificationService.sendNotificationWebsocket(order.getBuyer(), notification);
    }

    private void notifyBuyerQcDecisionRecorded(Order order, boolean acceptRemaining, BigDecimal additionalRefund) {
        String msg = acceptRemaining
                ? "Bạn đã chọn nhận phần hàng còn lại. Đơn sẽ được đóng gói và giao."
                : additionalRefund.signum() > 0
                ? buildBuyerRejectRefundMessage(order, additionalRefund)
                : "Bạn đã từ chối nhận phần còn lại. Đơn sẽ được cập nhật theo kết quả QC.";
        Notification notification = Notification.builder()
                .title("Xác nhận QC đơn #" + order.getId())
                .message(msg)
                .evidence(String.valueOf(order.getId()))
                .receiverType("USER_" + order.getBuyer().getId())
                .build();
        notificationService.sendNotificationWebsocket(order.getBuyer(), notification);
    }

    private String buildBuyerRejectRefundMessage(Order order, BigDecimal additionalRefund) {
        String amount = String.format("%,.0f", additionalRefund) + "đ";
        if ("COD".equalsIgnoreCase(order.getPaymentMethod())) {
            return "Bạn đã từ chối nhận phần còn lại. " + amount
                    + " sẽ được trừ khỏi số tiền COD khi nhận hàng.";
        }
        return "Bạn đã từ chối nhận phần còn lại. Hoàn thêm " + amount + " vào ví.";
    }

    private String buildQcCancelRefundMessage(Order order) {
        if ("COD".equalsIgnoreCase(order.getPaymentMethod())) {
            return "Toàn bộ hàng không đạt chất lượng. Số tiền COD khi nhận hàng đã được cập nhật (không thu thêm phần hàng hủy).";
        }
        return "Toàn bộ hàng không đạt chất lượng. Tiền đã được hoàn vào ví của bạn.";
    }

    private User getCurrentUser() {
        String email = SecurityContextHolder.getContext().getAuthentication().getName();
        return userRepository.findByEmail(email)
                .orElseThrow(() -> new AppException(ErrorCode.USER_NOT_EXISTED));
    }
}
