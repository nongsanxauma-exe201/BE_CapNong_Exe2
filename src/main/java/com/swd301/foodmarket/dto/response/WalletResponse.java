package com.swd301.foodmarket.dto.response;

import com.swd301.foodmarket.enums.WalletStatus;
import lombok.*;
import lombok.experimental.FieldDefaults;

import java.math.BigDecimal;
import java.time.LocalDateTime;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@FieldDefaults(level = AccessLevel.PRIVATE)
public class WalletResponse {

    Integer id;

    Integer shopOwnerId;

    Integer shipperId;

    WalletStatus status;

    BigDecimal totalBalance;
    BigDecimal frozenBalance;
//    BigDecimal withdrawAmount;
    BigDecimal totalRevenueAllTime;
    BigDecimal totalWithdrawn;
    BigDecimal commissionWallet;

    LocalDateTime createAt;
}