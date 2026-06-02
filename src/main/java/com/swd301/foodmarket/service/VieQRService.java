package com.swd301.foodmarket.service;

import java.math.BigDecimal;

public interface VieQRService {
    String generateVieQRCode( String bankBin,
                              String accountNo,
                              BigDecimal amount,
                              String addInfo,
                              String accountName);
}
