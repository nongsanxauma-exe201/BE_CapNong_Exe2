package com.swd301.foodmarket.service.impl;

import com.lowagie.text.*;
import com.lowagie.text.Font;
import com.lowagie.text.Rectangle;
import com.lowagie.text.pdf.*;
import com.swd301.foodmarket.dto.internal.ExportOrderRowDto;
import com.swd301.foodmarket.dto.internal.ExportSummaryDto;
import com.swd301.foodmarket.dto.response.AdminDeliveryReceiptIdsResponse;
import com.swd301.foodmarket.entity.Order;
import com.swd301.foodmarket.entity.OrderDetail;
import com.swd301.foodmarket.entity.User;
import com.swd301.foodmarket.enums.CodPaymentOption;
import com.swd301.foodmarket.enums.CodPaymentStatus;
import com.swd301.foodmarket.enums.OrderStatus;
import com.swd301.foodmarket.exception.AppException;
import com.swd301.foodmarket.exception.ErrorCode;
import com.swd301.foodmarket.repository.OrderRepository;
import com.swd301.foodmarket.repository.UserRepository;
import com.swd301.foodmarket.service.ExportService;
import com.swd301.foodmarket.service.support.MoneyRoundingUtil;
import com.swd301.foodmarket.service.support.OrderSettlementCalculator;
import jakarta.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.apache.poi.ss.usermodel.BorderStyle;
import org.apache.poi.ss.usermodel.Cell;
import org.apache.poi.ss.usermodel.CellStyle;
import org.apache.poi.ss.usermodel.FillPatternType;
import org.apache.poi.ss.usermodel.IndexedColors;
import org.apache.poi.ss.usermodel.Row;
import org.apache.poi.ss.usermodel.Sheet;
import org.apache.poi.ss.usermodel.Workbook;
import org.apache.poi.xssf.usermodel.XSSFWorkbook;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.stereotype.Service;

import java.awt.Color;
import java.io.IOException;
import java.io.InputStream;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
@Slf4j
public class ExportServiceImpl implements ExportService {

    private final OrderRepository orderRepository;
    private final UserRepository userRepository;
    private final OrderSettlementCalculator orderSettlementCalculator;

    private static final DateTimeFormatter DATE_FMT = DateTimeFormatter.ofPattern("dd/MM/yyyy HH:mm");
    /** Định dạng ngày trong tên file theo chuẩn Việt Nam (VD: 26-06-2024) */
    private static final DateTimeFormatter FILE_DATE_FMT = DateTimeFormatter.ofPattern("dd-MM-yyyy");

    /** Tạo chuỗi khoảng ngày cho tên file: "01-06-2024_den_30-06-2024" */
    private String fileDateRange(LocalDate from, LocalDate to) {
        String f = from != null ? from.format(FILE_DATE_FMT) : "ToanBo";
        String t = to != null ? to.format(FILE_DATE_FMT) : LocalDate.now().format(FILE_DATE_FMT);
        return f + "_den_" + t;
    }

    // ==============================
    // Helper: parse province từ địa chỉ
    // ==============================
    String parseProvince(String shippingAddress) {
        if (shippingAddress == null || shippingAddress.isBlank()) return "Không xác định";
        int lastComma = shippingAddress.lastIndexOf(',');
        if (lastComma < 0) return shippingAddress.trim();
        String province = shippingAddress.substring(lastComma + 1).trim();
        return province.isEmpty() ? "Không xác định" : province;
    }

    // ==============================
    // Helper: extract userId từ JWT
    // ==============================
    private Integer extractUserId(Jwt jwt) {
        Object raw = jwt.getClaim("userId");
        if (raw instanceof Integer i) return i;
        if (raw instanceof Long l) return l.intValue();
        if (raw instanceof Number n) return n.intValue();
        String email = jwt.getSubject();
        return userRepository.findByEmail(email)
                .map(User::getId)
                .orElseThrow(() -> new RuntimeException("User not found: " + email));
    }

    // ==============================
    // Helper: map Order → ExportOrderRowDto
    // ==============================
    ExportOrderRowDto mapToRow(Order order) {
        User buyer = order.getBuyer();
        User shop = order.getShopOwner();
        String itemsSummary = buildItemsSummary(order.getOrderDetails());

        return ExportOrderRowDto.builder()
                .orderId(order.getId())
                .createdAt(order.getCreatedAt())
                .buyerId(buyer != null ? buyer.getId() : null)
                .buyerFullName(buyer != null ? buyer.getFullName() : "")
                .buyerEmail(buyer != null ? buyer.getEmail() : "")
                .buyerPhone(order.getRecipientPhone() != null ? order.getRecipientPhone()
                        : (buyer != null ? buyer.getPhoneNumber() : ""))
                .shippingAddress(order.getShippingAddress())
                .province(parseProvince(order.getShippingAddress()))
                .shopName(shop != null
                        ? (shop.getShopName() != null ? shop.getShopName() : shop.getFullName())
                        : "")
                .paymentMethod(order.getPaymentMethod())
                .shippingFee(order.getShippingFee())
                .totalAmount(order.getTotalAmount())
                .status(order.getStatus() != null ? order.getStatus().name() : "")
                .itemsSummary(itemsSummary)
                .build();
    }

