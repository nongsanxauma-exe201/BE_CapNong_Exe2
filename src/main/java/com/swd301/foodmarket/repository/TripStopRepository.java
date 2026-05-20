package com.swd301.foodmarket.repository;

import com.swd301.foodmarket.entity.TripStop;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface TripStopRepository extends JpaRepository<TripStop, Integer> {
    List<TripStop> findByTripIdOrderBySequenceAsc(Integer tripId);
}
