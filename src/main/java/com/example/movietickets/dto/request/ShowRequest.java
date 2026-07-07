package com.example.movietickets.dto.request;

import jakarta.validation.constraints.NotNull;

import java.time.LocalDateTime;

public record ShowRequest(
    @NotNull Long movieId,
    @NotNull Long screenId,
    @NotNull LocalDateTime startTime,
    @NotNull LocalDateTime endTime
) {
}
