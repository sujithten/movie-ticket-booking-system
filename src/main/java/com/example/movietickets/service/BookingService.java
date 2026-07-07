package com.example.movietickets.service;

import com.example.movietickets.dto.request.CreateBookingRequest;
import com.example.movietickets.dto.response.BookingResponse;
import com.example.movietickets.entity.Booking;
import com.example.movietickets.entity.BookingSeat;
import com.example.movietickets.repository.BookingRepository;
import com.example.movietickets.repository.BookingSeatRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.Optional;

/**
 * Outer orchestrator — deliberately NOT @Transactional itself, so exceptions from
 * BookingWriter/CancellationWriter are caught (or notifications fired) from
 * *outside* their transaction boundaries. See BookingWriter's javadoc for why
 * that matters for the idempotency-key unique-constraint catch.
 */
@Service
@RequiredArgsConstructor
public class BookingService {

    private final BookingRepository bookingRepository;
    private final BookingSeatRepository bookingSeatRepository;
    private final BookingWriter bookingWriter;
    private final CancellationWriter cancellationWriter;
    private final NotificationService notificationService;

    public Booking confirmBooking(Long userId, CreateBookingRequest request, String idempotencyKey) {
        if (idempotencyKey != null && !idempotencyKey.isBlank()) {
            Optional<Booking> existing = bookingRepository.findByUserIdAndIdempotencyKey(userId, idempotencyKey);
            if (existing.isPresent()) {
                return existing.get();
            }
        }

        Booking booking;
        try {
            booking = bookingWriter.confirmBooking(userId, request, idempotencyKey);
        } catch (DataIntegrityViolationException e) {
            // Two requests carrying the same Idempotency-Key raced past the pre-check above;
            // the UNIQUE(user_id, idempotency_key) constraint caught it at insert time instead.
            if (idempotencyKey != null) {
                return bookingRepository.findByUserIdAndIdempotencyKey(userId, idempotencyKey).orElseThrow(() -> e);
            }
            throw e;
        }

        notificationService.sendBookingConfirmation(booking);
        return booking;
    }

    public List<Booking> getBookingsForUser(Long userId) {
        return bookingRepository.findByUserIdOrderByCreatedAtDesc(userId);
    }

    public CancellationResult cancelBooking(Long userId, Long bookingId) {
        CancellationResult result = cancellationWriter.cancelBooking(userId, bookingId);
        notificationService.sendBookingCancellation(result.booking(), result.refundAmount());
        return result;
    }

    public BookingResponse toResponse(Booking booking) {
        List<Long> seatIds = bookingSeatRepository.findByBookingId(booking.getId()).stream()
            .map(BookingSeat::getId)
            .map(id -> id.getSeatId())
            .toList();
        return new BookingResponse(booking.getId(), booking.getShowId(), booking.getStatus(),
            booking.getTotalAmount(), seatIds, booking.getCreatedAt());
    }
}
