package com.swd301.foodmarket.service.impl;

import com.swd301.foodmarket.dto.goong.LatLong;
import com.swd301.foodmarket.dto.request.CheckoutBatchRequest;
import com.swd301.foodmarket.dto.request.OrderCreationRequest;
import com.swd301.foodmarket.dto.request.OrderItemRequest;
import com.swd301.foodmarket.dto.request.OrderUpdateRequest;
import com.swd301.foodmarket.dto.request.ShopOrderRequest;
import com.swd301.foodmarket.dto.response.*;
import com.swd301.foodmarket.entity.*;
import com.swd301.foodmarket.enums.CheckoutBatchStatus;
import com.swd301.foodmarket.enums.OrderStatus;
import com.swd301.foodmarket.enums.ProductStatus;
import com.swd301.foodmarket.exception.AppException;
import com.swd301.foodmarket.exception.ErrorCode;
import com.swd301.foodmarket.mapper.OrderMapper;
import com.swd301.foodmarket.repository.*;
import com.swd301.foodmarket.service.*;
import org.springframework.transaction.annotation.Transactional;
import lombok.AccessLevel;
import lombok.RequiredArgsConstructor;
import lombok.experimental.FieldDefaults;
import lombok.experimental.NonFinal;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.DayOfWeek;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.temporal.TemporalAdjusters;
import java.util.*;

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
    TrackingRepository trackingRepository;


    ProductMysteryRepository productMysteryRepository;

    GoongMapService goongMapService;
    TrackingService trackingService;
    ReviewRepository reviewRepository;
    WarehouseRepository warehouseRepository;
    VoucherUserService voucherUserService;
    OrderRewardService orderRewardService;
    CheckoutBatchRepository checkoutBatchRepository;
    PayosService payosService;
    PaymentService paymentService;
    QcSettlementService qcSettlementService;

    @NonFinal
    @Value("${ADDRESS_WAREHOUSE}")
    String warehouseAddress;

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
            // 1. Lấy toạ độ kho (origin) — shipper giao hàng từ kho, không phải từ shop
            LatLong warehouseLatLng = getWarehouseLatLng();
            if (warehouseLatLng == null) {
                log.warn("[ShippingFee] Không lấy được toạ độ kho, dùng phí mặc định");
                return BigDecimal.valueOf(15_000);
            }

            // 2. Geocode địa chỉ buyer (destination)
            LatLong buyerLatLng = goongMapService.geocodeAddress(shippingAddress);
            if (buyerLatLng == null) {
                log.warn("[ShippingFee] Geocode địa chỉ buyer thất bại: '{}', dùng phí mặc định", shippingAddress);
                return BigDecimal.valueOf(15_000);
            }

            log.info("[ShippingFee] Kho ({}, {}) → Buyer ({}, {})",
                    warehouseLatLng.getLatitude(), warehouseLatLng.getLongitude(),
                    buyerLatLng.getLatitude(), buyerLatLng.getLongitude());

            // 3. Tính khoảng cách kho → buyer
            double distKm = goongMapService.getDistanceKm(warehouseLatLng, buyerLatLng);
            if (distKm == Double.MAX_VALUE || distKm > 150.0) {
                log.warn("[ShippingFee] Khoảng cách bất thường: {} km — dùng phí mặc định", distKm);
                return BigDecimal.valueOf(15_000);
            }

            // 4. Tính phí
            return calcFeeByDistance(distKm);

        } catch (Exception e) {
            log.error("[ShippingFee] Lỗi tính phí ship: {}", e.getMessage());
            return BigDecimal.valueOf(15_000);
        }
    }

    @Override
    public BigDecimal calculateShippingFeeByPlaceId(String placeId, Integer shopId) {
        try {
            LatLong warehouseLatLng = getWarehouseLatLng();
            if (warehouseLatLng == null) {
                log.warn("[ShippingFee] Không lấy được toạ độ kho, dùng phí mặc định");
                return BigDecimal.valueOf(15_000);
            }

            // Dùng Place Detail trực tiếp — không chạy AutoComplete nữa
            LatLong buyerLatLng = goongMapService.geocodeByPlaceId(placeId);
            if (buyerLatLng == null) {
                log.warn("[ShippingFee] Place Detail thất bại cho placeId='{}', dùng phí mặc định", placeId);
                return BigDecimal.valueOf(15_000);
            }

            log.info("[ShippingFee][placeId] Kho ({}, {}) → Buyer ({}, {})",
                    warehouseLatLng.getLatitude(), warehouseLatLng.getLongitude(),
                    buyerLatLng.getLatitude(), buyerLatLng.getLongitude());

            double distKm = goongMapService.getDistanceKm(warehouseLatLng, buyerLatLng);
            if (distKm == Double.MAX_VALUE || distKm > 150.0) {
                log.warn("[ShippingFee][placeId] Khoảng cách bất thường: {} km — dùng phí mặc định", distKm);
                return BigDecimal.valueOf(15_000);
            }

            return calcFeeByDistance(distKm);

        } catch (Exception e) {
            log.error("[ShippingFee][placeId] Lỗi: {}", e.getMessage());
            return BigDecimal.valueOf(15_000);
        }
    }

    /**
     * Lấy toạ độ kho đang active.
     * Ưu tiên lat/lng đã lưu trong DB (không cần geocode).
     * Fallback: geocode địa chỉ kho từ DB hoặc config ADDRESS_WAREHOUSE.
     */
    private LatLong getWarehouseLatLng() {
        Warehouse warehouse = warehouseRepository.findAllByActive(true)
                .stream().findFirst().orElse(null);

        if (warehouse != null && warehouse.getLatitude() != null && warehouse.getLongitude() != null) {
            log.info("[ShippingFee] Dùng toạ độ kho từ DB: id={} ({}, {})",
                    warehouse.getId(), warehouse.getLatitude(), warehouse.getLongitude());
            return new LatLong(warehouse.getLatitude(), warehouse.getLongitude());
        }

        String addr = (warehouse != null && warehouse.getAddress() != null && !warehouse.getAddress().isBlank())
                ? warehouse.getAddress()
                : warehouseAddress;
        log.info("[ShippingFee] Geocode địa chỉ kho: '{}'", addr);
        return goongMapService.geocodeAddress(addr);
    }

    @Override
    public Page<OrderResponse> getOrdersForStaff(int page, int size, OrderStatus status) {
        Pageable pageable = PageRequest.of(page, size, Sort.by("createdAt").descending());
        List<OrderStatus> listStatus = new ArrayList<>();
        if (status != null) {
            listStatus.add(status);
        } else {
            listStatus.addAll(List.of(
                            OrderStatus.SHIPPING_TO_WAREHOUSE,
                            OrderStatus.ARRIVED_AT_WAREHOUSE,
                            OrderStatus.QUALITY_CHECKING,
                            OrderStatus.QUALITY_CHECKED,
                            OrderStatus.READY
                    )
            );
        }
        Page<Order> resultPage = orderRepository.findAllByStatusIsIn(listStatus, pageable);
        return resultPage.map(orderMapper::toResponse);
    }

    @Override
    public StaffOrderReportResponse generateStaffOrderReport() {


        LocalDateTime startOfDay = LocalDate.now().atStartOfDay();
        LocalDateTime endOfDay = startOfDay.plusDays(1);
        StaffOrderReportResponse response = StaffOrderReportResponse.builder()
                .incomingOrders(orderRepository.countByStatus(OrderStatus.SHIPPING_TO_WAREHOUSE))
                .quantityChecking(orderRepository.countByStatus(OrderStatus.QUALITY_CHECKING))
                .packagingOrders(orderRepository.countByStatus(OrderStatus.QUALITY_CHECKED))
                .qualityCheckingOrders(orderDetailRepository.countByOrder_Status(OrderStatus.ARRIVED_AT_WAREHOUSE))
                .readyOrders(orderRepository.countByStatus(OrderStatus.READY))
                .approvedTodayOrders(trackingRepository.countByCreatedAtBetweenAndStatus(startOfDay, endOfDay, OrderStatus.QUALITY_CHECKED))
                .pendingFarmerApprovals(orderRepository.countDistinctShopOwnerIdByStatusIsIn(List.of(OrderStatus.ARRIVED_AT_WAREHOUSE, OrderStatus.QUALITY_CHECKING)))
                .build();
        return response;
    }
    @Override
    public OrderShopReport orderShopReport(String type) {
        User shopOwner = getCurrentUser();

        LocalDate today = LocalDate.now();

        LocalDateTime from;
        LocalDateTime to;

        if (type == null) {
            type = "all";
        }

        if (type.equalsIgnoreCase("week")) {
            // Tuần này: T2 -> CN
            LocalDate thisWeekMonday = today.with(
                    TemporalAdjusters.previousOrSame(DayOfWeek.MONDAY)
            );

            from = thisWeekMonday.atStartOfDay();
            to = thisWeekMonday.plusDays(7).atStartOfDay();

        } else if (type.equalsIgnoreCase("month")) {
            // Tháng này
            LocalDate firstDayOfMonth = today.with(TemporalAdjusters.firstDayOfMonth());
            LocalDate firstDayOfNextMonth = firstDayOfMonth.plusMonths(1);

            from = firstDayOfMonth.atStartOfDay();
            to = firstDayOfNextMonth.atStartOfDay();

        } else {
            // All: 6 tháng gần nhất, tính từ đầu tháng của 5 tháng trước đến hiện tại
            LocalDate startMonth = today.withDayOfMonth(1).minusMonths(5);
            LocalDate nextDay = today.plusDays(1);

            from = startMonth.atStartOfDay();
            to = nextDay.atStartOfDay();
        }

        Long totalOrder = orderRepository.countByShopOwnerFromTo(shopOwner, from, to);
        if (totalOrder == null) totalOrder = 0L;

        Double totalPrice = orderRepository.sumPriceByShop(shopOwner, from, to,OrderStatus.DELIVERED);
        if (totalPrice == null) totalPrice = 0D;

        Double averagePrice = totalOrder > 0 ? totalPrice / totalOrder : 0D;

        // Phí platform 10%, shop nhận 90%
        BigDecimal revenue = BigDecimal.valueOf(totalPrice * 0.9);

        Double averageReview = reviewRepository.getAverageRatingByShopOwnerByDate(
                shopOwner,
                from,
                to
        );
        if (averageReview == null) averageReview = 0D;

        return OrderShopReport.builder()
                .totalOrder(totalOrder)
                .averagePrice(averagePrice)
                .totalRevenue(revenue)
                .averageReview(averageReview)
                .reportOrderWeek(reportOrderChart(type, from, to))
                .topSold(getTopSold(from, to))
                .build();
    }

    @Override
    public OrderResponseReport orderReport() {
        User user = getCurrentUser();
        return OrderResponseReport.builder()
                .totalRevenua(user.getShopWallet().getTotalRevenueAllTime())
                .totalCreated(orderRepository.countAllByShopOwnerAndStatus(user,OrderStatus.PENDING))
                .totalComplete(orderRepository.countAllByShopOwnerAndStatus(user,OrderStatus.DELIVERED))
                .build();
    }

    public List<OrderShopReport.ReportOrderDay> reportOrderChart(
            String type,
            LocalDateTime reportFrom,
            LocalDateTime reportTo
    ) {
        if (type.equalsIgnoreCase("week")) {
            return reportOrderByCurrentWeek(reportFrom);
        }

        if (type.equalsIgnoreCase("month")) {
            return reportOrderByCurrentMonth(reportFrom, reportTo);
        }

        return reportOrderByLastSixMonths(reportFrom, reportTo);
    }private List<OrderShopReport.ReportOrderDay> reportOrderByCurrentWeek(LocalDateTime weekStart) {
        List<OrderShopReport.ReportOrderDay> result = new ArrayList<>();

        LocalDateTime cursor = weekStart;

        String[] labels = {"T2", "T3", "T4", "T5", "T6", "T7", "CN"};

        for (int i = 0; i < 7; i++) {
            LocalDateTime from = cursor;
            LocalDateTime to = cursor.plusDays(1);

            result.add(buildChartItem(labels[i], from, to));

            cursor = to;
        }

        return result;
    }private List<OrderShopReport.ReportOrderDay> reportOrderByCurrentMonth(
            LocalDateTime monthStart,
            LocalDateTime nextMonthStart
    ) {
        List<OrderShopReport.ReportOrderDay> result = new ArrayList<>();

        LocalDate startDate = monthStart.toLocalDate();
        LocalDate endDateExclusive = nextMonthStart.toLocalDate();

        for (int i = 0; i < 4; i++) {
            LocalDate bucketStart = startDate.plusDays(i * 7L);
            LocalDate bucketEndExclusive;

            if (i == 3) {
                bucketEndExclusive = endDateExclusive;
            } else {
                bucketEndExclusive = bucketStart.plusDays(7);
                if (bucketEndExclusive.isAfter(endDateExclusive)) {
                    bucketEndExclusive = endDateExclusive;
                }
            }

            if (!bucketStart.isBefore(endDateExclusive)) {
                break;
            }

            LocalDateTime from = bucketStart.atStartOfDay();
            LocalDateTime to = bucketEndExclusive.atStartOfDay();

            result.add(buildChartItem("Tuần " + (i + 1), from, to));
        }

        return result;
    }private List<OrderShopReport.ReportOrderDay> reportOrderByLastSixMonths(
            LocalDateTime reportFrom,
            LocalDateTime reportTo
    ) {
        List<OrderShopReport.ReportOrderDay> result = new ArrayList<>();

        LocalDate startMonth = reportFrom.toLocalDate().withDayOfMonth(1);

        for (int i = 0; i < 6; i++) {
            LocalDate monthStart = startMonth.plusMonths(i);
            LocalDate nextMonthStart = monthStart.plusMonths(1);

            LocalDateTime from = monthStart.atStartOfDay();
            LocalDateTime to = nextMonthStart.atStartOfDay();

            if (to.isAfter(reportTo)) {
                to = reportTo;
            }

            String label = "Th" + monthStart.getMonthValue();

            result.add(buildChartItem(label, from, to));
        }

        return result;
    }
    private OrderShopReport.ReportOrderDay buildChartItem(
            String label,
            LocalDateTime from,
            LocalDateTime to
    ) {
        User shopOwner = getCurrentUser();

        List<OrderStatus> processingStatuses = new ArrayList<>(Arrays.asList(OrderStatus.values()));
        processingStatuses.remove(OrderStatus.DELIVERED);
        processingStatuses.remove(OrderStatus.CANCELLED);

        Long cancelled = orderRepository.countByShopOwnerAndStatusFromTo(
                shopOwner,
                OrderStatus.CANCELLED,
                from,
                to
        );

        Long completed = orderRepository.countByShopOwnerAndStatusFromTo(
                shopOwner,
                OrderStatus.DELIVERED,
                from,
                to
        );

        Long processing = orderRepository.countByShopOwnerAndStatusInFromTo(
                shopOwner,
                processingStatuses,
                from,
                to
        );

        if (cancelled == null) cancelled = 0L;
        if (completed == null) completed = 0L;
        if (processing == null) processing = 0L;

        Long total = completed + processing + cancelled;

        return OrderShopReport.ReportOrderDay.builder()
                .label(label)
                .fromDate(from.toLocalDate())
                .toDate(to.minusSeconds(1).toLocalDate())
                .completed(completed)
                .processing(processing)
                .cancelled(cancelled)
                .total(total)
                .build();
    }public List<OrderShopReport.TopSold> getTopSold(LocalDateTime from, LocalDateTime to) {
        User shopOwner = getCurrentUser();

        List<Product> products = productRepository
                .topSold(shopOwner, OrderStatus.DELIVERED, from, to, PageRequest.of(0, 3))
                .getContent();

        List<OrderShopReport.TopSold> topSolds = new ArrayList<>();

        for (Product product : products) {
            Long sold = orderDetailRepository.calculateSoldByDate(
                    product.getId(),
                    OrderStatus.DELIVERED,
                    from,
                    to
            );

            if (sold == null) sold = 0L;

            OrderShopReport.TopSold topSold = OrderShopReport.TopSold.builder()
                    .price(product.getSellingPrice())
                    .sold(sold)
                    .productName(product.getProductName())
                    .productId(product.getId())
                    .build();

            topSolds.add(topSold);
        }

        return topSolds;
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
        return getAllOrdersPaged(page, size, null);
    }

    @Override
    public PageResponse<OrderResponse> getAllOrdersPaged(int page, int size, List<OrderStatus> statuses) {
        Pageable pageable = PageRequest.of(page, size, Sort.by("createdAt").descending());
        User shopOwner = getCurrentUser();
        Page<Order> pageResult = (statuses == null || statuses.isEmpty())
                ? orderRepository.findByShopOwner(shopOwner, pageable)
                : orderRepository.findByShopOwnerAndStatusIn(shopOwner, statuses, pageable);
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
        return getOrdersByUserIdPaged(userId, page, size, null);
    }

    @Transactional(readOnly = true)
    @Override
    public PageResponse<OrderResponse> getOrdersByUserIdPaged(Integer userId, int page, int size, List<OrderStatus> statuses) {
        Pageable pageable = PageRequest.of(page, size, Sort.by("createdAt").descending());
        Page<Order> pageResult = (statuses == null || statuses.isEmpty())
                ? orderRepository.findByBuyerIdWithBatch(userId, pageable)
                : orderRepository.findByBuyerIdAndStatusInWithBatch(userId, statuses, pageable);
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

        order.setPaymentMethod(
                request.getPaymentMethod() != null
                        ? request.getPaymentMethod()
                        : "PAYOS"
        );
//        order.setPaymentMethod(request.getPaymentMethod());
        order.setStatus(OrderStatus.PENDING);

        List<OrderDetail> orderDetails = new ArrayList<>();
        List<OrderMysteryBox> mysteryBoxes = new ArrayList<>();
        BigDecimal totalAmount = BigDecimal.ZERO;
        User shopOwner = null;

        for (OrderItemRequest item : request.getItems()) {


            // ================= PRODUCT =================
            if (item.getProductId() != null) {
                BigDecimal quantityKg = resolveRequestKg(item.getQuantityKg(), item.getQuantity());
                Product product = productRepository.findById(item.getProductId())
                        .orElseThrow(() -> new AppException(ErrorCode.PRODUCT_NOT_FOUND));

                if (product.getStatus() != ProductStatus.AVAILABLE) {
                    throw new AppException(ErrorCode.PRODUCT_NOT_FOUND);
                }

                if (resolveProductStockKg(product).compareTo(quantityKg) < 0) {
                    throw new RuntimeException(
                            "Sản phẩm chỉ còn " + resolveProductStockKg(product).stripTrailingZeros().toPlainString()
                                    + " kg trong kho, không đủ để mua " + quantityKg.stripTrailingZeros().toPlainString() + " kg."
                    );
                }

                product.setStockKg(resolveProductStockKg(product).subtract(quantityKg));
                product.setStockQuantity(product.getStockKg().setScale(0, RoundingMode.DOWN).intValue());
                productRepository.save(product);

                if (shopOwner == null) {
                    shopOwner = product.getShopOwner();
                } else {
                    assertSameShopOwner(shopOwner, product.getShopOwner());
                }

                BigDecimal effectivePrice = resolveEffectiveSellingPrice(product);

                OrderDetail detail = new OrderDetail();
                detail.setOrder(order);
                detail.setProduct(product);
                detail.setQuantity(resolveLegacyQuantity(quantityKg, item.getQuantity()));
                detail.setQuantityKg(quantityKg);
                detail.setUnitPrice(effectivePrice);
                detail.setPricePerKg(effectivePrice);
                detail.setUnitWeightKg(BigDecimal.ONE);
                detail.setRescuedWeightKg(quantityKg);

                orderDetails.add(detail);

                totalAmount = totalAmount.add(
                        effectivePrice.multiply(quantityKg)
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

                box.setSoldQuantity(box.getSoldQuantity() + item.getQuantity());
                if (box.getSoldQuantity() >= box.getTotalQuantity()) {
                    box.setIsActive(false);
                }
                mysteryBoxRepository.save(box);

                if (shopOwner == null && box.getShopOwner() != null) {
                    shopOwner = box.getShopOwner();
                } else if (box.getShopOwner() != null) {
                    assertSameShopOwner(shopOwner, box.getShopOwner());
                }

                OrderMysteryBox orderBox = OrderMysteryBox.builder()
                        .order(order)
                        .mysteryBox(box)
                        .quantity(item.getQuantity())
                        .unitWeightKg(calculateMysteryBoxUnitWeight(box))
                        .build();
                orderBox.setRescuedWeightKg(orderBox.getUnitWeightKg().multiply(BigDecimal.valueOf(item.getQuantity())));

                mysteryBoxes.add(orderBox);

                if (box.getPrice() != null) {
                    totalAmount = totalAmount.add(
                            box.getPrice().multiply(BigDecimal.valueOf(item.getQuantity()))
                    );
                }
            } else if (item.getBuildComboId() != null) {
                BigDecimal comboCount = resolveRequestKg(item.getQuantityKg(), item.getQuantity());
                BuildCombo combo = buildComboRepository.findById(item.getBuildComboId())
                        .orElseThrow(() -> new AppException(ErrorCode.BUILD_COMBO_NOT_FOUND));

                if (shopOwner == null && combo.getShopOwner() != null) {
                    shopOwner = combo.getShopOwner();
                } else if (combo.getShopOwner() != null) {
                    assertSameShopOwner(shopOwner, combo.getShopOwner());
                }

                // Tạo OrderDetail cho từng product trong combo
                for (ProductCombo comboItem : combo.getItems()) {
                    Product product = comboItem.getProduct();
                    BigDecimal requiredKg = resolveProductComboKg(comboItem).multiply(comboCount).setScale(2, RoundingMode.HALF_UP);

                    if (resolveProductStockKg(product).compareTo(requiredKg) < 0) {
                        throw new AppException(ErrorCode.PRODUCT_OUT_OF_STOCK);
                    }

                    product.setStockKg(resolveProductStockKg(product).subtract(requiredKg));
                    product.setStockQuantity(product.getStockKg().setScale(0, RoundingMode.DOWN).intValue());
                    productRepository.save(product);

                    OrderDetail detail = new OrderDetail();
                    detail.setOrder(order);
                    detail.setProduct(product);
                    detail.setQuantity(resolveLegacyQuantity(requiredKg, null));
                    detail.setQuantityKg(requiredKg);
                    detail.setUnitPrice(product.getSellingPrice());
                    detail.setPricePerKg(product.getSellingPrice());
                    detail.setUnitWeightKg(BigDecimal.ONE);
                    detail.setRescuedWeightKg(requiredKg);
                    orderDetails.add(detail);
                }

                // Tính giá theo discountPrice của combo
                BigDecimal comboPrice = combo.getDiscountPrice() != null
                        ? combo.getDiscountPrice()
                        : combo.getItems().stream()
                        .map(i -> resolveEffectiveSellingPrice(i.getProduct())
                                .multiply(resolveProductComboKg(i)))
                        .reduce(BigDecimal.ZERO, BigDecimal::add);
                totalAmount = totalAmount.add(comboPrice.multiply(comboCount));
            } else {
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
        BigDecimal rescuedWeight = calculateSnapshotRescuedWeight(orderDetails, mysteryBoxes);
        order.setTotalKg(rescuedWeight);
        order.setRescuedWeight(rescuedWeight);
        order.setRewardPoints(rescuedWeight);
        order.setRewardApplied(false);
        if ("COD".equalsIgnoreCase(order.getPaymentMethod())) {
            order.setStatus(OrderStatus.PENDING);
            order.setCodAmount(order.getTotalAmount());
            order.setCodSettled(false);
            order.setCodSettledAt(null);
        } else {
            order.setStatus(OrderStatus.PENDING);
            order.setCodAmount(BigDecimal.ZERO);
            order.setCodSettled(false);
            order.setCodSettledAt(null);
        }
        Order savedOrder = orderRepository.save(order);
        List<String> voucherIdentifiers = collectVoucherIdentifiers(request);
        if (!voucherIdentifiers.isEmpty()) {
            boolean shopOnly = Boolean.TRUE.equals(request.getRestrictToShopVouchers());
            voucherUserService.useVoucherForOrder(savedOrder, voucherIdentifiers, shopOnly);
            if ("COD".equalsIgnoreCase(savedOrder.getPaymentMethod())) {
                savedOrder.setCodAmount(savedOrder.getTotalAmount());
            }
            savedOrder = orderRepository.save(savedOrder);
        }

        if (!orderDetails.isEmpty()) {
            for (OrderDetail detail : orderDetails) {
                detail.setOrder(savedOrder);
            }
            orderDetailRepository.saveAll(orderDetails);
        }
        for (OrderItemRequest item : request.getItems()) {
            try {
                if (item.getProductId() != null) cartService.removeItem(item.getProductId());
                else if (item.getMysteryBoxId() != null) cartService.removeMysteryBox(item.getMysteryBoxId());
                else if (item.getBuildComboId() != null) cartService.removeBuildCombo(item.getBuildComboId());
            } catch (Exception ignored) {}
        }
        return orderMapper.toResponse(savedOrder);
    }

    @Transactional
    @Override
    public OrderResponse updateOrder(Integer id, OrderUpdateRequest request, List<MultipartFile> files) {

        Order order = orderRepository.findById(id)
                .orElseThrow(() -> new AppException(ErrorCode.ORDER_NOT_FOUND));

        if (request.getStatus() == OrderStatus.READY) {
            if (order.getStatus() == OrderStatus.QC_PENDING_BUYER) {
                throw new AppException(ErrorCode.QC_BUYER_DECISION_INVALID);
            }
            if (!hasShippableItemsForPacking(order)) {
                throw new AppException(ErrorCode.INVALID_ORDER_STATUS);
            }
        }

        // soldQuantity, nguyên liệu và product stock đã được cập nhật tại PENDING
        // → không cần thay đổi stock khi CONFIRMED

        // ================= CANCELLED =================
        // Hoàn trả stock khi huỷ từ bất kỳ trạng thái nào (PENDING hoặc CONFIRMED)
        if (request.getStatus() == OrderStatus.CANCELLED
                && order.getStatus() != OrderStatus.CANCELLED) {

            boolean paidViaPayos = "PAYOS".equalsIgnoreCase(order.getPaymentMethod())
                    && isPayosBuyerCancellableStatus(order.getStatus());

            Map<Integer, Product> productsToUpdate = new HashMap<>();

            // Hoàn trả stockQuantity cho product thường (đã giảm tại PENDING)
            for (OrderDetail detail : order.getOrderDetails()) {
                Product product = detail.getProduct();
                product.setStockKg(resolveProductStockKg(product).add(resolveOrderDetailKg(detail)));
                product.setStockQuantity(product.getStockKg().setScale(0, RoundingMode.DOWN).intValue());
                productsToUpdate.put(product.getId(), product);
            }

            for (OrderMysteryBox orderBox : order.getMysteryBoxes()) {

                MysteryBox box = orderBox.getMysteryBox();
                int boxQuantity = orderBox.getQuantity();

                // Hoàn trả soldQuantity (đã tăng tại PENDING)
                box.setSoldQuantity(Math.max(0, box.getSoldQuantity() - boxQuantity));
                if (!Boolean.TRUE.equals(box.getIsActive()) && box.getSoldQuantity() < box.getTotalQuantity()) {
                    box.setIsActive(true);
                }
                mysteryBoxRepository.save(box);
            }

            productRepository.saveAll(productsToUpdate.values());
            voucherUserService.restoreVoucherForOrder(order);

            if (paidViaPayos) {
                paymentService.refundPayosOrderOnCancel(order);
            } else {
                cartService.restoreOrderItemsToCart(order);
            }

            orderRewardService.revokeRewardIfApplied(order);

        }

        order.setStatus(request.getStatus());
        orderRewardService.applyRewardIfEligible(order);


        Order savedOrder = orderRepository.save(order);
        // tracking order
        trackingService.trackingOrder(order, files);

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

    @Override
    @Transactional
    public OrderResponse processQcBuyerDecision(Integer orderId, boolean acceptRemaining) {
        return qcSettlementService.processQcBuyerDecision(orderId, acceptRemaining);
    }

    private boolean isPayosBuyerCancellableStatus(OrderStatus status) {
        return status == OrderStatus.PAID || status == OrderStatus.CONFIRMED;
    }

    private boolean hasShippableItemsForPacking(Order order) {
        if (order.getOrderDetails() != null) {
            for (OrderDetail detail : order.getOrderDetails()) {
                QualityResult qr = detail.getQualityResult();
                if (qr == null) {
                    return false;
                }
                if (!qr.isLineCancelled() && qr.getApprovedKg() != null && qr.getApprovedKg().signum() > 0) {
                    return true;
                }
            }
        }
        if (order.getMysteryBoxes() != null) {
            for (OrderMysteryBox box : order.getMysteryBoxes()) {
                QualityResult qr = box.getQualityResult();
                if (qr == null) {
                    return false;
                }
                if (!qr.isLineCancelled() && qr.getApprovedKg() != null && qr.getApprovedKg().signum() > 0) {
                    return true;
                }
            }
        }
        return false;
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

    private User getCurrentUser() {
        String email = SecurityContextHolder.getContext().getAuthentication().getName();
        return userRepository.findByEmail(email)
                .orElseThrow(() -> new AppException(ErrorCode.USER_NOT_EXISTED));
    }

    private String getOrDefault(String input, String defaultValue) {
        return (input == null || input.isBlank()) ? defaultValue : input;
    }

    private BigDecimal resolveProductWeightKg(Product product) {
        return BigDecimal.ONE;
    }

    private BigDecimal resolveProductStockKg(Product product) {
        if (product.getStockKg() != null) return product.getStockKg();
        return BigDecimal.valueOf(product.getStockQuantity() == null ? 0 : product.getStockQuantity()).setScale(2, RoundingMode.HALF_UP);
    }

    private BigDecimal resolveEffectiveSellingPrice(Product product) {
        BigDecimal sellingPrice = product.getSellingPrice();
        Integer pct = product.getDiscountPercent();
        if (pct != null && pct > 0 && pct < 100 && sellingPrice != null) {
            LocalDateTime now = LocalDateTime.now();
            LocalDateTime start = product.getDiscountStartDate();
            LocalDateTime end = product.getDiscountEndDate();
            boolean active = (start == null || !now.isBefore(start))
                    && (end == null || !now.isAfter(end));
            if (active) {
                return sellingPrice.multiply(BigDecimal.valueOf(100 - pct))
                        .divide(BigDecimal.valueOf(100), 0, RoundingMode.HALF_UP);
            }
        }
        return sellingPrice;
    }

    private BigDecimal resolveRequestKg(BigDecimal quantityKg, Integer quantity) {
        BigDecimal resolved = quantityKg != null ? quantityKg : BigDecimal.valueOf(quantity == null ? 1 : quantity);
        return resolved.setScale(2, RoundingMode.HALF_UP);
    }

    private Integer resolveLegacyQuantity(BigDecimal quantityKg, Integer quantity) {
        if (quantity != null && quantity > 0) return quantity;
        return quantityKg == null ? 1 : Math.max(1, quantityKg.setScale(0, RoundingMode.CEILING).intValue());
    }

    private BigDecimal resolveOrderDetailKg(OrderDetail detail) {
        if (detail.getQuantityKg() != null) return detail.getQuantityKg();
        return BigDecimal.valueOf(detail.getQuantity() == null ? 0 : detail.getQuantity()).setScale(2, RoundingMode.HALF_UP);
    }

    private BigDecimal resolveProductComboKg(ProductCombo item) {
        if (item.getQuantityKg() != null) return item.getQuantityKg();
        return BigDecimal.valueOf(item.getQuantity() == null ? 0 : item.getQuantity()).setScale(2, RoundingMode.HALF_UP);
    }

    private BigDecimal calculateMysteryBoxUnitWeight(MysteryBox box) {
        // Mystery box should be counted by bag/box unit for reward preview.
        // 1 mystery box = 1 rescued unit (mapped to 1 point) to match FE display "... túi".
        return BigDecimal.ONE;
    }

    private BigDecimal calculateSnapshotRescuedWeight(List<OrderDetail> orderDetails, List<OrderMysteryBox> mysteryBoxes) {
        BigDecimal total = BigDecimal.ZERO;
        if (orderDetails != null) {
            for (OrderDetail detail : orderDetails) {
                if (detail.getRescuedWeightKg() != null) {
                    total = total.add(detail.getRescuedWeightKg());
                }
            }
        }
        if (mysteryBoxes != null) {
            for (OrderMysteryBox orderBox : mysteryBoxes) {
                if (orderBox.getRescuedWeightKg() != null) {
                    total = total.add(orderBox.getRescuedWeightKg());
                }
            }
        }
        return total.setScale(2, RoundingMode.HALF_UP);
    }


    private void validateMysteryBoxStock(MysteryBox box, Integer quantity) {
        int remaining = box.getTotalQuantity() - box.getSoldQuantity();
        if (remaining < quantity) {
            throw new AppException(ErrorCode.MYSTERY_BOX_SOLD_OUT);
        }
    }

    private void checkMysteryBoxAvailability(Product product) {

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

    private List<String> collectVoucherIdentifiers(OrderCreationRequest request) {
        LinkedHashSet<String> identifiers = new LinkedHashSet<>();
        if (request.getVoucherUserId() != null) {
            identifiers.add(request.getVoucherUserId().toString());
        }
        if (request.getVoucherUserIds() != null) {
            for (UUID voucherUserId : request.getVoucherUserIds()) {
                if (voucherUserId != null) {
                    identifiers.add(voucherUserId.toString());
                }
            }
        }
        if (request.getVoucherCodes() != null) {
            for (String voucherCode : request.getVoucherCodes()) {
                if (voucherCode != null && !voucherCode.isBlank()) {
                    identifiers.add(voucherCode.trim());
                }
            }
        }
        return new ArrayList<>(identifiers);
    }

    private void assertSameShopOwner(User currentShop, User itemShop) {
        if (currentShop == null || itemShop == null) return;
        if (!currentShop.getId().equals(itemShop.getId())) {
            throw new AppException(ErrorCode.ORDER_MULTI_SHOP_NOT_ALLOWED);
        }
    }

    @Transactional
    @Override
    public CheckoutBatchResponse checkoutBatch(CheckoutBatchRequest request) {
        if (request.getShopOrders() == null || request.getShopOrders().isEmpty()) {
            throw new AppException(ErrorCode.INVALID_REQUEST);
        }

        User buyer = getCurrentUser();
        boolean isPayos = request.getPaymentMethod() != null
                && !"COD".equalsIgnoreCase(request.getPaymentMethod());

        CheckoutBatch batch = CheckoutBatch.builder()
                .buyer(buyer)
                .paymentMethod(request.getPaymentMethod() != null ? request.getPaymentMethod() : "PAYOS")
                .status(isPayos ? CheckoutBatchStatus.PENDING : CheckoutBatchStatus.COMPLETED)
                .totalAmount(BigDecimal.ZERO)
                .build();
        checkoutBatchRepository.save(batch);

        List<OrderResponse> orderResponses = new ArrayList<>();
        List<Order> createdOrders = new ArrayList<>();
        BigDecimal totalBatch = BigDecimal.ZERO;

        for (ShopOrderRequest shopOrder : request.getShopOrders()) {
            if (shopOrder.getItems() == null || shopOrder.getItems().isEmpty()) {
                throw new AppException(ErrorCode.INVALID_REQUEST);
            }

            OrderCreationRequest single = OrderCreationRequest.builder()
                    .recipientName(request.getRecipientName())
                    .recipientPhone(request.getRecipientPhone())
                    .shippingAddress(request.getShippingAddress())
                    .note(request.getNote())
                    .paymentMethod(request.getPaymentMethod())
                    .voucherUserId(shopOrder.getVoucherUserId())
                    .voucherUserIds(shopOrder.getVoucherUserIds())
                    .voucherCodes(shopOrder.getVoucherCodes())
                    .restrictToShopVouchers(true)
                    .items(shopOrder.getItems())
                    .build();

            OrderResponse created = createOrder(single);
            Order order = orderRepository.findById(created.getId())
                    .orElseThrow(() -> new AppException(ErrorCode.ORDER_NOT_FOUND));

            if (shopOrder.getShopOwnerId() != null
                    && order.getShopOwner() != null
                    && !shopOrder.getShopOwnerId().equals(order.getShopOwner().getId())) {
                throw new AppException(ErrorCode.INVALID_REQUEST);
            }

            order.setCheckoutBatch(batch);
            if (isPayos) {
                order.setStatus(OrderStatus.AWAITING_PAYMENT);
                orderRepository.save(order);
                created.setStatus(OrderStatus.AWAITING_PAYMENT.name());
            }

            orderResponses.add(created);
            createdOrders.add(order);
            totalBatch = totalBatch.add(created.getTotalAmount() != null ? created.getTotalAmount() : BigDecimal.ZERO);
        }

        if (request.getBatchVoucherUserId() != null) {
            BigDecimal batchDiscount = voucherUserService.applyBatchVoucher(
                    batch, createdOrders, request.getBatchVoucherUserId());
            totalBatch = totalBatch.subtract(batchDiscount).max(BigDecimal.ZERO);
            for (int i = 0; i < createdOrders.size(); i++) {
                Order order = createdOrders.get(i);
                if ("COD".equalsIgnoreCase(order.getPaymentMethod())) {
                    order.setCodAmount(order.getTotalAmount());
                }
                orderRepository.save(order);
                if (i < orderResponses.size()) {
                    orderResponses.get(i).setTotalAmount(order.getTotalAmount());
                }
            }
            checkoutBatchRepository.save(batch);
        }

        batch.setTotalAmount(totalBatch);
        String checkoutUrl = null;
        String qrCodeUrl = null;
        String payosOrderCode = null;

        if (isPayos) {
            long orderCode = System.currentTimeMillis() + batch.getId();
            PayosPayoutResult qr = payosService.createOrderPaymentQr(
                    totalBatch,
                    orderCode,
                    batch.getId(),
                    buyer.getFullName()
            );
            if (qr == null || !qr.isSuccess()) {
                throw new AppException(ErrorCode.PAYOS_CREATE_QR_FAILED);
            }
            payosOrderCode = String.valueOf(orderCode);
            checkoutUrl = qr.getCheckoutUrl();
            qrCodeUrl = qr.getQrCodeUrl();
            batch.setPayosOrderCode(payosOrderCode);
            batch.setCheckoutUrl(checkoutUrl);
            batch.setQrCodeUrl(qrCodeUrl);
        }

        checkoutBatchRepository.save(batch);

        return CheckoutBatchResponse.builder()
                .batchId(batch.getId())
                .orders(orderResponses)
                .orderIds(orderResponses.stream().map(OrderResponse::getId).toList())
                .totalAmount(totalBatch)
                .paymentMethod(batch.getPaymentMethod())
                .checkoutUrl(checkoutUrl)
                .qrCodeUrl(qrCodeUrl)
                .payosOrderCode(payosOrderCode)
                .build();
    }
}