    private String buildItemsSummary(List<OrderDetail> details) {
        if (details == null || details.isEmpty()) return "";
        return details.stream()
                .map(d -> {
                    String name = d.getProduct() != null ? d.getProduct().getProductName() : "?";
                    return name + " x" + d.getQuantity();
                })
                .collect(Collectors.joining(", "));
    }

    private List<OrderStatus> resolveStatuses(List<String> rawStatuses, OrderStatus defaultStatus) {
        if (rawStatuses == null || rawStatuses.isEmpty()) {
            return List.of(defaultStatus);
        }
        return rawStatuses.stream()
                .map(s -> {
                    try { return OrderStatus.valueOf(s.toUpperCase()); }
                    catch (IllegalArgumentException e) { return null; }
                })
                .filter(Objects::nonNull)
                .collect(Collectors.toList());
    }

    private void validateDateRange(LocalDate from, LocalDate to) {
        if (from != null && to != null && from.isAfter(to)) {
            throw new IllegalArgumentException("from phải <= to");
        }
    }

    // ==============================
    // Task 4.3: exportAdminTransactionsExcel
    // ==============================
    @Override
    public void exportAdminTransactionsExcel(
            List<String> statuses, LocalDate from, LocalDate to,
            HttpServletResponse response) throws IOException {

        validateDateRange(from, to);
        List<OrderStatus> statusEnums = resolveStatuses(statuses, OrderStatus.DELIVERED);
        LocalDateTime dtFrom = from != null ? from.atStartOfDay() : null;
        LocalDateTime dtTo = to != null ? to.plusDays(1).atStartOfDay() : null;

        List<Order> orders = orderRepository.findForAdminExport(statusEnums, dtFrom, dtTo);
        List<ExportOrderRowDto> rows = orders.stream().map(this::mapToRow).toList();

        // BC_GiaoDich_01-06-2024_den_30-06-2024.xlsx
        String filename = "BC_GiaoDich_" + fileDateRange(from, to) + ".xlsx";
        response.setContentType("application/vnd.openxmlformats-officedocument.spreadsheetml.sheet");
        response.setHeader("Content-Disposition", "attachment; filename=\"" + filename + "\"");

        try (Workbook wb = buildExcelWorkbook(rows, from, to)) {
            wb.write(response.getOutputStream());
        }
    }

    Workbook buildExcelWorkbook(List<ExportOrderRowDto> rows, LocalDate from, LocalDate to) {
        Workbook wb = new XSSFWorkbook();

        // ---- Sheet 1: Transactions ----
        Sheet sheet = wb.createSheet("Transactions");
        String[] headers = {
                "Mã đơn", "Ngày tạo", "Mã người mua", "Tên người mua", "Email",
                "SĐT", "Địa chỉ giao", "Tỉnh/TP", "Tên shop", "PTTT",
                "Phí ship", "Tổng tiền", "Trạng thái", "Sản phẩm"
        };

        CellStyle headerStyle = wb.createCellStyle();
        org.apache.poi.ss.usermodel.Font hFont = wb.createFont();
        hFont.setBold(true);
        headerStyle.setFont(hFont);
        headerStyle.setFillForegroundColor(IndexedColors.LIGHT_BLUE.getIndex());
        headerStyle.setFillPattern(FillPatternType.SOLID_FOREGROUND);
        headerStyle.setBorderBottom(BorderStyle.THIN);

        Row headerRow = sheet.createRow(0);
        for (int i = 0; i < headers.length; i++) {
            Cell cell = headerRow.createCell(i);
            cell.setCellValue(headers[i]);
            cell.setCellStyle(headerStyle);
        }

        int rowIdx = 1;
        for (ExportOrderRowDto r : rows) {
            Row row = sheet.createRow(rowIdx++);
            row.createCell(0).setCellValue(r.getOrderId() != null ? r.getOrderId() : 0);
            row.createCell(1).setCellValue(r.getCreatedAt() != null ? r.getCreatedAt().format(DATE_FMT) : "");
            row.createCell(2).setCellValue(r.getBuyerId() != null ? r.getBuyerId() : 0);
            row.createCell(3).setCellValue(safeStr(r.getBuyerFullName()));
            row.createCell(4).setCellValue(safeStr(r.getBuyerEmail()));
            row.createCell(5).setCellValue(safeStr(r.getBuyerPhone()));
            row.createCell(6).setCellValue(safeStr(r.getShippingAddress()));
            row.createCell(7).setCellValue(safeStr(r.getProvince()));
            row.createCell(8).setCellValue(safeStr(r.getShopName()));
            row.createCell(9).setCellValue(safeStr(r.getPaymentMethod()));
            row.createCell(10).setCellValue(r.getShippingFee() != null ? r.getShippingFee().doubleValue() : 0);
            row.createCell(11).setCellValue(r.getTotalAmount() != null ? r.getTotalAmount().doubleValue() : 0);
            row.createCell(12).setCellValue(safeStr(r.getStatus()));
            row.createCell(13).setCellValue(safeStr(r.getItemsSummary()));
        }

        for (int i = 0; i < headers.length; i++) sheet.autoSizeColumn(i);

        // ---- Sheet 2: Summary ----
        Sheet summary = wb.createSheet("Summary");
        ExportSummaryDto sumDto = buildSummary(rows, from, to);

        int sr = 0;
        addSummaryRow(summary, sr++, "Tổng đơn hàng", String.valueOf(sumDto.getTotalOrders()));
        addSummaryRow(summary, sr++, "Số người mua (distinct)", String.valueOf(sumDto.getDistinctBuyers()));
        addSummaryRow(summary, sr++, "Tổng doanh thu", formatMoney(sumDto.getTotalRevenue()));
        addSummaryRow(summary, sr++, "Tổng phí vận chuyển", formatMoney(sumDto.getTotalShippingFee()));
        addSummaryRow(summary, sr++, "Từ ngày", from != null ? from.toString() : "—");
        addSummaryRow(summary, sr++, "Đến ngày", to != null ? to.toString() : "—");

        sr++; // blank
        Row ph = summary.createRow(sr++);
        ph.createCell(0).setCellValue("Tỉnh/TP");
        ph.createCell(1).setCellValue("Số đơn");

        for (Map.Entry<String, Long> e : sumDto.getProvinceBreakdown().entrySet()) {
            Row pr = summary.createRow(sr++);
            pr.createCell(0).setCellValue(e.getKey());
            pr.createCell(1).setCellValue(e.getValue());
        }

        summary.autoSizeColumn(0);
        summary.autoSizeColumn(1);
        return wb;
    }

