package com.example.movietickets.dto.request;

import com.example.movietickets.entity.SeatType;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;

public record SeatRequest(
    @NotNull Long screenId,
    @NotBlank String seatNumber,
    @NotNull SeatType seatType
) {
}
