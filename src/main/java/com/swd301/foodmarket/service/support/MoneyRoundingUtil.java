package com.swd301.foodmarket.service.support;

import java.math.BigDecimal;
import java.math.RoundingMode;

public final class MoneyRoundingUtil {

    private static final BigDecimal COMMISSION_RATE = BigDecimal.valueOf(0.1);

    private MoneyRoundingUtil() {
    }

    public static BigDecimal commission(BigDecimal base) {
        if (base == null || base.signum() <= 0) {
            return BigDecimal.ZERO;
        }
        return base.multiply(COMMISSION_RATE).setScale(0, RoundingMode.HALF_UP);
    }

    public static BigDecimal netAfterCommission(BigDecimal gross) {
        if (gross == null || gross.signum() <= 0) {
            return BigDecimal.ZERO;
        }
        return gross.subtract(commission(gross));
    }
}
