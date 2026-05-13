package com.swd301.foodmarket.entity;

import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.swd301.foodmarket.enums.ReturnStatus;
import jakarta.persistence.*;
import lombok.*;
import lombok.experimental.FieldDefaults;

import java.math.BigDecimal;
import java.time.LocalDateTime;

@Entity
@Table(name = "return_requests")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
@FieldDefaults(level = AccessLevel.PRIVATE)
public class ReturnRequest {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "return_id")
    Integer id;

    // ================= RELATIONSHIPS =================

    @ManyToOne
    @JoinColumn(name = "order_detail_id", nullable = false)
    OrderDetail orderDetail;

    @ManyToOne
    @JoinColumn(name = "shop_owner_id", nullable = false)
    @JsonIgnore
    User shopOwner;

    @ManyToOne
    @JoinColumn(name = "buyer_id", nullable = false)
    @JsonIgnore
    User buyer;

    // ================= DATA =================

    @Column(length = 1000)
    String reason;

    @Column(length = 2000)
    String evidence; // Semicolon separated URLs

    @Column(length = 1000)
    String shopResponse;

    @Column(length = 1000)
    String adminRemark;

    @Column(precision = 18, scale = 2)
    BigDecimal refundAmount;

    @Column(name = "payout_order_code")
    Long payoutOrderCode;

    @Enumerated(EnumType.STRING)
    @Column(length = 50)
    ReturnStatus status;

    @Column(name = "created_at")
    LocalDateTime createdAt;

    @Column(name = "updated_at")
    LocalDateTime updatedAt;

    @PrePersist
    protected void onCreate() {
        createdAt = LocalDateTime.now();
        updatedAt = LocalDateTime.now();
    }

    @PreUpdate
    protected void onUpdate() {
        updatedAt = LocalDateTime.now();
    }

    @JsonProperty("productName")
    public String getProductName() {
        return orderDetail != null && orderDetail.getProduct() != null ? orderDetail.getProduct().getProductName() : "Product";
    }

    @JsonProperty("quantity")
    public Integer getQuantity() {
        return orderDetail != null ? orderDetail.getQuantity() : 0;
    }

    @JsonProperty("unitPrice")
    public BigDecimal getUnitPrice() {
        return orderDetail != null ? orderDetail.getUnitPrice() : BigDecimal.ZERO;
    }

    @JsonProperty("orderId")
    public Integer getOrderId() {
        return orderDetail != null && orderDetail.getOrder() != null ? orderDetail.getOrder().getId() : null;
    }
}
