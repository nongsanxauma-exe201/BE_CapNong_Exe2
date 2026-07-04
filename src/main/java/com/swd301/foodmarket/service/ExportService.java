package com.swd301.foodmarket.service;

import jakarta.servlet.http.HttpServletResponse;
import org.springframework.security.oauth2.jwt.Jwt;

import java.io.IOException;
import java.time.LocalDate;
import java.util.List;

public interface ExportService {

    void exportAdminTransactionsExcel(
            List<String> statuses,
            LocalDate from,
            LocalDate to,
            HttpServletResponse response
    ) throws IOException;

    void exportAdminTransactionsPdf(
            List<Integer> orderIds,
            LocalDate from,
            LocalDate to,
            HttpServletResponse response
    ) throws IOException;

    void exportShopOrdersExcel(
            Jwt jwt,
            List<String> statuses,
            LocalDate from,
            LocalDate to,
            HttpServletResponse response
    ) throws IOException;

    void exportOrderReceiptPdf(
            Jwt jwt,
            Integer orderId,
            HttpServletResponse response
    ) throws IOException;
}