    private ExportSummaryDto buildSummary(List<ExportOrderRowDto> rows, LocalDate from, LocalDate to) {
        long totalOrders = rows.size();
        long distinctBuyers = rows.stream()
                .map(ExportOrderRowDto::getBuyerId).filter(Objects::nonNull).distinct().count();
        BigDecimal totalRevenue = rows.stream()
                .map(r -> r.getTotalAmount() != null ? r.getTotalAmount() : BigDecimal.ZERO)
                .reduce(BigDecimal.ZERO, BigDecimal::add);
        BigDecimal totalShipping = rows.stream()
                .map(r -> r.getShippingFee() != null ? r.getShippingFee() : BigDecimal.ZERO)
                .reduce(BigDecimal.ZERO, BigDecimal::add);
        Map<String, Long> provinceBreakdown = rows.stream()
                .collect(Collectors.groupingBy(
                        r -> r.getProvince() != null ? r.getProvince() : "Không xác định",
                        Collectors.counting()));

        return ExportSummaryDto.builder()
                .totalOrders(totalOrders)
                .distinctBuyers(distinctBuyers)
                .totalRevenue(totalRevenue)
                .totalShippingFee(totalShipping)
                .periodFrom(from)
                .periodTo(to)
                .provinceBreakdown(provinceBreakdown)
                .build();
    }

    private void addSummaryRow(Sheet sheet, int rowIdx, String label, String value) {
        Row row = sheet.createRow(rowIdx);
        row.createCell(0).setCellValue(label);
        row.createCell(1).setCellValue(value);
    }

    // ==============================
    // Task 4.4: exportAdminTransactionsPdf
    // ==============================
    @Override
    public void exportAdminTransactionsPdf(
            List<Integer> orderIds, LocalDate from, LocalDate to,
            HttpServletResponse response) throws IOException {

        validateDateRange(from, to);
        List<Order> orders;
        if (orderIds != null && !orderIds.isEmpty()) {
            orders = orderRepository.findByIdInWithDetails(orderIds);
        } else {
            LocalDateTime dtFrom = from != null ? from.atStartOfDay() : null;
            LocalDateTime dtTo = to != null ? to.plusDays(1).atStartOfDay() : null;
            orders = orderRepository.findForAdminExport(List.of(OrderStatus.DELIVERED), dtFrom, dtTo);
        }

        // HD_GiaoHang_01-06-2024_den_30-06-2024.pdf  (nếu theo date range)
        // HD_GiaoHang_DH1_DH2_DH3_26-06-2024.pdf     (nếu theo orderIds cụ thể)
        String filename;
        if (orderIds != null && !orderIds.isEmpty()) {
            String idList = orderIds.stream().map(id -> "DH" + id).collect(Collectors.joining("_"));
            filename = "HD_GiaoHang_" + idList + "_" + LocalDate.now().format(FILE_DATE_FMT) + ".pdf";
        } else {
            filename = "HD_GiaoHang_" + fileDateRange(from, to) + ".pdf";
        }
        response.setContentType("application/pdf");
        response.setHeader("Content-Disposition", "attachment; filename=\"" + filename + "\"");

        buildPdfDocument(orders, response);
    }

    void buildPdfDocument(List<Order> orders, HttpServletResponse response) throws IOException {
        Document doc = new Document(PageSize.A4, 40, 40, 50, 50);
        PdfWriter writer = PdfWriter.getInstance(doc, response.getOutputStream());
        doc.open();

        BaseFont baseFont = loadBaseFont();

        if (orders.isEmpty()) {
            Font msgFont = new Font(baseFont, 14, Font.NORMAL, Color.GRAY);
            doc.add(new Paragraph("Không có đơn hàng nào phù hợp với điều kiện tìm kiếm.", msgFont));
            doc.close();
            return;
        }

        boolean first = true;
        for (Order order : orders) {
            if (!first) doc.newPage();
            first = false;
            addReceiptPage(doc, order, baseFont, writer);
        }

        doc.close();
    }

