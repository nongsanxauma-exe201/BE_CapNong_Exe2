package com.swd301.foodmarket.mapper;

import org.mapstruct.*;
import com.swd301.foodmarket.dto.request.UserCreationRequest;
import com.swd301.foodmarket.dto.request.UserUpdateRequest;
import com.swd301.foodmarket.dto.response.UserResponse;
import com.swd301.foodmarket.entity.User;

@Mapper(componentModel = "spring")
public interface UserMapper {
    User toUser(UserCreationRequest request);

    @Mapping(target = "bankName", source = "bankName")
    @Mapping(target = "bankAccount", source = "bankAccount")
    @Mapping(target = "bankAccountHolder", source = "bankAccountHolder")
    @Mapping(target = "licenseImageUrl", source = "licenseImageUrl")
    @Mapping(target = "vehicleDocImageUrl", source = "vehicleDocImageUrl")
    UserResponse toUserResponse(User user);

    @BeanMapping(nullValuePropertyMappingStrategy = NullValuePropertyMappingStrategy.IGNORE)
    void updateUser(@MappingTarget User user, UserUpdateRequest request);
}