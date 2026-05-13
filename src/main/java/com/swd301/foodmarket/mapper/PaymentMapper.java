package com.swd301.foodmarket.mapper;

import com.swd301.foodmarket.dto.request.CreatePaymentRequest;
import com.swd301.foodmarket.dto.response.PaymentResponse;
import com.swd301.foodmarket.entity.Payment;
import org.mapstruct.*;

@Mapper(componentModel = "spring")
public interface PaymentMapper {

    // Map request -> entity (order sẽ set trong service)
    @Mapping(target = "order", ignore = true)
    @Mapping(target = "withdrawRequest", ignore = true)
    @Mapping(target = "transactions", ignore = true)
    @Mapping(target = "id", ignore = true)
    @Mapping(target = "status", ignore = true)
    @Mapping(target = "createdAt", ignore = true)
    @Mapping(target = "updatedAt", ignore = true)
    @Mapping(target = "paymentDate", ignore = true)
    Payment toEntity(CreatePaymentRequest request);

    @Mapping(target = "orderId", source = "order.id")
    @Mapping(target = "withdrawRequestId", source = "withdrawRequest.id")
    PaymentResponse toResponse(Payment payment);

    // Update Payment (VD: update status, gateway, amount...)
    @BeanMapping(nullValuePropertyMappingStrategy = NullValuePropertyMappingStrategy.IGNORE)
    @Mapping(target = "id", ignore = true)
    @Mapping(target = "order", ignore = true)
    @Mapping(target = "withdrawRequest", ignore = true)
    @Mapping(target = "transactions", ignore = true)
    void updatePayment(@MappingTarget Payment payment, CreatePaymentRequest request);
}