    private void addReceiptPage(Document doc, Order order, BaseFont baseFont, PdfWriter writer) throws DocumentException {
        Font titleFont = new Font(baseFont, 18, Font.BOLD, Color.BLACK);
        Font labelFont = new Font(baseFont, 10, Font.BOLD, new Color(80, 80, 80));
        Font valueFont = new Font(baseFont, 10, Font.NORMAL, Color.BLACK);
        Font smallFont = new Font(baseFont, 9, Font.NORMAL, new Color(120, 120, 120));

        Paragraph header = new Paragraph("HÓA ĐƠN GIAO HÀNG", titleFont);
        header.setAlignment(Element.ALIGN_CENTER);
        header.setSpacingAfter(4);
        doc.add(header);

        Paragraph subHeader = new Paragraph("CapNong - Nền tảng nông sản sạch",
                new Font(baseFont, 11, Font.NORMAL, new Color(100, 140, 60)));
        subHeader.setAlignment(Element.ALIGN_CENTER);
        subHeader.setSpacingAfter(16);
        doc.add(subHeader);

        addSeparator(doc, writer);

        addInfoLine(doc, "Mã đơn hàng:", "#" + order.getId(), labelFont, valueFont);
        addInfoLine(doc, "Ngày tạo:",
                order.getCreatedAt() != null ? order.getCreatedAt().format(DATE_FMT) : "—",
                labelFont, valueFont);

        addSeparator(doc, writer);

        User buyer = order.getBuyer();
        addInfoLine(doc, "Mã người mua:", buyer != null ? "#" + buyer.getId() : "—", labelFont, valueFont);
        addInfoLine(doc, "Tên người mua:", buyer != null ? safeStr(buyer.getFullName()) : "—", labelFont, valueFont);
        addInfoLine(doc, "Email:", buyer != null ? safeStr(buyer.getEmail()) : "—", labelFont, valueFont);
        addInfoLine(doc, "Số điện thoại:", safeStr(order.getRecipientPhone()), labelFont, valueFont);
        addInfoLine(doc, "Địa chỉ giao:", safeStr(order.getShippingAddress()), labelFont, valueFont);

        addSeparator(doc, writer);

        User shop = order.getShopOwner();
        String shopDisplayName = shop != null
                ? (shop.getShopName() != null ? shop.getShopName() : shop.getFullName())
                : "—";
        addInfoLine(doc, "Cửa hàng:", shopDisplayName, labelFont, valueFont);
        addInfoLine(doc, "Phương thức thanh toán:", safeStr(order.getPaymentMethod()), labelFont, valueFont);

        addSeparator(doc, writer);

        PdfPTable table = new PdfPTable(new float[]{4f, 1.5f, 2f, 2f});
        table.setWidthPercentage(100);
        table.setSpacingBefore(6);
        addTableHeader(table, baseFont, "Sản phẩm", "SL", "Đơn giá", "Thành tiền");

        List<OrderDetail> details = order.getOrderDetails();
        if (details != null) {
            for (OrderDetail detail : details) {
                String productName = detail.getProduct() != null
                        ? detail.getProduct().getProductName() : "?";
                BigDecimal unit = detail.getUnitPrice() != null ? detail.getUnitPrice() : BigDecimal.ZERO;
                int qty = detail.getQuantity() != null ? detail.getQuantity() : 0;
                BigDecimal lineTotal = unit.multiply(BigDecimal.valueOf(qty));
                addTableCell(table, productName, valueFont);
                addTableCell(table, String.valueOf(qty), valueFont);
                addTableCell(table, formatMoney(unit), valueFont);
                addTableCell(table, formatMoney(lineTotal), valueFont);
            }
        }
        doc.add(table);

        addSeparator(doc, writer);

        addInfoLine(doc, "Phí vận chuyển:", formatMoney(order.getShippingFee()), labelFont, valueFont);
        Font totalFont = new Font(baseFont, 12, Font.BOLD, new Color(30, 100, 30));
        addInfoLine(doc, "TỔNG CỘNG:", formatMoney(order.getTotalAmount()),
                new Font(baseFont, 12, Font.BOLD, new Color(80, 80, 80)), totalFont);

        if (order.getStatus() == OrderStatus.DELIVERED) {
            Paragraph stamp = new Paragraph("✓ ĐÃ GIAO HÀNG THÀNH CÔNG",
                    new Font(baseFont, 14, Font.BOLD, new Color(34, 139, 34)));
            stamp.setAlignment(Element.ALIGN_CENTER);
            stamp.setSpacingBefore(20);
            doc.add(stamp);
        }

        Paragraph footer = new Paragraph("Cảm ơn bạn đã mua hàng tại CapNong!", smallFont);
        footer.setAlignment(Element.ALIGN_CENTER);
        footer.setSpacingBefore(16);
        doc.add(footer);
    }

