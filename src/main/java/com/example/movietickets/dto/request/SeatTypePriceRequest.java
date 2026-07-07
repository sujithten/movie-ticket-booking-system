package com.example.movietickets.dto.request;

import com.example.movietickets.entity.SeatType;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;

import java.math.BigDecimal;

public record SeatTypePriceRequest(
    @NotNull SeatType seatType,
    @NotNull @Positive BigDecimal basePrice
) {
}
