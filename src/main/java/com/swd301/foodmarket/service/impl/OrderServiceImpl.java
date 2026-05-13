package com.swd301.foodmarket.service.impl;

import com.swd301.foodmarket.dto.goong.LatLong;
import com.swd301.foodmarket.dto.request.OrderCreationRequest;
import com.swd301.foodmarket.dto.request.OrderItemRequest;
import com.swd301.foodmarket.dto.request.OrderUpdateRequest;
import com.swd301.foodmarket.dto.response.OrderResponse;
import com.swd301.foodmarket.dto.response.PageResponse;
import com.swd301.foodmarket.entity.*;
import com.swd301.foodmarket.enums.OrderStatus;
import com.swd301.foodmarket.exception.AppException;
import com.swd301.foodmarket.exception.ErrorCode;
import com.swd301.foodmarket.mapper.OrderMapper;
import com.swd301.foodmarket.repository.*;
import com.swd301.foodmarket.service.CartService;
import com.swd301.foodmarket.service.GoongMapService;
import com.swd301.foodmarket.service.OrderService;
import com.swd301.foodmarket.service.UserService;
import org.springframework.transaction.annotation.Transactional;
import lombok.AccessLevel;
import lombok.RequiredArgsConstructor;
import lombok.experimental.FieldDefaults;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

@Service
@RequiredArgsConstructor
@FieldDefaults(level = AccessLevel.PRIVATE, makeFinal = true)
@Slf4j
public class OrderServiceImpl implements OrderService {

    OrderRepository orderRepository;
    UserRepository userRepository;
    ProductRepository productRepository;
    MysteryBoxRepository mysteryBoxRepository;
    OrderDetailRepository orderDetailRepository;
    OrderMapper orderMapper;
    UserService userService;
    CartService cartService;
    BuildComboRepository buildComboRepository;


    ProductMysteryRepository productMysteryRepository;

    GoongMapService goongMapService;   // ✅ dùng để geocode địa chỉ

    // ===================== SHIPPING FEE =====================

    /**
     * Công thức tính phí ship:
     * 0 – 2km  : 15,000đ
     * 2 – 5km  : 15,000 + 4,500/km vượt quá 2km
     * 5 – 10km : 28,500 + 4,000/km vượt quá 5km
     * > 10km   : 48,500 + 3,500/km vượt quá 10km
     */
//    private BigDecimal calcFeeByDistance(double distanceKm) {
//        double fee;
//        if (distanceKm <= 2.0) {
//            fee = 15_000;
//        } else if (distanceKm <= 5.0) {
//            fee = 15_000 + (distanceKm - 2.0) * 4_500;
//        } else if (distanceKm <= 10.0) {
//            fee = 28_500 + (distanceKm - 5.0) * 4_000;
//        } else {
//            fee = 48_500 + (distanceKm - 10.0) * 3_500;
//        }
//        // Làm tròn lên 1000đ gần nhất cho dễ hiển thị
//        long rounded = (long) (Math.ceil(fee / 1000.0) * 1000);
//        log.info("[ShippingFee] distance={} km → fee={} đ", String.format("%.2f", distanceKm), rounded);
//        return BigDecimal.valueOf(rounded);
//    }
    private BigDecimal calcFeeByDistance(double distanceKm) {
        double fee;
        if (distanceKm < 5.0) {
            fee = 15_000;
        } else if (distanceKm <= 10.0) {
            fee = 20_000;
        } else {
            fee = 20_000 + (distanceKm - 10.0) * 4_000;
        }

        // Làm tròn lên 1000đ gần nhất cho dễ hiển thị
        long rounded = (long) (Math.ceil(fee / 1000.0) * 1000);
        log.info("[ShippingFee] distance={} km → fee={} đ", String.format("%.2f", distanceKm), rounded);
        return BigDecimal.valueOf(rounded);
    }

