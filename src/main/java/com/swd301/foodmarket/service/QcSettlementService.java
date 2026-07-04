package com.swd301.foodmarket.service;

import com.swd301.foodmarket.dto.response.OrderResponse;
import com.swd301.foodmarket.entity.Order;
import com.swd301.foodmarket.entity.OrderDetail;
import com.swd301.foodmarket.entity.OrderMysteryBox;
import com.swd301.foodmarket.entity.QualityResult;
import com.swd301.foodmarket.enums.QcDamageTier;

import java.math.BigDecimal;

public interface QcSettlementService {

    QcDamageTier computeDamageTier(BigDecimal rejectedKg, BigDecimal requestedKg);

    void applyLineQcResult(
            Order order,
            OrderDetail orderDetail,
            OrderMysteryBox mysteryBox,
            QualityResult qualityResult,
            BigDecimal rejectedKg,
            BigDecimal requestedKg
    );

    void syncOrderAfterQcItem(Order order);

    OrderResponse processQcBuyerDecision(Integer orderId, boolean acceptRemaining);
}
