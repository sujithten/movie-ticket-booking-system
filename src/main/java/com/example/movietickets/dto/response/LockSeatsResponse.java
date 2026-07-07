package com.example.movietickets.dto.response;

import java.time.Instant;
import java.util.List;

public record LockSeatsResponse(List<Long> seatIds, Instant expiresAt) {
}
