package com.example.movietickets.dto.request;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;

public record TheaterRequest(
    @NotBlank String name,
    @NotNull Long cityId,
    String address
) {
}
