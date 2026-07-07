package com.example.movietickets.dto.request;

import jakarta.validation.constraints.NotEmpty;

import java.util.List;

public record LockSeatsRequest(@NotEmpty List<Long> seatIds) {
}
