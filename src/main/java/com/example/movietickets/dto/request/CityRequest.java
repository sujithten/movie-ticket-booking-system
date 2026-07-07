package com.example.movietickets.dto.request;

import jakarta.validation.constraints.NotBlank;

public record CityRequest(@NotBlank String name) {
}
