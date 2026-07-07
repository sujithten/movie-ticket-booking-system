package com.example.movietickets.dto.request;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;

public record MovieRequest(
    @NotBlank String title,
    @NotNull @Positive Integer durationMinutes,
    String language,
    String genre
) {
}
