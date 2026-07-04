package com.swd301.foodmarket.ai.util;

import com.swd301.foodmarket.entity.Product;
import lombok.Builder;
import lombok.Getter;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.text.Normalizer;
import java.util.Locale;
import java.util.regex.Pattern;

public final class AiQuantityConverter {

    private AiQuantityConverter() {
    }

    @Getter
    @Builder
    public static class ConversionResult {
        private final boolean valid;
        private final int cartQuantity;
        private final double displayQuantity;
        private final String displayUnit;
        private final String errorMessage;
        private final String note;

        public static ConversionResult success(
                int cartQuantity,
                double displayQuantity,
                String displayUnit
        ) {
            return ConversionResult.builder()
                    .valid(true)
                    .cartQuantity(cartQuantity)
                    .displayQuantity(displayQuantity)
                    .displayUnit(displayUnit)
                    .build();
        }

        public static ConversionResult successWithNote(
                int cartQuantity,
                double displayQuantity,
                String displayUnit,
                String note
        ) {
            return ConversionResult.builder()
                    .valid(true)
                    .cartQuantity(cartQuantity)
                    .displayQuantity(displayQuantity)
                    .displayUnit(displayUnit)
                    .note(note)
                    .build();
        }

        public static ConversionResult failure(String errorMessage) {
            return ConversionResult.builder()
                    .valid(false)
                    .errorMessage(errorMessage)
                    .build();
        }
    }

    public static ConversionResult resolveForProduct(
            Product product,
            Double requestedQuantity,
            String requestedUnit
    ) {
        if (product == null) {
            return ConversionResult.failure("Không tìm thấy sản phẩm.");
        }

        double rawQuantity = requestedQuantity == null || requestedQuantity <= 0
                ? 1.0
                : requestedQuantity;

        String userUnit = normalizeUnit(requestedUnit);
        if (userUnit == null || userUnit.isBlank()) {
            userUnit = "kg";
        }

        if (!"kg".equals(userUnit)) {
            return ConversionResult.failure(
                    "Hệ thống chỉ hỗ trợ đặt hàng theo kg. Vui lòng nhập khối lượng bằng kg, ví dụ 0.5 kg hoặc 2 kg."
            );
        }

        BigDecimal amountInProductUnit = BigDecimal.valueOf(rawQuantity).setScale(2, RoundingMode.HALF_UP);

        if (amountInProductUnit.compareTo(BigDecimal.ZERO) <= 0) {
            return ConversionResult.failure("Khối lượng không hợp lệ.");
        }

        int cartQuantity = Math.max(1, amountInProductUnit.setScale(0, RoundingMode.CEILING).intValue());
        BigDecimal stockKg = product.getStockKg() != null
                ? product.getStockKg()
                : BigDecimal.valueOf(product.getStockQuantity() == null ? 0 : product.getStockQuantity());

        if (stockKg.compareTo(amountInProductUnit) < 0) {
            return ConversionResult.failure(
                    "Sản phẩm \"" + product.getProductName() + "\" chỉ còn "
                            + stockKg.stripTrailingZeros().toPlainString() + " kg."
            );
        }

        return ConversionResult.successWithNote(
                cartQuantity,
                amountInProductUnit.doubleValue(),
                "kg",
                "Đơn vị đặt hàng đã được chuẩn hóa theo kg."
        );
    }

    public static ConversionResult resolveForMysteryBox(Integer requestedQuantity) {
        int qty = requestedQuantity == null || requestedQuantity <= 0 ? 1 : requestedQuantity;
        return ConversionResult.success(qty, qty, "kg");
    }

    public static String normalizeUnit(String unit) {
        if (unit == null) {
            return "";
        }

        String normalized = normalizeText(unit);

        return switch (normalized) {
            case "ki", "ky", "kg", "kilogram", "kilogam" -> "kg";
            default -> normalized;
        };
    }

    private static String normalizeText(String text) {
        String temp = Normalizer.normalize(text, Normalizer.Form.NFD);
        return Pattern.compile("\\p{InCombiningDiacriticalMarks}+")
                .matcher(temp)
                .replaceAll("")
                .toLowerCase(Locale.ROOT)
                .replace("đ", "d")
                .replaceAll("[^a-z0-9\\s]", " ")
                .replaceAll("\\s+", " ")
                .trim();
    }

}
