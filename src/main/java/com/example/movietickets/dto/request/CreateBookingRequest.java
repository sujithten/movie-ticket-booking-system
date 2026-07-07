package com.example.movietickets.dto.request;

import jakarta.validation.constraints.NotNull;

public record CreateBookingRequest(
    @NotNull Long showId,
    @NotNull java.util.List<Long> seatIds,
    String discountCode,
    // Deterministic mock-payment-failure trigger for tests/demo — see README "Assumptions".
    boolean simulatePaymentFailure
) {
}
