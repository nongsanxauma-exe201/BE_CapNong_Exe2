package com.swd301.foodmarket.service.impl;

import com.swd301.foodmarket.entity.Order;
import com.swd301.foodmarket.entity.OrderDetail;
import com.swd301.foodmarket.entity.OrderMysteryBox;
import com.swd301.foodmarket.entity.User;
import com.swd301.foodmarket.enums.OrderStatus;
import com.swd301.foodmarket.repository.OrderRepository;
import com.swd301.foodmarket.repository.UserRepository;
import com.swd301.foodmarket.service.OrderRewardService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.math.RoundingMode;

@Service
@RequiredArgsConstructor
public class OrderRewardServiceImpl implements OrderRewardService {
    private static final BigDecimal DEFAULT_UNIT_WEIGHT_KG = BigDecimal.ONE;

    private final OrderRepository orderRepository;
    private final UserRepository userRepository;

    @Override
    public void applyRewardIfEligible(Order order) {
        if (order == null || Boolean.TRUE.equals(order.getRewardApplied()) || !isRewardEligible(order)) {
            return;
        }

        BigDecimal rescuedWeight = calculateOrderWeight(order);
        if (rescuedWeight.signum() <= 0) {
            return;
        }

        BigDecimal rewardPoints = rescuedWeight;
        order.setRescuedWeight(rescuedWeight);
        order.setRewardPoints(rewardPoints);
        order.setRewardApplied(true);

        User buyer = order.getBuyer();
        buyer.setTotalRescuedWeight(safe(buyer.getTotalRescuedWeight()).add(rescuedWeight));
        buyer.setLoyaltyPoints(safe(buyer.getLoyaltyPoints()).add(rewardPoints));

        userRepository.save(buyer);
        orderRepository.save(order);
    }

    @Override
    public void revokeRewardIfApplied(Order order) {
        if (order == null || !Boolean.TRUE.equals(order.getRewardApplied())) {
            return;
        }

        BigDecimal rescuedWeight = safe(order.getRescuedWeight());
        BigDecimal rewardPoints = safe(order.getRewardPoints());
        User buyer = order.getBuyer();

        buyer.setTotalRescuedWeight(safe(buyer.getTotalRescuedWeight()).subtract(rescuedWeight).max(BigDecimal.ZERO));
        buyer.setLoyaltyPoints(safe(buyer.getLoyaltyPoints()).subtract(rewardPoints).max(BigDecimal.ZERO));

        order.setRewardApplied(false);
        order.setRewardPoints(BigDecimal.ZERO.setScale(2, RoundingMode.HALF_UP));
        order.setRescuedWeight(BigDecimal.ZERO.setScale(2, RoundingMode.HALF_UP));

        userRepository.save(buyer);
        orderRepository.save(order);
    }

    private boolean isRewardEligible(Order order) {
        if (order.getStatus() == OrderStatus.PAID) {
            return true;
        }
        return order.getStatus() == OrderStatus.DELIVERED
                && "COD".equalsIgnoreCase(order.getPaymentMethod());
    }

    private BigDecimal calculateOrderWeight(Order order) {
        BigDecimal total = BigDecimal.ZERO;

        if (order.getOrderDetails() != null) {
            for (OrderDetail detail : order.getOrderDetails()) {
                BigDecimal itemWeight = safe(detail.getRescuedWeightKg());
                if (itemWeight.signum() <= 0) {
                    BigDecimal unitWeight = resolveUnitWeight(detail.getUnitWeightKg());
                    itemWeight = unitWeight.multiply(BigDecimal.valueOf(safeQuantity(detail.getQuantity())));
                    detail.setUnitWeightKg(unitWeight);
                    detail.setRescuedWeightKg(itemWeight);
                }
                total = total.add(itemWeight);
            }
        }

        if (order.getMysteryBoxes() != null) {
            for (OrderMysteryBox orderBox : order.getMysteryBoxes()) {
                BigDecimal itemWeight = safe(orderBox.getRescuedWeightKg());
                if (itemWeight.signum() <= 0) {
                    BigDecimal unitWeight = resolveUnitWeight(orderBox.getUnitWeightKg());
                    itemWeight = unitWeight.multiply(BigDecimal.valueOf(safeQuantity(orderBox.getQuantity())));
                    orderBox.setUnitWeightKg(unitWeight);
                    orderBox.setRescuedWeightKg(itemWeight);
                }
                total = total.add(itemWeight);
            }
        }

        return total.setScale(2, RoundingMode.HALF_UP);
    }

    private BigDecimal resolveUnitWeight(BigDecimal weight) {
        BigDecimal normalized = safe(weight);
        return normalized.signum() > 0 ? normalized : DEFAULT_UNIT_WEIGHT_KG;
    }

    private int safeQuantity(Integer quantity) {
        return quantity == null || quantity <= 0 ? 0 : quantity;
    }

    private BigDecimal safe(BigDecimal value) {
        return value == null ? BigDecimal.ZERO : value;
    }
}
