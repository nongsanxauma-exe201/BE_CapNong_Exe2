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

        String productUnit = normalizeUnit(product.getUnit());
        if (productUnit == null || productUnit.isBlank()) {
            productUnit = "cai";
        }

        String userUnit = normalizeUnit(requestedUnit);
        if (userUnit == null || userUnit.isBlank()) {
            userUnit = productUnit;
        }

        UnitType productType = detectUnitType(productUnit);
        UnitType userType = detectUnitType(userUnit);

        BigDecimal amountInProductUnit;

        if (productType == UnitType.WEIGHT && userType == UnitType.WEIGHT) {
            BigDecimal grams = toGrams(rawQuantity, userUnit);
            amountInProductUnit = fromGrams(grams, productUnit);
        } else if (productType == UnitType.COUNT && userType == UnitType.COUNT) {
            if (!isSameCountUnit(userUnit, productUnit)) {
                return ConversionResult.failure(
                        "Sản phẩm \"" + product.getProductName() + "\" bán theo "
                                + product.getUnit() + ", không khớp với đơn vị \""
                                + formatUnitLabel(requestedUnit) + "\" bạn yêu cầu."
                );
            }
            amountInProductUnit = BigDecimal.valueOf(rawQuantity);
        } else if (productType == userType) {
            amountInProductUnit = BigDecimal.valueOf(rawQuantity);
        } else {
            return ConversionResult.failure(
                    "Sản phẩm \"" + product.getProductName() + "\" bán theo "
                            + product.getUnit() + ". Bạn vui lòng đặt theo đúng đơn vị này "
                            + "(ví dụ: 1" + product.getUnit() + ", 2" + product.getUnit() + ")."
            );
        }

        amountInProductUnit = amountInProductUnit.setScale(3, RoundingMode.HALF_UP);

        if (amountInProductUnit.compareTo(BigDecimal.ZERO) <= 0) {
            return ConversionResult.failure("Số lượng không hợp lệ.");
        }

        if (!isWholeNumber(amountInProductUnit)) {
            if (productType == UnitType.WEIGHT && "kg".equals(productUnit)) {
                if (amountInProductUnit.compareTo(BigDecimal.ONE) < 0) {
                    String requestedText = describeRequested(rawQuantity, requestedUnit);
                    return ConversionResult.successWithNote(
                            1,
                            1.0,
                            product.getUnit(),
                            "Sản phẩm bán theo kg (tối thiểu 1kg). "
                                    + requestedText
                                    + " ≈ "
                                    + amountInProductUnit.stripTrailingZeros().toPlainString()
                                    + "kg — mình đã thêm 1kg cho bạn."
                    );
                }

                int rounded = amountInProductUnit.setScale(0, RoundingMode.HALF_UP).intValue();
                rounded = Math.max(1, rounded);
                return ConversionResult.successWithNote(
                        rounded,
                        rounded,
                        product.getUnit(),
                        "Mình đã quy đổi thành " + rounded + product.getUnit() + "."
                );
            }

            return ConversionResult.failure(
                    "Sản phẩm \"" + product.getProductName() + "\" chỉ hỗ trợ số lượng nguyên theo "
                            + product.getUnit() + "."
            );
        }

        int cartQuantity = amountInProductUnit.intValue();

        if (product.getStockQuantity() != null && cartQuantity > product.getStockQuantity()) {
            return ConversionResult.failure(
                    "Sản phẩm \"" + product.getProductName() + "\" chỉ còn "
                            + product.getStockQuantity() + " " + product.getUnit() + "."
            );
        }

        return ConversionResult.success(
                cartQuantity,
                cartQuantity,
                product.getUnit() != null ? product.getUnit() : productUnit
        );
    }

    public static ConversionResult resolveForMysteryBox(Integer requestedQuantity) {
        int qty = requestedQuantity == null || requestedQuantity <= 0 ? 1 : requestedQuantity;
        return ConversionResult.success(qty, qty, "hộp");
    }

    private static String describeRequested(double quantity, String unit) {
        if (unit == null || unit.isBlank()) {
            return quantity + " (đơn vị không rõ)";
        }
        return quantity + formatUnitLabel(unit);
    }

    private static String formatUnitLabel(String unit) {
        if (unit == null || unit.isBlank()) {
            return "";
        }
        return unit.startsWith(" ") ? unit : " " + unit;
    }

    private static boolean isWholeNumber(BigDecimal value) {
        return value.stripTrailingZeros().scale() <= 0;
    }

    private static BigDecimal toGrams(double quantity, String unit) {
        String normalized = normalizeUnit(unit);
        if ("kg".equals(normalized)) {
            return BigDecimal.valueOf(quantity).multiply(BigDecimal.valueOf(1000));
        }
        return BigDecimal.valueOf(quantity);
    }

    private static BigDecimal fromGrams(BigDecimal grams, String productUnit) {
        if ("kg".equals(normalizeUnit(productUnit))) {
            return grams.divide(BigDecimal.valueOf(1000), 6, RoundingMode.HALF_UP);
        }
        return grams;
    }

    private static boolean isSameCountUnit(String userUnit, String productUnit) {
        return normalizeUnit(userUnit).equals(normalizeUnit(productUnit));
    }

    private static UnitType detectUnitType(String unit) {
        String normalized = normalizeUnit(unit);
        if ("kg".equals(normalized) || "g".equals(normalized)) {
            return UnitType.WEIGHT;
        }
        return UnitType.COUNT;
    }

    public static String normalizeUnit(String unit) {
        if (unit == null) {
            return "";
        }

        String normalized = normalizeText(unit);

        return switch (normalized) {
            case "ki", "ky", "kg", "kilogram", "kilogam" -> "kg";
            case "gram", "gam", "g" -> "g";
            case "hop", "hop nho" -> "hop";
            case "hu", "ho" -> "hu";
            case "tui", "tui nilon" -> "tui";
            case "bich" -> "bich";
            case "bo" -> "bo";
            case "nai", "nai chuoi" -> "nai";
            case "qua" -> "qua";
            case "trai" -> "trai";
            case "cai" -> "cai";
            case "chai" -> "chai";
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

    private enum UnitType {
        WEIGHT,
        COUNT
    }
}