    private void addSeparator(Document doc, PdfWriter writer) throws DocumentException {
        Paragraph sep = new Paragraph(" ");
        sep.setSpacingBefore(4);
        sep.setSpacingAfter(4);
        doc.add(sep);
        PdfContentByte canvas = writer.getDirectContent();
        canvas.setLineWidth(0.5f);
        canvas.setColorStroke(new Color(200, 200, 200));
        float y = writer.getVerticalPosition(true);
        canvas.moveTo(doc.leftMargin(), y);
        canvas.lineTo(doc.right() - doc.rightMargin(), y);
        canvas.stroke();
    }

    private void addInfoLine(Document doc, String label, String value,
                             Font labelFont, Font valueFont) throws DocumentException {
        PdfPTable t = new PdfPTable(new float[]{2.5f, 4f});
        t.setWidthPercentage(100);
        t.setSpacingBefore(2);
        PdfPCell lCell = new PdfPCell(new Phrase(label, labelFont));
        lCell.setBorder(Rectangle.NO_BORDER);
        lCell.setPaddingBottom(3);
        PdfPCell vCell = new PdfPCell(new Phrase(value, valueFont));
        vCell.setBorder(Rectangle.NO_BORDER);
        vCell.setPaddingBottom(3);
        t.addCell(lCell);
        t.addCell(vCell);
        doc.add(t);
    }

    private void addTableHeader(PdfPTable table, BaseFont baseFont, String... headers) {
        Font hFont = new Font(baseFont, 9, Font.BOLD, Color.WHITE);
        for (String h : headers) {
            PdfPCell cell = new PdfPCell(new Phrase(h, hFont));
            cell.setBackgroundColor(new Color(70, 130, 70));
            cell.setPadding(5);
            cell.setHorizontalAlignment(Element.ALIGN_CENTER);
            table.addCell(cell);
        }
    }

    private void addTableCell(PdfPTable table, String value, Font font) {
        PdfPCell cell = new PdfPCell(new Phrase(value, font));
        cell.setPadding(4);
        cell.setBorderColor(new Color(220, 220, 220));
        table.addCell(cell);
    }

    // ==============================
    // Task 4.5: exportShopOrdersExcel
    // ==============================
    @Override
    public void exportShopOrdersExcel(
            Jwt jwt, List<String> statuses, LocalDate from, LocalDate to,
            HttpServletResponse response) throws IOException {

        validateDateRange(from, to);
        Integer userId = extractUserId(jwt);
        User shopOwner = userRepository.findById(userId)
                .orElseThrow(() -> new RuntimeException("User not found: " + userId));

        List<OrderStatus> statusEnums = resolveStatuses(statuses, OrderStatus.DELIVERED);
        LocalDateTime dtFrom = from != null ? from.atStartOfDay() : null;
        LocalDateTime dtTo = to != null ? to.plusDays(1).atStartOfDay() : null;

        List<Order> orders = orderRepository.findForShopExport(shopOwner, dtFrom, dtTo, statusEnums);

        String rawName = shopOwner.getShopName() != null ? shopOwner.getShopName() : shopOwner.getFullName();
        String safeName = rawName != null ? rawName.replaceAll("[^a-zA-Z0-9]", "_") : "Shop";
        // DS_DonHang_{TenShop}_01-06-2024_den_30-06-2024.xlsx
        String filename = "DS_DonHang_" + safeName + "_" + fileDateRange(from, to) + ".xlsx";

        response.setContentType("application/vnd.openxmlformats-officedocument.spreadsheetml.sheet");
        response.setHeader("Content-Disposition", "attachment; filename=\"" + filename + "\"");

        try (Workbook wb = buildShopExcelWorkbook(orders, shopOwner, from, to)) {
            wb.write(response.getOutputStream());
        }
    }

