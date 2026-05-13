package com.swd301.foodmarket.dto.response;

import lombok.Data;

@Data
public class PayOSCreateResponse {
    private String code;
    private String desc;
    private DataObj data;

    @Data
    public static class DataObj {
        private String bin;
        private String accountNumber;
        private String accountName;
        private String amount;
        private String description;
        private String orderCode;
        private String qrCode;
        private String paymentLinkId;
        private String checkoutUrl;
    }

}
