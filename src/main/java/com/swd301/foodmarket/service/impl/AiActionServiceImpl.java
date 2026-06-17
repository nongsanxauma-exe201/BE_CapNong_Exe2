package com.swd301.foodmarket.service.impl;

import com.swd301.foodmarket.ai.exception.AiCartOperationException;
import com.swd301.foodmarket.dto.internal.AiCatalogOption;
import com.swd301.foodmarket.dto.request.AiItemCommand;
import com.swd301.foodmarket.dto.response.*;
import com.swd301.foodmarket.entity.MysteryBox;
import com.swd301.foodmarket.entity.Product;
import com.swd301.foodmarket.entity.User;
import com.swd301.foodmarket.enums.AiItemType;
import com.swd301.foodmarket.enums.AiPendingSelectionMode;
import com.swd301.foodmarket.enums.ProductStatus;
import com.swd301.foodmarket.repository.MysteryBoxRepository;
import com.swd301.foodmarket.repository.ProductRepository;
import com.swd301.foodmarket.service.AiActionService;
import com.swd301.foodmarket.service.AiCartGateway;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.text.Normalizer;
import java.time.LocalDateTime;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;
import java.util.regex.Pattern;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class AiActionServiceImpl implements AiActionService {

    private final ProductRepository productRepository;
    private final MysteryBoxRepository mysteryBoxRepository;
    private final AiCartGateway aiCartGateway;

    private final Map<String, PendingProductSelection> pendingSelections = new ConcurrentHashMap<>();

    @Override
    public AiChatResponse searchItems(User user, String sessionId, String keyword) {
        if (keyword == null || keyword.isBlank()) {
            return listAvailableCatalog(user, sessionId);
        }

        String productName = keyword.trim();
        AiItemType preferredType = inferItemType(productName);
        List<AiCatalogOption> options = searchCatalog(productName, preferredType);

        if (options.isEmpty()) {
            return AiChatResponse.text("Mình chưa tìm thấy sản phẩm phù hợp với: " + productName);
        }

        savePendingSelection(user, sessionId, options, AiPendingSelectionMode.BROWSE, null);

        return AiChatResponse.itemOptions(
                "Mình tìm thấy một số sản phẩm liên quan. Bạn muốn chọn loại nào?",
                options.stream()
                        .map(this::toOptionResponse)
                        .collect(Collectors.toList())
        );
    }

    private AiChatResponse listAvailableCatalog(User user, String sessionId) {
        List<AiCatalogOption> options = listAllAvailableOptions();

        if (options.isEmpty()) {
            return AiChatResponse.text("Hiện shop chưa có sản phẩm nào đang bán.");
        }

        savePendingSelection(user, sessionId, options, AiPendingSelectionMode.BROWSE, null);

        return AiChatResponse.itemOptions(
                "Hiện shop đang có các sản phẩm sau. Bạn muốn chọn loại nào?",
                options.stream()
                        .map(this::toOptionResponse)
                        .collect(Collectors.toList())
        );
    }

    private List<AiCatalogOption> listAllAvailableOptions() {
        List<AiCatalogOption> result = new ArrayList<>();

        result.addAll(
                productRepository.findByStatus(ProductStatus.AVAILABLE)
                        .stream()
                        .filter(this::isProductAvailable)
                        .map(this::toProductCatalogOption)
                        .limit(10)
                        .collect(Collectors.toList())
        );

        result.addAll(
                mysteryBoxRepository.findByIsActiveTrue()
                        .stream()
                        .filter(this::isMysteryBoxAvailable)
                        .map(this::toMysteryCatalogOption)
                        .limit(Math.max(0, 10 - result.size()))
                        .collect(Collectors.toList())
        );

        return result.stream().limit(10).collect(Collectors.toList());
    }

    @Override
    public AiChatResponse buyItems(User user, String sessionId, List<AiItemCommand> items) {
        if (user == null) {
            return AiChatResponse.error("Bạn cần đăng nhập trước khi mua hàng.");
        }

        if (items == null || items.isEmpty()) {
            return AiChatResponse.text("Bạn muốn mua sản phẩm nào?");
        }

        List<AiCartItemResponse> addedItems = new ArrayList<>();
        List<String> notFoundItems = new ArrayList<>();
        List<String> notes = new ArrayList<>();

        for (AiItemCommand item : items) {
            if (item.getName() == null || item.getName().isBlank()) {
                continue;
            }

            AiItemType preferredType = item.getItemType() == null
                    ? AiItemType.ANY
                    : item.getItemType();

            List<AiCatalogOption> options = searchCatalog(item.getName(), preferredType);

            if (options.isEmpty()) {
                notFoundItems.add(item.getName());
                continue;
            }

            if (options.size() > 1) {
                savePendingSelection(user, sessionId, options, AiPendingSelectionMode.BUY, item);

                return AiChatResponse.itemOptions(
                        "Mình tìm thấy nhiều lựa chọn cho \"" + item.getName() + "\". Bạn chọn loại số mấy?",
                        options.stream()
                                .map(this::toOptionResponse)
                                .collect(Collectors.toList())
                );
            }

            try {
                AiCatalogOption selected = options.get(0);
                AiCartPreviewResponse cartPreview = addSelectedOptionToCart(user, selected, item);

                if (cartPreview.getItems() != null) {
                    addedItems.addAll(cartPreview.getItems());
                }
                if (cartPreview.getNote() != null && !cartPreview.getNote().isBlank()) {
                    notes.add(cartPreview.getNote());
                }
            } catch (AiCartOperationException ex) {
                return AiChatResponse.error(ex.getMessage());
            }
        }

        if (addedItems.isEmpty() && !notFoundItems.isEmpty()) {
            return AiChatResponse.text(
                    "Mình chưa tìm thấy sản phẩm: " + String.join(", ", notFoundItems)
            );
        }

        AiCartPreviewResponse cart = AiCartPreviewResponse.builder()
                .items(addedItems)
                .totalAmount(
                        addedItems.stream()
                                .map(AiCartItemResponse::getTotalPrice)
                                .filter(Objects::nonNull)
                                .reduce(java.math.BigDecimal.ZERO, java.math.BigDecimal::add)
                )
                .build();

        String message = "Mình đã thêm sản phẩm vào giỏ hàng cho bạn.";

        if (!notes.isEmpty()) {
            message += " " + String.join(" ", notes);
        }

        if (!notFoundItems.isEmpty()) {
            message += " Tuy nhiên, mình chưa tìm thấy: " + String.join(", ", notFoundItems);
        }

        return AiChatResponse.cartUpdated(message, cart);
    }

    @Override
    public AiChatResponse selectItemOption(User user, String sessionId, Integer selectedIndex) {
        if (selectedIndex == null || selectedIndex <= 0) {
            return AiChatResponse.text("Bạn vui lòng chọn số sản phẩm hợp lệ.");
        }

        String pendingKey = resolvePendingKey(user, sessionId);
        PendingProductSelection pending = pendingSelections.get(pendingKey);

        if (pending == null) {
            return AiChatResponse.text("Hiện tại mình không có danh sách sản phẩm nào đang chờ bạn chọn.");
        }

        int realIndex = selectedIndex - 1;

        if (realIndex < 0 || realIndex >= pending.getOptions().size()) {
            return AiChatResponse.text("Số bạn chọn không nằm trong danh sách sản phẩm.");
        }

        AiCatalogOption selected = pending.getOptions().get(realIndex);
        pendingSelections.remove(pendingKey);

        if (pending.getMode() == AiPendingSelectionMode.BROWSE) {
            return buildProductDetailResponse(selected);
        }

        if (user == null) {
            return AiChatResponse.error("Bạn cần đăng nhập trước khi mua hàng.");
        }

        AiItemCommand originalItem = pending.getOriginalItem() != null
                ? pending.getOriginalItem()
                : AiItemCommand.builder().name(selected.getName()).quantity(1.0).build();

        try {
            AiCartPreviewResponse cartPreview = addSelectedOptionToCart(user, selected, originalItem);

            String message = "Mình đã thêm " + selected.getName() + " vào giỏ hàng cho bạn.";
            if (cartPreview.getNote() != null && !cartPreview.getNote().isBlank()) {
                message += " " + cartPreview.getNote();
            }

            return AiChatResponse.cartUpdated(message, cartPreview);
        } catch (AiCartOperationException ex) {
            return AiChatResponse.error(ex.getMessage());
        }
    }

    private void savePendingSelection(
            User user,
            String sessionId,
            List<AiCatalogOption> options,
            AiPendingSelectionMode mode,
            AiItemCommand originalItem
    ) {
        pendingSelections.put(
                resolvePendingKey(user, sessionId),
                PendingProductSelection.builder()
                        .userId(user != null ? user.getId() : null)
                        .originalItem(originalItem)
                        .options(options)
                        .mode(mode)
                        .createdAt(LocalDateTime.now())
                        .build()
        );
    }

    private String resolvePendingKey(User user, String sessionId) {
        if (user != null && user.getId() != null) {
            return "u:" + user.getId();
        }
        if (sessionId != null && !sessionId.isBlank()) {
            return "s:" + sessionId.trim();
        }
        return "anon";
    }

    private AiChatResponse buildProductDetailResponse(AiCatalogOption selected) {
        AiProductOptionResponse product = toOptionResponse(selected);

        String unit = product.getUnit() != null ? product.getUnit() : "sp";
        String priceText = product.getPrice() != null
                ? String.format("%,.0fđ/%s", product.getPrice().doubleValue(), unit)
                : "Liên hệ shop";

        String description = product.getDescription();
        if (description == null || description.isBlank()) {
            description = "Sản phẩm đang được cập nhật mô tả.";
        }

        String message = """
                **%s**

                **Giá:** %s
                **Loại:** %s

                %s
                """.formatted(
                product.getName(),
                priceText,
                product.getItemType() == AiItemType.MYSTERY_BOX ? "Túi mù" : "Sản phẩm",
                description.trim()
        );

        return AiChatResponse.productDetail(message, product);
    }

    @Override
    public AiChatResponse createCheckoutDraft(User user) {
        if (user == null) {
            return AiChatResponse.error("Bạn cần đăng nhập trước khi thanh toán.");
        }

        AiCheckoutDraftResponse checkout = aiCartGateway.createCheckoutDraft(user);

        return AiChatResponse.checkoutDraft(
                "Mình đã chuẩn bị trang thanh toán cho bạn. Bạn kiểm tra lại đơn hàng rồi bấm thanh toán nhé.",
                checkout
        );
    }

    private AiCartPreviewResponse addSelectedOptionToCart(
            User user,
            AiCatalogOption selected,
            AiItemCommand item
    ) {
        if (selected.getItemType() == AiItemType.MYSTERY_BOX) {
            int quantity = item.getQuantity() == null
                    ? 1
                    : Math.max(1, item.getQuantity().intValue());

            return aiCartGateway.addMysteryBoxToCart(
                    user,
                    selected.getMysteryBox(),
                    quantity
            );
        }

        double quantity = item.getQuantity() == null
                ? 1.0
                : item.getQuantity();

        return aiCartGateway.addProductToCart(
                user,
                selected.getProduct(),
                quantity,
                item.getUnit()
        );
    }

    private List<AiCatalogOption> searchCatalog(String keyword, AiItemType preferredType) {
        if (keyword == null || keyword.isBlank()) {
            return List.of();
        }

        AiItemType type = preferredType == null ? AiItemType.ANY : preferredType;

        List<AiCatalogOption> result = new ArrayList<>();

        if (type == AiItemType.PRODUCT || type == AiItemType.ANY) {
            result.addAll(searchProductsInternal(keyword));
        }

        if (type == AiItemType.MYSTERY_BOX || type == AiItemType.ANY) {
            result.addAll(searchMysteryBoxesInternal(keyword));
        }

        return result.stream()
                .limit(10)
                .collect(Collectors.toList());
    }

    private List<AiCatalogOption> searchProductsInternal(String keyword) {
        List<Product> products =
                productRepository.findTop10ByProductNameContainingIgnoreCaseAndStatus(
                        keyword.trim(),
                        ProductStatus.AVAILABLE
                );

        if (products.isEmpty()) {
            String normalizedKeyword = normalize(keyword);

            products = productRepository.findByStatus(ProductStatus.AVAILABLE)
                    .stream()
                    .filter(p -> p.getProductName() != null)
                    .filter(p -> normalize(p.getProductName()).contains(normalizedKeyword))
                    .limit(10)
                    .collect(Collectors.toList());
        }

        return products.stream()
                .filter(this::isProductAvailable)
                .map(this::toProductCatalogOption)
                .collect(Collectors.toList());
    }

    private List<AiCatalogOption> searchMysteryBoxesInternal(String keyword) {
        List<MysteryBox> boxes =
                mysteryBoxRepository.findTop10ByBoxTypeContainingIgnoreCaseAndIsActiveTrue(keyword.trim());

        if (boxes.isEmpty()) {
            String normalizedKeyword = normalize(keyword);

            boxes = mysteryBoxRepository.findByIsActiveTrue()
                    .stream()
                    .filter(box ->
                            normalize(box.getBoxType()).contains(normalizedKeyword)
                                    || normalize(box.getDescription()).contains(normalizedKeyword)
                                    || normalize(box.getNote()).contains(normalizedKeyword)
                                    || isMysteryKeyword(keyword)
                    )
                    .limit(10)
                    .collect(Collectors.toList());
        }

        return boxes.stream()
                .filter(this::isMysteryBoxAvailable)
                .map(this::toMysteryCatalogOption)
                .collect(Collectors.toList());
    }

    private boolean isProductAvailable(Product product) {
        if (product.getStatus() != ProductStatus.AVAILABLE) {
            return false;
        }

        if (product.getStockQuantity() == null) {
            return true;
        }

        return product.getStockQuantity() > 0;
    }

    private boolean isMysteryBoxAvailable(MysteryBox box) {
        if (box.getIsActive() == null || !box.getIsActive()) {
            return false;
        }

        int total = box.getTotalQuantity() == null ? 0 : box.getTotalQuantity();
        int sold = box.getSoldQuantity() == null ? 0 : box.getSoldQuantity();

        return total - sold > 0;
    }

    private AiCatalogOption toProductCatalogOption(Product product) {
        return AiCatalogOption.builder()
                .itemType(AiItemType.PRODUCT)
                .itemId(product.getId())
                .name(product.getProductName())
                .imageUrl(product.getImageUrl())
                .price(product.getSellingPrice())
                .unit(product.getUnit())
                .availableQuantity(product.getStockQuantity() == null ? null : product.getStockQuantity().doubleValue())
                .description(product.getDescription())
                .product(product)
                .build();
    }

    private AiCatalogOption toMysteryCatalogOption(MysteryBox box) {
        int total = box.getTotalQuantity() == null ? 0 : box.getTotalQuantity();
        int sold = box.getSoldQuantity() == null ? 0 : box.getSoldQuantity();
        int available = Math.max(total - sold, 0);

        return AiCatalogOption.builder()
                .itemType(AiItemType.MYSTERY_BOX)
                .itemId(box.getId())
                .name(box.getBoxType())
                .imageUrl(box.getImageUrl())
                .price(box.getPrice())
                .unit("hộp")
                .availableQuantity((double) available)
                .description(box.getDescription())
                .mysteryBox(box)
                .build();
    }

    private AiProductOptionResponse toOptionResponse(AiCatalogOption option) {
        return AiProductOptionResponse.builder()
                .itemType(option.getItemType())
                .itemId(option.getItemId())
                .name(option.getName())
                .imageUrl(option.getImageUrl())
                .price(option.getPrice())
                .unit(option.getUnit())
                .availableQuantity(option.getAvailableQuantity())
                .description(option.getDescription())
                .build();
    }

    private AiItemType inferItemType(String text) {
        if (isMysteryKeyword(text)) {
            return AiItemType.MYSTERY_BOX;
        }

        return AiItemType.ANY;
    }

    private boolean isMysteryKeyword(String text) {
        String normalized = normalize(text);

        return normalized.contains("mystery")
                || normalized.contains("mysty")
                || normalized.contains("mystybox")
                || normalized.contains("mystery box")
                || normalized.contains("box")
                || normalized.contains("tui mu")
                || normalized.contains("tui bi an")
                || normalized.contains("hop bi an");
    }

    private String normalize(String text) {
        if (text == null) return "";

        String temp = Normalizer.normalize(text, Normalizer.Form.NFD);

        return Pattern.compile("\\p{InCombiningDiacriticalMarks}+")
                .matcher(temp)
                .replaceAll("")
                .toLowerCase()
                .replace("đ", "d")
                .replaceAll("[^a-z0-9\\s]", " ")
                .replaceAll("\\s+", " ")
                .trim();
    }
}