    Workbook buildShopExcelWorkbook(List<Order> orders, User shopOwner, LocalDate from, LocalDate to) {
        Workbook wb = new XSSFWorkbook();

        Sheet sheet = wb.createSheet("Orders");
        String[] headers = {
                "Mã đơn", "Ngày tạo", "Tên người mua", "SĐT", "Địa chỉ giao",
                "PTTT", "Phí ship", "Tổng tiền", "Doanh thu thuần", "Trạng thái"
        };

        CellStyle headerStyle = wb.createCellStyle();
        org.apache.poi.ss.usermodel.Font hFont = wb.createFont();
        hFont.setBold(true);
        headerStyle.setFont(hFont);
        headerStyle.setFillForegroundColor(IndexedColors.LIGHT_GREEN.getIndex());
        headerStyle.setFillPattern(FillPatternType.SOLID_FOREGROUND);
        headerStyle.setBorderBottom(BorderStyle.THIN);

        Row headerRow = sheet.createRow(0);
        for (int i = 0; i < headers.length; i++) {
            Cell cell = headerRow.createCell(i);
            cell.setCellValue(headers[i]);
            cell.setCellStyle(headerStyle);
        }

        int rowIdx = 1;
        for (Order order : orders) {
            Row row = sheet.createRow(rowIdx++);
            row.createCell(0).setCellValue(order.getId() != null ? order.getId() : 0);
            row.createCell(1).setCellValue(
                    order.getCreatedAt() != null ? order.getCreatedAt().format(DATE_FMT) : "");
            User buyer = order.getBuyer();
            row.createCell(2).setCellValue(buyer != null ? safeStr(buyer.getFullName()) : "");
            row.createCell(3).setCellValue(safeStr(order.getRecipientPhone()));
            row.createCell(4).setCellValue(safeStr(order.getShippingAddress()));
            row.createCell(5).setCellValue(safeStr(order.getPaymentMethod()));
            BigDecimal fee = order.getShippingFee() != null ? order.getShippingFee() : BigDecimal.ZERO;
            BigDecimal total = order.getTotalAmount() != null ? order.getTotalAmount() : BigDecimal.ZERO;
            row.createCell(6).setCellValue(fee.doubleValue());
            row.createCell(7).setCellValue(total.doubleValue());
            row.createCell(8).setCellValue(total.subtract(fee).doubleValue());
            row.createCell(9).setCellValue(order.getStatus() != null ? order.getStatus().name() : "");
        }

        for (int i = 0; i < headers.length; i++) sheet.autoSizeColumn(i);

        Sheet summary = wb.createSheet("Summary");
        long totalOrders = orders.size();
        BigDecimal totalRevenue = orders.stream()
                .map(o -> {
                    BigDecimal t = o.getTotalAmount() != null ? o.getTotalAmount() : BigDecimal.ZERO;
                    BigDecimal s = o.getShippingFee() != null ? o.getShippingFee() : BigDecimal.ZERO;
                    return t.subtract(s);
                })
                .reduce(BigDecimal.ZERO, BigDecimal::add);

        int sr = 0;
        addSummaryRow(summary, sr++, "Cửa hàng",
                shopOwner.getShopName() != null ? shopOwner.getShopName() : shopOwner.getFullName());
        addSummaryRow(summary, sr++, "Tổng đơn hàng", String.valueOf(totalOrders));
        addSummaryRow(summary, sr++, "Tổng doanh thu thuần", formatMoney(totalRevenue));
        addSummaryRow(summary, sr++, "Từ ngày", from != null ? from.toString() : "—");
        addSummaryRow(summary, sr++, "Đến ngày", to != null ? to.toString() : "—");

        summary.autoSizeColumn(0);
        summary.autoSizeColumn(1);
        return wb;
    }

    // ==============================
    // Task 4.6: exportOrderReceiptPdf
    // ==============================
    @Override
    public void exportOrderReceiptPdf(Jwt jwt, Integer orderId, HttpServletResponse response) throws IOException {
        Order order = orderRepository.findByIdWithDetails(orderId)
                .orElseThrow(() -> new RuntimeException("Order not found: " + orderId));

        String callerRole = jwt.getClaimAsString("scope");
        Integer callerId = extractUserId(jwt);

        if (!"ADMIN".equals(callerRole)) {
            if ("SHOP_OWNER".equals(callerRole)) {
                User shopOwner = order.getShopOwner();
                if (shopOwner == null || !shopOwner.getId().equals(callerId)) {
                    throw new AccessDeniedException("Bạn không có quyền xem hóa đơn này");
                }
            } else if ("BUYER".equals(callerRole)) {
                User buyer = order.getBuyer();
                if (buyer == null || !buyer.getId().equals(callerId)) {
                    throw new AccessDeniedException("Bạn không có quyền xem hóa đơn này");
                }
            } else {
                throw new AccessDeniedException("Bạn không có quyền xem hóa đơn này");
            }
        }

        // HD_DonHang_DH{id}_{ngay-dat-don}.pdf
        String orderDate = order.getCreatedAt() != null
                ? order.getCreatedAt().toLocalDate().format(FILE_DATE_FMT)
                : LocalDate.now().format(FILE_DATE_FMT);
        String filename = "HD_DonHang_DH" + orderId + "_" + orderDate + ".pdf";
        response.setContentType("application/pdf");
        response.setHeader("Content-Disposition", "attachment; filename=\"" + filename + "\"");

        buildSingleReceiptPdf(order, response);
    }

    void buildSingleReceiptPdf(Order order, HttpServletResponse response) throws IOException {
        Document doc = new Document(PageSize.A4, 40, 40, 50, 50);
        PdfWriter writer = PdfWriter.getInstance(doc, response.getOutputStream());
        doc.open();
        BaseFont baseFont = loadBaseFont();
        addReceiptPage(doc, order, baseFont, writer);
        doc.close();
    }

    // ==============================
    // Admin delivery receipts (per-file) + COD details
    // ==============================
    @Override
    public AdminDeliveryReceiptIdsResponse listAdminDeliveryReceiptIds(LocalDate from, LocalDate to) {
        validateDateRange(from, to);
        LocalDateTime dtFrom = from != null ? from.atStartOfDay() : null;
        LocalDateTime dtTo = to != null ? to.plusDays(1).atStartOfDay() : null;
        List<Integer> ids = orderRepository.findAdminDeliveryReceiptOrderIds(
                OrderStatus.DELIVERED, dtFrom, dtTo);
        return AdminDeliveryReceiptIdsResponse.builder().orderIds(ids).build();
    }

