package com.example.movietickets.controller;

import com.example.movietickets.dto.request.CreateBookingRequest;
import com.example.movietickets.dto.response.BookingResponse;
import com.example.movietickets.dto.response.CancelBookingResponse;
import com.example.movietickets.entity.Booking;
import com.example.movietickets.security.CurrentUser;
import com.example.movietickets.service.BookingService;
import com.example.movietickets.service.CancellationResult;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/bookings")
@RequiredArgsConstructor
public class BookingController {

    private final BookingService bookingService;

    @PostMapping
    public ResponseEntity<BookingResponse> confirm(
            @Valid @RequestBody CreateBookingRequest request,
            @RequestHeader(value = "Idempotency-Key", required = false) String idempotencyKey) {
        Long userId = CurrentUser.id();
        Booking booking = bookingService.confirmBooking(userId, request, idempotencyKey);
        return ResponseEntity.status(HttpStatus.CREATED).body(bookingService.toResponse(booking));
    }

    @GetMapping
    public List<BookingResponse> myBookings() {
        Long userId = CurrentUser.id();
        return bookingService.getBookingsForUser(userId).stream().map(bookingService::toResponse).toList();
    }

    @PostMapping("/{id}/cancel")
    public CancelBookingResponse cancel(@PathVariable Long id) {
        Long userId = CurrentUser.id();
        CancellationResult result = bookingService.cancelBooking(userId, id);
        return new CancelBookingResponse(result.booking().getId(), result.booking().getStatus(),
            result.refundAmount());
    }
}
