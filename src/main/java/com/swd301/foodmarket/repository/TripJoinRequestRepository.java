package com.swd301.foodmarket.repository;

import com.swd301.foodmarket.dto.response.TripResponse;
import com.swd301.foodmarket.entity.TripJoinRequest;
import com.swd301.foodmarket.entity.User;
import com.swd301.foodmarket.enums.TripJoinStatus;
import com.swd301.foodmarket.enums.TripStatus;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface TripJoinRequestRepository extends JpaRepository<TripJoinRequest, Integer> {
    List<TripJoinRequest> findByTripId(Integer tripId);

    List<TripJoinRequest> findByTripIdAndStatus(Integer tripId, TripJoinStatus status);

    Page<TripJoinRequest> findByFarmer(User farmer, Pageable pageable);

    Page<TripJoinRequest> findByTrip_Owner(User tripOwner, Pageable pageable);

    List<TripJoinRequest> findByFarmerAndStatus(User farmer, TripJoinStatus status);

    List<TripJoinRequest> findByFarmerAndStatusAndTrip_Status(User farmer, TripJoinStatus status, TripStatus tripStatus);

    List<TripJoinRequest> findByFarmerAndStatusOrderByCreatedAtAsc(User farmer, TripJoinStatus status);

    List<TripJoinRequest> findByFarmerAndStatusAndTrip_StatusOrderByCreatedAtDesc(User farmer, TripJoinStatus status, TripStatus tripStatus);
}