    @Override
    public BigDecimal calculateShippingFee(String shippingAddress, Integer shopId) {
        try {
            // 1. Lấy địa chỉ shop
            User shop = userRepository.findById(shopId)
                    .orElseThrow(() -> new AppException(ErrorCode.USER_NOT_EXISTED));
            String shopAddress = shop.getAddress();

            if (shopAddress == null || shopAddress.isBlank()) {
                log.warn("[ShippingFee] Shop {} không có địa chỉ, dùng phí mặc định", shopId);
                return BigDecimal.valueOf(15_000);
            }

            // 2. Geocode 2 địa chỉ
            LatLong shopLatLng = goongMapService.geocodeAddress(shopAddress);
            LatLong buyerLatLng = goongMapService.geocodeAddress(shippingAddress);

            if (shopLatLng == null || buyerLatLng == null) {
                log.warn("[ShippingFee] Geocode thất bại, dùng phí mặc định");
                return BigDecimal.valueOf(15_000);
            }

            // 3. Tính khoảng cách Haversine
            double distKm = goongMapService.getDistanceKm(shopLatLng, buyerLatLng);
            if (distKm == Double.MAX_VALUE) {
                return BigDecimal.valueOf(15_000);
            }

            // 4. Tính phí
            return calcFeeByDistance(distKm);

        } catch (Exception e) {
            log.error("[ShippingFee] Lỗi tính phí ship: {}", e.getMessage());
            return BigDecimal.valueOf(15_000);
        }
    }

    // ===================== ORDER =====================


    @Override
    public List<OrderResponse> getAllOrders() {
        return orderRepository.findAll()
                .stream()
                .map(orderMapper::toResponse)
                .toList();
    }

    @Override
    public PageResponse<OrderResponse> getAllOrdersPaged(int page, int size) {
        Pageable pageable = PageRequest.of(page, size, Sort.by("createdAt").descending());
        Page<Order> pageResult = orderRepository.findAll(pageable);
        return PageResponse.<OrderResponse>builder()
                .content(pageResult.getContent().stream().map(orderMapper::toResponse).toList())
                .page(pageResult.getNumber())
                .size(pageResult.getSize())
                .totalElements(pageResult.getTotalElements())
                .totalPages(pageResult.getTotalPages())
                .first(pageResult.isFirst())
                .last(pageResult.isLast())
                .build();
    }

    @Override
    public List<OrderResponse> getOrdersByUserId(Integer userId) {
        return orderRepository.findByBuyerId(userId)
                .stream()
                .map(orderMapper::toResponse)
                .toList();
    }

    @Override
    public PageResponse<OrderResponse> getOrdersByUserIdPaged(Integer userId, int page, int size) {
        Pageable pageable = PageRequest.of(page, size, Sort.by("createdAt").descending());
        Page<Order> pageResult = orderRepository.findByBuyerId(userId, pageable);
        return PageResponse.<OrderResponse>builder()
                .content(pageResult.getContent().stream().map(orderMapper::toResponse).toList())
                .page(pageResult.getNumber())
                .size(pageResult.getSize())
                .totalElements(pageResult.getTotalElements())
                .totalPages(pageResult.getTotalPages())
                .first(pageResult.isFirst())
                .last(pageResult.isLast())
                .build();
    }

