    package com.swd301.foodmarket.entity;

    import com.fasterxml.jackson.annotation.JsonIgnore;
    import com.swd301.foodmarket.enums.ProductStatus;
    import jakarta.persistence.*;
    import lombok.*;
    import lombok.experimental.FieldDefaults;

    import java.math.BigDecimal;
    import java.time.LocalDate;
    import java.time.LocalDateTime;
    import java.util.List;

    @Entity
    @Table(name = "products")
    @Getter
    @Setter
    @NoArgsConstructor
    @AllArgsConstructor
    @Builder
    @FieldDefaults(level = AccessLevel.PRIVATE)
    public class Product {

        @Id
        @GeneratedValue(strategy = GenerationType.IDENTITY)
        @Column(name = "product_id")
        Integer id;

        // ================= RELATIONSHIPS =================

        // Shop owner
        @ManyToOne
        @JoinColumn(name = "shop_owner_id")
        @JsonIgnore
        User shopOwner;

        // Category
        @ManyToOne
        @JoinColumn(name = "category_id")
        @JsonIgnore
        Category category;


        // Build combo M-N
        @OneToMany(mappedBy = "product")
        @JsonIgnore
        List<ProductCombo> productCombos;

        // Product → Mystery mapping
        @OneToMany(mappedBy = "product")
        @JsonIgnore
        List<ProductMystery> productMysteries;

        // Product gallery images
        @OneToMany(mappedBy = "product", cascade = CascadeType.ALL, orphanRemoval = true, fetch = FetchType.LAZY)
        @JsonIgnore
        List<ProductImage> images;

        // ================= DATA =================

        @Column(name = "product_name", nullable = false)
        String productName;

        @Column(length = 50)
        @Builder.Default
        String unit = "kg";

        @Column(name = "stock_kg", precision = 12, scale = 2)
        BigDecimal stockKg;

        @Column(name = "min_order_kg", precision = 12, scale = 2)
        BigDecimal minOrderKg;

        @Column(precision = 18, scale = 2)
        BigDecimal sellingPrice;

        Integer stockQuantity;

        @Column(length = 500)
        String description;

        @Enumerated(EnumType.STRING)
        ProductStatus status;

        @Column(length = 500)
        String imageUrl;

        @Column(name = "expiry_date")
        LocalDate expiryDate;

        // ================= DISCOUNT =================

        @Column(name = "discount_percent")
        Integer discountPercent;

        @Column(name = "discount_start_date")
        LocalDateTime discountStartDate;

        @Column(name = "discount_end_date")
        LocalDateTime discountEndDate;
    }