    @Override
    public void exportAdminDeliveryReceiptPdf(Integer orderId, HttpServletResponse response) throws IOException {
        Order order = orderRepository.findByIdWithAdminDeliveryReceiptDetails(orderId)
                .orElseThrow(() -> new AppException(ErrorCode.ORDER_NOT_FOUND));

        if (order.getStatus() != OrderStatus.DELIVERED) {
            throw new AppException(ErrorCode.INVALID_ORDER_STATUS);
        }

        String orderDate = order.getCreatedAt() != null
                ? order.getCreatedAt().toLocalDate().format(FILE_DATE_FMT)
                : LocalDate.now().format(FILE_DATE_FMT);
        String filename = "HD_GiaoHang_DH" + orderId + "_" + orderDate + ".pdf";
        response.setContentType("application/pdf");
        response.setHeader("Content-Disposition", "attachment; filename=\"" + filename + "\"");

        Document doc = new Document(PageSize.A4, 40, 40, 50, 50);
        PdfWriter writer = PdfWriter.getInstance(doc, response.getOutputStream());
        doc.open();
        BaseFont baseFont = loadBaseFont();
        addAdminDeliveryReceiptPage(doc, order, baseFont, writer);
        doc.close();
    }

    private void addAdminDeliveryReceiptPage(
            Document doc, Order order, BaseFont baseFont, PdfWriter writer
    ) throws DocumentException {
        Font titleFont = new Font(baseFont, 18, Font.BOLD, Color.BLACK);
        Font labelFont = new Font(baseFont, 10, Font.BOLD, new Color(80, 80, 80));
        Font valueFont = new Font(baseFont, 10, Font.NORMAL, Color.BLACK);
        Font smallFont = new Font(baseFont, 9, Font.NORMAL, new Color(120, 120, 120));

        Paragraph header = new Paragraph("HÓA ĐƠN GIAO HÀNG", titleFont);
        header.setAlignment(Element.ALIGN_CENTER);
        header.setSpacingAfter(4);
        doc.add(header);

        Paragraph subHeader = new Paragraph("CapNong - Nền tảng nông sản sạch",
                new Font(baseFont, 11, Font.NORMAL, new Color(100, 140, 60)));
        subHeader.setAlignment(Element.ALIGN_CENTER);
        subHeader.setSpacingAfter(16);
        doc.add(subHeader);

        addSeparator(doc, writer);

        addInfoLine(doc, "Mã đơn hàng:", "#" + order.getId(), labelFont, valueFont);
        addInfoLine(doc, "Ngày tạo:",
                order.getCreatedAt() != null ? order.getCreatedAt().format(DATE_FMT) : "—",
                labelFont, valueFont);

        addSeparator(doc, writer);

        User buyer = order.getBuyer();
        addInfoLine(doc, "Mã người mua:", buyer != null ? "#" + buyer.getId() : "—", labelFont, valueFont);
        addInfoLine(doc, "Tên người mua:", buyer != null ? safeStr(buyer.getFullName()) : "—", labelFont, valueFont);
        addInfoLine(doc, "Email:", buyer != null ? safeStr(buyer.getEmail()) : "—", labelFont, valueFont);
        addInfoLine(doc, "Số điện thoại:", safeStr(order.getRecipientPhone()), labelFont, valueFont);
        addInfoLine(doc, "Địa chỉ giao:", safeStr(order.getShippingAddress()), labelFont, valueFont);

        addSeparator(doc, writer);

        User shop = order.getShopOwner();
        String shopDisplayName = shop != null
                ? (shop.getShopName() != null ? shop.getShopName() : shop.getFullName())
                : "—";
        addInfoLine(doc, "Cửa hàng:", shopDisplayName, labelFont, valueFont);
        addInfoLine(doc, "Phương thức thanh toán:", safeStr(order.getPaymentMethod()), labelFont, valueFont);

        if ("COD".equalsIgnoreCase(order.getPaymentMethod())) {
            addSeparator(doc, writer);
            addCodAdminSettlementSection(doc, order, labelFont, valueFont);
        }

        addSeparator(doc, writer);

        PdfPTable table = new PdfPTable(new float[]{4f, 1.5f, 2f, 2f});
        table.setWidthPercentage(100);
        table.setSpacingBefore(6);
        addTableHeader(table, baseFont, "Sản phẩm", "SL", "Đơn giá", "Thành tiền");

        List<OrderDetail> details = order.getOrderDetails();
        if (details != null) {
            for (OrderDetail detail : details) {
                String productName = detail.getProduct() != null
                        ? detail.getProduct().getProductName() : "?";
                BigDecimal unit = detail.getUnitPrice() != null ? detail.getUnitPrice() : BigDecimal.ZERO;
                int qty = detail.getQuantity() != null ? detail.getQuantity() : 0;
                BigDecimal lineTotal = unit.multiply(BigDecimal.valueOf(qty));
                addTableCell(table, productName, valueFont);
                addTableCell(table, String.valueOf(qty), valueFont);
                addTableCell(table, formatMoney(unit), valueFont);
                addTableCell(table, formatMoney(lineTotal), valueFont);
            }
        }
        doc.add(table);

        addSeparator(doc, writer);

        addInfoLine(doc, "Phí vận chuyển:", formatMoney(order.getShippingFee()), labelFont, valueFont);
        Font totalFont = new Font(baseFont, 12, Font.BOLD, new Color(30, 100, 30));
        addInfoLine(doc, "TỔNG CỘNG:", formatMoney(order.getTotalAmount()),
                new Font(baseFont, 12, Font.BOLD, new Color(80, 80, 80)), totalFont);

        if (order.getStatus() == OrderStatus.DELIVERED) {
            Paragraph stamp = new Paragraph("✓ ĐÃ GIAO HÀNG THÀNH CÔNG",
                    new Font(baseFont, 14, Font.BOLD, new Color(34, 139, 34)));
            stamp.setAlignment(Element.ALIGN_CENTER);
            stamp.setSpacingBefore(20);
            doc.add(stamp);
        }

        Paragraph footer = new Paragraph("Cảm ơn bạn đã mua hàng tại CapNong!", smallFont);
        footer.setAlignment(Element.ALIGN_CENTER);
        footer.setSpacingBefore(16);
        doc.add(footer);
    }

