package com.example.movietickets.dto.response;

import com.example.movietickets.entity.BookingStatus;

import java.math.BigDecimal;

public record CancelBookingResponse(Long bookingId, BookingStatus status, BigDecimal refundAmount) {
}