    @Transactional
    @Override
    public OrderResponse createOrder(OrderCreationRequest request) {

        User buyer = getCurrentUser();

        Order order = new Order();
        order.setBuyer(buyer);
        order.setRecipientName(getOrDefault(request.getRecipientName(), buyer.getFullName()));
        order.setRecipientPhone(getOrDefault(request.getRecipientPhone(), buyer.getPhoneNumber()));
        order.setShippingAddress(getOrDefault(request.getShippingAddress(), buyer.getAddress()));
        order.setNote(request.getNote());
        order.setPaymentMethod(request.getPaymentMethod());
        order.setStatus(OrderStatus.PENDING);

        List<OrderDetail> orderDetails = new ArrayList<>();
        List<OrderMysteryBox> mysteryBoxes = new ArrayList<>();
        BigDecimal totalAmount = BigDecimal.ZERO;
        User shopOwner = null;

        for (OrderItemRequest item : request.getItems()) {


            // ================= PRODUCT =================
            if (item.getProductId() != null) {
                    Product product = productRepository.findById(item.getProductId())
                            .orElseThrow(() -> new AppException(ErrorCode.PRODUCT_NOT_FOUND));

                    if (product.getStockQuantity() < item.getQuantity()) {
                        throw new RuntimeException(
                                "Product " + product.getProductName() + " is out of stock"
                        );
                    }

                    if (shopOwner == null) {
                        shopOwner = product.getShopOwner();
                    }

                    OrderDetail detail = new OrderDetail();
                    detail.setOrder(order);
                    detail.setProduct(product);
                    detail.setQuantity(item.getQuantity());
                    detail.setUnitPrice(product.getSellingPrice());

                    orderDetails.add(detail);

                    totalAmount = totalAmount.add(
                            product.getSellingPrice().multiply(BigDecimal.valueOf(item.getQuantity()))
                    );
                }

                // ================= MYSTERY BOX =================
                else if (item.getMysteryBoxId() != null) {

                    MysteryBox box = mysteryBoxRepository.findById(item.getMysteryBoxId())
                            .orElseThrow(() -> new AppException(ErrorCode.MYSTERY_BOX_NOT_FOUND));

                    if (!Boolean.TRUE.equals(box.getIsActive())) {
                        throw new AppException(ErrorCode.MYSTERY_BOX_NOT_AVAILABLE);
                    }

                    validateMysteryBoxStock(box, item.getQuantity());

                    if (shopOwner == null && box.getShopOwner() != null) {
                        shopOwner = box.getShopOwner();
                    }

                    OrderMysteryBox orderBox = OrderMysteryBox.builder()
                            .order(order)
                            .mysteryBox(box)
                            .quantity(item.getQuantity())
                            .build();

                    mysteryBoxes.add(orderBox);

                    if (box.getPrice() != null) {
                        totalAmount = totalAmount.add(
                                box.getPrice().multiply(BigDecimal.valueOf(item.getQuantity()))
                        );
                    }
                } else if (item.getBuildComboId() != null) {
                BuildCombo combo = buildComboRepository.findById(item.getBuildComboId())
                        .orElseThrow(() -> new AppException(ErrorCode.BUILD_COMBO_NOT_FOUND));

                if (shopOwner == null && combo.getShopOwner() != null) {
                    shopOwner = combo.getShopOwner();
                }

                // Tạo OrderDetail cho từng product trong combo
                for (ProductCombo comboItem : combo.getItems()) {
                    Product product = comboItem.getProduct();
                    int requiredQty = comboItem.getQuantity() * item.getQuantity();

                    if (product.getStockQuantity() < requiredQty) {
                        throw new AppException(ErrorCode.PRODUCT_OUT_OF_STOCK);
                    }

                    OrderDetail detail = new OrderDetail();
                    detail.setOrder(order);
                    detail.setProduct(product);
                    detail.setQuantity(requiredQty);
                    detail.setUnitPrice(product.getSellingPrice());
                    orderDetails.add(detail);
                }

                // Tính giá theo discountPrice của combo
                BigDecimal comboPrice = combo.getDiscountPrice() != null
                        ? combo.getDiscountPrice()
                        : combo.getItems().stream()
                        .map(i -> i.getProduct().getSellingPrice()
                                .multiply(BigDecimal.valueOf(i.getQuantity())))
                        .reduce(BigDecimal.ZERO, BigDecimal::add);
                totalAmount = totalAmount.add(comboPrice.multiply(BigDecimal.valueOf(item.getQuantity())));
            }else {
            throw new AppException(ErrorCode.INVALID_REQUEST);
        }
        }



            BigDecimal shippingFee = BigDecimal.valueOf(15_000); // fallback
            if (shopOwner != null) {
                String buyerAddress = getOrDefault(request.getShippingAddress(), buyer.getAddress());
                shippingFee = calculateShippingFee(buyerAddress, shopOwner.getId());
            }


            order.setShippingFee(shippingFee);
            if (shopOwner == null) {
                throw new AppException(ErrorCode.INVALID_REQUEST);
            }
            order.setShopOwner(shopOwner);
            order.setTotalAmount(totalAmount.add(shippingFee));
            order.setOrderDetails(orderDetails);
            order.setMysteryBoxes(mysteryBoxes);

            Order savedOrder = orderRepository.save(order);

            if (!orderDetails.isEmpty()) {
                for (OrderDetail detail : orderDetails) {
                    detail.setOrder(savedOrder);
                }
                orderDetailRepository.saveAll(orderDetails);
            }
//            cartService.clearCart();
            return orderMapper.toResponse(savedOrder);
        }

