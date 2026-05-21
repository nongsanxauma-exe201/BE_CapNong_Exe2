package com.swd301.foodmarket.repository;

import com.swd301.foodmarket.entity.Trip;
import com.swd301.foodmarket.entity.User;
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

    List<Trip> findByIsShareableAndDepartureTimeIsAfterAndStatus(Boolean isShareable, LocalDateTime departureTimeAfter, TripStatus status);

    List<Trip> findByIsShareableAndDepartureTimeIsAfterAndStatusAndOwnerIsNot(Boolean isShareable, LocalDateTime departureTimeAfter, TripStatus status, User owner);

    Page<Trip> findByStatusAndOwner(TripStatus status, User owner, Pageable pageable);

    Page<Trip> findAllByOwner(User owner, Pageable pageable);
}
