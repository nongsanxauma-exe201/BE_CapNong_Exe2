package com.swd301.foodmarket.mapper;

import com.swd301.foodmarket.dto.response.NotificationResponse;
import com.swd301.foodmarket.entity.Notification;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;

@Mapper(componentModel = "spring")
public interface NotificationMapper {

    @Mapping(source = "admin.id", target = "adminId")
    @Mapping(source = "admin.fullName", target = "adminName")
    @Mapping(target = "shopOwnerId", ignore = true)
    @Mapping(target = "shopOwnerName", ignore = true)
    @Mapping(target = "buyerId", ignore = true)
    @Mapping(target = "buyerName", ignore = true)
    NotificationResponse toResponse(Notification notification);
}