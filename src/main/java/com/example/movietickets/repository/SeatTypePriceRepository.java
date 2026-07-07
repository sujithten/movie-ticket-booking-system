package com.example.movietickets.repository;

import com.example.movietickets.entity.SeatType;
import com.example.movietickets.entity.SeatTypePrice;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;

public interface SeatTypePriceRepository extends JpaRepository<SeatTypePrice, Long> {

    Optional<SeatTypePrice> findBySeatType(SeatType seatType);
}
