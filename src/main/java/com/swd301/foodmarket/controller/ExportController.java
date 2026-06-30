package com.swd301.foodmarket.controller;

import com.swd301.foodmarket.service.ExportService;
import jakarta.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.web.bind.annotation.*;

import java.io.IOException;
import java.time.LocalDate;
import java.util.List;

@RestController
@RequestMapping("/export")
@RequiredArgsConstructor
public class ExportController {

    private final ExportService exportService;

    /**
     * GET /export/admin/transactions/excel
     * Admin xuất toàn bộ giao dịch ra Excel
     */
    @GetMapping("/admin/transactions/excel")
    @PreAuthorize("hasRole('ADMIN')")
    public void exportAdminTransactionsExcel(
            @RequestParam(required = false) List<String> statuses,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate from,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate to,
            HttpServletResponse response
    ) throws IOException {
        exportService.exportAdminTransactionsExcel(statuses, from, to, response);
    }

    /**
     * GET /export/admin/transactions/pdf
     * Admin xuất receipts PDF — theo orderIds hoặc theo date range
     */
    @GetMapping("/admin/transactions/pdf")
    @PreAuthorize("hasRole('ADMIN')")
    public void exportAdminTransactionsPdf(
            @RequestParam(required = false) List<Integer> orderIds,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate from,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate to,
            HttpServletResponse response
    ) throws IOException {
        exportService.exportAdminTransactionsPdf(orderIds, from, to, response);
    }

    /**
     * GET /export/shop/orders/excel
     * Shop owner xuất đơn hàng của shop mình ra Excel
     */
    @GetMapping("/shop/orders/excel")
    @PreAuthorize("hasRole('SHOP_OWNER')")
    public void exportShopOrdersExcel(
            @RequestParam(required = false) List<String> statuses,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate from,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate to,
            @AuthenticationPrincipal Jwt jwt,
            HttpServletResponse response
    ) throws IOException {
        exportService.exportShopOrdersExcel(jwt, statuses, from, to, response);
    }

    /**
     * GET /export/orders/{orderId}/receipt/pdf
     * Buyer / Shop owner / Admin tải hóa đơn PDF cho một đơn hàng cụ thể
     */
    @GetMapping("/orders/{orderId}/receipt/pdf")
    @PreAuthorize("hasAnyRole('ADMIN', 'SHOP_OWNER', 'BUYER')")
    public void exportOrderReceiptPdf(
            @PathVariable Integer orderId,
            @AuthenticationPrincipal Jwt jwt,
            HttpServletResponse response
    ) throws IOException {
        exportService.exportOrderReceiptPdf(jwt, orderId, response);
    }
}
