package com.swd301.foodmarket.repository;

import com.swd301.foodmarket.entity.TripJoinRequest;
import com.swd301.foodmarket.enums.TripJoinStatus;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface TripJoinRequestRepository extends JpaRepository<TripJoinRequest, Integer> {
    List<TripJoinRequest> findByTripId(Integer tripId);

    List<TripJoinRequest> findByTripIdAndStatus(Integer tripId, TripJoinStatus status);
}
