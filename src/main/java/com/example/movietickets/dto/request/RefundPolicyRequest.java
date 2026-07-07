package com.example.movietickets.dto.request;

import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotNull;

public record RefundPolicyRequest(
    @NotNull @Min(0) Integer cutoffHoursBeforeShow,
    @NotNull @Min(0) @Max(100) Integer refundPercentage
) {
}
