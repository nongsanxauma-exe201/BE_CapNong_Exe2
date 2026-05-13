package com.swd301.foodmarket.dto.response;

import com.swd301.foodmarket.enums.WithdrawRequestStatus;
import lombok.Data;

import java.math.BigDecimal;
import java.time.LocalDateTime;

@Data
public class WithdrawRequestResponse {
    private Integer id;
    private BigDecimal amount;
    private BigDecimal fee;
    private BigDecimal receiveAmount;

    private String reason;
    private WithdrawRequestStatus status;
    private String adminNote;
    private LocalDateTime processedAt;

    private Integer walletId;
    private Integer shopOwnerId;
    private Integer shipperId;

    // Bank info
    private String bankName;
    private String bankAccountNumber;
    private String bankAccountHolder;

    // Payout info
    private String payoutProvider;
    private String payoutStatus;
    private String payoutTransactionId;
    private String payoutMessage;
    private LocalDateTime payoutAt;


    private String qrCodeUrl;
    private String checkoutUrl;
}