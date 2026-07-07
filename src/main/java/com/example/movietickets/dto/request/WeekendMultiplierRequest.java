package com.example.movietickets.dto.request;

import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;

import java.math.BigDecimal;

public record WeekendMultiplierRequest(@NotNull @Positive BigDecimal multiplier) {
}