        @Transactional
        @Override
        public OrderResponse updateOrder (Integer id, OrderUpdateRequest request){

            Order order = orderRepository.findById(id)
                    .orElseThrow(() -> new AppException(ErrorCode.ORDER_NOT_FOUND));


                if (request.getStatus() == OrderStatus.CONFIRMED
                        && order.getStatus() != OrderStatus.CONFIRMED) {

                    List<Product> productsToUpdate = new ArrayList<>();

                    // -------- PRODUCT THƯỜNG --------
                    for (OrderDetail detail : order.getOrderDetails()) {
                        Product product = detail.getProduct();
                        if (product.getStockQuantity() < detail.getQuantity()) {
                            throw new AppException(ErrorCode.PRODUCT_OUT_OF_STOCK);
                        }
                        product.setStockQuantity(
                                product.getStockQuantity() - detail.getQuantity()
                        );

                        productsToUpdate.add(product);
                    }


                    // -------- MYSTERY BOX --------
                    for (OrderMysteryBox orderBox : order.getMysteryBoxes()) {


                        MysteryBox box = orderBox.getMysteryBox();
                        int boxQuantity = orderBox.getQuantity();

                        if (box.getSoldQuantity() + boxQuantity > box.getTotalQuantity()) {
                            throw new AppException(ErrorCode.MYSTERY_BOX_SOLD_OUT);
                        }

                        box.setSoldQuantity(box.getSoldQuantity() + boxQuantity);

                        if (box.getSoldQuantity() >= box.getTotalQuantity()) {
                            box.setIsActive(false);
                        }

                        mysteryBoxRepository.save(box);

                        List<ProductMystery> productMysteries =
                                productMysteryRepository.findByMysteryBox_Id(box.getId());

                        log.info("Processing mystery box: {}, ProductMysteries count: {}",
                                box.getId(), productMysteries.size());

                        boolean outOfStock = false;

                        for (ProductMystery pm : productMysteries) {

                            Product product = pm.getProduct();

                            int required = pm.getQuantity() * boxQuantity;

                            if (product.getStockQuantity() < required) {
                                outOfStock = true;
                                break;
                            }
                        }

                        // nếu thiếu stock → xoá box
                        if (outOfStock) {

                            log.warn("MysteryBox {} is out of stock -> disabling", box.getId());

                            box.setIsActive(false);

                            mysteryBoxRepository.save(box);

                            continue;
                        }

                        for (ProductMystery pm : productMysteries) {
                            Product product = pm.getProduct();
                            int required = pm.getQuantity() * boxQuantity;

                            log.info("Reducing stock for product: {}, from {} to {}",
                                    product.getProductName(), product.getStockQuantity(),
                                    product.getStockQuantity() - required);

                            product.setStockQuantity(
                                    product.getStockQuantity() - required

                            );

                            productsToUpdate.add(product);
                            checkMysteryBoxAvailability(product);
                        }
                    }

                    productRepository.saveAll(productsToUpdate);
                }

                // ================= CANCELLED =================
                if (request.getStatus() == OrderStatus.CANCELLED
                        && order.getStatus() == OrderStatus.CONFIRMED) {

                    Map<Integer, Product> productsToUpdate = new HashMap<>();


                    for (OrderDetail detail : order.getOrderDetails()) {
                        Product product = detail.getProduct();


                        product.setStockQuantity(
                                product.getStockQuantity() + detail.getQuantity()
                        );

                        productsToUpdate.put(product.getId(), product);
                    }

                    for (OrderMysteryBox orderBox : order.getMysteryBoxes()) {

                        MysteryBox box = orderBox.getMysteryBox();
                        int boxQuantity = orderBox.getQuantity();

                        List<ProductMystery> productMysteries =
                                productMysteryRepository.findByMysteryBox_Id(box.getId());

                        for (ProductMystery pm : productMysteries) {

                            Product product = pm.getProduct();

                            int restore = pm.getQuantity() * boxQuantity;

                            product.setStockQuantity(
                                    product.getStockQuantity() + restore
                            );

                            productsToUpdate.put(product.getId(), product);
                        }
                    }

                    productRepository.saveAll(productsToUpdate.values());

            }

            order.setStatus(request.getStatus());


        Order savedOrder = orderRepository.save(order);

        if (savedOrder.getStatus() == OrderStatus.FAILED) {

            Integer buyerId = savedOrder.getBuyer().getId();

            long failedCount = orderRepository
                    .countByBuyerIdAndStatus(buyerId, OrderStatus.FAILED);

            if (failedCount == 3) {

                userService.deactivateUser(buyerId);

                log.info("User {} has been auto locked due to 3 FAILED orders", buyerId);
            }
        }

        return orderMapper.toResponse(savedOrder);
    }

