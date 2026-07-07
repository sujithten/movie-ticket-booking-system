package com.example.movietickets.dto.response;

import com.example.movietickets.entity.BookingStatus;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;

public record BookingResponse(
    Long id,
    Long showId,
    BookingStatus status,
    BigDecimal totalAmount,
    List<Long> seatIds,
    LocalDateTime createdAt
) {
}
