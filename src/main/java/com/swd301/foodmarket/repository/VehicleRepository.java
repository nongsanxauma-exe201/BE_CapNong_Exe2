package com.swd301.foodmarket.repository;

import com.swd301.foodmarket.dto.response.VehicleResponse;
import com.swd301.foodmarket.entity.Vehicle;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.Collection;
import java.util.List;
import java.util.Optional;

@Repository
public interface VehicleRepository extends JpaRepository<Vehicle, Integer> {


    List<Vehicle> findByOwner_Id(Integer ownerId);


    boolean existsByVehicleNumber(String vehicleNumber);
}
