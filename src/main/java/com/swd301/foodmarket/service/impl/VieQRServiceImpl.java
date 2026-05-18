package com.swd301.foodmarket.service.impl;

import com.cloudinary.api.exceptions.ApiException;
import com.swd301.foodmarket.exception.AppException;
import com.swd301.foodmarket.exception.ErrorCode;
import com.swd301.foodmarket.service.VieQRService;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;

@Service
public class
VieQRServiceImpl  implements VieQRService {
    @Override
    public String generateVieQRCode(String bankBin, String accountNo, BigDecimal amount, String addInfo, String accountName) {
      {
            if (bankBin == null || bankBin.isBlank()) {
                throw new AppException(ErrorCode.BANK_NOT_FOUND);
            }

            if (accountNo == null || accountNo.isBlank()) {
                throw new AppException(ErrorCode.BANK_ACCOUNT_NOT_FOUND);
            }

            if (amount == null || amount.compareTo(BigDecimal.ZERO) <= 0) {
                throw new AppException(ErrorCode.INVALID_AMOUNT);
            }

            if (addInfo == null) {
                addInfo = "";
            }

            if (accountName == null) {
                accountName = "";
            }

            return org.springframework.web.util.UriComponentsBuilder
                    .fromHttpUrl(
                            "https://img.vietqr.io/image/"
                                    + bankBin
                                    + "-"
                                    + accountNo
                                    + "-compact2.png"
                    )
                    .queryParam("amount", amount.toPlainString())
                    .queryParam("addInfo", addInfo)
                    .queryParam("accountName", accountName)
                    .toUriString();
        }

    }
}
