package com.example.movietickets.dto.request;

import com.example.movietickets.entity.DiscountType;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;

import java.math.BigDecimal;
import java.time.LocalDateTime;

public record DiscountCodeRequest(
    @NotBlank String code,
    @NotNull DiscountType type,
    @NotNull @Positive BigDecimal value,
    BigDecimal minOrderAmount,
    LocalDateTime validFrom,
    LocalDateTime validTo
) {
}