    private void addCodAdminSettlementSection(
            Document doc, Order order, Font labelFont, Font valueFont
    ) throws DocumentException {
        OrderSettlementCalculator.SettlementAmounts amounts = orderSettlementCalculator.calculate(order);
        BigDecimal refund = order.getRefundAmount() != null ? order.getRefundAmount() : BigDecimal.ZERO;
        BigDecimal shipping = amounts.shippingFee();
        BigDecimal shopChargeable = amounts.shopGross().subtract(refund);
        if (shopChargeable.compareTo(BigDecimal.ZERO) < 0) {
            shopChargeable = BigDecimal.ZERO;
        }
        BigDecimal platformFeeFromShip = MoneyRoundingUtil.commission(shipping);
        BigDecimal prepaidAmount = order.getCodPrepaidAmount() != null
                ? order.getCodPrepaidAmount()
                : shopChargeable.add(platformFeeFromShip);

        User shipper = order.getShipper();
        String shipperName = shipper != null ? safeStr(shipper.getFullName()) : "—";
        if (shipperName.isBlank()) {
            shipperName = "—";
        }

        addInfoLine(doc, "Shipper:", shipperName, labelFont, valueFont);
        addInfoLine(doc, "Phương thức ứng COD:",
                formatCodPaymentOption(order.getCodPaymentOption()), labelFont, valueFont);
        addInfoLine(doc, "Trạng thái COD:",
                formatCodPaymentStatus(order.getCodPaymentStatus()), labelFont, valueFont);
        addInfoLine(doc, "Tổng chuyển/ứng admin:", formatMoney(prepaidAmount), labelFont, valueFont);
        addInfoLine(doc, "Tiền hàng (ứng):", formatMoney(shopChargeable), labelFont, valueFont);
        addInfoLine(doc, "Phí ship:", formatMoney(shipping), labelFont, valueFont);
        addInfoLine(doc, "Hoa hồng platform từ ship:", formatMoney(platformFeeFromShip), labelFont, valueFont);
        if (order.getShipperPaidAt() != null) {
            addInfoLine(doc, "Thời điểm shipper thanh toán:",
                    order.getShipperPaidAt().format(DATE_FMT), labelFont, valueFont);
        }
    }

    private String formatCodPaymentOption(CodPaymentOption option) {
        if (option == null) return "—";
        return switch (option) {
            case WALLET -> "Trừ ví shipper";
            case CASH -> "Tiền mặt (admin xác nhận)";
        };
    }

    private String formatCodPaymentStatus(CodPaymentStatus status) {
        if (status == null) return "—";
        return switch (status) {
            case NOT_REQUIRED -> "Không yêu cầu";
            case PENDING_ADMIN_CONFIRM -> "Chờ admin xác nhận";
            case PAID -> "Đã thanh toán";
            case REFUND_PENDING -> "Chờ hoàn ứng";
            case REFUNDED -> "Đã hoàn ứng";
        };
    }

    // ==============================
    // Font loading helper
    // ==============================
    private BaseFont loadBaseFont() {
        try {
            InputStream fontStream = getClass().getResourceAsStream("/fonts/NotoSans-Regular.ttf");
            if (fontStream != null) {
                byte[] fontBytes = fontStream.readAllBytes();
                return BaseFont.createFont("NotoSans-Regular.ttf", BaseFont.IDENTITY_H,
                        BaseFont.EMBEDDED, true, fontBytes, null);
            }
        } catch (Exception e) {
            log.warn("NotoSans font not found, falling back to Helvetica: {}", e.getMessage());
        }
        try {
            return BaseFont.createFont(BaseFont.HELVETICA, BaseFont.CP1252, BaseFont.NOT_EMBEDDED);
        } catch (Exception e) {
            throw new RuntimeException("Cannot load any font", e);
        }
    }

    private String safeStr(String s) {
        return s != null ? s : "";
    }

    private String formatMoney(BigDecimal amount) {
        if (amount == null) return "0 đ";
        return String.format("%,.0f đ", amount);
    }
}
