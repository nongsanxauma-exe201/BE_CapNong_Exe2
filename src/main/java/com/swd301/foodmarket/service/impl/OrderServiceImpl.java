package com.swd301.foodmarket.service.impl;

import com.swd301.foodmarket.dto.goong.LatLong;
import com.swd301.foodmarket.dto.request.OrderCreationRequest;
import com.swd301.foodmarket.dto.request.OrderItemRequest;
import com.swd301.foodmarket.dto.request.OrderUpdateRequest;
import com.swd301.foodmarket.dto.response.*;
import com.swd301.foodmarket.entity.*;
import com.swd301.foodmarket.enums.OrderStatus;
import com.swd301.foodmarket.exception.AppException;
import com.swd301.foodmarket.exception.ErrorCode;
import com.swd301.foodmarket.mapper.OrderMapper;
import com.swd301.foodmarket.repository.*;
import com.swd301.foodmarket.service.*;
import org.springframework.cglib.core.Local;
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
import org.springframework.web.multipart.MultipartFile;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.DayOfWeek;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
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

    GoongMapService goongMapService;   // ✅ dùng để geocode địa chỉ
    TrackingService trackingService;
    private final ReviewRepository reviewRepository;

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
    }private OrderShopReport.ReportOrderDay buildChartItem(
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
        Pageable pageable = PageRequest.of(page, size, Sort.by("createdAt").descending());
        Page<Order> pageResult = orderRepository.findByShopOwner(getCurrentUser(), pageable);
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
    public OrderResponse updateOrder(Integer id, OrderUpdateRequest request, List<MultipartFile> files) {

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


    private void validateMysteryBoxStock(MysteryBox box, Integer quantity) {

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
}
