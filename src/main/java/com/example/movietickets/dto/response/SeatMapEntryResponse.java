package com.example.movietickets.dto.response;

import com.example.movietickets.entity.SeatStatus;
import com.example.movietickets.entity.SeatType;

import java.math.BigDecimal;

public record SeatMapEntryResponse(
    Long seatId,
    String seatNumber,
    SeatType seatType,
    SeatStatus status,
    BigDecimal price
) {
}
