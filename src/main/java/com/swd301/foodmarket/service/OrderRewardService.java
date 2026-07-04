package com.swd301.foodmarket.service;

import com.swd301.foodmarket.entity.Order;

public interface OrderRewardService {
    void applyRewardIfEligible(Order order);

    void revokeRewardIfApplied(Order order);
}
