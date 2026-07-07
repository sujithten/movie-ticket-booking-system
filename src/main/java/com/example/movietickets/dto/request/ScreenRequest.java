package com.example.movietickets.dto.request;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;

public record ScreenRequest(
    @NotNull Long theaterId,
    @NotBlank String name,
    @NotNull @Positive Integer totalSeats
) {
}