    @Transactional
    @Override
    public OrderResponse getOrderById(Integer id) {
        Order order = orderRepository.findById(id)
                .orElseThrow(() -> new AppException(ErrorCode.ORDER_NOT_FOUND));
        return orderMapper.toResponse(order);
    }

    @Override
    public void deleteOrder(Integer id) {
        Order order = orderRepository.findById(id)
                .orElseThrow(() -> new AppException(ErrorCode.ORDER_NOT_FOUND));

        User currentUser = getCurrentUser();
        if (!order.getShopOwner().getId().equals(currentUser.getId())) {
            throw new AppException(ErrorCode.UNAUTHORIZED);
        }


        if (order.getStatus() != OrderStatus.PENDING) {

                throw new AppException(ErrorCode.INVALID_ORDER_STATUS);
            }
        orderRepository.delete(order);
        }

        private User getCurrentUser () {
            String email = SecurityContextHolder.getContext().getAuthentication().getName();
            return userRepository.findByEmail(email)
                    .orElseThrow(() -> new AppException(ErrorCode.USER_NOT_EXISTED));
        }

        private String getOrDefault (String input, String defaultValue){
            return (input == null || input.isBlank()) ? defaultValue : input;
        }


        private void validateMysteryBoxStock (MysteryBox box, Integer quantity){

            List<ProductMystery> items =
                    productMysteryRepository.findByMysteryBox_Id(box.getId());

            for (ProductMystery item : items) {

                Product product = item.getProduct();

                int requiredQuantity = item.getQuantity() * quantity;

                if (product.getStockQuantity() < requiredQuantity) {

                    throw new AppException(ErrorCode.MYSTERY_BOX_OUT_OF_STOCK);
                }
            }
        }

        private void checkMysteryBoxAvailability (Product product){

            List<ProductMystery> relations =
                    productMysteryRepository.findByProduct_Id(product.getId());

            for (ProductMystery pm : relations) {

                MysteryBox box = pm.getMysteryBox();
                if (!Boolean.TRUE.equals(box.getIsActive())) {
                    continue;
                }

                List<ProductMystery> items =
                        productMysteryRepository.findByMysteryBox_Id(box.getId());

                boolean outOfStock = false;

                for (ProductMystery item : items) {

                    Product p = item.getProduct();

                    if (p.getStockQuantity() < item.getQuantity()) {
                        outOfStock = true;
                        break;
                    }
                }

                if (outOfStock) {

                    log.warn("Auto disabling MysteryBox {} due to product stock", box.getId());

                    box.setIsActive(false);

                    mysteryBoxRepository.save(box);
                }
            }
        }
    }
