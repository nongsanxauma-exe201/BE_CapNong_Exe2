package com.swd301.foodmarket.dto.response;

import com.swd301.foodmarket.entity.Image;
import com.swd301.foodmarket.entity.Order;
import com.swd301.foodmarket.enums.OrderStatus;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.OneToMany;
import lombok.Builder;
import lombok.Data;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

@Data
public class TrackingResponse {
    private UUID trackingId;
    private OrderStatus status;
    private LocalDateTime createdAt ;
    private List<ImageResponse> images ;
}
