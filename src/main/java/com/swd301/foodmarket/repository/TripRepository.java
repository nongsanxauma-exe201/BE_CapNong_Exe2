package com.swd301.foodmarket.repository;

import com.swd301.foodmarket.entity.Trip;
import com.swd301.foodmarket.enums.TripStatus;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.time.LocalDateTime;
import java.util.List;

@Repository
public interface TripRepository extends JpaRepository<Trip, Integer> {

    Page<Trip> findByStatus(TripStatus status, Pageable pageable);

    List<Trip> findByIsShareableAndDepartureTimeIsAfter(Boolean isShareable, LocalDateTime departureTimeAfter);
}
