package com.example.movietickets.service;

import com.example.movietickets.entity.Booking;

import java.math.BigDecimal;

public record CancellationResult(Booking booking, BigDecimal refundAmount) {
}
