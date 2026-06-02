package com.swd301.foodmarket.service.impl;

import com.swd301.foodmarket.dto.request.VehicleCreateRequest;
import com.swd301.foodmarket.dto.request.VehicleUpdateRequest;
import com.swd301.foodmarket.dto.response.VehicleResponse;
import com.swd301.foodmarket.entity.User;
import com.swd301.foodmarket.entity.Vehicle;
import com.swd301.foodmarket.exception.AppException;
import com.swd301.foodmarket.exception.ErrorCode;
import com.swd301.foodmarket.mapper.VehicleMapper;
import com.swd301.foodmarket.repository.UserRepository;
import com.swd301.foodmarket.repository.VehicleRepository;
import com.swd301.foodmarket.service.VehicleService;
import lombok.AccessLevel;
import lombok.RequiredArgsConstructor;
import lombok.experimental.FieldDefaults;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.stereotype.Service;

import java.util.List;


@Service
@RequiredArgsConstructor
@FieldDefaults(level = AccessLevel.PRIVATE, makeFinal = true)
public class VehicleServiceImpl implements VehicleService {

    VehicleRepository vehicleRepository;
    UserRepository userRepository;
    VehicleMapper vehicleMapper;

    @Override
    public VehicleResponse create(VehicleCreateRequest request) {

        User owner = getCurrentUser();
        if (vehicleRepository.existsByVehicleNumber((request.getVehicleNumber())))
        {
            throw new AppException(ErrorCode.VEHICLE_NUMBER_ALREADY_EXISTED);
        }

        Vehicle vehicle = Vehicle.builder()
                .vehicleNumber(request.getVehicleNumber())
                .capacityKg(request.getCapacityKg())
                .type(request.getType())
                .owner(owner)
                .build();

        vehicleRepository.save(vehicle);

        return vehicleMapper.toVehicleResponse(vehicle);
    }

    @Override
    public List<VehicleResponse> getMyVehicles() {

        User owner = getCurrentUser();

        List<Vehicle> responses = vehicleRepository.findByOwner_Id(owner.getId());
        return responses.stream().map(vehicleMapper::toVehicleResponse).toList();
    }

    @Override
    public VehicleResponse getById(Integer id) {

        Vehicle vehicle = vehicleRepository.findById(id)
                .orElseThrow(() -> new AppException(ErrorCode.VEHICLE_NOT_FOUND));

        return vehicleMapper.toVehicleResponse(vehicle);
    }

    @Override
    public VehicleResponse update(Integer id, VehicleUpdateRequest request) {

        User owner = getCurrentUser();

        Vehicle vehicle = vehicleRepository.findById(id)
                .orElseThrow(() -> new AppException(ErrorCode.VEHICLE_NOT_FOUND));

        if (!vehicle.getOwner().getId().equals(owner.getId())) {
            throw new AppException(ErrorCode.UNAUTHORIZED);
        }
        if (vehicleRepository.existsByVehicleNumber((request.getVehicleNumber())))
        {
            throw new AppException(ErrorCode.VEHICLE_NUMBER_ALREADY_EXISTED);
        }
        vehicle.setVehicleNumber(request.getVehicleNumber());

        vehicle.setCapacityKg(request.getCapacityKg());
        vehicle.setType(request.getType());

        vehicleRepository.save(vehicle);

        return vehicleMapper.toVehicleResponse(vehicle);
    }

    @Override
    public void delete(Integer id) {


        User owner = getCurrentUser();

        Vehicle vehicle = vehicleRepository.findById(id)
                .orElseThrow(() -> new AppException(ErrorCode.VEHICLE_NOT_FOUND));

        if (!vehicle.getOwner().getId().equals(owner.getId())) {
            throw new AppException(ErrorCode.UNAUTHORIZED);
        }

        vehicleRepository.delete(vehicle);
    }

    private User getCurrentUser() {
        String email = SecurityContextHolder.getContext().getAuthentication().getName();
        return userRepository.findByEmail(email)
                .orElseThrow(() -> new AppException(ErrorCode.USER_NOT_EXISTED));
    }
}